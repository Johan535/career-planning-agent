package com.johan.careerplanningagent.demo.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/*
* 利用Spring AI 框架调用AI大模型
*
* */
@Component
public class SpringAiInvoke implements CommandLineRunner {

    @Resource
    private ChatModel dashcopeChatModel;


    @Override
    public void run(String... args) throws Exception {
        AssistantMessage output = dashcopeChatModel.call(new Prompt("你好，我是johan"))
                .getResult()
                .getOutput();
        System.out.println(output.getText());
    }
}
