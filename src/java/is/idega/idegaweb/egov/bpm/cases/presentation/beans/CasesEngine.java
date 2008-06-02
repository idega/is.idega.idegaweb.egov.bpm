package is.idega.idegaweb.egov.bpm.cases.presentation.beans;


import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessView;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.FinderException;
import javax.faces.component.UIComponent;

import org.chiba.xml.dom.DOMUtil;
import org.directwebremoting.util.DomUtil;
import org.jdom.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.business.CaseBusiness;
import com.idega.block.process.business.CaseManagersProvider;
import com.idega.block.process.data.Case;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.accesscontrol.business.NotLoggedOnException;
import com.idega.core.builder.business.BuilderService;
import com.idega.core.builder.business.BuilderServiceFactory;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;

@Service("casesEngineDWR")
@Scope("singleton")
public class CasesEngine {
	
	private BPMFactory bpmFactory;
	private CaseManagersProvider caseManagersProvider;
	private CasesBPMProcessView casesBPMProcessView;
	
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
		localizations.add(FILE_DOWNLOAD_LINK_STYLE_CLASS);														//	15
		localizations.add(PDF_GENERATOR_AND_DOWNLOAD_LINK_STYLE_CLASS);											//	16
		
		localizations.add(iwrb.getLocalizedString("cases_bpm.file_description", "Descriptive name"));			//  17
		localizations.add(iwrb.getLocalizedString("click_to_edit", "Click to edit..."));						//	18
		
		return localizations;
	}
	
	public Long getProcessInstanceId(String caseId) {
		
		if (caseId != null) {
			
			Long processInstanceId = getCasesBPMProcessView().getProcessInstanceId(caseId);
			return processInstanceId;
		}
		
		return null;
	}
	
	public Document getCaseManagerView(String caseIdStr) {
		
		IWContext iwc = IWContext.getInstance();
		
		if (caseIdStr == null || CoreConstants.EMPTY.equals(caseIdStr) || iwc == null) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Either not provided:\n caseId="+caseIdStr+", iwc="+iwc);
			return null;
		}
		
		try {
			Integer caseId = new Integer(caseIdStr);
			UIComponent caseAssets = getCasesBPMProcessView().getCaseManagerView(iwc, null, caseId);
			
			BuilderService service = BuilderServiceFactory.getBuilderService(iwc);
			Document rendered = service.getRenderedComponent(iwc, caseAssets, true);
			
			return rendered;
			
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while resolving rendered component for case assets view", e);
		}
		
		return null;
	}
	
	public boolean setCaseSubject(String caseId, String subject) {
		if (caseId == null || subject == null) {
			return false;
		}
	
		CaseBusiness caseBusiness = null;
		try {
			caseBusiness = (CaseBusiness) IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), CaseBusiness.class);
		} catch (IBOLookupException e) {
			e.printStackTrace();
		}
		if (caseBusiness == null) {
			return false;
		}
		
		Case theCase = null;
		try {
			theCase = caseBusiness.getCase(caseId);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (FinderException e) {
			e.printStackTrace();
		}
		if (theCase == null) {
			return false;
		}
		
		theCase.setSubject(subject);
		theCase.store();
		
		return true;
	}
	
	public String takeBPMProcessTask(Long taskInstanceId, boolean reAssign) {
		if (taskInstanceId == null) {
			return null;
		}
		
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}
		
		User currentUser = null;
		try {
			currentUser = iwc.getCurrentUser();
		} catch(NotLoggedOnException e) {
			e.printStackTrace();
		}
		if (currentUser == null) {
			return null;
		}
		
		try {
			ProcessManager processManager = getBpmFactory().getProcessManagerByTaskInstanceId(taskInstanceId);
			TaskInstanceW taskInstance = processManager.getTaskInstance(taskInstanceId);
			
			User assignedTo = taskInstance.getAssignedTo();
			if (assignedTo != null && !reAssign) {
				return assignedTo.getName();
			}
			else {
				taskInstance.assign(currentUser);
			}
			
			return currentUser.getName(); 
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	@Autowired
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	public CaseManagersProvider getCaseManagersProvider() {
		return caseManagersProvider;
	}

	@Autowired
	public void setCaseManagersProvider(CaseManagersProvider caseManagersProvider) {
		this.caseManagersProvider = caseManagersProvider;
	}

	public CasesBPMProcessView getCasesBPMProcessView() {
		return casesBPMProcessView;
	}

	@Autowired
	public void setCasesBPMProcessView(CasesBPMProcessView casesBPMProcessView) {
		this.casesBPMProcessView = casesBPMProcessView;
	}
}