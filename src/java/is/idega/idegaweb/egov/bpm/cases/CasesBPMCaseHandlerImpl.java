package is.idega.idegaweb.egov.bpm.cases;

import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.presentation.UICasesBPMAssets;
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
import com.idega.presentation.text.Link;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/02/14 15:49:53 $ by $Author: civilis $
 */
public class CasesBPMCaseHandlerImpl implements CaseHandler, ApplicationContextAware, ApplicationListener {

	public static final String PARAMETER_PROCESS_INSTANCE_PK = "pr_inst_pk";
	
	private ApplicationContext ctx;
	private static final String beanIdentifier = "casesBPMCaseHandler";
	public static final String caseHandlerType = "CasesBPM";
	
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
		
		
		Link link = new Link(bundle.getImage("images/folder-open3-16x16.png", bundle.getLocalizedString("openBPMProcess", "Open BPM process")));
		
		link.addParameter(PARAMETER_PROCESS_INSTANCE_PK, String.valueOf(theCase.getCaseHandler()));
		link.addParameter(CasesProcessor.PARAMETER_CASE_PK, theCase.getPrimaryKey().toString());
		link.addParameter(CasesProcessor.PARAMETER_ACTION, CasesProcessor.ACTION_CASE_HANDLER_INVOLVED);
		
		links.add(link);

		/*
		link = new Link(getBundle().getImage("images/folder-exec-16x16.png", getResourceBundle().getLocalizedString(getPrefix() + "view_case", "Take BPM process to My Cases")));
		
		link.addParameter(PARAMETER_PROCESS_INSTANCE_PK, String.valueOf(theCase.getBPMProcessInstanceId()));
		link.addParameter(PARAMETER_CASE_PK, theCase.getPrimaryKey().toString());
		link.addParameter(PARAMETER_ACTION, ACTION_JBPM_PROCESS_TASKS_LIST);
		
		links.add(link);
		*/
		
		return links;
	}

	public UIComponent getView(GeneralCase theCase) {
		
		FacesContext context = FacesContext.getCurrentInstance();
		UICasesBPMAssets assets = (UICasesBPMAssets)context.getApplication().createComponent(UICasesBPMAssets.COMPONENT_TYPE);
		assets.setId(context.getViewRoot().createUniqueId());
		
		return assets;
	}
}