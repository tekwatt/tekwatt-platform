package com.tekwatt.notification.service;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailClient {
    private final JavaMailSenderImpl mailSender;
    private final String host;
    private final String from;

    public SmtpEmailClient(@Value("${spring.mail.host:}") String host,
                           @Value("${spring.mail.port:587}") int port,
                           @Value("${spring.mail.username:}") String username,
                           @Value("${spring.mail.password:}") String password,
                           @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean auth,
                           @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}") boolean startTls,
                           @Value("${tekwatt.email.from:}") String from) {
        this.host = host;
        this.from = from;
        this.mailSender = new JavaMailSenderImpl();
        this.mailSender.setHost(host);
        this.mailSender.setPort(port);
        this.mailSender.setUsername(username);
        this.mailSender.setPassword(password);
        this.mailSender.getJavaMailProperties().put("mail.smtp.auth", String.valueOf(auth));
        this.mailSender.getJavaMailProperties().put("mail.smtp.starttls.enable", String.valueOf(startTls));
    }

    public String send(String recipient, String subject, String body) {
        if (host.isBlank() || from.isBlank())
            throw new EmailDeliveryException("Email delivery is not configured. Set SMTP_HOST, SMTP_USERNAME, SMTP_PASSWORD and SMTP_FROM.");
        if (recipient == null || !recipient.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new EmailDeliveryException("Email recipient is invalid.");
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(recipient);
            message.setSubject(subject == null || subject.isBlank() ? "TekWatt notification" : subject);
            message.setText(body);
            mailSender.send(message);
            return "SMTP-" + UUID.randomUUID();
        } catch (MailException exception) {
            throw new EmailDeliveryException("The SMTP provider rejected the email. Check the email settings and try again.");
        }
    }

    public static class EmailDeliveryException extends RuntimeException {
        public EmailDeliveryException(String message) { super(message); }
    }
}
