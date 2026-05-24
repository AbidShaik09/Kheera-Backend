package com.knightdevelopers.kheerabackend.service;

import com.knightdevelopers.kheerabackend.entity.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderService {
    @Autowired
    private JavaMailSender mailSender;

    public void sendEmail(Email email){
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email.getRecipientEmail());
        message.setSubject(email.getSubject());
        message.setText(email.getBody());

        mailSender.send(message);
    }
}
