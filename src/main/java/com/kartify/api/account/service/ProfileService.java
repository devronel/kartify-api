package com.kartify.api.account.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.kartify.api.account.dto.ProfileRequest;
import com.kartify.api.account.dto.ProfileResponse;
import com.kartify.api.service.ImageKitService;
import com.kartify.api.user.entity.User;
import com.kartify.api.user.entity.UserDetail;
import com.kartify.api.user.entity.UserFile;
import com.kartify.api.user.enums.FileType;
import com.kartify.api.user.repository.UserFileRepository;
import com.kartify.api.user.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class ProfileService {

    protected final UserRepository userRepository;
    protected final UserFileRepository userFileRepository;
    protected final ImageKitService imageKitService;

    public ProfileService(UserRepository userRepository, UserFileRepository userFileRepository, ImageKitService imageKitService){
        this.userRepository = userRepository;
        this.userFileRepository = userFileRepository;
        this.imageKitService = imageKitService;
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

    // --- Upload profile picture ---
    @Transactional
    public String uploadProfilePicture(Long userId, MultipartFile file){
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<UserFile> existingProfile = userFileRepository.findByUserIdAndType(userId, FileType.PROFILE_PICTURE);

        if (existingProfile.isPresent()) {
            UserFile userFile = existingProfile.get();
            userFileRepository.save(userFile);
        } else {
            // UserFile userFile = new UserFile();
            // userFileRepository.save(userFile);
        }

        return "Profile URL";
    }

}
