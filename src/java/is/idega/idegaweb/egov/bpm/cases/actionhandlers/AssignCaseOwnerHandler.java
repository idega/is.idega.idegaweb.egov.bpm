package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.accesscontrol.business.LoginSession;
import com.idega.core.business.DefaultSpringBean;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.utils.JBPMConstants;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $ Last modified: $Date: 2009/05/28 14:12:01 $ by $Author: valdas $
 */
@Service("assignCaseOwnerHandler")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AssignCaseOwnerHandler extends DefaultSpringBean implements ActionHandler {

	private static final long serialVersionUID = 340054091051722366L;
	private Long processInstanceId;
	private Integer ownerUserId;

	@Autowired
	private CasesBPMDAO casesBPMDAO;

	@Autowired
	private BPMFactory bpmFactory;

	@Autowired
	private BPMContext bpmContext;

	@Override
	@Transactional(readOnly = false)
	public void execute(ExecutionContext ectx) throws Exception {
		if (processInstanceId == null) {
			getLogger().warning("Proc. inst. ID is unknown, trying to resolve it from execution context");
			try {
				ProcessInstance pi = ectx.getProcessInstance();
				Token superProcessToken = pi.getSuperProcessToken();
				if (superProcessToken == null) {
					processInstanceId = pi.getId();
				} else {
					processInstanceId = superProcessToken.getProcessInstance().getId();
				}
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Error getting proc. inst. ID from execution context!", e);
			}
			if (processInstanceId != null) {
				getLogger().info("Got proc. inst. (" + processInstanceId + ") ID from execution context");
			}
		}

		CaseProcInstBind cpi = null;
		try {
			cpi = getCasesBPMDAO().find(CaseProcInstBind.class, processInstanceId);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error loading case and proc. inst. bind by proc. inst. ID: " + processInstanceId, e);
		}
		if (cpi == null) {
			throw new RuntimeException("Unable to find case and proc. inst. bind by proc. inst. ID: " + processInstanceId);
		}

		Integer caseId = cpi.getCaseId();

		IWApplicationContext iwac = getIWAC();
		CasesBusiness casesBusiness = getCasesBusiness(iwac);
		GeneralCase genCase = casesBusiness.getGeneralCase(caseId);

		JBPMConstants.bpmLogger.fine("Setting new owner for a case (" + caseId + ") to be user id = " + getOwnerUserId());

		User ownerUser = null;
		if (getOwnerUserId() == null) {
			ownerUser = getLoggedInUser();
		} else {
			ownerUser = getUserBusiness(iwac).getUser(getOwnerUserId());
		}
		genCase.setOwner(ownerUser);
		genCase.store();

		final User taskOwner = ownerUser;
		getBpmContext().execute(new JbpmCallback<Void>() {
			@Override
			public Void doInJbpm(JbpmContext context) throws JbpmException {
				TaskInstance taskInstance = getBpmFactory()
				        .getProcessManagerByProcessInstanceId(getProcessInstanceId())
				        .getProcessInstance(getProcessInstanceId())
				        .getStartTaskInstance().getTaskInstance(context);

				if (taskOwner != null)
					taskInstance.setActorId(taskOwner.getId());

				getBpmContext().saveProcessEntity(context, taskInstance);
				return null;
			}
		});
	}

	protected User getLoggedInUser() {
		try {
			LoginSession loginSession = ELUtil.getInstance().getBean(LoginSession.class);
			return loginSession.isLoggedIn() ? loginSession.getUser() : null;
		} catch(Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error getting logged in user", e);
		}
		return null;
	}

	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public Integer getOwnerUserId() {
		return ownerUserId;
	}

	public void setOwnerUserId(Integer ownerUserId) {
		this.ownerUserId = ownerUserId;
	}

	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return IBOLookup.getServiceInstance(iwac,
			    CasesBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	protected UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return IBOLookup.getServiceInstance(iwac,
			    UserBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
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

	CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	public BPMContext getBpmContext() {
		return bpmContext;
	}

	public void setBpmContext(BPMContext bpmContext) {
		this.bpmContext = bpmContext;
	}
}