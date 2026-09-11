package com.knightdevelopers.kheerabackend.repository;

import com.knightdevelopers.kheerabackend.entity.OneTimePassword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRepository extends JpaRepository<OneTimePassword, UUID> {
    Optional<OneTimePassword> findTopByEmailAndCreatedAtIsNotNullOrderByCreatedAtDesc(String email);
    public List<OneTimePassword> findByEmail(String email);
}
