package models;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name = "cos_term")
public class COSTerm {
	@Id
	@GeneratedValue
	public long id;
	
	@ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "cos_id") 
    public COS cos;
	
	@ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "term_id")
    public Term term;
	
    @Column(nullable = false, columnDefinition = "int default 1")
    public int value;
    
    public String remark;
    
    @OneToMany(mappedBy = "cosTerm")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<COSImage> cosImages;
    
    public COSTerm(){}
    
    public COSTerm(COS cos, Term term){
	    	this.cos = cos;
	    	this.term = term;
    }
    
    
}
