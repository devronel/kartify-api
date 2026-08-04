package com.kartify.api.database.seeder;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.kartify.api.user.entity.User;
import com.kartify.api.user.entity.UserDetail;
import com.kartify.api.user.enums.Role;
import com.kartify.api.user.enums.UserStatus;
import com.kartify.api.user.repository.UserRepository;

@Component
public class UserSeeder implements CommandLineRunner {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            loadSeedData();
        }
    }

    private void loadSeedData() {
        
        User admin = new User();
        admin.setEmail("admin@email.com");
        admin.setEmailVerifiedAt(LocalDateTime.now());
        admin.setPassword(passwordEncoder.encode("password"));
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);

        UserDetail userDetail = new UserDetail();
        userDetail.setFirstName("Kartify");
        userDetail.setLastName("Administrator");
        userDetail.setUser(admin);

        admin.setUserDetail(userDetail);

        userRepository.save(admin);

        System.out.println("Database successfully seeded admin account!");
    }
}
