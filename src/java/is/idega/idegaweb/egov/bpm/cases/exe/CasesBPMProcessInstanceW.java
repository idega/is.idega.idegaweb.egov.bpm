package is.idega.idegaweb.egov.bpm.cases.exe;

import is.idega.idegaweb.egov.bpm.cases.actionhandlers.CaseHandlerAssignmentHandler;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.ejb.FinderException;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.process.business.ProcessConstants;
import com.idega.bpm.exe.DefaultBPMProcessInstanceW;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.events.VariableCreatedEvent;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.ProcessWatch;
import com.idega.jbpm.exe.ProcessWatchType;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.ListUtil;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.23 $
 *
 *          Last modified: $Date: 2009/02/06 19:00:40 $ by $Author: civilis $
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
	@Resource(name = "casesBpmProcessManager")
	public void setProcessManager(ProcessManager processManager) {
		super.setProcessManager(processManager);
	}

	@Override
	@Transactional(readOnly = false)
	public void assignHandler(final Integer handlerUserId) {

		getBpmContext().execute(new JbpmCallback() {

			@Override
			public Object doInJbpm(JbpmContext context) throws JbpmException {
				ProcessInstance pi = getProcessInstance();

				// creating new token, so there are no race conditions for
				// variables
				Token tkn = new Token(pi.getRootToken(), "assignUnassignHandler_" + System.currentTimeMillis());

				ExecutionContext ectx = new ExecutionContext(tkn);

				IWContext iwc = IWContext.getCurrentInstance();
				Integer performerId = iwc.getCurrentUserId();

				ectx.setVariable(CaseHandlerAssignmentHandler.performerUserIdVarName, performerId);
				Map<String, Object> createdVars = new HashMap<String, Object>();
				createdVars.put(CaseHandlerAssignmentHandler.performerUserIdVarName, performerId);

				if (handlerUserId != null) {
					ectx.setVariable(CaseHandlerAssignmentHandler.handlerUserIdVarName,	handlerUserId);
					createdVars.put(CaseHandlerAssignmentHandler.handlerUserIdVarName, handlerUserId);
				}

				VariableCreatedEvent performerVarCreated = new VariableCreatedEvent(
						this,
						pi.getProcessDefinition().getName(),
						pi.getId(),
						null,
						createdVars
				);
				ELUtil.getInstance().publishEvent(performerVarCreated);

				String event = null;
				if (handlerUserId != null) {
					event = CaseHandlerAssignmentHandler.assignHandlerEventType;
				} else {
					event = CaseHandlerAssignmentHandler.unassignHandlerEventType;
				}

				// Firing event to process...
				pi.getProcessDefinition().fireEvent(event, ectx);

				// And to it's subprocesses...
				List<ProcessInstance> subProcesses = getBpmDAO().getSubprocessInstancesOneLevel(pi.getId());
				for (ProcessInstance subProcessInstance: subProcesses) {
					subProcessInstance.getProcessDefinition().fireEvent(event, ectx);
				}

				return null;
			}
		});
	}

	private IWApplicationContext getIWAC() {

		final IWContext iwc = IWContext.getCurrentInstance();
		final IWApplicationContext iwac;
		// trying to get iwma from iwc, if available, downgrading to default
		// iwma, if not

		if (iwc != null) {

			iwac = iwc;

		} else {
			iwac = IWMainApplication.getDefaultIWApplicationContext();
		}

		return iwac;
	}

	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return IBOLookup.getServiceInstance(iwac,
					CasesBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public String getProcessDescription() {

		String description = (String) getProcessInstance().getContextInstance()
				.getVariable(ProcessConstants.CASE_DESCRIPTION);

		return description;
	}

	@Override
	@Transactional(readOnly = true)
	public String getProcessIdentifier() {

		return (String) getProcessInstance().getContextInstance().getVariable(
				ProcessConstants.CASE_IDENTIFIER);
	}

	@Override
	@Transactional(readOnly = true)
	public Integer getHandlerId() {

		CaseProcInstBind cpi = getCasesBPMDAO().find(CaseProcInstBind.class,
				getProcessInstanceId());

		Integer caseId = cpi.getCaseId();

		try {
			IWApplicationContext iwac = getIWAC();
			CasesBusiness casesBusiness = getCasesBusiness(iwac);
			GeneralCase genCase = casesBusiness.getGeneralCase(caseId);
			User currentHandler = genCase.getHandledBy();

			return currentHandler == null ? null : new Integer(currentHandler
					.getPrimaryKey().toString());

		} catch (RemoteException e) {
			throw new IBORuntimeException(e);
		} catch (FinderException e) {
			throw new IBORuntimeException(e);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public boolean hasHandlerAssignmentSupport() {

		Boolean res = getBpmContext().execute(new JbpmCallback() {

			@Override
			@SuppressWarnings("unchecked")
			public Object doInJbpm(JbpmContext context) throws JbpmException {
				Map<String, Event> events = getProcessInstance()
						.getProcessDefinition().getEvents();

				boolean result = events != null
						&& events.containsKey(CaseHandlerAssignmentHandler.assignHandlerEventType);
				if (result == Boolean.FALSE) {
					 List<ProcessInstance> subprocesses = getBpmDAO().getSubprocessInstancesOneLevel(getProcessInstance().getId());
					 if (ListUtil.isEmpty(subprocesses)) {
						 return Boolean.FALSE;
					 }

					 for (ProcessInstance pi : subprocesses) {
						 events = pi.getProcessDefinition().getEvents();
						 if (events != null && events.containsKey(CaseHandlerAssignmentHandler.assignHandlerEventType)) {
							 return Boolean.TRUE;
						 }
					 }
				}

				return result;
			}
		});

		return res;
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	@Override
	public ProcessWatch getProcessWatcher() {
		return processWatcher;
	}
}