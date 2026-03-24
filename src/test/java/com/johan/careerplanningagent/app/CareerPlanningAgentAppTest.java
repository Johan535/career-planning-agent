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
    @Test
    void doChatWithTools() {

        testMessage("周末想去几家公司实习，推荐几个适合大二学生的公司？");

        testMessage("最近被公司炒了，看看编程导航网站（https://www.nowcoder.com/）的其他人是怎么解决矛盾的？");

        testMessage("直接下载一张适合做手机壁纸的程序员图片为文件");

        testMessage("执行 Python3 脚本来生成数据分析报告");

        testMessage("保存我的职业规划档案为文件");

        testMessage("生成一份‘大二到就业的规划指导’PDF，包含何时找实习，需要培养什么硬核技能");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = careerPlanningAgentApp.doChatWithTool(message, chatId);
        Assertions.assertNotNull(answer);
    }

}