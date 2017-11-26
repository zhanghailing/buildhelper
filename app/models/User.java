package models;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name="user")
public class User{
	@Id
	@GenericGenerator(name = "generator", strategy = "foreign", parameters = @Parameter(name = "property", value = "account"))
	@GeneratedValue(generator = "generator")
	@Column(name = "account_id", unique = true, nullable = false)
	public long accountId;
	
	@OneToOne(fetch = FetchType.LAZY)
	@PrimaryKeyJoinColumn
	public Account account;
	
	public String name;
	
	@Column(name="aler_email1")
	public String alterEmail1;
	
	@Column(name="aler_email2")
	public String alterEmail2;
	
	@Column(name="office_no")
	public String officePhone;
	
	public String mobile;
	
	@Column(name="pe_no")
	public String peNo;
	
	@Column(name="qecp_no")
	public String qecpNo;
	
	@Column(name="is_civil")
	public boolean isCivil;
	
	@Column(name="is_qecp")
	public boolean isQECP;
	
	@Column(name="is_geo")
	public boolean isGeotechnical;
	
	@Column(name="is_electric")
	public boolean isElectric;
	
	@Column(name="is_mechanical")
	public boolean isMechanical;
	
	@OneToMany(mappedBy = "user")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Document> documents;
	
	public User(){}
	
	public User(Account account){
		this.account = account;
		this.isCivil = false;
		this.isQECP = false;
		this.isGeotechnical = false;
		this.isElectric = false;
		this.isMechanical = false;
	}
	
}
