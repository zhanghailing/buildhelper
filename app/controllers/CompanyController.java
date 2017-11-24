package controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

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
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.i18n.MessagesApi;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import tools.Constants;
import tools.Utils;
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

		Company company = null;
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String companyId = requestData.get("companyId");
		if(!Utils.isBlank(companyId)){
			company = jpaApi.em().find(Company.class, companyId);
		}
		
		return ok(createcompany.render(company));
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
			String uenNo = requestData.get("uenNo");
			String email = requestData.get("email");
			String phone = requestData.get("phone");
			String address = requestData.get("address");
			
			Company company = new Company();
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
				Avatar logo = new Avatar(company, logoPart.getFile());
				jpaApi.em().persist(logo);
				
				LetterHead lh = new LetterHead(company, letterHead.getFile());
				jpaApi.em().persist(lh);
			} catch (NullPointerException | IOException e) {
				responseData.code = 4000;
				responseData.message = "Logo uplaod failure";
			}
		}
		
		if(responseData.code != 0) {
			return notFound(errorpage.render(responseData));
		}
		
		return redirect(routes.CompanyController.companys(0));
	}
	
	@Transactional
	public Result showLetterHead(String uuid, boolean isLarge){
		TypedQuery<LetterHead> query = jpaApi.em()
				.createQuery("from LetterHead lh where lh.uuid = :uuid", LetterHead.class)
				.setParameter("uuid", uuid);
		
		InputStream imageStream = null;
		try{
			LetterHead letterHead = query.getSingleResult();
			if(isLarge){
				imageStream = letterHead.download();
			}else{
				imageStream = letterHead.downloadThumbnail();
			}
		}catch(NoResultException e){
			imageStream = application.get().classloader().getResourceAsStream(LetterHead.PLACEHOLDER);
		}
		return ok(imageStream);
	}
	
	@Transactional
	public Result showLogo(String uuid, boolean isLarge){
		TypedQuery<Avatar> query = jpaApi.em()
				.createQuery("from Avatar lh where av.uuid = :uuid", Avatar.class)
				.setParameter("uuid", uuid);
		
		InputStream imageStream = null;
		try{
			Avatar logo = query.getSingleResult();
			if(isLarge){
				imageStream = logo.download();
			}else{
				imageStream = logo.downloadThumbnail();
			}
		}catch(NoResultException e){
			imageStream = application.get().classloader().getResourceAsStream(Avatar.DEFAULT_AVATAR);
		}
		return ok(imageStream);
	}
	
}









