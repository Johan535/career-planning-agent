package com.johan.careerplanningagent.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * AI职业规划智能体，拥有自主规划能力
 */
@Component
public class AIManus extends ToolCallAgent {
    public AIManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("xiaogaManus");
        String systemPrompt = """
                You are YuManus, an all-capable AI assistant, aimed at solving any task presented by the user.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.
                When external tools fail or return insufficient data, continue solving the user's task using your own knowledge.
                Always provide a clear, structured final answer in Chinese unless the user asks otherwise.
                """;
        String nextStepPrompt = """
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                For complex tasks, break down the problem and use different tools step by step only when tools are truly useful.
                After using each tool, clearly explain the execution results and continue producing a useful answer.
                If you want to stop the interaction at any point, use the `terminate` tool/function call.
                """;
        this.setSystemPrompt(systemPrompt + "\n" + nextStepPrompt);
        this.setMaxSteps(3);

        ChatClient chatClient = ChatClient.builder(dashscopeChatModel).build();
        this.setChatClient(chatClient);
    }
}