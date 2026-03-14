package org.lifelab.lifelabbe.dto.experiment;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class CalendarExperimentDto {

    private Long experimentId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
}