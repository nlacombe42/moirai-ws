package net.nlacombe.moirai.service;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

@Service
public class EmailSenderService {

    private static final Logger logger = LoggerFactory.getLogger(EmailSenderService.class);

    private JavaMailSender mailSender;
    private String senderEmail;
    private String senderName;
    private String errorEmailAddress;

    @Inject
    public EmailSenderService(JavaMailSender mailSender, @Value("${email.sender.emailAddress}") String senderEmail,
                              @Value("${email.sender.name}") String senderName,
                              @Value("${email.error.emailAddress}") String errorEmailAddress) {

        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.errorEmailAddress = errorEmailAddress;
    }

    @Async
    public void sendErrorEmail(Exception exception) {
        String subject = "Error during calendar sync";

        String body = "";

        body += "<h4>Stacktrace</h4>";
        body += "<pre>" + ExceptionUtils.getStackTrace(exception) + "</pre>";

        sendEmail(errorEmailAddress, subject, body);
    }

    private void sendEmail(String recipientEmailAddress, String emailSubject, String emailBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper messageHelper = new MimeMessageHelper(message);

            messageHelper.setFrom(new InternetAddress(senderEmail, senderName));
            messageHelper.setTo(recipientEmailAddress);
            messageHelper.setSubject(emailSubject);
            messageHelper.setText(emailBody, true);

            logger.debug("Sending email: from: \"" + senderEmail + "\", recipientEmailAddress: \"" + recipientEmailAddress + "\", emailSubject: \"" + emailSubject + "\"");

            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException exception) {
            throw new RuntimeException(exception);
        }
    }
}
