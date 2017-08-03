package is.idega.idegaweb.egov.bpm.cases.manager;

import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.FinderException;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.servlet.http.HttpSession;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.db.GraphSession;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.process.business.CaseBusiness;
import com.idega.block.process.business.CasesCacheCriteria;
import com.idega.block.process.business.CasesRetrievalManager;
import com.idega.block.process.business.CasesRetrievalManagerImpl;
import com.idega.block.process.business.ProcessConstants;
import com.idega.block.process.data.Case;
import com.idega.block.process.data.CaseCode;
import com.idega.block.process.event.CaseDeletedEvent;
import com.idega.block.process.event.CaseModifiedEvent;
import com.idega.block.process.presentation.beans.CaseManagerState;
import com.idega.block.process.presentation.beans.CasePresentation;
import com.idega.block.process.presentation.beans.CasePresentationComparator;
import com.idega.block.process.presentation.beans.CasesSearchCriteriaBean;
import com.idega.bpm.BPMConstants;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.builder.data.ICPage;
import com.idega.core.builder.data.ICPageHome;
import com.idega.data.IDOLookup;
import com.idega.data.IDOLookupException;
import com.idega.data.SimpleQuerier;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.CaseTypesProcDefBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.data.ProcessManagerBind;
import com.idega.jbpm.event.ProcessInstanceCreatedEvent;
import com.idega.jbpm.event.TaskInstanceSubmitted;
import com.idega.jbpm.exe.BPMDocument;
import com.idega.jbpm.exe.BPMEmailDocument;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.presentation.IWContext;
import com.idega.presentation.paging.PagedDataCollection;
import com.idega.presentation.text.Link;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.expression.ELUtil;
import com.idega.webface.WFUtil;

import is.idega.idegaweb.egov.application.business.ApplicationBusiness;
import is.idega.idegaweb.egov.application.data.Application;
import is.idega.idegaweb.egov.application.data.ApplicationHome;
import is.idega.idegaweb.egov.bpm.business.CasesSubcriberManager;
import is.idega.idegaweb.egov.bpm.cases.bundle.ProcessBundleCasesImpl;
import is.idega.idegaweb.egov.bpm.cases.presentation.AbstractCasesBPMAssets;
import is.idega.idegaweb.egov.bpm.cases.presentation.UICasesBPMAssets;
import is.idega.idegaweb.egov.bpm.cases.presentation.beans.BPMCasesEngine;
import is.idega.idegaweb.egov.bpm.cases.presentation.beans.CasesBPMAssetsState;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.CaseCategory;
import is.idega.idegaweb.egov.cases.data.GeneralCase;
import is.idega.idegaweb.egov.cases.util.CasesConstants;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.13 $
 *
 * Last modified: $Date: 2009/07/14 16:25:04 $ by $Author: valdas $
 */
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service(BPMCasesRetrievalManagerImpl.beanIdentifier)
@Transactional(readOnly = true)
public class BPMCasesRetrievalManagerImpl	extends CasesRetrievalManagerImpl
											implements CasesRetrievalManager, BPMCasesRetrievalManager, ApplicationListener<ApplicationEvent> {

	private static final Logger LOGGER = Logger.getLogger(BPMCasesRetrievalManagerImpl.class.getName());

	public static final String PARAMETER_PROCESS_INSTANCE_PK = "pr_inst_pk";

	@Autowired
	private CasesBPMDAO casesBPMDAO;

	@Autowired
	private BPMContext bpmContext;

	@Autowired
	private BPMFactory bpmFactory;

	@Autowired
	private VariablesHandler variablesHandler;

	@Autowired
	private BPMCasesEngine casesEngine;

	private ApplicationBusiness applicationBusiness;

	public static final String	beanIdentifier = "casesBPMCaseHandler",
								caseHandlerType = ProcessConstants.BPM_CASE;

	@Override
	public String getBeanIdentifier() {
		return beanIdentifier;
	}

	@Override
	public String getType() {
		return caseHandlerType;
	}

	@Override
	public String getProcessIdentifier(Case theCase) {
		final Long piId = getProcessInstanceId(theCase);
		if (piId == null) {
			return null;
		}

		return getBpmContext().execute(new JbpmCallback<String>() {
			@Override
			public String doInJbpm(JbpmContext context) throws JbpmException {
				return (String) context.getProcessInstance(piId).getContextInstance().getVariable(ProcessConstants.CASE_IDENTIFIER);
			}
		});
	}

	@Override
	public Long getProcessInstanceId(Case theCase) {
		Integer caseId = theCase.getPrimaryKey() instanceof Integer ?
				(Integer) theCase.getPrimaryKey() :
				Integer.valueOf((theCase.getPrimaryKey().toString()));

		CaseProcInstBind cpi = getCasesBPMDAO().getCaseProcInstBindByCaseId(caseId);

		return cpi == null ? null : cpi.getProcInstId();
	}

	@Override
	public Long getProcessInstanceIdByCaseId(Object id) {
		Integer caseId = id instanceof Integer ? (Integer) id : Integer.valueOf(id.toString());

		CaseProcInstBind cpi = getCasesBPMDAO().getCaseProcInstBindByCaseId(caseId);

		return cpi == null ? null : cpi.getProcInstId();
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getConnectedUsers(java.util.Collection)
	 */
	@Override
	public List<User> getConnectedUsers(Collection<ProcessInstanceW> processInstances) {
		if (ListUtil.isEmpty(processInstances)) {
			return Collections.emptyList();
		}

		List<User> allUsers = new ArrayList<User>();
		for (ProcessInstanceW instance: processInstances) {
			List<User> users = instance.getUsersConnectedToProcess();
			if (ListUtil.isEmpty(users)) {
				continue;
			}

			allUsers.addAll(users);
		}

		return allUsers;
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getConnectedUsers(com.idega.block.process.data.Case)
	 */
	@Override
	public List<User> getConnectedUsers(Case theCase) {
		if (theCase == null) {
			return Collections.emptyList();
		}

		ProcessInstanceW processInstanceW = getProcessInstancesW(theCase);
		if (processInstanceW == null) {
			return Collections.emptyList();
		}

		return getConnectedUsers(Arrays.asList(processInstanceW));
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getBPMEmailDocuments(java.util.Collection, com.idega.user.data.User)
	 */
	@Override
	public List<BPMEmailDocument> getBPMEmailDocuments(
			Collection<ProcessInstanceW> processInstances,
			User owner) {
		if (ListUtil.isEmpty(processInstances) || owner == null) {
			return Collections.emptyList();
		}

		ArrayList<BPMEmailDocument> emails = new ArrayList<BPMEmailDocument>();
		for (ProcessInstanceW instance: processInstances) {
			List<BPMEmailDocument> entities = instance.getAttachedEmails(owner);
			if (ListUtil.isEmpty(entities)) {
				continue;
			}

			emails.addAll(entities);
		}

		return emails;
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getTaskBPMDocuments(java.util.Collection, com.idega.user.data.User, java.util.Locale)
	 */
	@Override
	public List<BPMDocument> getTaskBPMDocuments(
			Collection<ProcessInstanceW> processInstances,
			User owner, Locale locale) {
		if (ListUtil.isEmpty(processInstances) || owner == null) {
			return Collections.emptyList();
		}

		if (locale == null) {
			locale = getCurrentLocale();
		}

		ArrayList<BPMDocument> taskDocuments = new ArrayList<BPMDocument>();
		for (ProcessInstanceW instance: processInstances) {
			List<BPMDocument> entities = instance.getTaskDocumentsForUser(owner, locale);
			if (ListUtil.isEmpty(entities)) {
				continue;
			}

			taskDocuments.addAll(entities);
		}

		return taskDocuments;
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getSubmittedBPMDocuments(java.util.Collection, com.idega.user.data.User, java.util.Locale)
	 */
	@Override
	public List<BPMDocument> getSubmittedBPMDocuments(
			Collection<ProcessInstanceW> processInstances,
			User owner, Locale locale) {
		if (ListUtil.isEmpty(processInstances) || owner == null) {
			return Collections.emptyList();
		}

		if (locale == null) {
			locale = getCurrentLocale();
		}

		ArrayList<BPMDocument> submittedDocuments = new ArrayList<BPMDocument>();
		for (ProcessInstanceW instance: processInstances) {
			List<BPMDocument> entities = instance.getSubmittedDocumentsForUser(owner, locale);
			if (ListUtil.isEmpty(entities)) {
				continue;
			}

			submittedDocuments.addAll(entities);
		}

		return submittedDocuments;
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getProcessInstancesW(java.lang.Object)
	 */
	@Override
	public List<ProcessInstanceW> getProcessInstancesW(Object applicationPrimaryKey) {
		Application application = getApplication(applicationPrimaryKey);
		if (application == null) {
			return Collections.emptyList();
		}

		return getProcessInstancesW(application.getUrl());
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getProcessInstancesW(is.idega.idegaweb.egov.application.data.Application)
	 */
	@Override
	public List<ProcessInstanceW> getProcessInstancesW(Application application) {
		if (application == null) {
			return Collections.emptyList();
		}

		return getProcessInstancesW(application.getUrl());
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getProcessInstancesW(java.lang.String)
	 */
	@Override
	public List<ProcessInstanceW> getProcessInstancesW(String processDefinitionName) {
		if (StringUtil.isEmpty(processDefinitionName)) {
			return Collections.emptyList();
		}

		ProcessManager processManager = getBpmFactory().getProcessManager(
				processDefinitionName);
		if (processManager == null) {
			return Collections.emptyList();
		}

		List<Long> porcessInstancesIds = getProcessInstancesIDs(processDefinitionName);
		if (ListUtil.isEmpty(porcessInstancesIds)) {
			return null;
		}

		ArrayList<ProcessInstanceW> instancesW = new ArrayList<ProcessInstanceW>();
		for (Long porcessInstanceId: porcessInstancesIds) {
			instancesW.add(processManager.getProcessInstance(porcessInstanceId));
		}

		return instancesW;
	}

	@Override
	public boolean hasManagerAccess(Collection<String> caseIdentifiers) {
		String[] roles = getManagerRoleNames(caseIdentifiers);
		if (ArrayUtil.isEmpty(roles)) {
			return Boolean.FALSE;
		}

		AccessController accessController = getApplication().getAccessController();
		if (accessController == null) {
			return Boolean.FALSE;
		}

		if (accessController.hasRole(getCurrentUser(), Arrays.asList(roles))) {
			return Boolean.TRUE;
		}

		return Boolean.FALSE;
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getManagerRoleName(java.lang.String)
	 */
	@Override
	public String[] getManagerRoleNames(Collection<String> caseIdentifiers) {
		if (ListUtil.isEmpty(caseIdentifiers)) {
			return null;
		}

		StringBuilder sb = new StringBuilder("SELECT jvi.stringvalue_ ");
		sb.append("FROM jbpm_variableinstance jvi, bpm_cases_processinstances bcpi ");
		sb.append("WHERE jvi.PROCESSINSTANCE_ = bcpi.process_instance_id ");
		sb.append("AND jvi.NAME_='" + BPMConstants.VAR_MANAGER_ROLE + "' ");
		sb.append("AND bcpi.case_identifier IN (");

		for (Iterator<String> iterator = caseIdentifiers.iterator(); iterator.hasNext();) {
			sb.append(CoreConstants.QOUTE_SINGLE_MARK)
			.append(iterator.next())
			.append(CoreConstants.QOUTE_SINGLE_MARK);

			if (iterator.hasNext()) {
				sb.append(CoreConstants.COMMA).append(CoreConstants.SPACE);
			}
		}

		sb.append(")");

		String[] managerRoleNames = null;
		try {
			managerRoleNames = SimpleQuerier.executeStringQuery(sb.toString());
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Unable to get manager role names: ", e);
		}

		if (ArrayUtil.isEmpty(managerRoleNames)) {
			return null;
		}
		return managerRoleNames;
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getProcessInstancesW(com.idega.block.process.data.Case)
	 */
	@Override
	public ProcessInstanceW getProcessInstancesW(Case theCase) {
		Long processInstanceID = getProcessInstanceId(theCase);
		if (processInstanceID == null) {
			return null;
		}

		ProcessManager processManager = getBpmFactory()
				.getProcessManagerByProcessInstanceId(processInstanceID);
		if (processManager == null) {
			return null;
		}

		return processManager.getProcessInstance(processInstanceID);
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getProcessInstances(java.lang.String)
	 */
	@Override
	public List<ProcessInstance> getProcessInstances(String processDefinitionName) {
		return getBpmFactory().getBPMDAO()
				.getProcessInstances(Arrays.asList(processDefinitionName));
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getProcessInstancesIDs(java.lang.String)
	 */
	@Override
	public List<Long> getProcessInstancesIDs(String processDefinitionName) {
		return getBpmFactory().getBPMDAO()
				.getProcessInstanceIdsByProcessDefinitionNames(
						Arrays.asList(processDefinitionName));
	}

	@Override
	public Long getProcessDefinitionId(final Case theCase) {
		return getBpmContext().execute(new JbpmCallback<Long>() {
			@Override
			public Long doInJbpm(JbpmContext context) throws JbpmException {
				ProcessDefinition processDefinition = getProcessDefinition(context, theCase);
				return processDefinition == null ? null : processDefinition.getId();
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getProcessDefinitions(java.lang.String)
	 */
	@Override
	public List<ProcessDefinition> getProcessDefinitions(
			Collection<String> processDefinitionNames) {
		return getBpmFactory().getBPMDAO().getProcessDefinitions(processDefinitionNames);
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getProcessDefinitionsIDs(java.lang.String)
	 */
	@Override
	public List<Long> getProcessDefinitionsIDs(String processDefinitionName) {
		return getBpmFactory().getBPMDAO().getProcessDefinitionIdsByName(processDefinitionName);
	}

	/**
	 *
	 * @param applications to get {@link Application#getUrl()} from,
	 * not <code>null</code>;
	 * @return {@link Collection} of {@link Application#getUrl()} or
	 * {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	protected List<String> getApplicationUrls(Collection<Application> applications) {
		ArrayList<String> urls = new ArrayList<String>();
		if (!ListUtil.isEmpty(applications)) {
			for (Application application :applications) {
				urls.add(application.getUrl());
			}
		}

		return urls;
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getProcessDefinitions(is.idega.idegaweb.egov.application.data.Application)
	 */
	@Override
	public List<ProcessDefinition> getProcessDefinitionsByApplications(
			Collection<Application> applications) {
		return getProcessDefinitions(getApplicationUrls(applications));
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getProcessDefinitionsIDs(is.idega.idegaweb.egov.application.data.Application)
	 */
	@Override
	public List<Long> getProcessDefinitionsIDs(Application application) {
		if (application == null) {
			return Collections.emptyList();
		}

		return getProcessDefinitionsIDs(application.getUrl());
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getProcessDefinitions(java.lang.Object)
	 */
	@Override
	public List<ProcessDefinition> getProcessDefinitions(java.lang.Number applicationPrimaryKey) {
		Application application = getApplication(applicationPrimaryKey);
		if (application == null) {
			return Collections.emptyList();
		}

		return getProcessDefinitions(Arrays.asList(application.getUrl()));
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getProcessDefinitionsIDs(java.lang.Object)
	 */
	@Override
	public List<Long> getProcessDefinitionsIDs(Object applicationPrimaryKey) {
		Application application = getApplication(applicationPrimaryKey);
		if (application == null) {
			return Collections.emptyList();
		}

		return getProcessDefinitionsIDs(application.getUrl());
	}

	@Override
	public String getProcessDefinitionName(final Case theCase) {
		return getBpmContext().execute(new JbpmCallback<String>() {
			@Override
			public String doInJbpm(JbpmContext context) throws JbpmException {
				ProcessDefinition processDefinition = getProcessDefinition(context, theCase);
				return processDefinition == null ? null : processDefinition.getName();
			}
		});
	}

	@Transactional(readOnly = true)
	private ProcessDefinition getProcessDefinition(JbpmContext context, Case theCase) {
		final Long piId = getProcessInstanceId(theCase);
		if (piId == null)
			return null;

		return context.getProcessInstance(piId).getProcessDefinition();
	}

	@Override
	public List<Link> getCaseLinks(Case theCase, String casesComponentType) {
		throw new UnsupportedOperationException("Implement with correct pages if needed");
	}

	@Override
	public UIComponent getView(IWContext iwc, Integer caseId, String type, String caseManagerType) {
		if (!caseHandlerType.equals(caseManagerType))
			return super.getView(iwc, caseId, type, caseManagerType);

		CasesBPMAssetsState stateBean = WFUtil.getBeanInstance(CasesBPMAssetsState.beanIdentifier);
		stateBean.setDisplayPropertyForStyleAttribute(Boolean.FALSE);
		stateBean.setStandAloneComponent(Boolean.FALSE);

		CaseManagerState managerState = ELUtil.getInstance().getBean(CaseManagerState.beanIdentifier);
		managerState.setFullView(!CasesRetrievalManager.CASE_LIST_TYPE_USER.equals(type));

		if (stateBean.getCustomView() == null) {
			UICasesBPMAssets casesAssets = (UICasesBPMAssets)iwc.getApplication().createComponent(UICasesBPMAssets.COMPONENT_TYPE);
			UIViewRoot viewRoot = iwc.getViewRoot();
			if (viewRoot != null)
				casesAssets.setId(viewRoot.createUniqueId());

			casesAssets.setUsePdfDownloadColumn(stateBean.getUsePDFDownloadColumn() == null ? false : stateBean.getUsePDFDownloadColumn());
			casesAssets.setAllowPDFSigning(stateBean.getAllowPDFSigning() == null ? false : stateBean.getAllowPDFSigning());
			casesAssets.setHideEmptySection(stateBean.getHideEmptySection() == null ? false : stateBean.getHideEmptySection());
			casesAssets.setCommentsPersistenceManagerIdentifier(stateBean.getCommentsPersistenceManagerIdentifier());
			casesAssets.setShowAttachmentStatistics(stateBean.getShowAttachmentStatistics() == null ? false : stateBean.getShowAttachmentStatistics());
			casesAssets.setShowOnlyCreatorInContacts(stateBean.getShowOnlyCreatorInContacts() == null ? false : stateBean.getShowOnlyCreatorInContacts());
			casesAssets.setShowLogExportButton(stateBean.isShowLogExportButton());
			casesAssets.setShowComments(stateBean.getShowComments() == null ? Boolean.TRUE : stateBean.getShowComments());
			casesAssets.setShowContacts(stateBean.getShowContacts() == null ? Boolean.TRUE : stateBean.getShowContacts());
			casesAssets.setSpecialBackPage(stateBean.getSpecialBackPage());
			casesAssets.setNameFromExternalEntity(stateBean.isNameFromExternalEntity());
			casesAssets.setShowUserProfilePicture(stateBean.getShowUserProfilePicture());
			casesAssets.setShowUserCompany(stateBean.getShowUserCompany());
			casesAssets.setShowLastLoginDate(stateBean.getShowLastLoginDate());
			casesAssets.setInactiveTasksToShow(stateBean.getInactiveTasksToShow());

			if (caseId != null)
				casesAssets.setCaseId(caseId);
			return casesAssets;
		} else {
			AbstractCasesBPMAssets casesAssets =  (AbstractCasesBPMAssets)iwc.getApplication().createComponent(stateBean.getCustomView());

			UIViewRoot viewRoot = iwc.getViewRoot();
			if (viewRoot != null)
				casesAssets.setId(viewRoot.createUniqueId());

			casesAssets.setCaseState(stateBean);
			if (caseId != null)
				casesAssets.setCaseId(caseId);
			return casesAssets;
		}
	}

	@Override
	public PagedDataCollection<CasePresentation> getCases(
			User user,
			String type,
			Locale locale,
			List<String> caseCodes,
			List<String> caseStatusesToHide,
			List<String> caseStatusesToShow,
			int startIndex,
			int count,
			boolean onlySubscribedCases,
			boolean showAllCases
	) {
		return getCases(
				user,
				type,
				locale,
				caseCodes,
				caseStatusesToHide,
				caseStatusesToShow,
				startIndex,
				count,
				onlySubscribedCases,
				showAllCases,
				null,
				null,
				false
		);
	}

	@Override
	public PagedDataCollection<CasePresentation> getCases(
			User user,
			String type,
			Locale locale,
			List<String> caseCodes,
			List<String> caseStatusesToHide,
			List<String> caseStatusesToShow,
			int startIndex,
			int count,
			boolean onlySubscribedCases,
			boolean showAllCases,
			List<Long> procInstIds,
			Set<String> roles,
			boolean searchQuery
	) {
		try {
			long start = System.currentTimeMillis();

			if (ListUtil.isEmpty(roles) && user != null) {
				roles = getApplication().getAccessController().getAllRolesForUser(user);
			}

			List<Integer> casesIds = getCasePrimaryKeys(
					user,
					type,
					caseCodes,
					caseStatusesToHide,
					caseStatusesToShow,
					onlySubscribedCases,
					showAllCases,
					procInstIds,
					roles,
					null,
					searchQuery
			);
			long duration = System.currentTimeMillis() - start;
			if (duration > 1000) {
				getLogger().info("It took " + duration + " ms to resolve cases IDs for " + user + ", type: " + type + ", locale: " + locale +
						", case codes: " + caseCodes + ", statuses to hide: " + caseStatusesToHide + ", statuses to show: " + caseStatusesToShow +
						", only subscribed cases: " + onlySubscribedCases + ", show all cases: " + showAllCases + ", proc. inst. IDs: " + procInstIds +
						", roles: " + roles);
			}

			if (ListUtil.isEmpty(casesIds)) {
				LOGGER.info("User '" + user + "' doesn't have any cases in case list: " + type);
				return new PagedDataCollection<CasePresentation>(new ArrayList<CasePresentation>(), Long.valueOf(0));
			}

			start = System.currentTimeMillis();
			int totalCount = casesIds.size();
			Collection<? extends Case> cases = null;
			if (startIndex < totalCount) {
				Collection<Integer> casesToFetch = null;
				if (startIndex + count < totalCount) {
					casesToFetch = casesIds.subList(startIndex, (startIndex + count));
				} else {
					casesToFetch = casesIds.subList(startIndex, totalCount);
				}
				if (!CasesRetrievalManager.CASE_LIST_TYPE_USER.equals(type)) {
					cases = getCasesBusiness().getGeneralCaseHome().findAllByIds(casesToFetch);
				} else {
					cases = getCaseBusiness().getCasesByIds(casesToFetch);
				}
			} else if (startIndex == Integer.MAX_VALUE) {
				cases = getCaseBusiness().getCasesByIds(casesIds);
			} else {
				cases = getCaseBusiness().getCasesByIds(casesIds);
			}

			List<CasePresentation> casesPresentation = convertToPresentationBeans(cases, locale);
			if (!ListUtil.isEmpty(casesPresentation) &&
					IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("extra_cases_sorting", Boolean.FALSE))
				Collections.sort(casesPresentation, new CasePresentationComparator());

			duration = System.currentTimeMillis() - start;
			if (duration > 1000) {
				getLogger().info("Cases (total: " + cases.size() + ") converted into beans in " + duration + " ms");
			}

			return new PagedDataCollection<CasePresentation>(casesPresentation, Long.valueOf(totalCount));
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Some error occurred while getting cases for user: " + user + " by type: " + type + ", by locale: " + locale +
					", by case codes: " + caseCodes + ", by case statuses to hide: " + caseStatusesToHide + ", by case statuses to show: " +
					caseStatusesToShow + ", start index: " + startIndex + ", count: " + count + ", only subscribed cases: " + onlySubscribedCases, e);
		}

		return new PagedDataCollection<CasePresentation>(new ArrayList<CasePresentation>(), Long.valueOf(0));
	}

	@Override
	public List<Integer> getCasePrimaryKeys(
			User user,
			String type,
			List<String> caseCodes,
			List<String> caseStatusesToHide,
			List<String> caseStatusesToShow,
			boolean onlySubscribedCases,
			boolean showAllCases,
			List<Long> procInstIds,
			Set<String> roles,
			Collection<Long> handlerCategoryIDs,
			boolean searchQuery
	) throws Exception {
		return getCasesIds(
				user,
				type,
				caseCodes,
				caseStatusesToHide,
				caseStatusesToShow,
				onlySubscribedCases,
				showAllCases,
				null,
				procInstIds,
				roles,
				handlerCategoryIDs,
				searchQuery
		);
	}

	@Override
	public List<Integer> getCaseIds(
			User user,
			String type,
			List<String> caseCodes,
			List<String> statusesToHide,
			List<String> statusesToShow,
			boolean onlySubscribedCases,
			boolean showAllCases
	) throws Exception {
		return getCasesIds(user, type, caseCodes, statusesToHide, statusesToShow, onlySubscribedCases, showAllCases, null, null, null, null, false);
	}

	@Override
	public List<Integer> getCaseIds(
			User user,
			String type,
			List<String> caseCodes,
			List<String> caseStatusesToHide,
			List<String> caseStatusesToShow,
			boolean onlySubscribedCases,
			boolean showAllCases,
			List<Long> procInstIds,
			Set<String> roles
	) throws Exception {
		return getCasesIds(
				user,
				type,
				caseCodes,
				caseStatusesToHide,
				caseStatusesToShow,
				onlySubscribedCases,
				showAllCases,
				null,
				procInstIds,
				roles,
				null,
				false
		);
	}

	private Set<String> getRoles(HttpSession session, AccessController access, User user) {
		if (user == null) {
			return null;
		}

		if (session == null) {
			return access.getAllRolesForUser(user);
		}

		String activeRole = (String) session.getAttribute(CoreConstants.ACTIVE_ROLE);
		if (StringUtil.isEmpty(activeRole)) {
			return access.getAllRolesForUser(user);
		}

		return new HashSet<String>(Arrays.asList(activeRole));
	}

	private Map<Integer, Date> getCaseIds(
			User user,
			String type,
			List<String> caseCodes,
			List<String> caseStatusesToHide,
			List<String> caseStatusesToShow,
			boolean onlySubscribedCases,
			boolean showAllCases,
			Integer caseId,
			List<Long> procInstIds,
			Set<String> roles,
			Collection<Long> handlerCategoryIDs,
			Timestamp from,
			Timestamp to,
			List<String> statusesToShow,
			List<String> statusesToHide,
			List<Integer> groups,
			List<String> casecodes,
			boolean isSuperAdmin
	) throws Exception {
		if (CasesRetrievalManager.CASE_LIST_TYPE_OPEN.equals(type)) {
			return isSuperAdmin ?
						getCasesBPMDAO().getOpenCasesIdsForAdmin(
								caseCodes,
								statusesToShow,
								statusesToHide,
								caseId,
								procInstIds,
								handlerCategoryIDs,
								from,
								to
						) :
						getCasesBPMDAO().getOpenCasesIds(
								user,
								caseCodes,
								statusesToShow,
								statusesToHide,
								groups,
								roles,
								onlySubscribedCases,
								caseId,
								procInstIds,
								handlerCategoryIDs,
								from,
								to
						);
		} else if (CasesRetrievalManager.CASE_LIST_TYPE_CLOSED.equals(type)) {
			return isSuperAdmin ?
						getCasesBPMDAO().getClosedCasesIdsForAdmin(
								statusesToShow,
								statusesToHide,
								caseId,
								procInstIds,
								handlerCategoryIDs,
								from,
								to
						) :
						getCasesBPMDAO().getClosedCasesIds(
								user,
								statusesToShow,
								statusesToHide,
								groups,
								roles,
								onlySubscribedCases,
								caseId,
								procInstIds,
								handlerCategoryIDs,
								from,
								to
						);
		} else if (CasesRetrievalManager.CASE_LIST_TYPE_MY.equals(type)) {
			return getCasesBPMDAO().getMyCasesIds(
					user,
					statusesToShow,
					statusesToHide,
					onlySubscribedCases,
					caseId,
					procInstIds,
					handlerCategoryIDs,
					from,
					to
			);
		} else if (CasesRetrievalManager.CASE_LIST_TYPE_USER.equals(type)) {
			return getCasesBPMDAO().getUserCasesIds(
					user,
					statusesToShow,
					statusesToHide,
					casecodes,
					roles,
					onlySubscribedCases,
					caseId,
					procInstIds,
					handlerCategoryIDs,
					from,
					to
			);
		} else if (CasesRetrievalManager.CASE_LIST_TYPE_PUBLIC.equals(type)) {
			return getCasesBPMDAO().getPublicCasesIds(
					statusesToShow,
					statusesToHide,
					caseCodes,
					caseId != null ? Arrays.asList(caseId) : null,
					procInstIds,
					handlerCategoryIDs,
					from,
					to
			);
		} else if (CasesRetrievalManager.CASE_LIST_TYPE_HANDLER.equals(type)) {
			return getCasesBPMDAO().getHandlerCasesIds(
					user,
					caseStatusesToShow,
					caseStatusesToHide,
					casecodes,
					caseId != null ? Arrays.asList(caseId) : null,
					procInstIds,
					roles,
					handlerCategoryIDs,
					from,
					to
			);
		} else {
			getLogger().warning("Unknown cases list type: '" + type + "'");
		}
		return null;
	}

	private Collection<Long> getHandlerCategoryIDs(User user, Collection<Long> handlerCategoryIDs) {
		if (user == null || ListUtil.isEmpty(handlerCategoryIDs)) {
			return null;
		}

		handlerCategoryIDs = new ArrayList<Long>(handlerCategoryIDs);
		Map<String, CasesSubcriberManager> beans = getBeansOfType(CasesSubcriberManager.class);
		if (!MapUtil.isEmpty(beans)) {
			for (CasesSubcriberManager bean: beans.values()) {
				List<Integer> ids = bean.getSuperHandlersGroups(user);
				if (!ListUtil.isEmpty(ids)) {
					for (Integer id: ids) {
						Long tmp = id.longValue();
						if (!handlerCategoryIDs.contains(tmp)) {
							handlerCategoryIDs.add(tmp);
						}
					}
				}
			}
		}

		return handlerCategoryIDs;
	}

	@Override
	protected List<Integer> getCasesIds(
			User user,
			String type,
			List<String> caseCodes,
			List<String> caseStatusesToHide,
			List<String> caseStatusesToShow,
			boolean onlySubscribedCases,
			boolean showAllCases,
			Integer caseId,
			List<Long> procInstIds,
			Set<String> roles,
			Collection<Long> handlerCategoryIDs,
			boolean searchQuery
	) throws Exception {
		Map<Integer, Date> casesIds = null;

		List<String> statusesToShow = caseStatusesToShow == null ? new ArrayList<String>() : new ArrayList<String>(caseStatusesToShow);
		List<String> statusesToHide = caseStatusesToHide == null ? new ArrayList<String>() : new ArrayList<String>(caseStatusesToHide);
		statusesToHide = ListUtil.getFilteredList(statusesToHide);

		List<Integer> groups = null;
		List<String> casecodes = null;

		/* Checking for administrator or cases super administrator */
		IWContext iwc = CoreUtil.getIWContext();
		AccessController accessController = IWMainApplication.getDefaultIWMainApplication().getAccessController();
		boolean isSuperAdmin = false;
		if (iwc == null) {
			isSuperAdmin = user == null ? false :
					user.equals(accessController.getAdministratorUser()) || accessController.hasRole(user, CasesConstants.ROLE_CASES_SUPER_ADMIN);
		} else {
			isSuperAdmin = iwc.isSuperAdmin() || iwc.hasRole(CasesConstants.ROLE_CASES_SUPER_ADMIN);
		}

		CasesCacheCriteria key = null;
		boolean casesListCacheTurnedOn = isCacheUpdateTurnedOn();
		try {
			CasesListParameters params = new CasesListParameters(user, type, isSuperAdmin, statusesToHide, statusesToShow);
			params = resolveParameters(params, showAllCases);
			params.setRoles(roles);
			statusesToShow = showAllCases ? statusesToShow : ListUtil.isEmpty(caseStatusesToShow) ? params.getStatusesToShow() : caseStatusesToShow;
			statusesToHide = showAllCases ? statusesToHide : ListUtil.isEmpty(caseStatusesToHide) ? params.getStatusesToHide() : caseStatusesToHide;

			/* Getting roles for given user */
			roles = params.getRoles();
			if (ListUtil.isEmpty(roles) && user != null) {
				roles = getRoles(iwc == null ? getSession() : iwc.getSession(), accessController, user);
			}

			statusesToShow = showAllCases ? statusesToShow : ListUtil.isEmpty(caseStatusesToShow) ? params.getStatusesToShow() : caseStatusesToShow;
			statusesToHide = showAllCases ? statusesToHide : ListUtil.isEmpty(caseStatusesToHide) ? params.getStatusesToHide() : caseStatusesToHide;
			groups = params.getGroups();
			caseCodes = ListUtil.isEmpty(caseCodes) ? null : caseCodes;
			casecodes = params.getCodes();
			casecodes = ListUtil.isEmpty(casecodes) ? null : casecodes;
			type = StringUtil.isEmpty(type) ? CasesRetrievalManager.CASE_LIST_TYPE_OPEN : type;
			handlerCategoryIDs = getHandlerCategoryIDs(user, handlerCategoryIDs);

			/* Querying cache */
			if (casesListCacheTurnedOn && caseId == null) {
				key = getCacheKey(
						user,
						type,
						caseCodes,
						statusesToHide,
						statusesToShow,
						onlySubscribedCases,
						roles,
						groups,
						casecodes,
						showAllCases,
						procInstIds,
						handlerCategoryIDs
				);

				Map<Integer, Date> cachedData = getCachedIds(key);
				if (!MapUtil.isEmpty(cachedData) && cachedData.size() > 5) {
					if (!searchQuery) {
						IWTimestamp from = new IWTimestamp();
						from.setHour(0);
						from.setMinute(0);
						from.setSecond(0);
						from.setMilliSecond(0);
						IWTimestamp to = new IWTimestamp(from.getTime());
						to.setDay(to.getDay() + 1);
						to.setMilliSecond(to.getMilliSecond() - 1);
						casesIds = getCaseIds(
								user,
								type,
								caseCodes,
								caseStatusesToHide,
								caseStatusesToShow,
								onlySubscribedCases,
								showAllCases,
								caseId,
								procInstIds,
								roles,
								handlerCategoryIDs,
								from.getTimestamp(),
								to.getTimestamp(),
								statusesToShow,
								statusesToHide,
								groups,
								casecodes,
								isSuperAdmin
						);
						if (!MapUtil.isEmpty(casesIds)) {
							for (Map.Entry<Integer, Date> caseEntry: casesIds.entrySet()) {
								cachedData.put(caseEntry.getKey(), caseEntry.getValue());
							}
						}
					}

					return getSortedIds(cachedData);
				} else {
					removeFromCache(key);
				}
			}

			/* Re-subscribing if not subscribed before */
			if (onlySubscribedCases && user != null) {
				try {
					Map<String, CasesSubcriberManager> beans = getBeansOfType(CasesSubcriberManager.class);
					if (!MapUtil.isEmpty(beans)) {
						for (CasesSubcriberManager bean: beans.values()) {
							bean.doEnsureUserIsSubscribed(user);
						}
					}
				} catch (Exception e) {
					getLogger().log(Level.WARNING, "Error while ensuring user " + user + " is subscribed to cases", e);
				}
			}

			if (!ListUtil.isEmpty(procInstIds) && procInstIds.size() > 7500) {
				procInstIds = Collections.emptyList();
			}

			casesIds = getCaseIds(
					user,
					type,
					caseCodes,
					caseStatusesToHide,
					caseStatusesToShow,
					onlySubscribedCases,
					showAllCases,
					caseId,
					procInstIds,
					roles,
					handlerCategoryIDs,
					null,	//	from
					null,	//	to
					statusesToShow,
					statusesToHide,
					groups,
					casecodes,
					isSuperAdmin
			);
			if (casesIds == null) {
				return Collections.emptyList();
			}

			return new ArrayList<Integer>(casesIds.keySet());
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting cases for " + user + ", type: " + type + ", case codes: " + caseCodes + ", statuses to hide: " + caseStatusesToHide + ", statuses to show: " +
					caseStatusesToShow + ", only subscribed cases to: " + onlySubscribedCases + ", show all cases: " + showAllCases + ", case ID: " + caseId + ", proc. inst. IDs: " + procInstIds + ", roles: " + roles +
					", handler category IDs: " + handlerCategoryIDs + ", statuses to show: " + statusesToShow + ", statuses to hide: " + statusesToHide + ", groups: " + groups + ", case codes: " + casecodes +
					", super admin: " + isSuperAdmin, e);
			throw new RuntimeException(e);
		} finally {
			if (casesListCacheTurnedOn && caseId == null) {
				if (key == null) {
					key = getCacheKey(
							user,
							type,
							caseCodes,
							statusesToHide,
							statusesToShow,
							onlySubscribedCases,
							roles,
							groups,
							casecodes,
							showAllCases,
							procInstIds,
							handlerCategoryIDs
					);
				}
				putIdsToCache(casesIds, key);
			}
			getLogger().log(Level.INFO, this.getClass().getName() + " found " + casesIds.size() + " case ids.");
		}
	}

	private CasesListParameters resolveParameters(CasesListParameters params, boolean showAllCases) throws Exception {
		User user = params.getUser();
		String type = params.getType();
		boolean isSuperAdmin = params.isSuperAdmin();

		List<String> statusesToShow = params.getStatusesToShow();
		List<String> statusesToHide = params.getStatusesToHide();

		Set<String> roles = params.getRoles();
		List<Integer> groups = params.getGroups();
		List<String> codes = params.getCodes();

		CasesBusiness casesBusiness = getServiceInstance(CasesBusiness.class);
		UserBusiness userBusiness = getServiceInstance(UserBusiness.class);
		AccessController accessController = IWMainApplication.getDefaultIWMainApplication().getAccessController();

		if (CasesRetrievalManager.CASE_LIST_TYPE_OPEN.equals(type)) {
			String[] caseStatuses = casesBusiness.getStatusesForOpenCases();
			if (!showAllCases) {
				statusesToShow.addAll(Arrays.asList(caseStatuses));
				statusesToShow = ListUtil.getFilteredList(statusesToShow);
				statusesToHide.addAll(Arrays.asList(casesBusiness.getStatusesForClosedCases()));
				statusesToHide = ListUtil.getFilteredList(statusesToHide);
			}

			if (!isSuperAdmin) {
				roles = getRoles(getSession(), accessController, user);

				Collection<Group> groupBeans = userBusiness.getUserGroupsDirectlyRelated(user);
				if (!ListUtil.isEmpty(groupBeans)) {
					groups = new ArrayList<Integer>(groupBeans.size());
					for (Group group : groupBeans)
						groups.add(new Integer(group.getPrimaryKey().toString()));
				}
			}

		} else if (CasesRetrievalManager.CASE_LIST_TYPE_CLOSED.equals(type)) {
			String[] caseStatuses = casesBusiness.getStatusesForClosedCases();
			if (!showAllCases) {
				statusesToShow.addAll(Arrays.asList(caseStatuses));
				statusesToShow = ListUtil.getFilteredList(statusesToShow);
			}

			if (!isSuperAdmin) {
				if (user != null) {
					roles = getRoles(getSession(), accessController, user);
				}

				Collection<Group> groupBeans = userBusiness.getUserGroupsDirectlyRelated(user);
				if (!ListUtil.isEmpty(groupBeans)) {
					groups = new ArrayList<Integer>(groupBeans.size());
					for (Group group : groupBeans) {
						groups.add(new Integer(group.getPrimaryKey().toString()));
					}
				}
			}

		}  else if (CasesRetrievalManager.CASE_LIST_TYPE_MY.equals(type)) {
			String[] caseStatuses = casesBusiness.getStatusesForMyCases();
			if (!showAllCases) {
				statusesToShow.addAll(Arrays.asList(caseStatuses));
				statusesToShow = ListUtil.getFilteredList(statusesToShow);
			}

		} else if (CasesRetrievalManager.CASE_LIST_TYPE_USER.equals(type)) {
			statusesToShow = showAllCases ? statusesToShow : ListUtil.getFilteredList(statusesToShow);
			CaseCode[] casecodes = getCaseBusiness().getCaseCodesForUserCasesList();
			if (user != null) {
				roles = getRoles(getSession(), accessController, user);
			}

			codes = new ArrayList<String>(casecodes.length);
			for (CaseCode code : casecodes) {
				codes.add(code.getCode());
			}
		}

		params.setCodes(codes);
		params.setGroups(groups);
		params.setRoles(roles);
		if (!showAllCases) {
			params.setStatusesToHide(statusesToHide);
			params.setStatusesToShow(statusesToShow);
		}

		return params;
	}

	@Override
	protected CaseBusiness getCaseBusiness() {
		try {
			return IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), CasesBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	private CasesBusiness getCasesBusiness() {
		try {
			return (CasesBusiness) getCaseBusiness();
		} catch (Exception e) {
			throw new IBORuntimeException(e);
		}
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}

	private final HashMap<String, String> uris = new HashMap<String, String>(2);

	protected String getPageUri(IWApplicationContext iwac, String pageType) {
		if (uris != null && uris.containsKey(pageType))
			return uris.get(pageType);

		Collection<ICPage> icpages = getPages(pageType);
		if (icpages == null || icpages.isEmpty()) {
//			TODO: create egov bpm page, as not found
			throw new RuntimeException("No page found by page type: "+pageType);
		}

		ICPage icPage = null;
		if (icPage == null)
			icPage = icpages.iterator().next();

		String uri = icPage.getDefaultPageURI();
		if (!uri.startsWith(CoreConstants.PAGES_URI_PREFIX))
			uri = CoreConstants.PAGES_URI_PREFIX + uri;

		String ruri = iwac.getIWMainApplication().getTranslatedURIWithContext(uri);
		uris.put(pageType, ruri);

		return ruri;
	}

	protected Collection<ICPage> getPages(String pageSubType) {
		try {
			ICPageHome home = (ICPageHome) IDOLookup.getHome(ICPage.class);
			return home.findBySubType(pageSubType, false);
		} catch (Exception e) {
			throw new RuntimeException("Exception while resolving pages by subType: "+pageSubType, e);
		}
	}

	public BPMContext getBpmContext() {
		return bpmContext;
	}

	public void setBpmContext(BPMContext bpmContext) {
		this.bpmContext = bpmContext;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	@Override
	public Map<Long, String> getAllCaseProcessDefinitionsWithName() {
		List<ProcessDefinition> allProcesses = getAllProcessDefinitions();
		if (ListUtil.isEmpty(allProcesses)) {
			return null;
		}

		Map<Long, String> processes = new HashMap<Long, String>();

		Locale locale = null;
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc != null) {
			locale = iwc.getCurrentLocale();
		}
		if (locale == null) {
			locale = Locale.ENGLISH;
		}

		ApplicationHome appHome = null;
		try {
			appHome = (ApplicationHome) IDOLookup.getHome(Application.class);
		} catch (IDOLookupException e) {
			e.printStackTrace();
		}
		if (appHome == null) {
			return null;
		}

		String localizedName = null;
		for (ProcessDefinition pd: allProcesses) {
			localizedName = getProcessDefinitionLocalizedName(pd, locale, appHome);
			localizedName = StringUtil.isEmpty(localizedName) ? pd.getName() : localizedName;

			processes.put(Long.valueOf(pd.getId()), localizedName);
		}
		return processes;
	}

	@Override
	public List<Long> getAllCaseProcessDefinitions() {
		List<ProcessDefinition> allProcesses = getAllProcessDefinitions();
		if (ListUtil.isEmpty(allProcesses)) {
			return null;
		}

		List<Long> ids = new ArrayList<Long>();
		for (ProcessDefinition pd: allProcesses) {
			ids.add(Long.valueOf(pd.getId()));
		}

		return ids;
	}

	//	TODO: use case type!
	private List<ProcessDefinition> getAllProcessDefinitions() {
		final List<CaseTypesProcDefBind> casesProcesses = getCasesBPMDAO().getAllCaseTypes();
		if (ListUtil.isEmpty(casesProcesses))
			return null;

		return getBpmContext().execute(new JbpmCallback<List<ProcessDefinition>>() {

			@Override
			public List<ProcessDefinition> doInJbpm(JbpmContext context) throws JbpmException {

				ProcessDefinition pd = null;
				List<ProcessDefinition> caseProcessDefinitions = new ArrayList<ProcessDefinition>();

				GraphSession graphSession = context.getGraphSession();

				for (CaseTypesProcDefBind caseTypesProcDefBind : casesProcesses) {
					pd = graphSession.findLatestProcessDefinition(caseTypesProcDefBind.getProcessDefinitionName());

					if (pd != null && !caseProcessDefinitions.contains(pd)) {
						caseProcessDefinitions.add(pd);
					}
				}

				return caseProcessDefinitions;
			}
		});
	}

	@Override
	public String getProcessName(String processName, Locale locale) {
		ProcessDefinition pd = getBpmFactory().getBPMDAO().findLatestProcessDefinition(processName);
		if (pd == null) {
			return null;
		}

		try {
			return getProcessDefinitionLocalizedName(pd, locale, (ApplicationHome) IDOLookup.getHome(Application.class));
		} catch (IDOLookupException e) {
			e.printStackTrace();
		}

		return null;
	}

	private String getProcessDefinitionLocalizedName(ProcessDefinition pd, Locale locale, ApplicationHome appHome) {
		if (pd == null || locale == null || appHome == null) {
			return null;
		}

		Collection<Application> apps = null;
		try {
			apps = appHome.findAllByApplicationUrl(pd.getName());
		} catch (FinderException e) {
			e.printStackTrace();
		}
		if (ListUtil.isEmpty(apps)) {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Didn't find any application by URL: " + pd.getName() +
					", returning standard name!");
			return pd.getName();
		}

		ApplicationBusiness applicationBusiness = getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), ApplicationBusiness.class);
		if (applicationBusiness == null) {
			return pd.getName();
		}

		return applicationBusiness.getApplicationName(apps.iterator().next(), locale);
	}

	@Override
	public Long getLatestProcessDefinitionIdByProcessName(String name) {
		ProcessDefinition pd = getBpmFactory().getBPMDAO().findLatestProcessDefinition(name);
		return pd == null ? null : pd.getId();
	}

	@Override
	public PagedDataCollection<CasePresentation> getCasesByIds(List<Integer> ids, Locale locale) {
		Long start = System.currentTimeMillis();
		Collection<Case> cases = getCasesBusiness().getCasesByIds(ids);
		return getCasesByEntities(cases, locale);
	}

	@Override
	public PagedDataCollection<CasePresentation> getCasesByEntities(Collection<Case> cases, Locale locale) {
		return new PagedDataCollection<CasePresentation>(convertToPresentationBeans(cases, locale), Long.valueOf(cases.size()));
	}

	@SuppressWarnings("unchecked")
	@Override
	public PagedDataCollection<CasePresentation> getClosedCases(Collection<Group> groups) {
		try {
			Collection<Case> closedCases = getCasesBusiness().getClosedCases(groups);
			List<CasePresentation> presentationBeans = convertToPresentationBeans(closedCases, CoreUtil.getIWContext().getCurrentLocale());
			return new PagedDataCollection<CasePresentation>(presentationBeans);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return super.getClosedCases(groups);
	}

	@SuppressWarnings("unchecked")
	@Override
	public PagedDataCollection<CasePresentation> getMyCases(User user) {
		try {
			Collection<GeneralCase> closedCases = getCasesBusiness().getMyCases(user);
			List<CasePresentation> presentationBeans = convertToPresentationBeans(closedCases, CoreUtil.getIWContext().getCurrentLocale());
			return new PagedDataCollection<CasePresentation>(presentationBeans);
		}
		catch (RemoteException re) {
			throw new IBORuntimeException(re);
		}
	}

	@Override
	protected CasePresentation convertToPresentation(Case theCase, CasePresentation bean, Locale locale) {
		if (bean == null) {
			bean = new CasePresentation();
		}

		bean = super.convertToPresentation(theCase, bean, locale);

		bean.setBpm(caseHandlerType.equals(theCase.getCaseManagerType()));

		String caseIdentifier = theCase.getCaseIdentifier();
		if (caseIdentifier == null) {
			caseIdentifier = getProcessIdentifier(theCase);
		}
		if (caseIdentifier == null) {
			caseIdentifier = theCase.getPrimaryKey().toString();
		}
		bean.setCaseIdentifier(caseIdentifier);

		if (theCase instanceof GeneralCase) {

			GeneralCase generalCase = (GeneralCase) theCase;

			bean.setHandledBy(generalCase.getHandledBy());
			bean.setPrivate(generalCase.isPrivate());
			CaseCategory caseCategory = generalCase.getCaseCategory();
			if (caseCategory != null) {
				if (ProcessBundleCasesImpl.defaultCaseCategoryName.equals(caseCategory.getName())) {
					String processName = getProcessDefinitionName(theCase);
					bean.setProcessName(processName);
				}

				bean.setCategoryId(caseCategory.getPrimaryKey().toString());
			}
			try {
				bean.setCaseStatus(getCasesBusiness().getCaseStatus(theCase.getStatus()));
			} catch (RemoteException e) {
				bean.setCaseStatus(theCase.getCaseStatus());
			}

			if (bean.getCaseStatus() != null) {
				bean.setLocalizedStatus(getLocalizedStatus(theCase, theCase.getCaseStatus(), locale));
			}
		}
		return bean;
	}

	@Override
	public Long getTaskInstanceIdForTask(Case theCase, String taskName) {
		Long piId = getProcessInstanceId(theCase);
		if (piId == null) {
			return null;
		}

		ProcessInstanceW processInstance = getBpmFactory().getProcessManagerByProcessInstanceId(piId).getProcessInstance(piId);
		if (processInstance == null) {
			return null;
		}

		List<TaskInstanceW> tasks = processInstance.getAllUnfinishedTaskInstances();
		if (ListUtil.isEmpty(tasks)) {
			return null;
		}

		for (TaskInstanceW task: tasks) {
			if (taskName.equals(task.getTaskInstance().getName())) {
				return task.getTaskInstanceId();
			}
		}

		return null;
	}

	@Override
	public List<Long> getCasesIdsByProcessDefinitionName(String processDefinitionName) {
		return getCasesBPMDAO().getCaseIdsByProcessDefinition(processDefinitionName);
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getCases(java.util.Collection, java.util.Collection, java.util.Collection, java.util.Collection)
	 */
	@Override
	public List<Case> getCases(Collection<String> processDefinitionNames,
			Collection<Long> processInstanceIds,
			Collection<String> caseStatuses,
			Collection<User> subscribers,
			Collection<String> caseManagerTypes,
			Date dateFrom,
			Date dateTo) {
		String[] primaryKeys = getCasesPrimaryKeys(
				processDefinitionNames,
				null,
				caseStatuses,
				subscribers,
				caseManagerTypes,
				dateFrom,
				dateTo);
		if (ArrayUtil.isEmpty(primaryKeys)) {
			return Collections.emptyList();
		}

		ArrayList<Integer> ids = new ArrayList<Integer>(primaryKeys.length);
		for (String id : primaryKeys) {
			ids.add(Integer.valueOf(id));
		}

		Collection<Case> casesCollection = getCasesBusiness().getCasesByIds(ids);
		if (ListUtil.isEmpty(casesCollection)) {
			return Collections.emptyList();
		}

		return new ArrayList<Case>(casesCollection);
	}
	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getCases(java.util.Collection, java.util.Collection, java.util.Collection)
	 */
	@Override
	public List<Case> getCases(Collection<String> processDefinitionNames,
			Collection<String> caseStatuses, Collection<User> subscribers) {
		return getCases(processDefinitionNames, null, caseStatuses, subscribers, null, null, null);
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getCases(java.util.Collection, java.util.Collection)
	 */
	@Override
	public List<Case> getCases(
			Collection<String> processDefinitionNames,
			Collection<String> caseStatuses) {
		return getCases(processDefinitionNames, caseStatuses, null);
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getCasesPrimaryKeys(java.util.Collection, java.util.Collection, java.util.Collection, java.util.Collection)
	 */
	@Override
	public String[] getCasesPrimaryKeys(
			Collection<String> processDefinitionNames,
			Collection<? extends Number> processInstanceIds,
			Collection<String> caseStatuses,
			Collection<User> subscribers,
			Collection<String> caseManagerTypes,
			Date dateCreatedFrom,
			Date dateCreatedTo) {

		List<Long> subscribersIds = null;

		/* Searching by subscribers */
		if (!ListUtil.isEmpty(subscribers)) {
			subscribersIds = new ArrayList<Long>(subscribers.size());
			for (User subscriber: subscribers) {
				subscribersIds.add(Long.valueOf(subscriber.getPrimaryKey().toString()));
			}
		}

		Map<Integer, Date> data = getCasesBPMDAO().getCasesPrimaryKeys(
				processDefinitionNames,
				processInstanceIds,
				caseStatuses, null, subscribersIds, null, null, null,
				caseManagerTypes, null, null, null, null, null, null, null, null,
				dateCreatedFrom, dateCreatedTo);
		if (MapUtil.isEmpty(data)) {
			return null;
		}

		List<String> ids = new ArrayList<String>();
		for (Integer id: data.keySet()) {
			ids.add(String.valueOf(id));
		}
		return ArrayUtil.convertListToArray(ids);
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getCasesPrimaryKeys(java.util.Collection, java.util.Collection, java.util.Collection)
	 */
	@Override
	public String[] getCasesPrimaryKeys(
			Collection<String> processDefinitionNames,
			Collection<String> caseStatuses,
			Collection<User> subscribers) {
		return getCasesPrimaryKeys(processDefinitionNames, null, caseStatuses,
				subscribers, null, null, null);
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager#getCasesPrimaryKeys(java.util.Collection, java.util.Collection)
	 */
	@Override
	public String[] getCasesPrimaryKeys(
			Collection<String> processDefinitionNames,
			Collection<String> caseStatuses) {
		return getCasesPrimaryKeys(processDefinitionNames, null,
				caseStatuses, null, null, null, null);
	}

	public VariablesHandler getVariablesHandler() {
		return variablesHandler;
	}

	public void setVariablesHandler(VariablesHandler variablesHandler) {
		this.variablesHandler = variablesHandler;
	}

	@Override
	public String resolveCaseId(IWContext iwc) {
		String caseId = super.resolveCaseId(iwc);
		if (!StringUtil.isEmpty(caseId))
			return caseId;

		String processInstanceId = iwc.getParameter(ProcessManagerBind.processInstanceIdParam);
		if (StringUtil.isEmpty(processInstanceId))
			return null;

		processInstanceId = StringHandler.replace(processInstanceId, CoreConstants.DOT, CoreConstants.EMPTY);
		processInstanceId = StringHandler.replace(processInstanceId, CoreConstants.SPACE, CoreConstants.EMPTY);
		try {
			Long piId = Long.valueOf(processInstanceId);
			return getCaseId(piId);
		} catch(NumberFormatException e) {
			getLogger().log(Level.WARNING, "Error converting process instance ID '" + processInstanceId + "' to Long");
		}
		return null;
	}

	private String getCaseId(Long processInstanceId) {
		CaseProcInstBind bind = null;
		try {
			bind = getCasesBPMDAO().getCaseProcInstBindByProcessInstanceId(processInstanceId);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return bind == null ? null : bind.getCaseId().toString();
	}

	@Override
	public User getCaseOwner(Object entityId) {
		String caseId = null;
		if (entityId instanceof Long) {
			caseId = getCaseId((Long) entityId);
		}
		return super.getCaseOwner(caseId == null ? entityId : caseId);
	}

	@Override
	public Collection<CasePresentation> getReLoadedCases(CasesSearchCriteriaBean criterias) {
		return getCasesEngine().getReLoadedCases(criterias);
	}

	public BPMCasesEngine getCasesEngine() {
		return casesEngine;
	}

	public void setCasesEngine(BPMCasesEngine casesEngine) {
		this.casesEngine = casesEngine;
	}

	private User getUser(Integer userId) {
		if (userId == null || userId < 0)
			return null;

		try {
			UserBusiness userBusiness = getServiceInstance(UserBusiness.class);
			return userBusiness.getUser(userId);
		} catch (Exception e) {}

		return null;
	}

	private boolean isCacheUpdateTurnedOn() {
		return getApplication().getSettings().getBoolean("update_cases_list_cache", Boolean.TRUE);
	}

	private Map<String, Boolean> casesBeingUpdated = new HashMap<String, Boolean>();

	private void doManageCasesCache(final Case theCase, final boolean ommitClearing) {
		if (theCase == null) {
			getLogger().warning("Case is undefined, unable to manage cache");
			return;
		}

		final String id = theCase.getId();
		if (casesBeingUpdated.containsKey(id)) {
			return;
		}

		Thread cacheManager = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					casesBeingUpdated.put(id, Boolean.TRUE);

					if (!isCacheUpdateTurnedOn()) {
						return;
					}

					Integer caseId = Integer.valueOf(id);
					Collection<CasesCacheCriteria> criterias = getCacheKeySet();
					for (CasesCacheCriteria criteria: criterias) {
						if (criteria == null)
							continue;

						if (ommitClearing && containsElement(criteria, caseId)) {
							//	No need to execute SQL query to verify if case ID still belongs to the cache because it is forbidden to remove ID
							continue;
						}

						User user = getUser(criteria.getUserId());
						boolean superAdmin = false;
						try {
							superAdmin = user == null ?
								false :
								IWMainApplication.getDefaultIWMainApplication().getAccessController().getAdministratorUser().equals(user);
						} catch (Exception e) {
							getLogger().log(Level.WARNING, "Error while resolving if user " + user + " is administrator");
						}

						List<Integer> ids = null;
						if (!superAdmin) {
							try {
								ids = getCasesIds(
										user,
										criteria.getType(),
										criteria.getCaseCodes(),
										criteria.getStatusesToHide(),
										criteria.getStatusesToShow(),
										criteria.isOnlySubscribedCases(),
										criteria.isShowAllCases(),
										caseId,
										criteria.getProcInstIds(),
										criteria.getRoles(),
										criteria.getHandlercategoryIds(),
										false
								);
							} catch (Exception e) {
								String caseInfo = "Case ID " + id + " (identifier: '" + theCase.getCaseIdentifier() + "', subject: '" +
										theCase.getSubject() + "', status: " + theCase.getCaseStatus() + ", created: " + theCase.getCreated() + ")";
								String message = "Error while verifying if modified case " + caseInfo + " belongs to the cache by key " +
										criteria.getKey() + " and user " + user;
								LOGGER.log(Level.WARNING, message, e);
								clearCache();
								return;
							}
						}

						boolean contains = superAdmin ? true : ListUtil.isEmpty(ids) ? false : ids.contains(caseId);
						if (contains) {
							addElementToCache(criteria, caseId, theCase.getCreated());
						} else if (!ommitClearing) {
							removeElementFromCache(criteria, caseId);
						}
					}
				} catch (Exception e) {
					String message = "Error updating cases list after case was modified or created: " + theCase;
					getLogger().log(Level.WARNING, message, e);

					CoreUtil.clearAllCaches();
				} finally {
					casesBeingUpdated.remove(id);
				}
			}
		});
		cacheManager.start();
	}

	@Override
	public void onApplicationEvent(final ApplicationEvent event) {
		if (event instanceof CaseModifiedEvent) {
			if (!isCacheUpdateTurnedOn()) {
				try {
					clearCache();
				} catch (Exception e) {
					getLogger().log(Level.WARNING, "Error clearing cases cache", e);
				}
				return;
			}

			Object source = event.getSource();
			if (!(source instanceof Case))
				return;

			Case theCase = (Case) source;
			doManageCasesCache(theCase, theCase == null || StringUtil.isEmpty(theCase.getSubject()));
		} else if (event instanceof TaskInstanceSubmitted) {
			if (!isCacheUpdateTurnedOn()) {
				try {
					clearCache();
				} catch (Exception e) {
					getLogger().log(Level.WARNING, "Error clearing cases cache", e);
				}
				return;
			}

			TaskInstanceSubmitted taskSubmitted = (TaskInstanceSubmitted) event;
			doUpdateBPMCase(taskSubmitted.getProcessInstanceId());
		} else if (event instanceof ProcessInstanceCreatedEvent) {
			if (!isCacheUpdateTurnedOn()) {
				try {
					clearCache();
				} catch (Exception e) {
					getLogger().log(Level.WARNING, "Error clearing cases cache", e);
				}
				return;
			}

			ProcessInstanceCreatedEvent procCreatedEvent = (ProcessInstanceCreatedEvent) event;

			Long piId = procCreatedEvent.getProcessInstanceId();
			doUpdateBPMCase(piId);
		} else if (event instanceof CaseDeletedEvent) {
			if (!isCacheUpdateTurnedOn()) {
				try {
					clearCache();
				} catch (Exception e) {
					getLogger().log(Level.WARNING, "Error clearing cases cache", e);
				}
				return;
			}

			Object source = event.getSource();
			if (!(source instanceof Case))
				return;

			Case theCase = (Case) source;
			Integer id = Integer.valueOf(theCase.getId());
			Collection<CasesCacheCriteria> keys = null;
			try {
				keys = getCacheKeySet();
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Error getting criterias of cases cache", e);
			}
			if (!ListUtil.isEmpty(keys)) {
				for (CasesCacheCriteria key: keys) {
					try {
						removeElementFromCache(key, id);
					} catch (Exception e) {
						getLogger().log(Level.WARNING, "Error removing element '" + id + "' from cases cache by criteria " + key, e);
					}
				}
			}
		}
	}

	private void doUpdateBPMCase(Long piId) {
		if (piId == null)
			return;

		CaseProcInstBind bind = null;
		try {
			bind = getCasesBPMDAO().getCaseProcInstBindByProcessInstanceId(piId);
		} catch (Exception e) {}
		if (bind == null) {
			getLogger().warning("Bind for process instance " + piId + " was not created yet!");
			return;
		}

		Case theCase = null;
		try {
			theCase = getCaseBusiness().getCase(bind.getCaseId());
		} catch (Exception e) {}
		if (theCase == null) {
			getLogger().warning("Case can not be found by ID " + bind.getCaseId() + " for process instance " + piId);
			return;
		}

		doManageCasesCache(theCase, true);
	}

	private class CasesListParameters {
		private User user;
		private String type;
		private boolean superAdmin;
		private List<String> statusesToHide;
		private List<String> statusesToShow;
		private Set<String> roles;
		private List<Integer> groups;
		private List<String> codes;

		private CasesListParameters(User user, String type, boolean superAdmin, List<String> statusesToHide, List<String> statusesToShow) {
			this.user = user;
			this.type = type;
			this.superAdmin = superAdmin;
			this.statusesToHide = statusesToHide;
			this.statusesToShow = statusesToShow;
		}

		public User getUser() {
			return user;
		}
		public String getType() {
			return type;
		}
		public boolean isSuperAdmin() {
			return superAdmin;
		}
		public List<String> getStatusesToHide() {
			return statusesToHide;
		}
		public void setStatusesToHide(List<String> statusesToHide) {
			this.statusesToHide = statusesToHide;
		}
		public List<String> getStatusesToShow() {
			return statusesToShow;
		}
		public void setStatusesToShow(List<String> statusesToShow) {
			this.statusesToShow = statusesToShow;
		}
		public Set<String> getRoles() {
			return roles;
		}
		public void setRoles(Set<String> roles) {
			this.roles = roles;
		}
		public List<Integer> getGroups() {
			return groups;
		}
		public void setGroups(List<Integer> groups) {
			this.groups = groups;
		}
		public List<String> getCodes() {
			return codes;
		}
		public void setCodes(List<String> codes) {
			this.codes = codes;
		}
	}

	protected Application getApplication(Object applicationID) {
		try {
			return getApplicationBusiness().getApplication(applicationID);
		} catch (RemoteException e) {
			getLogger().log(Level.WARNING,
					"Unable to connect to data source: ", e);
		} catch (FinderException e) {
			getLogger().log(Level.WARNING,
					"Unable to find " + Application.class + " by ID: " +
							applicationID);
		}

		return null;
	}

	protected ApplicationBusiness getApplicationBusiness() {
		if (this.applicationBusiness != null) {
			return this.applicationBusiness;
		}

		try {
			this.applicationBusiness = IBOLookup.getServiceInstance(
					getApplication().getIWApplicationContext(), ApplicationBusiness.class);
		} catch (IBOLookupException e) {
			getLogger().log(Level.WARNING,
					"Unable to get " + ApplicationBusiness.class + " cause of: ", e);
		}

		return this.applicationBusiness;
	}

	@Override
	public List<Case> getCases(Collection<ProcessInstanceW> processInstances) {
		if (ListUtil.isEmpty(processInstances)) {
			return Collections.emptyList();
		}

		ArrayList<Long> piids = new ArrayList<Long>();
		for (ProcessInstanceW processInstance: processInstances) {
			piids.add(processInstance.getProcessInstanceId());
		}

		return getCases(null, piids, null, null, null, null, null);
	}
}