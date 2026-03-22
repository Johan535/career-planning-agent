package com.johan.careerplanningagent.rag;


import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.stereotype.Component;

/*
* 创建一个上下文查询增强的工厂
* */
@Component
public class CareerPlanningAgentContextualQueryAugmenterFactory {

    //创建一个上下文查询增强
    public static ContextualQueryAugmenter createInstance(){
        PromptTemplate emptyContextPromptTemplate = new PromptTemplate(
                """
                        你应该输出下面的内容：
                        抱歉，我只能回答恋爱相关的问题，别的没办法帮到您哦
                        有问题可以联系职业规划导师客服 https://blog.csdn.net/xwhxy?type=blog      
                         """
        );

        //返回上下文查询增强
        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(false) //禁止空上下文
                .emptyContextPromptTemplate(emptyContextPromptTemplate) //设置空上下文提示
                .build();
    }
}
