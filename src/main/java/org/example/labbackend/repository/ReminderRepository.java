package org.example.labbackend.repository;

import org.example.labbackend.model.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    Optional<Reminder> findByReminderId(String reminderId);

    List<Reminder> findByBatch(String batch);

    List<Reminder> findByBatchContainingIgnoreCase(String batch);

    List<Reminder> findByNotifiedFalseAndDueBefore(Instant now);

    List<Reminder> findByNotifiedFalse();

    @Query("SELECT r FROM Reminder r WHERE r.notified = false ORDER BY r.due ASC")
    List<Reminder> findAllPendingOrderByDue();

    @Query("SELECT r FROM Reminder r ORDER BY r.due ASC")
    List<Reminder> findAllOrderByDue();

    @Modifying
    @Transactional
    @Query("DELETE FROM Reminder r WHERE r.notified = true")
    void deleteAllNotified();

    @Modifying
    @Transactional
    void deleteByReminderId(String reminderId);

    boolean existsByReminderId(String reminderId);

}
