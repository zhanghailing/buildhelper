package models;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "acc_notification")
public class AccountNotification {
	
	@Id
	@GeneratedValue
	public long id;
	
	@ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "account_id") 
    public Account account;
	
	@ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "notification_id")
    public Notification notification;
	
	@Column(name="is_read", columnDefinition = "boolean default false")
	public boolean isRead;
	
	public AccountNotification() {}
	
	public AccountNotification(Account account, Notification notification) {
		this.account = account;
		this.notification = notification;
	}
}

















