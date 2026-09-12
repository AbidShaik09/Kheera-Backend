package com.knightdevelopers.kheerabackend.worker;

import com.knightdevelopers.kheerabackend.entity.Email;
import com.knightdevelopers.kheerabackend.repository.EmailRepository;
import com.knightdevelopers.kheerabackend.service.EmailSenderService;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class EmailWorkerService {

    private final EmailRepository emailRepository;
    private final EmailSenderService emailSenderService;

    public EmailWorkerService(EmailRepository emailRepository, EmailSenderService emailSenderService) {
        this.emailRepository = emailRepository;
        this.emailSenderService = emailSenderService;
    }

    @Scheduled(fixedDelay = 20000)
    @Transactional
    public void processEmails() {

        List<Email> pendingEmails =
                emailRepository
                        .findTop20ByIsSentFalseAndIsFailedFalseAndSendAtBeforeOrderByIsUrgentDescSendAtAsc(
                                new Date()
                        );

        for (Email email : pendingEmails) {

            try {

                emailSenderService.sendEmail(email);

                email.setIsSent(true);
                email.setIsFailed(false);

            } catch (Exception ex) {

                int tries = email.getTries() == null ? 0 : email.getTries();
                email.setTries(tries + 1);

                if (email.getTries() >= 4) {
                    email.setIsFailed(true);
                }

                System.out.println("Failed to send email: " + ex.getMessage());
            }

            emailRepository.save(email);
        }
    }
}
