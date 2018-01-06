package models;

import java.util.Date;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "notification")
public class Notification {
	
	@Id
	@GeneratedValue
	public long id;

//	public String projectTitle;
//	
//	public String content;
//	
//	public Date dateDatetime;
//	
//	@ManyToOne
//	public COS cos;
//	
//	public List<Account> inspectors;// COS created
//	
//	public List<Account> qps;//pending to issue
//	
//	public List<Account> builders;//issued
//	
//	public Reject inspectedReject;// send to builder
//	
//	public Reject issuedReject; // send to inspector and builder
//	
//	public String action; 
}








