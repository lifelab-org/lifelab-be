from app.main import app  # 기존 app 그대로 가져오기

from pydantic import BaseModel, Field
from typing import List, Optional, Literal

from app.ml.daily_model import MetricInput, PreStateInput, generate_summary

Polarity = Literal["HIGHER_IS_BETTER", "LOWER_IS_BETTER"]
SummaryStatus = Literal[
    "GENERATED",
    "NEED_TODAY_RECORD",
    "INSUFFICIENT_METRICS",
]

class MetricDelta(BaseModel):
    recordItemKey: str
    yesterday: float
    today: float
    polarity: Polarity
    unit: Optional[str] = None

class PreState(BaseModel):
    recordItemKey: str
    value: float
    polarity: Polarity
    unit: Optional[str] = None

class DailySummaryReq(BaseModel):
    experimentName: str
    recordDate: str
    # 오늘 기록이 없으면 False
    hasTodayRecord: bool = True
    # metrics는 없을 수도 있음
    metrics: List[MetricDelta] = Field(default_factory=list)
    # 스프링에서 보내는 preStates 받기 (없으면 빈 리스트)
    preStates: List[PreState] = Field(default_factory=list)

class DailySummaryRes(BaseModel):
    status: SummaryStatus
    summary: str

def _fallback_no_today(experiment_name: str, record_date: str, has_pre: bool) -> DailySummaryRes:
    if has_pre:
        msg = (
            f"{record_date} 기록이 아직 없어요. "
            "실험 전 상태 값은 저장되어 있어요. 오늘 기록을 남기면 한줄 요약을 만들어 드릴게요."
        )
    else:
        msg = (
            f"{record_date} 기록이 아직 없어요. "
            "오늘의 기록을 남기면 한줄 요약을 만들어 드릴게요."
        )
    return DailySummaryRes(status="NEED_TODAY_RECORD", summary=msg)

def _fallback_insufficient_metrics(experiment_name: str, record_date: str, has_pre: bool) -> DailySummaryRes:
    if has_pre:
        msg = (
            f"{record_date} 기준 비교할 지표가 부족해요. "
            "실험 전 상태 값은 저장되어 있어요."
        )
    else:
        msg = (
            f"{record_date} 기준 비교할 지표가 부족해요. "
            "기록이 더 쌓이면 변화 기반 요약이 가능해요."
        )
    return DailySummaryRes(status="INSUFFICIENT_METRICS", summary=msg)

@app.post("/ai/daily-summary", response_model=DailySummaryRes)
def daily_summary(req: DailySummaryReq):
    has_pre = len(req.preStates) > 0

    # 오늘 기록 자체가 없으면
    if not req.hasTodayRecord:
        return _fallback_no_today(req.experimentName, req.recordDate, has_pre)

    # 비교할 metrics가 없으면
    if not req.metrics:
        return _fallback_insufficient_metrics(req.experimentName, req.recordDate, has_pre)

    # 모델 입력 생성
    metrics_in = [
        MetricInput(
            key=m.recordItemKey,
            yesterday=m.yesterday,
            today=m.today,
            polarity=m.polarity,
        )
        for m in req.metrics
    ]

    pre_in = [
        PreStateInput(
            key=p.recordItemKey,
            value=p.value,
            polarity=p.polarity,
        )
        for p in req.preStates
    ]

    # 모델로 요약 생성 (pre_states 전달)
    summary = generate_summary(
        req.experimentName,
        req.recordDate,
        metrics_in,
        has_today_record=req.hasTodayRecord,
        pre_states=pre_in,
    )
    return DailySummaryRes(status="GENERATED", summary=summary)