package org.lifelab.lifelabbe.dto.archive;

public record ArchiveAttendanceRateResponse(
        String experimentId,
        int attendanceRate
) {}
