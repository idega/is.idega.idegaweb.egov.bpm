package is.idega.idegaweb.egov.bpm.cases.exe;


import java.util.ArrayList;
import java.util.Collection;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/06/15 12:07:28 $ by $Author: civilis $
 */
@Scope("prototype")
@Service("casesPIW")
public class CasesBPMProcessInstanceW implements ProcessInstanceW {
	
	private Long processInstanceId;
	private IdegaJbpmContext idegaJbpmContext;
	private CasesBPMProcessManager casesBPMProcessManager;
	
	public Collection<TaskInstanceW> getAllTaskInstances() {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			ProcessInstance processInstance = ctx.getProcessInstance(processInstanceId);
			
			@SuppressWarnings("unchecked")
			Collection<TaskInstance> taskInstances = processInstance.getTaskMgmtInstance().getTaskInstances();
			return encapsulateInstances(taskInstances);
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public Collection<TaskInstanceW> getUnfinishedTaskInstances(Token rootToken) {
		
		ProcessInstance processInstance = rootToken.getProcessInstance();
		
		@SuppressWarnings("unchecked")
		Collection<TaskInstance> taskInstances = processInstance.getTaskMgmtInstance().getUnfinishedTasks(rootToken);

		return encapsulateInstances(taskInstances);
	}
	
	private Collection<TaskInstanceW> encapsulateInstances(Collection<TaskInstance> taskInstances) {
		Collection<TaskInstanceW> instances = new ArrayList<TaskInstanceW>();
		
		for(TaskInstance instance : taskInstances) {
			TaskInstanceW taskW = getCasesBPMProcessManager().getTaskInstance(instance.getId());
			instances.add(taskW);
		}
		
		return instances;
	}

	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}
	
	public CasesBPMProcessManager getCasesBPMProcessManager() {
		return casesBPMProcessManager;
	}

	@Autowired
	public void setCasesBPMProcessManager(
			CasesBPMProcessManager casesBPMProcessManager) {
		this.casesBPMProcessManager = casesBPMProcessManager;
	}
}