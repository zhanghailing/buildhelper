package controllers;

import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import ModelVOs.AccountVO;
import actions.AuthAction;
import models.Account;
import models.AccountType;
import models.Builder;
import models.Client;
import models.Company;
import models.DrawingFile;
import models.Engineer;
import models.LetterHead;
import models.Project;
import models.ProjectStatus;
import models.ResponseData;
import play.Application;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPA;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import services.MailerService;
import tools.Constants;
import tools.Utils;
import views.html.*;

@SuppressWarnings("unchecked")
public class ProjectController extends Controller{
	@Inject private FormFactory formFactory;
	@Inject private JPAApi jpaApi;
	@Inject private Provider<Application> application;
	
	@With(AuthAction.class)
	@Transactional
	public Result projectOfEngineer(int offset){
		ResponseData responseData = new ResponseData();

		long accountId = ((Account) ctx().args.get("account")).id;
		Account account = jpaApi.em().find(Account.class, accountId);
		if (account == null) {
			responseData.code = 4000;
			responseData.message = "Account doesn't exist.";
		}else{
			int totalAmount = ((BigInteger) jpaApi.em()
					.createNativeQuery("SELECT COUNT(*) FROM (SELECT pro.id FROM project pro WHERE pro.engineer_id=:engineerId" + 
							" UNION " + 
							"SELECT  pt.project_id as id FROM project_team pt WHERE pt.account_id=:accountId " + 
							"ORDER BY id) as t")
					.setParameter("engineerId", account.id)
					.setParameter("accountId", account.id)
					.getSingleResult()).intValue();
			
			int pageIndex = (int) Math.ceil(offset / Constants.COMPANY_PAGE_SIZE) + 1;
			
			List<String> results = jpaApi.em().createNativeQuery("SELECT pro.id FROM project pro WHERE pro.engineer_id=:engineerId" + 
					" UNION " + 
					"SELECT pt.project_id as id FROM project_team pt WHERE pt.account_id=:accountId " + 
					"ORDER BY id")
					.setParameter("engineerId", account.id)
					.setParameter("accountId", account.id)
					.setFirstResult(offset).setMaxResults(Constants.COMPANY_PAGE_SIZE).getResultList();
			
			String projectIDCause = "";
			for (int i = 0; i < results.size(); i++) {
				if (i == results.size() - 1) {
					projectIDCause += "pro.id='" + ((BigInteger) ((Object)results.get(i))).intValue() + "'";
				} else {
					projectIDCause += "pro.id='" + ((BigInteger) ((Object)results.get(i))).intValue() + "' OR ";
				}
			}
			
			String projectCause = "";
			if(Utils.isBlank(projectIDCause)) {
				projectCause = "pro.engineer_id=" + account.id; 
			}else {
				projectCause = projectIDCause;
			}

			List<Project> projects = jpaApi.em()
					.createNativeQuery("SELECT * FROM project pro WHERE " + projectCause + " AND pro.is_archived = :isArchived", Project.class)
					.setParameter("isArchived", false)
					.setFirstResult(offset).setMaxResults(Constants.COMPANY_PAGE_SIZE).getResultList();
			return ok(projectofengineer.render(account, projects, pageIndex,totalAmount));
		}
		return notFound(errorpage.render(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result projectOfCompany(int offset){
		ResponseData responseData = new ResponseData();

		Account account = (Account) ctx().args.get("account");
		if (account.accType != AccountType.ADMIN) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}else{
			try {
				Company company = jpaApi.em().createQuery("FROM Company cy WHERE cy.account = :account", Company.class)
						.setParameter("account", account)
						.getSingleResult();
				
				List<Engineer> engineers = jpaApi.em()
						.createNativeQuery("SELECT * FROM engineer eng LEFT JOIN account ac ON ac.id = eng.account_id WHERE eng.company_id = :companyId AND ac.blocked = :blocked AND ac.deleted = :deleted AND ac.active = :active", Engineer.class)
						.setParameter("companyId", company.id)
						.setParameter("blocked", false)
						.setParameter("deleted", false)
						.setParameter("active", true)
						.getResultList();
				
				String engineerIDCause = "";
				for (int i = 0; i < engineers.size(); i++) {
					if (i == engineers.size() - 1) {
						engineerIDCause += "pro.engineer_id='" + engineers.get(i).accountId + "'";
					} else {
						engineerIDCause += "pro.engineer_id='" + engineers.get(i).accountId + "' OR ";
					}
				}
				
				String countSql = "SELECT COUNT(*) FROM project pro";
				String sql = "SELECT * FROM project pro";
				if(!Utils.isBlank(engineerIDCause)){
					countSql = countSql + " WHERE " + engineerIDCause;
					sql = sql + " WHERE " + engineerIDCause;
					
					int totalAmount = ((BigInteger) jpaApi.em().createNativeQuery(countSql).getSingleResult()).intValue();
					int pageIndex = (int) Math.ceil(offset / Constants.COMPANY_PAGE_SIZE) + 1;
					
					List<Project> projects = jpaApi.em().createNativeQuery(sql, Project.class)
							.setFirstResult(offset).setMaxResults(Constants.COMPANY_PAGE_SIZE).getResultList();
					
					return ok(projectofcompany.render(account, projects, pageIndex, totalAmount));
				}else {
					return ok(projectofcompany.render(account, null, 1, 0));
				}
			}catch(NoResultException e) {
				responseData.code = 4000;
				responseData.message = "Cannot find the company.";
			}
		}
		return notFound(errorpage.render(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result createProject(long projectId) {
		ResponseData responseData = new ResponseData();

		Account account = (Account) ctx().args.get("account");
		Engineer engineer = jpaApi.em().find(Engineer.class, account.id);
		if (engineer == null) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}else{
			String qpSql = "SELECT * FROM account ac WHERE ac.deleted=0 AND ac.blocked=0 AND ac.active=1 AND ac.acc_type=3 AND ac.company_id = " + engineer.company.id;
			List<Account> qpList = jpaApi.em().createNativeQuery(qpSql, Account.class).getResultList();
			
			String inspectorSql = "SELECT * FROM account ac WHERE ac.deleted=0 AND ac.blocked=0 AND ac.active=1 AND ac.acc_type=2 AND ac.company_id = " + engineer.company.id;
			List<Account> inspectors = jpaApi.em().createNativeQuery(inspectorSql, Account.class).getResultList();
			
			Project project = null;
			if(projectId > 0){
				project = jpaApi.em().find(Project.class, projectId);
			}
			
			return ok(createproject.render(account, project, qpList, inspectors));
		}

		return notFound(errorpage.render(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result searchQP(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String name = requestData.get("name");
    	
		Account account = (Account) ctx().args.get("account");
		Account adminAccount = jpaApi.em().find(Account.class, account.id);
		if (adminAccount.engineer == null) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}else{
			String sql = "SELECT * FROM account ac LEFT JOIN user u ON ac.id=u.account_id "
					+ "WHERE ac.deleted=0 AND ac.blocked=0 AND ac.active=1 AND ac.acc_type=3"
					+ " AND REPLACE(u.name, ' ', '') LIKE '%" + name.trim() + "%' AND ac.company_id=" + adminAccount.engineer.company.id;

			List<Account> accounts = jpaApi.em().createNativeQuery(sql, Account.class).getResultList();
			List<AccountVO> accountVOs = new ArrayList<AccountVO>();
			for(Account acc : accounts){
				AccountVO accVo = new AccountVO(acc);
				accountVOs.add(accVo);
			}
			responseData.data = accountVOs;
		}
		
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result searchInspector(){
		ResponseData responseData = new ResponseData();

		DynamicForm requestData = formFactory.form().bindFromRequest();
		String name = requestData.get("name");
    	
		Account account = (Account) ctx().args.get("account");
		Account adminAccount = jpaApi.em().find(Account.class, account.id);
		if (adminAccount.engineer == null) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}else{
			String sql = "SELECT * FROM account ac LEFT JOIN user u ON ac.id=u.account_id "
					+ "WHERE ac.deleted=0 AND ac.blocked=0 AND ac.active=1 AND ac.acc_type=2"
					+ " AND REPLACE(u.name, ' ', '') LIKE '%" + name.trim() + "%' AND ac.company_id=" + adminAccount.engineer.company.id;

			List<Account> accounts = jpaApi.em().createNativeQuery(sql, Account.class).getResultList();
			List<AccountVO> accountVOs = new ArrayList<AccountVO>();
			for(Account acc : accounts){
				AccountVO accVo = new AccountVO(acc);
				accountVOs.add(accVo);
			}
			
			responseData.data = accountVOs;
		}
		
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result saveProject(){
		ResponseData responseData = new ResponseData();

		DynamicForm requestData = formFactory.form().bindFromRequest();
		String projectId = requestData.get("projectId");
		String projectTitle = requestData.get("title");
		String startDate = requestData.get("startDate");
		String endDate = requestData.get("endDate");
		String gondola = Utils.isBlank(requestData.get("gondola")) ? "0" : requestData.get("gondola");
		String mcwp = Utils.isBlank(requestData.get("mcwp")) ? "0" : requestData.get("mcwp");
		String scaffold = Utils.isBlank(requestData.get("scaffold")) ? "0" : requestData.get("scaffold");
		String formwork = Utils.isBlank(requestData.get("formwork")) ? "0" : requestData.get("formwork");
		String others = Utils.isBlank(requestData.get("others")) ? "0" : requestData.get("others");
		String useCompanyLetterHead = Utils.isBlank(requestData.get("useCompanyLetterHead")) ? "0" : requestData.get("useCompanyLetterHead");
		String clientCompanyName = requestData.get("clientCompanyName");
		String builderCompanyName = requestData.get("builderCompanyName");
		
		try {
			Account account = (Account) ctx().args.get("account");
			Account adminAccount = jpaApi.em().find(Account.class, account.id);
			if (adminAccount.engineer == null) {
				responseData.code = 4000;
				responseData.message = "You do not have permission.";
			}else{
				if(!Utils.isBlank(projectId)) {
					Project project = jpaApi.em().find(Project.class, Long.parseLong(projectId));
					if(project == null) {
						responseData.code = 4000;
						responseData.message = "The project doesn't exist.";
					}else {
						project.title = projectTitle;
						project.startDate = Utils.parse("yyyy-MM-dd", startDate);
						project.endDate = Utils.parse("yyyy-MM-dd", endDate);
						project.isGondola = gondola.equals("1") ? true : false;
						project.isMCWP = mcwp.equals("1") ? true : false;
						project.isScaffold = scaffold.equals("1") ? true : false;
						project.isFormwork = formwork.equals("1") ? true : false;
						project.useLetterHead = useCompanyLetterHead.equals("1") ? true : false;
						jpaApi.em().persist(project);
						return redirect(routes.ProjectController.projectOfEngineer(0));
					}
				}else {
					List<Project> projects = new ArrayList<Project>();
					if(gondola.equals("1")) {
						Project project = new Project(adminAccount.engineer, projectTitle);
						project.startDate = Utils.parse("yyyy-MM-dd", startDate);
						project.endDate = Utils.parse("yyyy-MM-dd", endDate);
						project.useLetterHead = useCompanyLetterHead.equals("1") ? true : false;
						project.isGondola = true;
						jpaApi.em().persist(project);
						projects.add(project);
						
						Project.initQP(project, requestData.data());
						Project.initInspector(project, requestData.data());
					}
					
					if(mcwp.equals("1")) {
						Project project = new Project(adminAccount.engineer, projectTitle);
						project.startDate = Utils.parse("yyyy-MM-dd", startDate);
						project.endDate = Utils.parse("yyyy-MM-dd", endDate);
						project.useLetterHead = useCompanyLetterHead.equals("1") ? true : false;
						project.isMCWP = true;
						jpaApi.em().persist(project);
						projects.add(project);
						
						Project.initQP(project, requestData.data());
						Project.initInspector(project, requestData.data());
					}
					
					if(scaffold.equals("1")) {
						Project project = new Project(adminAccount.engineer, projectTitle);
						project.startDate = Utils.parse("yyyy-MM-dd", startDate);
						project.endDate = Utils.parse("yyyy-MM-dd", endDate);
						project.useLetterHead = useCompanyLetterHead.equals("1") ? true : false;
						project.isScaffold = true;
						jpaApi.em().persist(project);
						projects.add(project);
						
						Project.initQP(project, requestData.data());
						Project.initInspector(project, requestData.data());
					}
					
					if(formwork.equals("1")) {
						Project project = new Project(adminAccount.engineer, projectTitle);
						project.startDate = Utils.parse("yyyy-MM-dd", startDate);
						project.endDate = Utils.parse("yyyy-MM-dd", endDate);
						project.useLetterHead = useCompanyLetterHead.equals("1") ? true : false;
						project.isFormwork = true;
						jpaApi.em().persist(project);
						projects.add(project);
						
						Project.initQP(project, requestData.data());
						Project.initInspector(project, requestData.data());
					}
					
					if(others.equals("1")) {
						Project project = new Project(adminAccount.engineer, projectTitle);
						project.startDate = Utils.parse("yyyy-MM-dd", startDate);
						project.endDate = Utils.parse("yyyy-MM-dd", endDate);
						project.useLetterHead = useCompanyLetterHead.equals("1") ? true : false;
						project.isOthers = true;
						jpaApi.em().persist(project);
						projects.add(project);
						
						Project.initQP(project, requestData.data());
						Project.initInspector(project, requestData.data());
					}
					
					Client.initClient(clientCompanyName, projects, requestData.data());
					Builder.initBuilder(builderCompanyName, projects, requestData.data());
					
					if(projects.size() > 0) {
						Project project = projects.get(0);
						if(project.clients != null && project.clients.size() > 0) {
							for(Client client : project.clients) {
								String link = "http://" + request().host() + "/account/active?token=" + URLEncoder.encode(client.account.token, "UTF-8");
								String htmlBody = "Your account is: " + client.account.email + " and password is: " + client.account.password + " <p><a href='" + link + "'>Click here to active your account!</a></p>";
								CompletableFuture.supplyAsync(() 
										-> MailerService.getInstance().send(client.account.email, "Account Information", htmlBody));
							}
						}
						
						if(project.builders != null && project.builders.size() > 0) {
							for(Builder builder : project.builders) {
								String link = "http://" + request().host() + "/account/active?token=" + URLEncoder.encode(builder.account.token, "UTF-8");
								String htmlBody = "Your account is: " + builder.account.email + " and password is: " + builder.account.password + " <p><a href='" + link + "'>Click here to active your account!</a></p>";
								CompletableFuture.supplyAsync(() 
										-> MailerService.getInstance().send(builder.account.email, "Account Information", htmlBody));
							}
						}
					}
					
				}
				return redirect(routes.ProjectController.projectOfEngineer(0));
			}
		}catch (Exception e) {
			responseData.code = 4001;
			responseData.message = e.getMessage();
		}
		
		return notFound(errorpage.render(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result addClient(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String projectId = requestData.get("projectId");
		String clientEmail = requestData.get("clientEmail");
		String clientPassword = requestData.get("clientPassword");
		String clientName = requestData.get("clientName");
		String clientHpNo = requestData.get("clientHpNo");
		String clientDesignation = requestData.get("clientDesignation");
		String clientCompanyName = requestData.get("clientCompanyName");
		String clientNotify =  Utils.isBlank(requestData.get("clientNotify")) ? "0" : requestData.get("clientNotify");
		
		Account account = (Account) ctx().args.get("account");
		Account engineerAccount = jpaApi.em().find(Account.class, account.id);
		if(engineerAccount == null) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}else {
			if(Utils.isBlank(projectId)) {
				responseData.code = 4000;
				responseData.message = "Missing parameters.";
			}else {
				Project project = jpaApi.em().find(Project.class, Long.parseLong(projectId));
				if(project == null) {
					responseData.code = 4000;
					responseData.message = "Project doesn't exits.";
				}else {
					Client client = null;
					if(!AuthController.notExists(clientEmail)){
			    			client = jpaApi.em().find(Client.class, account.id);
					}else {
						Account clientAccount = new Account(clientEmail, clientPassword);
				    		account.accType = AccountType.CLIENT;
				    		JPA.em().persist(clientAccount);
				    		
				    		String useNotifyStr = Utils.isBlank(clientNotify) ? "0" : clientNotify;
				    		client = new Client(clientAccount);
				    		client.companyName = clientCompanyName;
				    		client.name = clientName;
				    		client.designation = clientDesignation;
				    		client.hpNo = clientHpNo;
				    		client.isNotify = useNotifyStr.equals("1") ? true : false;
					}
					
					client.projects.add(project);
					JPA.em().persist(client);
					responseData.data = client;
				}
			}
		}
		
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result addBuilder(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String projectId = requestData.get("projectId");
		String builderEmail = requestData.get("clientEmail");
		String builderPassword = requestData.get("builderPassword");
		String builderName = requestData.get("builderName");
		String builderHpNo = requestData.get("builderHpNo");
		String builderDesignation = requestData.get("builderDesignation");
		String builderCompanyName = requestData.get("builderCompanyName");
		String builderNotify =  Utils.isBlank(requestData.get("builderNotify")) ? "0" : requestData.get("builderNotify");
		
		Account account = (Account) ctx().args.get("account");
		Account engineerAccount = jpaApi.em().find(Account.class, account.id);
		if(engineerAccount == null) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}else {
			if(Utils.isBlank(projectId)) {
				responseData.code = 4000;
				responseData.message = "Missing parameters.";
			}else {
				Project project = jpaApi.em().find(Project.class, Long.parseLong(projectId));
				if(project == null) {
					responseData.code = 4000;
					responseData.message = "Project doesn't exits.";
				}else {
					Builder builder = null;
					if(!AuthController.notExists(builderEmail)){
						builder = jpaApi.em().find(Builder.class, account.id);
					}else {
						Account builderAccount = new Account(builderEmail, builderPassword);
				    		account.accType = AccountType.CLIENT;
				    		JPA.em().persist(builderAccount);
				    		
				    		String useNotifyStr = Utils.isBlank(builderNotify) ? "0" : builderNotify;
				    		builder = new Builder(builderAccount);
				    		builder.companyName = builderCompanyName;
				    		builder.name = builderName;
				    		builder.designation = builderDesignation;
				    		builder.hpNo = builderHpNo;
				    		builder.isNotify = useNotifyStr.equals("1") ? true : false;
					}
					builder.projects.add(project);
					JPA.em().persist(builder);
					responseData.data = builder;
				}
			}
		}
		
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result removeClient(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String clientId = requestData.get("clientId");
		
		Account account = (Account) ctx().args.get("account");
		Account engineerAccount = jpaApi.em().find(Account.class, account.id);
		if(engineerAccount == null) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}else {
			if(Utils.isBlank(clientId)) {
				responseData.code = 4000;
				responseData.message = "Missing parameters.";
			}else {
				Client client = jpaApi.em().find(Client.class, Long.parseLong(clientId));
				jpaApi.em().remove(client.account);
			}
		}
		
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result removeBuilder(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String builderId = requestData.get("builderId");
		
		Account account = (Account) ctx().args.get("account");
		Account engineerAccount = jpaApi.em().find(Account.class, account.id);
		if(engineerAccount == null) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}else {
			if(Utils.isBlank(builderId)) {
				responseData.code = 4000;
				responseData.message = "Missing parameters.";
			}else {
				Builder builder = jpaApi.em().find(Builder.class, Long.parseLong(builderId));
				jpaApi.em().remove(builder.account);
			}
		}
		
		return ok(Json.toJson(responseData));
	}
	

	@With(AuthAction.class)
	@Transactional
	public Result addTeam(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String projectId = requestData.get("projectId");
		String teamAccountId = requestData.get("teamAccountId");
		
		Account account = (Account) ctx().args.get("account");
		Account engineerAccount = jpaApi.em().find(Account.class, account.id);
		if(engineerAccount == null) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}else {
			if(Utils.isBlank(projectId) || Utils.isBlank(teamAccountId)) {
				responseData.code = 4000;
				responseData.message = "Missing parameters.";
			}else {
				Project project = jpaApi.em().find(Project.class, Long.parseLong(projectId));
				if(project == null) {
					responseData.code = 4000;
					responseData.message = "Project doesn't exist.";
				}else {
					Account teamAccount = jpaApi.em().find(Account.class, Long.parseLong(teamAccountId));
					if(teamAccount == null) {
						responseData.code = 4000;
						responseData.message = "Team account doesn't exist.";
					}else {
						teamAccount.projectsJoined.add(project);
			    			jpaApi.em().persist(teamAccount);
					}
				}
			}
		}
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result removeTeam(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String projectId = requestData.get("projectId");
		String teamAccountId = requestData.get("teamAccountId");
		
		Account account = (Account) ctx().args.get("account");
		Account engineerAccount = jpaApi.em().find(Account.class, account.id);
		if(engineerAccount == null) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}else {
			if(Utils.isBlank(projectId) || Utils.isBlank(teamAccountId)) {
				responseData.code = 4000;
				responseData.message = "Missing parameters.";
			}else {
				Project project = jpaApi.em().find(Project.class, Long.parseLong(projectId));
				if(project == null) {
					responseData.code = 4000;
					responseData.message = "Project doesn't exist.";
				}else {
					Account teamAccount = jpaApi.em().find(Account.class, Long.parseLong(teamAccountId));
					if(teamAccount == null) {
						responseData.code = 4000;
						responseData.message = "Team account doesn't exist.";
					}else {
						teamAccount.projectsJoined.remove(project);
			    			jpaApi.em().persist(teamAccount);
					}
				}
			}
		}
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result archiveProject(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String projectId = requestData.get("projectId");
		
		Account account = (Account) ctx().args.get("account");
		Account engineerAccount = jpaApi.em().find(Account.class, account.id);
		if(Utils.isBlank(projectId)) {
			responseData.code = 4000;
			responseData.message = "Missing Parameter.";
		}else {
			Project project = jpaApi.em().find(Project.class, Long.parseLong(projectId));
			if(project != null) {
				if(project.isValidateAccount(engineerAccount)) {
					project.isArchived = true;
					jpaApi.em().persist(project);
					
				}else {
					responseData.code = 4000;
					responseData.message = "Account is not validated";
				}
			}else {
				responseData.code = 4000;
				responseData.message = "Project doesn't not exist.";
			}
		}
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result projectCompleted(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String projectId = requestData.get("projectId");
		
		Account account = (Account) ctx().args.get("account");
		Account engineerAccount = jpaApi.em().find(Account.class, account.id);
		if(Utils.isBlank(projectId)) {
			responseData.code = 4000;
			responseData.message = "Missing Parameter.";
		}else {
			Project project = jpaApi.em().find(Project.class, Long.parseLong(projectId));
			if(project != null) {
				if(project.isValidateAccount(engineerAccount)) {
					project.status = ProjectStatus.COMPLETE;
					jpaApi.em().persist(project);
					
				}else {
					responseData.code = 4000;
					responseData.message = "Account is not validated";
				}
			}else {
				responseData.code = 4000;
				responseData.message = "Project doesn't not exist.";
			}
		}
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result projectExecution(int offset){
		ResponseData responseData = new ResponseData();
		
		long accountId = ((Account) ctx().args.get("account")).id;
		Account account = jpaApi.em().find(Account.class, accountId);
		if(account == null) {
			responseData.code = 4000;
			responseData.message = "You don't have permission.";
		}else{
			int totalAmount = ((BigInteger) jpaApi.em()
					.createNativeQuery("SELECT COUNT(*) FROM (SELECT pro.id FROM project pro WHERE pro.engineer_id=:engineerId" + 
							" UNION " + 
							"SELECT  pt.project_id as id FROM project_team pt WHERE pt.account_id=:accountId " + 
							"ORDER BY id) as t")
					.setParameter("engineerId", account.id)
					.setParameter("accountId", account.id)
					.getSingleResult()).intValue();
			
			int pageIndex = (int) Math.ceil(offset / Constants.COMPANY_PAGE_SIZE) + 1;
			
			List<String> results = jpaApi.em().createNativeQuery("SELECT pro.id FROM project pro WHERE pro.engineer_id=:engineerId" + 
					" UNION " + 
					"SELECT pt.project_id as id FROM project_team pt WHERE pt.account_id=:accountId " + 
					"ORDER BY id")
					.setParameter("engineerId", account.id)
					.setParameter("accountId", account.id)
					.setFirstResult(offset).setMaxResults(Constants.COMPANY_PAGE_SIZE).getResultList();
			
			String projectIDCause = "";
			for (int i = 0; i < results.size(); i++) {
				if (i == results.size() - 1) {
					projectIDCause += "pro.id='" + ((BigInteger) ((Object)results.get(i))).intValue() + "'";
				} else {
					projectIDCause += "pro.id='" + ((BigInteger) ((Object)results.get(i))).intValue() + "' OR ";
				}
			}
			
			String projectCause = "";
			if(Utils.isBlank(projectIDCause)) {
				projectCause = "pro.engineer_id=" + account.id; 
			}else {
				projectCause = projectIDCause;
			}

			List<Project> projects = jpaApi.em()
					.createNativeQuery("SELECT * FROM project pro WHERE " + projectCause + " AND pro.is_archived = :isArchived", Project.class)
					.setParameter("isArchived", false)
					.setFirstResult(offset).setMaxResults(Constants.COMPANY_PAGE_SIZE).getResultList();
			return ok(projectexecution.render(account, projects, pageIndex,totalAmount));
		}
		return notFound(errorpage.render(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result drawingFile(long projectId){
		ResponseData responseData = new ResponseData();
		
		Account account = (Account) ctx().args.get("account");
		Account engineerAccount = jpaApi.em().find(Account.class, account.id);
		if(engineerAccount.engineer == null) {
			responseData.code = 4000;
			responseData.message = "You don't have permission.";
		}else{
			Project project = jpaApi.em().find(Project.class, projectId);
			if(project != null) {
				return ok(uploaddrawfile.render(account, project));
			}else {
				responseData.code = 4000;
				responseData.message = "Project doesn't exist.";
			}
		}
		
		return notFound(errorpage.render(responseData));
	}
	 
	
	@With(AuthAction.class)
	@Transactional
	public Result uploadDrawingFile(){
		ResponseData responseData = new ResponseData();
		
		Account account = (Account) ctx().args.get("account");
		Account engineerAccount = jpaApi.em().find(Account.class, account.id);
		if(engineerAccount.engineer == null) {
			responseData.code = 4000;
			responseData.message = "You don't have permission.";
		}else{
			try {
				DynamicForm requestData = formFactory.form().bindFromRequest();
				String projectId = requestData.get("projectId");
				String fileName = requestData.get("fileName") + "";
				String isAdd = requestData.get("isAdd");
				
				Project project = jpaApi.em().find(Project.class, Long.parseLong(projectId));
				if(project != null) {
					MultipartFormData<File> body = request().body().asMultipartFormData();
					FilePart<File> pdfPart = body.getFile("pdfFile");
					
					if(pdfPart != null){
						DrawingFile drawFile = new DrawingFile(project, pdfPart.getFile());
						drawFile.fileName = fileName;
						drawFile.setLocation(requestData.data());
						jpaApi.em().persist(drawFile);
						
						if(!Utils.isBlank(isAdd) && isAdd.equals("true")){
							return redirect(routes.ProjectController.drawingFile(project.id));
						}else{
							return redirect(routes.ProjectController.projectExecution(0));
						}
					}else{
						responseData.code = 4001;
						responseData.message = "PDF file cannot be empty";
					}
				}else {
					responseData.code = 4001;
					responseData.message = "Project doesn't exist.";
				}
			} catch (Exception e) {
				responseData.code = 4000;
				responseData.message = e.getLocalizedMessage();
			} 
		}
		
		return notFound(errorpage.render(responseData));
	}
	
	@Transactional
	public Result showDrawing(String uuid) {
		TypedQuery<DrawingFile> query = jpaApi.em()
				.createQuery("from DrawingFile df where df.uuid = :uuid", DrawingFile.class).setParameter("uuid", uuid);

		InputStream imageStream = null;
		try {
			DrawingFile drawingFile = query.getSingleResult();
			imageStream = drawingFile.download();
		} catch (NoResultException e) {
			imageStream = application.get().classloader().getResourceAsStream(LetterHead.PLACEHOLDER);
		}
		return ok(imageStream);
	}
}



























