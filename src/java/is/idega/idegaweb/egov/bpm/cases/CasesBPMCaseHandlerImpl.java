package is.idega.idegaweb.egov.bpm.cases;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.presentation.UICasesBPMAssets;
import is.idega.idegaweb.egov.cases.business.CaseHandlerPluggedInEvent;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;
import is.idega.idegaweb.egov.cases.presentation.CasesProcessor;
import is.idega.idegaweb.egov.cases.presentation.MyCases;
import is.idega.idegaweb.egov.cases.presentation.OpenCases;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.idega.block.process.business.CaseManager;
import com.idega.block.process.data.Case;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.ProcessUserBind;
import com.idega.idegaweb.egov.bpm.data.ProcessUserBind.Status;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.presentation.IWContext;
import com.idega.presentation.text.Link;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.7 $
 *
 * Last modified: $Date: 2008/02/26 17:58:24 $ by $Author: civilis $
 */
public class CasesBPMCaseHandlerImpl implements CaseManager, ApplicationContextAware, ApplicationListener {

	public static final String PARAMETER_PROCESS_INSTANCE_PK = "pr_inst_pk";
	
	private ApplicationContext ctx;
	private CasesBPMDAO casesBPMDAO;
	private static final String beanIdentifier = "casesBPMCaseHandler";
	public static final String caseHandlerType = "CasesBPM";
	
	private static final String PARAMETER_ACTION = "cbcAct";
	private static final String ACTION_OPEN_PROCESS = "cbcActOP";
	
	public void setApplicationContext(ApplicationContext applicationcontext)
			throws BeansException {
		ctx = applicationcontext;
	}

	public void onApplicationEvent(ApplicationEvent applicationevent) {
		
		if(applicationevent instanceof ContextRefreshedEvent) {
			
			//publish xforms factory registration
			ctx.publishEvent(new CaseHandlerPluggedInEvent(this));
		}
	}

	public String getBeanIdentifier() {
		return beanIdentifier;
	}

	public String getType() {
		return caseHandlerType;
	}

	public List<Link> getCaseLinks(Case theCase) {
		
		IWMainApplication iwma = IWMainApplication.getIWMainApplication(FacesContext.getCurrentInstance());
		IWBundle bundle = iwma.getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		
		List<Link> links = new ArrayList<Link>();
		
		
		Link link = new Link(bundle.getImage("images/folder-exec-16x16.png", bundle.getLocalizedString("openBPMProcess", "Open BPM process")));
		
		link.addParameter(PARAMETER_PROCESS_INSTANCE_PK, String.valueOf(theCase.getCaseManagerType()));
		link.addParameter(CasesProcessor.PARAMETER_CASE_PK, theCase.getPrimaryKey().toString());
		link.addParameter(CasesProcessor.PARAMETER_ACTION, CasesProcessor.SHOW_CASE_HANDLER);
		link.addParameter(PARAMETER_ACTION, ACTION_OPEN_PROCESS);
		
		links.add(link);

		return links;
	}

	public UIComponent getView(IWContext iwc, Case theCase) {
		
		FacesContext context = FacesContext.getCurrentInstance();
		UICasesBPMAssets assets = (UICasesBPMAssets)context.getApplication().createComponent(UICasesBPMAssets.COMPONENT_TYPE);
		assets.setId(context.getViewRoot().createUniqueId());
		return assets;
	}

	public boolean isDisplayedInList(Case theCase) {
		return true;
	}

	public Collection<? extends Case> getCases(User user, String casesComponentType) {
		
		IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
		
		try {
			CasesBusiness casesBusiness = getCasesBusiness(iwc);
			UserBusiness userBusiness = getUserBusiness(iwc);
			Collection<GeneralCase> cases;
			
			if(OpenCases.TYPE.equals(casesComponentType)) {
				
				cases = OpenCases.getOpenCases(user, iwc.getIWMainApplication(), iwc, userBusiness, casesBusiness, new String[] {getType()});
				
			} else if(MyCases.TYPE.equals(casesComponentType)) {
				
				cases = OpenCases.getOpenCases(user, iwc.getIWMainApplication(), iwc, userBusiness, casesBusiness, new String[] {getType()});
				
				HashMap<Integer, GeneralCase> casesNPKs = new HashMap<Integer, GeneralCase>(cases.size());
				
				for (GeneralCase caze : cases) {
					
					casesNPKs.put(new Integer(caze.getPrimaryKey().toString()), caze);
				}
				
				List<ProcessUserBind> binds = getCasesBPMDAO().getProcessUserBinds(new Integer(user.getPrimaryKey().toString()), casesNPKs.keySet());
				List<GeneralCase> casesToReturn = new ArrayList<GeneralCase>(binds.size());
				
				for (ProcessUserBind processUserBind : binds) {

					if(Status.PROCESS_WATCHED == processUserBind.getStatus()) {
						
						if(casesNPKs.containsKey(processUserBind.getCaseProcessBind().getCaseId())) {
							
							casesToReturn.add(casesNPKs.get(processUserBind.getCaseProcessBind().getCaseId()));
						}
					}
				}
				
				cases = casesToReturn;
				
			} else
				cases = null;
			
			return cases;
			
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}
	
	public CasesBusiness getCasesBusiness(IWContext iwc) {
		
		try {
			return (CasesBusiness)IBOLookup.getServiceInstance(iwc, CasesBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	public UserBusiness getUserBusiness(IWContext iwc) {
		
		try {
			return (UserBusiness)IBOLookup.getServiceInstance(iwc, UserBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}
}