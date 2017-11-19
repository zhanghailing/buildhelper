package models;

import java.io.File;
import java.io.IOException;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
@DiscriminatorValue("letter_head")
public class LetterHead extends Image{
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "company_id")
	public Company company;
	
	public LetterHead(){}
	public LetterHead(Company company, File file) throws IOException{
		super(file);
		this.company = company;
	}
	
}
