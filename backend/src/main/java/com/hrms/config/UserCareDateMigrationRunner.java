package com.hrms.config;

import com.hrms.entity.User;
import com.hrms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UserCareDateMigrationRunner implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        Set<String> demoUsers = Set.of("admin", "hr", "emp");
        for (User user : userRepository.findAll()) {
            if (!demoUsers.contains(user.getUsername())) {
                continue;
            }
            boolean changed = false;
            if (user.getBirthday() == null) {
                user.setBirthday(LocalDate.now().plusDays(7 + Math.toIntExact(user.getId() % 5)));
                changed = true;
            }
            if (user.getHireDate() == null) {
                user.setHireDate(LocalDate.now().plusDays(15 + Math.toIntExact(user.getId() % 7)).minusYears(1));
                changed = true;
            }
            if (changed) {
                userRepository.save(user);
            }
        }
    }
}
