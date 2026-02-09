package org.example.labbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.example.labbackend.model.Reminder;
import org.example.labbackend.repository.ReminderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotService {

    private final ReminderRepository reminderRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String botToken;
    private final String chatId;
    private long lastUpdateId = 0;

    private static final String TELEGRAM_API_BASE = "https://api.telegram.org/bot%s/%s";
    private static final String[] INTERVALS = {"48h", "7d", "3m", "1y"};
    private static final Pattern BATCH_PATTERN = Pattern.compile("^([A-Z]{2}\\d{4}[A-Z]?)(?:,(-?\\d+))?$");

    public TelegramBotService(
            ReminderRepository reminderRepository,
            @Value("${telegram.bot.token:}") String botToken,
            @Value("${telegram.chat.id:}") String chatId) {
        this.reminderRepository = reminderRepository;
        this.botToken = botToken;
        this.chatId = chatId;
        this.restTemplate = createRestTemplateWithTrustAllCerts();
        this.objectMapper = new ObjectMapper();
    }

    private RestTemplate createRestTemplateWithTrustAllCerts() {
        try {
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                    .build();

            CloseableHttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                            .setTlsSocketStrategy(new DefaultClientTlsStrategy(sslContext, NoopHostnameVerifier.INSTANCE))
                            .build())
                    .build();

            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
            return new RestTemplate(factory);
        } catch (Exception e) {
            System.err.println("Failed to create SSL-bypassing RestTemplate, using default: " + e.getMessage());
            return new RestTemplate();
        }
    }

    public void sendMessage(String text) {
        if (chatId == null || chatId.isEmpty()) {
            return;
        }
        sendMessageToChat(chatId, text);
    }

    public void sendMessageToChat(String targetChatId, String text) {
        if (botToken == null || botToken.isEmpty()) {
            System.err.println("Telegram bot token not configured");
            return;
        }

        try {
            String url = String.format(TELEGRAM_API_BASE, botToken, "sendMessage");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("chat_id", targetChatId);
            body.put("text", text);
            body.put("parse_mode", "Markdown");

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, request, String.class);

            System.out.println("Telegram message sent successfully");
        } catch (Exception e) {
            System.err.println("Failed to send Telegram message: " + e.getMessage());
        }
    }

    @Scheduled(fixedRate = 5000)
    public void pollForMessages() {
        if (botToken == null || botToken.isEmpty()) {
            return;
        }

        try {
            String url = String.format(TELEGRAM_API_BASE, botToken, "getUpdates") +
                    "?offset=" + (lastUpdateId + 1) + "&timeout=1";

            String response = restTemplate.getForObject(url, String.class);
            if (response == null) return;

            processUpdates(response);
        } catch (Exception e) {
            // Silently ignore polling errors to avoid log spam
        }
    }

    @SuppressWarnings("unchecked")
    private void processUpdates(String jsonResponse) {
        try {
            Map<String, Object> response = objectMapper.readValue(jsonResponse, Map.class);

            if (!Boolean.TRUE.equals(response.get("ok"))) return;

            List<Map<String, Object>> updates = (List<Map<String, Object>>) response.get("result");
            if (updates == null || updates.isEmpty()) return;

            for (Map<String, Object> update : updates) {
                processUpdate(update);
            }
        } catch (Exception e) {
            System.err.println("Error processing Telegram updates: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void processUpdate(Map<String, Object> update) {
        long updateId = ((Number) update.get("update_id")).longValue();
        lastUpdateId = Math.max(lastUpdateId, updateId);

        Map<String, Object> message = (Map<String, Object>) update.get("message");
        if (message == null) return;

        String text = (String) message.get("text");
        if (text == null) return;

        Map<String, Object> chat = (Map<String, Object>) message.get("chat");
        if (chat == null) return;

        String messageChatId = String.valueOf(((Number) chat.get("id")).longValue());

        String upperText = text.trim().toUpperCase();
        Matcher matcher = BATCH_PATTERN.matcher(upperText);
        if (matcher.matches()) {
            String batch = matcher.group(1);
            int dayOffset = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;

            addBatchReminders(batch, dayOffset);

            String responseMessage = dayOffset == 0
                    ? "Batch *" + batch + "* added!"
                    : "Batch *" + batch + "* added with *" + dayOffset + " day* offset!";

            sendMessageToChat(messageChatId, responseMessage);
        }
    }

    public void addBatchReminders(String batch, int dayOffset) {
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

    public Instant calculateDueDate(Instant baseTime, String interval, int dayOffset) {
        Instant dueDate = switch (interval) {
            case "48h" -> baseTime.plus(48, ChronoUnit.HOURS);
            case "3m" -> baseTime.plus(90, ChronoUnit.DAYS);
            case "1y" -> baseTime.plus(365, ChronoUnit.DAYS);
            default -> baseTime.plus(7, ChronoUnit.DAYS); // "7d" and any other
        };

        if (dayOffset != 0) {
            dueDate = dueDate.plus(dayOffset, ChronoUnit.DAYS);
        }

        return dueDate;
    }

    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void checkDueReminders() {
        List<Reminder> dueReminders = reminderRepository.findByNotifiedFalseAndDueBefore(Instant.now());

        for (Reminder reminder : dueReminders) {
            String message = "Batch " + reminder.getBatch() + " - " + reminder.getIntervalType() + " check is due!";
            sendMessage(message);

            reminder.setNotified(true);
            reminderRepository.save(reminder);
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupNotifiedReminders() {
        reminderRepository.deleteAllNotified();
    }

}
