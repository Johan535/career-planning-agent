package com.johan.careerplanningagent.app;
import com.johan.careerplanningagent.rag.QueryRewriter;
import com.johan.careerplanningagent.service.PersistentMemoryService;
import jakarta.annotation.Resource;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.util.List;

@Component
public class CareerPlanningAgentApp {
    private static final org.slf4j.Logger log  = LoggerFactory.getLogger(CareerPlanningAgentApp.class);

    private final ChatClient chatClient;
    private final PersistentMemoryService persistentMemoryService;
    private static final String SYSTEM_PROMPT = "你是一名拥有 10 年以上职业规划经验的资深职业规划师，" +
            "专注于全行业职场人群的职业发展指导，熟悉各行业（互联网、金融、制造、教育等）的岗位要求、" +
            "晋升路径、技能体系，擅长将复杂的职业规划需求拆解为可落地的步骤，沟通风格专业、耐心、易懂，拒绝空话、套话，" +
            "所有建议均需贴合用户实际情况。" +
            "当用户消息中出现「历史对话」或「用户最新问题」等前缀时，你必须结合其中列出的过往问答理解上下文，保持多轮连贯；" +
            "不要声称自己无法记忆、看不到之前的内容，除非当前轮确实未提供任何历史文本。";

    /** 从 chat-memory 日志尾部读取的最大行数（每轮 USER+ASSISTANT 各占一行） */
    private static final int HISTORY_LOG_LINES = 200;

/*    //基于内存的AI职业规划师知识库问答功能
    @Resource(name = "carePlanningAgentVectorStore")
    private VectorStore vectorStore;*/

    @Resource
    private ObjectProvider<Advisor> cloudRagAdvisorProvider;

    // 默认向量存储（内存向量库）
    @Resource(name = "carePlanningAgentVectorStore")
    private VectorStore defaultVectorStore;

    // 可选PgVector向量存储（按配置启用）
    @Autowired(required = false)
    @Qualifier("pgVectorStore")
    private VectorStore pgVectorStore;

    //查询重写功能
    @Resource
    private QueryRewriter queryRewriter;

    //AI调用工具能力
    @Resource
    private ToolCallback[] toolCallbacks;

    @Value("${app.rag.use-pgvector:false}")
    private boolean usePgVector;

    //通过构造函数注入来创建ChatClient的方式，目的是创建一个ChatClient对象，并设置系统提示语
    public CareerPlanningAgentApp(ChatModel dashscopeChatModel, PersistentMemoryService persistentMemoryService){
        this.persistentMemoryService = persistentMemoryService;
        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    /**
     * AI基础对话（支持多轮对话记忆）
     * @param message 对话
     * @param chatId 会话ID
     * @return
     */
    public String chat(String message,String chatId) {
        String withHistory = persistentMemoryService.buildMessageWithHistory(chatId, message, HISTORY_LOG_LINES);
        ChatResponse chatResponse = chatClient
                .prompt() //创建一个Prompt对象
                .user(withHistory) //设置用户输入
                .call()  //调用ChatClient对象，执行对话
                .chatResponse(); //获取ChatResponse对象
        String text = chatResponse.getResult().getOutput().getText(); //获取结果
        persistentMemoryService.append(chatId, message, text);
        log.info("text:{}",text);
        return text;
    }

    /**
     * AI基础对话（支持多轮对话记忆,SSE流式传输，支持后续异步调用，无需等待）
     * @param message 对话
     * @param chatId 会话ID
     * @return
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        String withHistory = persistentMemoryService.buildMessageWithHistory(chatId, message, HISTORY_LOG_LINES);
        StringBuilder answerBuilder = new StringBuilder();
        return chatClient
                .prompt()
                .user(withHistory)
                .stream()
                .content()
                .doOnNext(answerBuilder::append)
                // 仅 doOnComplete 时持久化会在客户端提前断开、取消订阅时丢失本轮记录，导致下一轮无上下文
                .doFinally(signalType -> {
                    String text = answerBuilder.toString().trim();
                    if (text.isEmpty()) {
                        if (signalType == SignalType.CANCEL) {
                            text = "（输出已中断）";
                        } else if (signalType == SignalType.ON_ERROR) {
                            text = "（生成过程异常，未完成）";
                        } else {
                            text = "（本轮模型未返回可见文本）";
                        }
                    }
                    persistentMemoryService.append(chatId, message, text);
                });
    }

    //定义结构化输出格式
    record Report(String title, List<String> suggestions) {

    }

    /**
     * AI 职业报告功能（支持结构化输出）
     */
    public Report doChatWithReport(String message,String chatId) {
        String withHistory = persistentMemoryService.buildMessageWithHistory(chatId, message, HISTORY_LOG_LINES);
        Report report = chatClient
                .prompt() //创建一个Prompt对象
                .system(SYSTEM_PROMPT + "每次对话后都要生成职业规划结果，" +
                        "标题为{用户名}的职业规划报告，内容为建议列表")
                .user(withHistory) //设置用户输入
                .call()  //调用ChatClient对象，执行对话
                .entity(Report.class); //获取结构化输出,自定义结构化输出格式
        log.info("loveReport:{}",report);
        return report;
    }


    /**
     * 在RAG知识库的基础上进行问答对话
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRag(String message,String chatId){
        String rewrittenMessage = queryRewriter.doQueryRewrite(
                persistentMemoryService.buildMessageWithHistory(chatId, message, HISTORY_LOG_LINES)); //查询重写
        Advisor cloudAdvisor = cloudRagAdvisorProvider.getIfAvailable();
        VectorStore ragVectorStore = resolveRagVectorStore();
        ChatResponse chatResponse;
        if (cloudAdvisor != null) {
            chatResponse = chatClient
                    .prompt()
                    .user(rewrittenMessage)
                    .advisors(new QuestionAnswerAdvisor(ragVectorStore))
                    .advisors(cloudAdvisor)
                    .call()
                    .chatResponse();
        } else {
            chatResponse = chatClient
                    .prompt()
                    .user(rewrittenMessage)
                    .advisors(new QuestionAnswerAdvisor(ragVectorStore))
                    .call()
                    .chatResponse();
        }
        String text = chatResponse.getResult().getOutput().getText();
        persistentMemoryService.append(chatId, message, text);
        log.info("text:{}",text);
        return text;
    }


    /**
     * AI 职业报告功能（支持调用工具）
     */
    public String doChatWithTool(String message, String chatId) {
        String withHistory = persistentMemoryService.buildMessageWithHistory(chatId, message, HISTORY_LOG_LINES);
        ChatResponse chatResponse = chatClient
                .prompt() //创建一个Prompt对象
                .system(SYSTEM_PROMPT + "每次对话后都要生成职业规划结果，" +
                        "标题为{用户名}的职业规划报告，内容为建议列表")
                .user(withHistory) //设置用户输入
                .toolCallbacks(toolCallbacks)
                .call()  //调用ChatClient对象，执行对话
                .chatResponse();
        String context = chatResponse.getResult().getOutput().getText();
        persistentMemoryService.append(chatId, message, context);
        log.info("context:{}",context);
        return context;
    }

    private VectorStore resolveRagVectorStore() {
        if (usePgVector && pgVectorStore != null) {
            return pgVectorStore;
        }
        return defaultVectorStore;
    }

}
