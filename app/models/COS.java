package models;

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
	
	@Temporal(TemporalType.DATE)
	@Column(name="issue_date")
	public Date issueDate;
	
	@Temporal(TemporalType.DATE)
	@Column(name="inspect_date")
	public Date inspectDate;
	
	@OneToMany(mappedBy = "cos")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Account> routeAccounts;
	
	@OneToMany(mappedBy = "cos")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<COSTerm> cosTerms;
	
	@OneToOne(mappedBy = "cos")
	public Signature signature;
	
	@Column(name="pass_type")
	public String passType;
	
	public COS() {}
	public COS(Project project, String subject) {
		this.project = project;
		this.subject = subject;
		this.issueDate = new Date();
	}
	
}













































