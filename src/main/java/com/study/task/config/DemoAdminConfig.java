package com.study.task.config;

import com.study.task.model.User;
import com.study.task.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("demo")
public class DemoAdminConfig {
    @Bean
    CommandLineRunner demoAdmin(
            UserRepository users,
            PasswordEncoder encoder,
            @Value("${demo.admin.email:admin@example.com}") String email,
            @Value("${demo.admin.password}") String password) {
        return args -> {
            if (password.length() < 8)
                throw new IllegalArgumentException("DEMO_ADMIN_PASSWORD >= 8 символов");
            if (!users.existsByEmail(email))
                users.save(new User(email, encoder.encode(password), com.study.task.model.Role.ROLE_ADMIN));
        };
    }
}
