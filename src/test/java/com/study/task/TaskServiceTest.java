package com.study.task;

import com.study.task.model.Role;
import com.study.task.model.Task;
import com.study.task.model.User;
import com.study.task.repository.TaskRepository;
import com.study.task.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock
    TaskRepository repository;
    @InjectMocks
    TaskService service;

    User user(long id, Role role) {
        User u = new User("u" + id + "@test.com", "hash", role);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    @Test
    void anotherUserCannotDelete() {
        User a = user(1, Role.ROLE_USER), b = user(2, Role.ROLE_USER);
        Task t = new Task("Task", null, Task.Priority.HIGH, a, null);
        when(repository.findById(1L)).thenReturn(Optional.of(t));
        assertThatThrownBy(() -> service.delete(1L, b)).isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).delete(any(Task.class));
    }

    @Test
    void adminCanDeleteAnotherUsersTask() {
        Task t = new Task("Task", null, Task.Priority.HIGH, user(1, Role.ROLE_USER), null);
        when(repository.findById(1L)).thenReturn(Optional.of(t));
        service.delete(1L, user(2, Role.ROLE_ADMIN));
        verify(repository).delete(t);
    }
}
