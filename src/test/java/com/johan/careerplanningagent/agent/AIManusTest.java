package com.johan.careerplanningagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AIManusTest {

    @Resource
    private AIManus aiManus;

    @Test
    public void run(){
        String userPrompt = "请帮我写一个关于大二想走后端开发路线的一些学习建议和职业规划" +
                "并结合一些网络图片，制定一份详细的约会计划" +
                "并以PDF格式进行输出";
        String answer = aiManus.run(userPrompt);
        Assertions.assertNotNull(answer);
    }
}