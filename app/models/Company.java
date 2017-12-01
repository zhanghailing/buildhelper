package models;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name="company")
public class Company {
	
	@Id
	@GeneratedValue	
	public long id;
	
	public String name;
	
	@Column(name="une_no")
	public String uenNo;
	
	public String email;
	
	public String phone;
	
	@Lob
	public String address;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "create_datetime")
	public Date createDatetime; 
	
	@OneToOne(mappedBy = "company")
	public LetterHead letterHead;
	
	@OneToOne(mappedBy = "company")
	public Avatar logo;
	
	@Column(name="use_cus_lh", columnDefinition = "boolean default false")
	public boolean useCustomizedLetterHead;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "acc_id")
	@JsonIgnore
	public Account account;
	
	@OneToMany(mappedBy = "company")
	@LazyCollection(LazyCollectionOption.EXTRA)
	@JsonIgnore
	public List<Account> qpAccounts;
	
	public Company(){
		this.useCustomizedLetterHead = false;
	}
}
