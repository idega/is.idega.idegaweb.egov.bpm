package is.idega.idegaweb.egov.bpm.cases.exe;


import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Resource;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.BPMContext;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.TaskInstanceW;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2008/06/15 16:03:17 $ by $Author: civilis $
 */
@Scope("prototype")
@Service("casesPIW")
public class CasesBPMProcessInstanceW implements ProcessInstanceW {
	
	private Long processInstanceId;
	
	private BPMContext idegaJbpmContext;
	private ProcessManager processManager;
	
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
			TaskInstanceW taskW = getProcessManager().getTaskInstance(instance.getId());
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

	public BPMContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Required
	@Autowired
	public void setIdegaJbpmContext(BPMContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public ProcessManager getProcessManager() {
		return processManager;
	}

	@Required
	@Resource(name="casesBpmProcessManager")
	public void setProcessManager(ProcessManager processManager) {
		this.processManager = processManager;
	}
}