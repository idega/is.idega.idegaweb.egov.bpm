package is.idega.idegaweb.egov.bpm.cases.exe;


import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.TaskInstanceW;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/06/15 16:02:34 $ by $Author: civilis $
 */
public abstract class CasesBPMProcessManager implements ProcessManager {

	public ProcessDefinitionW getProcessDefinition(long pdId) {
		
		return createProcessDefinition(pdId);
	}

	public ProcessInstanceW getProcessInstance(long piId) {
		
		return createProcessInstance(piId);
	}
	
	public TaskInstanceW getTaskInstance(long tiId) {
		
		return createTaskInstance(tiId);
	}

	protected abstract ProcessDefinitionW createPDW();
	
//	synchronized because spring doesn't do it when autowiring beans
	public synchronized ProcessDefinitionW createProcessDefinition(long pdId) {
		
		
		ProcessDefinitionW pdw = createPDW();
		pdw.setProcessDefinitionId(pdId);
		
		return pdw;
	}

	protected abstract ProcessInstanceW createPIW();
	
//	synchronized because spring doesn't do it when autowiring beans
	public synchronized ProcessInstanceW createProcessInstance(long piId) {
		
		ProcessInstanceW piw = createPIW();
		piw.setProcessInstanceId(piId);
		
		return piw;
	}
	
	protected abstract TaskInstanceW createTIW();
	
//	synchronized because spring doesn't do it when autowiring beans
	public synchronized TaskInstanceW createTaskInstance(long tiId) {
		
		TaskInstanceW tiw = createTIW();
		tiw.setTaskInstanceId(tiId);
		
		return tiw;
	}
}