package org.example.companyboiler.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "monthly_batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyBatch {

    @Id
    @Column(name = "batch")
    private String batch;

    @Column(name = "code")
    private Integer code;

    @Column(name = "date_start")
    private LocalDateTime dateStart;

    @Column(name = "date_end")
    private LocalDateTime dateEnd;

    @Column(name = "lbs")
    private Integer lbs;

    @Column(name = "released")
    private String released;

    @Column(name = "type")
    private String type;

}
