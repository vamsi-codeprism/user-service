package org.userservice.service;

import org.userservice.model.User;

public interface UserService {
    User signUp(User user);

    boolean verifyOtp(String username, String otp);

    String signIn(String username, String password);

}
