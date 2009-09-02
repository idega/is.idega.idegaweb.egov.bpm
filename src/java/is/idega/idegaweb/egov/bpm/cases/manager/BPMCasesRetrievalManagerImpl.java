package is.idega.idegaweb.egov.bpm.cases.manager;

import is.idega.idegaweb.egov.application.business.ApplicationBusiness;
import is.idega.idegaweb.egov.application.data.Application;
import is.idega.idegaweb.egov.application.data.ApplicationHome;
import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessConstants;
import is.idega.idegaweb.egov.bpm.cases.bundle.ProcessBundleCasesImpl;
import is.idega.idegaweb.egov.bpm.cases.presentation.UICasesBPMAssets;
import is.idega.idegaweb.egov.bpm.cases.presentation.beans.CasesBPMAssetsState;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.CaseCategory;
import is.idega.idegaweb.egov.cases.data.GeneralCase;
import is.idega.idegaweb.egov.cases.util.CasesConstants;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.process.business.CaseBusiness;
import com.idega.block.process.business.CasesRetrievalManager;
import com.idega.block.process.business.CasesRetrievalManagerImpl;
import com.idega.block.process.data.Case;
import com.idega.block.process.data.CaseCode;
import com.idega.block.process.presentation.beans.CaseManagerState;
import com.idega.block.process.presentation.beans.CasePresentation;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
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
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.presentation.IWContext;
import com.idega.presentation.paging.PagedDataCollection;
import com.idega.presentation.text.Link;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.Group;
import com.idega.user.data.GroupBMPBean;
import com.idega.user.data.User;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
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
public class BPMCasesRetrievalManagerImpl extends CasesRetrievalManagerImpl implements CasesRetrievalManager {

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
	
	static final String beanIdentifier = "casesBPMCaseHandler";
	public static final String caseHandlerType = "CasesBPM";

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
		
	//	System.out.println("_________PROCESS INSTANCE ID++++="+piId);
		
		return getBpmContext().execute(new JbpmCallback() {

			public Object doInJbpm(JbpmContext context) throws JbpmException {
				return context.getProcessInstance(piId).getContextInstance().getVariable(CasesBPMProcessConstants.caseIdentifier);
			}
		});
	}
	
	@Override
	public Long getProcessInstanceId(Case theCase) {
		
		Integer caseId = theCase.getPrimaryKey() instanceof Integer ? (Integer) theCase.getPrimaryKey() : Integer.valueOf((theCase.getPrimaryKey().toString()));
		
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
	public Long getProcessDefinitionId(Case theCase) {
		ProcessDefinition processDefinition = getProcessDefinition(theCase);
		return processDefinition == null ? null : processDefinition.getId();
	}
	
	@Override
	public String getProcessDefinitionName(Case theCase) {
		ProcessDefinition processDefinition = getProcessDefinition(theCase);
		return processDefinition == null ? null : processDefinition.getName();
	}
	
	private ProcessDefinition getProcessDefinition(Case theCase) {
		final Long piId = getProcessInstanceId(theCase);
		if (piId == null) {
			return null;
		}
		
		return getBpmContext().execute(new JbpmCallback() {

			public Object doInJbpm(JbpmContext context) throws JbpmException {
				return context.getProcessInstance(piId).getProcessDefinition();
			}
		});
	}

	@Override
	public List<Link> getCaseLinks(Case theCase, String casesComponentType) {
		
		throw new UnsupportedOperationException("Implement with correct pages if needed");
		
	}

	@Override
	public UIComponent getView(IWContext iwc, Integer caseId, String type, String caseManagerType) {
		if (!caseHandlerType.equals(caseManagerType)) {
			return super.getView(iwc, caseId, type, caseManagerType);
		}
		
		CasesBPMAssetsState stateBean = (CasesBPMAssetsState) WFUtil.getBeanInstance(CasesBPMAssetsState.beanIdentifier);
		stateBean.setDisplayPropertyForStyleAttribute(Boolean.FALSE);
		stateBean.setStandAloneComponent(Boolean.FALSE);
		
		CaseManagerState managerState = ELUtil.getInstance().getBean(CaseManagerState.beanIdentifier);
		managerState.setFullView(!CasesRetrievalManager.CASE_LIST_TYPE_USER.equals(type));
		
		UICasesBPMAssets casesAssets = (UICasesBPMAssets)iwc.getApplication().createComponent(UICasesBPMAssets.COMPONENT_TYPE);
		UIViewRoot viewRoot = iwc.getViewRoot();
		if (viewRoot != null) {
			casesAssets.setId(viewRoot.createUniqueId());
		}
		
		casesAssets.setUsePdfDownloadColumn(stateBean.getUsePDFDownloadColumn() == null ? false : stateBean.getUsePDFDownloadColumn());
		casesAssets.setAllowPDFSigning(stateBean.getAllowPDFSigning() == null ? false : stateBean.getAllowPDFSigning());
		casesAssets.setHideEmptySection(stateBean.getHideEmptySection() == null ? false : stateBean.getHideEmptySection());
		casesAssets.setCommentsPersistenceManagerIdentifier(stateBean.getCommentsPersistenceManagerIdentifier());
		casesAssets.setShowAttachmentStatistics(stateBean.getShowAttachmentStatistics() == null ? false : stateBean.getShowAttachmentStatistics());
		casesAssets.setShowOnlyCreatorInContacts(stateBean.getShowOnlyCreatorInContacts() == null ? false : stateBean.getShowOnlyCreatorInContacts());
		
		if (caseId != null) {
			casesAssets.setCaseId(caseId);
		}
		
		return casesAssets;
	}

	@Override
	public PagedDataCollection<CasePresentation> getCases(User user, String type, Locale locale, List<String> caseCodes, List<String> caseStatusesToHide,
			List<String> caseStatusesToShow, int startIndex, int count, boolean onlySubscribedCases) {

		try {
			List<Integer> casesIds = getCaseIds(user, type, caseCodes, caseStatusesToHide, caseStatusesToShow, onlySubscribedCases);

			if (ListUtil.isEmpty(casesIds)) {
				LOGGER.info("User '" + user + "' doesn't have any cases!");
				return new PagedDataCollection<CasePresentation>(new ArrayList<CasePresentation>(), 0);
			}
			
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
			} else {
				cases = new ArrayList<Case>();
			}
			return new PagedDataCollection<CasePresentation>(convertToPresentationBeans(cases, locale), totalCount);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Some error occurred while getting cases for user: " + user + " by type: " + type + ", by locale: " + locale +
					", by case codes: " + caseCodes + ", by case statuses to hide: " + caseStatusesToHide + ", by case statuses to show: " + caseStatusesToShow +
					", start index: " + startIndex + ", count: " + count + ", only subscribed cases: " + onlySubscribedCases, e);
		}

		return new PagedDataCollection<CasePresentation>(new ArrayList<CasePresentation>(), 0);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<Integer> getCaseIds(User user, String type, List<String> caseCodes, List<String> caseStatusesToHide, List<String> caseStatusesToShow,
			boolean onlySubscribedCases) throws Exception {

		IWContext iwc = CoreUtil.getIWContext();
		IWMainApplication iwma = iwc.getIWMainApplication();

		List<Integer> caseIds = null;
		
		List<String> statusesToShow = caseStatusesToShow == null ? new ArrayList() : new ArrayList<String>(caseStatusesToShow);
		List<String> statusesToHide = caseStatusesToHide == null ? new ArrayList() : new ArrayList<String>(caseStatusesToHide);
		statusesToHide = ListUtil.getFilteredList(statusesToHide);
		
		try {
			boolean isSuperAdmin = iwc.isSuperAdmin() || iwc.hasRole(CasesConstants.ROLE_CASES_SUPER_ADMIN);

			CasesBusiness casesBusiness = (CasesBusiness) IBOLookup.getServiceInstance(iwc, CasesBusiness.class);
			UserBusiness userBusiness = (UserBusiness) IBOLookup.getServiceInstance(iwc, UserBusiness.class);

			if (CasesRetrievalManager.CASE_LIST_TYPE_OPEN.equals(type)) {

				String[] caseStatuses = casesBusiness.getStatusesForOpenCases();
				statusesToShow.addAll(Arrays.asList(caseStatuses));
				statusesToShow = ListUtil.getFilteredList(statusesToShow);
				statusesToHide.addAll(Arrays.asList(casesBusiness.getStatusesForClosedCases()));
				statusesToHide = ListUtil.getFilteredList(statusesToHide);
				
				if (isSuperAdmin) {
					caseIds = getCasesBPMDAO().getOpenCasesIdsForAdmin(caseCodes, statusesToShow, statusesToHide);
				} else {
					Set<String> roles = iwma.getAccessController().getAllRolesForUser(user);
					List<Integer> groups = null;
					Collection<GroupBMPBean> groupBeans = userBusiness.getUserGroupsDirectlyRelated(user);
					if (!ListUtil.isEmpty(groupBeans)) {
						groups = new ArrayList<Integer>(groupBeans.size());
						for (GroupBMPBean group : groupBeans) {
							groups.add(new Integer(group.getPrimaryKey().toString()));
						}
					}
					caseIds = getCasesBPMDAO().getOpenCasesIds(user, caseCodes, statusesToShow, statusesToHide, groups, roles, onlySubscribedCases);
				}
			} else if (CasesRetrievalManager.CASE_LIST_TYPE_CLOSED.equals(type)) {

				String[] caseStatuses = casesBusiness.getStatusesForClosedCases();
				statusesToShow.addAll(Arrays.asList(caseStatuses));
				statusesToShow = ListUtil.getFilteredList(statusesToShow);
				
				if (isSuperAdmin) {
					caseIds = getCasesBPMDAO().getClosedCasesIdsForAdmin(statusesToShow, statusesToHide);
				} else {
					Set<String> roles = iwma.getAccessController().getAllRolesForUser(user);
					List<Integer> groups = null;
					Collection<GroupBMPBean> groupBeans = userBusiness.getUserGroupsDirectlyRelated(user);
					if (!ListUtil.isEmpty(groupBeans)) {
						groups = new ArrayList<Integer>(groupBeans.size());
						for (GroupBMPBean group : groupBeans) {
							groups.add(new Integer(group.getPrimaryKey().toString()));
						}
					}
					caseIds = getCasesBPMDAO().getClosedCasesIds(user, statusesToShow, statusesToHide, groups, roles, onlySubscribedCases);
				}

			} else if (CasesRetrievalManager.CASE_LIST_TYPE_MY.equals(type)) {

				String[] caseStatus = casesBusiness.getStatusesForMyCases();
				statusesToShow.addAll(Arrays.asList(caseStatus));
				statusesToShow = ListUtil.getFilteredList(statusesToShow);
				
				caseIds = getCasesBPMDAO().getMyCasesIds(user, statusesToShow, statusesToHide, onlySubscribedCases);

			} else if (CasesRetrievalManager.CASE_LIST_TYPE_USER.equals(type)) {
				
				statusesToShow = ListUtil.getFilteredList(statusesToShow);
				CaseCode[] casecodes = getCaseBusiness().getCaseCodesForUserCasesList();
				Set<String> roles = iwma.getAccessController().getAllRolesForUser(user);

				List<String> codes = new ArrayList<String>(casecodes.length);
				for (CaseCode code : casecodes) {
					codes.add(code.getCode());
				}
				caseIds = getCasesBPMDAO().getUserCasesIds(user, statusesToShow, statusesToHide, codes, roles, onlySubscribedCases);
				
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return caseIds;
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
	
	public UserBusiness getUserBusiness(IWContext iwc) {
		
		try {
			return (UserBusiness)IBOLookup.getServiceInstance(iwc, UserBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
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
		
		if(uris.containsKey(pageType))
			return uris.get(pageType);
		
		Collection<ICPage> icpages = getPages(pageType);
		
		ICPage icPage = null;
		
		if(icpages == null || icpages.isEmpty()) {
			
//			TODO: create egov bpm page, as not found
			throw new RuntimeException("No page found by page type: "+pageType);			
		}
		
		if(icPage == null)
			icPage = icpages.iterator().next();
		
		String uri = icPage.getDefaultPageURI();
		
		if(!uri.startsWith("/pages"))
			uri = "/pages"+uri;
		
		String ruri = iwac.getIWMainApplication().getTranslatedURIWithContext(uri);
		uris.put(pageType, ruri);
		
		return ruri;
	}
	
	protected Collection<ICPage> getPages(String pageSubType) {
		
		try {
			ICPageHome home = (ICPageHome) IDOLookup.getHome(ICPage.class);
			@SuppressWarnings("unchecked")
			Collection<ICPage> icpages = home.findBySubType(pageSubType, false);
			
			return icpages;
			
		} catch (Exception e) {
			throw new RuntimeException("Exception while resolving icpages by subType: "+pageSubType, e);
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
		if (ListUtil.isEmpty(casesProcesses)) {
			return null;
		}
		
		return getBpmContext().execute(new JbpmCallback() {

			public Object doInJbpm(JbpmContext context) throws JbpmException {
				
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
		
		ApplicationBusiness applicationBusiness = null;
		try {
			applicationBusiness = (ApplicationBusiness) IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(),
																															ApplicationBusiness.class);
		} catch (IBOLookupException e) {
			e.printStackTrace();
		}
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
		if (!StringUtil.isEmpty(caseId)) {
			return caseId;
		}
		
		String processInstanceId = iwc.getParameter(ProcessManagerBind.processInstanceIdParam);
		if (StringUtil.isEmpty(processInstanceId)) {
			return null;
		}
		
		try {
			return getCaseId(Long.valueOf(processInstanceId));
		} catch(NumberFormatException e) {
			e.printStackTrace();
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
	
}