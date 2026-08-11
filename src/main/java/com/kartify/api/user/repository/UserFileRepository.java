package com.kartify.api.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kartify.api.user.entity.UserFile;
import com.kartify.api.user.enums.FileType;

@Repository
public interface UserFileRepository extends JpaRepository<UserFile, Long> {

    Optional<UserFile> findByUserIdAndType(Long userId, FileType fileType);

}
