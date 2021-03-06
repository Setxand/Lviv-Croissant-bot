package com.example.demo.services.peopleRegisterService;

import com.example.demo.entity.peopleRegister.TUser;
import com.example.demo.constcomponent.telegramEnums.TelegramUserStatus;

import java.util.List;

public interface TelegramUserRepositoryService {
	public void delete(TUser tUser);

	public TUser saveAndFlush(TUser tUser);

	public TUser findByChatId(Integer chatId);

	public void changeStatus(TUser user, TelegramUserStatus status);

	public TUser findByUserName(String userName);

	public List<String> findTopUserNames();
}
