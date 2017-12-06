package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name="doc_format")
public class DocFormat {
	
	@Id
	@GeneratedValue	
	@JsonIgnore
	public long id;
	
	public String uuid;
	
	public String type;
	
	@Column(name="work_type")
	public String workType;
	
	public String department;
	
}
