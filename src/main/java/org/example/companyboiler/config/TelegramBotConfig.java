package org.example.companyboiler.config;

import org.example.companyboiler.service.TelegramBotService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import jakarta.annotation.PostConstruct;

@Configuration
public class TelegramBotConfig {

    private final TelegramBotService telegramBotService;

    @Value("${telegram.bot.enabled:false}")
    private boolean botEnabled;

    public TelegramBotConfig(TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    @PostConstruct
    public void registerBot() {
        if (!botEnabled) {
            System.out.println("Telegram bot is disabled. Set telegram.bot.enabled=true to enable.");
            return;
        }

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramBotService);
            System.out.println("Telegram bot registered successfully.");
        } catch (TelegramApiException e) {
            System.err.println("Failed to register Telegram bot: " + e.getMessage());
        }
    }

}
