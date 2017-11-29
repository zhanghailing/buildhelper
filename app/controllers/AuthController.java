package controllers;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Provider;
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
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import tools.MailerService;
import tools.Utils;
import views.html.*;

public class AuthController extends Controller{
	@Inject private FormFactory formFactory;
	@Inject private CacheApi cache;
	@Inject private JPAApi jpaApi;
	@Inject private Provider<Application> application;
	@Inject private MessagesApi messagesApi;
	
	
	@Transactional
	public Result loginPage(){		
		return ok(loginpage.render());
	}
	
	@Transactional
	public Result login(){	
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String email = requestData.get("email");
		String password = requestData.get("password");
		
		TypedQuery<Account> query = JPA.em().createQuery("from Account ac where ac.email = :email", Account.class)
	            .setParameter("email", email);
		try{
			Account account = query.getSingleResult();
			
			if(!password.equals(account.password)){
				responseData.message = "The passowrd is incorrect.";
				responseData.code = 4000;
			}else{
				session(AuthAction.LOGGED_KEY, account.token);
				responseData.data = account;
			}
		}catch(NoResultException e){
			responseData.message = "Account does not exist.";
			responseData.code = 4000;
		}
		
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result signup(){
		ResponseData responseData = new ResponseData();
		Messages messages = messagesApi.preferred(request());
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String email = requestData.get("email");
		String password = requestData.get("password");
		
		if(Utils.isBlank(email)){
			responseData.message = messages.at("error_email_empty");
			responseData.code = 4000;
		}
		
		if(notExists(email)){
			String subject = "System-no-Reply";
			String body = messages.at("message.email_body", email, password);
			CompletableFuture.supplyAsync(() -> MailerService.getInstance().send(email, subject, body));
			cache.set(Account.EMAIL_PREFIX + email, password, 60 * 60);
		}else{
			responseData.code = 4000;
			responseData.message = "The mobile number was already registered.";
		}
		
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public static boolean notExists(String email){
		Query query = JPA.em().createQuery("select count(*) from Account ac where ac.email = :email")
	            .setParameter("email", email);
		
		Long count;
		try{
			count = (Long)query.getSingleResult();
		}catch(NoResultException e){
			count = 0L;
		}
		
		if (count == 0)
			return true;

		return false;
	}
	
	public Result logout() {
	    session().clear();
	    return redirect(controllers.routes.AuthController.loginPage());
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result blockAccount(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String accountId = requestData.get("accountId");
		
		Account account = jpaApi.em().find(Account.class, Long.parseLong(accountId));
		if(account == null) {
			responseData.code = 4000;
			responseData.message  = "The QP Account doesn't exist.";
		}
		
		account.blocked = true;
		jpaApi.em().persist(account);
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result unBlockAccount(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String accountId = requestData.get("accountId");
		
		Account account = jpaApi.em().find(Account.class, Long.parseLong(accountId));
		if(account == null) {
			responseData.code = 4000;
			responseData.message  = "The QP Account doesn't exist.";
		}
		
		account.blocked = false;
		jpaApi.em().persist(account);
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result deleteAccount(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String accountId = requestData.get("accountId");
		
		Account account = jpaApi.em().find(Account.class, Long.parseLong(accountId));
		if(account == null) {
			responseData.code = 4000;
			responseData.message  = "Account doesn't exist.";
		}
		
		account.deleted = true;
		jpaApi.em().persist(account);
		return ok(Json.toJson(responseData));
	}
	
}
