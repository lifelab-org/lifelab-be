package org.lifelab.lifelabbe.service;

import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.domain.Experiment;
import org.lifelab.lifelabbe.dto.experiment.CalendarExperimentDto;
import org.lifelab.lifelabbe.dto.experiment.CalendarMonthResponse;
import org.lifelab.lifelabbe.repository.ExperimentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final ExperimentRepository experimentRepository;

    public CalendarMonthResponse getExperimentsByMonth(String month, Long userId) {

        // month 검증
        if (!month.matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException("YYYY-MM 형식이어야 합니다.");
        }

        YearMonth yearMonth = YearMonth.parse(month);
        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        // 해당 월과 겹치는 실험 조회
        List<Experiment> experiments =
                experimentRepository.findByUserIdAndDateOverlap(
                        userId,
                        startOfMonth,
                        endOfMonth
                );

        //DTO 변환
        List<CalendarExperimentDto> dtos =
                experiments.stream()
                        .map(e -> new CalendarExperimentDto(
                                e.getId(),
                                e.getTitle(),
                                e.getStartDate(),
                                e.getEndDate()
                        ))
                        .toList();

        return new CalendarMonthResponse(month, dtos);
    }
}