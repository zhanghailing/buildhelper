package models;

import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import play.db.jpa.JPA;
import services.MailerService;

@Entity
@Table(name = "notification")
@SuppressWarnings("unchecked")
public class Notification {
	
	@Id
	@GeneratedValue
	public long id;

	public String title;
	
	public String content;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="creation_datetime")
	public Date creationDateTime;
	
	@OneToMany(mappedBy = "notification")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<AccountNotification> accNotifications;
	
	@ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "cos_id")
	public COS cos;
	
	public Notification() {
		this.creationDateTime = new Date();
	}
	
	public Notification(COS cos, String title, String content) {
		this();
		this.cos = cos;
		this.title = title;
		this.content = content;
	}
	
	public static void notifyQPByCOS(COS cos) {
		List<Account> qpList = JPA.em()
				.createNativeQuery("SELECT * FROM account ac LEFT JOIN project_team pt ON pt.account_id=ac.id WHERE pt.project_id=:projectId AND ac.acc_type=:accountType", Account.class)
				.setParameter("projectId", cos.project.id)
				.setParameter("accountType", 3)
				.getResultList();
		for(Account qp : qpList) {
			Notification notification = new Notification(cos, cos.project.title, "Pending to inspect");
			JPA.em().persist(notification);
			
			AccountNotification accNfy = new AccountNotification(qp, notification);
			JPA.em().persist(accNfy);
			
			String htmlBody = "";
			CompletableFuture.supplyAsync(() 
					-> MailerService.getInstance().send(qp.email, "Notification", htmlBody));
		}
	}
	
	public static void notifyInspectorByCOS(COS cos) {
		List<Account> inspectors = JPA.em()
				.createNativeQuery("SELECT * FROM account ac LEFT JOIN project_team pt ON pt.account_id=ac.id WHERE pt.project_id=:projectId AND ac.acc_type=:accountType", Account.class)
				.setParameter("projectId", cos.project.id)
				.setParameter("accountType", 2)
				.getResultList();
		for(Account inspector : inspectors) {
			Notification notification = new Notification(cos, cos.project.title, "Pending to inspect");
			JPA.em().persist(notification);
			
			AccountNotification accNfy = new AccountNotification(inspector, notification);
			JPA.em().persist(accNfy);
		}
	}
	
	public static void notifyBuilderByCOS(COS cos, String message) {
		List<Builder> builders = JPA.em()
				.createNativeQuery("SELECT * FROM builder bl WHERE bl.project_id=:projectId", Builder.class)
				.setParameter("projectId", cos.project.id)
				.getResultList();
		for(Builder builder : builders) {
			Notification notification = new Notification(cos, cos.project.title, message);
			JPA.em().persist(notification);
			
			AccountNotification accNfy = new AccountNotification(builder.account, notification);
			JPA.em().persist(accNfy);
		}
	}
	
}


//COS 创建之后，通知这个项目的所有inspector
//inspect之后，通知所有builder去issue
//issue之后，通知Builder和builder






