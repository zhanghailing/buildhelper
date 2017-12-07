package models;

import java.io.InputStream;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.annotation.JsonIgnore;

import services.S3Plugin;

@Entity
@Table(name="doc_format")
public class DocFormat {
	
	@Id
	@GeneratedValue	
	@JsonIgnore
	public long id;
	
	public String uuid;
	
	@Column(name="doc_type")
	public String docType;
	
	@Column(name="work_type")
	public String workType;
	
	public String department;
	
	@Column(name="thumb_uuid")
	public String thumbnialUUID;
	
	@Column(name="partial_url")
	public String partialUrl;
	
	public InputStream download(){
		S3Object s3Object = S3Plugin.amazonS3.getObject(new GetObjectRequest(S3Plugin.s3Bucket, this.thumbnialUUID));
		InputStream stream = s3Object.getObjectContent();
		return stream;
	}

	public void delete(){
		S3Plugin.amazonS3.deleteObject(S3Plugin.s3Bucket, this.uuid);
	}
	
}
