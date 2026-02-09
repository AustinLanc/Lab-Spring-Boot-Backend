package org.example.labbackend.controller;

import org.example.labbackend.model.Reminder;
import org.example.labbackend.repository.ReminderRepository;
import org.example.labbackend.service.TelegramBotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private final ReminderRepository reminderRepository;
    private final TelegramBotService telegramBotService;

    public ReminderController(ReminderRepository reminderRepository, TelegramBotService telegramBotService) {
        this.reminderRepository = reminderRepository;
        this.telegramBotService = telegramBotService;
    }

    @GetMapping
    public List<Reminder> getAll() {
        return reminderRepository.findAllOrderByDue();
    }

    @GetMapping("/pending")
    public List<Reminder> getPending() {
        return reminderRepository.findAllPendingOrderByDue();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reminder> getById(@PathVariable Long id) {
        return reminderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/batch/{batch}")
    public List<Reminder> getByBatch(@PathVariable String batch) {
        return reminderRepository.findByBatch(batch);
    }

    @GetMapping("/search")
    public List<Reminder> searchByBatch(@RequestParam String batch) {
        return reminderRepository.findByBatchContainingIgnoreCase(batch);
    }

    @PostMapping
    public ResponseEntity<List<Reminder>> createBatchReminders(@RequestBody BatchReminderRequest request) {
        String batch = request.batch().toUpperCase();
        int dayOffset = request.dayOffset() != null ? request.dayOffset() : 0;

        telegramBotService.addBatchReminders(batch, dayOffset);
        telegramBotService.sendMessage("Batch " + batch + " added!");

        return ResponseEntity.ok(reminderRepository.findByBatch(batch));
    }

    @PostMapping("/single")
    public ResponseEntity<Reminder> createSingleReminder(@RequestBody SingleReminderRequest request) {
        String batch = request.batch().toUpperCase();
        String reminderId = batch + "-" + request.intervalType();

        if (reminderRepository.existsByReminderId(reminderId)) {
            return ResponseEntity.badRequest().build();
        }

        Instant baseTime = Instant.now()
                .truncatedTo(ChronoUnit.DAYS)
                .plus(19, ChronoUnit.HOURS);

        int dayOffset = request.dayOffset() != null ? request.dayOffset() : 0;
        Instant dueDate = telegramBotService.calculateDueDate(baseTime, request.intervalType(), dayOffset);

        Reminder reminder = Reminder.builder()
                .reminderId(reminderId)
                .batch(batch)
                .intervalType(request.intervalType())
                .due(dueDate)
                .notified(false)
                .createdAt(Instant.now())
                .build();

        return ResponseEntity.ok(reminderRepository.save(reminder));
    }

    @PutMapping("/{id}/notified")
    public ResponseEntity<Reminder> markAsNotified(@PathVariable Long id) {
        return reminderRepository.findById(id)
                .map(reminder -> {
                    reminder.setNotified(true);
                    return ResponseEntity.ok(reminderRepository.save(reminder));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!reminderRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        reminderRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/reminder/{reminderId}")
    public ResponseEntity<Void> deleteByReminderId(@PathVariable String reminderId) {
        if (!reminderRepository.existsByReminderId(reminderId)) {
            return ResponseEntity.notFound().build();
        }
        reminderRepository.deleteByReminderId(reminderId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/cleanup")
    public ResponseEntity<Void> cleanupNotified() {
        reminderRepository.deleteAllNotified();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/test-telegram")
    public ResponseEntity<String> testTelegram() {
        try {
            telegramBotService.sendMessage("Test message from LabBackend!");
            return ResponseEntity.ok("Message sent successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    public record BatchReminderRequest(String batch, Integer dayOffset) {}
    public record SingleReminderRequest(String batch, String intervalType, Integer dayOffset) {}

}
