package org.example.labbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class TelegramBotConfig {

    @Value("${telegram.bot.enabled:false}")
    private boolean botEnabled;

    @Value("${telegram.bot.token:}")
    private String botToken;

    @Value("${telegram.chat.id:}")
    private String chatId;

    @PostConstruct
    public void init() {
        if (!botEnabled) {
            System.out.println("Telegram bot is disabled. Set telegram.bot.enabled=true to enable.");
            return;
        }

        if (botToken == null || botToken.isEmpty()) {
            System.out.println("Telegram bot token not configured.");
            return;
        }

        System.out.println("Telegram bot configured (using direct HTTP API).");
    }

}
