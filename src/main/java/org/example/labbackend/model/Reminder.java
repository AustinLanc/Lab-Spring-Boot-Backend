package org.example.labbackend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "reminders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reminder_id", unique = true)
    private String reminderId;

    @Column(name = "batch")
    private String batch;

    @Column(name = "interval_type")
    private String intervalType;

    @Column(name = "due")
    private Instant due;

    @Column(name = "notified")
    private Boolean notified;

    @Column(name = "created_at")
    private Instant createdAt;

}
