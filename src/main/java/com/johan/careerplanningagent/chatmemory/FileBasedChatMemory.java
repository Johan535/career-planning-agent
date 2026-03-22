package com.johan.careerplanningagent.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天记录的本地文件管理器
 */

public class FileBasedChatMemory implements ChatMemory {

    // 存储聊天文件的根目录（比如 "./chat-data"），一旦指定就不能改
    private final String BASE_DIR;
    // 默认只保留最后10条消息（可自定义）
    private final int defaultLastN;
    // 序列化/反序列化工具（Kryo 比Java自带的更快、更小）
    private static final Kryo kryo = new Kryo();

    // 静态代码块：给Kryo做基础配置，程序启动时只执行一次
    static {
        // 不用提前注册要序列化的类（比如Message），简化使用
        kryo.setRegistrationRequired(false);
        // 兼容没有无参构造函数的类，避免序列化失败
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    // 简单用法：只传存储目录，默认保留最后10条
    public FileBasedChatMemory(String dir) {
        this(dir, 10);
    }

    // 自定义用法：传目录 + 自定义默认保留条数（比如想默认保留20条）
    public FileBasedChatMemory(String dir, int defaultLastN) {
        this.BASE_DIR = dir;
        this.defaultLastN = defaultLastN;
        // 检查目录是否存在，不存在就自动创建（比如 "./chat-data" 文件夹）
        File baseDir = new File(dir);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        // 第一步：先读这个会话已有的消息（没有就返回空列表）
        List<Message> oldMessages = getOrCreateConversation(conversationId);
        // 第二步：把新消息追加到旧消息后面
        oldMessages.addAll(messages);
        // 第三步：把合并后的所有消息重新存回文件
        saveConversation(conversationId, oldMessages);
    }

    // 适配 Spring AI 旧接口：只传会话ID，用默认的10条
    @Override
    public List<Message> get(String conversationId) {
        return get(conversationId, this.defaultLastN);
    }

    // 自定义获取：传会话ID + 想要的条数（比如只取最后5条）
    public List<Message> get(String conversationId, int lastN) {
        // 先读这个会话的所有消息
        List<Message> allMessages = getOrCreateConversation(conversationId);
        // 计算要跳过多少条：总条数 - 想要的条数（比如总15条，要5条，就跳过10条）
        long skip = Math.max(0, allMessages.size() - lastN);
        // 只返回最后N条，避免返回所有消息占内存
        return allMessages.stream().skip(skip).toList();
    }

    //清空消息
    @Override
    public void clear(String conversationId) {
        // 找到这个会话对应的.kryo文件（比如 user_123.kryo）
        File file = getConversationFile(conversationId);
        // 存在就删掉，相当于清空这个会话的所有消息
        if (file.exists()) {
            file.delete();
        }
    }

    // 读文件：获取某个会话的所有消息（文件不存在就返回空列表）
    private List<Message> getOrCreateConversation(String conversationId) {
        File file = getConversationFile(conversationId);
        List<Message> messages = new ArrayList<>();
        if (file.exists()) {
            try (Input input = new Input(new FileInputStream(file))) {
                // 把文件字节转回 List<Message>
                messages = kryo.readObject(input, ArrayList.class);
            } catch (IOException e) {
                // 读文件出错时打印错误（生产环境建议用日志）
                e.printStackTrace();
            }
        }
        return messages;
    }

    // 写文件：把消息列表存到文件里
    private void saveConversation(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        try (Output output = new Output(new FileOutputStream(file))) {
            // 把 List<Message> 转成字节写进文件
            kryo.writeObject(output, messages);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 拼接文件路径：比如 BASE_DIR 是 "./chat-data"，会话ID是 user_123 → ./chat-data/user_123.kryo
    private File getConversationFile(String conversationId) {
        return new File(BASE_DIR, conversationId + ".kryo");
    }
}