package com.kartify.api.account.service;

import org.springframework.stereotype.Service;

import com.kartify.api.account.dto.ProfileRequest;
import com.kartify.api.account.dto.ProfileResponse;
import com.kartify.api.user.entity.User;
import com.kartify.api.user.entity.UserDetail;
import com.kartify.api.user.repository.UserRepository;

@Service
public class ProfileService {

    protected final UserRepository userRepository;

    public ProfileService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    // --- Create Or Update User Detail ---
    public ProfileResponse upsertProfile(Long userId, ProfileRequest payload)
    {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        UserDetail detail = user.getUserDetail();
        if (detail == null) {
            detail = new UserDetail();
            detail.setUser(user);
            user.setUserDetail(detail);
        }

        detail.setFirstName(payload.firstName());
        detail.setLastName(payload.lastName());
        detail.setPhone(payload.phone());
        detail.setDateOfBirth(payload.dateOfBirth());
        detail.setGender(payload.gender());

        userRepository.save(user);

        return new ProfileResponse(
            detail.getFirstName(),
            detail.getLastName(),
            detail.getPhone(),
            detail.getDateOfBirth(),
            detail.getGender() 
        );
    }

}
