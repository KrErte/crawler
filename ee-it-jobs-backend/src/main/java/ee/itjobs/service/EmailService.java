package ee.itjobs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail-from:noreply@eeitjobs.ee}")
    private String fromAddress;

    @Value("${app.base-url:http://localhost:4200}")
    private String baseUrl;

    public void sendPasswordResetEmail(String to, String token) {
        String resetUrl = baseUrl + "/reset-password?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("EE IT Jobs - Password Reset");
        message.setText("Click the link below to reset your password:\n\n" +
                resetUrl + "\n\nThis link expires in 1 hour.\n\n" +
                "If you did not request this, please ignore this email.");
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}", to, e);
        }
    }

    public void sendVerificationEmail(String to, String token) {
        String verifyUrl = baseUrl + "/verify-email?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("EE IT Jobs - Verify Your Email");
        message.setText("Welcome to EE IT Jobs!\n\n" +
                "Click the link below to verify your email:\n\n" +
                verifyUrl + "\n\nThis link expires in 24 hours.");
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}", to, e);
        }
    }

    public void sendJobAlertEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send job alert email to {}", to, e);
        }
    }
}
