package models;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name="company")
public class Company {
	
	@Id
	@GeneratedValue	
	@JsonIgnore
	public long id;
	
	public String name;
	
	@Column(name="une_no")
	public String uenNo;
	
	public String email;
	
	public String mobile;
	
	@Lob
	public String address;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "create_datetime")
	public Date createDatetime; 
	
	@OneToOne(fetch = FetchType.LAZY, mappedBy = "company", cascade = CascadeType.ALL)
	public LetterHead letterHead;
	
	@OneToOne(fetch = FetchType.LAZY, mappedBy = "company", cascade = CascadeType.ALL)
	public Avatar logo;
		
}
