package is.idega.idegaweb.egov.bpm.cases;

import is.idega.idegaweb.egov.cases.business.CasesBusiness;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.JbpmContext;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.form.process.XFormsView;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.jbpm.def.View;
import com.idega.jbpm.exe.BPMAccessControlException;
import com.idega.jbpm.exe.ProcessConstants;
import com.idega.jbpm.exe.ProcessException;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.identity.RolesManager;
import com.idega.user.business.UserBusiness;
import com.idega.util.CoreConstants;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/05/06 21:43:25 $ by $Author: civilis $
 */
@Scope("prototype")
@Service("casesTIW")
public class CasesBPMTaskInstanceW implements TaskInstanceW {
	
	private Long taskInstanceId;
	private CasesBPMResources casesBPMResources;
	
	public void assign(int userId) {
		JbpmContext ctx = getCasesBPMResources().getIdegaJbpmContext().createJbpmContext();
		
		try {
			Long taskInstanceId = getTaskInstanceId();
			RolesManager rolesManager = getCasesBPMResources().getBpmFactory().getRolesManager();
			rolesManager.hasRightsToAssignTask(taskInstanceId, userId);
			
			TaskInstance taskInstance = ctx.getTaskInstance(taskInstanceId);
			taskInstance.setActorId(String.valueOf(userId));
			ctx.save(taskInstance);
		
		} catch (BPMAccessControlException e) {
			throw new ProcessException(e, e.getUserFriendlyMessage());
			
		} finally {
			getCasesBPMResources().getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}

	public void start(int userId) {
		JbpmContext ctx = getCasesBPMResources().getIdegaJbpmContext().createJbpmContext();
		
		try {
			Long taskInstanceId = getTaskInstanceId();
			RolesManager rolesManager = getCasesBPMResources().getBpmFactory().getRolesManager();
			rolesManager.hasRightsToStartTask(taskInstanceId, userId);
			
			TaskInstance taskInstance = ctx.getTaskInstance(taskInstanceId);
			taskInstance.start();
			
			ctx.save(taskInstance);
		
		} catch (BPMAccessControlException e) {
			throw new ProcessException(e, e.getUserFriendlyMessage());
			
		} finally {
			getCasesBPMResources().getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}

	public void submit(View view) {
		submit(view, true);
	}

	public void submit(View view, boolean proceedProcess) {
		
		JbpmContext ctx = getCasesBPMResources().getIdegaJbpmContext().createJbpmContext();
		
		try {
			Long taskInstanceId = getTaskInstanceId();
			TaskInstance taskInstance = ctx.getTaskInstance(taskInstanceId);
			
			if(taskInstance.hasEnded())
				throw new ProcessException("Task instance ("+taskInstanceId+") is already submitted", "Task instance is already submitted");
			
	    	submitVariablesAndProceedProcess(taskInstance, view.resolveVariables(), proceedProcess);
	    	ctx.save(taskInstance);
			
		} finally {
			getCasesBPMResources().getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	protected void submitVariablesAndProceedProcess(TaskInstance ti, Map<String, Object> variables, boolean proceed) {
		
		getCasesBPMResources().getVariablesHandler().submitVariables(variables, ti.getId(), true);
		
		if(proceed) {
		
			String actionTaken = (String)ti.getVariable(CasesBPMProcessConstants.actionTakenVariableName);
	    	
	    	if(actionTaken != null && !CoreConstants.EMPTY.equals(actionTaken) && false)
	    		ti.end(actionTaken);
	    	else
	    		ti.end();
		} else {
			ti.setEnd(new Date());
		}
    	
    	ti.setActorId(null);
	}
	
	public View loadView() {
		
		CasesBPMResources bpmRes = getCasesBPMResources();
		Long taskInstanceId = getTaskInstanceId();
		JbpmContext ctx = bpmRes.getIdegaJbpmContext().createJbpmContext();
		
		try {
			TaskInstance taskInstance = ctx.getTaskInstance(taskInstanceId);
			
			List<String> preferred = new ArrayList<String>(1);
			preferred.add(XFormsView.VIEW_TYPE);
			
			View view;
			
			if(taskInstance.hasEnded()) {
				
				view = bpmRes.getBpmFactory().getViewByTaskInstance(taskInstanceId, false, preferred);
				
			} else {
				
				view = bpmRes.getBpmFactory().takeView(taskInstanceId, true, preferred);
			}
			
			Map<String, String> parameters = new HashMap<String, String>(1);
			parameters.put(ProcessConstants.TASK_INSTANCE_ID, String.valueOf(taskInstance.getId()));
			view.populateParameters(parameters);
			view.populateVariables(bpmRes.getVariablesHandler().populateVariables(taskInstance.getId()));
			
			return view;
		
		} catch(RuntimeException e) {
			throw e;
		} catch(Exception e) {
			throw new RuntimeException(e);
		} finally {
			bpmRes.getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public Long getTaskInstanceId() {
		return taskInstanceId;
	}

	public CasesBPMResources getCasesBPMResources() {
		return casesBPMResources;
	}

	@Autowired(required=true)
	public void setCasesBPMResources(CasesBPMResources casesBPMResources) {
		this.casesBPMResources = casesBPMResources;
	}

	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	protected UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(iwac, UserBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}
}