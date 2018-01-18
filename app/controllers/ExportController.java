package controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.NoResultException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import actions.AuthAction;
import models.Account;
import models.AccountType;
import models.COS;
import models.Company;
import models.Engineer;
import models.Inspection;
import models.Issue;
import models.Project;
import models.ResponseData;
import models.SubGrid;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import tools.Utils;
import views.html.*;

@SuppressWarnings("unchecked")
public class ExportController extends Controller{
	@Inject private JPAApi jpaApi;
	
	@With(AuthAction.class)
	@Transactional
	public Result exportQPByCompany() {
		ResponseData responseData = new ResponseData();

		Company company = null;
		Account account = (Account) ctx().args.get("account");
		if (account.accType != AccountType.ADMIN) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}
		
		try{
			List<Company> companys = jpaApi.em()
				.createNativeQuery("select * from company cy where cy.acc_id=:accId", Company.class)
				.setParameter("accId", account.id).getResultList();
			if(companys.size() > 0) {
				company = companys.get(0);
				List<Account> qpList = jpaApi.em()
						.createQuery("FROM Account ac WHERE ac.company = :company AND ac.accType = :accType AND ac.deleted = :deleted", Account.class)
						.setParameter("company", company)
						.setParameter("accType", AccountType.QP)
						.setParameter("deleted", false).getResultList();
				
				String[] qpColumn = {"Serial No.", "Name", "Primary Email/ID", "Alternative Email 1", "Alternative Email 2", "Office No.", "HP No.", "Branch", "PE No."};
				
				List<Object[]> excelData = new ArrayList<Object[]>();
				excelData.add(qpColumn);
				
				int rowNum = 1;
				for(Account qpAccount : qpList) {
					Object[] qpData = new Object[qpColumn.length];
					qpData[0] = rowNum+"";
					qpData[1] = qpAccount.user.name;
					qpData[2] = qpAccount.email;
					qpData[3] = qpAccount.user.alterEmail1;
					qpData[4] = qpAccount.user.alterEmail2;
					qpData[5] = qpAccount.user.officePhone;
					qpData[6] = qpAccount.user.mobile;
					qpData[7] = qpAccount.user.qecpNo;
					qpData[8] = qpAccount.user.peNo;
					
					excelData.add(qpData);
					rowNum++;
				}
				
				
				XSSFWorkbook workbook = new XSSFWorkbook();
		        XSSFSheet sheet = workbook.createSheet("Datatypes in Java");
		        rowNum = 0;
				for(Object[] data : excelData) {
					Row row = sheet.createRow(rowNum++);
		            int colNum = 0;
		            for (Object field : data) {
		                Cell cell = row.createCell(colNum++);
		                if (field instanceof String) {
		                    cell.setCellValue((String) field);
		                } else if (field instanceof Integer) {
		                    cell.setCellValue((Integer) field);
		                }
		            }
				}
				
				try {
					String FILE_NAME = "/tmp/qp_list.xlsx";
		            FileOutputStream outputStream = new FileOutputStream(FILE_NAME);
		            workbook.write(outputStream);
		            workbook.close();
		            
		            File file  = new File(FILE_NAME);
		            return ok(file).withHeader("Content-Disposition", "attachment;filename=qp_list.xlsx");
		        } catch (IOException e) {
		        	responseData.code = 4001;
					responseData.message = e.getMessage();
		        }
			}else {
				responseData.code = 4000;
				responseData.message = "Two or more company found";
			}
		}catch(NoResultException e){
			responseData.code = 4000;
			responseData.message = "Company doesn't exist.";
		}

		return notFound(errorpage.render(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result exportInspectorByCompany() {
		ResponseData responseData = new ResponseData();

		Company company = null;
		Account account = (Account) ctx().args.get("account");
		if (account.accType != AccountType.ADMIN) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}
		try{
			List<Company> companys = jpaApi.em()
				.createNativeQuery("select * from company cy where cy.acc_id=:accId", Company.class)
				.setParameter("accId", account.id).getResultList();
			if(companys.size() > 0) {
				company = companys.get(0);
				List<Account> inspectors = jpaApi.em()
						.createQuery("FROM Account ac WHERE ac.company = :company AND ac.accType = :accType AND ac.deleted = :deleted", Account.class)
						.setParameter("company", company)
						.setParameter("accType", AccountType.INSPECTOR)
						.setParameter("deleted", false).getResultList();
				
				String[] qpColumn = {"Serial No.", "Name", "Primary Email/ID", "Alternative Email 1", "Alternative Email 2", "Office No.", "HP No.", "Designation"};
				
				List<Object[]> excelData = new ArrayList<Object[]>();
				excelData.add(qpColumn);
				
				int rowNum = 1;
				for(Account inspector : inspectors) {
					Object[] qpData = new Object[qpColumn.length];
					qpData[0] = rowNum+"";
					qpData[1] = inspector.user.name;
					qpData[2] = inspector.email;
					qpData[3] = inspector.user.alterEmail1;
					qpData[4] = inspector.user.alterEmail2;	
					qpData[5] = inspector.user.officePhone;
					qpData[6] = inspector.user.mobile;
					qpData[7] = inspector.user.designation;
					
					excelData.add(qpData);
					rowNum++;
				}
				
				XSSFWorkbook workbook = new XSSFWorkbook();
		        XSSFSheet sheet = workbook.createSheet("Datatypes in Java");
		        rowNum = 0;
				for(Object[] data : excelData) {
					Row row = sheet.createRow(rowNum++);
		            int colNum = 0;
		            for (Object field : data) {
		                Cell cell = row.createCell(colNum++);
		                if (field instanceof String) {
		                    cell.setCellValue((String) field);
		                } else if (field instanceof Integer) {
		                    cell.setCellValue((Integer) field);
		                }
		            }
				}
				
				try {
					String FILE_NAME = "/tmp/inspector_list.xlsx";
		            FileOutputStream outputStream = new FileOutputStream(FILE_NAME);
		            workbook.write(outputStream);
		            workbook.close();
		            
		            File file  = new File(FILE_NAME);
		            return ok(file).withHeader("Content-Disposition", "attachment;filename=inspector_list.xlsx");
		        } catch (IOException e) {
		        	responseData.code = 4001;
					responseData.message = e.getMessage();
		        }
			}else {
				responseData.code = 4000;
				responseData.message = "Two or more company found";
			}
		}catch(NoResultException e){
			responseData.code = 4000;
			responseData.message = "Company doesn't exist.";
		}

		return notFound(errorpage.render(responseData));
	}
	
	
	@With(AuthAction.class)
	@Transactional
	public Result exportProjectSummary() {
		ResponseData responseData = new ResponseData();

		Account account = (Account) ctx().args.get("account");
		if (account.accType != AccountType.ADMIN) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}
		try{
			List<String> results = jpaApi.em().createNativeQuery("SELECT pro.id FROM project pro WHERE pro.engineer_id=:engineerId" + 
					" UNION " + 
					"SELECT pt.project_id as id FROM project_team pt WHERE pt.account_id=:accountId " + 
					"ORDER BY id")
					.setParameter("engineerId", account.id)
					.setParameter("accountId", account.id)
					.getResultList();
			
			String projectIDCause = "";
			for (int i = 0; i < results.size(); i++) {
				if (i == results.size() - 1) {
					projectIDCause += "pro.id='" + ((BigInteger) ((Object)results.get(i))).intValue() + "'";
				} else {
					projectIDCause += "pro.id='" + ((BigInteger) ((Object)results.get(i))).intValue() + "' OR ";
				}
			}
			
			String projectCause = "";
			if(Utils.isBlank(projectIDCause)) {
				projectCause = "pro.engineer_id=" + account.id; 
			}else {
				projectCause = projectIDCause;
			}

			List<Project> projects = jpaApi.em()
					.createNativeQuery("SELECT * FROM project pro WHERE " + projectCause + " AND pro.is_archived = :isArchived", Project.class)
					.setParameter("isArchived", false)
					.getResultList();
			
			if(projects.size() > 0) {
				XSSFWorkbook workbook = new XSSFWorkbook();
				int rowNum = 1;
				for(Project project : projects) {
					List<Object[]> excelData = new ArrayList<Object[]>();
					String[] excelColumn = {"Serial No.", "Location", "Reference No.", "Subject", "Inspected By", "Inspection Date", "Issue By", "Issue Date"};
					for(int i = 0; i < 5; i++) {
						Object[] obj = new Object[excelColumn.length];
						excelData.add(obj);
					}
					excelData.add(excelColumn);//end create first row and indicator row
					
					XSSFSheet sheet = workbook.createSheet("Project " + project.id);
					sheet.addMergedRegion(new CellRangeAddress(1, 5, 1, 8));
					
			        for(COS cos : project.coses) {
			        		Object[] cosData = new Object[7];
			        		cosData[0] = rowNum+"";
			        		cosData[1] = cos.location;
			        		cosData[2] = cos.referenceNo;
			        		
			        		String subject = "";
			        		if(cos.subGrids.size() > 0) {
			        			for(SubGrid subGrid : cos.subGrids) {
			        				subject += subGrid.subject+",";
			        			}
			        		}
			        		
			        		if(subject.length() > 1) {
			        			cosData[3] = subject.substring(0, subject.length() - 1);
			        		}
			        		
			        		if(cos.inspections.size() > 0) {
			        			Inspection inspection = cos.inspections.get(0);
			        			if(inspection.inspectedBy != null) {
				        			cosData[4] = inspection.inspectedBy.user.name;
					        		cosData[5] = inspection.inspectDate;	
				        		}
			        		}
			        		
			        		if(cos.issues.size() > 0) {
			        			Issue issue = cos.issues.get(0);
			        			if(issue.issuedBy != null) {
				        			cosData[6] = issue.issuedBy.user.name;
					        		cosData[7] = issue.issueDate;
				        		}
			        		}
						excelData.add(cosData);
						rowNum++;
			        }
			        
			        //row 0
			        Row row0 = sheet.createRow(0);
			        Cell cell00 = row0.createCell(0);
			        cell00.setCellValue("Project: ");
			        
			        Cell cell01 = row0.createCell(1);
			        cell01.setCellValue(project.title);
			        
			        //row 1
			        Row row1 = sheet.createRow(1);
			        Cell cell10 = row1.createCell(0);
			        cell10.setCellValue("Type of Work: ");
			        
			        Cell cell11 = row1.createCell(1);
			        String projectType = "";
			        if(project.isGondola) {
			        		projectType += "Gondala, ";
			        }
			        if(project.isMCWP) {
		        			projectType += "MCWP, ";
			        }
			        if(project.isFormwork) {
		        			projectType += "Formwork, ";
			        }
			        if(project.isScaffold) {
		        			projectType += "Scaffold, ";
			        }
			        cell11.setCellValue(projectType.length() > 2 ? projectType.substring(0, projectType.length()-2) : "");
			        
			        //row2
			        Row row2 = sheet.createRow(2);
			        Cell cell20 = row2.createCell(0);
			        cell20.setCellValue("Starting Date: ");
			        
			        Cell cell21 = row2.createCell(1);
			        cell21.setCellValue(project.startDate.toString());
			        
			        //row3
			        Row row3 = sheet.createRow(3);
			        Cell cell30 = row3.createCell(0);
			        cell30.setCellValue("End Date: ");
			        
			        Cell cell31 = row3.createCell(1);
			        cell31.setCellValue(project.endDate.toString());
			        
			        //row4
			        Row row4 = sheet.createRow(4);
			        Cell cell40 = row4.createCell(0);
			        cell40.setCellValue("Developer/Client: ");
			        
			        Cell cell41 = row4.createCell(1);
			        cell41.setCellValue((project.clients != null && project.clients.size() > 0) ? project.clients.get(0).companyName : "");
			        
			        //row5
			        Row row5 = sheet.createRow(5);
			        Cell cell50 = row5.createCell(0);
			        cell50.setCellValue("Builder/Contactor: ");
			        
			        Cell cell51 = row5.createCell(1);
			        cell51.setCellValue((project.builders != null && project.builders.size() > 0) ? project.builders.get(0).companyName : "");
			        
			        //row6
			        Row row6 = sheet.createRow(6);
			        Cell cell60 = row6.createCell(0);
			        cell60.setCellValue("QP & Inspector in charge: ");
			        
			        Cell cell61 = row6.createCell(1);
			        String teamMember = "";
			        if(project.teamAccounts != null && project.teamAccounts.size() > 0) {
			        		for(Account member : project.teamAccounts) {
			        			teamMember += member.user.name + ", ";
			        		}
			        }
			        cell61.setCellValue(teamMember.length() > 2 ? teamMember.substring(0, teamMember.length() - 2) : "");
			        
			        rowNum = 7; // start row 6
					for(Object[] data : excelData) {
						Row row = sheet.createRow(rowNum++);
			            int colNum = 0;
			            for (Object field : data) {
			                Cell cell = row.createCell(colNum++);
			                if (field instanceof String) {
			                    cell.setCellValue((String) field);
			                } else if (field instanceof Integer) {
			                    cell.setCellValue((Integer) field);
			                }
			            }
					}
				}
				
				try {
					String FILE_NAME = "/tmp/project_list.xlsx";
		            FileOutputStream outputStream = new FileOutputStream(FILE_NAME);
		            workbook.write(outputStream);
		            workbook.close();
		            
		            File file  = new File(FILE_NAME);
		            return ok(file).withHeader("Content-Disposition", "attachment;filename=project_list.xlsx");
		        } catch (IOException e) {
		        	responseData.code = 4001;
					responseData.message = e.getMessage();
		        }
			}else {
				responseData.code = 4000;
				responseData.message = "No Project found!";
			}
		}catch(NoResultException e){
			responseData.code = 4000;
			responseData.message = "Company doesn't exist.";
		}

		return notFound(errorpage.render(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result exportEngineers() {
		ResponseData responseData = new ResponseData();

		Company company = null;
		Account account = (Account) ctx().args.get("account");
		if (account.accType != AccountType.ADMIN) {
			responseData.code = 4000;
			responseData.message = "You do not have permission.";
		}
		try{
			List<Company> companys = jpaApi.em()
				.createNativeQuery("select * from company cy where cy.acc_id=:accId", Company.class)
				.setParameter("accId", account.id).getResultList();
			if(companys.size() > 0) {
				company = companys.get(0);
				List<Engineer> engineers = jpaApi.em()
						.createQuery("FROM Engineer eng WHERE eng.company = :company", Engineer.class)
						.setParameter("company", company).getResultList();
				
				XSSFWorkbook workbook = new XSSFWorkbook();
				XSSFSheet sheet = workbook.createSheet("Engineer Summary");
				String[] engineerColumn = {"Serial No.", "Name", "Project Title", "Authority", "Type of Work", "Status"};
				List<Object[]> excelData = new ArrayList<Object[]>();
				excelData.add(engineerColumn);
				
				Row headerRow = sheet.createRow(0);
				for(int i = 0; i < engineerColumn.length; i++) {
					Cell cell = headerRow.createCell(i);
					cell.setCellValue(engineerColumn[i]);
					i++;
				}//end header
				
				int rowNum = 1;
				boolean flag = true;
				for(Engineer engineer : engineers) {
					flag = true;
					if(engineer.projects != null && engineer.projects.size() > 0) {
						for(Project project : engineer.projects) {
							Row row = sheet.createRow(rowNum++);
							if(flag) {
								Cell cell0 = row.createCell(0);
								cell0.setCellValue(engineer.accountId+"");
								
								Cell cell1 = row.createCell(1);
								cell1.setCellValue(engineer.account.user.name);
								flag = false;
								
								sheet.addMergedRegion(new CellRangeAddress(0, engineer.projects.size()-1, 0, 1));
							}
							
							Cell cell2 = row.createCell(2);
							cell2.setCellValue(project.title);
							
							Cell cell3 = row.createCell(3);
							cell3.setCellValue("MOM");
							
							Cell cell4 = row.createCell(4);
							String projectType = "";
					        if(project.isGondola) {
					        		projectType += "Gondala, ";
					        }
					        if(project.isMCWP) {
				        			projectType += "MCWP, ";
					        }
					        if(project.isFormwork) {
				        			projectType += "Formwork, ";
					        }
					        if(project.isScaffold) {
				        			projectType += "Scaffold, ";
					        }
					        cell4.setCellValue(projectType.length() > 2 ? projectType.substring(0, projectType.length()-2) : "");
					        
					        Cell cell5 = row.createCell(5);
					        switch(project.status) {
					        		case NEW:
					        			cell5.setCellValue("NEW");
					        			break;
					        		case STARTED:
					        			cell5.setCellValue("START");
					        			break;
					        		case PENDING:
					        			cell5.setCellValue("PENDING");
					        			break;
					        		case STOP:
					        			cell5.setCellValue("STOP");
					        			break;
					        		case COMPLETE:
					        			cell5.setCellValue("COMPLETE");
					        			break;
					        }
						}
					}
					rowNum += engineer.projects.size();
				}
				
				try {
					String FILE_NAME = "/tmp/engineer_list.xlsx";
		            FileOutputStream outputStream = new FileOutputStream(FILE_NAME);
		            workbook.write(outputStream);
		            workbook.close();
		            
		            File file  = new File(FILE_NAME);
		            return ok(file).withHeader("Content-Disposition", "attachment;filename=engineer_list.xlsx");
		        } catch (IOException e) {
		        	responseData.code = 4001;
					responseData.message = e.getMessage();
		        }
			}else {
				responseData.code = 4000;
				responseData.message = "Two or more company found";
			}
		}catch(NoResultException e){
			responseData.code = 4000;
			responseData.message = "Company doesn't exist.";
		}

		return notFound(errorpage.render(responseData));
	} 
	
	@With(AuthAction.class)
	@Transactional
	public Result exportProAdminSummary() {
		ResponseData responseData = new ResponseData();

		long accountId = ((Account) ctx().args.get("account")).id;
		Account account = jpaApi.em().find(Account.class, accountId);
		if (account == null) {
			responseData.code = 4000;
			responseData.message = "Account doesn't exist.";
		}else{
			List<String> results = jpaApi.em().createNativeQuery("SELECT pro.id FROM project pro WHERE pro.engineer_id=:engineerId" + 
					" UNION " + 
					"SELECT pt.project_id as id FROM project_team pt WHERE pt.account_id=:accountId " + 
					"ORDER BY id")
					.setParameter("engineerId", account.id)
					.setParameter("accountId", account.id)
					.getResultList();
			
			String projectIDCause = "";
			for (int i = 0; i < results.size(); i++) {
				if (i == results.size() - 1) {
					projectIDCause += "pro.id='" + ((BigInteger) ((Object)results.get(i))).intValue() + "'";
				} else {
					projectIDCause += "pro.id='" + ((BigInteger) ((Object)results.get(i))).intValue() + "' OR ";
				}
			}
			
			String projectCause = "";
			if(Utils.isBlank(projectIDCause)) {
				projectCause = "pro.engineer_id=" + account.id; 
			}else {
				projectCause = projectIDCause;
			}

			List<Project> projects = jpaApi.em()
					.createNativeQuery("SELECT * FROM project pro WHERE " + projectCause + " AND pro.is_archived = :isArchived", Project.class)
					.setParameter("isArchived", false)
					.getResultList();
			
			if(projects.size() > 0) {
				List<Object[]> excelData = new ArrayList<Object[]>();
				String[] excelColumn = {"Serial No.", "Project Title", "Type of Work", "Starting Date", "Ending Date", "Developer/Client", "Builder/Contractor", "QP", "Inspector"};
				excelData.add(excelColumn);
				
				int rowNum = 1;
				for(Project project : projects) {
					Object[] cosData = new Object[excelColumn.length];
		        		cosData[0] = rowNum+"";
		        		cosData[1] = project.title;
		        		
		        		String projectType = "";
			        if(project.isGondola) {
			        		projectType += "Gondala, ";
			        }
			        if(project.isMCWP) {
		        			projectType += "MCWP, ";
			        }
			        if(project.isFormwork) {
		        			projectType += "Formwork, ";
			        }
			        if(project.isScaffold) {
		        			projectType += "Scaffold, ";
			        }
			        cosData[2] = projectType.length() > 2 ? projectType.substring(0, projectType.length()-2) : "";
		        		cosData[3] = project.startDate;
		        		cosData[4] = project.endDate;
		        		cosData[5] = (project.clients != null && project.clients.size() > 0) ? project.clients.get(0).companyName : "";
		        		cosData[6] = (project.builders != null && project.builders.size() > 0) ? project.builders.get(0).companyName : "";
		        		
		        		String qpName = "";
		        		String inspectorName = "";
		        		if(project.teamAccounts != null && project.teamAccounts.size() > 0) {
		        			for(Account member : project.teamAccounts) {
		        				if(member.accType == AccountType.QP) {
		        					qpName += member.user.name + ", ";
		        				}else if(member.accType == AccountType.INSPECTOR){
		        					inspectorName += member.user.name = ",";
		        				}
		        			}
		        			
		        			if(qpName.length() > 2) {
		        				qpName = qpName.substring(0, qpName.length() - 2);
		        			}
		        			
		        			if(inspectorName.length() > 2) {
		        				inspectorName = inspectorName.substring(0, inspectorName.length() - 2);
		        			}
		        		}
		        		cosData[7] = qpName;
		        		cosData[8] = inspectorName;
					excelData.add(cosData);
					rowNum++;
				}
				
				XSSFWorkbook workbook = new XSSFWorkbook();
				XSSFSheet sheet = workbook.createSheet("Datatypes in Java");
		        rowNum = 0;
				for(Object[] data : excelData) {
					Row row = sheet.createRow(rowNum++);
		            int colNum = 0;
		            for (Object field : data) {
		                Cell cell = row.createCell(colNum++);
		                if (field instanceof String) {
		                    cell.setCellValue((String) field);
		                } else if (field instanceof Integer) {
		                    cell.setCellValue((Integer) field);
		                }
		            }
				}
				
				try {
					String FILE_NAME = "/tmp/project_list.xlsx";
		            FileOutputStream outputStream = new FileOutputStream(FILE_NAME);
		            workbook.write(outputStream);
		            workbook.close();
		            
		            File file  = new File(FILE_NAME);
		            return ok(file).withHeader("Content-Disposition", "attachment;filename=project_list.xlsx");
		        } catch (IOException e) {
		        	responseData.code = 4001;
					responseData.message = e.getMessage();
		        }
			}else {
				responseData.code = 4000;
				responseData.message = "No Project found!";
			}
		}
		return notFound(errorpage.render(responseData));
	}
	
}
