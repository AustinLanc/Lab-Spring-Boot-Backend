package org.example.companyboiler.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "names")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductName {

    @Id
    @Column(name = "code")
    private Integer code;

    @Column(name = "name")
    private String name;

}
