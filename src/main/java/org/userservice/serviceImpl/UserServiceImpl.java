package org.userservice.serviceImpl;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.userservice.model.User;
import org.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.userservice.service.EmailService;
import org.userservice.service.UserService;
import org.mindrot.jbcrypt.BCrypt;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;


@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    private static final String EMAIL_REGEX = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
    private Map<String, String> otpMap = new ConcurrentHashMap<>();
    private Map<String, LocalDateTime> otpExpiryMap = new ConcurrentHashMap<>();
    private Map<String, Boolean> emailVerifiedMap = new ConcurrentHashMap<>();

    public static boolean isValidEmail(String email) {
        return Pattern.matches(EMAIL_REGEX, email);
    }

    @Override
    public User signUp(User user) {
        if (user == null || user.getUsername() == null || user.getPassword() == null) {
            throw new IllegalArgumentException("User details cannot be null");
        }

        if (!isValidEmail(user.getUsername())) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (userRepository.findByUsername(user.getUsername()) != null) {
            throw new RuntimeException("Username already exists");
        }

        try {
            String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
            user.setPassword(hashedPassword);

            String otp = emailService.generateVerificationCode();
            otpMap.put(user.getUsername(), otp);
            otpExpiryMap.put(user.getUsername(), LocalDateTime.now().plusMinutes(5));
            System.out.println("OTP set for " + user.getUsername() + ": " + otp);
            System.out.println("Expiry time set for " + user.getUsername() + ": " + otpExpiryMap.get(user.getUsername()));
            emailService.sendVerificationCode(user.getUsername(), otp);

            user.setEmailVerified(false);
            return user;
        } catch (Exception e) {
            throw new RuntimeException("An error occurred during signup: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyOtp(String username, String otp) {
        String storedOtp = otpMap.get(username);
        LocalDateTime expiryTime = otpExpiryMap.get(username);
        System.out.println("Payload for verifyOtp: " + username + ", " + otp);
        System.out.println("Stored OTP: " + otpMap + ", " + otpExpiryMap);

        if (storedOtp != null && storedOtp.equals(otp) && expiryTime != null && expiryTime.isAfter(LocalDateTime.now())) {
            try {
                emailVerifiedMap.put(username, true);
                System.out.println("Email verified"+ emailVerifiedMap);

                return true;

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Firebase error: " + e.getMessage());
            }
        } else return false;
    }

    @Override
    public String signIn(String username, String password) {
        try {
            User user = userRepository.findByUsername(username);

            Boolean isEmailVerified = emailVerifiedMap.get(username);
            if ((isEmailVerified == null || !isEmailVerified) && (user == null || !user.isEmailVerified())) {
                throw new RuntimeException("Email is not verified. Please verify your email first.");
            }

            if (user != null) {
                if (!BCrypt.checkpw(password, user.getPassword())) {
                    throw new RuntimeException("Invalid credentials.");
                }
                UserRecord userRecord = FirebaseAuth.getInstance().getUserByEmail(username);
                return FirebaseAuth.getInstance().createCustomToken(userRecord.getUid());
            }

            return createNewUser(username, password);
        } catch (Exception e) {
            throw new RuntimeException("An error occurred during sign-in: " + e.getMessage(), e);
        }
    }

    private String createNewUser(String username, String password) throws FirebaseAuthException {

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        newUser.setEmailVerified(true);

        try {
            UserRecord newUserRecord = FirebaseAuth.getInstance().createUser(
                new UserRecord.CreateRequest()
                    .setEmail(username)
                    .setPassword(password)
                    .setEmailVerified(true)
            );
            
            userRepository.save(newUser);
            return FirebaseAuth.getInstance().createCustomToken(newUserRecord.getUid());
        } catch (FirebaseAuthException e) {
            if ("EMAIL_EXISTS".equals(e.getErrorCode())) {
                throw new RuntimeException("Email already registered with Firebase.", e);
            } else {
                throw new RuntimeException("Firebase error: " + e.getMessage(), e);
            }
        }
    }

}
