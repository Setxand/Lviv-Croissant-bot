package com.example.demo.service.telegramService.impl;

import com.example.demo.entity.SpeakingMessage;
import com.example.demo.entity.lvivCroissants.CustomerOrdering;
import com.example.demo.entity.peopleRegister.MUser;
import com.example.demo.entity.peopleRegister.TUser;
import com.example.demo.constantEnum.messengerEnums.Roles;
import com.example.demo.dto.telegram.Message;
import com.example.demo.entity.peopleRegister.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.eventService.telegramEventService.TelegramCreatingOwnCroissantEventService;
import com.example.demo.service.repositoryService.CustomerOrderingRepositoryService;
import com.example.demo.service.peopleRegisterService.TelegramUserRepositoryService;
import com.example.demo.service.repositoryService.SpeakingMessagesRepositoryService;
import com.example.demo.service.telegramService.TelegramMessageParserHelperService;
import com.example.demo.service.telegramService.TelegramMessageSenderService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ResourceBundle;

import static com.example.demo.constantEnum.messengerEnums.speaking.ServerSideSpeaker.*;
import static com.example.demo.constantEnum.telegramEnums.TelegramUserStatus.OWN_MENU_STATUS;

@Service
public class TelegramMessageParserHelperServiceImpl implements TelegramMessageParserHelperService {
    @Autowired
    private TelegramUserRepositoryService telegramUserRepositoryService;
    @Autowired
    private CustomerOrderingRepositoryService customerOrderingRepositoryService;
    @Autowired
    private TelegramMessageSenderService telegramMessageSenderService;
    @Autowired
    private TelegramCreatingOwnCroissantEventService telegramCreatingOwnCroissantEventService;
    @Autowired
    private SpeakingMessagesRepositoryService speakingMessagesRepositoryService;
    @Autowired
    private UserRepository userRepository;
    private static final Logger logger = Logger.getLogger(TelegramMessageParserHelperServiceImpl.class);
    @Value("${subscription.url}")
    private String SUBSCRIPTION_URL;
    @Value("${app.verify.token}")
    private String VER_TOK;
    @Value("${server.url}")
    private String SERVER_URL;
    @Override
    public void helpStart(Message message) {
        TUser tUser;

        User user = userRepository.findByTelegramId(message.getChat().getId()).orElse(new User());
        if(user.getMUser()==null) {
            if (telegramUserRepositoryService.findByChatId(message.getChat().getId()) == null) {
                tUser = new TUser();

                tUser.setLocale(message.getFrom().getLanguageCode());
                tUser.setRole(Roles.CUSTOMER);
            } else tUser = telegramUserRepositoryService.findByChatId(message.getChat().getId());

            tUser.setName(message.getFrom().getFirstName());
            tUser.setLastName(message.getFrom().getLastName());
            tUser.setChatId(message.getChat().getId());
            tUser.setUserName(message.getFrom().getUserName());


            telegramUserRepositoryService.saveAndFlush(tUser);

        }
        else {
            MUser mUser = user.getMUser();

        }
        SpeakingMessage speakingMessage = speakingMessagesRepositoryService.findByKey(HELLO_MESSAGE.name());
        if(message.getPlatform()==null)
        telegramMessageSenderService.simpleMessage(speakingMessage.getMessage(),message);
        else{
            String helloMessage = ResourceBundle.getBundle("dictionary").getString(HELLO_SERVICE.name());
            telegramMessageSenderService.simpleMessage(helloMessage,message);
        }

    }

    @Override
    public void helpDeleteOrderings(Message message) {
        List<CustomerOrdering>customerOrderings = customerOrderingRepositoryService.findAll();
        TUser tUser = telegramUserRepositoryService.findByChatId(message.getChat().getId());
        for(CustomerOrdering customerOrdering:customerOrderings){
          tUser.getCustomerOrderings().remove(customerOrdering);
          customerOrdering.setTUser(null);
          customerOrderingRepositoryService.delete(customerOrdering);
        }
        telegramUserRepositoryService.saveAndFlush(tUser);
        telegramMessageSenderService.simpleMessage( ResourceBundle.getBundle("dictionary").getString(DONE.name()),message);
    }

    @Override
    public void helpCreateOwnCroissant(Message message) {
        TUser tUser = telegramUserRepositoryService.findByChatId(message.getChat().getId());
        telegramUserRepositoryService.changeStatus(tUser,OWN_MENU_STATUS);
        telegramCreatingOwnCroissantEventService.createOwn(message);

    }




}
