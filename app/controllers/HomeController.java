package controllers;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import com.fasterxml.jackson.core.JsonProcessingException;

import actions.AuthAction;
import models.Account;
import models.ResponseData;
import play.Application;
import play.cache.CacheApi;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPA;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.*;
import services.MailerService;
import tools.Utils;
import views.html.*;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller {
	@Inject private FormFactory formFactory;
	@Inject private CacheApi cache;
	@Inject private JPAApi jpaApi;
	@Inject private Provider<Application> application;
	@Inject private MessagesApi messagesApi;

	@With(AuthAction.class)
    public Result index() {
        return ok(index.render());
    }    
}











