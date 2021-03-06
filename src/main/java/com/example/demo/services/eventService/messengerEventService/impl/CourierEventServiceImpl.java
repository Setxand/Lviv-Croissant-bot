package com.example.demo.services.eventService.messengerEventService.impl;

import com.example.demo.entity.lvivCroissants.CustomerOrdering;
import com.example.demo.entity.peopleRegister.CourierRegister;
import com.example.demo.entity.peopleRegister.User;
import com.example.demo.model.messanger.*;
import com.example.demo.services.eventService.messengerEventService.CourierEventService;
import com.example.demo.services.eventService.messengerEventService.UserEventService;
import com.example.demo.services.messangerService.MessageSenderService;
import com.example.demo.services.peopleRegisterService.CourierRegisterService;
import com.example.demo.services.peopleRegisterService.UserRepositoryService;
import com.example.demo.services.repositoryService.CustomerOrderingRepositoryService;
import com.example.demo.services.supportService.RecognizeService;
import com.example.demo.services.supportService.TextFormatter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.example.demo.constcomponent.messengerEnums.Cases.COMPLETE_ORDERINGS_LIST;
import static com.example.demo.constcomponent.messengerEnums.Cases.ORDERING_LIST_FILLING;
import static com.example.demo.constcomponent.messengerEnums.CasesCourierActions.COMPLETING_ORDERINGS;
import static com.example.demo.constcomponent.messengerEnums.CasesCourierActions.GET_LIST_OF_ORDERING;
import static com.example.demo.constcomponent.messengerEnums.payloads.Payloads.*;
import static com.example.demo.constcomponent.messengerEnums.payloads.QuickReplyPayloads.COURIER_QUESTION_PAYLOAD;
import static com.example.demo.constcomponent.messengerEnums.payloads.QuickReplyPayloads.ONE_MORE_ACTION_COURIER_PAYLOAD;
import static com.example.demo.constcomponent.messengerEnums.speaking.ServerSideSpeaker.*;
import static com.example.demo.constcomponent.messengerEnums.types.AttachmentType.template;
import static com.example.demo.constcomponent.messengerEnums.types.ButtonType.postback;
import static com.example.demo.constcomponent.messengerEnums.types.ButtonType.web_url;
import static com.example.demo.constcomponent.messengerEnums.types.TemplateType.generic;

@Service
public class CourierEventServiceImpl implements CourierEventService {

	private static final org.apache.log4j.Logger logger = Logger.getLogger(CourierEventServiceImpl.class);
	@Autowired
	private CourierRegisterService courierRegisterService;
	@Autowired
	private MessageSenderService messageSenderService;
	@Autowired
	private CustomerOrderingRepositoryService customerOrderingRepositoryService;
	@Autowired
	private RecognizeService recognizeService;
	@Autowired
	private UserRepositoryService userRepositoryService;
	@Autowired
	private UserEventService userEventService;
	@Value("${server.url}")
	private String SERVER_URL;

	@Override
	public void parseCourier(Messaging messaging) {
		try {


			if (messaging.getMessage().getQuickReply() != null) {
				QuickReply quickReply = messaging.getMessage().getQuickReply();
				String payload = quickReply.getPayload().toUpperCase();

				if (payload.equals(GET_LIST_OF_ORDERING.name())) {
					getOrderingList(messaging);
				} else if (payload.equals(COMPLETING_ORDERINGS.name())) {
					completeOrderings(messaging);

				} else if (TextFormatter.ejectPaySinglePayload(quickReply.getPayload()).equals(COURIER_QUESTION_PAYLOAD.name())) {
					courierRegisterCreator(messaging);
				}


			} else if (messaging.getPostback() != null) {
				String postBackPayload = messaging.getPostback().getPayload();
				String payload = TextFormatter.ejectContext(postBackPayload);
				if (payload.equals(GET_LIST_OF_ORDERING.name()) || payload.equals(ORDERING_LIST_FILLING.name())) {
					messaging.getMessage().setQuickReply(new QuickReply());
					messaging.getMessage().getQuickReply().setPayload(" ");
					getOrderingList(messaging);
				}
			} else {
				CourierRegister courierRegister = courierRegisterService.findByRecipientId(messaging.getSender().getId());
				if (courierRegister.getName() == null) {
					parseName(messaging, courierRegister);
				} else if (courierRegister.getPhoneNumber() == null) {
					parsePhoneNumber(messaging, courierRegister);
				}


			}
		} catch (Exception ex) {
			logger.warn(ex);
			User user = userRepositoryService.findOnebyRId(messaging.getSender().getId());
			user.setStatus(null);
			userRepositoryService.saveAndFlush(user);
			messageSenderService.errorMessage(messaging.getSender().getId());
			if (courierRegisterService.findTop().getPhoneNumber() == null)
				courierRegisterService.remove(courierRegisterService.findTop());
		}
	}

	@Override
	public void completeOrderingsFinalize(Messaging messaging, Long orderId) {
		try {
			CourierRegister courierRegister = courierRegisterService.findByRecipientId(messaging.getSender().getId());


			CustomerOrdering customerOrdering = customerOrderingRepositoryService.findOne(orderId);
			courierRegister.getCustomerOrderings().remove(customerOrdering);
			customerOrdering.setCourierRegister(null);
			courierRegisterService.saveAndFlush(courierRegister);
			User user = userRepositoryService.findOnebyRId(messaging.getSender().getId());
			user.getCustomerOrderings().remove(customerOrdering);
			userRepositoryService.saveAndFlush(user);
			customerOrderingRepositoryService.delete(customerOrdering);


			messageSenderService.sendSimpleMessage(recognizeService.recognize(DONE.name(), messaging.getSender().getId()), messaging.getSender().getId());

			askOneMoreAction(messaging.getSender().getId());
		} catch (Exception ex) {
			logger.warn(ex);
		}
	}

	@Override
	public void getOrderingList(Messaging messaging) {


		if (!customerOrderingRepositoryService.findAll().isEmpty()) {
			getOrderingListFirstStepIsNotEmpty(messaging);

		} else {
			userEventService.changeStatus(messaging, null);
			messageSenderService.sendSimpleMessage(recognizeService.recognize(EMPTY_LIST.name(), messaging.getSender().getId()), messaging.getSender().getId());
			askOneMoreAction(messaging.getSender().getId());
		}

	}

	@Override
	public void orderingFilling(Messaging messaging, Long orderId) {
		try {


			CourierRegister courierRegister = courierRegisterService.findByRecipientId(messaging.getSender().getId());


			CustomerOrdering customerOrdering = customerOrderingRepositoryService.findOne(orderId);
			courierRegister.addOne(customerOrdering);
			courierRegisterService.saveAndFlush(courierRegister);
			customerOrderingRepositoryService.saveAndFlush(customerOrdering);

			messageSenderService.sendSimpleMessage(recognizeService.recognize(ADDED_TO_DB.name(), messaging.getSender().getId()), messaging.getSender().getId());
			userEventService.changeStatus(messaging, null);
			askOneMoreAction(messaging.getSender().getId());

		} catch (Exception ex) {
			logger.warn(ex);

		}

	}

	private void completeOrderings(Messaging messaging) {
		CourierRegister courierRegister = courierRegisterService.findByRecipientId(messaging.getSender().getId());

		if (!courierRegister.getCustomerOrderings().isEmpty()) {

			if (!messaging.getMessage().getQuickReply().getPayload().equals(COMPLETE_ORDERINGS_LIST.name())) {
				getOwnOrderingList(messaging);
			}
		} else {
			messageSenderService.sendSimpleMessage(recognizeService.recognize(EMPTY_LIST.name(), messaging.getSender().getId()), messaging.getSender().getId());
			userEventService.changeStatus(messaging, null);

			askOneMoreAction(messaging.getSender().getId());
		}

	}

	private void getOwnOrderingList(Messaging messaging) {
		CourierRegister courierRegister = courierRegisterService.findByRecipientId(messaging.getSender().getId());
		if (!courierRegister.getCustomerOrderings().isEmpty()) {
			messageSenderService.sendMessage(makeGeneric(messaging, courierRegister.getCustomerOrderings()));

		} else {
			messageSenderService.sendSimpleMessage(recognizeService.recognize(EMPTY_LIST.name(), messaging.getSender().getId()), messaging.getSender().getId());

		}
		userEventService.changeStatus(messaging, null);
	}

	private void getOrderingListFirstStepIsNotEmpty(Messaging messaging) {
		List<CustomerOrdering> customerOrderings = customerOrderingRepositoryService.findAll();
		List<CustomerOrdering> customerOrderings1 = new ArrayList<>();
		for (CustomerOrdering customerOrdering : customerOrderings) {
			if (customerOrdering.getCourierRegister() == null) {
				customerOrderings1.add(customerOrdering);
			}
		}

		if (customerOrderings1.isEmpty()) {
			getOrderingListFirstStepIsEmpty(messaging);

		} else if (!messaging.getMessage().getQuickReply().getPayload().equals(ORDERING_LIST_FILLING.name())) {
			messageSenderService.sendMessage(makeGeneric(messaging, customerOrderings1));
			userEventService.changeStatus(messaging, null);
		}
	}

	private Messaging makeGeneric(Messaging messaging, List<CustomerOrdering> customerOrderings1) {
		Attachment attachment = new Attachment(template.name(), new Payload());
		List<Element> elements = new ArrayList<>();
		attachment.getPayload().setElements(elements);
		attachment.getPayload().setTemplateType(generic.name());
		List<CustomerOrdering> subList;
		int index = 0;
		if (messaging.getPostback() != null) {
			index = Integer.parseInt(TextFormatter.ejectVariableWithContext(messaging.getPostback().getPayload()));
		}
		try {
			subList = customerOrderings1.subList(index, index + 10);
			index += 9;
		} catch (Exception ex) {
			subList = customerOrderings1.subList(index, customerOrderings1.size());
			index += (customerOrderings1.size() - index - 1);
		}

		for (CustomerOrdering customerOrdering : subList) {
			Element element = new Element();
			if (customerOrdering == subList.get(subList.size() - 1) && customerOrdering != customerOrderings1.get(customerOrderings1.size() - 1)) {
				Button button = new Button(postback.name(), "Show more", SHOW_MORE_PAYLOAD.name() + "?" + messaging.getMessage().getQuickReply().getPayload() + "&" + index);
				element.getButtons().add(button);
				element.setTitle("End of the list");
				elements.add(element);

				break;
			}
			element.setTitle("Замовлення №" + customerOrdering.getId() + ", Ім'я:" + customerOrdering.getName());
			element.setSubtitle(" Адреса:" + customerOrdering.getAddress() + "\n номeр: " + customerOrdering.getPhoneNumber());
			Button openViewButton = new Button(web_url.name(), recognizeService.recognize(OPEN_ORDER.name(), messaging.getSender().getId()));
			openViewButton.setMesExtentions(true);
			openViewButton.setUrl(SERVER_URL + "/showMore/" + customerOrdering.getId());
			openViewButton.setHeightRatio("tall");
			element.getButtons().add(openViewButton);
			Button courierButton;
			if (messaging.getMessage().getQuickReply().getPayload().equals(GET_LIST_OF_ORDERING.name()))
				courierButton = new Button(postback.name(), recognizeService.recognize(GETTING_ORDER.name(), messaging.getSender().getId()), GET_ORDER.name() + "?" + customerOrdering.getId());
			else
				courierButton = new Button(postback.name(), recognizeService.recognize(COMPLETING_ORDER.name(), messaging.getSender().getId()), COMPLETE_ORDER.name() + "?" + customerOrdering.getId());

			element.getButtons().add(courierButton);
			elements.add(element);

		}
		Messaging newMes = new Messaging(new Message(), new Recipient(messaging.getSender().getId()));
		newMes.getMessage().setAttachment(attachment);
		return newMes;
	}

	private void getOrderingListFirstStepIsEmpty(Messaging messaging) {
		messageSenderService.sendSimpleMessage(recognizeService.recognize(EMPTY_LIST.name(), messaging.getSender().getId()), messaging.getSender().getId());
		askOneMoreAction(messaging.getSender().getId());
		User user = userRepositoryService.findOnebyRId(messaging.getSender().getId());
		user.setStatus(null);
		userRepositoryService.saveAndFlush(user);
	}

	private void parsePhoneNumber(Messaging messaging, CourierRegister courierRegister) {
		if (TextFormatter.isPhoneNumber(messaging.getMessage().getText())) {
			courierRegister.setPhoneNumber(messaging.getMessage().getText());
			courierRegisterService.saveAndFlush(courierRegister);
			messageSenderService.sendSimpleMessage(recognizeService.recognize(ADDED_TO_DB.name(), messaging.getSender().getId()), messaging.getSender().getId());
			User user = userRepositoryService.findOnebyRId(messaging.getSender().getId());
			user.setStatus(null);
			userRepositoryService.saveAndFlush(user);
		} else {
			messageSenderService.sendSimpleMessage(recognizeService.recognize(NON_CORRECT_FORMAT_OF_NUMBER_OF_TELEPHONE.name(), messaging.getSender().getId()), messaging.getSender().getId());
			messageSenderService.sendSimpleMessage(recognizeService.recognize(NUMBER_OF_PHONE.name(), messaging.getSender().getId()), messaging.getSender().getId());

		}
	}


	private void parseName(Messaging messaging, CourierRegister courierRegister) {
		courierRegister.setName(messaging.getMessage().getText());
		courierRegisterService.saveAndFlush(courierRegister);
		messageSenderService.sendSimpleMessage(recognizeService.recognize(NUMBER_OF_PHONE.name(), messaging.getSender().getId()), messaging.getSender().getId());

	}

	private void courierRegisterCreator(Messaging messaging) {
		CourierRegister courierRegister = new CourierRegister();
		courierRegister.setRecipientId(messaging.getSender().getId());
		courierRegisterService.saveAndFlush(courierRegister);
		messageSenderService.sendSimpleMessage(recognizeService.recognize(NAME_LASTNAME.name(), messaging.getSender().getId()), messaging.getSender().getId());
	}


	private void askOneMoreAction(Long recipient) {
		messageSenderService.sendSimpleQuestion(recipient, recognizeService.recognize(ONE_MORE_ACTION_COURIER.name(), recipient), ONE_MORE_ACTION_COURIER_PAYLOAD.name(), "?");
	}
}
