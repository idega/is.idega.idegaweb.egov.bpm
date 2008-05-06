package is.idega.idegaweb.egov.bpm.cases;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/05/06 21:43:25 $ by $Author: civilis $
 */
@Scope("prototype")
@Service("casesPIW")
public class CasesBPMProcessInstanceW implements ProcessInstanceW {
	
	private Long processInstanceId;
	private CasesBPMResources casesBPMResources;
	
	public TaskInstanceW getTaskInstance(long tiId) {
		return getCasesBPMResources().createTaskInstance(tiId);
	}

	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}
	
	public CasesBPMResources getCasesBPMResources() {
		return casesBPMResources;
	}

	@Autowired(required=true)
	public void setCasesBPMResources(CasesBPMResources casesBPMResources) {
		this.casesBPMResources = casesBPMResources;
	}
}