package com.kartify.api.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kartify.api.user.entity.UserAddress;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    
    Optional<UserAddress> findByUserIdAndIsDefaultTrue(Long userId);

    Optional<UserAddress> findByUserId(Long userId);
    
}
