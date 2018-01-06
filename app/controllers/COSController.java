package controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import actions.AuthAction;
import models.Account;
import models.AccountType;
import models.Approve;
import models.ApproveSign;
import models.Avatar;
import models.COS;
import models.COSImage;
import models.COSTerm;
import models.Inspection;
import models.Issue;
import models.LetterHead;
import models.Project;
import models.Reject;
import models.RejectSign;
import models.ResponseData;
import models.Signature;
import models.Term;
import models.TermType;
import play.Application;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import tools.Constants;
import tools.Utils;
import views.html.*;

@SuppressWarnings("unchecked")
public class COSController extends Controller{
	@Inject private FormFactory formFactory;
	@Inject private JPAApi jpaApi;
	@Inject private Provider<Application> application;
	
	@With(AuthAction.class)
	@Transactional
	public Result createCOS(long projectId) {
		ResponseData responseData = new ResponseData();

		long accountId = ((Account) ctx().args.get("account")).id;
		Account account = jpaApi.em().find(Account.class, accountId);
		if (account == null) {
			responseData.code = 4000;
			responseData.message = "Account doesn't exist.";
		}else{
			Project project = jpaApi.em().find(Project.class, projectId);
			if(project != null) {
				List<String> locationLineArray = jpaApi.em().createNativeQuery("SELECT df.location FROM drawingfile df WHERE df.project_id = :projectId")
						.setParameter("projectId", project.id)
						.getResultList();
				
				List<String> locations = new ArrayList<String>();
				if(locationLineArray != null && locationLineArray.size() > 0) {
					for(String locationLine : locationLineArray) {
						String[] loc = locationLine.split("\\|");
						locations.addAll(Arrays.asList(loc));
					}
				}
				
				List<Term> terms = jpaApi.em().createQuery("FROM Term", Term.class).getResultList();
				
				List<Account> qpList = new ArrayList<Account>();
				List<Account> inspectors = new ArrayList<Account>();
				for(Account acc : project.teamAccounts) {
					if(acc.accType == AccountType.QP) {
						qpList.add(acc);
					}else if(acc.accType == AccountType.INSPECTOR) {
						inspectors.add(acc);
					}
				}
				
				return ok(requestcos.render(project, locations, terms, qpList, inspectors));
			}else {
				responseData.code = 4000;
				responseData.message = "Project doesn't exist.";
			}
		}

		return notFound(errorpage.render(responseData));
	}
	
	
	@With(AuthAction.class)
	@Transactional
	public Result saveCOS() {
		ResponseData responseData = new ResponseData();

		DynamicForm requestData = formFactory.form().bindFromRequest();
		String projectId = requestData.get("projectId");
		String location = requestData.get("location");
		String extraLocation = requestData.get("extraLocation");
		String subject = requestData.get("subject");
		String locgrid = requestData.get("locgrid");
		String serialNo = requestData.get("serialNo");
		//Gondola
		String gondolaNo = requestData.get("gondolaNo");
		String leRegisterNo = requestData.get("leRegisterNo");
		String distinctiveNo = requestData.get("distinctiveNo");
		String gondolaMaxLength = requestData.get("gondolaMaxLength");
		String gondolaMaxSWL = requestData.get("gondolaMaxSWL");
		
		//MWCP
		String cmwpSerialNo = requestData.get("cmwpSerialNo");
		String mcwpMaxHeight = requestData.get("mcwpMaxHeight");
		String mcwpMaxLength = requestData.get("mcwpMaxLength");
		
		long accountId = ((Account) ctx().args.get("account")).id;
		Account account = jpaApi.em().find(Account.class, accountId);
		if (account == null) {
			responseData.code = 4000;
			responseData.message = "Account doesn't exist.";
		}else{
			Project project = jpaApi.em().find(Project.class, Long.parseLong(projectId));
			if(project != null) {
				COS cos = new COS(project, subject);
				cos.extraLocation = extraLocation;
				cos.location = location;
				cos.gridLine = locgrid;
				cos.serialNo = serialNo;
				if(project.isGondola) {
					cos.gondolaNo = gondolaNo;
					cos.leRegistrationNo = leRegisterNo;
					cos.distinctiveNo = distinctiveNo;
					cos.gondolaMaxLength = gondolaMaxLength;
					cos.gondolaMaxSWL = gondolaMaxSWL;
				}
				if(project.isMCWP) {
					cos.cmwpSerialNo = cmwpSerialNo;
					cos.mcwpMaxHeight = mcwpMaxHeight;
					cos.mcwpMaxLength = mcwpMaxLength;
				}
				jpaApi.em().persist(cos); //EOF cos
				
				try {
					List<Term> terms = jpaApi.em().createQuery("FROM Term", Term.class).getResultList();
					
					Map<String, List<FilePart<File>>> fileMap = new HashMap<>();
					MultipartFormData<File> body = request().body().asMultipartFormData();
					List<FilePart<File>> generalPartFileParts = body.getFiles();
					for (FilePart<File> generalFilePart : generalPartFileParts) {
						if(generalFilePart.getFile() != null && generalFilePart.getFile().length() > 0) {
							String key = generalFilePart.getKey();
							if(key.equals("signature")) {
								Signature signature = new Signature(cos, generalFilePart.getFile());
								jpaApi.em().persist(signature); //EOF Signature
							}else if(key.contains("-")){
								String termId = key.split("-")[0];
								List<FilePart<File>> termFileList = null;
								if(fileMap.containsKey(termId)) {
									termFileList = fileMap.get(termId);
								}else {
									termFileList = new ArrayList<>();
									fileMap.put(termId, termFileList);
								}
								termFileList.add(generalFilePart);
							}
						}
			        }
					
					List<COSTerm> cosTerms = new ArrayList<>();
					for(Term term : terms) {
						String remark = requestData.get(term.id + "-remark");
						String optVal = requestData.get(term.id + "-value");
						
						COSTerm generalcosTerm = new COSTerm(cos, term);
						generalcosTerm.value = Integer.parseInt(optVal);
						if(!Utils.isBlank(remark)) {
							generalcosTerm.remark = remark;
						}
						jpaApi.em().persist(generalcosTerm);
						cosTerms.add(generalcosTerm);
						if(project.isFormwork && term.termType == TermType.FORMWORK) {
							COSTerm formworkCOSTerm = new COSTerm(cos, term);
							if(!Utils.isBlank(remark)) {
								formworkCOSTerm.remark = remark;
							}
							jpaApi.em().persist(formworkCOSTerm);
							cosTerms.add(formworkCOSTerm);
						}
					}
					
					for(COSTerm cosTerm : cosTerms) {
						String key = cosTerm.term.id+"";
						if(fileMap.size() > 0) {
							if(fileMap.containsKey(key)) {
								List<FilePart<File>> filePartList = fileMap.get(key);
								for(FilePart<File> filePart : filePartList) {
									COSImage cosImage = new COSImage(cosTerm, filePart.getFile());
									jpaApi.em().persist(cosImage);
								}
							}
						}
					}
					
					//Start add route member
					Iterator<String> iterator = requestData.data().keySet().iterator();
					List<String> routeAccounts = new ArrayList<>();
				    
				    String key;
				    while(iterator.hasNext()){
					    	key = iterator.next();
					    	if(key.contains("qp")){
					    		String qpAcc = requestData.data().get(key);
					    		if(!Utils.isBlank(qpAcc)) {
					    			routeAccounts.add(qpAcc);
					    		}
					    	}
					    	
					    	if(key.contains("inspector")){
					    		String inspectorAcc = requestData.data().get(key);
					    		if(!Utils.isBlank(inspectorAcc)) {
					    			routeAccounts.add(inspectorAcc);
					    		}
					    	}
				    }
				    
				    String routeWhereCause = "";
				    for(String qpAcc : routeAccounts){
				    		routeWhereCause	 += "ac.id=" + qpAcc + " or ";
				    }
					if(routeWhereCause.length() > 4) {
						routeWhereCause = routeWhereCause.substring(0, routeWhereCause.length() - 4);
					}
					
					List<Account> accountList = jpaApi.em().createNativeQuery("SELECT * FROM account ac WHERE " + routeWhereCause, Account.class).getResultList();
					for(Account a : accountList) {
						a.cos = cos;
						jpaApi.em().persist(a);
					}
				}catch (IOException e) {
					responseData.code = 4001;
					responseData.message = e.getMessage();
				}
			}else {
				responseData.code = 4000;
				responseData.message = "Project doesn't exist.";
			}
		}
		
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result viewCOS(long projectId, int offset) {
		ResponseData responseData = new ResponseData();
		
		long accountId = ((Account) ctx().args.get("account")).id;
		Account account = jpaApi.em().find(Account.class,accountId);
		if (account == null) {
			responseData.code = 4000;
			responseData.message = "Account doesn't exist.";
		}else{
			Project project = jpaApi.em().find(Project.class, projectId);
			if(project != null) {
				   String countSql = "SELECT COUNT(*) FROM cos cs WHERE cs.project_id = :projectId";

					int totalAmount = ((BigInteger) jpaApi.em().createNativeQuery(countSql).setParameter("projectId", project.id).getSingleResult()).intValue();
					int pageIndex = (int) Math.ceil(offset / Constants.COMPANY_PAGE_SIZE) + 1;
					List<COS> coses = jpaApi.em()
							   .createNativeQuery("SELECT * FROM cos cs WHERE cs.project_id = :projectId", COS.class)
							   .setParameter("projectId", project.id).getResultList();
					
				return ok(viewcos.render(account, coses, pageIndex, totalAmount));
			}else {
				responseData.code = 4000;
				responseData.message = "Project doesn't exist.";
			}
		}
		
		return notFound(errorpage.render(responseData));
	}
	
	@Transactional
	public Result showCOSImage(String uuid, boolean isLarge) {
		TypedQuery<COSImage> query = jpaApi.em()
				.createQuery("from COSImage ci where ci.uuid = :uuid", COSImage.class).setParameter("uuid", uuid);

		InputStream imageStream = null;
		try {
			COSImage cosImage = query.getSingleResult();
			if (isLarge) {
				imageStream = cosImage.download();
			} else {
				imageStream = cosImage.downloadThumbnail();
			}
		} catch (NoResultException e) {
			imageStream = application.get().classloader().getResourceAsStream(LetterHead.PLACEHOLDER);
		}
		return ok(imageStream);
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result inspectCOS(long cosId) {
		ResponseData responseData = new ResponseData();
		
		long accountId = ((Account) ctx().args.get("account")).id;
		Account account = jpaApi.em().find(Account.class, accountId);
		if (account == null) {
			responseData.code = 4000;
			responseData.message = "Account doesn't exist.";
		}else{
			COS cos = jpaApi.em().find(COS.class, cosId);
			if(cos != null) {
				return ok(inspectcos.render(cos));
			}else {
				responseData.code = 4000;
				responseData.message = "COS doesn't exist.";
			}
		}
		
		return notFound(errorpage.render(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result saveInspect() {
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String passType = requestData.get("passType");
		String cosId = requestData.get("cosId");
		String inspectDate = requestData.get("inspectDate");
		
		Account account = (Account) ctx().args.get("account");
		Account inspectedBy = jpaApi.em().find(Account.class, account.id);
		if (inspectedBy == null) {
			responseData.code = 4000;
			responseData.message = "Account doesn't exist.";
		}else{
			COS cos = jpaApi.em().find(COS.class, Long.parseLong(cosId));
			if(cos != null) {
				if(cos.inspections.size() > 0) {
					for(Inspection inspection : cos.inspections) {
						jpaApi.em().remove(inspection);
					}
				}
				try {
					Inspection inspection = new Inspection(cos, inspectedBy);
					inspection.passType = passType;
					inspection.inspectDate = Utils.parse("yyyy-MM-dd", inspectDate);
					jpaApi.em().persist(inspection);
					if(inspection.passType.equals("approve")) {
						return ok(approvecos.render(cos));
					}else {
						return ok(rejectcos.render(cos));
					}
				} catch (ParseException e) {
					responseData.code = 4001;
					responseData.message = e.getMessage();
				}
			}else {
				responseData.code = 4000;
				responseData.message = "COS doesn't exist.";
			}
		}
		return notFound(errorpage.render(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result saveIssue() {
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String passType = requestData.get("passType");
		String cosId = requestData.get("cosId");
		String issueDate = requestData.get("issueDate");
		
		Account account = (Account) ctx().args.get("account");
		Account issuedBy = jpaApi.em().find(Account.class, account.id);
		if (issuedBy == null) {
			responseData.code = 4000;
			responseData.message = "Account doesn't exist.";
		}else{
			COS cos = jpaApi.em().find(COS.class, Long.parseLong(cosId));
			if(cos != null) {
				if(cos.issues.size() > 0) {
					for(Issue issue : cos.issues) {
						jpaApi.em().remove(issue);
					}
				}
				try {
					Issue issue = new Issue(cos, issuedBy);
					issue.passType = passType;
					issue.issueDate = Utils.parse("yyyy-MM-dd", issueDate);
					jpaApi.em().persist(issue);
					
					MultipartFormData<File> body = request().body().asMultipartFormData();
					List<FilePart<File>> generalPartFileParts = body.getFiles();
					for (FilePart<File> generalFilePart : generalPartFileParts) {
						if(generalFilePart.getFile() != null && generalFilePart.getFile().length() > 0) {
							String key = generalFilePart.getKey();
							if(key.equals("images")) {
								COSImage cosImage = new COSImage(issue, generalFilePart.getFile());
								jpaApi.em().persist(cosImage);
							}
						}
			        }
					
					if(issue.passType.equals("approve")) {
						return ok(approvecos.render(cos));
					}else {
						return ok(rejectcos.render(cos));
					}
				} catch (IOException | ParseException e) {
					responseData.code = 4001;
					responseData.message = e.getMessage();
				}
			}else {
				responseData.code = 4000;
				responseData.message = "COS doesn't exist.";
			}
		}
		return notFound(errorpage.render(responseData));
	}
	
	
	@With(AuthAction.class)
	@Transactional
	public Result rejectCOS() {
		ResponseData responseData = new ResponseData();

		DynamicForm requestData = formFactory.form().bindFromRequest();
		String cosId = requestData.get("cosId");
		String reason = requestData.get("reason");
		
		long accountId = ((Account) ctx().args.get("account")).id;
		Account account = jpaApi.em().find(Account.class, accountId);
		if (account == null) {
			responseData.code = 4000;
			responseData.message = "Account doesn't exist.";
		}else{
			COS cos = jpaApi.em().find(COS.class, Long.parseLong(cosId));
			if(cos != null) {
				if(cos.inspections.size() > 0) {
					Inspection inspection = cos.inspections.get(0);
					if(inspection.rejects != null && inspection.rejects.size() > 0) {
						for(Reject reject : inspection.rejects) {
							jpaApi.em().remove(reject);
						}
					}

					Reject reject = new Reject(inspection, reason);
					jpaApi.em().persist(reject);
					
					MultipartFormData<File> body = request().body().asMultipartFormData();
					FilePart<File> rejectSignPart = body.getFile("rejectSign");
					try {
						RejectSign rejectSign = new RejectSign(reject, rejectSignPart.getFile());
						jpaApi.em().persist(rejectSign);
					} catch (IOException e) {
						responseData.code = 4001;
						responseData.message = e.getMessage();
					}
				}else {
					responseData.code = 4000;
					responseData.message = "COS haven't inspection.";
				}
			}else {
				responseData.code = 4000;
				responseData.message = "COS doesn't exist.";
			}
		}
		
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result approveCOS() {
		ResponseData responseData = new ResponseData();

		DynamicForm requestData = formFactory.form().bindFromRequest();
		String cosId = requestData.get("cosId");
		String reason = requestData.get("reason");
		String comment = requestData.get("comment");
		String approveDate = requestData.get("approveDate");
		
		long accountId = ((Account) ctx().args.get("account")).id;
		Account account = jpaApi.em().find(Account.class, accountId);
		if (account == null) {
			responseData.code = 4000;
			responseData.message = "Account doesn't exist.";
		}else{
			COS cos = jpaApi.em().find(COS.class, Long.parseLong(cosId));
			if(cos != null) {
				if(cos.inspections.size() > 0) {
					Inspection inspection = cos.inspections.get(0);
					for(Approve approve: inspection.approves) {
						jpaApi.em().remove(approve);
					}
					try {
						Approve approve = new Approve(inspection, reason);
						approve.approveDate = Utils.parse("yyyy-MM-dd", approveDate);
						approve.comment = comment;
						jpaApi.em().persist(approve);
						
						MultipartFormData<File> body = request().body().asMultipartFormData();
						FilePart<File> approveSignPart = body.getFile("approveSign");
						
						ApproveSign approveSign = new ApproveSign(approve, approveSignPart.getFile());
						jpaApi.em().persist(approveSign);
					} catch (ParseException | IOException e) {
						responseData.code = 4001;
						responseData.message = e.getMessage();
					}
				}else {
					responseData.code = 4000;
					responseData.message = "COS haven't inspection.";
				}
			}else {
				responseData.code = 4000;
				responseData.message = "COS doesn't exist.";
			}
		}
		
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result showSignature(String uuid, boolean isLarge) {
		TypedQuery<Signature> query = jpaApi.em().createQuery("from Signature sn where sn.uuid = :uuid", Signature.class)
				.setParameter("uuid", uuid);

		InputStream imageStream = null;
		try {
			Signature sign = query.getSingleResult();

			if (isLarge) {
				imageStream = sign.download();
			} else {
				imageStream = sign.downloadThumbnail();
			}
		} catch (NoResultException e) {
			imageStream = application.get().classloader().getResourceAsStream(Avatar.DEFAULT_AVATAR);
		}
		
		return ok(imageStream);
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result issueCOS(long cosId) {
		ResponseData responseData = new ResponseData();
		
		long accountId = ((Account) ctx().args.get("account")).id;
		Account account = jpaApi.em().find(Account.class, accountId);
		if (account == null) {
			responseData.code = 4000;
			responseData.message = "Account doesn't exist.";
		}else{
			COS cos = jpaApi.em().find(COS.class, cosId);
			if(cos != null) {
				return ok(issuecos.render(cos));
			}else {
				responseData.code = 4000;
				responseData.message = "COS doesn't exist.";
			}
		}
		
		return notFound(errorpage.render(responseData));
	}
}



























