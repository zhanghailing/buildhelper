package models;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

import play.db.jpa.JPA;
import tools.Utils;

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
	
	@Column(name="is_gondola")
	public boolean isGondola;
	
	@Column(name="is_mcwp")
	public boolean isMCWP;
	
	@Column(name="is_scaffold")
	public boolean isScaffold;
	
	@Column(name="is_formwork")
	public boolean isFormwork;
	
	@Column(name="use_lh")
	public boolean useLetterHead;
	
	@OneToMany(mappedBy = "project")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Client> clients;
	
	@OneToMany(mappedBy = "project")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Builder> builders;
	
	@OneToMany(mappedBy = "project")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Account> teamAccounts;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "engineer_id")
	@JsonIgnore
    public Engineer engineer;
	
	public Project() {}
	public Project(Engineer engineer, String title) {
		this.engineer = engineer;
		this.title = title;
	}
	
	public static void initQP(Project project, Map<String, String> data) throws ParseException{
		Iterator<String> iterator = data.keySet().iterator();
		Map<Integer, String> qpAccountMap = new HashMap<>();
	    
	    String key;
	    while(iterator.hasNext()){
		    	key = iterator.next();
		    	if(key.contains("qpAccounts")){
		    		int startIdx = key.indexOf("[") + 1;
		    		int endIdx = key.indexOf("]");
		    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
		    		qpAccountMap.put(pos, data.get(key));
		    	}
	    }
	    
	    for(int i = 0; i < qpAccountMap.size(); i++){
	    		Account account = JPA.em().find(Account.class, Long.parseLong(qpAccountMap.get(i)));
	    		account.project = project;
	    		JPA.em().persist(account);
	    }
	}
	
	public static void initInspector(Project project, Map<String, String> data) throws ParseException{
		Iterator<String> iterator = data.keySet().iterator();
		Map<Integer, String> inspectorAccountMap = new HashMap<>();
	    
	    String key;
	    while(iterator.hasNext()){
		    	key = iterator.next();
		    	if(key.contains("qpAccounts")){
		    		int startIdx = key.indexOf("[") + 1;
		    		int endIdx = key.indexOf("]");
		    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
		    		inspectorAccountMap.put(pos, data.get(key));
		    	}
	    }
	    
	    for(int i = 0; i < inspectorAccountMap.size(); i++){
	    		Account account = JPA.em().find(Account.class, Long.parseLong(inspectorAccountMap.get(i)));
	    		account.project = project;
	    		JPA.em().persist(account);
	    }
	}

}
