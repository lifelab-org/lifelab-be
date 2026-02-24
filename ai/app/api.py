from app.main import app  # 기존 app 그대로 가져오기

from pydantic import BaseModel, Field
from typing import List, Optional, Literal

from app.ml.daily_model import MetricInput, generate_summary 

Polarity = Literal["HIGHER_IS_BETTER", "LOWER_IS_BETTER"]
SummaryStatus = Literal[
    "GENERATED",
    "NEED_TODAY_RECORD",
    "NEED_YESTERDAY_RECORD",
    "INSUFFICIENT_METRICS",
]

class MetricDelta(BaseModel):
    recordItemKey: str
    yesterday: float
    today: float
    polarity: Polarity
    unit: Optional[str] = None


class DailySummaryReq(BaseModel):
    experimentName: str
    recordDate: str  

    #오늘 기록이 없으면 False로 보내기
    hasTodayRecord: bool = True

    # metrics는 없을 수도 있음
    metrics: List[MetricDelta] = Field(default_factory=list)


class DailySummaryRes(BaseModel):
    status: SummaryStatus
    summary: str

def _fallback_no_today(experiment_name: str, record_date: str) -> DailySummaryRes:
    msg = (
        f"{record_date} 기록이 아직 없어요. "
        "오늘의 기록을 남기면 어제 대비 변화로 한줄 요약을 만들어 드릴게요."
    )
    return DailySummaryRes(status="NEED_TODAY_RECORD", summary=msg)


def _fallback_no_yesterday(experiment_name: str, record_date: str) -> DailySummaryRes:
    msg = (
        f"{record_date} 기준 어제 기록이 없어 비교가 어려워요. "
        "오늘 기록을 쌓으면 내일부터 변화 기반 요약이 가능해요."
    )
    return DailySummaryRes(status="NEED_YESTERDAY_RECORD", summary=msg)




# 엔드포인트
@app.post("/ai/daily-summary", response_model=DailySummaryRes)
def daily_summary(req: DailySummaryReq):
    #오늘 기록 자체가 없으면: 모델 추론 불가
    if not req.hasTodayRecord:
        return _fallback_no_today(req.experimentName, req.recordDate)

    
    #어제 기록이 없어서 yesterday=today로 채워 넣은 케이스 감지
    all_no_compare = all(abs(m.today - m.yesterday) < 1e-9 for m in req.metrics)
    if all_no_compare:
        return _fallback_no_yesterday(req.experimentName, req.recordDate)

    #모델 입력 생성
    metrics_in = [
        MetricInput(
            key=m.recordItemKey,
            yesterday=m.yesterday,
            today=m.today,
            polarity=m.polarity,
        )
        for m in req.metrics
    ]

    #모델로 요약 생성
    summary = generate_summary(req.experimentName, req.recordDate, metrics_in)
    return DailySummaryRes(status="GENERATED", summary=summary)