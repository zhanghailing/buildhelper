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
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name="project")
public class Project {

	@Id
	@GeneratedValue	
	public long id;
	
	public String title;
	
	@Temporal(TemporalType.DATE)
	@Column(name="start_date")
	public Date startDate;
	
	@Temporal(TemporalType.DATE)
	@Column(name="end_date")
	public Date endDate;
	
	@Column(name="work_type")
	public WorkType workType;
	
	@Column(name="use_lh")
	public boolean useLetterHead;
	
	@OneToMany(mappedBy = "project")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Client> clients;
	
	@OneToMany(mappedBy = "project")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Builder> builders;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "engineer_id")
	@JsonIgnore
    public Engineer engineer;

}
