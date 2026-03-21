package com.johan.careerplanningagent.app;

import cn.hutool.core.lang.UUID;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CareerPlanningAgentAppTest {

    @Resource
    private  CareerPlanningAgentApp careerPlanningAgentApp;

    @Test
    void doChatWithReport() {
        String chatId = UUID.fastUUID().toString();
        String message = "我想要了解一下新手小白如何在职场生存";
        String answer = String.valueOf(careerPlanningAgentApp.doChatWithReport(message,chatId));
        Assertions.assertNotNull(answer);

    }
}