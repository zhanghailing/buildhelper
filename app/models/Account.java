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
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
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
	
	@OneToOne(fetch = FetchType.LAZY, mappedBy = "account", cascade = CascadeType.ALL)
	@LazyToOne(LazyToOneOption.NO_PROXY)
	@JsonIgnore
	public Engineer engineer;
	
	@OneToOne(fetch = FetchType.LAZY, mappedBy = "account", cascade = CascadeType.ALL)
	@LazyToOne(LazyToOneOption.NO_PROXY)
	@JsonIgnore
	public Client client;
	
	@OneToOne(fetch = FetchType.LAZY, mappedBy = "account", cascade = CascadeType.ALL)
	@LazyToOne(LazyToOneOption.NO_PROXY)
	@JsonIgnore
	public Builder builder;
	
	@OneToMany(mappedBy = "account")
	@LazyCollection(LazyCollectionOption.EXTRA)
	@JsonIgnore
	public List<Company> companys;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "company_id")
	@JsonIgnore
	public Company company;
	
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "project_team", joinColumns = @JoinColumn(name = "account_id"), inverseJoinColumns = @JoinColumn(name = "project_id"))
	@LazyCollection(LazyCollectionOption.EXTRA)
	@JsonIgnore
    public List<Project> projectsJoined;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cos_id")
	@JsonIgnore
    public COS cos;
	
	@Column(columnDefinition = "boolean default false")
	public boolean blocked;
	
	@Column(columnDefinition = "boolean default false")
	public boolean deleted;
	
	@Column(columnDefinition = "boolean default true")
	public boolean active;
	
	public Account(){}
	
	public Account(String email, String password){
		this.accType = AccountType.CLIENT;
		this.email = email;
		this.password = password;
		this.creationDateTime = new Date();
		this.token = Utils.genernateAcessToken(this.creationDateTime, this.email);
	}
	
	public static Account findByToken(String token){
		try{
			Account account = JPA.em().createQuery("from Account ac where ac.token = :token", Account.class)
					.setParameter("token", token).getSingleResult();
			return account;
		}catch(NoResultException e){
			return null;
		}
	}
}
