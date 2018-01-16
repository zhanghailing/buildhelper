package services;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import play.Application;
import play.Play;

@Singleton
public class MailerService {
	private static final String FROM = "niu2yue@gmail.com";
	private static final String FROMNAME = "Changming";
	private static final String SMTP_KEY = "ses.email.key";
	private static final String SMTP_SECRET = "ses.email.secret";
	private static final String HOST = "email-smtp.us-east-1.amazonaws.com";
	private static MailerService mailerService;
	
	public static MailerService getInstance() {
		if(mailerService == null){
			mailerService = new MailerService();
		}
		return mailerService;
	}
	
	@SuppressWarnings("deprecation")
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
	        
	        String emailKey = Play.application().configuration().getString(SMTP_KEY);
	        String emailSecret = Play.application().configuration().getString(SMTP_SECRET);
	        
	        transport = session.getTransport();
	        transport.connect(HOST, emailKey, emailSecret);
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