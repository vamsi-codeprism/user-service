package org.userservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Setter
@Getter
@ToString
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Username is required")
    @Email(message = "Username must be a valid email address")
    @Size(max = 100, message = "Username cannot exceed 100 characters")
    private String username;

    @Column(nullable = false)
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!*()]).{8,}$",
        message = "Password must contain at least one digit, one lowercase, one uppercase, and one special character"
    )
    private String password;

    @Size(min = 6, max = 6, message = "Verification code must be exactly 6 characters")
    @Pattern(regexp = "^[0-9]{6}$", message = "Verification code must be a 6-digit number")
    private String verificationCode;

    private LocalDateTime codeExpiryTime;

    @Column(nullable = false)
    private boolean emailVerified = false;
}
