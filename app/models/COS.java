package models;

import java.io.File;
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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import com.fasterxml.jackson.annotation.JsonIgnore;
import play.db.jpa.JPA;
import tools.Utils;

@Entity
@Table(name = "cos")
public class COS {
	
	@Id
	@GeneratedValue
	public long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
	@JsonIgnore
    public Project project;
	
	@Column(name="ref_no")
	public String referenceNo;
	
	@Column(name="loc")
	public String location;
	
	@Column(name="extra_loc")
	public String extraLocation;
	
	@OneToMany(mappedBy = "cos")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<SubGrid> subGrids;

	@Column(name="serial_no")
	public String serialNo;
	
	@Column(name="gondola_no")
	public String gondolaNo;
	
	@Column(name="le_reg_no")
	public String leRegistrationNo;
	
	@Column(name="distinctive_no")
	public String distinctiveNo;
	
	@Column(name="gondola_max_length")
	public String gondolaMaxLength;
	
	@Column(name="gondola_max_swl")
	public String gondolaMaxSWL;
	
	@Column(name="cmwp_serial_no")
	public String cmwpSerialNo;
	
	@Column(name="mcwp_max_height")
	public String mcwpMaxHeight;
	
	@Column(name="mcwp_serial_no")
	public String mcwpMaxLength;
	
	@OneToMany(mappedBy = "cos")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Account> routeAccounts;
	
	@OneToMany(mappedBy = "cos")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<COSTerm> cosTerms;
	
	@OneToOne(mappedBy = "cos")
	public Signature signature;
	
	@OneToMany(mappedBy = "cos")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Inspection> inspections;
	
	@OneToMany(mappedBy = "cos")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Issue> issues;
	
	@OneToMany(mappedBy = "cos")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Notification> notifications;
	
	@OneToMany(mappedBy = "cos")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<COSImage> additionalImages;
	
	public COS() {
		this.routeAccounts = new ArrayList<>();
		this.cosTerms = new ArrayList<>();
		this.inspections = new ArrayList<>();
		this.issues = new ArrayList<>();
		this.notifications = new ArrayList<>();
	}
	public COS(Project project) {
		this.project = project;
		this.referenceNo = (System.currentTimeMillis()+"").substring(5);
	}
	
	public void initSubGrid(Map<String, String> data) {
		Iterator<String> iterator = data.keySet().iterator();
		Map<Integer, String> subjectMap = new HashMap<>();
	    Map<Integer, String> gridMap = new HashMap<>();
	    
	    String key;
	    while(iterator.hasNext()){
		    	key = iterator.next();
		    	if(key.contains("subject")){
		    		int startIdx = key.indexOf("[") + 1;
		    		int endIdx = key.indexOf("]");
		    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
		    		subjectMap.put(pos, data.get(key));
		    	}
		    	
		    	if(key.contains("locgrid")){
		    		int startIdx = key.indexOf("[") + 1;
		    		int endIdx = key.indexOf("]");
		    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
		    		gridMap.put(pos, data.get(key));
		    	}
	    }
	    
	    int size = Math.max(subjectMap.size(), gridMap.size());
	    
	    if(this.subGrids != null && this.subGrids.size() > 0){
		    	for(SubGrid subGrid : this.subGrids){
		    		JPA.em().remove(subGrid);
		    	}
	    }
	    
	    for(int i = 0; i < size; i++){
	    		SubGrid subGrid = new SubGrid(this);
	    		subGrid.subject = subjectMap.get(i);
	    		subGrid.gridLine = gridMap.get(i);
		    	JPA.em().persist(subGrid);
	    }
	}
	
	public void inspectorApproveCOS(String reason, String comment, String approveDate, File signImage) throws Exception{
		if(this.inspections.size() > 0) {
			Inspection inspection = this.inspections.get(0);
			for(Approve approve: inspection.approves) {
				JPA.em().remove(approve);
			}
			Approve approve = new Approve(inspection, reason);
			approve.approveDate = Utils.parse("yyyy-MM-dd", approveDate);
			approve.comment = comment;
			JPA.em().persist(approve);
			
			if(signImage != null) {
				ApproveSign approveSign = new ApproveSign(approve, signImage);
				JPA.em().persist(approveSign);
			}
		}else {
			throw new Exception("COS haven't inspection.");
		}
	}
	
	public void issueApproveCOS(String reason, String comment, String approveDate, File signImage) throws Exception{
		if(this.issues.size() > 0) {
			Issue issue = this.issues.get(0);
			for(Approve approve: issue.approves) {
				JPA.em().remove(approve);
			}
			Approve approve = new Approve(issue, reason);
			approve.approveDate = Utils.parse("yyyy-MM-dd", approveDate);
			approve.comment = comment;
			JPA.em().persist(approve);
			
			if(signImage != null) {
				ApproveSign approveSign = new ApproveSign(approve, signImage);
				JPA.em().persist(approveSign);
			}
		}else {
			throw new Exception("COS haven't issue.");
		}
	}
	
	public void inspectorRejectCOS(String reason, File signImage) throws Exception{
		if(this.inspections.size() > 0) {
			Inspection inspection = this.inspections.get(0);
			if(inspection.rejects != null && inspection.rejects.size() > 0) {
				for(Reject reject : inspection.rejects) {
					JPA.em().remove(reject);
				}
			}

			Reject reject = new Reject(inspection, reason);
			JPA.em().persist(reject);
			
			RejectSign rejectSign = new RejectSign(reject, signImage);
			JPA.em().persist(rejectSign);
		}else {
			throw new Exception("COS haven't inspection.");
		}
	}
	
	public void issueRejectCOS(String reason, File signImage) throws Exception{
		if(this.issues.size() > 0) {
			Issue issue = this.issues.get(0);
			if(issue.rejects != null && issue.rejects.size() > 0) {
				for(Reject reject : issue.rejects) {
					JPA.em().remove(reject);
				}
			}

			Reject reject = new Reject(issue, reason);
			JPA.em().persist(reject);
			
			RejectSign rejectSign = new RejectSign(reject, signImage);
			JPA.em().persist(rejectSign);
		}else {
			throw new Exception("COS haven't issue.");
		}
	}
	
}













































