package org.example.companyboiler.service;

import org.example.companyboiler.model.Reminder;
import org.example.companyboiler.repository.ReminderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotService extends TelegramLongPollingBot {

    private final ReminderRepository reminderRepository;

    @Value("${telegram.bot.token:}")
    private String botToken;

    @Value("${telegram.bot.username:}")
    private String botUsername;

    @Value("${telegram.chat.id:}")
    private String chatId;

    private static final String[] INTERVALS = {"48h", "7d", "3m", "1y"};
    private static final Pattern BATCH_PATTERN = Pattern.compile("^([A-Z]{2}\\d{4}[A-Z]?)(?:,(-?\\d+))?$");

    public TelegramBotService(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText().trim().toUpperCase();
            Long messageChatId = update.getMessage().getChatId();

            Matcher matcher = BATCH_PATTERN.matcher(messageText);
            if (matcher.matches()) {
                String batch = matcher.group(1);
                int dayOffset = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;

                addBatchReminders(batch, dayOffset);

                String responseMessage;
                if (dayOffset == 0) {
                    responseMessage = "Batch *" + batch + "* added!";
                } else {
                    responseMessage = "Batch *" + batch + "* added with *" + dayOffset + " day* offset!";
                }

                sendMessageToChat(messageChatId.toString(), responseMessage);
            }
        }
    }

    public void sendMessage(String text) {
        if (chatId == null || chatId.isEmpty()) {
            return;
        }
        sendMessageToChat(chatId, text);
    }

    private void sendMessageToChat(String targetChatId, String text) {
        if (botToken == null || botToken.isEmpty()) {
            return;
        }

        SendMessage message = new SendMessage();
        message.setChatId(targetChatId);
        message.setText(text);
        message.setParseMode("Markdown");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Failed to send Telegram message: " + e.getMessage());
        }
    }

    private void addBatchReminders(String batch, int dayOffset) {
        Instant baseTime = Instant.now()
                .truncatedTo(ChronoUnit.DAYS)
                .plus(19, ChronoUnit.HOURS);

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

            reminderRepository.save(reminder);
        }
    }

    private Instant calculateDueDate(Instant baseTime, String interval, int dayOffset) {
        Instant dueDate = switch (interval) {
            case "48h" -> baseTime.plus(48, ChronoUnit.HOURS);
            case "7d" -> baseTime.plus(7, ChronoUnit.DAYS);
            case "3m" -> baseTime.plus(90, ChronoUnit.DAYS);
            case "1y" -> baseTime.plus(365, ChronoUnit.DAYS);
            default -> baseTime.plus(7, ChronoUnit.DAYS);
        };

        if (dayOffset != 0) {
            dueDate = dueDate.plus(dayOffset, ChronoUnit.DAYS);
        }

        return dueDate;
    }

    @Scheduled(fixedRate = 30 * 60 * 1000) // Every 30 minutes
    public void checkDueReminders() {
        List<Reminder> dueReminders = reminderRepository.findByNotifiedFalseAndDueBefore(Instant.now());

        for (Reminder reminder : dueReminders) {
            String message = "Batch " + reminder.getBatch() + " - " + reminder.getIntervalType() + " check is due!";
            sendMessage(message);

            reminder.setNotified(true);
            reminderRepository.save(reminder);
        }
    }

    @Scheduled(cron = "0 0 3 * * ?") // Every day at 3 AM
    public void cleanupNotifiedReminders() {
        reminderRepository.deleteAllNotified();
    }

}
