package org.example.labbackend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "qc")
@Getter
@Setter
@NoArgsConstructor
public class QcLog {

    @Id
    @Column(name = "batch")
    private String batch;

    @Column(name = "code")
    private String code;

    @Column(name = "suffix")
    private String suffix;

    @Column(name = "pen_60x")
    private String pen60x;

    @Column(name = "drop_point")
    private String dropPoint;

    @Column(name = "date")
    private String date;

    @Column(name = "released_by")
    private String releasedBy;

}
