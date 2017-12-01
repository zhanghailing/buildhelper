package services;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import play.Application;

public class MailerService {
	private static final String FROM = "niu2yue@gmail.com";
	private static final String FROMNAME = "Changming";
	private static final String SMTP_USERNAME = "aws.ses.username";
	private static final String SMTP_PASSWORD = "aws.ses.password";
	private static final String HOST = "email-smtp.us-east-1.amazonaws.com";
	private static MailerService mailerService;
	private final Application application;
	
	@Inject
    public MailerService(Application application){
    		this.application = application;
    }
	
	public static MailerService getInstance() {
		return mailerService;
	}
	
//	public String send(String to, String subject, String body){
//		Properties props = new Properties();
//	    props.put("mail.smtp.user", "niu2yue@gmail.com");
//	    props.put("mail.smtp.host", "smtp.gmail.com");
//	    props.put("mail.debug", "true");
//	    props.put("mail.smtp.auth", "true");
//	    props.put("mail.smtp.EnableSSL.enable", "true");
//	    props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
//	    props.setProperty("mail.smtp.socketFactory.fallback", "false");
//	    props.setProperty("mail.smtp.port", "465");
//	    props.setProperty("mail.smtp.socketFactory.port", "465");
//	    
//	    Session session = Session.getInstance(props,
//	            new Authenticator() {
//	                protected PasswordAuthentication getPasswordAuthentication() {
//	                    return new PasswordAuthentication("niu2yue@gmail.com", "Iloveyue1314");
//	                }
//	            });
//	    try {
//	        Message message = new MimeMessage(session);
//	        message.setFrom(new InternetAddress("niu2yue@gmail.com"));
//	        message.setRecipients(Message.RecipientType.TO,
//	                InternetAddress.parse(to));
//	        message.setSubject(subject);
//	        message.setText(body);
//
//	        Transport.send(message);
//	        return "success";
//	    } catch (Exception e) {
//	    	return e.getLocalizedMessage();
//	    }
//	}
	
	
	public String send(String to, String subject, String body){
		String result = "";
	    	Properties props = System.getProperties();
	    	props.put("mail.transport.protocol", "smtp");
	    	props.put("mail.smtp.port", 587); 
	    	props.put("mail.smtp.starttls.enable", "true");
	    	props.put("mail.smtp.auth", "true");
	    	Session session = Session.getDefaultInstance(props);

	    	Transport transport = null;
        try {
        		MimeMessage msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(FROM, FROMNAME));
			msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
	        msg.setSubject(subject);
	        msg.setContent(body, "text/html");
	        
	        transport = session.getTransport();
	        transport.connect(HOST, application.configuration().getString(SMTP_USERNAME), application.configuration().getString(SMTP_PASSWORD));
            transport.sendMessage(msg, msg.getAllRecipients());
            
            result = "success";
		} catch (UnsupportedEncodingException | MessagingException e) {
			result = e.getLocalizedMessage();
		}finally{
            try {
            		if(transport != null) {
            			transport.close();
            		}
			} catch (MessagingException e) {
				result = e.getLocalizedMessage();
			}
        }
        return result; 
    }
	
}
