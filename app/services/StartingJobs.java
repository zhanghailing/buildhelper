package services;

import javax.inject.Inject;
import javax.inject.Singleton;

import models.Account;
import models.AccountType;
import models.Term;
import models.TermType;
import play.db.jpa.JPAApi;
import javax.inject.Provider;
import play.Application;

@Singleton
public class StartingJobs {
	
	@Inject
	public StartingJobs(Provider<Application> app, JPAApi jpaApi) {
		jpaApi.withTransaction(() -> { 
			
			int superAdminCount = ((Long)jpaApi.em()
					.createQuery("select count(*) from Account ac where ac.accType = :accType")
					.setParameter("accType", AccountType.SADMIN).getSingleResult()).intValue();

			if(superAdminCount == 0){
				Account superAdmin = new Account("niu2yue@gmail.com", "111111");
				superAdmin.accType = AccountType.SADMIN;
				jpaApi.em().persist(superAdmin);
			}
			
			int termCount = ((Long)jpaApi.em().createQuery("select count(*) from Term tm").getSingleResult()).intValue();
			if(termCount == 0) {
				Term termGen1 = new Term("Formwork erected and supervised by a competent person.", TermType.GENTERAL);
				jpaApi.em().persist(termGen1);
				
				Term termGen2 = new Term("Structure endorsed by the Professional Engineer (if designed by him) before concreting works commences.", TermType.GENTERAL);
				jpaApi.em().persist(termGen2);
				
				Term termGen3 = new Term("Structure has been inspected by designated person during erecion; during concreting and after concreting words. Details of such inspections are recorded.", TermType.GENTERAL);
				jpaApi.em().persist(termGen3);
				
				Term termGen4 = new Term("Material used is adequate in strength.", TermType.FORMWORK);
				jpaApi.em().persist(termGen4);
				
				Term termGen5 = new Term("Formwork constructed in accordance to design.", TermType.FORMWORK);
				jpaApi.em().persist(termGen5);
			}
		   
		});
	}
	
}