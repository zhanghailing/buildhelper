package models;

import java.util.ArrayList;
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
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.Parameter;

import com.fasterxml.jackson.annotation.JsonIgnore;

import controllers.AuthController;
import play.db.jpa.JPA;
import tools.Utils;

@Entity
@Table(name="builder")
public class Builder {

	@Id
	@GenericGenerator(name = "generator", strategy = "foreign", parameters = @Parameter(name = "property", value = "account"))
	@GeneratedValue(generator = "generator")
	@Column(name = "account_id", unique = true, nullable = false)
	public long accountId;
	
	@OneToOne(fetch = FetchType.LAZY)
	@PrimaryKeyJoinColumn
	public Account account;
	
	@Column(name="company_name")
	public String companyName;
	
	public String name;
	
	public String designation;
	
	@Column(name="hp_no")
	public String hpNo;
	
	@Column(name="is_notify")
	public boolean isNotify;
	
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "project_builder", joinColumns = @JoinColumn(name = "builder_id"), inverseJoinColumns = @JoinColumn(name = "project_id"))
	@LazyCollection(LazyCollectionOption.EXTRA)
	@JsonIgnore
    public List<Project> projects;
	
	public Builder() {
		this.projects = new ArrayList<Project>();
	}
	
	public Builder(Account account) {
		this();
		this.account = account;
	}
	
	public static List<Builder> initBuilder(String companyName, List<Project> projects, Map<String, String> data) throws Exception, NoResultException{
		Iterator<String> iterator = data.keySet().iterator();
		Map<Integer, String> builderEmailMap = new HashMap<>();
	    Map<Integer, String> builderPasswordMap = new HashMap<>();
	    Map<Integer, String> builderNameMap = new HashMap<>();
	    Map<Integer, String> builderHpNoMap = new HashMap<>();
	    Map<Integer, String> builderNotifyMap = new HashMap<>();
	    Map<Integer, String> builderDesignationMap = new HashMap<>();
	    
	    String key;
	    while(iterator.hasNext()){
		    	key = iterator.next();
		    	if(key.contains("builderEmail")){
		    		int startIdx = key.indexOf("[") + 1;
		    		int endIdx = key.indexOf("]");
		    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
		    		builderEmailMap.put(pos, data.get(key));
		    	}
		    	
		    	if(key.contains("builderPassword")){
		    		int startIdx = key.indexOf("[") + 1;
		    		int endIdx = key.indexOf("]");
		    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
		    		builderPasswordMap.put(pos, data.get(key));
		    	}
		    	
		    	if(key.contains("builderName")){
		    		int startIdx = key.indexOf("[") + 1;
		    		int endIdx = key.indexOf("]");
		    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
		    		builderNameMap.put(pos, data.get(key));
		    	}
		    	
		    	if(key.contains("builderHpNo")){
		    		int startIdx = key.indexOf("[") + 1;
		    		int endIdx = key.indexOf("]");
		    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
		    		builderHpNoMap.put(pos, data.get(key));
		    	}
		    	
		    	if(key.contains("builderDesignation")){
		    		int startIdx = key.indexOf("[") + 1;
		    		int endIdx = key.indexOf("]");
		    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
		    		builderDesignationMap.put(pos, data.get(key));
		    	}
		    	
		    	if(key.contains("builderNotify")){
		    		int startIdx = key.indexOf("[") + 1;
		    		int endIdx = key.indexOf("]");
		    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
		    		builderNotifyMap.put(pos, data.get(key));
		    	}
	    }
	    
	    List<Builder> builders = new ArrayList<Builder>();
	    for(int i = 0; i < builderEmailMap.size(); i++){
	    		Account account;
	    		Builder builder = null;
		    	if(!AuthController.notExists(builderEmailMap.get(i))){
		    		account = JPA.em()
		    				.createQuery("from Account ac where ac.email=:email", Account.class)
		    				.setParameter("email", builderEmailMap.get(i)).getSingleResult();
		    		if(account.builder == null) {
		    			builder = new Builder(account);
		    		}else {
		    			builder = account.builder;
		    		}
	    		}else {
	    			account = new Account(builderEmailMap.get(i), builderPasswordMap.get(i));
		    		account.accType = AccountType.CONTRACTOR;
		    		JPA.em().persist(account);
		    		builder = new Builder(account);
	    		}
		    
		    	String useNotifyStr = Utils.isBlank(builderNotifyMap.get(i)) ? "0" : builderNotifyMap.get(i);
	    		builder.companyName = companyName;
	    		builder.name = builderNameMap.get(i);
	    		builder.hpNo = builderHpNoMap.get(i);
	    		builder.designation = builderDesignationMap.get(i);
	    		builder.isNotify = useNotifyStr.equals("1") ? true : false;
	    		for(Project project : projects) {
	    			builder.projects.add(project);
		    	}
	    		
	    		JPA.em().persist(builder);
	    		builders.add(builder);
	    }
	    return builders;
	}
}
