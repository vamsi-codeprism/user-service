package org.userservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestTemplate;
import org.userservice.model.User;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.userservice.service.EmailService;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.config.location=classpath:application-test.properties"
    }
)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class IntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }
    }

    @LocalServerPort
    private int port;

    private String baseUrl = "http://localhost";

    @Autowired
    private RestTemplate restTemplate;

    @MockBean
    private EmailService emailService;
    private User testUser;
    private String otpSent;
    private final String MOCK_OTP = "123456";

    @BeforeEach
    public void setUp() {
        baseUrl = baseUrl.concat(":").concat(port + "").concat("/api/users");
        testUser = User.builder()
            .username("test@gmail.com")
            .password("Password@123")
            .build();

        when(emailService.generateVerificationCode()).thenReturn(MOCK_OTP);
        doNothing().when(emailService).sendVerificationCode(any(), any());
    }

    @Test
    public void testUserServiceFlow() {
        // 1. Send OTP
        String otpResponse = restTemplate.postForObject(
            baseUrl + "/auth/send",
            testUser,
            String.class
        );
        assertNotNull(otpResponse);
        assertTrue(otpResponse.contains("OTP sent to test@gmail.com"));

        // 2. Verify OTP    
        testUser.setVerificationCode(MOCK_OTP);
        String verifyResponse = restTemplate.postForObject(
            baseUrl + "/auth/verify",
            testUser,
            String.class
        );
        assertNotNull(verifyResponse);
        assertEquals("OTP verified successfully", verifyResponse);
        System.out.println("testUser: "+testUser);
        // 3. Sign In
        String signInResponse = restTemplate.postForObject(
            baseUrl + "/auth/usersignin",
            testUser,
            String.class
        );
        assertNotNull(signInResponse);
        assertTrue(signInResponse.contains("Custom Token:"));
    }

}