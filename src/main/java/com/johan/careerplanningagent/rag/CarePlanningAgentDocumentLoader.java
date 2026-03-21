package com.johan.careerplanningagent.rag;

import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
* AI智能职业规划师文档加载器
* */
@Component
public class CarePlanningAgentDocumentLoader {
    private static final org.slf4j.Logger log  = LoggerFactory.getLogger(CarePlanningAgentDocumentLoader.class);

    //可以通过这个来加载多个文档
    private final ResourcePatternResolver resourcePatternResolver;

    //通过构造函数注入来创建MarkdownDocumentLoader对象
    public CarePlanningAgentDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    public List<Document> loadMarkDowns(){
        List<Document> allDocuments = new ArrayList<>();
        //加载多个文档 src/main/resources/markdowns/
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true) // 添加水平分割线
                        .withIncludeCodeBlock(false) // 添加代码块
                        .withIncludeBlockquote(false) // 添加引用
                        .withAdditionalMetadata("filename", filename) // 添加文件名元数据
                        .build(); // 创建MarkdownDocumentReaderConfig对象
                // 创建MarkdownDocumentReader对象
                MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource, config);
                allDocuments.addAll(markdownDocumentReader.get()); // 添加文档
            }
        } catch (IOException e) {
            log.error("markdown 文档加载失败",e);

        }
        return allDocuments;
    }
}
