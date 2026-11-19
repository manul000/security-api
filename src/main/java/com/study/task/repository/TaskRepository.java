package com.study.task.repository;

import com.study.task.model.Task;
import com.study.task.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByOwner(User owner);
}
