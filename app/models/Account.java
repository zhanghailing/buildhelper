package models;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

import com.fasterxml.jackson.annotation.JsonIgnore;

import play.db.jpa.JPA;
import tools.Utils;

@Entity
@Table(name="account")
public class Account{
	@Transient
	@JsonIgnore
	public static final String EMAIL_PREFIX = "EMAIL_";
	
	@Id
	@GeneratedValue	
	@JsonIgnore
	public long id;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="creation_datetime")
	public Date creationDateTime;	
	
	public String email;
	
	public String password;
	
	public String token;
	
	@Column(name="acc_type")
	public AccountType accType;
	
	@OneToOne(fetch = FetchType.LAZY, mappedBy = "account", cascade = CascadeType.ALL)
	@LazyToOne(LazyToOneOption.NO_PROXY)
	@JsonIgnore
	public User user;
	
	@OneToMany(mappedBy = "account")
	@LazyCollection(LazyCollectionOption.EXTRA)
	@JsonIgnore
	public List<Company> companys;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "company_id")
	public Company company;
	
	public boolean blocked;
	
	public Account(){}
	
	public Account(String email, String password){
		this.accType = AccountType.CLIENT;
		this.email = email;
		this.password = password;
		this.creationDateTime = new Date();
		this.token = Utils.genernateAcessToken(this.creationDateTime, this.email);
		this.blocked = false;
	}
	
	public static Account findByToken(String token){
		Account account = null;
		try{
			account = JPA.em().createQuery("from Account ac where ac.token = :token", Account.class)
					.setParameter("token", token).getSingleResult();
		}catch(NoResultException e){
			e.printStackTrace();
		}
		return account;
	}
}
