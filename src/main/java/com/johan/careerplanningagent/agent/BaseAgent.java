package com.johan.careerplanningagent.agent;


import com.itextpdf.styledxmlparser.jsoup.internal.StringUtil;
import com.johan.careerplanningagent.agent.model.AgentState;
import com.johan.careerplanningagent.manus.ManusSseContext;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/*
 * 抽象基础代理类，用于管理代理状态和执行流程
 *
 * 提供状态转换，内存管理和基于步骤的执行循环的基础功能
 * 子类必须实现step方法
 * */
public abstract class BaseAgent {
    private static final org.slf4j.Logger log  = LoggerFactory.getLogger(BaseAgent.class);
    //核心属性
    private String name;

    //提示词
    private  String systemPrompt;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getNextPrompt() {
        return nextPrompt;
    }

    public void setNextPrompt(String nextPrompt) {
        this.nextPrompt = nextPrompt;
    }

    public AgentState getState() {
        return state;
    }

    public void setState(AgentState state) {
        this.state = state;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    public ChatClient getChatClient() {
        return chatClient;
    }

    public void setChatClient(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public List<Message> getMessageList() {
        return messageList;
    }

    public void setMessageList(List<Message> messageList) {
        this.messageList = messageList;
    }

    private  String nextPrompt;

    //代理状态
    private AgentState state = AgentState.IDLE;

    //执行步骤控制
    private int currentStep = 0;
    private int maxSteps = 10;

    // LLM大模型
    private ChatClient chatClient;

    //Memory 记忆（需要自主维护会话上下文）
    private List<Message> messageList = new ArrayList<>();

    /**
     *运行代理
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public String run(String userPrompt) {
        //基础校验
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }
        if (StringUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }
        //执行，更改状态
        state = AgentState.RUNNING;
        //记录消息上下文
        messageList.add(new UserMessage(userPrompt));
        //执行循环
        List<String> results = new ArrayList<>();
        try {
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step " + stepNumber + "/" + maxSteps);
                //单步执行
                String stepResult = step();
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);
            }
            //检查是否超出步骤限制
            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            }
            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("Error executing agent", e);
            return "执行错误" + e.getMessage();
        } finally {
            //清理资源
            this.cleanup();
        }
    }


    /**
     * 运行代理（流式输出），默认匿名会话。
     */
    public SseEmitter runStream(String userPrompt) {
        return runStream(userPrompt, "anonymous");
    }

    /**
     * 运行代理（流式输出）。
     *
     * @param sessionKey 与前端 chatId 对齐，供工具注册下载归属（如 PDF）。
     */
    public SseEmitter runStream(String userPrompt, String sessionKey) {

        SseEmitter emitter = new SseEmitter(300000L);

        CompletableFuture.runAsync(() -> {
            try {
                if (this.state != AgentState.IDLE) {
                    try {
                        emitter.send("错误：无法从状态运行代理: " + this.state);
                    } catch (IOException ignored) {
                        // ignore
                    }
                    emitter.complete();
                    return;
                }
                if (StringUtil.isBlank(userPrompt)) {
                    try {
                        emitter.send("错误：不能使用空提示词运行代理");
                    } catch (IOException ignored) {
                        // ignore
                    }
                    emitter.complete();
                    return;
                }

                ManusSseContext.open(sessionKey, emitter);
                state = AgentState.RUNNING;
                messageList.add(new UserMessage(userPrompt));

                try {
                    for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                        int stepNumber = i + 1;
                        currentStep = stepNumber;
                        log.info("Executing step " + stepNumber + "/" + maxSteps);

                        String stepResult = step();
                        String result = "Step " + stepNumber + ": " + stepResult;
                        try {
                            emitter.send(result);
                        } catch (IOException io) {
                            throw new RuntimeException(io);
                        }
                    }

                    if (currentStep >= maxSteps) {
                        state = AgentState.FINISHED;
                        try {
                            emitter.send("执行结束: 达到最大步骤 (" + maxSteps + ")");
                        } catch (IOException io) {
                            throw new RuntimeException(io);
                        }
                    }

                    emitter.complete();
                } catch (Exception e) {
                    state = AgentState.ERROR;
                    log.error("执行智能体失败", e);
                    try {
                        try {
                            emitter.send("执行错误: " + e.getMessage());
                        } catch (IOException io) {
                            log.debug("SSE send after error: {}", io.toString());
                        }
                        emitter.complete();
                    } catch (Exception ex) {
                        emitter.completeWithError(ex);
                    }
                } finally {
                    this.cleanup();
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });


        emitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timed out");
        });

        emitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });

        return emitter;
    }

    //定义单个步骤
    public abstract String step();

    //清理资源
    protected void cleanup() {
        ManusSseContext.clear();
    }
}
