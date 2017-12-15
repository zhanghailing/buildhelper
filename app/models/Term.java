package models;

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
@Table(name = "term")
public class Term {

	@Id
	@GeneratedValue
	public long id;
	
	public String subject;
	
	@OneToMany(mappedBy = "term")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<COSImage> cosImages;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cos_id")
	@JsonIgnore
    public COS cos;
	
	public TermType Type;
	
	@OneToMany(mappedBy = "term")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<COSTerm> cosTerms;
	
	public Term() {}
	public Term(String subject, TermType type) {
		this.subject = subject;
		this.Type = type;
	}
}














