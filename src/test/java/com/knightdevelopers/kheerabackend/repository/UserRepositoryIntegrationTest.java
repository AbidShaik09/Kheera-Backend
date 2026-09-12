package com.knightdevelopers.kheerabackend.repository;

import com.knightdevelopers.kheerabackend.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(statements = "TRUNCATE TABLE users RESTART IDENTITY CASCADE", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserRepositoryIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmailReturnsPersistedUser() {
        User user = userRepository.saveAndFlush(new User("sam@example.com", "secret", "Sam"));

        assertThat(userRepository.findByEmail("sam@example.com"))
                .hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(user.getId()));
    }

    @Test
    void findByEmailReturnsEmptyForUnknownEmail() {
        assertThat(userRepository.findByEmail("missing@example.com")).isEmpty();
    }

    @Test
    void duplicateEmailViolatesDatabaseUniqueConstraint() {
        userRepository.saveAndFlush(new User("duplicate@example.com", "secret", "First"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(new User("duplicate@example.com", "secret", "Second")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
