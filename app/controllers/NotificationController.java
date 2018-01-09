package controllers;

import java.math.BigInteger;
import java.util.List;

import javax.inject.Inject;

import actions.AuthAction;
import models.Account;
import models.Notification;
import models.ResponseData;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import tools.Constants;
import views.html.*;

@SuppressWarnings("unchecked")
public class NotificationController  extends Controller{
	@Inject private FormFactory formFactory;
	@Inject private JPAApi jpaApi;

	@With(AuthAction.class)
	@Transactional
	public Result notifications(int offset) {
		ResponseData responseData = new ResponseData();

		long accountId = ((Account) ctx().args.get("account")).id;
		Account account = jpaApi.em().find(Account.class, accountId);
		if(account == null) {
			responseData.code = 4000;
			responseData.message = "Account doesn't exist.";
		}else {
			int totalAmount = ((BigInteger) jpaApi.em()
					.createNativeQuery("SELECT count(*) FROM notification nf LEFT JOIN acc_notification an ON an.notification_id=nf.id WHERE an.account_id=:accountId")
					.setParameter("accountId", account.id)
					.getSingleResult()).intValue();
			int pageIndex = (int) Math.ceil(offset / Constants.DEFAULT_PAGE_SIZE) + 1;

			List<Notification> notificationList = jpaApi.em()
					.createNativeQuery("SELECT * FROM notification nf LEFT JOIN acc_notification an ON an.notification_id=nf.id WHERE an.account_id=:accountId ORDER BY nf.creation_datetime asc", Notification.class)
					.setParameter("accountId", account.id)
					.setFirstResult(offset)
					.setMaxResults(Constants.DEFAULT_PAGE_SIZE).getResultList();
			
			return ok(notifications.render(account, notificationList, pageIndex, totalAmount));
		}

		return notFound(errorpage.render(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result notificationAmount(int offset) {
		ResponseData responseData = new ResponseData();

		long accountId = ((Account) ctx().args.get("account")).id;
		Account account = jpaApi.em().find(Account.class, accountId);
		if(account == null) {
			responseData.code = 4000;
			responseData.message = "Account doesn't exist.";
		}else {
			int totalAmount = ((Long) jpaApi.em()
					.createNativeQuery("SELECT count(*) FROM notification nf LEFT JOIN acc_notification an ON an.notification_id=nf.id WHERE an.account_id=:accountId")
					.setParameter("accountId", account.id)
					.getSingleResult())
					.intValue();
			return ok(totalAmount+"");
		}

		return ok(Json.toJson(responseData));
	}
}















