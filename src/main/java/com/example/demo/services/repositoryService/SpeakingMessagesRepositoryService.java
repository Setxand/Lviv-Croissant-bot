package com.example.demo.services.repositoryService;

import com.example.demo.entity.SpeakingMessage;

public interface SpeakingMessagesRepositoryService {
	public SpeakingMessage findByKey(String key);

	public SpeakingMessage saveAndFlush(SpeakingMessage speakingMessage);

	public void delete(SpeakingMessage speakingMessage);

}
