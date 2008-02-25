package is.idega.idegaweb.egov.bpm.cases;

import java.io.Serializable;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.jbpm.IdegaJbpmContext;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/02/25 16:16:25 $ by $Author: civilis $
 */
public class CasesBPMProcessView {
	
	private IdegaJbpmContext idegaJbpmContext;

	public CasesBPMTaskViewBean getTaskView(long taskInstanceId) {

		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			TaskInstance ti = ctx.getTaskInstance(taskInstanceId);
			
			if(ti == null) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "No task instance found for task instance id provided: "+taskInstanceId);
				return new CasesBPMTaskViewBean();
			}
			
			CasesBPMTaskViewBean bean = new CasesBPMTaskViewBean();
			bean.setTaskName(ti.getName());
			bean.setTaskStatus("Unknown");
			bean.setAssignedTo("Unknown");
			bean.setCreatedDate(ti.getCreate());
			return bean;
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public CasesBPMProcessViewBean getProcessView(long processInstanceId) {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			ProcessInstance pi = ctx.getProcessInstance(processInstanceId);
			
			if(pi == null) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "No process instance found for process instance id provided: "+processInstanceId);
				return new CasesBPMProcessViewBean();
			}
			
			CasesBPMProcessViewBean bean = new CasesBPMProcessViewBean();
			bean.setProcessName(pi.getProcessDefinition().getName());
			bean.setProcessStatus("Unknown");
			return bean;
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public class CasesBPMProcessViewBean implements Serializable {
		
		private static final long serialVersionUID = -1209671586005809408L;
		
		private String processName;
		private String processStatus;
		
		public String getProcessName() {
			return processName;
		}
		public void setProcessName(String processName) {
			this.processName = processName;
		}
		public String getProcessStatus() {
			return processStatus;
		}
		public void setProcessStatus(String processStatus) {
			this.processStatus = processStatus;
		}
	}
	
	public class CasesBPMTaskViewBean implements Serializable {
		
		private static final long serialVersionUID = -6402627297789228878L;
		
		private String taskName;
		private String taskStatus;
		private String assignedTo;
		private Date createdDate;
		
		public String getTaskName() {
			return taskName;
		}
		public void setTaskName(String taskName) {
			this.taskName = taskName;
		}
		public String getTaskStatus() {
			return taskStatus;
		}
		public void setTaskStatus(String taskStatus) {
			this.taskStatus = taskStatus;
		}
		public String getAssignedTo() {
			return assignedTo;
		}
		public void setAssignedTo(String assignedTo) {
			this.assignedTo = assignedTo;
		}
		public Date getCreatedDate() {
			return createdDate;
		}
		public void setCreatedDate(Date createdDate) {
			this.createdDate = createdDate;
		}
	}

	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}
}