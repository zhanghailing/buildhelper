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
@Table(name = "inspection")
public class Inspection {

	@Id
	@GeneratedValue
	public long id;
	
	@Temporal(TemporalType.DATE)
	@Column(name="inspect_date")
	public Date inspectDate;
	
	@Column(name="pass_type")
	public String passType;//approve or reject by inspector
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "inspect_acc_id")
	@JsonIgnore
	public Account inspectedBy;
	
	@ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "cos_id")
    public COS cos;
	
	@OneToMany(mappedBy = "inspection")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Reject> rejects; //use onetomany to be alternative to onetoone
	
	@OneToMany(mappedBy = "inspection")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Approve> approves; //use onetomany to be alternative to onetoone
	
	public Inspection() {
		this.rejects = new ArrayList<>();
		this.approves = new ArrayList<>();
	}
	public Inspection(COS cos, Account inspectedBy) {
		this.cos = cos;
		this.inspectedBy = inspectedBy;
	}
}
