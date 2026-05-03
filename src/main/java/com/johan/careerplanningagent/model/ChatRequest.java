package com.johan.careerplanningagent.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "message 不能為空")
        @Size(max = 4000, message = "message 長度不能超過 4000")
        String message,

        @Size(max = 64, message = "chatId 長度不能超過 64")
        String chatId
) {
}
