package controllers;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import ModelVOs.AccountVO;
import ModelVOs.UserVO;
import actions.AuthAction;
import models.Account;
import models.AccountType;
import models.Company;
import models.Document;
import models.ResponseData;
import models.User;
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
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import services.MailerService;
import tools.Utils;
import views.html.*;

public class AuthController extends Controller{
	@Inject private FormFactory formFactory;
	@Inject private CacheApi cache;
	@Inject private JPAApi jpaApi;
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
				if(account.deleted) {
					responseData.message = "Your account was deleted.";
					responseData.code = 4000;
				}else {
					if(account.blocked) {
						responseData.message = "Your account was blocked.";
						responseData.code = 4000;
					}else {
						session(AuthAction.LOGGED_KEY, account.token);
						AccountVO accVO = new AccountVO(account);
						responseData.data = accVO;
					}
				}
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
	public Result activeAccount(String token){
		ResponseData responseData = new ResponseData();
		Messages messages = messagesApi.preferred(request());
		
		if(Utils.isBlank(token)){
			responseData.message = messages.at("active_auth_error");
			responseData.code = 4000;
		}else{
			Account account = Account.findByToken(token);
			if(account != null){
				account.active = true;
				jpaApi.em().persist(account);
			}else{
				responseData.message = messages.at("account_not_found");
				responseData.code = 4000;
			}
		}
		
		return redirect(routes.AuthController.loginPage());
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result saveQPAccount() {
		ResponseData responseData = new ResponseData();

		Account account = (Account) ctx().args.get("account");
		if (account.accType != AccountType.ADMIN) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		} else {
			DynamicForm requestData = formFactory.form().bindFromRequest();
			String qpAccountId = requestData.get("qpAccountId");
			String name = requestData.get("name");
			String email = requestData.get("email");
			String password = requestData.get("password");
			String alerEmail1 = requestData.get("alerEmail1");
			String alerEmail2 = requestData.get("alerEmail2");
			String officePhone = requestData.get("officePhone");
			String mobile = requestData.get("mobile");
			String isCivil = Utils.isBlank(requestData.get("isCivil")) ? "0" : requestData.get("isCivil");
			String isQECP = Utils.isBlank(requestData.get("isQECP")) ? "0" : requestData.get("isQECP");
			String isGeo = Utils.isBlank(requestData.get("isGeo")) ? "0" : requestData.get("isGeo");
			String isElectric = Utils.isBlank(requestData.get("isElectric")) ? "0" : requestData.get("isElectric");
			String isMechnical = Utils.isBlank(requestData.get("isMechnical")) ? "0" : requestData.get("isMechnical");
			String peNo = requestData.get("peNo");
			String qecpNo = requestData.get("qecpNo");
			
			Account qpAccount = null;
			if (!Utils.isBlank(qpAccountId)) {
				qpAccount = jpaApi.em().find(Account.class, Long.parseLong(qpAccountId));
			} else {
				if(AuthController.notExists(email)) {
					qpAccount = new Account(email, password);
					qpAccount.accType = AccountType.QP;
					qpAccount.active = false;
				}else {
					responseData.code = 4000;
					responseData.message = "Account already exists.";
				}
			}
			
			if(qpAccount != null) {
				qpAccount.email = email;
				qpAccount.password = password;
				Company company = (Company) jpaApi.em()
						.createNativeQuery("select * from company cy where cy.acc_id=:accId", Company.class)
						.setParameter("accId", account.id).getSingleResult();
				try{
					if (company == null) {
						responseData.code = 4000;
						responseData.message = "The account don't have company.";
					} else {
						qpAccount.company = company;
						jpaApi.em().persist(qpAccount);
	
						User user = null;
						if (qpAccount.user != null) {
							user = qpAccount.user;
						} else {
							user = new User(qpAccount);
						}
						
						if(AuthController.mobileNotExists(mobile)){
							user.name = name;
							user.alterEmail1 = alerEmail1;
							user.alterEmail2 = alerEmail2;
							user.officePhone = officePhone;
							user.mobile = mobile;
							user.isCivil = isCivil.equals("1") ? true : false;
							user.isQECP = isQECP.equals("1") ? true : false;
							user.isGeotechnical = isGeo.equals("1") ? true : false;
							user.isElectric = isElectric.equals("1") ? true : false;
							user.isMechanical = isMechnical.equals("1") ? true : false;
							user.peNo = peNo;
							user.qecpNo = qecpNo;
							jpaApi.em().persist(user);
							
							MultipartFormData<File> body = request().body().asMultipartFormData();
							List<FilePart<File>> fileParts = body.getFiles();
	
							List<Document> documentWillDelete = user.documents;
							if(documentWillDelete != null && documentWillDelete.size() > 0){
								for (Document d : documentWillDelete) {
									d.delete();
									jpaApi.em().remove(d);
								}
							}
								
							for (FilePart<File> filePart : fileParts) {
								Document doc = new Document(user, filePart.getFile());
								doc.name = filePart.getFilename();
								jpaApi.em().persist(doc);
							}
							
							String link = "http://" + request().host() + "/account/active?token=" + URLEncoder.encode(qpAccount.token, "UTF-8");
							String htmlBody = "Your account is: " + email + " and password is: " + password + " <p><a href='" + link + "'>Click here to active your account!</a></p>";
							CompletableFuture.supplyAsync(() 
									-> MailerService.getInstance().send(email, "Account Information", htmlBody));
						}else{
							responseData.code = 4000;
							responseData.message = "The mobile already exists.";
							jpaApi.em().remove(qpAccount);
						}
					}
				}catch(UnsupportedEncodingException e){
					responseData.code = 4001;
					responseData.message = e.getLocalizedMessage();
				}
			}
		}

		if (responseData.code != 0) {
			return notFound(errorpage.render(responseData));
		}
		
		return redirect(routes.CompanyController.qpList(0));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result saveInspectorAccount() {
		ResponseData responseData = new ResponseData();

		Account account = (Account) ctx().args.get("account");
		if (account.accType != AccountType.ADMIN) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		} else {
			DynamicForm requestData = formFactory.form().bindFromRequest();
			String inspectorAccountId = requestData.get("inpectorAccountId");
			String name = requestData.get("name");
			String email = requestData.get("email");
			String password = requestData.get("password");
			String alerEmail1 = requestData.get("alerEmail1");
			String alerEmail2 = requestData.get("alerEmail2");
			String officePhone = requestData.get("officePhone");
			String mobile = requestData.get("mobile");
			String designation = requestData.get("designation");

			Account inspectorAccount = null;
			if (!Utils.isBlank(inspectorAccountId)) {
				inspectorAccount = jpaApi.em().find(Account.class, Long.parseLong(inspectorAccountId));
			} else {
				if(AuthController.notExists(email)) {
					inspectorAccount = new Account(email, password);
					inspectorAccount.accType = AccountType.INSPECTOR;
					inspectorAccount.active = false;
				}else {
					responseData.code = 4000;
					responseData.message = "Email already exists.";
				}
			}
			
			if(inspectorAccount != null) {
				inspectorAccount.email = email;
				inspectorAccount.password = password;

				Company company = (Company) jpaApi.em()
						.createNativeQuery("select * from company cy where cy.acc_id=:accId", Company.class)
						.setParameter("accId", account.id).getSingleResult();
				try {
					if (company == null) {
						responseData.code = 4000;
						responseData.message = "The account don't have company.";
					} else {
						inspectorAccount.company = company;
						jpaApi.em().persist(inspectorAccount);
	
						User user = null;
						if (inspectorAccount.user != null) {
							user = inspectorAccount.user;
						} else {
							user = new User(inspectorAccount);
						}
						
						if(AuthController.mobileNotExists(mobile)){
							user.name = name;
							user.alterEmail1 = alerEmail1;
							user.alterEmail2 = alerEmail2;
							user.officePhone = officePhone;
							user.mobile = mobile;
							user.designation = designation;
							jpaApi.em().persist(user);
	
							MultipartFormData<File> body = request().body().asMultipartFormData();
							List<FilePart<File>> fileParts = body.getFiles();
							
							List<Document> documentWillDelete = user.documents;
							if(documentWillDelete != null && documentWillDelete.size() > 0){
								for (Document d : documentWillDelete) {
									d.delete();
									jpaApi.em().remove(d);
								}
							}
								
							for (FilePart<File> filePart : fileParts) {
								Document doc = new Document(user, filePart.getFile());
								doc.name = filePart.getFilename();
								jpaApi.em().persist(doc);
							}
							
							String link = "http://" + request().host() + "/account/active?token=" + URLEncoder.encode(inspectorAccount.token, "UTF-8");
							String htmlBody = "Your account is: " + email + " and password is: " + password + " <p><a href='" + link + "'>Click here to active your account!</a></p>";
							CompletableFuture.supplyAsync(() 
									-> MailerService.getInstance().send(email, "Account Information", htmlBody));
						}else{
							responseData.code = 4000;
							responseData.message = "The mobile already exists.";
							jpaApi.em().remove(inspectorAccount);
						}
					}
				} catch (UnsupportedEncodingException e) {
					responseData.code = 4001;
					responseData.message = e.getLocalizedMessage();
				}
			}
		}

		if (responseData.code != 0) {
			return notFound(errorpage.render(responseData));
		}

		return redirect(routes.CompanyController.inspectors(0));
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
	
	@Transactional
	public static boolean mobileNotExists(String mobile){
		Query query = JPA.em().createQuery("select count(*) from User u where u.mobile = :mobile")
	            .setParameter("mobile", mobile);
		
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
		
		if(account.accType == AccountType.QP){
			return redirect(routes.CompanyController.qpList(0));
		}else{
			return redirect(routes.CompanyController.inspectors(0));
		}
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result showMyInfo() {
		ResponseData responseData = new ResponseData();
		
		long accountId = ((Account) ctx().args.get("account")).id;
		Account account = jpaApi.em().find(Account.class, accountId);
		
		if(account != null) {
			UserVO userVO = new UserVO(account);
			responseData.data = userVO;
		}
		
		return ok(Json.toJson(responseData));
	}
	
}









