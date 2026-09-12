package com.knightdevelopers.kheerabackend;

import com.knightdevelopers.kheerabackend.repository.EmailRepository;
import com.knightdevelopers.kheerabackend.repository.OtpRepository;
import com.knightdevelopers.kheerabackend.repository.SpaceMembersRepository;
import com.knightdevelopers.kheerabackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "jwt.secret=abcdefghijklmnopqrstuvwxyz123456",
        "server.port=0"
})
class KheeraBackendApplicationTests {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private OtpRepository otpRepository;

    @MockBean
    private EmailRepository emailRepository;

    @MockBean
    private SpaceMembersRepository spaceMembersRepository;

    @Test
    void contextLoads() {
    }

}
