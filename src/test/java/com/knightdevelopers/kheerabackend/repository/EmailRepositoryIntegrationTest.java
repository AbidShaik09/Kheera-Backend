package com.knightdevelopers.kheerabackend.repository;

import com.knightdevelopers.kheerabackend.entity.Email;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(statements = "TRUNCATE TABLE emails RESTART IDENTITY CASCADE", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class EmailRepositoryIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private EmailRepository emailRepository;

    @Test
    void pendingQueueFiltersOrdersAndLimitsResults() {
        Instant now = Instant.parse("2026-01-02T12:00:00Z");
        emailRepository.save(email("sent@example.com", true, false, false, now.minusSeconds(60)));
        emailRepository.save(email("failed@example.com", false, true, false, now.minusSeconds(60)));
        emailRepository.save(email("future@example.com", false, false, true, now.plusSeconds(60)));
        Email urgentLater = emailRepository.save(email("urgent-later@example.com", false, false, true, now.minusSeconds(60)));
        Email urgentEarlier = emailRepository.save(email("urgent-earlier@example.com", false, false, true, now.minusSeconds(120)));
        Email normalEarlier = emailRepository.save(email("normal-earlier@example.com", false, false, false, now.minusSeconds(180)));

        for (int index = 0; index < 25; index++) {
            emailRepository.save(email("bulk-" + index + "@example.com", false, false, false, now.minusSeconds(120 - index)));
        }
        emailRepository.flush();

        assertThat(emailRepository.findTop20ByIsSentFalseAndIsFailedFalseAndSendAtBeforeOrderByIsUrgentDescSendAtAsc(Date.from(now)))
                .hasSize(20)
                .extracting(Email::getId)
                .startsWith(urgentEarlier.getId(), urgentLater.getId())
                .contains(normalEarlier.getId())
                .doesNotContain(emailRepository.findAll().stream()
                        .filter(email -> email.getRecipientEmail().equals("sent@example.com"))
                        .findFirst()
                        .orElseThrow()
                        .getId());
    }

    private static Email email(String recipient, boolean sent, boolean failed, boolean urgent, Instant sendAt) {
        Email email = new Email();
        email.setRecipientEmail(recipient);
        email.setSubject("Subject");
        email.setBody("Body");
        email.setIsSent(sent);
        email.setIsFailed(failed);
        email.setIsUrgent(urgent);
        email.setSendAt(Date.from(sendAt));
        email.setTries(0);
        email.setCreatedAt(Date.from(sendAt));
        return email;
    }
}
