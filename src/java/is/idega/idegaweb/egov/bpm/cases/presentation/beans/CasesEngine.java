package is.idega.idegaweb.egov.bpm.cases.presentation.beans;


import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessView;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;

import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.FinderException;
import javax.faces.component.UIComponent;

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
import com.idega.core.builder.business.BuilderService;
import com.idega.core.builder.business.BuilderServiceFactory;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.presentation.IWContext;
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
	
	public Long getProcessInstanceId(String caseId) {
		
		if (caseId != null) {
			
			Long processInstanceId = getCasesBPMProcessView().getProcessInstanceId(caseId);
			return processInstanceId;
		}
		
		return null;
	}
	
	public Document getCaseManagerView(String caseIdStr) {
		IWContext iwc = CoreUtil.getIWContext();
		
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