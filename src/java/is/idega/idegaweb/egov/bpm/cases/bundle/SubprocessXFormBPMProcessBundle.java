package is.idega.idegaweb.egov.bpm.cases.bundle;

import is.idega.idegaweb.egov.bpm.cases.exe.CasesBPMManagersCreator;

import java.io.IOException;
import java.util.Properties;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.def.Task;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * TODO: what's the point of this class, and why it is called generically, though does something very specific
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 * 
 * Last modified: $Date: 2008/12/02 09:29:59 $ by $Author: civilis $
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