package org.example.labbackend.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

@Entity
@Table(name = "retains")
@Getter
@Setter
@NoArgsConstructor
public class Retain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch")
    private String batch;

    @Column(name = "code")
    private Long code;

    @Column(name = "date")
    private Date date;

    @Column(name = "box")
    private Long box;

}
