package controllers;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import actions.AuthAction;
import models.Company;
import play.Application;
import play.cache.CacheApi;
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
	@Inject private CacheApi cache;
	@Inject private JPAApi jpaApi;
	@Inject private Provider<Application> application;
	@Inject private MessagesApi messagesApi;
	
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
	
	
}
