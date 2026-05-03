package com.johan.careerplanningagent.tool;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FileOperationToolTest {

    @Resource
    private FileOperationTool fileOperationTool;

    @Test
    void readFile() {
        String fileName = "test.txt";
        String result = fileOperationTool.readFile(fileName);
        Assertions.assertNotNull(result);
    }

    @Test
    void writeFile() {
        String fileName = "test.txt";
        String content = "Hello, World!";
        String result = fileOperationTool.writeFile(fileName, content);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("fileId="), result);
        Assertions.assertTrue(result.contains("成功") || result.contains("写入"), result);
    }
}