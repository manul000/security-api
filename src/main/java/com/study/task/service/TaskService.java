package com.study.task.service;

import com.study.task.dto.TaskRequest;
import com.study.task.dto.TaskResponse;
import com.study.task.exception.ApiException;
import com.study.task.model.Role;
import com.study.task.model.Task;
import com.study.task.model.User;
import com.study.task.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TaskService {
    private final TaskRepository tasks;

    public TaskService(TaskRepository tasks) {
        this.tasks = tasks;
    }

    private Task load(Long id) {
        return tasks
                .findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found: " + id));
    }

    private void check(Task task, User user) {
        if (user.getRole() != Role.ROLE_ADMIN && !task.getOwner().getId().equals(user.getId()))
            throw new AccessDeniedException("Not your task");
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAll(User user) {
        return (user.getRole() == Role.ROLE_ADMIN ? tasks.findAll() : tasks.findAllByOwner(user))
                .stream().map(TaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getMyTasks(User user) {
        return tasks.findAllByOwner(user).stream().map(TaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long id, User user) {
        Task t = load(id);
        check(t, user);
        return TaskResponse.from(t);
    }

    public TaskResponse create(TaskRequest r, User user) {
        return TaskResponse.from(
                tasks.save(
                        new Task(r.getTitle(), r.getDescription(), r.getPriority(), user, r.getDueDate())));
    }

    public TaskResponse update(Long id, TaskRequest r, User user) {
        Task t = load(id);
        check(t, user);
        t.update(r.getTitle(), r.getDescription(), r.getPriority(), r.getDueDate());
        return TaskResponse.from(tasks.save(t));
    }

    public TaskResponse updateStatus(Long id, Task.Status status, User user) {
        Task t = load(id);
        check(t, user);
        t.setStatus(status);
        return TaskResponse.from(tasks.save(t));
    }

    public void delete(Long id, User user) {
        Task t = load(id);
        check(t, user);
        tasks.delete(t);
    }

    @Transactional(readOnly = true)
    public boolean isOwner(Long id, String email) {
        return tasks.findById(id).map(t -> t.getOwner().getEmail().equals(email)).orElse(false);
    }
}
