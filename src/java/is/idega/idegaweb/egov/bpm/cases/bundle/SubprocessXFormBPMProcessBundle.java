package is.idega.idegaweb.egov.bpm.cases.bundle;

import is.idega.idegaweb.egov.bpm.cases.CasesBPMManagersCreator;

import java.io.IOException;
import java.util.Properties;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.def.Task;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 * Last modified: $Date: 2008/04/21 05:09:05 $ by $Author: civilis $
 * 
 */
@Scope("prototype")
@Service("subprocessXFormBPMProcessBundle")
public class SubprocessXFormBPMProcessBundle extends CasesBPMProcessBundle {

	private static final String emailTaskProp = "email_task";
	private static final String taskNamePostfixProp = ".name";

	public void configure(ProcessDefinition pd) {
		
		try {
			Properties properties = resolveBundleProperties();
			String emailTaskKey = properties.getProperty(emailTaskProp);
			String emailTaskName = properties.getProperty(emailTaskKey+taskNamePostfixProp);
			Task emailTask = pd.getTaskMgmtDefinition().getTask(emailTaskName);
			pd.getTaskMgmtDefinition().setStartTask(emailTask);
			
		} catch (IOException e) {
			throw new RuntimeException("IOException while accessing process bundle properties");
		}
	}
	
	public String getManagersType() {
		
		return CasesBPMManagersCreator.MANAGERS_TYPE;
	}
}