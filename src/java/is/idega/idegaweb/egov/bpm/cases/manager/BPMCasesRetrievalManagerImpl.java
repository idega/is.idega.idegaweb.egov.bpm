package is.idega.idegaweb.egov.bpm.cases.manager;

import is.idega.idegaweb.egov.application.business.ApplicationBusiness;
import is.idega.idegaweb.egov.application.data.Application;
import is.idega.idegaweb.egov.application.data.ApplicationHome;
import is.idega.idegaweb.egov.bpm.business.CasesSubcriberManager;
import is.idega.idegaweb.egov.bpm.cases.bundle.ProcessBundleCasesImpl;
import is.idega.idegaweb.egov.bpm.cases.presentation.UICasesBPMAssets;
import is.idega.idegaweb.egov.bpm.cases.presentation.beans.BPMCasesEngine;
import is.idega.idegaweb.egov.bpm.cases.presentation.beans.CasesBPMAssetsState;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.CaseCategory;
import is.idega.idegaweb.egov.cases.data.GeneralCase;
import is.idega.idegaweb.egov.cases.util.CasesConstants;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.FinderException;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.db.GraphSession;
import org.jbpm.graph.def.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.support.WebApplicationContextUtils;

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
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.builder.data.ICPage;
import com.idega.core.builder.data.ICPageHome;
import com.idega.data.IDOLookup;
import com.idega.data.IDOLookupException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.CaseTypesProcDefBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.data.ProcessManagerBind;
import com.idega.jbpm.events.ProcessInstanceCreatedEvent;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.presentation.IWContext;
import com.idega.presentation.paging.PagedDataCollection;
import com.idega.presentation.text.Link;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.expression.ELUtil;
import com.idega.webface.WFUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.13 $
 *
 * Last modified: $Date: 2009/07/14 16:25:04 $ by $Author: valdas $
 */
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service(BPMCasesRetrievalManagerImpl.beanIdentifier)
@Transactional(readOnly = true)
public class BPMCasesRetrievalManagerImpl extends CasesRetrievalManagerImpl implements CasesRetrievalManager, ApplicationListener<ApplicationEvent> {

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

	public static final String	beanIdentifier = "casesBPMCaseHandler",
								caseHandlerType = "CasesBPM";

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

		if (caseId != null)
			casesAssets.setCaseId(caseId);

		return casesAssets;
	}

	@Override
	public PagedDataCollection<CasePresentation> getCases(User user,
			String type, Locale locale, List<String> caseCodes,
			List<String> caseStatusesToHide, List<String> caseStatusesToShow,
			int startIndex, int count, boolean onlySubscribedCases,
			boolean showAllCases) {
		return getCases(user, type, locale, caseCodes, caseStatusesToHide, caseStatusesToShow, startIndex, count, onlySubscribedCases, showAllCases,
				null, null);
	}

	@Override
	public PagedDataCollection<CasePresentation> getCases(User user, String type, Locale locale, List<String> caseCodes,
			List<String> caseStatusesToHide, List<String> caseStatusesToShow, int startIndex, int count, boolean onlySubscribedCases,
			boolean showAllCases, List<Long> procInstIds, Set<String> roles) {

		try {
			if (ListUtil.isEmpty(roles) && user != null) {
				roles = getApplication().getAccessController().getAllRolesForUser(user);
			}

			List<Integer> casesIds = getCaseIds(user, type, caseCodes,
					caseStatusesToHide, caseStatusesToShow, onlySubscribedCases,
					showAllCases, procInstIds, roles);

			if (ListUtil.isEmpty(casesIds)) {
				LOGGER.info("User '" + user + "' doesn't have any cases!");
				return new PagedDataCollection<CasePresentation>(new ArrayList<CasePresentation>(), 0);
			}

			long start = System.currentTimeMillis();
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
			} else
				cases = new ArrayList<Case>();

			List<CasePresentation> casesPresentation = convertToPresentationBeans(cases, locale);

			if (!ListUtil.isEmpty(casesPresentation) &&
					IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("extra_cases_sorting", Boolean.FALSE))
				Collections.sort(casesPresentation, new CasePresentationComparator());

			getLogger().info("Cases (total: " + cases.size() + ") converted into beans in " + (System.currentTimeMillis() - start) + " ms");

			return new PagedDataCollection<CasePresentation>(casesPresentation, totalCount);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Some error occurred while getting cases for user: " + user + " by type: " + type + ", by locale: " + locale +
					", by case codes: " + caseCodes + ", by case statuses to hide: " + caseStatusesToHide + ", by case statuses to show: " +
					caseStatusesToShow + ", start index: " + startIndex + ", count: " + count + ", only subscribed cases: " + onlySubscribedCases, e);
		}

		return new PagedDataCollection<CasePresentation>(new ArrayList<CasePresentation>(), 0);
	}

	@Override
	public List<Integer> getCaseIds(User user, String type, List<String> caseCodes, List<String> statusesToHide, List<String> statusesToShow,
			boolean onlySubscribedCases, boolean showAllCases) throws Exception {
		return getCaseIds(user, type, caseCodes, statusesToHide, statusesToShow, onlySubscribedCases, showAllCases, null, null, null);
	}
	@Override
	public List<Integer> getCaseIds(User user, String type, List<String> caseCodes, List<String> caseStatusesToHide, List<String> caseStatusesToShow,
			boolean onlySubscribedCases, boolean showAllCases, List<Long> procInstIds, Set<String> roles) throws Exception {
		return getCaseIds(user, type, caseCodes, caseStatusesToHide, caseStatusesToShow, onlySubscribedCases, showAllCases, null, procInstIds, roles);
	}

	@Override
	protected List<Integer> getCaseIds(User user, String type, List<String> caseCodes, List<String> caseStatusesToHide,
			List<String> caseStatusesToShow, boolean onlySubscribedCases, boolean showAllCases, Integer caseId, List<Long> procInstIds,
			Set<String> roles) throws Exception {

		List<Integer> caseIds = null;

		List<String> statusesToShow = caseStatusesToShow == null ? new ArrayList<String>() : new ArrayList<String>(caseStatusesToShow);
		List<String> statusesToHide = caseStatusesToHide == null ? new ArrayList<String>() : new ArrayList<String>(caseStatusesToHide);
		statusesToHide = ListUtil.getFilteredList(statusesToHide);

		List<Integer> groups = null;
		List<String> casecodes = null;

		try {
			IWContext iwc = CoreUtil.getIWContext();
			AccessController accessController = IWMainApplication.getDefaultIWMainApplication().getAccessController();

			boolean isSuperAdmin = false;
			if (iwc == null) {
				isSuperAdmin = user == null ? false :
						user.equals(accessController.getAdministratorUser()) || accessController.hasRole(user, CasesConstants.ROLE_CASES_SUPER_ADMIN);
			} else
				isSuperAdmin = iwc.isSuperAdmin() || iwc.hasRole(CasesConstants.ROLE_CASES_SUPER_ADMIN);

			CasesListParameters params = new CasesListParameters(user, type, isSuperAdmin, statusesToHide, statusesToShow);
			params = resolveParameters(params, showAllCases);
			params.setRoles(roles);
			statusesToShow = showAllCases ? statusesToShow : ListUtil.isEmpty(caseStatusesToShow) ? params.getStatusesToShow() : caseStatusesToShow;
			statusesToHide = showAllCases ? statusesToHide : ListUtil.isEmpty(caseStatusesToHide) ? params.getStatusesToHide() : caseStatusesToHide;
			roles = params.getRoles();
			if (ListUtil.isEmpty(roles) && user != null)
				roles = accessController.getAllRolesForUser(user);

			groups = params.getGroups();
			casecodes = params.getCodes();
			type = StringUtil.isEmpty(type) ? CasesRetrievalManager.CASE_LIST_TYPE_OPEN : type;

			caseIds = caseId == null ?
					getCachedIds(user, type, caseCodes, statusesToHide, statusesToShow, onlySubscribedCases, roles, groups, casecodes, showAllCases,
							procInstIds) :
					null;
			if (!ListUtil.isEmpty(caseIds)) {
				if (caseIds.size() > 5)
					return caseIds;
				else {
					CasesCacheCriteria key = getCacheKey(user, type, caseCodes, statusesToHide, statusesToShow, onlySubscribedCases, roles, groups,
							casecodes, showAllCases, procInstIds);
					getLogger().warning("Resolved only few cases IDs (" + caseIds + ") from cache by key '" + key +
							"', it is probably invalid cache entry, will delete it and will query DB");
					getCache().remove(key);
				}
			}

			if (onlySubscribedCases && user != null) {
				try {
					Map<?, ?> beans = WebApplicationContextUtils.getWebApplicationContext(getApplication().getServletContext())
							.getBeansOfType(CasesSubcriberManager.class);
					if (!MapUtil.isEmpty(beans)) {
						for (Object bean: beans.values()) {
							if (bean instanceof CasesSubcriberManager)
								((CasesSubcriberManager) bean).doEnsureUserIsSubscribed(user);
						}
					}
				} catch (Exception e) {
					getLogger().log(Level.WARNING, "Error while ensuring user " + user + " is subscribed to cases", e);
				}
			}

			if (!ListUtil.isEmpty(procInstIds) && procInstIds.size() > 7500)
				procInstIds = Collections.emptyList();

			if (CasesRetrievalManager.CASE_LIST_TYPE_OPEN.equals(type)) {
				caseIds = isSuperAdmin ?
							getCasesBPMDAO().getOpenCasesIdsForAdmin(caseCodes, statusesToShow, statusesToHide, caseId, procInstIds) :
							getCasesBPMDAO().getOpenCasesIds(user, caseCodes, statusesToShow, statusesToHide, groups, roles, onlySubscribedCases,
									caseId, procInstIds);

			} else if (CasesRetrievalManager.CASE_LIST_TYPE_CLOSED.equals(type)) {
				caseIds = isSuperAdmin ?
							getCasesBPMDAO().getClosedCasesIdsForAdmin(statusesToShow, statusesToHide, caseId, procInstIds) :
							getCasesBPMDAO().getClosedCasesIds(user, statusesToShow, statusesToHide, groups, roles, onlySubscribedCases, caseId,
									procInstIds);

			} else if (CasesRetrievalManager.CASE_LIST_TYPE_MY.equals(type)) {
				caseIds = getCasesBPMDAO().getMyCasesIds(user, statusesToShow, statusesToHide, onlySubscribedCases, caseId, procInstIds);

			} else if (CasesRetrievalManager.CASE_LIST_TYPE_USER.equals(type)) {
				caseIds = getCasesBPMDAO().getUserCasesIds(user, statusesToShow, statusesToHide, casecodes, roles, onlySubscribedCases, caseId,
						procInstIds);

			} else if (CasesRetrievalManager.CASE_LIST_TYPE_PUBLIC.equals(type)) {
				caseIds = getCasesBPMDAO().getPublicCasesIds(statusesToShow, statusesToHide, caseCodes, caseId, procInstIds);

			} else if (CasesRetrievalManager.CASE_LIST_TYPE_HANDLER.equals(type)) {
				caseIds = getCasesBPMDAO().getHandlerCasesIds(user,
						caseStatusesToShow, caseStatusesToHide, casecodes,
						roles, onlySubscribedCases, caseId, procInstIds);

			} else {
				getLogger().warning("Unknown cases list type: '" + type + "'");
			}
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting cases", e);
			throw new RuntimeException(e);
		}

		if (caseId == null)
			putIdsToCache(caseIds, user, type, caseCodes, statusesToHide, statusesToShow, onlySubscribedCases, roles, groups, casecodes, showAllCases,
					procInstIds);
		return caseIds;
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
				roles = accessController.getAllRolesForUser(user);
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
				if (user != null)
					roles = accessController.getAllRolesForUser(user);
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
			if (user != null)
				roles = accessController.getAllRolesForUser(user);

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
			return (CaseBusiness) IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), CasesBusiness.class);
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

	protected synchronized String getPageUri(IWApplicationContext iwac, String pageType) {
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
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Didn't find any application by URL: " + pd.getName() + ", returning standard name!");
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
		Collection<Case> cases = getCasesBusiness().getCasesByIds(ids);
		return getCasesByEntities(cases, locale);
	}

	@Override
	public PagedDataCollection<CasePresentation> getCasesByEntities(Collection<Case> cases, Locale locale) {
		return new PagedDataCollection<CasePresentation>(convertToPresentationBeans(cases, locale), cases.size());
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

	private void doManageCasesCache(final Case theCase, final boolean ommitClearing) {
		Thread cacheManager = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					if (!isCacheUpdateTurnedOn())
						return;

					Map<CasesCacheCriteria, Map<Integer, Boolean>> cache = getCache();
					if (MapUtil.isEmpty(cache))
						return;

					if (theCase == null) {
						getLogger().warning("Case is undefined, unable to manage cache");
						return;
					}

					String theCaseId = theCase.getId();
					Integer caseId = Integer.valueOf(theCaseId);
					for (CasesCacheCriteria criteria: cache.keySet()) {
						if (criteria == null)
							continue;

						Map<Integer, Boolean> cachedIds = cache.get(criteria);

						if (ommitClearing && (!MapUtil.isEmpty(cachedIds) && cachedIds.containsKey(caseId)))
							//	No need to execute SQL query to verify if case ID still belongs to the cache because it is forbidden to remove ID
							continue;

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
								ids = getCaseIds(
										user,
										criteria.getType(),
										criteria.getCaseCodes(),
										criteria.getStatusesToHide(),
										criteria.getStatusesToShow(),
										criteria.isOnlySubscribedCases(),
										criteria.isShowAllCases(),
										caseId,
										criteria.getProcInstIds(),
										criteria.getRoles()
								);
							} catch (Exception e) {
								String caseInfo = "Case ID " + theCaseId + " (identifier: '" + theCase.getCaseIdentifier() + "', subject: '" +
										theCase.getSubject() + "', status: " + theCase.getCaseStatus() + ", created: " + theCase.getCreated() + ")";
								String message = "Error while verifying if modified case " + caseInfo + " belongs to the cache by key " +
										criteria.getKey() + " and user " + user;
								LOGGER.log(Level.WARNING, message, e);
								cache.clear();
								return;
							}
						}

						boolean contains = superAdmin ? true : ListUtil.isEmpty(ids) ? false : ids.contains(caseId);
						if (contains) {
							if (cachedIds == null) {
								cachedIds = new LinkedHashMap<Integer, Boolean>();
								cache.put(criteria, cachedIds);
							}
							cachedIds.put(caseId, Boolean.TRUE);
						} else if (!ommitClearing) {
							if (cachedIds != null)
								cachedIds.remove(caseId);
						}
					}
				} catch (Exception e) {
					String message = "Error updating cases list after case was modified or created: " + theCase;
					getLogger().log(Level.WARNING, message, e);
				}
			}
		});
		cacheManager.start();
	}

	@Override
	public void onApplicationEvent(final ApplicationEvent event) {
		if (event instanceof CaseModifiedEvent) {
			Object source = event.getSource();
			if (!(source instanceof Case))
				return;

			if (!isCacheUpdateTurnedOn()) {
				getCache().clear();
				return;
			}

			Case theCase = (Case) source;
			doManageCasesCache(theCase, theCase == null || StringUtil.isEmpty(theCase.getSubject()));
		} else if (event instanceof ProcessInstanceCreatedEvent) {
			if (!isCacheUpdateTurnedOn()) {
				getCache().clear();
				return;
			}

			ProcessInstanceCreatedEvent procCreatedEvent = (ProcessInstanceCreatedEvent) event;

			Long piId = procCreatedEvent.getProcessInstanceId();
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
		} else if (event instanceof CaseDeletedEvent) {
			Map<CasesCacheCriteria, Map<Integer, Boolean>> cache = getCache();
			if (cache == null)
				return;

			if (!isCacheUpdateTurnedOn()) {
				cache.clear();
				return;
			}

			Object source = event.getSource();
			if (!(source instanceof Case))
				return;

			Case theCase = (Case) source;
			Integer id = Integer.valueOf(theCase.getId());
			for (CasesCacheCriteria key: cache.keySet()) {
				Map<Integer, Boolean> ids = cache.get(key);
				if (ids != null)
					ids.remove(id);
			}
		}
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
}