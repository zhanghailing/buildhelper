package models;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "subgrid")
public class SubGrid {
	
	@Id
	@GeneratedValue
	public long id;
	
	public String subject;

	@Column(name="grid_line")
	public String gridLine;
	
	@ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "cos_id") 
    public COS cos;
	
	public SubGrid() {}
	
	public SubGrid(COS cos) {
		this.cos = cos;
	}
}
