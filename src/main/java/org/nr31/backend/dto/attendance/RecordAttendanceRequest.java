package org.nr31.backend.dto.attendance;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordAttendanceRequest {

    @NotNull
    private Instant occurrenceDate;

    @NotNull
    private List<Long> memberIds;
}
