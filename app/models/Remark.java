package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "remark")
public class Remark {
	
	@Id
	@GeneratedValue
	public long id;
	
	public String remark;
    
    @OneToMany(mappedBy = "remark")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<COSImage> cosImages;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "costerm_id")
	@JsonIgnore
    public COSTerm cosTerm;
    
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id")
	@JsonIgnore
    public Account author;
    
    public Remark() {
    		cosImages = new ArrayList<>();	
    }
    
    public Remark(Account account, COSTerm cosTerm) {
    		this.author = account;
    		this.cosTerm = cosTerm;
    }

}
