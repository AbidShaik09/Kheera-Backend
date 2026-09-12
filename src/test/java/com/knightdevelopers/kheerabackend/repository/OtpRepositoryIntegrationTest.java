package com.knightdevelopers.kheerabackend.repository;

import com.knightdevelopers.kheerabackend.entity.OneTimePassword;
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
@Sql(statements = "TRUNCATE TABLE one_time_passwords RESTART IDENTITY CASCADE", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OtpRepositoryIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private OtpRepository otpRepository;

    @Test
    void newestOtpForEmailIgnoresOlderOtherEmailAndLegacyNullTimestampRecords() {
        OneTimePassword older = otp("person@example.com", 111111L, Instant.parse("2026-01-01T10:00:00Z"));
        OneTimePassword newest = otp("person@example.com", 222222L, Instant.parse("2026-01-02T10:00:00Z"));
        OneTimePassword otherUser = otp("other@example.com", 333333L, Instant.parse("2026-01-03T10:00:00Z"));
        OneTimePassword legacy = otp("person@example.com", 444444L, Instant.parse("2026-01-04T10:00:00Z"));
        legacy.setCreatedAt(null);
        otpRepository.save(older);
        otpRepository.save(newest);
        otpRepository.save(otherUser);
        otpRepository.save(legacy);
        otpRepository.flush();

        assertThat(otpRepository.findTopByEmailAndCreatedAtIsNotNullOrderByCreatedAtDesc("person@example.com"))
                .hasValueSatisfying(found -> assertThat(found.getOtp()).isEqualTo(222222L));
        assertThat(otpRepository.findByEmail("person@example.com"))
                .extracting(OneTimePassword::getOtp)
                .containsExactlyInAnyOrder(111111L, 222222L, 444444L);
    }

    @Test
    void unknownEmailReturnsEmptyResults() {
        assertThat(otpRepository.findTopByEmailAndCreatedAtIsNotNullOrderByCreatedAtDesc("missing@example.com")).isEmpty();
        assertThat(otpRepository.findByEmail("missing@example.com")).isEmpty();
    }

    private static OneTimePassword otp(String email, Long code, Instant createdAt) {
        OneTimePassword otp = new OneTimePassword();
        otp.setEmail(email);
        otp.setOtp(code);
        otp.setCreatedAt(createdAt);
        otp.setExpiresAt(Date.from(createdAt.plusSeconds(600)));
        return otp;
    }
}
