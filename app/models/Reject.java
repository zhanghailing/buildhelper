package models;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name="reject")
public class Reject {

	@Id
	@GeneratedValue	
	public long id;
	
	@Column(name="reason")
	public String reason;
	
	@OneToOne(mappedBy = "reject")
	public RejectSign rejectSign;
	
	@ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "inspection_id") 
    public Inspection inspection;
	
	@ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "issue_id") 
    public Issue issue;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="creation_datetime")
	public Date creationDateTime;
	
	
	public Reject() {}
	
	public Reject(Inspection inspection, String reason) {
		this.inspection = inspection;
		this.reason = reason;
		this.creationDateTime = new Date();
	}
	
	public Reject(Issue issue, String reason) {
		this.issue = issue;
		this.reason = reason;
		this.creationDateTime = new Date();
	}
}
