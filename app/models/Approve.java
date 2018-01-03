package models;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name="approve")
public class Approve {

	@Id
	@GeneratedValue	
	public long id;
	
	@Lob
	@Column(name="reason")
	public String reason;
	
	@Lob
	@Column(name="comment")
	public String comment;
	
	@OneToOne(mappedBy = "approve")
	public ApproveSign approveSign;
	
	@ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "cos_id") 
    public COS cos;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="creation_datetime")
	public Date creationDateTime;
	
	@Temporal(TemporalType.DATE)
	@Column(name="approve_date")
	public Date approveDate;
	
	public Approve() {}
	
	public Approve(COS cos, String reason) {
		this.cos = cos;
		this.reason = reason;
		this.creationDateTime = new Date();
	}	
	
}
