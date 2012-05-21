package is.idega.idegaweb.egov.bpm.cases;

import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.process.business.CaseManagersProvider;
import com.idega.block.process.business.CasesRetrievalManager;
import com.idega.block.process.business.ProcessConstants;
import com.idega.block.process.data.Case;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.identity.BPMAccessControlException;
import com.idega.jbpm.identity.BPMUser;
import com.idega.jbpm.identity.RolesManager;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.IWTimestamp;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.24 $ Last modified: $Date: 2009/03/17 20:53:23 $ by $Author: civilis $
 */
@Scope("singleton")
@Service(CasesBPMProcessView.BEAN_IDENTIFIER)
public class CasesBPMProcessView {

	public static final String BEAN_IDENTIFIER = "casesBPMProcessView";

	private BPMContext idegaJbpmContext;
	private BPMFactory BPMFactory;
	private CasesBPMDAO casesBPMDAO;
	private CaseManagersProvider caseManagersProvider;

	@Transactional(readOnly = true)
	public CasesBPMTaskViewBean getTaskView(final long taskInstanceId) {

		return getIdegaJbpmContext().execute(new JbpmCallback() {

			@Override
			public Object doInJbpm(JbpmContext context) throws JbpmException {

				// ProcessInstanceW processInstanceW = getBPMFactory()
				// .getProcessManagerByProcessInstanceId(processInstanceId)
				// .getProcessInstance(processInstanceId);
				//
				// Collection<TaskInstanceW> taskInstances = processInstanceW
				// .getAllTaskInstances();
				//
				// TaskInstanceW ti = null;
				// for (TaskInstanceW task : taskInstances) {
				// if (task.getTaskInstanceId() == taskInstanceId) {
				// ti = task;
				// }
				// }

				// TODO: changed the way tiw is resolved, test if it works!

				TaskInstanceW tiw = getBPMFactory()
				        .getProcessManagerByTaskInstanceId(taskInstanceId)
				        .getTaskInstance(taskInstanceId);

				if (tiw == null) {
					Logger.getLogger(getClass().getName()).log(
					    Level.WARNING,
					    "No task instance found for task instance id provided: "
					            + taskInstanceId);
					return new CasesBPMTaskViewBean();
				}

				TaskInstance ti = tiw.getTaskInstance();

				IWContext iwc = IWContext.getInstance();

				IWTimestamp createTime = new IWTimestamp(ti.getCreate());
				String taskStatus = getTaskStatus(ti);
				String assignedTo = getTaskAssignedTo(ti);

				CasesBPMTaskViewBean bean = new CasesBPMTaskViewBean();
				bean.setTaskName(tiw.getName(iwc.getCurrentLocale()));
				bean.setTaskStatus(taskStatus);
				bean.setAssignedTo(assignedTo);
				bean.setCreatedDate(createTime.getLocaleDateAndTime(iwc
				        .getLocale(), IWTimestamp.SHORT, IWTimestamp.SHORT));
				return bean;
			}
		});
	}

	protected String getTaskStatus(TaskInstance taskInstance) {

		if (taskInstance.hasEnded())
			return "Ended";
		if (taskInstance.getStart() != null)
			return "In progress";

		return "Not started";
	}

	@Transactional(readOnly = true)
	protected String getTaskAssignedTo(TaskInstance taskInstance) {

		String actorId = taskInstance.getActorId();

		if (actorId != null) {

			try {
				int assignedTo = Integer.parseInt(actorId);
				return getUserBusiness().getUser(assignedTo).getName();

			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(
				    Level.SEVERE,
				    "Exception while resolving assigned user name for actor id: "
				            + actorId, e);
			}
		}

		return "No one";
	}

	@Transactional(readOnly = false)
	public void startTask(long taskInstanceId, int actorUserId) {

		ProcessManager processManager = getBPMFactory()
		        .getProcessManagerByTaskInstanceId(taskInstanceId);
		processManager.getTaskInstance(taskInstanceId).start(actorUserId);
	}

	@Transactional(readOnly = false)
	public void assignTask(long taskInstanceId, int actorUserId) {

		ProcessManager processManager = getBPMFactory()
		        .getProcessManagerByTaskInstanceId(taskInstanceId);
		processManager.getTaskInstance(taskInstanceId).assign(actorUserId);
	}

	/**
	 * @param taskInstanceId
	 * @param userId
	 * @return null if task can be started, err message otherwise
	 */
	@Transactional(readOnly = true)
	public String getCanStartTask(long taskInstanceId, int userId) {

		try {
			RolesManager rolesManager = getBPMFactory().getRolesManager();
			rolesManager.hasRightsToStartTask(taskInstanceId, userId);

		} catch (BPMAccessControlException e) {
			return e.getUserFriendlyMessage();
		}

		return null;
	}

	@Transactional(readOnly = true)
	public String getCanTakeTask(long taskInstanceId, int userId) {

		try {
			RolesManager rolesManager = getBPMFactory().getRolesManager();
			rolesManager.hasRightsToAssignTask(taskInstanceId, userId);

		} catch (BPMAccessControlException e) {
			return e.getUserFriendlyMessage();
		}

		return null;
	}

	@Transactional(readOnly = true)
	public CasesBPMProcessViewBean getProcessView(final long processInstanceId, final int caseId) {

		return getIdegaJbpmContext().execute(new JbpmCallback() {

			@Override
			public Object doInJbpm(JbpmContext context) throws JbpmException {
				ProcessInstance pi = context.getProcessInstance(processInstanceId);

				if (pi == null) {
					Logger.getLogger(getClass().getName()).log(Level.WARNING, "No process instance found for process instance id provided: "
							+ processInstanceId);
					return new CasesBPMProcessViewBean();
				}

				ContextInstance ci = pi.getContextInstance();

				String ownerFirstName = (String) ci.getVariable(CasesBPMProcessConstants.caseOwnerFirstNameVariableName);
				String ownerLastName = (String) ci.getVariable(CasesBPMProcessConstants.caseOwnerLastNameVariableName);
				String ownerName = new StringBuffer(ownerFirstName == null ? CoreConstants.EMPTY : ownerFirstName).append(
				            ownerFirstName != null && ownerLastName != null ? CoreConstants.SPACE : CoreConstants.EMPTY).append(
				            ownerLastName == null ? CoreConstants.EMPTY : ownerLastName).toString();

				IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());

				String processStatus = pi.hasEnded() ? "Ended" : "In progress";
				IWTimestamp time = new IWTimestamp(pi.getStart());
				String createDate = time.getLocaleDate(iwc.getLocale());

				String caseIdentifier = (String) ci.getVariable(ProcessConstants.CASE_IDENTIFIER);

				String caseCategory;
				String caseType;

				try {
					GeneralCase genCase = getCaseBusiness(iwc).getGeneralCase(Integer.valueOf(caseId));
					caseCategory = genCase.getCaseCategory().getLocalizedCategoryName(iwc.getLocale());
					caseType = genCase.getCaseType().getName();
				} catch (Exception e) {
					caseCategory = null;
					caseType = null;
				}

				CasesBPMProcessViewBean bean = new CasesBPMProcessViewBean();
				bean.setProcessName(pi.getProcessDefinition().getName());
				bean.setProcessStatus(processStatus);
				bean.setEnded(pi.hasEnded());
				bean.setProcessOwner(ownerName);
				bean.setProcessCreateDate(createDate);
				bean.setCaseCategory(caseCategory);
				bean.setCaseType(caseType);
				bean.setCaseIdentifier(caseIdentifier);

				return bean;
			}
		});
	}

	public BPMUser getCurrentBPMUser() {

		return getBPMFactory().getBpmUserFactory().getCurrentBPMUser();
	}

	@Transactional(readOnly = true)
	public Long getProcessInstanceId(Object casePK) {

		if (casePK != null) {

			Integer caseId;

			if (casePK instanceof Integer)
				caseId = (Integer) casePK;
			else
				caseId = new Integer(String.valueOf(casePK));

			CaseProcInstBind cpi = getCPIBind(caseId, null);

			if (cpi != null)
				return cpi.getProcInstId();
		}

		return null;
	}

	@Transactional(readOnly = true)
	public Integer getCaseId(Long processInstanceId) {

		CaseProcInstBind cpi = getCPIBind(null, processInstanceId);

		if (cpi != null)
			return cpi.getCaseId();

		return null;
	}

	/**
	 * either of parameters should be not null
	 *
	 * @param processInstanceId
	 * @param caseId
	 * @return
	 */
	@Transactional(readOnly = true)
	public UIComponent getCaseManagerView(IWContext iwc,
	        Long processInstanceId, Integer caseId, String caseProcessorType) {

		if (iwc == null)
			iwc = IWContext.getInstance();

		if (processInstanceId == null && caseId == null)
			throw new IllegalArgumentException(
			        "Neither processInstanceId, nor caseId provided");

		caseId = caseId != null ? caseId : getCaseId(processInstanceId);

		try {
			Case theCase = getCasesBusiness(iwc).getCase(caseId);

			CasesRetrievalManager caseManager;

			if (theCase.getCaseManagerType() != null)
				caseManager = getCaseManagersProvider().getCaseManager();
			else
				caseManager = null;

			if (caseManager != null) {

				UIComponent caseAssets = caseManager.getView(iwc, caseId,
				    caseProcessorType, theCase.getCaseManagerType());

				if (caseAssets != null)
					return caseAssets;
				else
					Logger.getLogger(getClass().getName()).log(
					    Level.WARNING,
					    "No case assets component resolved from case manager: "
					            + caseManager.getType() + " by case pk: "
					            + theCase.getPrimaryKey().toString());
			} else
				Logger.getLogger(getClass().getName()).log(
				    Level.WARNING,
				    "No case manager resolved by type="
				            + theCase.getCaseManagerType());

		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE,
			    "Exception while resolving case manager view", e);
		}

		return null;
	}

	@Transactional(readOnly = true)
	protected CaseProcInstBind getCPIBind(Integer caseId, Long processInstanceId) {

		if (caseId != null) {

			CaseProcInstBind bind = getCasesBPMDAO()
			        .getCaseProcInstBindByCaseId(caseId);

			if (bind != null) {

				return bind;

			} else {
				Logger.getLogger(getClass().getName()).log(
				    Level.SEVERE,
				    "No case process instance bind found for caseId provided: "
				            + caseId);
			}

		} else if (processInstanceId != null) {

			CaseProcInstBind bind;
			try {
				bind = getCasesBPMDAO().find(CaseProcInstBind.class,
				    processInstanceId);

			} catch (Exception e) {
				bind = null;
			}

			if (bind != null) {

				return bind;

			} else {
				Logger.getLogger(getClass().getName()).log(
				    Level.SEVERE,
				    "No case process instance bind found for process instanceid provided: "
				            + processInstanceId);
			}
		}

		return null;
	}

	public class CasesBPMProcessViewBean implements Serializable {

		private static final long serialVersionUID = -1209671586005809408L;

		private Boolean ended;
		private String processName;
		private String processStatus;
		private String processOwner;
		private String processCreateDate;
		private String caseCategory;
		private String caseType;
		private String caseIdentifier;

		public String getProcessOwner() {
			return processOwner;
		}

		public void setProcessOwner(String processOwner) {
			this.processOwner = processOwner;
		}

		public String getProcessCreateDate() {
			return processCreateDate;
		}

		public void setProcessCreateDate(String processCreateDate) {
			this.processCreateDate = processCreateDate;
		}

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

		public String getCaseCategory() {
			return caseCategory;
		}

		public void setCaseCategory(String caseCategory) {
			this.caseCategory = caseCategory;
		}

		public String getCaseType() {
			return caseType;
		}

		public void setCaseType(String caseType) {
			this.caseType = caseType;
		}

		public String getCaseIdentifier() {
			return caseIdentifier;
		}

		public void setCaseIdentifier(String caseIdentifier) {
			this.caseIdentifier = caseIdentifier;
		}

		public Boolean getEnded() {
			return ended == null ? false : ended;
		}

		public void setEnded(Boolean ended) {
			this.ended = ended;
		}
	}

	public class CasesBPMTaskViewBean implements Serializable {

		private static final long serialVersionUID = -6402627297789228878L;

		private String taskName;
		private String taskStatus;
		private String assignedTo;
		private String createdDate;

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

		public String getCreatedDate() {
			return createdDate;
		}

		public void setCreatedDate(String createdDate) {
			this.createdDate = createdDate;
		}
	}

	public BPMContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
	public void setIdegaJbpmContext(BPMContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	protected CasesBusiness getCaseBusiness(IWContext iwc) {

		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwc,
			    CasesBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	protected UserBusiness getUserBusiness() {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(CoreUtil
			        .getIWContext(), UserBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	public BPMFactory getBPMFactory() {
		return BPMFactory;
	}

	@Autowired
	public void setBPMFactory(BPMFactory factory) {
		BPMFactory = factory;
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	@Autowired
	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}

	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac,
			    CasesBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	public CaseManagersProvider getCaseManagersProvider() {
		return caseManagersProvider;
	}

	@Autowired
	public void setCaseManagersProvider(
	        CaseManagersProvider caseManagersProvider) {
		this.caseManagersProvider = caseManagersProvider;
	}
}