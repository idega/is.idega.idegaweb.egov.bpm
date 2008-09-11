package is.idega.idegaweb.egov.bpm.cases.exe;


import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessConstants;
import is.idega.idegaweb.egov.bpm.cases.actionhandlers.CaseHandlerAssignmentHandler;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.FinderException;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.ProcessWatch;
import com.idega.jbpm.exe.ProcessWatchType;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.identity.BPMUser;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.permission.BPMTypedPermission;
import com.idega.jbpm.identity.permission.PermissionsFactory;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.user.util.UserComparator;
import com.idega.util.CoreUtil;

/**
 * TODO: we could create abstract class for some generic methods, like getPeopleConntectedToTheProcess
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.15 $
 *
 * Last modified: $Date: 2008/09/11 17:21:44 $ by $Author: civilis $
 */
@Scope("prototype")
@Service("casesPIW")
public class CasesBPMProcessInstanceW implements ProcessInstanceW {
	
	private Long processInstanceId;
	private ProcessInstance processInstance;
	
	private BPMContext idegaJbpmContext;
	private ProcessManager processManager;
	@Autowired
	private CasesBPMDAO casesBPMDAO;
	@Autowired
	private BPMFactory bpmFactory;
	@Autowired
	private PermissionsFactory permissionsFactory;
	
	@Autowired
	@ProcessWatchType("cases")
	private ProcessWatch processWatcher;
	
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

	public void assignHandler(Integer handlerUserId) {
		
		ProcessInstance pi = getProcessInstance();
		
//		creating new token, so there are no race conditions for variables
		Token tkn = new Token(pi.getRootToken(), "assignUnassignHandler_"+System.currentTimeMillis());
		
		ExecutionContext ectx = new ExecutionContext(tkn);
		
		IWContext iwc = IWContext.getCurrentInstance();
		Integer performerId = iwc.getCurrentUserId();
		
		ectx.setVariable(CaseHandlerAssignmentHandler.performerUserIdVarName, performerId);
		
		if(handlerUserId != null)
			ectx.setVariable(CaseHandlerAssignmentHandler.handlerUserIdVarName, handlerUserId);
		
		pi.getProcessDefinition().fireEvent(handlerUserId != null ? CaseHandlerAssignmentHandler.assignHandlerEventType : CaseHandlerAssignmentHandler.unassignHandlerEventType, ectx);
	}
	
	private IWApplicationContext getIWAC() {
		
		final IWContext iwc = IWContext.getCurrentInstance();
		final IWApplicationContext iwac;
//		trying to get iwma from iwc, if available, downgrading to default iwma, if not
		
		if(iwc != null) {
			
			iwac = iwc;
			
		} else {
			iwac = IWMainApplication.getDefaultIWApplicationContext();
		}
		
		return iwac;
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

	public String getProcessDescription() {

		String description = (String) getProcessInstance().getContextInstance().getVariable(CasesBPMProcessConstants.caseDescription);
		
		return description;
	}
	
	public String getCaseIdentifier() {
	    
		   String identifier = (String) getProcessInstance().getContextInstance().getVariable(CasesBPMProcessConstants.caseIdentifier);
			
		   return identifier;
	}
	
	public ProcessDefinitionW getProcessDefinitionW () {
		
		Long pdId = getProcessInstance().getProcessDefinition().getId();
		return getProcessManager().getProcessDefinition(pdId);
	}
	
	public Integer getHandlerId() {
		
		CaseProcInstBind cpi = getCasesBPMDAO().find(CaseProcInstBind.class, getProcessInstanceId());
		
		Integer caseId = cpi.getCaseId();

		try {
			IWApplicationContext iwac = getIWAC();
			CasesBusiness casesBusiness = getCasesBusiness(iwac);
			GeneralCase genCase = casesBusiness.getGeneralCase(caseId);
			User currentHandler = genCase.getHandledBy();
			
			return currentHandler == null ? null : new Integer(currentHandler.getPrimaryKey().toString());
			
		} catch (RemoteException e) {
			throw new IBORuntimeException(e);
		} catch (FinderException e) {
			throw new IBORuntimeException(e);
		}
	}
	
	public List<User> getUsersConnectedToProcess() {
		
		final Collection<User> users;
		
		try {
			Long processInstanceId = getProcessInstanceId();
			BPMTypedPermission perm = (BPMTypedPermission)getPermissionsFactory().getRoleAccessPermission(processInstanceId, null, false);
			users = getBpmFactory().getRolesManager().getAllUsersForRoles(null, processInstanceId, perm);
			
		} catch(Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while resolving all process instance users", e);
			return null;
		}
		
		if(users != null && !users.isEmpty()) {

//			using separate list, as the resolved one could be cashed (shared) and so
			ArrayList<User> connectedPeople = new ArrayList<User>(users);

			for (Iterator<User> iterator = connectedPeople.iterator(); iterator.hasNext();) {
				
				User user = iterator.next();
				String hideInContacts = user.getMetaData(BPMUser.HIDE_IN_CONTACTS);
				
				if(hideInContacts != null)
//					excluding ones, that should be hidden in contacts list
					iterator.remove();
			}
			
			try {
				Collections.sort(connectedPeople, new UserComparator(CoreUtil.getIWContext().getCurrentLocale()));
			} catch(Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while sorting contacts list ("+connectedPeople+")", e);
			}
			
			return connectedPeople; 
		}
		
		return null;
	}
	
	public boolean hasHandlerAssignmentSupport() {
		
		@SuppressWarnings("unchecked")
		Map<String, Event> events = getProcessInstance().getProcessDefinition().getEvents();
		
		return events != null && events.containsKey(CaseHandlerAssignmentHandler.assignHandlerEventType);
	}
	
	public void setContactsPermission(Role role, Integer userId) {
		
		Long processInstanceId = getProcessInstanceId();
	
		getBpmFactory().getRolesManager().setContactsPermission(
				role, processInstanceId, userId
		);
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	public ProcessWatch getProcessWatcher() {
		return processWatcher;
	}
	
	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	public PermissionsFactory getPermissionsFactory() {
		return permissionsFactory;
	}

	public void setPermissionsFactory(PermissionsFactory permissionsFactory) {
		this.permissionsFactory = permissionsFactory;
	}
}