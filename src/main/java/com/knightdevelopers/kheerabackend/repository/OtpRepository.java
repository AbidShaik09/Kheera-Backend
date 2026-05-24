package com.knightdevelopers.kheerabackend.repository;

import com.knightdevelopers.kheerabackend.entity.OneTimePassword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Repository
public interface OtpRepository extends JpaRepository<OneTimePassword, UUID> {
    public List<OneTimePassword> findTop5ByEmailOrderByCreatedAtDesc(String email);
    public List<OneTimePassword> findByEmail(String email);
}
