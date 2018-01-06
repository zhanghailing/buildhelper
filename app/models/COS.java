package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
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
@Table(name = "cos")
public class COS {
	
	@Id
	@GeneratedValue
	public long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
	@JsonIgnore
    public Project project;
	
	@Column(name="ref_no")
	public String referenceNo;
	
	@Column(name="loc")
	public String location;
	
	@Column(name="extra_loc")
	public String extraLocation;
	
	public String subject;

	@Column(name="grid_line")
	public String gridLine;

	@Column(name="serial_no")
	public String serialNo;
	
	@Column(name="gondola_no")
	public String gondolaNo;
	
	@Column(name="le_reg_no")
	public String leRegistrationNo;
	
	@Column(name="distinctive_no")
	public String distinctiveNo;
	
	@Column(name="gondola_max_length")
	public String gondolaMaxLength;
	
	@Column(name="gondola_max_swl")
	public String gondolaMaxSWL;
	
	@Column(name="cmwp_serial_no")
	public String cmwpSerialNo;
	
	@Column(name="mcwp_max_height")
	public String mcwpMaxHeight;
	
	@Column(name="mcwp_serial_no")
	public String mcwpMaxLength;
	
	@OneToMany(mappedBy = "cos")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Account> routeAccounts;
	
	@OneToMany(mappedBy = "cos")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<COSTerm> cosTerms;
	
	@OneToOne(mappedBy = "cos")
	public Signature signature;
	
	@OneToMany(mappedBy = "cos")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Inspection> inspections;
	
	@OneToMany(mappedBy = "cos")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Issue> issues;
	
	public COS() {
		this.routeAccounts = new ArrayList<>();
		this.cosTerms = new ArrayList<>();
		this.inspections = new ArrayList<>();
		this.issues = new ArrayList<>();
	}
	public COS(Project project, String subject) {
		this.project = project;
		this.subject = subject;
		this.referenceNo = (System.currentTimeMillis()+"").substring(5);
	}
	
}













































