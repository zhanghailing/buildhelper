package models;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
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
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.annotation.JsonIgnore;

import services.S3Plugin;
import tools.Utils;


@Entity
@Table(name = "drawingfile")
public class DrawingFile{
	@Transient
	@JsonIgnore
	public static final String DEFAULT_AVATAR = "public/images/image_placeholder.png";
	
	@Id
	@GeneratedValue
	public long id;
		
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
	@JsonIgnore
    public Project project;
	
	@Column(name="filename")
	public String fileName;
	
	public String location;
	
	public String uuid;
	
	public long size;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="uploaded_datetime")
	public Date uploadedDatetime;
	
	public DrawingFile(){}
	public DrawingFile(Project project, File file) throws IOException{
		this.project = project;
		this.uuid = Utils.uuid();
		this.uploadedDatetime = new Date();
		this.size = file.length();
		
		upload(file);
	}
	
	public void upload(File file){
		if (S3Plugin.amazonS3 == null) {
            throw new RuntimeException("S3 Could not save");
        }else {
            PutObjectRequest putObjectRequest = new PutObjectRequest(S3Plugin.s3Bucket, this.uuid, file);
            putObjectRequest.withCannedAcl(CannedAccessControlList.PublicRead);
            S3Plugin.amazonS3.putObject(putObjectRequest); 
        }
	}
	
	public InputStream download(){
		S3Object s3Object = S3Plugin.amazonS3.getObject(new GetObjectRequest(S3Plugin.s3Bucket, this.uuid));
		InputStream stream = s3Object.getObjectContent();
		return stream;
	}

	public void delete(){
		S3Plugin.amazonS3.deleteObject(S3Plugin.s3Bucket, this.uuid);
	}
	
	public void setLocation(Map<String, String> data) throws Exception{
		Iterator<String> iterator = data.keySet().iterator();
		Map<Integer, String> locationMap = new HashMap<>();
	    
	    String key;
	    while(iterator.hasNext()){
		    	key = iterator.next();
		    	if(key.contains("location")){
		    		int startIdx = key.indexOf("[") + 1;
		    		int endIdx = key.indexOf("]");
		    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
		    		locationMap.put(pos, data.get(key));
		    	}
	    }
	    
	    for(int i = 0; i < locationMap.size(); i++){
	    		this.location += locationMap.get(i) + "|";
	    }
	    
	    if(!Utils.isBlank(this.location) && this.location.length() > 0){
	    		this.location = this.location.substring(0, this.location.length() - 1).replace("null", "");
	    }
	}
	
}




















