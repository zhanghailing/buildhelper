package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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
	
	@Column(name="max_height")
	public String workingMaxHeight;
	
	@Column(name="working_serial_no")
	public String workginMaxLength;
}

















