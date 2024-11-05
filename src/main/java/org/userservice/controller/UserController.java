package org.userservice.controller;

import org.springframework.http.HttpStatus;
import org.userservice.model.User;
import org.userservice.serviceImpl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserServiceImpl userService;

    @PostMapping("/auth/send")
    public ResponseEntity<?> sendAuth(@RequestBody User user) {
        try {
            if (user.getUsername() == null || user.getPassword() == null || user.getPassword().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid input");
            }
            User savedUser = userService.signUp(user);
            if (savedUser != null) {
                return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username already exists");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred");
        }
    }

    @PostMapping("/auth/verify")
    public ResponseEntity<?> verifyAuth(@RequestBody User user) {
        try {
            boolean isVerified = userService.verifyOtp(user.getUsername(), user.getVerificationCode());
            if (isVerified) {
                return ResponseEntity.status(HttpStatus.OK).body("OTP verified successfully");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid OTP or OTP expired");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during OTP verification");
        }
    }

    @PostMapping("/auth/usersignin")
    public ResponseEntity<?> usersignin(@RequestBody User user) {
        try {
            String token = userService.signIn(user.getUsername(), user.getPassword());
            if (token != null) {
                return ResponseEntity.status(HttpStatus.OK).body("Custom Token: " + token);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials or email not verified");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during sign-in");
        }
    }

}
