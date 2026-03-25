package com.johan.careerplanningagent.app;
import com.johan.careerplanningagent.rag.CarePlanningAgentRagCloudAdvisor;
import com.johan.careerplanningagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
public class CareerPlanningAgentApp {
    private static final org.slf4j.Logger log  = LoggerFactory.getLogger(CareerPlanningAgentApp.class);

    private final ChatClient chatClient;
    private static final String SYSTEM_PROMPT = "你是一名拥有 10 年以上职业规划经验的资深职业规划师，" +
            "专注于全行业职场人群的职业发展指导，熟悉各行业（互联网、金融、制造、教育等）的岗位要求、" +
            "晋升路径、技能体系，擅长将复杂的职业规划需求拆解为可落地的步骤，沟通风格专业、耐心、易懂，拒绝空话、套话，" +
            "所有建议均需贴合用户实际情况。";

    //基于内存的AI职业规划师知识库问答功能
    @Resource(name = "carePlanningAgentVectorStore")
    private VectorStore vectorStore;

    //基于云知识库的RAG知识库问答功能
    @Resource
    private CarePlanningAgentRagCloudAdvisor carePlanningAgentRagCloudAdvisor;

    //基于PgVector向量存储的RAG知识库问答功能
    @Resource(name = "pgVectorStore")
    private VectorStore pgVectorStore;

    //查询重写功能
    @Resource
    private QueryRewriter queryRewriter;

    //AI调用工具能力
    @Resource
    private ToolCallback[] toolCallbacks;

    //AI调用MCP服务
    @Resource
    private ToolCallbackProvider toolCallbackProvider;


    //通过构造函数注入来创建ChatClient的方式，目的是创建一个ChatClient对象，并设置系统提示语
    public CareerPlanningAgentApp(ChatModel dashscopeChatModel){
    /*    //使用基于文件的对话记忆
        String fileDir =  System.getProperty("user.dir") + "/chat-memory";
        FileBasedChatMemory fileBasedChatMemory = new FileBasedChatMemory(fileDir);*/

        //初始化记忆内存的对话记忆
        ChatMemory chatMemory = new InMemoryChatMemory();
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
        ChatResponse chatResponse = chatClient
                .prompt() //创建一个Prompt对象
                .user(message) //设置用户输入
                .call()  //调用ChatClient对象，执行对话
                .chatResponse(); //获取ChatResponse对象
        String text = chatResponse.getResult().getOutput().getText(); //获取结果
        log.info("text:{}",text);
        return text;
    }

    //定义结构化输出格式
    record Report(String title, List<String> suggestions) {

    }

    /**
     * AI 职业报告功能（支持结构化输出）
     */
    public Report doChatWithReport(String message,String chatId) {
        Report report = chatClient
                .prompt() //创建一个Prompt对象
                .system(SYSTEM_PROMPT + "每次对话后都要生成职业规划结果，" +
                        "标题为{用户名}的职业规划报告，内容为建议列表")
                .user(message) //设置用户输入
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
    private String doChatWithRag(String message,String chatId){
        String rewrittenMessage = queryRewriter.doQueryRewrite(message); //查询重写
        ChatResponse chatResponse = chatClient
                .prompt() //创建一个Prompt对象
                .user(rewrittenMessage) //设置用户输入(使用查询重写的结果)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId) //根据会话ID进行对话记忆
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10)) //实现会话记忆，保存最近十条聊天记录
                //应用RAG知识库问答（基于本地知识库）
                .advisors(new QuestionAnswerAdvisor(vectorStore))
                //应用RAG检索增强服务(基于云知识库)
                //.advisors((Consumer<ChatClient.AdvisorSpec>) carePlanningAgentRagCloudAdvisor)
                //应用RAG检索增强服务(基于PgVector向量存储)
                .advisors(new QuestionAnswerAdvisor(pgVectorStore))
                //应用自定义RAG检索增强服务
/*                .advisors(
                        CareerPlanningAgentCustomAdvisorFactory.createCareerPlanningAgentCustom(
                                pgVectorStore,
                                "新人"
                        )
                )*/
                .call()  //调用ChatClient对象，执行对话
                .chatResponse();
        String text = chatResponse.getResult().getOutput().getText();
        log.info("text:{}",text);
        return text;
    }


    /**
     * AI 职业报告功能（支持调用工具）
     */
    public String doChatWithTool(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt() //创建一个Prompt对象
                .system(SYSTEM_PROMPT + "每次对话后都要生成职业规划结果，" +
                        "标题为{用户名}的职业规划报告，内容为建议列表")
                .user(message) //设置用户输入
                .toolCallbacks(toolCallbacks)
                .call()  //调用ChatClient对象，执行对话
                .chatResponse();
        String context = chatResponse.getResult().getOutput().getText();
        log.info("context:{}",context);
        return context;
    }

    /**
     * AI 职业报告功能（调用MCP服务）
     */
    public String doChatWithMcp(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt() //创建一个Prompt对象
                .system(SYSTEM_PROMPT + "每次对话后都要生成职业规划结果，" +
                        "标题为{用户名}的职业规划报告，内容为建议列表")
                .user(message) //设置用户输入
                .toolCallbacks(toolCallbackProvider)
                .call()  //调用ChatClient对象，执行对话
                .chatResponse();
        String context = chatResponse.getResult().getOutput().getText();
        log.info("context:{}",context);
        return context;
    }



}
