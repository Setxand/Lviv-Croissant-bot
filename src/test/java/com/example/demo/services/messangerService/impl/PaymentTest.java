package com.example.demo.services.messangerService.impl;

import com.example.demo.DemoApplicationTests;
import com.example.demo.model.messanger.Button;
import com.example.demo.services.messangerService.MessageSenderService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.example.demo.constcomponent.messengerEnums.types.ButtonType.web_url;

public class PaymentTest extends DemoApplicationTests {
    List<Button> buttons;
    @Value("${server.url}")
    private String SERVER_URL;
    @Mock
    RestTemplate restTemplate;
    @Autowired
    MessageSenderService messageSenderService;
    @Before
    public void setUp(){
        buttons = new ArrayList<>();
        Button button = new Button(web_url.name(),"payment");
        button.setMesExtentions(true);
        button.setUrl(SERVER_URL+"/payment");
        buttons.add(button);

    }


    @Test
    public void paymentTesting(){
        messageSenderService.sendButtons(buttons,"payment",userId);
        logger.info("payment button has bet sent");
        HttpHeaders httpHeaders = new HttpHeaders();


    }
}
