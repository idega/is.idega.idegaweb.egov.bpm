package is.idega.idegaweb.egov.bpm.cases.exe;


import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.FinderException;

import org.jbpm.JbpmContext;
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
import com.idega.jbpm.exe.ProcessWatch;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.ProcessWatchType;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.7 $
 *
 * Last modified: $Date: 2008/08/05 07:09:43 $ by $Author: civilis $
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

		CaseProcInstBind cpi = getCasesBPMDAO().find(CaseProcInstBind.class, getProcessInstanceId());
		
		Integer caseId = cpi.getCaseId();

		try {
			IWApplicationContext iwac = getIWAC();
			CasesBusiness casesBusiness = getCasesBusiness(iwac);
			GeneralCase genCase = casesBusiness.getGeneralCase(caseId);
			
//			TODO: either send message, or fire an event of this stuff to the process
			
			if(handlerUserId == null) {
				casesBusiness.untakeCase(genCase);
				getProcessWatcher().removeWatch(getProcessInstanceId(), caseId);
				
			} else {
			
				User currentHandler = genCase.getHandledBy();
				
				if(currentHandler == null || !String.valueOf(handlerUserId).equals(String.valueOf(currentHandler.getPrimaryKey()))) {
					
					UserBusiness userBusiness = getUserBusiness(iwac);
					
					IWContext iwc = IWContext.getCurrentInstance();
					
					User handler = userBusiness.getUser(handlerUserId);
					User performer = iwc.getCurrentUser();
					
					casesBusiness.takeCase(genCase, handler, iwc, performer, true, false);
					
					getProcessWatcher().assignWatch(getProcessInstanceId(), handlerUserId);
				}
			}
			
		} catch (RemoteException e) {
			throw new IBORuntimeException(e);
		} catch (FinderException e) {
			throw new IBORuntimeException(e);
		}
	}
	
//	private void fireAssignedEvent(long processInstanceId) {
//		
//	}
	
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

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	public ProcessWatch getProcessWatcher() {
		return processWatcher;
	}
}