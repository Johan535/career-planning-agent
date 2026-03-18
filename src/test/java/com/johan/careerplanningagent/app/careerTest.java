package com.johan.careerplanningagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class careerTest {


    @Resource
    private Career career;


    @Test
    void chat() {
        String chatId = UUID.randomUUID().toString();

        //第一轮测试
        String message ="你好，我是 johan";
        String answer = career.chat(message,chatId);
        //第二轮测试
        String message2 = "我叫 johan，现在在哪工作";
        String answer2 = career.chat(message2,chatId);
        Assertions.assertNotNull(answer2);
        //第三轮测试
        String message3 = "现在在哪工作";
        String answer3 = career.chat(message3,chatId);
        Assertions.assertNotNull(answer3);
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message ="你好，我是 johan,我想要了解一下跟电子商务有关的职业种类，但是我不知道有哪些";
        career.doChatWithReport(message,chatId);
    }
}