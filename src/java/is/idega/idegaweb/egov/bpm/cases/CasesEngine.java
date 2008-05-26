package is.idega.idegaweb.egov.bpm.cases;


import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.presentation.beans.CasesBPMAssetsState;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;

import org.jdom.Document;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.SpringBeanLookup;
import com.idega.core.builder.business.BuilderService;
import com.idega.core.builder.business.BuilderServiceFactory;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.IWContext;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;

@Service("casesEngineDWR")
@Scope("singleton")
public class CasesEngine {
	
	public static final String FILE_DOWNLOAD_LINK_STYLE_CLASS = "casesBPMAttachmentDownloader";
	public static final String PDF_GENERATOR_AND_DOWNLOAD_LINK_STYLE_CLASS = "casesBPMPDFGeneratorAndDownloader";
	
	public List<String> getLocalizedStrings() {
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}
		
		IWResourceBundle iwrb = null;
		try {
			iwrb = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc);
		} catch(Exception e) {
			e.printStackTrace();
		}
		if (iwrb == null) {
			return null;
		}
		
		List<String> localizations = new ArrayList<String>();
		
		localizations.add(iwrb.getLocalizedString("cases_bpm.human_name", "Name"));								//	0
		localizations.add(iwrb.getLocalizedString("sender", "Sender"));											//	1
		localizations.add(iwrb.getLocalizedString("date", "Date"));												//	2
		localizations.add(iwrb.getLocalizedString("cases_bpm.assigned_to", "Taken by"));						//	3
		localizations.add(iwrb.getLocalizedString("email_address", "E-mail address"));							//	4
		localizations.add(iwrb.getLocalizedString("phone_number", "Phone number"));								//	5
		localizations.add(iwrb.getLocalizedString("address", "Address"));										//	6
		localizations.add(iwrb.getLocalizedString("cases_bpm.subject", "Subject"));								//	7
		localizations.add(iwrb.getLocalizedString("cases_bpm.file_name", "File name"));							//	8
		localizations.add(iwrb.getLocalizedString("cases_bpm.change_access_rights", "Change access rights"));	//	9
		localizations.add(iwrb.getLocalizedString("cases_bpm.task_name", "Task name"));							//	10
		localizations.add(iwrb.getLocalizedString("cases_bpm.document_name", "Document name"));					//	11
		localizations.add(iwrb.getLocalizedString("cases_bpm.get_document_as_pdf", "Download document"));		//	12
		localizations.add(iwrb.getLocalizedString("cases_bpm.file_size", "File size"));							//	13
		localizations.add(iwrb.getLocalizedString("cases_bpm.submitted_by", "Submitted by"));					//	14
		
		//	Other info
		localizations.add(FILE_DOWNLOAD_LINK_STYLE_CLASS);								//	15
		localizations.add(PDF_GENERATOR_AND_DOWNLOAD_LINK_STYLE_CLASS);					//	16
		
		return localizations;
	}
	
	public Long getProcessInstanceId(String caseId) {
		if (caseId == null) {
			return null;
		}
		
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}
		
		CasesBPMAssetsState assetsState = null;
		try {
			assetsState = (CasesBPMAssetsState) SpringBeanLookup.getInstance().getSpringBean(iwc.getServletContext(), CasesBPMAssetsState.beanIdentifier);
		} catch(Exception e) {
			e.printStackTrace();
		}
		if (assetsState == null) {
			return null;
		}
		
		try {
			assetsState.setCaseId(Integer.valueOf(caseId));
		} catch(NumberFormatException e) {
			e.printStackTrace();
			return null;
		}
		return assetsState.getProcessInstanceId();
	}

	public Document getInfoForCase(String caseId) {
		if (caseId == null || CoreConstants.EMPTY.equals(caseId)) {
			return null;
		}
		
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}
		
		CasesBPMCaseHandlerImpl casesHandler = null;
		try {
			casesHandler = (CasesBPMCaseHandlerImpl) SpringBeanLookup.getInstance().getSpringBean(iwc.getServletContext(), CasesBPMCaseHandlerImpl.beanIdentifier);
		} catch(Exception e) {
			e.printStackTrace();
		}
		if (casesHandler == null) {
			return null;
		}
		
		UIComponent caseInfo = null;
		try {
			caseInfo = casesHandler.getView(iwc, casesHandler.getCasesBusiness(iwc).getCase(caseId));
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (caseInfo == null) {
			return null;
		}
		
		BuilderService service = null;
		try {
			service = BuilderServiceFactory.getBuilderService(iwc);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		if (service == null) {
			return null;
		}
		return service.getRenderedComponent(iwc, caseInfo, true);
	}
	
}
