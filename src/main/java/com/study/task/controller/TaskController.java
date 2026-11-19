package com.study.task.controller;

import com.study.task.dto.TaskRequest;
import com.study.task.dto.TaskResponse;
import com.study.task.model.Task;
import com.study.task.model.User;
import com.study.task.service.TaskService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/tasks", "/api/tasks"})
@SecurityRequirement(name = "bearerAuth")
public class TaskController {
    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    public List<TaskResponse> all(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Task.Status status,
            @RequestParam(required = false) Task.Priority priority) {
        return service.getAll(user).stream()
                .filter(t -> status == null || t.status() == status)
                .filter(t -> priority == null || t.priority() == priority)
                .toList();
    }

    @GetMapping("/my")
    public List<TaskResponse> my(@AuthenticationPrincipal User user) {
        return service.getMyTasks(user);
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return service.findById(id, user);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<TaskResponse> create(
            @Valid @RequestBody TaskRequest r, @AuthenticationPrincipal User user) {
        TaskResponse t = service.create(r, user);
        return ResponseEntity.created(URI.create("/tasks/" + t.id())).body(t);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @taskService.isOwner(#id,authentication.name)")
    public TaskResponse update(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest r,
            @AuthenticationPrincipal User user) {
        return service.update(id, r, user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User user) {
        service.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/own")
    @PreAuthorize("hasRole('ADMIN') or @taskService.isOwner(#id,authentication.name)")
    public ResponseEntity<Void> deleteOwn(@PathVariable Long id, @AuthenticationPrincipal User user) {
        service.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public TaskResponse status(
            @PathVariable Long id,
            @Valid @RequestBody StatusRequest r,
            @AuthenticationPrincipal User user) {
        return service.updateStatus(id, r.status(), user);
    }

    @GetMapping("/stats")
    public Map<Task.Status, Long> stats(@AuthenticationPrincipal User user) {
        return service.getAll(user).stream()
                .collect(
                        java.util.stream.Collectors.groupingBy(
                                TaskResponse::status, java.util.stream.Collectors.counting()));
    }

    public record StatusRequest(@NotNull Task.Status status) {
    }
}
