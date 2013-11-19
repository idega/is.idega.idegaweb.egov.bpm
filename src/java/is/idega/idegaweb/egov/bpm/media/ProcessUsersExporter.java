package is.idega.idegaweb.egov.bpm.media;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.core.business.GeneralCompanyBusiness;
import com.idega.core.company.bean.GeneralCompany;
import com.idega.core.contact.data.Phone;
import com.idega.core.file.util.MimeTypeUtil;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.io.DownloadWriter;
import com.idega.io.MediaWritable;
import com.idega.jbpm.artifacts.presentation.ProcessArtifacts;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.presentation.IWContext;
import com.idega.user.bean.UserDataBean;
import com.idega.user.business.UserApplicationEngine;
import com.idega.user.data.User;
import com.idega.util.IOUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

public class ProcessUsersExporter extends DownloadWriter implements MediaWritable {

	public static final String PROCESS_INSTANCE_ID = "pr-inst-id";
	public static final String SHOW_USER_COMPANY = "show-u-c";

	private HSSFWorkbook workBook;
	
	@Autowired
	private BPMFactory bpmFactory;
	
	@Autowired
	private ProcessArtifacts processArtifacts;
	
	@Autowired
	UserApplicationEngine userApplicationEngine;
	
	@Autowired
	GeneralCompanyBusiness generalCompanyBusiness;

	@Override
	public String getMimeType() {
		return MimeTypeUtil.MIME_TYPE_EXCEL_2;
	}

	@Override
	public void init(HttpServletRequest req, IWContext iwc) {

		ELUtil.getInstance().autowire(this);
		String id = iwc.getParameter(PROCESS_INSTANCE_ID);
		long processInstanceId = Long.valueOf(id);
		ProcessManager processManager = bpmFactory.getProcessManagerByProcessInstanceId(processInstanceId);
		ProcessInstanceW piw = processManager.getProcessInstance(processInstanceId);
		Collection<User> users = processArtifacts.getUsersConnectedToProces(piw);

		workBook = new HSSFWorkbook();

		HSSFFont bigFont = workBook.createFont();
		bigFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		bigFont.setFontHeightInPoints((short) 16);
		HSSFCellStyle bigStyle = workBook.createCellStyle();
		bigStyle.setFont(bigFont);
		
		HSSFFont normalFont = workBook.createFont();
		normalFont.setFontHeightInPoints((short) 16);
		HSSFCellStyle normalStyle = workBook.createCellStyle();
		normalStyle.setFont(normalFont);

		String fileNameLabel = "File name";
		IWResourceBundle bpmIwrb = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc);
		fileNameLabel = bpmIwrb.getLocalizedString("cases_bpm.file_name", fileNameLabel);
		
		String fileName = bpmIwrb.getLocalizedString("exported_search_results_in_excel_file_name", "Exported search results");
		
		HSSFSheet sheet = workBook.createSheet();
		
		int columnWidth = 10240;
		int rowNum = 0;
		HSSFRow row = sheet.createRow(rowNum++);
		
		int column = 0;
		
		sheet.setColumnWidth(column, columnWidth);
		HSSFCell cell = row.createCell(column++);
		cell.setCellStyle(bigStyle);
		cell.setCellValue(bpmIwrb.getLocalizedString("name", "Name"));
		
		boolean showUserCompany = "y".equals(iwc.getParameter(SHOW_USER_COMPANY));
		if(showUserCompany){
			sheet.setColumnWidth(column, columnWidth);
			cell = row.createCell(column++);
			cell.setCellStyle(bigStyle);
			cell.setCellValue(bpmIwrb.getLocalizedString("cases_bpm.company", "Company"));
		}
		
		sheet.setColumnWidth(column, columnWidth);
		cell = row.createCell(column++);
		cell.setCellStyle(bigStyle);
		cell.setCellValue(bpmIwrb.getLocalizedString("email_address", "E-mail address"));
		
		sheet.setColumnWidth(column, columnWidth);
		cell = row.createCell(column++);
		cell.setCellStyle(bigStyle);
		cell.setCellValue(bpmIwrb.getLocalizedString("phone_number", "Phone number"));
		
		
		for(User user : users){
			UserDataBean userDataBean = userApplicationEngine.getUserInfo(user);
			row = sheet.createRow(rowNum++);
			
			column = 0;
			
			cell = row.createCell(column++);
			cell.setCellStyle(normalStyle);
			cell.setCellValue(userDataBean.getName());
			
			if(showUserCompany){
				cell = row.createCell(column++);
				cell.setCellStyle(normalStyle);
				Collection<GeneralCompany> companies = generalCompanyBusiness.getJBPMCompaniesForUser(user);
				String companyName;
				if(!ListUtil.isEmpty(companies)){
					GeneralCompany company = companies.iterator().next();
					companyName = company.getName();
				}else{
					companyName = "-";
				}
				cell.setCellValue(companyName);
			}
			
			
			cell = row.createCell(column++);
			cell.setCellStyle(normalStyle);
			cell.setCellValue(userDataBean.getEmail());
			
			@SuppressWarnings("unchecked")
			Collection<Phone> phones = user.getPhones();
			StringBuilder userPhones = new StringBuilder();
			for(Phone phone : phones){
				String number = phone.getNumber();
				if(StringUtil.isEmpty(number)){
					continue;
				}
				userPhones.append(number).append("; ");
			}
			cell = row.createCell(column++);
			cell.setCellStyle(normalStyle);
			cell.setCellValue(userPhones.toString());
			
		}
		// TODO: count content length
		setAsDownload(iwc, fileName + ".xls",0);
	}

	@Override
	public void writeTo(OutputStream streamOut) throws IOException {
		try {
			if(workBook != null){
				workBook.write(streamOut);
			}
		} catch (Exception e) {
			Logger.getLogger(ProcessUsersExporter.class.getName()).log(Level.WARNING, "Error writing search results to Excel!", e);
			return;
		} finally {
			IOUtil.closeOutputStream(streamOut);
		}
		streamOut.flush();
		streamOut.close();
	}

}