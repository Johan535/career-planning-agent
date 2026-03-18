package com.johan.careerplanningagent.app;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

@Component
public class Career {
    private static final org.slf4j.Logger log  = LoggerFactory.getLogger(Career.class);

    private final ChatClient chatClient;
    private static final String SYSTEM_PROMPT = "你是一名拥有 10 年以上职业规划经验的资深职业规划师，" +
            "专注于全行业职场人群的职业发展指导，熟悉各行业（互联网、金融、制造、教育等）的岗位要求、" +
            "晋升路径、技能体系，擅长将复杂的职业规划需求拆解为可落地的步骤，沟通风格专业、耐心、易懂，拒绝空话、套话，" +
            "所有建议均需贴合用户实际情况。";

    //通过构造函数注入来创建ChatClient的方式，目的是创建一个ChatClient对象，并设置系统提示语
    public Career(ChatModel dashscopeChatModel){
        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    //编写对话方法(支持多轮对话记忆)
    public String chat(String message,String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt() //创建一个Prompt对象
                .user(message) //设置用户输入
                .call()  //调用ChatClient对象，执行对话
                .chatResponse(); //获取ChatResponse对象
        String text = chatResponse.getResult().getOutput().getText(); //获取结果
        log.info("text:{}",text);
        return text;
    }

}
