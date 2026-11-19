package com.study.task.dto;

import com.study.task.model.Task;

import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        Task.Priority priority,
        Task.Status status,
        LocalDateTime createdAt,
        LocalDateTime dueDate,
        String ownerEmail) {
    public static TaskResponse from(Task t) {
        return new TaskResponse(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getPriority(),
                t.getStatus(),
                t.getCreatedAt(),
                t.getDueDate(),
                t.getOwner().getEmail());
    }
}
