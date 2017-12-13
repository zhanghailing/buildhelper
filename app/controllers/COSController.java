package controllers;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import actions.AuthAction;
import models.Account;
import models.Engineer;
import models.Project;
import models.ResponseData;
import play.Application;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import views.html.*;

public class COSController extends Controller{
	@Inject private FormFactory formFactory;
	@Inject private JPAApi jpaApi;
	@Inject private Provider<Application> application;
	
	@With(AuthAction.class)
	@Transactional
	public Result createCOS(long projectId) {
		ResponseData responseData = new ResponseData();

		Account account = (Account) ctx().args.get("account");
		Engineer engineer = jpaApi.em().find(Engineer.class, account.id);
		if (engineer == null) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}else{
			Project project = jpaApi.em().find(Project.class, projectId);
			if(project != null) {
				List<String> locations = jpaApi.em().createNativeQuery("SELECT df.location FROM drawingfile df WHERE df.project_id = :projectId")
						.setParameter("projectId", project.id)
						.getResultList();
				
				System.out.println("---------> " + locations);
				
				return ok(requestcos.render(project));
			}else {
				responseData.code = 4000;
				responseData.message = "Project doesn't exist.";
			}
		}

		return notFound(errorpage.render(responseData));
	}

}
