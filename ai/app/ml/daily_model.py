import os
import math
import random
from dataclasses import dataclass
from typing import List, Tuple, Optional

from joblib import dump, load
from sklearn.calibration import CalibratedClassifierCV
from sklearn.ensemble import HistGradientBoostingClassifier

MODEL_VERSION = "v6_neutral_only_up_down"
MODEL_PATH = os.path.join(os.path.dirname(__file__), f"model_{MODEL_VERSION}.joblib")


@dataclass
class MetricInput:
    key: str
    yesterday: float
    today: float
    polarity: str  # 지금은 문장/방향 판단에는 사용하지 않음 (그대로 받아두기만)


@dataclass
class PreStateInput:
    key: str
    value: float
    polarity: str


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


def _clean_text(s: str) -> str:
    if not s:
        return s
    s = s.replace("**", "")
    s = s.replace(" ,", ",").replace(" .", ".").replace(" !", "!").replace(" ?", "?")
    while ",  " in s:
        s = s.replace(",  ", ", ")
    while "  " in s:
        s = s.replace("  ", " ")
    return s.strip()


def _iga(word: str) -> str:
    if not word:
        return "이"
    last = word[-1]
    code = ord(last)
    if 0xAC00 <= code <= 0xD7A3:
        jong = (code - 0xAC00) % 28
        return "이" if jong != 0 else "가"
    return "이"


def _raw_delta(yesterday: float, today: float) -> float:
    return today - yesterday


def featurize(metrics: List[MetricInput]) -> List[List[float]]:
    if not metrics:
        return [[0.0] * 14]

    deltas = [_raw_delta(m.yesterday, m.today) for m in metrics]
    absd = [abs(d) for d in deltas]
    n = len(deltas)

    improved_ratio = sum(1 for d in deltas if d > 0) / n
    worsened_ratio = sum(1 for d in deltas if d < 0) / n
    near_zero_ratio = sum(1 for d in deltas if abs(d) < 1e-6) / n

    rel = []
    for m in metrics:
        d = _raw_delta(m.yesterday, m.today)
        rel.append(_safe_div(d, abs(m.yesterday) + 1e-8))

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
    if os.path.exists(MODEL_PATH):
        return

    rng = random.Random(42)
    X: List[List[float]] = []
    y: List[int] = []

    for _ in range(9000):
        n = rng.randint(3, 8)
        day_mood = rng.gauss(0, 0.5)

        deltas: List[float] = []
        for _i in range(n):
            scale = math.exp(rng.gauss(0, 0.75))
            base = (0.55 * day_mood) + rng.gauss(0, 0.35)
            if rng.random() < 0.10:
                base += rng.gauss(0, 1.6)
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

        score = (
            0.78 * feat[0] +
            0.14 * feat[12] +
            0.10 * feat[7] -
            0.48 * feat[4] -
            0.18 * feat[5]
        )

        if score > 0.62:
            y.append(2)
        elif score > -0.20:
            y.append(1)
        else:
            y.append(0)

    base = HistGradientBoostingClassifier(
        max_depth=3,
        learning_rate=0.06,
        max_iter=380,
        random_state=42
    )

    model = CalibratedClassifierCV(base, method="sigmoid", cv=3)
    model.fit(X, y)
    dump(model, MODEL_PATH)


_MODEL = None


def load_model():
    global _MODEL
    if _MODEL is None:
        _bootstrap_train_if_missing()
        _MODEL = load(MODEL_PATH)
    return _MODEL


def predict_label(metrics: List[MetricInput]) -> int:
    model = load_model()
    x = featurize(metrics)
    proba = model.predict_proba(x)[0]
    label = max(range(len(proba)), key=lambda i: proba[i])
    return int(label)


def _label_by_scale_rule(metrics: List[MetricInput]) -> int:
    if not metrics:
        return 1

    abs_diffs = [abs(m.today - m.yesterday) for m in metrics]
    max_abs = max(abs_diffs) if abs_diffs else 0.0

    if max_abs >= 2.0:
        return 2
    if max_abs >= 1.0:
        return 1
    return 0


def pick_up_down(metrics: List[MetricInput]) -> Tuple[Optional[str], Optional[str], bool]:
    if not metrics:
        return None, None, True

    eps = 1e-6

    
    changes: List[Tuple[str, float]] = [
        (m.key, _raw_delta(m.yesterday, m.today))
        for m in metrics
    ]

    all_same = all(abs(d) < eps for (_, d) in changes)
    if all_same:
        return None, None, True

    up = max(changes, key=lambda x: x[1])
    down = min(changes, key=lambda x: x[1])

    up_key = up[0] if up[1] > eps else None
    down_key = down[0] if down[1] < -eps else None

    return up_key, down_key, False


def _label_to_style(label: int) -> Tuple[int, List[str]]:
    if label == 0:
        max_mentions = 1
        mains = [
            "전반적으로 큰 변동은 없었어요.",
            "오늘은 지표 변동이 크지 않았어요.",
            "전체 흐름은 비교적 비슷했어요.",
        ]
    elif label == 2:
        max_mentions = 2
        mains = [
            "오늘은 지표 변동이 눈에 띄었어요.",
            "지표 변동이 있었어요.",
            "오늘은 값이 전반적으로 달라졌어요.",
        ]
    else:
        max_mentions = 2
        mains = [
            "지표 변동이 있었어요.",
            "오늘은 지표가 달라졌어요.",
            "변화가 있었어요.",
        ]
    return max_mentions, mains


def generate_one_liner(
    experiment_name: str,
    record_date: str,
    metrics: List[MetricInput],
    has_today_record: bool = True,
    pre_states: Optional[List[PreStateInput]] = None
) -> str:
    pre_states = pre_states or []

    if not has_today_record:
        return _clean_text("오늘 기록이 없어요.")

    if not metrics:
        return _clean_text("비교할 지표가 없어요.")

    up_key, down_key, all_same = pick_up_down(metrics)
    if all_same:
        return _clean_text("전반적으로 변화 없어요.")

    label = _label_by_scale_rule(metrics)
    max_mentions, main_candidates = _label_to_style(label)

    parts: List[str] = []
    if up_key:
        parts.append(f"{up_key}{_iga(up_key)} 높아졌어요.")
    if down_key:
        parts.append(f"{down_key}{_iga(down_key)} 낮아졌어요.")

    parts = parts[:max_mentions]

    if not parts:
        return _clean_text("전반적으로 변화 없어요.")

    rng = random.Random()
    main = rng.choice(main_candidates)
    detail = " " + " ".join(parts)

    templates = [
        "{main}{detail}",
        "{main} {detail}",
    ]
    s = rng.choice(templates).format(main=main, detail=detail)

    return _clean_text(s)


def generate_summary(
    experiment_name: str,
    record_date: str,
    metrics: List[MetricInput],
    has_today_record: bool = True,
    pre_states: Optional[List[PreStateInput]] = None
) -> str:
    return generate_one_liner(
        experiment_name=experiment_name,
        record_date=record_date,
        metrics=metrics,
        has_today_record=has_today_record,
        pre_states=pre_states
    )


if __name__ == "__main__":
    sample = [
        MetricInput("피로도", 3, 7, "LOWER_IS_BETTER"),        # 값 증가 -> "피로도가 높아졌어요."
        MetricInput("집중력", 5, 1, "HIGHER_IS_BETTER"),       # 값 감소 -> "집중력이 낮아졌어요."
        MetricInput("수면만족도", 3, 3, "HIGHER_IS_BETTER"),   # 변화 없음
        MetricInput("밤", 3, 7, "HIGHER_IS_BETTER"),          # "높아졌어요."
    ]

    print(generate_one_liner("실험", "2026-02-28", sample, has_today_record=True))