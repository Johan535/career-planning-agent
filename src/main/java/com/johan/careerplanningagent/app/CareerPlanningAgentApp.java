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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
public class CareerPlanningAgentApp {
    private static final org.slf4j.Logger log  = LoggerFactory.getLogger(CareerPlanningAgentApp.class);

    private final ChatClient chatClient;
    private final PersistentMemoryService persistentMemoryService;
    private static final String SYSTEM_PROMPT = "你是一名拥有 10 年以上职业规划经验的资深职业规划师，" +
            "专注于全行业职场人群的职业发展指导，熟悉各行业（互联网、金融、制造、教育等）的岗位要求、" +
            "晋升路径、技能体系，擅长将复杂的职业规划需求拆解为可落地的步骤，沟通风格专业、耐心、易懂，拒绝空话、套话，" +
            "所有建议均需贴合用户实际情况。";

/*    //基于内存的AI职业规划师知识库问答功能
    @Resource(name = "carePlanningAgentVectorStore")
    private VectorStore vectorStore;*/

    @Resource
    private ObjectProvider<Advisor> cloudRagAdvisorProvider;

    //基于PgVector向量存储的RAG知识库问答功能
    @Resource(name = "pgVectorStore")
    private VectorStore pgVectorStore;

    //查询重写功能
    @Resource
    private QueryRewriter queryRewriter;

    //AI调用工具能力
    @Resource
    private ToolCallback[] toolCallbacks;


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
        String withHistory = persistentMemoryService.buildMessageWithHistory(chatId, message, 20);
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
            String withHistory = persistentMemoryService.buildMessageWithHistory(chatId, message, 20);
            StringBuilder answerBuilder = new StringBuilder();
            return chatClient
                    .prompt()
                    .user(withHistory)
                    .stream()
                    .content()
                    .doOnNext(answerBuilder::append)
                    .doOnComplete(() -> persistentMemoryService.append(chatId, message, answerBuilder.toString()));
    }

    //定义结构化输出格式
    record Report(String title, List<String> suggestions) {

    }

    /**
     * AI 职业报告功能（支持结构化输出）
     */
    public Report doChatWithReport(String message,String chatId) {
        String withHistory = persistentMemoryService.buildMessageWithHistory(chatId, message, 20);
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
                persistentMemoryService.buildMessageWithHistory(chatId, message, 20)); //查询重写
        Advisor cloudAdvisor = cloudRagAdvisorProvider.getIfAvailable();
        ChatResponse chatResponse;
        if (cloudAdvisor != null) {
            chatResponse = chatClient
                    .prompt()
                    .user(rewrittenMessage)
                    .advisors(new QuestionAnswerAdvisor(pgVectorStore))
                    .advisors(cloudAdvisor)
                    .call()
                    .chatResponse();
        } else {
            chatResponse = chatClient
                    .prompt()
                    .user(rewrittenMessage)
                    .advisors(new QuestionAnswerAdvisor(pgVectorStore))
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
        String withHistory = persistentMemoryService.buildMessageWithHistory(chatId, message, 20);
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

}
