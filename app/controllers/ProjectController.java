package controllers;

import java.math.BigInteger;
import java.util.List;

import javax.inject.Inject;

import actions.AuthAction;
import models.Account;
import models.AccountType;
import models.Engineer;
import models.Project;
import models.ResponseData;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
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
	public Result projectAdmin(int offset){
		ResponseData responseData = new ResponseData();

		Account account = (Account) ctx().args.get("account");
		Engineer engineer = jpaApi.em().find(Engineer.class, account.id);
		if (engineer == null) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}else{
			int totalAmount = ((BigInteger) jpaApi.em()
					.createNativeQuery("SELECT COUNT(*) FROM project pro WHERE pro.engineer_id = :engineerId")
					.setParameter("engineerId", engineer.accountId)
					.getSingleResult()).intValue();
			int pageIndex = (int) Math.ceil(offset / Constants.COMPANY_PAGE_SIZE) + 1;

			List<Project> projects = jpaApi.em()
					.createNativeQuery("SELECT * FROM project pro WHERE pro.engineer_id=:engineerId", Engineer.class)
					.setParameter("engineerId", engineer.accountId)
					.setFirstResult(offset).setMaxResults(Constants.COMPANY_PAGE_SIZE).getResultList();
			return ok(projectadmin.render(projects, pageIndex,totalAmount));
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
}








