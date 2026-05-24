package com.knightdevelopers.kheerabackend.repository;

import com.knightdevelopers.kheerabackend.entity.Email;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface EmailRepository extends JpaRepository<Email, UUID> {
    List<Email> findTop20ByIsSentFalseAndIsFailedFalseAndSendAtBeforeOrderByIsUrgentDescSendAtAsc(Date now);

}
