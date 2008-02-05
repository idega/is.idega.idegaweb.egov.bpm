package is.idega.idegaweb.egov.bpm.application;

import java.util.List;

import is.idega.idegaweb.egov.application.business.ApplicationType;
import is.idega.idegaweb.egov.application.business.ApplicationTypePluggedInEvent;
import is.idega.idegaweb.egov.application.data.Application;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.jbpm.graph.def.ProcessDefinition;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.idega.jbpm.data.dao.BpmBindsDAO;
import com.idega.presentation.IWContext;
import com.idega.presentation.ui.DropdownMenu;

/**
 * Interface is meant to be extended by beans, reflecting application type for egov applications
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/02/05 19:32:16 $ by $Author: civilis $
 *
 */
public class ApplicationTypeBPM implements ApplicationType, ApplicationContextAware, ApplicationListener {

	private ApplicationContext ctx;
	private BpmBindsDAO bpmBindsDAO;
	public static final String beanIdentifier = "appTypeBPM";
	private static final String appType = "EGOV_BPM";
	
	public UIComponent getHandlerComponent(FacesContext ctx, Application app) {
		
		UIApplicationTypeBPMHandler h = new UIApplicationTypeBPMHandler();
		h.setApplication(app);
		return h;
	}

	public String getLabel(IWContext iwc) {
		return "EGOV BPM";
	}

	public String getType() {
		return appType;
	}

	public void save(IWContext iwc, Application app) {
		
		String procDef = iwc.getParameter(UIApplicationTypeBPMHandler.menuParam);
		System.out.println("saving ..BPM... "+procDef);
	}

	public void setApplicationContext(ApplicationContext applicationcontext)
			throws BeansException {
		ctx = applicationcontext;		
	}

	public void onApplicationEvent(ApplicationEvent applicationevent) {
		
		if(applicationevent instanceof ContextRefreshedEvent) {
			
			ApplicationTypePluggedInEvent event = new ApplicationTypePluggedInEvent(this);
			event.setAppTypeBeanIdentifier(beanIdentifier);
			ctx.publishEvent(event);
		}
	}
	
	public BpmBindsDAO getBpmBindsDAO() {
		return bpmBindsDAO;
	}

	public void setBpmBindsDAO(BpmBindsDAO bpmBindsDAO) {
		this.bpmBindsDAO = bpmBindsDAO;
	}
	
	public void fillMenu(DropdownMenu menu) {

		List<ProcessDefinition> procDefs = getBpmBindsDAO().getAllManagersTypeProcDefs();
		
		
		if(procDefs != null) {
			
			for (ProcessDefinition processDefinition : procDefs) {
				
				menu.addMenuElement(String.valueOf(processDefinition.getId()), processDefinition.getName());
			}
		}
	}
}