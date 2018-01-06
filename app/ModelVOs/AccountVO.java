package ModelVOs;

import models.Account;
import models.AccountType;

public class AccountVO {

	public long accountId;
	public String name;
	public String peNo;
	public String designation;
	public AccountType accType;
	public boolean isEngineer;
	public boolean isBuilder;
	public boolean isClient;
	public boolean hasProjects;
	
	public AccountVO(Account account){
		this.accountId = account.id;
		this.name = account.user == null ? "Not Set" : account.user.name;
		this.peNo = account.user == null ? "Not Set" : account.user.peNo;
		this.designation = account.user == null ? "Not Set" : account.user.designation;
		this.accType = account.accType;
		this.isEngineer = account.engineer != null;
		this.isBuilder = account.builder != null;
		this.isClient = account.client != null;
		this.hasProjects = account.projectsJoined != null;
	}
	
}
