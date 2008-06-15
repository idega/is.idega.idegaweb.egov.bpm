package is.idega.idegaweb.egov.bpm.cases.exe;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
 * @version $Revision: 1.6 $
 *
 * Last modified: $Date: 2008/06/15 16:33:16 $ by $Author: civilis $
 */
@Scope("prototype")
@Service("casesPIW")
public class CasesBPMProcessInstanceW implements ProcessInstanceW {
	
	private Long processInstanceId;
	private ProcessInstance processInstance;
	
	private BPMContext idegaJbpmContext;
	private ProcessManager processManager;
	
	public List<TaskInstanceW> getAllTaskInstances() {
		
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
	
	public List<TaskInstanceW> getUnfinishedTaskInstances(Token rootToken) {
		
		ProcessInstance processInstance = rootToken.getProcessInstance();
		
		@SuppressWarnings("unchecked")
		Collection<TaskInstance> taskInstances = processInstance.getTaskMgmtInstance().getUnfinishedTasks(rootToken);

		return encapsulateInstances(taskInstances);
	}
	
	public List<TaskInstanceW> getAllUnfinishedTaskInstances() {
	
		ProcessInstance processInstance = getProcessInstance();
	
		@SuppressWarnings("unchecked")
		Collection<TaskInstance> taskInstances = processInstance.getTaskMgmtInstance().getUnfinishedTasks(processInstance.getRootToken());
		
		@SuppressWarnings("unchecked")
		List<Token> tokens = processInstance.findAllTokens();
		
		for (Token token : tokens) {
			
			if(!token.equals(processInstance.getRootToken())) {
		
				@SuppressWarnings("unchecked")
				Collection<TaskInstance> tis = processInstance.getTaskMgmtInstance().getUnfinishedTasks(token);
				taskInstances.addAll(tis);
			}
		}

		return encapsulateInstances(taskInstances);
	}
	
	private ArrayList<TaskInstanceW> encapsulateInstances(Collection<TaskInstance> taskInstances) {
		ArrayList<TaskInstanceW> instances = new ArrayList<TaskInstanceW>(taskInstances.size());
		
		for(TaskInstance instance : taskInstances) {
			TaskInstanceW tiw = getProcessManager().getTaskInstance(instance.getId());
			instances.add(tiw);
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

	public ProcessInstance getProcessInstance() {
		
		if(processInstance == null && getProcessInstanceId() != null) {
			
			JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
			
			try {
				processInstance = ctx.getProcessInstance(getProcessInstanceId());
				
			} finally {
				getIdegaJbpmContext().closeAndCommit(ctx);
			}
		}
		return processInstance;
	}
}