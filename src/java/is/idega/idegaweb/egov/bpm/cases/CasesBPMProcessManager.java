package is.idega.idegaweb.egov.bpm.cases;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.TaskInstanceW;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.20 $
 *
 * Last modified: $Date: 2008/05/16 09:38:34 $ by $Author: civilis $
 */
@Scope("singleton")
@Service("casesBpmProcessManager")
public class CasesBPMProcessManager implements ProcessManager {

	private CasesBPMResources casesBPMResources;

	public ProcessDefinitionW getProcessDefinition(long pdId) {
		
		return getCasesBPMResources().createProcessDefinition(pdId);
	}

	public ProcessInstanceW getProcessInstance(long piId) {
		
		return getCasesBPMResources().createProcessInstance(piId);
	}
	
	public TaskInstanceW getTaskInstance(long tiId) {
		
		return getCasesBPMResources().createTaskInstance(tiId);
	}

	public CasesBPMResources getCasesBPMResources() {
		return casesBPMResources;
	}

	@Autowired
	public void setCasesBPMResources(CasesBPMResources casesBPMResources) {
		this.casesBPMResources = casesBPMResources;
	}
}