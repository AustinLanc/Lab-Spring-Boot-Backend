package org.example.labbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyStatsDTO {
    private Integer year;
    private Integer month;
    private String monthName;
    private Long batchesReleased;
    private Long totalPounds;
    private Long reworkBatches;
    private Long reworkPounds;
}
