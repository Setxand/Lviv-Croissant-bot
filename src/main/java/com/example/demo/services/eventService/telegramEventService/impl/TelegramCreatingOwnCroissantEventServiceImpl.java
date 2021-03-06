package com.example.demo.services.eventService.telegramEventService.impl;

import com.example.demo.entity.lvivCroissants.Croissant;
import com.example.demo.entity.lvivCroissants.CroissantsFilling;
import com.example.demo.entity.peopleRegister.TUser;
import com.example.demo.constcomponent.messengerEnums.types.CroissantsTypes;
import com.example.demo.services.eventService.telegramEventService.TelegramCreatingOwnCroissantEventService;
import com.example.demo.services.eventService.telegramEventService.TelegramGetMenuEventService;
import com.example.demo.services.peopleRegisterService.TelegramUserRepositoryService;
import com.example.demo.services.repositoryService.CroissantRepositoryService;
import com.example.demo.services.repositoryService.CroissantsFillingRepositoryService;
import com.example.demo.services.repositoryService.MenuOfFillingRepositoryService;
import com.example.demo.test.TelegramClientEx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import telegram.Message;

import java.util.ResourceBundle;

import static com.example.demo.constcomponent.messengerEnums.speaking.ServerSideSpeaker.*;
import static com.example.demo.constcomponent.telegramEnums.TelegramUserStatus.GETTING_MENU_STATUS;
import static com.example.demo.constcomponent.telegramEnums.TelegramUserStatus.INPUTTING_FILLINGS_IN_OWN_CROISSANT_STATUS;

@Service
public class TelegramCreatingOwnCroissantEventServiceImpl implements TelegramCreatingOwnCroissantEventService {
	@Autowired
	private TelegramClientEx telegramClient;
	@Autowired
	private TelegramGetMenuEventService telegramGetMenuEventService;
	@Autowired
	private TelegramUserRepositoryService telegramUserRepositoryService;
	@Autowired
	private MenuOfFillingRepositoryService menuOfFillingRepositoryService;
	@Autowired
	private CroissantRepositoryService croissantRepositoryService;
	@Autowired
	private CroissantsFillingRepositoryService croissantsFillingRepositoryService;

	@Override
	public void createOwn(Message message) {
		TUser tUser = telegramUserRepositoryService.findByChatId(message.getChat().getId());
		switch (tUser.getStatus()) {
			case OWN_MENU_STATUS:
				startCreatingOwnCroissant(message, tUser);
				break;
			case INPUTTING_FILLINGS_IN_OWN_CROISSANT_STATUS:
				inputtingFillingsIfOnwCroissant(message, tUser);
				break;
			default:
				telegramClient.errorMessage(message);
				break;
		}


	}

	private void inputtingFillingsIfOnwCroissant(Message message, TUser tUser) {
		String text = message.getText();
		Croissant croissant = new Croissant();
		croissant.setImageUrl("http://www.mlinar.hr/wp-content/uploads/2013/06/mlinar-proizvodi-croissant-600x380.png");
		croissant.setName(ResourceBundle.getBundle("dictionary").getString(OWN_CROISSANT.name()));
		croissant.setType(CroissantsTypes.OWN.name());
		croissant = croissantRepositoryService.saveAndFlush(croissant);
		try {
			String[] fillings = text.split(",");
			for (String filling : fillings) {
				CroissantsFilling croissantsFilling = new CroissantsFilling(menuOfFillingRepositoryService.findOne(Long.parseLong(filling)));
				croissant.addSingleFilling(croissantsFilling);
				croissant.setPrice(croissant.getPrice() + croissantsFilling.getPrice());
				croissantsFillingRepositoryService.saveAndFlush(croissantsFilling);
			}
			tUser.addCroissant(croissant);
			telegramUserRepositoryService.saveAndFlush(tUser);
			croissantRepositoryService.saveAndFlush(croissant);
			finalCreating(message, tUser);

		} catch (HttpClientErrorException hEx) {
			hEx.printStackTrace();
			telegramUserRepositoryService.changeStatus(tUser, null);
		} catch (Exception ex) {
			ex.printStackTrace();
			telegramClient.simpleMessage(ResourceBundle.getBundle("dictionary").getString(NON_CORRECT_FORMAT_OF_FILLING.name()), message);
			telegramClient.simpleMessage(ResourceBundle.getBundle("dictionary").getString(ID_OF_FILLING.name()), message);
		}


	}

	private void finalCreating(Message message, TUser tUser) {
		telegramClient.simpleMessage(ResourceBundle.getBundle("dictionary").getString(CREATED_CROISSANT.name()), message);
		telegramUserRepositoryService.changeStatus(tUser, GETTING_MENU_STATUS);
		message.setText(CroissantsTypes.OWN.name());
		telegramGetMenuEventService.getMenu(message);
	}

	private void startCreatingOwnCroissant(Message message, TUser tUser) {
		telegramClient.simpleMessage("Here you can to create your own croissant!!!", message);
		telegramGetMenuEventService.getMenuOfFillings(message);
		telegramClient.simpleMessage(ResourceBundle.getBundle("dictionary").getString(ID_OF_FILLING.name()), message);
		telegramUserRepositoryService.changeStatus(tUser, INPUTTING_FILLINGS_IN_OWN_CROISSANT_STATUS);
	}
}
