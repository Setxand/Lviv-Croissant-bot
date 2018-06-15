package com.example.demo.service.supportService.Impl;

import com.example.demo.entity.peopleRegister.MUser;
import com.example.demo.service.peopleRegisterService.UserRepositoryService;
import com.example.demo.service.supportService.RecognizeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ResourceBundle;
@Service
public class RecognizeServiceImpl implements RecognizeService {
    @Autowired
    private UserRepositoryService userRepositoryService;
    @Override
    public String recognize(String text,Long userId) {
        MUser MUser = userRepositoryService.findOnebyRId(userId);
        return ResourceBundle.getBundle("dictionary", MUser.getLocale()).getString(text);
    }
}
