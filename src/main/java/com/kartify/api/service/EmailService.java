package com.kartify.api.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendEmail(String to, String resetUrl) throws MessagingException {

        Context context = new Context();
        context.setVariable("resetUrl", resetUrl);

        String htmlContent =
            templateEngine.process(
                "password-reset-email",
                context
            );


        MimeMessage message =
            mailSender.createMimeMessage();

        MimeMessageHelper helper =
            new MimeMessageHelper(message, true);

        helper.setTo(to);
        helper.setSubject("Reset Password");
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}
