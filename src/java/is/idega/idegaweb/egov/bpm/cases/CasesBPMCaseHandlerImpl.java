package is.idega.idegaweb.egov.bpm.cases;

import is.idega.idegaweb.egov.cases.business.CaseHandlerPluggedInEvent;
import is.idega.idegaweb.egov.cases.presentation.CaseHandler;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/02/12 14:37:24 $ by $Author: civilis $
 */
public class CasesBPMCaseHandlerImpl implements CaseHandler, ApplicationContextAware, ApplicationListener {

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
}