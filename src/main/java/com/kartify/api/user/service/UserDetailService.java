package com.kartify.api.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartify.api.user.dto.UserDetailRequest;
import com.kartify.api.user.dto.UserDetailResponse;
import com.kartify.api.user.entity.User;
import com.kartify.api.user.entity.UserDetail;
import com.kartify.api.user.repository.UserRepository;

@Service
public class UserDetailService {

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public UserDetailResponse upsertDetail(Long userId, UserDetailRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        UserDetail detail = user.getUserDetail();
        if (detail == null) {
            detail = new UserDetail();
            detail.setUser(user);
            user.setUserDetail(detail);
        }

        detail.setFirstName(request.firstName());
        detail.setLastName(request.lastName());
        detail.setPhone(request.phone());
        detail.setDateOfBirth(request.dateOfBirth());
        detail.setGender(request.gender());

        userRepository.save(user);

        return new UserDetailResponse(
            user.getId(),
            detail.getFirstName(),
            detail.getLastName(),
            detail.getPhone(),
            detail.getDateOfBirth(),
            detail.getGender()
        );
    }

    public UserDetailResponse getUserDetail(Long userId){

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        UserDetail detail = user.getUserDetail();

        return new UserDetailResponse(
            user.getId(),
            detail.getFirstName(),
            detail.getLastName(),
            detail.getPhone(),
            detail.getDateOfBirth(),
            detail.getGender()
        );
    }

}
