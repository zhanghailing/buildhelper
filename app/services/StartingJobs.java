package services;

import javax.inject.Inject;
import javax.inject.Singleton;

import models.Account;
import models.AccountType;
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
		   
		});
	}
	
}