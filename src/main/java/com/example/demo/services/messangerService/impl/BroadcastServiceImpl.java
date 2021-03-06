package com.example.demo.services.messangerService.impl;

import com.example.demo.controller.TestController;
import com.example.demo.model.messanger.Message;
import com.example.demo.model.messanger.broadcast.*;
import com.example.demo.services.messangerService.BroadcastService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class BroadcastServiceImpl implements BroadcastService {
	private static final Logger logger = Logger.getLogger(BroadcastServiceImpl.class);
	@Autowired
	TestController testController;
	@Value("${broadcast.message.creatives}")
	private String BROADCAST_MESSAGE_CREATIVES;
	@Value("${broadcast.message}")
	private String BROADCAST_MESSAGE;
	@Value("${page.access.token}")
	private String PAGE_ACCESS_TOKEN;
	@Value("${domain.broadcast}")
	private String DOMAIN_BROADCAST;
	@Value("${broadcast.message.sent}")
	private String BROADCAST_MESSAGE_SENT;
	@Value("${broadcast.label}")
	private String BROADCAST_LABEL;
	@Value("${custom.label}")
	private String CUSTOM_LABEL;
	@Value(("${estimation}"))
	private String ESTIMATION_URL;
	@Value("${token}")
	private String TOKEN_ARG;

	@Override
	public Long createBroadCastMessage(List<Message> messages) {
		String url = BROADCAST_MESSAGE_CREATIVES + PAGE_ACCESS_TOKEN;
		BroadcastRequest broadcastRequest = new BroadcastRequest();
		broadcastRequest.setMessages(messages);

		ResponseEntity<BroadcastRequest> response = new RestTemplate()
				.postForEntity(url, broadcastRequest, BroadcastRequest.class);
		logger.debug("Broadcast message successfully has been created...");
		return response.getBody().getMessageCreativeId();
	}

	@Override
	public Long sendBroadCastMessage(BroadcastMessage broadcastMessage) {
		String url = BROADCAST_MESSAGE + PAGE_ACCESS_TOKEN;

		ResponseEntity<BroadcastRequest> response = new RestTemplate()
				.postForEntity(url, broadcastMessage, BroadcastRequest.class);
		logger.debug("B. message successfully has been sent...");

		return response.getBody().getBroadcastId();
	}

	@Override
	public List<Data> totalUsersNumber(Long broadcastId) {
		String url = DOMAIN_BROADCAST + broadcastId + BROADCAST_MESSAGE_SENT + PAGE_ACCESS_TOKEN;

		ResponseEntity<BroadcastRequest> response = new RestTemplate().getForEntity(url, BroadcastRequest.class);
		logger.debug("Request 'totalUsersNumber' successfully has been sent");
		return response.getBody().getData();
	}

	@Override
	public Long createCustomLabel(String labelName) {
		String url = BROADCAST_LABEL + PAGE_ACCESS_TOKEN;
		CustomLabel customLabel = new CustomLabel(labelName);
		ResponseEntity<CustomLabel> response = new RestTemplate()
				.postForEntity(url, customLabel, CustomLabel.class);
		logger.debug("Custom label successfully has been created...");
		return response.getBody().getId();
	}

	@Override
	public void associateCustomLabel(Long userId, Long customLabelId) {
		String url = DOMAIN_BROADCAST + customLabelId + "/label?access_token=" + PAGE_ACCESS_TOKEN;
		CustomLabel customLabel = new CustomLabel();
		customLabel.setUserId(userId);
		new RestTemplate().postForObject(url, customLabel, Void.class);

		logger.debug("associate request successfully has been sent");
	}

	@Override
	public void deleteCustomLabelFromUserId(Long customLabelId, Long userId) {
		String url = DOMAIN_BROADCAST + customLabelId + "/label?access_token=" + PAGE_ACCESS_TOKEN;
		CustomLabel customLabel = new CustomLabel();
		customLabel.setId(userId);
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
		HttpEntity<CustomLabel> httpEntity = new HttpEntity<>(customLabel, httpHeaders);
		new RestTemplate().exchange(url, HttpMethod.DELETE, httpEntity, Void.class);
		logger.debug("Custom label from user id was separated...");
	}

	@Override
	public List<Data> retrieveAssociateLabels(Long userId) {
		String url = DOMAIN_BROADCAST + userId + "/custom_labels?access_token=" + PAGE_ACCESS_TOKEN;
		ResponseEntity<BroadcastRequest> response = new RestTemplate().getForEntity(url, BroadcastRequest.class);
		logger.debug("Associate labels was retrieved...");
		return response.getBody().getData();
	}

	@Override
	public Data retrieveLabelDetails(Long customLabelId) {
		String url = DOMAIN_BROADCAST + customLabelId + "?fields=name&access_token=" + PAGE_ACCESS_TOKEN;
		ResponseEntity<Data> response = new RestTemplate().getForEntity(url, Data.class);
		logger.debug("the details of current label was retrieved...");
		return response.getBody();
	}

	@Override
	public List<Data> retrieveListOfLabels() {
		String url = DOMAIN_BROADCAST + "me/custom_labels?fields=name&access_token=" + PAGE_ACCESS_TOKEN;
		ResponseEntity<BroadcastRequest> response = new RestTemplate().getForEntity(url, BroadcastRequest.class);
		logger.debug("List of labels was retrieved...");
		return response.getBody().getData();
	}

	@Override
	public void deleteLabel(Long customLabelId) {
		String url = DOMAIN_BROADCAST + customLabelId + TOKEN_ARG + PAGE_ACCESS_TOKEN;
		String labelName = retrieveLabelDetails(customLabelId).getName();
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.delete(url);

		logger.debug("Label :" + labelName + " was deleted...");
	}

	@Override
	public String estimateTheReach(Long customLabelId) {
		String url = ESTIMATION_URL + PAGE_ACCESS_TOKEN;
		BroadcastRequest broadcastRequest = new BroadcastRequest();
		broadcastRequest.setCustomLabelId(customLabelId);
		ResponseEntity<BroadcastRequest> response = new RestTemplate().postForEntity(url, broadcastRequest, BroadcastRequest.class);
		logger.debug("Estimation id successfully has been received");
		return response.getBody().getReachEstimationId();
	}

	@Override
	public EstimationReach retrieveRichEstimate(String estimationId) {
		String url = DOMAIN_BROADCAST + estimationId + TOKEN_ARG + PAGE_ACCESS_TOKEN;
		ResponseEntity<EstimationReach> response = new RestTemplate().getForEntity(url, EstimationReach.class);
		logger.debug("func 'retrieveRichEstimate' finished!");
		return response.getBody();
	}


}
