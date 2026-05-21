package org.nr31.backend.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.service.EmailSenderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class EmailSenderServiceImpl implements EmailSenderService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MessageSource messageSource;
    private final String fromAddress;

    public EmailSenderServiceImpl(
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            MessageSource messageSource,
            @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.messageSource = messageSource;
        this.fromAddress = fromAddress;
    }

    @Override
    @Async
    public void sendHtmlEmail(String to, String subjectKey, String templateName, Map<String, Object> variables, Locale locale) {
        log.info("Sending HTML email to: {} with subjectKey: {} and locale: {}", to, subjectKey, locale);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            String subject = messageSource.getMessage(subjectKey, null, locale);

            Context context = new Context(locale);
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom(fromAddress);

            mailSender.send(message);
            log.info("Successfully sent HTML email to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
