package controllers;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.NoResultException;

import ModelVOs.AccountVO;
import actions.AuthAction;
import models.Account;
import models.AccountType;
import models.Builder;
import models.Client;
import models.Company;
import models.Engineer;
import models.Project;
import models.ProjectStatus;
import models.ResponseData;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import tools.Constants;
import tools.Utils;
import views.html.*;

@SuppressWarnings("unchecked")
public class ProjectController extends Controller{
	@Inject private FormFactory formFactory;
	@Inject private JPAApi jpaApi;
	
	@With(AuthAction.class)
	@Transactional
	public Result projectOfEngineer(int offset){
		ResponseData responseData = new ResponseData();

		Account account = (Account) ctx().args.get("account");
		Engineer engineer = jpaApi.em().find(Engineer.class, account.id);
		if (engineer == null) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}else{
			int totalAmount = ((BigInteger) jpaApi.em()
					.createNativeQuery("SELECT COUNT(*) FROM project pro WHERE pro.engineer_id = :engineerId")
					.setParameter("engineerId", engineer.account.id)
					.getSingleResult()).intValue();
			int pageIndex = (int) Math.ceil(offset / Constants.COMPANY_PAGE_SIZE) + 1;

			List<Project> projects = jpaApi.em()
					.createNativeQuery("SELECT * FROM project pro WHERE pro.engineer_id=:engineerId AND pro.is_archived = :isArchived", Project.class)
					.setParameter("engineerId", engineer.account.id)
					.setParameter("isArchived", false)
					.setFirstResult(offset).setMaxResults(Constants.COMPANY_PAGE_SIZE).getResultList();
			return ok(projectofengineer.render(projects, pageIndex,totalAmount));
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
				Company company = jpaApi.em().createQuery("FROM Company cy WHERE cy.acc_id = :accountId", Company.class)
						.setParameter("accountId", account.id)
						.getSingleResult();
				
				List<Engineer> engineers = jpaApi.em()
						.createQuery("FROM Engineer eng LEFT JOIN Account ac ON ac.id = eng.accountId WHERE eng.company = :company AND ac.blocked = :blocked AND ac.deleted = :deleted AND ac.active = :active", Engineer.class)
						.setParameter("company", company)
						.setParameter("blocked", false)
						.setParameter("deleted", false)
						.setParameter("active", true)
						.getResultList();
				
				String engineerIDCause = "";
				for (int i = 0; i < engineers.size(); i++) {
					if (i == engineers.size() - 1) {
						engineerIDCause += "pro.engineer_id='" + engineers.get(i).accountId + "'";
					} else {
						engineerIDCause += "pro.engineer_id='" + engineers.get(i).accountId + "' AND ";
					}
				}
				
				String countSql = "SELECT COUNT(*) FROM project pro";
				String sql = "SELECT * FROM project pro";
				if(!Utils.isBlank(engineerIDCause)){
					if(engineerIDCause.contains("AND")) {
						engineerIDCause = engineerIDCause.substring(0, engineerIDCause.length() - 5);
					}
					countSql = countSql + " WHERE " + engineerIDCause;
					sql = sql + " WHERE " + engineerIDCause;
				}
				
				int totalAmount = ((BigInteger) jpaApi.em().createNativeQuery(countSql).getSingleResult()).intValue();
				int pageIndex = (int) Math.ceil(offset / Constants.COMPANY_PAGE_SIZE) + 1;
				
				List<Project> projects = jpaApi.em().createNativeQuery(sql, Project.class)
						.setFirstResult(offset).setMaxResults(Constants.COMPANY_PAGE_SIZE).getResultList();
				
				return ok(projectofcompany.render(projects, pageIndex, totalAmount));
			}catch(NoResultException e) {
				responseData.code = 4000;
				responseData.message = "Cannot find the company.";
			}
		}
		return notFound(errorpage.render(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result createProject() {
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
			
			return ok(createproject.render(engineer, qpList, inspectors));
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
		String projectTitle = requestData.get("title");
		String startDate = requestData.get("startDate");
		String endDate = requestData.get("endDate");
		String gondola = Utils.isBlank(requestData.get("gondola")) ? "0" : requestData.get("gondola");
		String mcwp = Utils.isBlank(requestData.get("mcwp")) ? "0" : requestData.get("mcwp");
		String scaffold = Utils.isBlank(requestData.get("scaffold")) ? "0" : requestData.get("scaffold");
		String formwork = Utils.isBlank(requestData.get("formwork")) ? "0" : requestData.get("formwork");
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
				Project project = new Project(adminAccount.engineer, projectTitle);
				project.startDate = Utils.parse("yyyy-MM-dd", startDate);
				project.endDate = Utils.parse("yyyy-MM-dd", endDate);
				project.isGondola = gondola.equals("1") ? true : false;
				project.isMCWP = mcwp.equals("1") ? true : false;
				project.isScaffold = scaffold.equals("1") ? true : false;
				project.isFormwork = formwork.equals("1") ? true : false;
				project.useLetterHead = useCompanyLetterHead.equals("1") ? true : false;
				jpaApi.em().persist(project);
				
				Client.initClient(clientCompanyName, project, requestData.data());
				Builder.initBuilder(builderCompanyName, project, requestData.data());
				Project.initQP(project, requestData.data());
				Project.initInspector(project, requestData.data());
				
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
	
}



























