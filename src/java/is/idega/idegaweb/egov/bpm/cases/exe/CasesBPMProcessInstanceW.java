package is.idega.idegaweb.egov.bpm.cases.exe;

import is.idega.idegaweb.egov.bpm.cases.actionhandlers.CaseHandlerAssignmentHandler;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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
import org.springframework.beans.factory.config.BeanDefinition;
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
import com.idega.jbpm.bean.VariableInstanceInfo;
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
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
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
		getBpmContext().execute(new JbpmCallback<Void>() {

			@Override
			public Void doInJbpm(JbpmContext context) throws JbpmException {
				ProcessInstance pi = getProcessInstance(context);

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

				VariableCreatedEvent performerVarCreated = new VariableCreatedEvent(this, pi.getProcessDefinition().getName(), pi.getId(), createdVars);
				ELUtil.getInstance().publishEvent(performerVarCreated);

				pi.getProcessDefinition().fireEvent(handlerUserId != null ?
						CaseHandlerAssignmentHandler.assignHandlerEventType	: CaseHandlerAssignmentHandler.unassignHandlerEventType, ectx);
				return null;
			}
		});
	}

	private IWApplicationContext getIWAC() {
		final IWContext iwc = IWContext.getCurrentInstance();
		final IWApplicationContext iwac;
		if (iwc != null) {
			iwac = iwc;
		} else {
			iwac = IWMainApplication.getDefaultIWApplicationContext();
		}

		return iwac;
	}

	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	private <T extends Serializable> T getVariableValue(String variableName) {
		Collection<VariableInstanceInfo> vars = getVariableInstanceQuerier()
				.getVariableByProcessInstanceIdAndVariableName(getProcessInstanceId(), variableName);
		if (ListUtil.isEmpty(vars))
			return null;

		T value = null;
		for (Iterator<VariableInstanceInfo> varsIter = vars.iterator(); (varsIter.hasNext() && value == null);) {
			value =  varsIter.next().getValue();
		}
		return value;
	}

	@Override
	@Transactional(readOnly = true)
	public String getProcessDescription() {
		String description = getVariableValue(ProcessConstants.CASE_DESCRIPTION);
		return description;
	}

	@Override
	@Transactional(readOnly = true)
	public String getProcessIdentifier() {
		String identifier = getVariableValue(ProcessConstants.CASE_IDENTIFIER);
		return identifier;
	}

	@Override
	@Transactional(readOnly = true)
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
	@Transactional(readOnly = true)
	public boolean hasHandlerAssignmentSupport() {
		Boolean res = getBpmContext().execute(new JbpmCallback<Boolean>() {

			@Override
			public Boolean doInJbpm(JbpmContext context) throws JbpmException {
				@SuppressWarnings("unchecked")
				Map<String, Event> events = getProcessInstance(context).getProcessDefinition().getEvents();

				return events != null && events.containsKey(CaseHandlerAssignmentHandler.assignHandlerEventType);
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