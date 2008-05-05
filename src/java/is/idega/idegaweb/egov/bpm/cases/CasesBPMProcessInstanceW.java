package is.idega.idegaweb.egov.bpm.cases;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/05/05 12:17:42 $ by $Author: civilis $
 */
@Scope("prototype")
@Service("casesPIW")
public class CasesBPMProcessInstanceW implements ProcessInstanceW {
	
	private Long processInstanceId;
	private CasesBPMResources bpmResources;
	
	public TaskInstanceW getTaskInstance(long tiId) {
		return getBpmResources().createTaskInstance(tiId);
	}

	protected CasesBPMResources getBpmResources() {
		return bpmResources;
	}

	@Autowired
	public void setBpmResources(CasesBPMResources bpmResources) {
		this.bpmResources = bpmResources;
	}

	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}
}