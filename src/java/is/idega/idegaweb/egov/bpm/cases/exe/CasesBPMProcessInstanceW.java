package is.idega.idegaweb.egov.bpm.cases.exe;


import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessConstants;
import is.idega.idegaweb.egov.bpm.cases.actionhandlers.CaseHandlerAssignmentHandler;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;

import java.rmi.RemoteException;
import java.util.Map;

import javax.annotation.Resource;
import javax.ejb.FinderException;

import org.jbpm.graph.def.Event;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.bpm.exe.DefaultBPMProcessInstanceW;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.ProcessWatch;
import com.idega.jbpm.exe.ProcessWatchType;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;

/**
 * TODO: we could create abstract class for some generic methods, like getPeopleConntectedToTheProcess
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.21 $
 *
 * Last modified: $Date: 2008/11/30 08:23:13 $ by $Author: civilis $
 */
@Scope("prototype")
@Service("casesPIW")
public class CasesBPMProcessInstanceW extends DefaultBPMProcessInstanceW {
	
	@Autowired
	private CasesBPMDAO casesBPMDAO;
	
	@Autowired
	@ProcessWatchType("cases")
	private ProcessWatch processWatcher;
	
	@Override
	@Required
	@Resource(name="casesBpmProcessManager")
	public void setProcessManager(ProcessManager processManager) {
		super.setProcessManager(processManager);
	}

	@Override
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
	
	private static final String caseDescriptionVariableName = "string_caseDescription";
	
	@Override
	public String getProcessDescription() {

		String description = (String) getProcessInstance().getContextInstance().getVariable(caseDescriptionVariableName);
		
		return description;
	}
	
	@Override
	public String getProcessIdentifier() {
	    
		return (String) getProcessInstance().getContextInstance().getVariable(CasesBPMProcessConstants.caseIdentifier);
	}
	
	@Override
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
	
	@Override
	public boolean hasHandlerAssignmentSupport() {
		
		@SuppressWarnings("unchecked")
		Map<String, Event> events = getProcessInstance().getProcessDefinition().getEvents();
		
		return events != null && events.containsKey(CaseHandlerAssignmentHandler.assignHandlerEventType);
	}
	
	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	public ProcessWatch getProcessWatcher() {
		return processWatcher;
	}
}