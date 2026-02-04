package org.example.companyboiler.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "testing_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestingData {

    @Id
    @Column(name = "batch")
    private String batch;

    @Column(name = "code")
    private String code;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "pen_0x")
    private String pen0x;

    @Column(name = "pen_60x")
    private String pen60x;

    @Column(name = "pen_10k")
    private String pen10k;

    @Column(name = "pen_100k")
    private String pen100k;

    @Column(name = "drop_point")
    private String dropPoint;

    @Column(name = "weld")
    private String weld;

    @Column(name = "timken")
    private String timken;

    @Column(name = "rust")
    private String rust;

    @Column(name = "copper_corrosion")
    private String copperCorrosion;

    @Column(name = "oxidation")
    private String oxidation;

    @Column(name = "oil_bleed")
    private String oilBleed;

    @Column(name = "spray_off")
    private String sprayOff;

    @Column(name = "washout")
    private String washout;

    @Column(name = "pressure_bleed")
    private String pressureBleed;

    @Column(name = "roll_stability_dry")
    private String rollStabilityDry;

    @Column(name = "roll_stability_wet")
    private String rollStabilityWet;

    @Column(name = "wear")
    private String wear;

    @Column(name = "ft_ir")
    private String ftIr;

    @Column(name = "minitest_minus_40")
    private String minitestMinus40;

    @Column(name = "minitest_minus_30")
    private String minitestMinus30;

    @Column(name = "minitest_minus_20")
    private String minitestMinus20;

    @Column(name = "minitest_0")
    private String minitest0;

    @Column(name = "minitest_20")
    private String minitest20;

    @Column(name = "rheometer")
    private String rheometer;

    @Column(name = "rheometer_temp")
    private String rheometerTemp;

}
