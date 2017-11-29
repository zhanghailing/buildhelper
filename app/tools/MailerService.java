package tools;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailerService {
	
	private static MailerService mailerService;
	
	
	public static MailerService getInstance(){
		
		if(mailerService == null){
			mailerService = new MailerService();
		}
		
		return mailerService;
	}
	
	public String send(String to, String subject, String body){
		Properties props = new Properties();
	    props.put("mail.smtp.user", "niu2yue@gmail.com");
	    props.put("mail.smtp.host", "smtp.gmail.com");
	    props.put("mail.debug", "true");
	    props.put("mail.smtp.auth", "true");
	    props.put("mail.smtp.EnableSSL.enable", "true");
	    props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
	    props.setProperty("mail.smtp.socketFactory.fallbac k", "false");
	    props.setProperty("mail.smtp.port", "465");
	    props.setProperty("mail.smtp.socketFactory.port", "465");
	    
	    Session session = Session.getInstance(props,
	            new Authenticator() {
	                protected PasswordAuthentication getPasswordAuthentication() {
	                    return new PasswordAuthentication("niu2yue@gmail.com", "Iloveyue1314");
	                }
	            });
	    try {
	        Message message = new MimeMessage(session);
	        message.setFrom(new InternetAddress("niu2yue@gmail.com"));
	        message.setRecipients(Message.RecipientType.TO,
	                InternetAddress.parse(to));
	        message.setSubject(subject);
	        message.setText(body);

	        Transport.send(message);
	        return "success";
	    } catch (Exception e) {
	    	return e.getLocalizedMessage();
	    }
	}
	
}
