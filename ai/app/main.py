from typing import List, Optional, Literal, Dict
from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.status import HTTP_422_UNPROCESSABLE_ENTITY
from pydantic import BaseModel, Field
from datetime import date
import random
import json
import sys
import os
import math

# ML 모델 의존성: 있으면 사용, 없으면 폴백

try:
    from joblib import dump, load
    from sklearn.calibration import CalibratedClassifierCV
    from sklearn.ensemble import HistGradientBoostingClassifier
    SKLEARN_AVAILABLE = True
except Exception:
    SKLEARN_AVAILABLE = False


app = FastAPI()


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    try:
        body_bytes = await request.body()
        body_text = body_bytes.decode("utf-8", errors="ignore")
    except Exception:
        body_text = "<failed to read body>"

    print("=== 422 Validation Error ===", file=sys.stderr, flush=True)
    print("URL:", str(request.url), file=sys.stderr, flush=True)
    print("Errors:", json.dumps(exc.errors(), ensure_ascii=False, indent=2), file=sys.stderr, flush=True)
    print("Body:", body_text, file=sys.stderr, flush=True)

    return JSONResponse(
        status_code=HTTP_422_UNPROCESSABLE_ENTITY,
        content={"detail": exc.errors()},
    )


# 모델 / 입력 스키마
Polarity = Literal["HIGHER_IS_BETTER", "LOWER_IS_BETTER"]


class MetricChange(BaseModel):
    key: str = Field(..., description="지표 이름(예: 피로도, 소화 상태)")
    before: float
    after: float
    polarity: Polarity = Field(..., description="높을수록 좋은지/낮을수록 좋은지")
    unit: Optional[str] = Field(default=None, description="단위(선택)")


class CommentReq(BaseModel):
    experimentName: str
    startDate: Optional[date] = None
    endDate: Optional[date] = None

    # 100 초과 허용(표시도 그대로 가능)
    attendanceRate: float = Field(..., ge=0, description="출석률(0 이상, 100 초과도 허용)")
    successRate: Optional[float] = Field(default=None, ge=0, description="성공률(0 이상, 100 초과도 허용)")

    metrics: List[MetricChange] = Field(default_factory=list)
    topChangedMetricKey: Optional[str] = None


class AiCommentGenerateResponse(BaseModel):
    comment: str



# ML 모델 파일 경로

MODEL_VERSION = "v1_comment_action_picker"
MODEL_PATH = os.path.join(os.path.dirname(__file__), f"model_{MODEL_VERSION}.joblib")

def _cap_percent(x: float) -> float:
    """톤/분기 판단용: 0~100으로 캡"""
    if x < 0:
        return 0.0
    return min(x, 100.0)


def _att_floor_int(x: float) -> int:
    
    if x <= 0:
        return 0
    return int(math.floor(x))


def _is_improved(before: float, after: float, polarity: Polarity) -> Optional[bool]:
    if after == before:
        return None
    if polarity == "HIGHER_IS_BETTER":
        return after > before
    return after < before


def _fmt_delta(before: float, after: float) -> str:
    d = after - before
    sign = "+" if d > 0 else ""
    return f"{sign}{d:.2f}"


def _attendance_tone_short(att: float) -> str:
    a = _cap_percent(att)
    if a >= 85:
        return "꽤 꾸준했어요"
    if a >= 70:
        return "전반적으로 괜찮았어요"
    if a >= 50:
        return "중간중간 비는 날이 있었어요"
    return "기록이 적어서 해석이 조심스러워요"


def _utility_score(before: float, after: float, polarity: str) -> float:
    
    delta = after - before
    abs_delta = abs(delta)
    rel = abs_delta / (abs(before) + 1.0)

    if after == before:
        direction_sign = 0.0
    else:
        if polarity == "HIGHER_IS_BETTER":
            direction_sign = 1.0 if delta > 0 else -1.0
        else:
            direction_sign = 1.0 if delta < 0 else -1.0

    magnitude = (0.6 * rel) + (0.4 * abs_delta)
    return direction_sign * magnitude


def _pick_top_metric(analyzed: List[Dict], top_key: Optional[str]) -> Optional[Dict]:
    if top_key:
        t = next((x for x in analyzed if x["key"] == top_key), None)
        if t:
            return t
    if not analyzed:
        return None
    return max(analyzed, key=lambda x: abs(x["utility"]))


def _success_phrase(success_rate: Optional[float]) -> str:
    if success_rate is None:
        return ""
    return f", 성공률 {success_rate:.0f}%"


def _change_verb(before: float, after: float) -> str:
    if after == before:
        return random.choice(["변화는 없었어요", "그대로였어요", "크게 달라지진 않았어요"])
    if after > before:
        return random.choice(["늘었어요", "올랐어요", "더 커졌어요"])
    return random.choice(["줄었어요", "내려갔어요", "더 작아졌어요"])


def _fmt_value(x: float) -> str:
    return f"{x:.1f}"


def _mean(xs: List[float]) -> float:
    return sum(xs) / len(xs) if xs else 0.0


def _median(xs: List[float]) -> float:
    ys = sorted(xs)
    n = len(ys)
    if n == 0:
        return 0.0
    mid = n // 2
    if n % 2 == 1:
        return ys[mid]
    return (ys[mid - 1] + ys[mid]) / 2.0


def _std(xs: List[float]) -> float:
    n = len(xs)
    if n <= 1:
        return 0.0
    m = _mean(xs)
    var = sum((x - m) ** 2 for x in xs) / (n - 1)
    return var ** 0.5


def _safe_div(a: float, b: float, eps: float = 1e-8) -> float:
    if abs(b) < eps:
        return a / (eps if b >= 0 else -eps)
    return a / b


def _signed_good_delta(before: float, after: float, polarity: str) -> float:
    # "좋은 방향"이 +가 되게 정규화 (모델 입력용)
    if polarity == "LOWER_IS_BETTER":
        return before - after
    return after - before


def featurize(metrics: List[MetricChange]) -> List[List[float]]:
    if not metrics:
        return [[0.0] * 14]

    deltas = [_signed_good_delta(m.before, m.after, m.polarity) for m in metrics]
    absd = [abs(d) for d in deltas]
    n = len(deltas)

    improved_ratio = sum(1 for d in deltas if d > 0) / n
    worsened_ratio = sum(1 for d in deltas if d < 0) / n
    near_zero_ratio = sum(1 for d in deltas if abs(d) < 1e-6) / n

    rel = []
    for m in metrics:
        d = _signed_good_delta(m.before, m.after, m.polarity)
        rel.append(_safe_div(d, abs(m.before) + 1e-8))

    ds_sorted = sorted(deltas)
    k = 2 if n >= 2 else 1
    bottom_k_mean = _mean(ds_sorted[:k])
    top_k_mean = _mean(ds_sorted[-k:])

    return [[
        _mean(deltas),          
        max(deltas),            
        min(deltas),            
        _median(deltas),        
        _std(deltas),           
        _mean(absd),            
        max(absd),             
        sum(deltas),            
        float(improved_ratio),  
        float(worsened_ratio),  
        float(near_zero_ratio), 
        _mean(rel),             
        top_k_mean,             
        bottom_k_mean,          
    ]]


def _bootstrap_train_if_missing() -> None:
    
    if not SKLEARN_AVAILABLE:
        return
    if os.path.exists(MODEL_PATH):
        return

    rng = random.Random(42)
    X: List[List[float]] = []
    y: List[int] = []

    for _ in range(7000):
        n = rng.randint(3, 8)
        mood = rng.gauss(0, 0.5)

        deltas: List[float] = []
        for _i in range(n):
            scale = math.exp(rng.gauss(0, 0.75))
            base = (0.55 * mood) + rng.gauss(0, 0.35)
            if rng.random() < 0.10:
                base += rng.gauss(0, 1.4)
            deltas.append(base * scale)

        absd = [abs(d) for d in deltas]
        improved_ratio = sum(1 for d in deltas if d > 0) / n
        worsened_ratio = sum(1 for d in deltas if d < 0) / n
        near_zero_ratio = sum(1 for d in deltas if abs(d) < 0.05) / n

        ds_sorted = sorted(deltas)
        k = 2 if n >= 2 else 1
        bottom_k_mean = _mean(ds_sorted[:k])
        top_k_mean = _mean(ds_sorted[-k:])

        feat = [
            _mean(deltas),
            max(deltas),
            min(deltas),
            _median(deltas),
            _std(deltas),
            _mean(absd),
            max(absd),
            sum(deltas),
            float(improved_ratio),
            float(worsened_ratio),
            float(near_zero_ratio),
            0.0,
            top_k_mean,
            bottom_k_mean,
        ]
        X.append(feat)

        # 변화 작음/중간/큼
        score = (
            0.70 * feat[0] +
            0.12 * feat[12] +
            0.08 * feat[7] -
            0.40 * feat[4] -
            0.15 * feat[5]
        )
        if score > 0.60:
            y.append(2)   # 변동 큼
        elif score > -0.15:
            y.append(1)   # 변동 중간
        else:
            y.append(0)   # 변동 작음

    base = HistGradientBoostingClassifier(
        max_depth=3,
        learning_rate=0.06,
        max_iter=320,
        random_state=42
    )
    model = CalibratedClassifierCV(base, method="sigmoid", cv=3)
    model.fit(X, y)
    dump(model, MODEL_PATH)


def load_model():
    if not SKLEARN_AVAILABLE:
        return None
    _bootstrap_train_if_missing()
    return load(MODEL_PATH)


def predict_label(metrics: List[MetricChange]) -> Optional[int]:
    model = load_model()
    if model is None:
        return None
    x = featurize(metrics)
    proba = model.predict_proba(x)[0]
    label = max(range(len(proba)), key=lambda i: proba[i])
    return int(label)


# 모델 라벨 기반 우선 결정

def _pick_next_action_by_label(
    label: Optional[int],
    att: float,
    improved_cnt: int,
    worsened_cnt: int,
    abs_delta_mean: float,
    abs_delta_max: float
) -> str:
    templates = {
        "ROUTINE": [
            "다음엔 기록 시간을 하나로 고정해서 7일 연속 기록부터 만들어보세요",
            "해석을 또렷하게 하려면 우선 ‘연속 기록’을 먼저 잡아보는 게 좋아요",
            "기록이 들쭉날쭉하면 판단이 흐려져서, 다음엔 연속 기록을 먼저 만들어보세요",
            "다음엔 기록 루틴부터 고정해보면 결과가 더 선명해질 거예요",
        ],
        "FIX_CAUSE": [
            "변수가 많아 보이니, 원인 후보 1~2개만 잡고 재실험해보세요",
            "이번엔 원인 후보를 1~2개로 줄여서 확인해보면 좋아요",
            "원인 후보를 좁히면 다음 결과가 훨씬 깔끔해져요",
            "이번엔 한두 가지만 바꿔서 영향이 큰 요인을 먼저 찾아보세요",
        ],
        "TUNE_VARIABLE": [
            "좋았던 조건은 유지하고 변수 하나만 살짝 바꿔 최적점을 찾아보세요",
            "흐름이 보이니, 한 가지만 조절하면서 영향이 큰 요인을 좁혀보세요",
            "잘 먹힌 조건은 유지하고, 딱 한 가지만 조절해서 효과를 더 키워보세요",
            "다음엔 한 변수만 조정해서 ‘가장 잘 맞는 지점’을 찾아보면 좋아요",
        ],
        "EXTEND_PERIOD": [
            "변화가 작았다면 기간을 조금 늘리거나 지표를 더 구체적으로 바꿔보세요",
            "다음엔 기간/지표를 조금 조정해서 ‘변화가 보이게’ 만들어보는 것도 좋아요",
            "표본이 더 쌓이면 판단이 선명해져서, 기간을 살짝 늘려보는 것도 좋아요",
            "기간을 조금만 늘리면 흐름이 더 확실해질 가능성이 커요",
        ],
    }

    a = _cap_percent(att)

    # 모델 없을 때 폴백(기존 규칙 기반 점수)
    def score_actions() -> Dict[str, float]:
        scores = {"ROUTINE": 0.0, "FIX_CAUSE": 0.0, "TUNE_VARIABLE": 0.0, "EXTEND_PERIOD": 0.0}
        scores["ROUTINE"] += max(0, 70 - a) / 10.0
        scores["FIX_CAUSE"] += max(0, worsened_cnt - improved_cnt) * 1.2
        scores["TUNE_VARIABLE"] += improved_cnt * 1.0
        low_change = 1.0 if abs_delta_mean < 0.5 and abs_delta_max < 1.0 else 0.0
        scores["EXTEND_PERIOD"] += 2.5 * low_change
        if a >= 70 and abs_delta_max >= 2.0:
            scores["FIX_CAUSE"] += 0.8
            scores["TUNE_VARIABLE"] += 0.8
        return scores

    if label is None:
        best = max(score_actions().items(), key=lambda kv: kv[1])[0]
        return random.choice(templates[best])

    if label == 0:
        priority = ["EXTEND_PERIOD", "ROUTINE", "TUNE_VARIABLE", "FIX_CAUSE"]
    elif label == 2:
        priority = ["FIX_CAUSE", "TUNE_VARIABLE", "ROUTINE", "EXTEND_PERIOD"]
    else:
        priority = ["TUNE_VARIABLE", "ROUTINE", "FIX_CAUSE", "EXTEND_PERIOD"]

    if a < 50 and "ROUTINE" in priority:
        priority = ["ROUTINE"] + [x for x in priority if x != "ROUTINE"]

    if worsened_cnt > improved_cnt and "FIX_CAUSE" in priority:
        priority = ["FIX_CAUSE"] + [x for x in priority if x != "FIX_CAUSE"]

    pick = priority[0]
    return random.choice(templates[pick])



# 코멘트 문장 생성

def _ai_comment_one_or_two_sentences(
    attendance_rate: float,
    success_rate: Optional[float],
    att_tone: str,
    top: Optional[Dict],
    next_action: Optional[str],
) -> str:
    succ = _success_phrase(success_rate)
    att_i = _att_floor_int(attendance_rate) 

    if top:
        verb = _change_verb(top["before"], top["after"])
        v_before = _fmt_value(top["before"])
        v_after = _fmt_value(top["after"])

        first_templates = [
            "출석률 {att_i}%({att_tone}){succ}, '{top_key}'는 {before}→{after}로 {verb}.",
            "출석률 {att_i}%({att_tone}){succ}. '{top_key}' {before}→{after}로 {verb}.",
            "지금 상태는 출석률 {att_i}%({att_tone}){succ}이고, '{top_key}'는 {before}→{after}로 {verb}.",
            "출석률 {att_i}%({att_tone}){succ}였고, 가장 눈에 띈 건 '{top_key}' {before}→{after}로 {verb}.",
            "현재 출석률 {att_i}%({att_tone}){succ}; '{top_key}'는 {before}→{after}로 {verb}.",
        ]
        first = random.choice(first_templates).format(
            att_i=att_i,
            att_tone=att_tone,
            succ=succ,
            top_key=top["key"],
            before=v_before,
            after=v_after,
            verb=verb,
        )
    else:
        first_templates = [
            "출석률 {att_i}%({att_tone}){succ}예요. 지표가 더 쌓이면 변화도 더 정확히 잡을 수 있어요.",
            "출석률 {att_i}%({att_tone}){succ}였어요—이번엔 지표가 적어서 흐름 판단은 조금 보수적으로 보는 게 좋아요.",
            "출석률 {att_i}%({att_tone}){succ}입니다. 우선 기록을 조금 더 쌓아보면 좋아요.",
            "현재는 출석률 {att_i}%({att_tone}){succ} 수준이라, 데이터가 더 있으면 포인트가 또렷해져요.",
        ]
        first = random.choice(first_templates).format(
            att_i=att_i,
            att_tone=att_tone,
            succ=succ,
        )

    if next_action:
        second_templates = [
            "다음은 {action}.",
            "다음엔 {action}.",
            "다음 단계로는 {action}.",
            "바로 다음은 {action}.",
        ]
        second = random.choice(second_templates).format(action=next_action)
        return f"{first} {second}"

    return first



# API

@app.get("/health")
def health():
    return {"ok": True}


@app.post("/ai/comments", response_model=AiCommentGenerateResponse)
def ai_comments(req: CommentReq):
    analyzed: List[Dict] = []
    for m in req.metrics:
        improved = _is_improved(m.before, m.after, m.polarity)
        abs_change = abs(m.after - m.before)
        utility = _utility_score(m.before, m.after, m.polarity)

        analyzed.append({
            "key": m.key,
            "before": m.before,
            "after": m.after,
            "polarity": m.polarity,
            "improved": improved,
            "abs_change": abs_change,
            "delta_str": _fmt_delta(m.before, m.after),
            "utility": utility,
        })

    improved_metrics = [x for x in analyzed if x["improved"] is True]
    worsened_metrics = [x for x in analyzed if x["improved"] is False]

    abs_deltas = [x["abs_change"] for x in analyzed] or [0.0]
    abs_delta_mean = sum(abs_deltas) / len(abs_deltas)
    abs_delta_max = max(abs_deltas)

    top = _pick_top_metric(analyzed, req.topChangedMetricKey)

    att_tone = _attendance_tone_short(req.attendanceRate)

    # 모델 라벨 예측
    label = predict_label(req.metrics) 
    next_action = _pick_next_action_by_label(
        label=label,
        att=req.attendanceRate,
        improved_cnt=len(improved_metrics),
        worsened_cnt=len(worsened_metrics),
        abs_delta_mean=abs_delta_mean,
        abs_delta_max=abs_delta_max,
    )

    comment = _ai_comment_one_or_two_sentences(
        attendance_rate=req.attendanceRate,
        success_rate=req.successRate,
        att_tone=att_tone,
        top=top,
        next_action=next_action,
    )

    return AiCommentGenerateResponse(comment=comment)