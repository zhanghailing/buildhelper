package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name = "term")
public class Term {

	@Id
	@GeneratedValue
	public long id;
	
	public String subject;
	
	@Column(name="type")
	public TermType termType;
	
	@OneToMany(mappedBy = "term")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<COSTerm> cosTerms;
	
	public Term() {
		this.cosTerms = new ArrayList<>();
	}
	public Term(String subject, TermType type) {
		this.subject = subject;
		this.termType = type;
	}
}














