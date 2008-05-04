package is.idega.idegaweb.egov.bpm.cases;

import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/05/04 18:11:48 $ by $Author: civilis $
 */
public class CasesBPMProcessInstanceW implements ProcessInstanceW {
	
	private final long processInstanceId;
	private final CasesBPMResources bpmResources;
	
	public CasesBPMProcessInstanceW(long processInstanceId, CasesBPMResources bpmResources) {
		this.processInstanceId = processInstanceId;
		this.bpmResources = bpmResources;
	}
	
	public TaskInstanceW getTaskInstance(long tiId) {
		return getBpmResources().createTaskInstance(tiId);
	}

	public long getProcessInstanceId() {
		return processInstanceId;
	}

	protected CasesBPMResources getBpmResources() {
		return bpmResources;
	}
}