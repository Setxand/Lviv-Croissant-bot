package com.bots.lvivCroissantBot.service.adminPanelService.impl;

import com.bots.lvivCroissantBot.entity.lvivCroissants.CroissantEntity;
import com.bots.lvivCroissantBot.entity.lvivCroissants.CustomerOrdering;
import com.bots.lvivCroissantBot.entity.register.TUser;
import com.bots.lvivCroissantBot.constantEnum.messengerEnum.Objects;
import com.bots.lvivCroissantBot.constantEnum.BotCommands;
import com.bots.lvivCroissantBot.constantEnum.messengerEnum.Role;
import com.bots.lvivCroissantBot.dto.messanger.Shell;
import com.bots.lvivCroissantBot.dto.telegram.CallBackQuery;
import com.bots.lvivCroissantBot.dto.telegram.Message;
import com.bots.lvivCroissantBot.dto.telegram.button.InlineKeyboardButton;
import com.bots.lvivCroissantBot.dto.telegram.button.InlineKeyboardMarkup;
import com.bots.lvivCroissantBot.dto.telegram.button.Markup;
import com.bots.lvivCroissantBot.service.adminPanelService.BotCommandParseHelperService;
import com.bots.lvivCroissantBot.service.peopleRegisterService.TelegramUserRepositoryService;
import com.bots.lvivCroissantBot.service.repositoryService.CroissantRepositoryService;
import com.bots.lvivCroissantBot.service.repositoryService.CustomerOrderingRepositoryService;
import com.bots.lvivCroissantBot.service.supportService.TextFormatter;
import com.bots.lvivCroissantBot.service.telegramService.TelegramMessageSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import static com.bots.lvivCroissantBot.constantEnum.messengerEnum.speaking.ServerSideSpeaker.*;
import static com.bots.lvivCroissantBot.constantEnum.telegramEnum.CallBackData.*;

@Service
public class BotCommandParseHelperServiceImpl implements BotCommandParseHelperService {
    @Autowired
    private TelegramMessageSenderService telegramMessageSenderService;
    @Autowired
    private TelegramUserRepositoryService telegramUserRepositoryService;
    @Autowired
    private CustomerOrderingRepositoryService customerOrderingRepositoryService;
    @Autowired
    private CroissantRepositoryService croissantRepositoryService;
    @Value("${server.url}")
    private String SERVER_URL;
    @Value("${app.verify.token}")
    private String VER_TOK;
    @Value("${subscription.url}")
    private String SUBSCRIPTION_URL;
    @Value("${picture.ordering}")
    private String PICTURE_ORDERING;
    private   final static Logger logger = LoggerFactory.getLogger(BotCommandParseHelperServiceImpl.class);



    @Override
    public void helpInvokeBotHelpCommand(Message message) {
        StringBuilder helpMessage = new StringBuilder();
        for(BotCommands command: BotCommands.values()){
            if(command!=BotCommands.HELP && command!=BotCommands.START)
            helpMessage.append("/"+command.name().toLowerCase()+" - "+ResourceBundle.getBundle("botCommands").getString(command.name())+"\n");
        }
        telegramMessageSenderService.simpleMessage(helpMessage.toString(),message);
    }

    @Override
    public void helpSetUpMessenger(Message message) {
        TUser tUser = telegramUserRepositoryService.findByChatId(message.getChat().getId());
        if(tUser.getUser().getRole()!= Role.ADMIN){
            telegramMessageSenderService.noEnoughPermissions(message);
            return;
        }
        Shell setMessengerWebHook = new Shell();
        setMessengerWebHook.setCallbackUrl(SERVER_URL+"/WebHook");
        setMessengerWebHook.setVerToken(VER_TOK);
        setMessengerWebHook.setObject(Objects.page);
        setMessengerWebHook.setFields(new String[]{"messages","messaging_postbacks"});
        makeRequestToFacebook(message,setMessengerWebHook);

    }

    private void makeRequestToFacebook(Message message, Shell setMessengerWebHook) {
        try {
            ResponseEntity<?> messengerWebhook = new RestTemplate().postForEntity(SUBSCRIPTION_URL,setMessengerWebHook,Object.class);
            logger.debug("Messenger webhook:"+messengerWebhook.getBody());
            telegramMessageSenderService.simpleMessage("Facebook messenger: "+messengerWebhook.getBody().toString()+" /help",message);
        }
        catch (Exception ex){
            logger.error("Error",ex);
            telegramMessageSenderService.simpleMessage(ex.getMessage(),message);
        }
    }

    @Override
    public void helpGetListOfOrdering(CallBackQuery callBackQuery) {
        String data = TextFormatter.ejectPaySinglePayload(callBackQuery.getData());
        TUser tUser = telegramUserRepositoryService.findByChatId(callBackQuery.getMessage().getChat().getId());
        String uah = ResourceBundle.getBundle("dictionary").getString(CURRENCY.name());
        String getOrder = ResourceBundle.getBundle("dictionary").getString(GETTING_ORDER.name());
        String completeOrder = ResourceBundle.getBundle("dictionary").getString(COMPLETE_ORDERING.name());
        List<CustomerOrdering> customerOrderings = customerOrderingRepositoryService.findAll();
        StringBuilder croissants = new StringBuilder();
        for(CustomerOrdering customerOrdering: customerOrderings) {
            if (customerOrdering.getCourier() == null && data.equals(LIST_OF_ORDERING_DATA.name())) {
                Markup markup = new InlineKeyboardMarkup(Arrays.asList(Arrays.asList(new InlineKeyboardButton(getOrder, GET_ORDER_DATA.name() + "?" + customerOrdering.getId()))));
                getListOfOrderings(callBackQuery,customerOrdering,uah, markup,croissants);
            }
            else if(customerOrdering.getCourier()==tUser && data.equals(LIST_OF_COMPLETE_ORDERING_DATA.name()) && customerOrdering.getCompletedTime()==null){
                Markup markup = new InlineKeyboardMarkup(Arrays.asList(Arrays.asList(new InlineKeyboardButton(completeOrder, COMPLETE_ORDER_DATA.name() + "?" + customerOrdering.getId()))));
                getListOfOrderings(callBackQuery,customerOrdering,uah, markup,croissants);
            }
        }
    }

    @Override
    public void helpCompleteOrderData(CallBackQuery callBackQuery) {
        String orderId = TextFormatter.ejectSingleVariable(callBackQuery.getData());
        CustomerOrdering customerOrdering = customerOrderingRepositoryService.findOne(Long.parseLong(orderId));
        TUser tUser = customerOrdering.getTUser();
        callBackQuery.getMessage().getChat().setId(tUser.getChatId());
        callBackQuery.getMessage().setPlatform(null);
        String text = ResourceBundle.getBundle("dictionary").getString(RECEiVE_ORDER.name());
        telegramMessageSenderService.simpleQuestion(QUESTION_COMPLETE_DATA,"?"+orderId+"&",text,callBackQuery.getMessage());
    }

    private void getListOfOrderings(CallBackQuery callBackQuery, CustomerOrdering customerOrdering, String uah, Markup markup, StringBuilder croissants) {
        croissants.setLength(0);
        for (String orderId : customerOrdering.getCroissants()) {
            long id = Long.parseLong(orderId);
            CroissantEntity croissantEntity = croissantRepositoryService.findOne(id);
            if (croissantEntity.getType().equals(OWN.name())) {
                croissants.append(croissantEntity);
                continue;
            }
            croissants.append(croissantEntity.getName() + "; ");
        }
        String caption = customerOrdering.getId() + ". " + "time: " + customerOrdering.getTime() + "\naddress: " + customerOrdering.getAddress() + "" +
                "\nphone number: " + customerOrdering.getPhoneNumber() + "\n" + croissants + "\n" +
                customerOrdering.getPrice() + uah;
        telegramMessageSenderService.sendPhoto(PICTURE_ORDERING, caption, markup, callBackQuery.getMessage());
    }
}
