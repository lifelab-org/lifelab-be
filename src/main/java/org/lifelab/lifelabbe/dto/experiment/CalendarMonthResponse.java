package org.lifelab.lifelabbe.dto.experiment;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CalendarMonthResponse {

    private String month;
    private List<CalendarExperimentDto> experiments;
}