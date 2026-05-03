package com.johan.careerplanningagent.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.johan.careerplanningagent.agent.model.AgentState;
import lombok.EqualsAndHashCode;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
public class ToolCallAgent extends ReActAgent {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ToolCallAgent.class);

    private final ToolCallback[] availableTools;
    private final ToolCallingManager toolCallingManager;
    private final ChatOptions chatOptions;
    private ChatResponse toolCallChatResponse;
    private String lastAssistantText;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = DashScopeChatOptions.builder()
                .withInternalToolExecutionEnabled(false)
                .build();
    }

    @Override
    public boolean think() {
        if (StrUtil.isNotBlank(getNextPrompt())) {
            getMessageList().add(new UserMessage(getNextPrompt()));
        }

        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        try {
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(availableTools)
                    .call()
                    .chatResponse();

            this.toolCallChatResponse = chatResponse;
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            this.lastAssistantText = assistantMessage.getText();
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();

            log.info("{}的思考: {}", getName(), lastAssistantText);
            log.info("{}选择了 {} 个工具来使用", getName(), toolCallList.size());
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称：%s，参数：%s", toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);

            getMessageList().add(assistantMessage);
            if (toolCallList.isEmpty()) {
                setState(AgentState.FINISHED);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("{}的思考过程遇到了问题: {}", getName(), e.getMessage(), e);
            this.lastAssistantText = "处理时遇到错误: " + e.getMessage();
            getMessageList().add(new AssistantMessage(lastAssistantText));
            setState(AgentState.FINISHED);
            return false;
        }
    }

    @Override
    public String act() {
        if (toolCallChatResponse == null || !toolCallChatResponse.hasToolCalls()) {
            return lastAssistantText == null || lastAssistantText.isBlank() ? "没有工具调用" : lastAssistantText;
        }

        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        setMessageList(toolExecutionResult.conversationHistory());

        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        String results = toolResponseMessage.getResponses().stream()
                .map(response -> "工具 " + response.name() + " 完成了它的任务。结果: " + response.responseData())
                .collect(Collectors.joining("\n"));

        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> "doTerminate".equals(response.name()));
        if (terminateToolCalled) {
            setState(AgentState.FINISHED);
        }

        String finalAnswer = getChatClient().prompt(new Prompt(getMessageList(), chatOptions))
                .system(getSystemPrompt() + "\n请基于以上工具执行结果，直接给用户一份完整、结构化、可执行的最终回答。如果工具失败，不要停在错误上，请用已有知识继续完成任务。")
                .call()
                .content();
        log.info(results);
        log.info("{}的最终回答: {}", getName(), finalAnswer);
        setState(AgentState.FINISHED);
        return finalAnswer == null || finalAnswer.isBlank() ? results : finalAnswer;
    }
}