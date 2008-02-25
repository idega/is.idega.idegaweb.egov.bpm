package is.idega.idegaweb.egov.bpm.cases;

import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.presentation.UICasesBPMAssets;
import is.idega.idegaweb.egov.bpm.cases.presentation.UICasesBPMTakeWatch;
import is.idega.idegaweb.egov.cases.business.CaseHandlerPluggedInEvent;
import is.idega.idegaweb.egov.cases.data.GeneralCase;
import is.idega.idegaweb.egov.cases.presentation.CaseHandler;
import is.idega.idegaweb.egov.cases.presentation.CasesProcessor;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.presentation.IWContext;
import com.idega.presentation.text.Link;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/02/25 16:16:25 $ by $Author: civilis $
 */
public class CasesBPMCaseHandlerImpl implements CaseHandler, ApplicationContextAware, ApplicationListener {

	public static final String PARAMETER_PROCESS_INSTANCE_PK = "pr_inst_pk";
	
	private ApplicationContext ctx;
	private static final String beanIdentifier = "casesBPMCaseHandler";
	public static final String caseHandlerType = "CasesBPM";
	
	private static final String PARAMETER_ACTION = "cbcAct";
	private static final String ACTION_OPEN_PROCESS = "cbcActOP";
	private static final String ACTION_SHOW_WATCH = "cbcActSW";
	
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

	public List<Link> getCaseLinks(GeneralCase theCase) {
		
		IWMainApplication iwma = IWMainApplication.getIWMainApplication(FacesContext.getCurrentInstance());
		IWBundle bundle = iwma.getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		
		List<Link> links = new ArrayList<Link>();
		
		
		Link link = new Link(bundle.getImage("images/folder-exec-16x16.png", bundle.getLocalizedString("openBPMProcess", "Open BPM process")));
		
		link.addParameter(PARAMETER_PROCESS_INSTANCE_PK, String.valueOf(theCase.getCaseHandler()));
		link.addParameter(CasesProcessor.PARAMETER_CASE_PK, theCase.getPrimaryKey().toString());
		link.addParameter(CasesProcessor.PARAMETER_ACTION, CasesProcessor.SHOW_CASE_HANDLER);
		link.addParameter(PARAMETER_ACTION, ACTION_OPEN_PROCESS);
		
		links.add(link);

		return links;
	}

	public UIComponent getView(IWContext iwc, GeneralCase theCase) {
		
		String action = iwc.getParameter(PARAMETER_ACTION);
		
		if(ACTION_SHOW_WATCH.equals(action)) {
			
			FacesContext context = FacesContext.getCurrentInstance();
			UICasesBPMTakeWatch takeWatch = (UICasesBPMTakeWatch)context.getApplication().createComponent(UICasesBPMTakeWatch.COMPONENT_TYPE);
			takeWatch.setId(context.getViewRoot().createUniqueId());
			return takeWatch;
			
		} else {
		
			FacesContext context = FacesContext.getCurrentInstance();
			UICasesBPMAssets assets = (UICasesBPMAssets)context.getApplication().createComponent(UICasesBPMAssets.COMPONENT_TYPE);
			assets.setId(context.getViewRoot().createUniqueId());
			return assets;
		}
	}

	public boolean isDisplayedInList(GeneralCase theCase) {
		return true;
	}
}