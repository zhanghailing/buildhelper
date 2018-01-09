package ModelVOs;

import models.Account;
import models.AccountType;

public class UserVO {
	public long accountId;
	public String name;
	public String peNo;
	public String designation;
	public AccountType accType;
	public String company;
	
	public UserVO(Account account){
		this.accountId = account.id;
		this.name = account.user == null ? "Not Set" : account.user.name;
		this.accType = account.accType;
		this.company = account.company == null ? "No Company" : account.company.name;
	}
}
