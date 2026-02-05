package org.example.companyboiler.controller;

import org.example.companyboiler.model.Reminder;
import org.example.companyboiler.repository.ReminderRepository;
import org.example.companyboiler.service.TelegramBotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private final ReminderRepository reminderRepository;
    private final TelegramBotService telegramBotService;

    private static final String[] INTERVALS = {"48h", "7d", "3m", "1y"};

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

        Instant baseTime = Instant.now()
                .truncatedTo(ChronoUnit.DAYS)
                .plus(19, ChronoUnit.HOURS); // 7 PM

        List<Reminder> createdReminders = new ArrayList<>();

        for (String interval : INTERVALS) {
            String reminderId = batch + "-" + interval;

            if (reminderRepository.existsByReminderId(reminderId)) {
                continue;
            }

            Instant dueDate = calculateDueDate(baseTime, interval, dayOffset);

            Reminder reminder = Reminder.builder()
                    .reminderId(reminderId)
                    .batch(batch)
                    .intervalType(interval)
                    .due(dueDate)
                    .notified(false)
                    .createdAt(Instant.now())
                    .build();

            createdReminders.add(reminderRepository.save(reminder));
        }

        if (!createdReminders.isEmpty()) {
            telegramBotService.sendMessage("Batch " + batch + " added!");
        }

        return ResponseEntity.ok(createdReminders);
    }

    @PostMapping("/single")
    public ResponseEntity<Reminder> createSingleReminder(@RequestBody SingleReminderRequest request) {
        String reminderId = request.batch().toUpperCase() + "-" + request.intervalType();

        if (reminderRepository.existsByReminderId(reminderId)) {
            return ResponseEntity.badRequest().build();
        }

        Instant baseTime = Instant.now()
                .truncatedTo(ChronoUnit.DAYS)
                .plus(19, ChronoUnit.HOURS);

        int dayOffset = request.dayOffset() != null ? request.dayOffset() : 0;
        Instant dueDate = calculateDueDate(baseTime, request.intervalType(), dayOffset);

        Reminder reminder = Reminder.builder()
                .reminderId(reminderId)
                .batch(request.batch().toUpperCase())
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

    private Instant calculateDueDate(Instant baseTime, String interval, int dayOffset) {
        Instant dueDate = switch (interval) {
            case "48h" -> baseTime.plus(48, ChronoUnit.HOURS);
            case "7d" -> baseTime.plus(7, ChronoUnit.DAYS);
            case "3m" -> baseTime.plus(90, ChronoUnit.DAYS);
            case "1y" -> baseTime.plus(365, ChronoUnit.DAYS);
            default -> {
                try {
                    int hours = Integer.parseInt(interval);
                    yield baseTime.plus(hours, ChronoUnit.HOURS);
                } catch (NumberFormatException e) {
                    yield baseTime.plus(7, ChronoUnit.DAYS);
                }
            }
        };

        if (dayOffset != 0) {
            dueDate = dueDate.plus(dayOffset, ChronoUnit.DAYS);
        }

        return dueDate;
    }

    public record BatchReminderRequest(String batch, Integer dayOffset) {}
    public record SingleReminderRequest(String batch, String intervalType, Integer dayOffset) {}

}
