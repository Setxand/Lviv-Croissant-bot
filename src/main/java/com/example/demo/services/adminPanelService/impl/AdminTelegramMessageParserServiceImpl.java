package com.example.demo.services.adminPanelService.impl;

import com.example.demo.entities.peopleRegister.TUser;

import com.example.demo.models.telegram.Message;
import com.example.demo.services.adminPanelService.AdminTelegramMessageParserHelperService;
import com.example.demo.services.adminPanelService.AdminTelegramMessageParserService;
import com.example.demo.services.adminPanelService.BotCommandsParserService;
import com.example.demo.services.eventService.servicePanel.TelegramAddingRecordingsEventService;
import com.example.demo.services.peopleRegisterService.TelegramUserRepositoryService;
import com.example.demo.services.telegramService.TelegramMessageSenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.demo.models.telegram.TelegramEntity;
import java.util.List;

import static com.example.demo.enums.telegramEnums.TelegramUserStatus.ADDING_FILLING_STATUS_1;

@Service
public class AdminTelegramMessageParserServiceImpl implements AdminTelegramMessageParserService {
    @Autowired
    private TelegramUserRepositoryService telegramUserRepositoryService;
    @Autowired
    private BotCommandsParserService botCommandsParserService;
    @Autowired
    private TelegramMessageSenderService telegramMessageSenderService;
    @Autowired
    private TelegramAddingRecordingsEventService telegramAddingRecordingsEventService;
    @Autowired
    private AdminTelegramMessageParserHelperService adminTelegramMessageParserHelperService;
    @Override
    public void parseMessage(Message message) {
        if (message.getEntities() != null) {
            List<TelegramEntity> entities = message.getEntities();
            for (TelegramEntity telegramEntity : entities) {
                if(telegramEntity.getType().equals("url")){
                    checkingByStatus(message);
                    break;
                }
                botCommandsParserService.parseBotCommand(message);
            }
            return;
        }
        checkingByStatus(message);

    }

    private void checkingByStatus(Message message) {
        TUser tUser = telegramUserRepositoryService.findByChatId(message.getChat().getId());
        switch (tUser.getStatus()) {
            case ADDING_FILLING_STATUS:
                addingFillingStatus(message, tUser);
                break;
            case ADDING_FILLING_STATUS_1:
                addingFillingStatus1(message, tUser);
                break;
            case NULL_CHECKING_ADDING_CROISSANT_STATUS_1:
                telegramAddingRecordingsEventService.addCroissant(message);
                break;
            case NULL_CHECKING_ADDING_CROISSANT_STATUS:
                telegramAddingRecordingsEventService.addCroissant(message);
                break;
            case SETTING_ROLE_STATUS:
                adminTelegramMessageParserHelperService.helpRoleQuestion(message);
                break;
            case SETTING_ADMIN_STATUS:
                adminTelegramMessageParserHelperService.helpSetRole(message);
                break;
            default:
                telegramMessageSenderService.errorMessage(message);
                break;

        }
    }

    private void addingFillingStatus1(Message message, TUser tUser) {
        telegramAddingRecordingsEventService.addFilling(message);
    }

    private void addingFillingStatus(Message message, TUser tUser) {
        telegramUserRepositoryService.changeStatus(tUser, ADDING_FILLING_STATUS_1);
        telegramAddingRecordingsEventService.addFilling(message);

    }
}
