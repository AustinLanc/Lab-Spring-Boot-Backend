package org.example.companyboiler.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;

@Entity
@Table(name = "retains")
@Getter
@Setter
@NoArgsConstructor
public class Retain {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
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
