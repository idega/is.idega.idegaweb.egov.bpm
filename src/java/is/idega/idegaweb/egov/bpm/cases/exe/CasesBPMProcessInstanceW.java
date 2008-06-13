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

import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/06/13 11:55:50 $ by $Author: anton $
 */
@Scope("prototype")
@Service("casesPIW")
public class CasesBPMProcessInstanceW implements ProcessInstanceW {
	
	private Long processInstanceId;
	private CasesBPMResources casesBPMResources;
	
	public TaskInstanceW getTaskInstance(long tiId) {
		return getCasesBPMResources().createTaskInstance(tiId);
	}
	
	public Collection<TaskInstanceW> getTaskInstances(long processInstanceId, JbpmContext ctx) {
		ProcessInstance processInstance = ctx.getProcessInstance(processInstanceId);
		
		@SuppressWarnings("unchecked")
		Collection<TaskInstance> taskInstances = processInstance.getTaskMgmtInstance().getTaskInstances();
		
		return encapsulateInstances(taskInstances);
	}
	
	public Collection<TaskInstanceW> getUnfinishedTasks(long processInstanceId, Token rootToken, JbpmContext ctx) {
		ProcessInstance processInstance = ctx.getProcessInstance(processInstanceId);
		
		@SuppressWarnings("unchecked")
		Collection<TaskInstance> taskInstances = processInstance.getTaskMgmtInstance().getUnfinishedTasks(rootToken);

		return encapsulateInstances(taskInstances);
	}
	
	private Collection<TaskInstanceW> encapsulateInstances(Collection<TaskInstance> taskInstances) {
		Collection<TaskInstanceW> instances = new ArrayList<TaskInstanceW>();
		
		for(TaskInstance instance : taskInstances) {
			TaskInstanceW taskW = getTaskInstance(instance.getId());
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
	
	public CasesBPMResources getCasesBPMResources() {
		return casesBPMResources;
	}

	@Autowired(required=true)
	public void setCasesBPMResources(CasesBPMResources casesBPMResources) {
		this.casesBPMResources = casesBPMResources;
	}
}