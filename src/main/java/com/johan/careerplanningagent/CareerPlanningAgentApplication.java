package com.johan.careerplanningagent;

import com.johan.careerplanningagent.rag.PgVectorStoreConfig;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CareerPlanningAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareerPlanningAgentApplication.class, args);
    }

}
