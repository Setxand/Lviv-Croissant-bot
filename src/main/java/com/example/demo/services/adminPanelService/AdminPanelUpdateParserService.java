package com.example.demo.services.adminPanelService;

import com.example.demo.models.telegram.Update;

public interface AdminPanelUpdateParserService {
    public  void parseUpdate(Update update);
}