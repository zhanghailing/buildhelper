package models;

import java.util.ArrayList;
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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "issue")
public class Issue {

	@Id
	@GeneratedValue
	public long id;
	
	@Temporal(TemporalType.DATE)
	@Column(name="issue_date")
	public Date issueDate;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "issue_acc_id")
	@JsonIgnore
	public Account issuedBy;
	
	@ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "cos_id")
    public COS cos;
	
	@OneToMany(mappedBy = "issue")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Reject> rejects; //use onetomany to be alternative to onetoone
	
	@OneToMany(mappedBy = "issue")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Approve> approves; //use onetomany to be alternative to onetoone
	
	@Column(name="pass_type")
	public String passType;//approve or reject by inspector
	
	public Issue() {
		this.rejects = new ArrayList<>();
		this.approves = new ArrayList<>();
	}
	public Issue(COS cos, Account issuedBy) {
		this.cos = cos;
		this.issuedBy = issuedBy;
	}
}









