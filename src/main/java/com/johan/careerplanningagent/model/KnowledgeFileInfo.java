package com.johan.careerplanningagent.model;

public record KnowledgeFileInfo(String fileName, long size, String uploadTime, boolean indexed, String indexMessage) {
}
