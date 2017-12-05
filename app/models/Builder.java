package models;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
	@JsonIgnore
    public Project project;
	
	public Builder() {}
	
	public Builder(Account account, Project project) {
		this.account = account;
		this.project = project;
	}
	
	public static void initBuilder(String companyName, Project project, Map<String, String> data) throws ParseException{
		Iterator<String> iterator = data.keySet().iterator();
		Map<Integer, String> builderEmailMap = new HashMap<>();
	    Map<Integer, String> builderPasswordMap = new HashMap<>();
	    Map<Integer, String> builderNameMap = new HashMap<>();
	    Map<Integer, String> builderHpNoMap = new HashMap<>();
	    Map<Integer, String> builderNotifyMap = new HashMap<>();
	    
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
		    	
		    	if(key.contains("builderNotify")){
		    		int startIdx = key.indexOf("[") + 1;
		    		int endIdx = key.indexOf("]");
		    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
		    		builderNotifyMap.put(pos, data.get(key));
		    	}
	    }
	    
	    for(int i = 0; i < builderEmailMap.size(); i++){
	    		Account account = new Account(builderEmailMap.get(i), builderPasswordMap.get(i));
	    		account.accType = AccountType.CONTRACTOR;
	    		JPA.em().persist(account);
	    		
	    		String useNotifyStr = Utils.isBlank(builderNotifyMap.get(i)) ? "0" : builderNotifyMap.get(i);
	    		Builder builder = new Builder(account, project);
	    		builder.companyName = companyName;
	    		builder.name = builderNameMap.get(i);
	    		builder.hpNo = builderHpNoMap.get(i);
	    		builder.isNotify = useNotifyStr.equals("1") ? true : false;
	    		JPA.em().persist(builder);
	    }
	}
}
