package controllers;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import actions.AuthAction;
import models.Account;
import models.AccountType;
import models.Avatar;
import models.Company;
import models.LetterHead;
import models.ResponseData;
import play.Application;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.cache.CacheApi;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.i18n.MessagesApi;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import tools.Constants;
import views.html.*;

public class CompanyController extends Controller{
	@Inject private FormFactory formFactory;
	@Inject private JPAApi jpaApi;
	@Inject private Provider<Application> application;
	@Inject private MessagesApi messageApi; 
	
	@With(AuthAction.class)
	@Transactional
    public Result companys(int offset) {
		
		int totalAmount = ((Long)jpaApi.em()
				.createQuery("select count(*) from Company cy").getSingleResult()).intValue();
		int pageIndex = (int) Math.ceil(offset / Constants.COMPANY_PAGE_SIZE) + 1;
		
		List<Company> companyList = jpaApi.em()
				.createQuery("from Company cy order by cy.createDatetime asc", Company.class)
				.setFirstResult(offset)
				.setMaxResults(Constants.COMPANY_PAGE_SIZE)
				.getResultList();
		
		return ok(companys.render(companyList, pageIndex, totalAmount));
    }   
	
	@With(AuthAction.class)
	@Transactional
	public Result createCompany() {
		ResponseData responseData = new ResponseData();
		
		Account account = (Account) ctx().args.get("account");
		if(account.accType != AccountType.SADMIN) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
			return notFound(errorpage.render(responseData));
		}
		
		return ok(createcompany.render());
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result saveCompany() {
		ResponseData responseData = new ResponseData();
			
		Account account = (Account) ctx().args.get("account");
		if(account.accType != AccountType.SADMIN) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}else {
			DynamicForm requestData = formFactory.form().bindFromRequest();
			String name = requestData.get("name");
			String uenNo = requestData.get("uenno");
			String email = requestData.get("email");
			String mobile = requestData.get("mobile");
			String address = requestData.get("address");
			
			Company company = new Company();
			company.address = address;
			company.name = name;
			company.uenNo = uenNo;
			company.email = email;
			company.mobile = mobile;
			
			jpaApi.em().persist(company);
			
			MultipartFormData<File> body = request().body().asMultipartFormData();
		    FilePart<File> avatar = body.getFile("avatar"); 
		    FilePart<File> letterHead = body.getFile("letterHead"); 
		    
			try {
				Avatar logo = new Avatar(company, avatar.getFile());
				jpaApi.em().persist(logo);
				
				LetterHead lh = new LetterHead(company, letterHead.getFile());
				jpaApi.em().persist(lh);
			} catch (IOException e) {
				responseData.code = 4000;
				responseData.message = "Logo uplaod failure";
			}
		}
		
		if(responseData.code != 0) {
			return notFound(errorpage.render(responseData));
		}
		
		return redirect(routes.CompanyController.companys(0));
	}
	
}









