package com.johan.careerplanningagent;

import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = PgVectorStoreAutoConfiguration.class)
public class CareerPlanningAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareerPlanningAgentApplication.class, args);
    }

}
