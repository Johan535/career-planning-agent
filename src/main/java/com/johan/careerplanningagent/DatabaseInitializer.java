package com.johan.careerplanningagent;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * 数据库初始化配置
 * 在应用启动时自动创建 vector_store 表
 */
@Configuration
public class DatabaseInitializer {

    @Bean
    public CommandLineRunner initDatabase(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                // 读取 schema.sql 文件
                ClassPathResource resource = new ClassPathResource("schema.sql");
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
                );
                
                String sql = reader.lines().collect(Collectors.joining("\n"));
                reader.close();
                
                // 分割并执行 SQL 语句
                String[] statements = sql.split(";");
                for (String statement : statements) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                        try {
                            jdbcTemplate.execute(trimmed);
                            System.out.println("执行 SQL 成功: " + trimmed.substring(0, Math.min(50, trimmed.length())));
                        } catch (Exception e) {
                            // 忽略已存在的错误
                            System.out.println("SQL 执行提示: " + e.getMessage());
                        }
                    }
                }
                
                System.out.println("数据库初始化完成！vector_store 表已创建。");
                
            } catch (Exception e) {
                System.err.println("数据库初始化失败: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
}
