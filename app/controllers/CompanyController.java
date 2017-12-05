package controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
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
import models.Avatar;
import models.Company;
import models.Engineer;
import models.LetterHead;
import models.ResponseData;
import play.Application;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import services.MailerService;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import tools.Constants;
import tools.Utils;
import views.html.*;

@SuppressWarnings("unchecked")
public class CompanyController extends Controller {
	@Inject
	private FormFactory formFactory;
	@Inject
	private JPAApi jpaApi;
	@Inject
	private Provider<Application> application;
	@Inject
	private MessagesApi messageApi;
	
	@With(AuthAction.class)
	@Transactional
	public Result company() {
		ResponseData responseData = new ResponseData();

		Company cy = null;
		Account account = (Account) ctx().args.get("account");
		if (account.accType != AccountType.ADMIN) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}
		
		try{
			List<Company> companys = jpaApi.em()
				.createNativeQuery("select * from company cy where cy.acc_id=:accId", Company.class)
				.setParameter("accId", account.id).getResultList();
			if(companys.size() > 0) {
				cy = companys.get(0);
			}
		}catch(NoResultException e){
			responseData.code = 4000;
			responseData.message = "Company doesn't exist.";
		}
		
		if(responseData.code != 0){
			return notFound(errorpage.render(responseData));
		}

		return ok(company.render(cy));
	}

	@With(AuthAction.class)
	@Transactional
	public Result assignAccountForCompany() {
		ResponseData responseData = new ResponseData();

		DynamicForm requestData = formFactory.form().bindFromRequest();
		String companyId = requestData.get("companyId");
		String email = requestData.get("email");
		String password = requestData.get("password");

		Company company = jpaApi.em().find(Company.class, Long.parseLong(companyId));
		if (company == null) {
			responseData.code = 4000;
			responseData.message = "Company doesn't exist.";
		} else {
			if (AuthController.notExists(email)) {
				if(company.account != null) {
					company.account.email = email;
					company.account.password = password;
					jpaApi.em().persist(company.account);
				}else {
					Account account = new Account(email, password);
					account.accType = AccountType.ADMIN;
					jpaApi.em().persist(account);

					company.account = account;
					jpaApi.em().persist(company);
					
					CompletableFuture.supplyAsync(() 
							-> MailerService.getInstance()
							.send(email, "Account Information", "Your account is: " + email + " and password is: " + password));
				}
			} else {
				responseData.code = 4000;
				responseData.message = "The email already exist.";
			}
		}

		return ok(Json.toJson(responseData));
	}

	@With(AuthAction.class)
	@Transactional
	public Result companys(int offset) {

		int totalAmount = ((Long) jpaApi.em().createQuery("select count(*) from Company cy").getSingleResult())
				.intValue();
		int pageIndex = (int) Math.ceil(offset / Constants.COMPANY_PAGE_SIZE) + 1;

		List<Company> companyList = jpaApi.em()
				.createQuery("from Company cy order by cy.createDatetime asc", Company.class).setFirstResult(offset)
				.setMaxResults(Constants.COMPANY_PAGE_SIZE).getResultList();

		return ok(companys.render(companyList, pageIndex, totalAmount));
	}

	@With(AuthAction.class)
	@Transactional
	public Result createCompany(long companyId) {
		ResponseData responseData = new ResponseData();

		Account account = (Account) ctx().args.get("account");
		if (account.accType != AccountType.SADMIN) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
			return notFound(errorpage.render(responseData));
		}

		Company company = null;
		if (companyId > 0) {
			company = jpaApi.em().find(Company.class, companyId);
		}

		return ok(createcompany.render(company));
	}

	@With(AuthAction.class)
	@Transactional
	public Result saveCompany() {
		ResponseData responseData = new ResponseData();

		Account account = (Account) ctx().args.get("account");
		if (account.accType != AccountType.SADMIN) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		} else {
			DynamicForm requestData = formFactory.form().bindFromRequest();
			String companyId = requestData.get("companyId");
			String name = requestData.get("name");
			String uenNo = requestData.get("uenNo");
			String email = requestData.get("email");
			String phone = requestData.get("phone");
			String address = requestData.get("address");

			Company company = null;
			if (!Utils.isBlank(companyId)) {
				company = jpaApi.em().find(Company.class, Long.parseLong(companyId));
			} else {
				company = new Company();
			}

			company.address = address;
			company.name = name;
			company.uenNo = uenNo;
			company.email = email;
			company.phone = phone;

			jpaApi.em().persist(company);

			MultipartFormData<File> body = request().body().asMultipartFormData();
			FilePart<File> logoPart = body.getFile("logoImage");
			FilePart<File> letterHead = body.getFile("letterhead");
			try {
				if (logoPart != null && !Utils.isBlank(logoPart.getFilename())) {
					if (company.logo != null) {
						company.logo.deleteThumbnail();
						company.logo.delete();
						jpaApi.em().remove(company.logo);
					}
					Avatar logo = new Avatar(company, logoPart.getFile());
					jpaApi.em().persist(logo);
				}

				if (letterHead != null && !Utils.isBlank(letterHead.getFilename())) {
					if (company.letterHead != null) {
						company.letterHead.deleteThumbnail();
						company.letterHead.delete();
						jpaApi.em().remove(company.letterHead);
					}
					LetterHead lh = new LetterHead(company, letterHead.getFile());
					jpaApi.em().persist(lh);
				}

			} catch (NullPointerException | IOException e) {
				responseData.code = 4000;
				responseData.message = "Logo uplaod failure";
			}
		}

		if (responseData.code != 0) {
			return notFound(errorpage.render(responseData));
		}

		return redirect(routes.CompanyController.companys(0));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result saveCompanyBasic() {
		ResponseData responseData = new ResponseData();

		Account account = (Account) ctx().args.get("account");
		if (account.accType != AccountType.ADMIN) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		} else {
			DynamicForm requestData = formFactory.form().bindFromRequest();
			String companyId = requestData.get("companyId");
			String name = requestData.get("name");
			String uenNo = requestData.get("uenNo");
			String email = requestData.get("email");
			String phone = requestData.get("phone");
			String address = requestData.get("address");
			String useCustomizedLetterHead = Utils.isBlank(requestData.get("useCustomizedLetterHead")) ? "0" : requestData.get("useCustomizedLetterHead");
			
			Company company = null;
			if (!Utils.isBlank(companyId)) {
				company = jpaApi.em().find(Company.class, Long.parseLong(companyId));
				if(company != null){
					company.address = address;
					company.name = name;
					company.uenNo = uenNo;
					company.email = email;
					company.phone = phone;
					company.useCustomizedLetterHead = useCustomizedLetterHead.equals("1") ? true : false;
					jpaApi.em().persist(company);
				}else{
					responseData.code = 4000;
					responseData.message = "The company doesn't exits.";
				}
			} else {
				responseData.code = 4000;
				responseData.message = "Missing parameters.";
			}
		}

		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result uploadLogo(){
		ResponseData responseData = new ResponseData();
		try {
			MultipartFormData<File> body = request().body().asMultipartFormData();
		    FilePart<File> logoPart = body.getFile("logoImage");
			if(logoPart != null){
				DynamicForm requestData = formFactory.form().bindFromRequest();
		    	long companyId = Long.parseLong(requestData.get("companyId"));
		    	
				Company company = jpaApi.em().find(Company.class, companyId);
				if(company == null){
					responseData.message = "The Company cannot be found.";
					responseData.code = 4000;
				}else{
					if (company.logo != null) {
						company.logo.deleteThumbnail();
						company.logo.delete();
						jpaApi.em().remove(company.logo);
					}
					Avatar logo = new Avatar(company, logoPart.getFile());
					jpaApi.em().persist(logo);
					responseData.data = logo;
				}	
			}
		}catch (IOException e) {
			responseData.message = e.getLocalizedMessage();
			responseData.code = 4001;
		}
		return ok(Json.toJson(responseData));
	}

	@Transactional
	public Result showLetterHead(String uuid, boolean isLarge) {
		TypedQuery<LetterHead> query = jpaApi.em()
				.createQuery("from LetterHead lh where lh.uuid = :uuid", LetterHead.class).setParameter("uuid", uuid);

		InputStream imageStream = null;
		try {
			LetterHead letterHead = query.getSingleResult();
			if (isLarge) {
				imageStream = letterHead.download();
			} else {
				imageStream = letterHead.downloadThumbnail();
			}
		} catch (NoResultException e) {
			imageStream = application.get().classloader().getResourceAsStream(LetterHead.PLACEHOLDER);
		}
		return ok(imageStream);
	}

	@Transactional
	public Result showLogo(String uuid, boolean isLarge) {
		TypedQuery<Avatar> query = jpaApi.em().createQuery("from Avatar av where av.uuid = :uuid", Avatar.class)
				.setParameter("uuid", uuid);

		InputStream imageStream = null;
		try {
			Avatar logo = query.getSingleResult();

			if (isLarge) {
				imageStream = logo.download();
			} else {
				imageStream = logo.downloadThumbnail();
			}
		} catch (NoResultException e) {
			imageStream = application.get().classloader().getResourceAsStream(Avatar.DEFAULT_AVATAR);
		}
		return ok(imageStream);
	}

	@With(AuthAction.class)
	@Transactional
	public Result createQPAccount(long qpAccountId) {
		ResponseData responseData = new ResponseData();

		Account account = (Account) ctx().args.get("account");
		if (account.accType != AccountType.ADMIN) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
			return notFound(errorpage.render(responseData));
		}

		Account qpAccount = null;
		if (qpAccountId > 0) {
			qpAccount = jpaApi.em().find(Account.class, qpAccountId);
		}

		return ok(createqpaccount.render(qpAccount));
	}

	@With(AuthAction.class)
	@Transactional
	public Result qpList(int offset) {
		ResponseData responseData = new ResponseData();

		Account account = (Account) ctx().args.get("account");
		if (account.accType != AccountType.ADMIN) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}
		
		Account dbAcc = jpaApi.em().find(Account.class, account.id);

		String companyIDCause = "";
		for (int i = 0; i < dbAcc.companys.size(); i++) {
			if (i == dbAcc.companys.size() - 1) {
				companyIDCause += "ac.company_id='" + dbAcc.companys.get(i).id + "'";
			} else {
				companyIDCause += "ac.company_id='" + dbAcc.companys.get(i).id + "' AND ";
			}
		}

		String countSql = "SELECT COUNT(*) FROM account ac LEFT JOIN company cy ON ac.id=cy.acc_id WHERE ac.deleted=0 AND ac.acc_type=3";
		String sql = "SELECT * FROM account ac LEFT JOIN company cy ON ac.id=cy.acc_id WHERE ac.deleted=0 AND ac.acc_type=3";
		if(!Utils.isBlank(companyIDCause)){
			countSql = "SELECT COUNT(*) FROM account ac LEFT JOIN company cy ON ac.id=cy.acc_id WHERE ac.deleted=0 AND ac.acc_type=3 AND " + companyIDCause;
			sql = "SELECT * FROM account ac LEFT JOIN company cy ON ac.id=cy.acc_id WHERE ac.deleted=0 AND ac.acc_type=3 AND " + companyIDCause;
		}
		
		int totalAmount = ((BigInteger) jpaApi.em().createNativeQuery(countSql).getSingleResult()).intValue();
		int pageIndex = (int) Math.ceil(offset / Constants.COMPANY_PAGE_SIZE) + 1;

		List<Account> qpAccounts = jpaApi.em()
				.createNativeQuery(
						sql,
						Account.class)
				.setFirstResult(offset).setMaxResults(Constants.COMPANY_PAGE_SIZE).getResultList();

		return ok(qplist.render(qpAccounts, pageIndex, totalAmount));
	}

	@With(AuthAction.class)
	@Transactional
	public Result createInspectorAccount(long inspectorAccountId) {
		ResponseData responseData = new ResponseData();

		Account account = (Account) ctx().args.get("account");
		if (account.accType != AccountType.ADMIN) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
			return notFound(errorpage.render(responseData));
		}

		Account inspectorAccount = null;
		if (inspectorAccountId > 0) {
			inspectorAccount = jpaApi.em().find(Account.class, inspectorAccountId);
		}

		return ok(createinspectoraccount.render(inspectorAccount));
	}

	@With(AuthAction.class)
	@Transactional
	public Result inspectors(int offset) {
		ResponseData responseData = new ResponseData();

		Account account = (Account) ctx().args.get("account");
		if (account.accType != AccountType.ADMIN) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}

		Account dbAcc = jpaApi.em().find(Account.class, account.id);
		String companyIDCause = "";
		for (int i = 0; i < dbAcc.companys.size(); i++) {
			if (i == dbAcc.companys.size() - 1) {
				companyIDCause += "ac.company_id='" + dbAcc.companys.get(i).id + "'";
			} else {
				companyIDCause += "ac.company_id='" + dbAcc.companys.get(i).id + "' AND ";
			}
		}

		String countSql = "SELECT COUNT(*) FROM account ac LEFT JOIN company cy ON ac.id=cy.acc_id WHERE ac.deleted=0 AND ac.acc_type=2";
		String sql = "SELECT * FROM account ac LEFT JOIN company cy ON ac.id=cy.acc_id WHERE ac.deleted=0 AND ac.acc_type=2";
		if(!Utils.isBlank(companyIDCause)){
			countSql = "SELECT COUNT(*) FROM account ac LEFT JOIN company cy ON ac.id=cy.acc_id WHERE ac.deleted=0 AND ac.acc_type=2 AND " + companyIDCause;
			sql = "SELECT * FROM account ac LEFT JOIN company cy ON ac.id=cy.acc_id WHERE ac.deleted=0 AND ac.acc_type=2 AND " + companyIDCause;
		}

		int totalAmount = ((BigInteger) jpaApi.em().createNativeQuery(countSql).getSingleResult()).intValue();
		int pageIndex = (int) Math.ceil(offset / Constants.COMPANY_PAGE_SIZE) + 1;

		List<Account> inpectors = jpaApi.em()
				.createNativeQuery(
						sql,
						Account.class)
				.setFirstResult(offset).setMaxResults(Constants.COMPANY_PAGE_SIZE).getResultList();

		return ok(inspectors.render(inpectors, pageIndex, totalAmount));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result engineers(int offset) {
		ResponseData responseData = new ResponseData();

		Account account = (Account) ctx().args.get("account");
		if (account.accType != AccountType.ADMIN) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}else{

			int totalAmount = 0;
			int pageIndex = 1;

			List<Account> engineerList = null;
			return ok(engineers.render(engineerList, pageIndex, totalAmount));
		}
		return notFound(errorpage.render(responseData)); 
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result searchAccount(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String name = requestData.get("name");
    	
		Account account = (Account) ctx().args.get("account");
		if (account.accType != AccountType.ADMIN) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}else{
			Account adminAccount = jpaApi.em().find(Account.class, account.id);
			if (adminAccount != null) {
				String companyIDCause = "";
				for (int i = 0; i < adminAccount.companys.size(); i++) {
					if (i == adminAccount.companys.size() - 1) {
						companyIDCause += "ac.company_id='" + adminAccount.companys.get(i).id + "'";
					} else {
						companyIDCause += "ac.company_id='" + adminAccount.companys.get(i).id + "' AND ";
					}
				}

				String sql = "SELECT * FROM account ac LEFT JOIN user u ON ac.id=u.account_id "
						+ "WHERE ac.deleted=0 AND ac.blocked=0 AND ac.active=1 AND (ac.acc_type=3 OR ac.acc_type=2)"
						+ " AND REPLACE(u.name, ' ', '') LIKE '%" + name.trim() + "%'";
				if(!Utils.isBlank(companyIDCause)){
					sql = "SELECT * FROM account ac LEFT JOIN user u ON ac.id=u.account_id "
							+ "WHERE ac.deleted=0 AND ac.blocked=0 AND ac.active=1 AND (ac.acc_type=3 OR ac.acc_type=2)"
							+ " AND REPLACE(u.name, ' ', '') LIKE '%" + name.trim() + "%' AND " + companyIDCause;
				}

				List<Account> accounts = jpaApi.em().createNativeQuery(sql, Account.class).getResultList();
				List<Engineer> engineers = jpaApi.em().createQuery("FROM Engineer e WHERE e.company = :company", Engineer.class)
						.setParameter("company", adminAccount.companys.get(0))
						.getResultList();				
				
				List<Account> accountAdded = new ArrayList<Account>();
				for(Account acc : accounts){
					for(Engineer eng : engineers){
						if(eng.accountId == acc.id){
							accountAdded.add(acc);
							break;
						}
					}
				}
				accounts.removeAll(accountAdded);
				
				List<AccountVO> accountVOs = new ArrayList<AccountVO>();
				for(Account acc : accounts){
					AccountVO accVo = new AccountVO(acc);
					accountVOs.add(accVo);
				}
				responseData.data = accountVOs;
			}else{
				responseData.code = 4000;
				responseData.message = "Account cannot be found.";
			}
		}
		
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result addEngineerPage(){
		ResponseData responseData = new ResponseData();

		Account account = (Account) ctx().args.get("account");
		if (account.accType != AccountType.ADMIN) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}else{
			Account adminAccount = jpaApi.em().find(Account.class, account.id);
			if (adminAccount != null) {
				String companyIDCause = "";
				for (int i = 0; i < adminAccount.companys.size(); i++) {
					if (i == adminAccount.companys.size() - 1) {
						companyIDCause += "ac.company_id='" + adminAccount.companys.get(i).id + "'";
					} else {
						companyIDCause += "ac.company_id='" + adminAccount.companys.get(i).id + "' AND ";
					}
				}

				String sql = "SELECT * FROM account ac LEFT JOIN company cy ON ac.id=cy.acc_id WHERE ac.deleted=0 AND ac.blocked=0 AND ac.active=1 AND (ac.acc_type=3 OR ac.acc_type=2)";
				if(!Utils.isBlank(companyIDCause)){
					sql = "SELECT * FROM account ac LEFT JOIN company cy ON ac.id=cy.acc_id WHERE ac.deleted=0 AND ac.blocked=0 AND ac.active=1 AND (ac.acc_type=3 OR ac.acc_type=2) AND " + companyIDCause;
				}

				List<Account> accounts = jpaApi.em().createNativeQuery(sql, Account.class).getResultList();
				
				List<Engineer> engineers = jpaApi.em().createQuery("FROM Engineer e WHERE e.company = :company", Engineer.class)
						.setParameter("company", adminAccount.companys.get(0))
						.getResultList();				
				
				List<Account> accountAdded = new ArrayList<Account>();
				for(Account acc : accounts){
					for(Engineer eng : engineers){
						if(eng.accountId == acc.id){
							accountAdded.add(acc);
							break;
						}
					}
				}
				
				accounts.removeAll(accountAdded);
				return ok(createngineer.render(accounts, engineers));
			}else{
				responseData.code = 4000;
				responseData.message = "Account cannot be found.";
			}
		}

		return notFound(errorpage.render(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result addEngineer(){
		ResponseData responseData = new ResponseData();

		Account account = (Account) ctx().args.get("account");
		if (account.accType != AccountType.ADMIN) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}else{
			DynamicForm requestData = formFactory.form().bindFromRequest();
	    	long accountId = Long.parseLong(requestData.get("accountId"));
			Account engineerAccount = jpaApi.em().find(Account.class, accountId);
			if (engineerAccount != null) {
				List<Company> companys = jpaApi.em()
						.createNativeQuery("select * from company cy where cy.acc_id=:accId", Company.class)
						.setParameter("accId", account.id).getResultList();
					if(companys.size() > 0) {
						Company cy = companys.get(0);
						Engineer engineer = new Engineer(engineerAccount, cy);
						jpaApi.em().persist(engineer);
						
						AccountVO accVO = new AccountVO(engineerAccount);
						responseData.data = accVO;
					}else{
						responseData.code = 4000;
						responseData.message = "Company cannot be found.";
					}
			}else{
				responseData.code = 4000;
				responseData.message = "Account cannot be found.";
			}
		}

		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result removeEngineer(){
		ResponseData responseData = new ResponseData();

		Account account = (Account) ctx().args.get("account");
		if (account.accType != AccountType.ADMIN) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}else{
			DynamicForm requestData = formFactory.form().bindFromRequest();
	    	long accountId = Long.parseLong(requestData.get("accountId"));
	    	Account engineerAccount = jpaApi.em().find(Account.class, accountId);
	    	
			Engineer engineer = jpaApi.em().find(Engineer.class, accountId);
			if (engineer != null) {
				AccountVO accVO = new AccountVO(engineerAccount);
				responseData.data = accVO;
				engineerAccount.engineer = null;
				jpaApi.em().remove(engineer);
			}else{
				responseData.code = 4000;
				responseData.message = "Engineer cannot be found.";
			}
		}
		return ok(Json.toJson(responseData));
	}

}





































