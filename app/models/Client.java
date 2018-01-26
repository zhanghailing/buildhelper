package models;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
import services.MailerService;
import tools.Utils;

@Entity
@Table(name="client")
public class Client {

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
	@JoinTable(name = "project_client", joinColumns = @JoinColumn(name = "client_id"), inverseJoinColumns = @JoinColumn(name = "project_id"))
	@LazyCollection(LazyCollectionOption.EXTRA)
	@JsonIgnore
    public List<Project> projects;
	
	public Client() {
		this.projects = new ArrayList<Project>();
	}
	
	public Client(Account account) {
		this();
		this.account = account;
	}
	
	public static List<Client> initClient(String clientCompany, List<Project> projects, Map<String, String> data) throws Exception, NoResultException{
		Iterator<String> iterator = data.keySet().iterator();
		Map<Integer, String> clientEmailMap = new HashMap<>();
	    Map<Integer, String> clientPasswordMap = new HashMap<>();
	    Map<Integer, String> clientNameMap = new HashMap<>();
	    Map<Integer, String> clientHpNoMap = new HashMap<>();
	    Map<Integer, String> clientNotifyMap = new HashMap<>();
	    Map<Integer, String> clientDesignationMap = new HashMap<>();
	    
	    String key;
	    while(iterator.hasNext()){
		    	key = iterator.next();
		    	if(key.contains("clientEmail")){
		    		int startIdx = key.indexOf("[") + 1;
		    		int endIdx = key.indexOf("]");
		    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
		    		clientEmailMap.put(pos, data.get(key));
		    	}
		    	
		    	if(key.contains("clientPassword")){
		    		int startIdx = key.indexOf("[") + 1;
		    		int endIdx = key.indexOf("]");
		    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
		    		clientPasswordMap.put(pos, data.get(key));
		    	}
		    	
		    	if(key.contains("clientName")){
		    		int startIdx = key.indexOf("[") + 1;
		    		int endIdx = key.indexOf("]");
		    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
		    		clientNameMap.put(pos, data.get(key));
		    	}
		    	
		    	if(key.contains("clientHpNo")){
		    		int startIdx = key.indexOf("[") + 1;
		    		int endIdx = key.indexOf("]");
		    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
		    		clientHpNoMap.put(pos, data.get(key));
		    	}
		    	
		    	if(key.contains("clientNotify")){
		    		int startIdx = key.indexOf("[") + 1;
		    		int endIdx = key.indexOf("]");
		    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
		    		clientNotifyMap.put(pos, data.get(key));
		    	}
		    	
		    	if(key.contains("clientDesignation")){
		    		int startIdx = key.indexOf("[") + 1;
		    		int endIdx = key.indexOf("]");
		    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
		    		clientDesignationMap.put(pos, data.get(key));
		    	}
	    }
	    
	    List<Client> clients = new ArrayList<Client>();
	    for(int i = 0; i < clientEmailMap.size(); i++){
	    		Account account;
	    		Client client = null;
		    	if(!AuthController.notExists(clientEmailMap.get(i))){
		    		account = JPA.em()
		    				.createQuery("from Account ac where ac.email=:email", Account.class)
		    				.setParameter("email", clientEmailMap.get(i)).getSingleResult();
		    		if(account.client == null) {
		    			client = new Client(account);
		    		}else {
		    			client = account.client;
		    		}
	    		}else {
	    			account = new Account(clientEmailMap.get(i), clientPasswordMap.get(i));
		    		account.accType = AccountType.CONTRACTOR;
		    		JPA.em().persist(account);
		    		client = new Client(account);
	    		}
		    
		    	String useNotifyStr = Utils.isBlank(clientNotifyMap.get(i)) ? "0" : clientNotifyMap.get(i);
		    	client.companyName = clientCompany;
		    	client.name = clientNameMap.get(i);
		    	client.hpNo = clientHpNoMap.get(i);
		    	client.designation = clientDesignationMap.get(i);
		    	client.isNotify = useNotifyStr.equals("1") ? true : false;
		    	
		    	for(Project project : projects) {
		    		client.projects.add(project);
		    	}
		 
	    		JPA.em().persist(client);
	    		
	    		clients.add(client);
	    }
	    return clients;
	}
	
}
