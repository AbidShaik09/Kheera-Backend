package com.knightdevelopers.kheerabackend.worker;

import com.knightdevelopers.kheerabackend.entity.Email;
import com.knightdevelopers.kheerabackend.repository.EmailRepository;
import com.knightdevelopers.kheerabackend.service.EmailSenderService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailWorkerServiceTest {

    private final EmailRepository emailRepository = mock(EmailRepository.class);
    private final EmailSenderService emailSenderService = mock(EmailSenderService.class);
    private final EmailWorkerService emailWorkerService = new EmailWorkerService(emailRepository, emailSenderService);

    @Test
    void processEmailsDoesNothingWhenNoPendingEmailsExist() {
        when(emailRepository.findTop20ByIsSentFalseAndIsFailedFalseAndSendAtBeforeOrderByIsUrgentDescSendAtAsc(any(Date.class)))
                .thenReturn(List.of());

        emailWorkerService.processEmails();

        verify(emailSenderService, never()).sendEmail(any());
        verify(emailRepository, never()).save(any());
    }

    @Test
    void processEmailsMarksSuccessfulDeliverySentAndNotFailed() {
        Email email = queuedEmail(0);
        when(emailRepository.findTop20ByIsSentFalseAndIsFailedFalseAndSendAtBeforeOrderByIsUrgentDescSendAtAsc(any(Date.class)))
                .thenReturn(List.of(email));

        emailWorkerService.processEmails();

        verify(emailSenderService).sendEmail(email);
        verify(emailRepository).save(email);
        assertThat(email.getIsSent()).isTrue();
        assertThat(email.getIsFailed()).isFalse();
        assertThat(email.getTries()).isZero();
    }

    @Test
    void processEmailsIncrementsRetryCountAfterDeliveryFailure() {
        Email email = queuedEmail(1);
        when(emailRepository.findTop20ByIsSentFalseAndIsFailedFalseAndSendAtBeforeOrderByIsUrgentDescSendAtAsc(any(Date.class)))
                .thenReturn(List.of(email));
        doThrow(new RuntimeException("smtp down")).when(emailSenderService).sendEmail(email);

        emailWorkerService.processEmails();

        verify(emailRepository).save(email);
        assertThat(email.getIsSent()).isFalse();
        assertThat(email.getIsFailed()).isFalse();
        assertThat(email.getTries()).isEqualTo(2);
    }

    @Test
    void processEmailsMarksFourthFailurePermanentlyFailed() {
        Email email = queuedEmail(3);
        when(emailRepository.findTop20ByIsSentFalseAndIsFailedFalseAndSendAtBeforeOrderByIsUrgentDescSendAtAsc(any(Date.class)))
                .thenReturn(List.of(email));
        doThrow(new RuntimeException("smtp down")).when(emailSenderService).sendEmail(email);

        emailWorkerService.processEmails();

        verify(emailRepository).save(email);
        assertThat(email.getIsFailed()).isTrue();
        assertThat(email.getTries()).isEqualTo(4);
    }

    @Test
    void processEmailsTreatsNullRetryCountAsZero() {
        Email email = queuedEmail(null);
        when(emailRepository.findTop20ByIsSentFalseAndIsFailedFalseAndSendAtBeforeOrderByIsUrgentDescSendAtAsc(any(Date.class)))
                .thenReturn(List.of(email));
        doThrow(new RuntimeException("smtp down")).when(emailSenderService).sendEmail(email);

        emailWorkerService.processEmails();

        verify(emailRepository).save(email);
        assertThat(email.getIsFailed()).isFalse();
        assertThat(email.getTries()).isEqualTo(1);
    }

    @Test
    void processEmailsContinuesAfterOneEmailFails() {
        Email failed = queuedEmail(0);
        Email sent = queuedEmail(0);
        when(emailRepository.findTop20ByIsSentFalseAndIsFailedFalseAndSendAtBeforeOrderByIsUrgentDescSendAtAsc(any(Date.class)))
                .thenReturn(List.of(failed, sent));
        doThrow(new RuntimeException("smtp down")).when(emailSenderService).sendEmail(failed);

        emailWorkerService.processEmails();

        verify(emailSenderService).sendEmail(failed);
        verify(emailSenderService).sendEmail(sent);
        verify(emailRepository).save(failed);
        verify(emailRepository).save(sent);
        assertThat(failed.getTries()).isEqualTo(1);
        assertThat(failed.getIsFailed()).isFalse();
        assertThat(sent.getIsSent()).isTrue();
        assertThat(sent.getIsFailed()).isFalse();
    }

    @Test
    void processEmailsQueriesPendingQueueUsingCurrentTime() {
        when(emailRepository.findTop20ByIsSentFalseAndIsFailedFalseAndSendAtBeforeOrderByIsUrgentDescSendAtAsc(any(Date.class)))
                .thenReturn(List.of());

        emailWorkerService.processEmails();

        ArgumentCaptor<Date> nowCaptor = ArgumentCaptor.forClass(Date.class);
        verify(emailRepository).findTop20ByIsSentFalseAndIsFailedFalseAndSendAtBeforeOrderByIsUrgentDescSendAtAsc(nowCaptor.capture());
        assertThat(nowCaptor.getValue()).isCloseTo(new Date(), 1_000);
    }

    private static Email queuedEmail(Integer tries) {
        Email email = new Email();
        email.setRecipientEmail("member@example.com");
        email.setSubject("Subject");
        email.setBody("Body");
        email.setIsSent(false);
        email.setIsFailed(false);
        email.setTries(tries);
        email.setSendAt(new Date());
        return email;
    }
}
