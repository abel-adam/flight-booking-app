package com.example.flightbooking.util;

import android.os.AsyncTask;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailUtils extends AsyncTask<Void, Void, Boolean> {
    private String email;
    private String subject;
    private String messageBody;
    private EmailCallback callback;

    public interface EmailCallback {
        void onResult(boolean success);
    }

    public EmailUtils(String email, String subject, String messageBody, EmailCallback callback) {
        this.email = email;
        this.subject = subject;
        this.messageBody = messageBody;
        this.callback = callback;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        final String username = "coderbela@gmail.com";
        final String password = "mwqe kkzc bcap ckuw"; // Note: This is an App Password, NOT your regular password

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject(subject);
            message.setText(messageBody);

            Transport.send(message);
            return true;

        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (callback != null) {
            callback.onResult(result);
        }
    }
}
