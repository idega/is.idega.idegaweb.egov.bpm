package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import javax.servlet.ServletContext;

import org.jdom2.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.idega.block.process.business.CaseBusiness;
import com.idega.block.process.business.CaseManagersProvider;
import com.idega.block.process.business.CasesRetrievalManager;
import com.idega.block.process.business.ProcessConstants;
import com.idega.block.process.business.file.CaseAttachment;
import com.idega.block.process.business.pdf.CaseConverterToPDF;
import com.idega.block.process.business.pdf.CasePDF;
import com.idega.block.process.data.Case;
import com.idega.block.process.data.CaseStatus;
import com.idega.block.process.event.CaseModifiedEvent;
import com.idega.block.process.presentation.CaseBlock;
import com.idega.block.process.presentation.UICasesList;
import com.idega.block.process.presentation.UserCases;
import com.idega.block.process.presentation.beans.CaseComparator;
import com.idega.block.process.presentation.beans.CaseListPropertiesBean;
import com.idega.block.process.presentation.beans.CasePresentation;
import com.idega.block.process.presentation.beans.CasesSearchCriteriaBean;
import com.idega.block.process.presentation.beans.CasesSearchResults;
import com.idega.block.process.presentation.beans.CasesSearchResultsHolder;
import com.idega.block.process.presentation.beans.GeneralCasesListBuilder;
import com.idega.block.web2.business.JQuery;
import com.idega.block.web2.business.JQueryUIType;
import com.idega.block.web2.business.Web2Business;
import com.idega.bpm.bean.CasesBPMAssetProperties;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.builder.business.AdvancedPropertyComparator;
import com.idega.builder.business.BuilderLogic;
import com.idega.builder.business.BuilderLogicWrapper;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.core.accesscontrol.bean.UserHasLoggedInEvent;
import com.idega.core.builder.data.ICPage;
import com.idega.core.business.DefaultSpringBean;
import com.idega.core.component.bean.RenderedComponent;
import com.idega.core.component.business.ICObjectBusiness;
import com.idega.core.component.data.ICObjectInstance;
import com.idega.core.component.data.ICObjectInstanceHome;
import com.idega.data.IDOLookup;
import com.idega.data.IDOLookupException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.idegaweb.RepositoryStartedEvent;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.idegaweb.egov.bpm.presentation.CasesExporter;
import com.idega.io.DownloadWriter;
import com.idega.io.MediaWritable;
import com.idega.jbpm.bean.BPMProcessVariable;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.bean.VariableInstanceType;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.jbpm.presentation.BPMTaskViewer;
import com.idega.jbpm.utils.JBPMConstants;
import com.idega.jbpm.variables.MultipleSelectionVariablesResolver;
import com.idega.jbpm.variables.VariablesPreloader;
import com.idega.presentation.IWContext;
import com.idega.presentation.ListNavigator;
import com.idega.presentation.paging.PagedDataCollection;
import com.idega.presentation.ui.handlers.IWDatePickerHandler;
import com.idega.user.business.GroupBusiness;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.FileUtil;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.URIUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.expression.ELUtil;
import com.idega.webface.WFUtil;

import is.idega.idegaweb.egov.application.business.ApplicationBusiness;
import is.idega.idegaweb.egov.application.business.ApplicationType;
import is.idega.idegaweb.egov.application.data.Application;
import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.business.CaseListVariableCache;
import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessView;
import is.idega.idegaweb.egov.bpm.cases.actionhandlers.CaseHandlerAssignmentHandler;
import is.idega.idegaweb.egov.bpm.cases.exe.CasesBPMProcessDefinitionW;
import is.idega.idegaweb.egov.bpm.cases.presentation.UIProcessVariables;
import is.idega.idegaweb.egov.bpm.cases.search.CasesListSearchCriteriaBean;
import is.idega.idegaweb.egov.bpm.cases.search.CasesListSearchFilter;
import is.idega.idegaweb.egov.bpm.cases.search.impl.DefaultCasesListSearchFilter;
import is.idega.idegaweb.egov.bpm.media.CasesSearchResultsExporter;
import is.idega.idegaweb.egov.cases.bean.CasesExportParams;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.business.TSOCManager;
import is.idega.idegaweb.egov.cases.business.TimeSpentOnCaseManager;
import is.idega.idegaweb.egov.cases.presentation.ClosedCases;
import is.idega.idegaweb.egov.cases.presentation.MyCases;
import is.idega.idegaweb.egov.cases.presentation.OpenCases;
import is.idega.idegaweb.egov.cases.presentation.PublicCases;
import is.idega.idegaweb.egov.cases.util.CasesConstants;

@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service("casesEngineDWR")
public class CasesEngineImp extends DefaultSpringBean implements BPMCasesEngine, ApplicationListener<ApplicationEvent> {

	@Autowired
	private CasesBPMProcessView casesBPMProcessView;

	@Autowired
	private BuilderLogicWrapper builderLogic;

	@Autowired
	private GeneralCasesListBuilder casesListBuilder;

	private CasesRetrievalManager casesRetrievalManager;

	@Autowired
	private CaseManagersProvider caseManagersProvider;

	@Autowired
	private Web2Business web2;

	@Autowired
	private JQuery jQuery;

	@Autowired
	private CasesBPMDAO casesBPMDAO;

	@Autowired
	private CaseConverterToPDF caseConverter;

	@Autowired
	private VariableInstanceQuerier variableInstanceQuerier;

	public static final String	FILE_DOWNLOAD_LINK_STYLE_CLASS = "casesBPMAttachmentDownloader",
								PDF_GENERATOR_AND_DOWNLOAD_LINK_STYLE_CLASS = "casesBPMPDFGeneratorAndDownloader",
								DOWNLOAD_TASK_IN_PDF_LINK_STYLE_CLASS = "casesBPMDownloadTaskInPDF",
								CASE_LOGS_PDF_DOWNLOAD_LINK_STYLE_CLASS = "caselogspdfdownload";

	private static final Logger LOGGER = Logger.getLogger(CasesEngineImp.class.getName());

	public static final String CASE_DATA_CACHE_NAME = "caseDataCache";
	public static final String VARIABLE_INFO_CACHE_NAME = "variableInfoCache";
	public static final String CASE_PROC_ID_BIND = "caseProcInstIdBind";
	public static final String STATUS_MAP_CACHE = "statusMapCache";
	protected CasesRetrievalManager getCasesRetrievalManager() {
		if (this.casesRetrievalManager == null) {
			this.casesRetrievalManager = getCaseManagersProvider().getCaseManager();
		}

		return this.casesRetrievalManager;
	}

	@Override
	public Long getProcessInstanceId(String caseId) {
		if (caseId == null) {
			LOGGER.warning("Case ID is not provided!");
			return null;
		}

		Long processInstanceId = getCasesBPMProcessView().getProcessInstanceId(caseId);
		return processInstanceId;
	}

	@Override
	public RenderedComponent getRenderedCaseTaskView(Long taskId, List<AdvancedProperty> properties) {
		if (taskId == null) {
			return null;
		}

		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}

		RenderedComponent taskView = null;
		try {
			BPMTaskViewer taskViewer =  (BPMTaskViewer) iwc.getApplication().createComponent(BPMTaskViewer.COMPONENT_TYPE);
			taskViewer.setTaskInstanceId(taskId);
			UIViewRoot viewRoot = iwc.getViewRoot();
			if (viewRoot != null) {
				taskViewer.setId(viewRoot.createUniqueId());
			}
			taskView = getBuilderLogic().getBuilderService(iwc).getRenderedComponent(taskViewer, properties);
			return taskView;
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception while rendering component for task (ID: " + taskId + ") view", e);
		}

		return null;
	}

	@Override
	public Document getCaseTaskView(Long taskId) {
		if (taskId == null) {
			return null;
		}

		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}

		Document taskView = null;
		try {
			BPMTaskViewer taskViewer =  (BPMTaskViewer) iwc.getApplication().createComponent(BPMTaskViewer.COMPONENT_TYPE);
			taskViewer.setTaskInstanceId(taskId);
			UIViewRoot viewRoot = iwc.getViewRoot();
			if (viewRoot != null) {
				taskViewer.setId(viewRoot.createUniqueId());
			}
			taskView = getBuilderLogic().getBuilderService(iwc).getRenderedComponent(iwc, taskViewer, true);
			return taskView;
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception while rendering component for task (ID: " + taskId + ") view", e);
		}

		return null;
	}

	@Override
	public Document getCaseManagerView(CasesBPMAssetProperties properties) {
		if (properties == null) {
			return null;
		}

		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}

		String caseIdStr = properties.getCaseId();
		if (caseIdStr == null || CoreConstants.EMPTY.equals(caseIdStr) || iwc == null) {
			LOGGER.log(Level.WARNING, "Either not provided:\n caseId="+caseIdStr+", iwc="+iwc);
			return null;
		}

		Document caseView = null;
		try {
			CasesBPMAssetsState stateBean = WFUtil.getBeanInstance(CasesBPMAssetsState.beanIdentifier);
			if (stateBean != null) {
				stateBean.setUsePDFDownloadColumn(properties.isUsePDFDownloadColumn());
				stateBean.setAllowPDFSigning(properties.isAllowPDFSigning());
				stateBean.setHideEmptySection(properties.isHideEmptySection());
				stateBean.setCommentsPersistenceManagerIdentifier(properties.getCommentsPersistenceManagerIdentifier());
				stateBean.setShowAttachmentStatistics(properties.isShowAttachmentStatistics());
				stateBean.setShowOnlyCreatorInContacts(properties.isShowOnlyCreatorInContacts());
				stateBean.setAutoShowComments(properties.isAutoShowComments());
				stateBean.setShowLogExportButton(properties.isShowLogExportButton());
				stateBean.setShowComments(properties.isShowComments());
				stateBean.setShowContacts(properties.isShowContacts());
				stateBean.setSpecialBackPage(properties.getSpecialBackPage());
				stateBean.setNameFromExternalEntity(properties.isNameFromExternalEntity());
				stateBean.setShowUserProfilePicture(properties.isShowUserProfilePicture());
				stateBean.setAddExportContacts(properties.isAddExportContacts());
				stateBean.setShowUserCompany(properties.isShowUserCompany());
				stateBean.setShowLastLoginDate(properties.isShowLastLoginDate());
				stateBean.setInactiveTasksToShow(properties.getInactiveTasksToShow());
				stateBean.setCustomView(properties.getCustomView());
			}

			Integer caseId = new Integer(caseIdStr);
			UIComponent caseViewUI = getCasesBPMProcessView().getCaseManagerView(iwc, null, caseId, properties.getProcessorType());

			caseView = getBuilderLogic().getBuilderService(iwc).getRenderedComponent(iwc, caseViewUI, true);
			return caseView;
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception while rendering component for case (ID: " + caseIdStr + ") view", e);
		}

		return null;
	}

	@Override
	public boolean setCaseSubject(String caseId, String subject) {
		if (caseId == null || subject == null) {
			return false;
		}

		CaseBusiness caseBusiness = null;
		try {
			caseBusiness = IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), CaseBusiness.class);
		} catch (IBOLookupException e) {
			LOGGER.log(Level.SEVERE, "Error getting CaseBusiness", e);
		}
		if (caseBusiness == null) {
			return false;
		}

		Case theCase = null;
		try {
			theCase = caseBusiness.getCase(caseId);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Unable to get case by ID: " + caseId, e);
		}
		if (theCase == null) {
			return false;
		}

		theCase.setSubject(subject);
		theCase.store();

		return true;
	}

	@Override
	public Document getRenderedCasesByQuery(CasesSearchCriteriaBean criterias) {
		if (criterias instanceof CasesListSearchCriteriaBean) {
			return getCasesListByUserQuery((CasesListSearchCriteriaBean) criterias);
		}

		getLogger().warning("Criterias " + criterias + " are not instance of " + CasesListSearchCriteriaBean.class + " (actual implementation: " +
				criterias.getClass().getName() + "), returning null");
		return null;
	}

	@Override
	public Document getCasesListByUserQuery(CasesListSearchCriteriaBean criterias) {
		if (criterias == null) {
			LOGGER.log(Level.SEVERE, "Can not execute search - search criterias unknown");
			return null;
		}

		criterias.setSearch(true);
		if (criterias.isClearResults()) {
			//	Clearing search result before new search
			clearSearchResults(criterias.getId());
		}

		List<Long> piIds = criterias.getProcInstIds();
		LOGGER.log(Level.INFO, new StringBuilder("Search query: caseNumber: ").append(criterias.getCaseNumber()).append(", description: ")
				.append(criterias.getDescription()).append(", name: ").append(criterias.getName()).append(", personalId: ")
				.append(criterias.getPersonalId()).append(", processId: ").append(criterias.getProcessId()).append(", statusId: ")
				.append(criterias.getStatusId()).append(", dateRange: ").append(criterias.getDateRange()).append(", casesListType: ")
				.append(criterias.getCaseListType()).append(", contact: ").append(criterias.getContact()).append(", variables provided: ")
				.append(ListUtil.isEmpty(criterias.getProcessVariables()) ? "none" : criterias.getProcessVariables())
				.append(", address: ").append(criterias.getAddress())
				.append(", process instance IDs: ").append(ListUtil.isEmpty(piIds) ? "none" :
					piIds.size() >= 1000 ? new ArrayList<Long>(piIds).subList(0, 999) + " ..." : piIds)
		.toString());

		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			LOGGER.warning(IWContext.class + " is unavailable!");
			return null;
		}

		addSearchQueryToSession(iwc, criterias);

		PagedDataCollection<CasePresentation> cases = null;
		if (criterias.isNothingFound()) {
			Collection<CasePresentation> emptyList = Collections.emptyList();
			cases = new PagedDataCollection<CasePresentation>(emptyList);
		} else {
			cases = getCasesByQuery(iwc, criterias);
			if (criterias.isClearResults()) {
				if (cases == null) {
					Collection<CasePresentation> externalData = getExternalSearchResults(getSearchResultsHolder(), criterias.getId());
					if (externalData != null) {
						setSearchResults(iwc, externalData, criterias);
					}

				} else {
					setSearchResults(iwc, cases.getCollection(), criterias);
				}
			}
		}

		CaseListPropertiesBean properties = new CaseListPropertiesBean();
		properties.setType(ProcessConstants.CASE_LIST_TYPE_SEARCH_RESULTS);
		properties.setUsePDFDownloadColumn(criterias.isUsePDFDownloadColumn());
		properties.setAllowPDFSigning(criterias.isAllowPDFSigning());
		properties.setShowStatistics(criterias.isShowStatistics());
		properties.setHideEmptySection(criterias.isHideEmptySection());
		properties.setPageSize(criterias.getPageSize());
		properties.setPage(criterias.getPage());
		properties.setInstanceId(criterias.getInstanceId());
		properties.setShowCaseNumberColumn(criterias.isShowCaseNumberColumn());
		properties.setShowCreationTimeInDateColumn(criterias.isShowCreationTimeInDateColumn());
		properties.setCaseCodes(criterias.getCaseCodesInList());
		properties.setStatusesToShow(criterias.getStatusesToShowInList());
		properties.setStatusesToHide(criterias.getStatusesToHideInList());
		properties.setOnlySubscribedCases(criterias.isOnlySubscribedCases());
		properties.setComponentId(criterias.getComponentId());
		properties.setCriteriasId(criterias.getCriteriasId());
		properties.setFoundResults(criterias.getFoundResults());
		properties.setCasesListCustomizer(criterias.getCasesListCustomizer());
		properties.setCustomColumns(criterias.getCustomColumns());
		properties.setShowLoadingMessage(criterias.isShowLoadingMessage());
		properties.setSubscribersGroupId(criterias.getSubscribersGroupId());
		properties.setShowUserProfilePicture(criterias.isShowUserProfilePicture());
		properties.setShowAttachmentStatistics(criterias.isShowAttachmentStatistics());
		properties.setShowUserCompany(criterias.isShowUserCompany());
		properties.setAddExportContacts(criterias.isAddExportContacts());
		properties.setSearch(criterias.isSearch());
		properties.setShowContacts(criterias.isShowContacts());
		properties.setCustomColumnsForExport(criterias.getExportColumns());
		properties.setCustomView(criterias.getCustomView());
		properties.setShowTimeSpentOnCase(criterias.isShowTimeSpentOnCase());
		properties.setShowCaseSubstatus(criterias.isShowCaseSubstatus());
		UIComponent component = null;
		if (CasesRetrievalManager.CASE_LIST_TYPE_USER.equals(criterias.getCaseListType())) {
			properties.setAddCredentialsToExernalUrls(false);
			component = getCasesListBuilder().getUserCasesList(iwc, cases, null, properties);
		} else {
			properties.setShowCheckBoxes(false);
			component = getCasesListBuilder().getCasesList(iwc, cases, properties);
		}
		if (component == null) {
			LOGGER.warning("Unable to get UIComponent for cases list!");
			return null;
		}

		return getBuilderLogic().getBuilderService(iwc).getRenderedComponent(iwc, component, true);
	}

	private IWResourceBundle getResourceBundle(IWContext iwc) {
		return iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc);
	}

	protected String getSelectedGroup(Long id) {
		if (id == null) {
			return null;
		}

		try {
			Group group = IDOLookup.findByPrimaryKey(
					Group.class,
					id.intValue());
			if (group != null) {
				return group.getName();
			}
		} catch (IDOLookupException e) {
			getLogger().log(
					Level.WARNING,
					"Failed to get data access object, casue of: ", e);
		} catch (FinderException e) {
			getLogger().log(
					Level.WARNING,
					"Failed to find group by id: " + id,
					e);
		}

		return null;
	}

	@Override
	public void addSearchQueryToSession(IWContext iwc, CasesListSearchCriteriaBean criterias) {
		List<AdvancedProperty> searchFields = new ArrayList<AdvancedProperty>();

		Locale locale = iwc.getCurrentLocale();
		IWResourceBundle iwrb = getResourceBundle(iwc);

		if (!StringUtil.isEmpty(criterias.getCaseNumber())) {
			searchFields.add(new AdvancedProperty("case_nr", criterias.getCaseNumber()));
		}
		if (!StringUtil.isEmpty(criterias.getDescription())) {
			searchFields.add(new AdvancedProperty("description", criterias.getDescription()));
		}
		if (!StringUtil.isEmpty(criterias.getName())) {
			searchFields.add(new AdvancedProperty("name", criterias.getName()));
		}
		if (!StringUtil.isEmpty(criterias.getPersonalId())) {
			searchFields.add(new AdvancedProperty("personal_id", criterias.getPersonalId()));
		}
		if (!StringUtil.isEmpty(criterias.getContact())) {
			searchFields.add(new AdvancedProperty("contact", criterias.getContact()));
		}
		if (!StringUtil.isEmpty(criterias.getAddress())) {
			searchFields.add(new AdvancedProperty("address", criterias.getAddress()));
		}

		if (criterias.getSubscribersGroupId() != null) {
			searchFields.add(new AdvancedProperty("handler_category_id", getSelectedGroup(criterias.getSubscribersGroupId())));
		}

		if (!StringUtil.isEmpty(criterias.getProcessId())) {
			String processName = null;
			try {
				ProcessDefinitionW bpmCasesManager = ELUtil.getInstance().getBean(CasesBPMProcessDefinitionW.SPRING_BEAN_IDENTIFIER);
				bpmCasesManager.setProcessDefinitionId(Long.valueOf(criterias.getProcessId()));
				processName = bpmCasesManager.getProcessName(locale);
			} catch(Exception e) {
				LOGGER.log(Level.WARNING, "Error getting process name by: " + criterias.getProcessId());
			}
			searchFields.add(new AdvancedProperty("cases_search_select_process", StringUtil.isEmpty(processName) ? "general_cases" : processName));
		}
		if (!StringUtil.isEmpty(criterias.getStatusId())) {
			String status = null;
			try {
				status = getCasesBusiness(iwc).getLocalizedCaseStatusDescription(
						null,
						getCasesBusiness(iwc).getCaseStatus(criterias.getStatusId()),
						locale
				);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error getting status name by: " + criterias.getStatusId(), e);
			}
			searchFields.add(new AdvancedProperty("status", StringUtil.isEmpty(status) ?
					iwrb.getLocalizedString("unknown_status", "Unknown") :
					status)
			);
		}
		if (!StringUtil.isEmpty(criterias.getDateRange())) {
			searchFields.add(new AdvancedProperty("date_range", criterias.getDateRange()));
		}
		if (!ListUtil.isEmpty(criterias.getProcessVariables())) {
			for (BPMProcessVariable variable: criterias.getProcessVariables()) {
				String value = variable.getValue();
				String name = variable.getName();
				if (CaseHandlerAssignmentHandler.handlerUserIdVarName.equals(name) || CaseHandlerAssignmentHandler.performerUserIdVarName.equals(name)
						|| name.startsWith(VariableInstanceType.OBJ_LIST.getPrefix()) || name.startsWith(VariableInstanceType.LIST.getPrefix())
						|| !StringUtil.isEmpty(isResolverExist(MultipleSelectionVariablesResolver.BEAN_NAME_PREFIX + variable.getName()))
						) {
					MultipleSelectionVariablesResolver resolver = null;
					try {
						resolver = ELUtil.getInstance().getBean(MultipleSelectionVariablesResolver.BEAN_NAME_PREFIX + variable.getName());
					} catch (Throwable e) {}
					if (resolver != null) {
						value = resolver.getPresentation(variable);
					}
				}

				searchFields.add(new AdvancedProperty(
						iwrb.getLocalizedString(JBPMConstants.VARIABLE_LOCALIZATION_PREFIX.concat(variable.getName()), variable.getName()), value)
				);
			}
		}
		iwc.setSessionAttribute(GeneralCasesListBuilder.USER_CASES_SEARCH_QUERY_BEAN_ATTRIBUTE, searchFields);
	}

	private List<CasesListSearchFilter> getFilters(ServletContext servletContext, CasesSearchCriteriaBean criterias) {
		List<CasesListSearchFilter> filtersList = new ArrayList<CasesListSearchFilter>();

		try {
			WebApplicationContext webAppContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);
			Map<?, ?> filters = webAppContext.getBeansOfType(CasesListSearchFilter.class);
			if (filters == null || filters.isEmpty()) {
				return filtersList;
			}

			for (Object filterObject: filters.values()) {
				CasesListSearchFilter filter = (CasesListSearchFilter) filterObject;
				filter.setCriterias(criterias);
				filtersList.add(filter);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return filtersList;
	}

	@Override
	public PagedDataCollection<CasePresentation> getCasesByQuery(CasesSearchCriteriaBean criterias) {
		if (criterias instanceof CasesListSearchCriteriaBean) {
			return getCasesByQuery(CoreUtil.getIWContext(), (CasesListSearchCriteriaBean) criterias);
		}

		getLogger().warning("Unable to get cases by query " + criterias + " because it is not instance of " +
				CasesListSearchCriteriaBean.class.getName() + ". Actual implementation: " + criterias.getClass().getName());
		return null;
	}

	private boolean isPagingTurnedOn() {
		return IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean(UICasesList.DYNAMIC_CASES_NAVIGATOR, Boolean.TRUE);
	}

	protected List<Long> convertTo(Collection<String> collection) {
		if (ListUtil.isEmpty(collection)) {
			return Collections.emptyList();
		}

		List<Long> subscribedGroupsIds = new ArrayList<Long>();
		for (String id : collection) {
			subscribedGroupsIds.add(Long.valueOf(id));
		}

		return subscribedGroupsIds;
	}

	private PagedDataCollection<CasePresentation> getCasesByQuery(
			IWContext iwc,
			CasesListSearchCriteriaBean criterias
	) {
		User currentUser = null;
		if (!iwc.isLoggedOn() || (currentUser = iwc.getCurrentUser()) == null) {
			LOGGER.info("Not logged in, skipping searching");
			return null;
		}

		long start = System.currentTimeMillis();
		String casesProcessorType = criterias.getCaseListType() == null ?
				CasesRetrievalManager.CASE_LIST_TYPE_OPEN :
				criterias.getCaseListType();

		Collection<Long> handlerCategoryIDs = null;
		Set<Integer> casesIDs = new HashSet<>();
		try {
			if (criterias.isShowAllCases()) {
				CasesBusiness casesBusiness = getCasesBusiness(iwc);
				Collection<CaseStatus> statuses = casesBusiness.getCaseStatuses();
				StringBuffer allStatuses = null;
				if (!ListUtil.isEmpty(statuses)) {
					allStatuses = new StringBuffer();
					for (Iterator<CaseStatus> statusesIter = statuses.iterator(); statusesIter.hasNext();) {
						allStatuses.append(statusesIter.next().getStatus());
						if (statusesIter.hasNext()) {
							allStatuses.append(CoreConstants.COMMA);
						}
					}
				}

				criterias.setStatusesToShow(allStatuses == null ? null : allStatuses.toString());
				criterias.setStatusesToHide(null);
			}

			handlerCategoryIDs = criterias.getSubscribersGroupId() == null ? null : Arrays.asList(criterias.getSubscribersGroupId());

			List<CasesRetrievalManager> retrievalManagers = getCaseManagersProvider().getCaseManagers();
			if (ListUtil.isEmpty(retrievalManagers)) {
				return null;
			}
			final User user = currentUser;
			final Collection<Long> handlerCategoryIds = handlerCategoryIDs;
			retrievalManagers.parallelStream().forEach(retrievalManager -> {
				try {
					List<Integer> ids = retrievalManager.getCasePrimaryKeys(
							user,
							casesProcessorType,
							criterias.getCaseCodesInList(),
							criterias.getStatusesToHideInList(),
							criterias.getStatusesToShowInList(),
							criterias.isOnlySubscribedCases(),
							criterias.isShowAllCases(),
							criterias.getProcInstIds(),
							criterias.getRoles(),
							handlerCategoryIds,
							true
					);
					if (!ListUtil.isEmpty(ids)) {
						casesIDs.addAll(ids);
					}
				} catch (Exception e) {
					getLogger().log(Level.WARNING, "Unable to get casess IDs using retrieval manager " + retrievalManager.getClass().getName(), e);
				}
			});
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Some error occured getting cases by criterias: " + criterias, e);
		}
		if (ListUtil.isEmpty(casesIDs)) {
			LOGGER.warning("No primary keys for cases found, terminating search. Parameters: current user: " + currentUser + ", cases processor type: " + casesProcessorType +
					", case codes: " + criterias.getCaseCodesInList() + ", statuses to hide: " + criterias.getStatusesToHideInList() + ", statuses to show: " + criterias.getStatusesToShowInList() +
					", only subscribed cases: " + criterias.isOnlySubscribedCases() + ", show all cases: " + criterias.isShowAllCases() + ", proc. inst. IDs: " + criterias.getProcInstIds() +
					", roles: " + criterias.getRoles() + ", handler category IDs: " + handlerCategoryIDs + ", search query: " + true);
			return null;
		}

		List<Integer> casesIds = new ArrayList<>(casesIDs);

		long end = System.currentTimeMillis();
		LOGGER.info("Cases IDs were resolved in " + (end - start) + " ms");

		if (!StringUtil.isEmpty(criterias.getStatusId())) {
			criterias.setStatuses(new String[] {criterias.getStatusId()});
		}

		if (ListUtil.isEmpty(criterias.getProcInstIds())) {
			//	Filtering out the initial set of cases IDs by filters
			List<CasesListSearchFilter> filters = getFilters(iwc.getServletContext(), criterias);
			if (ListUtil.isEmpty(filters)) {
				return null;
			}

			for (CasesListSearchFilter filter: filters) {
				casesIds = filter.doFilter(casesIds);
			}
		} else {
			//	Selecting cases IDs by provided process instance IDs
			List<Integer> casesIdsByProcInstIds = casesBPMDAO.getCasesIdsByProcInstIds(criterias.getProcInstIds());
			if (ListUtil.isEmpty(casesIdsByProcInstIds)) {
				return null;
			}

			//	Making sure user will see cases that are available to her/him only
			casesIds = DefaultCasesListSearchFilter.getNarrowedResults(casesIds, casesIdsByProcInstIds);
			getLogger().info("Found cases IDs (" + casesIds.size() + ": " + casesIds + ") by search and after narrowed results");
		}
		start = System.currentTimeMillis();
		LOGGER.info("Searh was executed in " + (start - end) + " ms");

		if (ListUtil.isEmpty(casesIds)) {
			return null;
		}

		boolean usePaging = isPagingTurnedOn();
		boolean noSortingOptions = ListUtil.isEmpty(criterias.getSortingOptions());
		if (usePaging && !criterias.isNoOrdering()) {
			Comparator<Integer> c = new Comparator<Integer>() {
				@Override
				public int compare(Integer o1, Integer o2) {
					return -o1.compareTo(o2);
				}
			};
			getLogger().info("Sorting cases by IDs descending");
			Collections.sort(casesIds, c);
		}

		int totalCount = casesIds.size();
		criterias.setFoundResults(totalCount);
		if (criterias.getPageSize() <= 0) {
			criterias.setPageSize(20);
		}
		if (criterias.getPage() <= 0) {
			criterias.setPage(1);
		}
		criterias.setAllDataLoaded(!(totalCount > criterias.getPageSize()));
		int count = criterias.getPageSize();
		int startIndex = (criterias.getPage() - 1) * count;
		boolean preLoadCases = false;
		boolean preLoadStatus = false;

		Locale locale = iwc.getCurrentLocale();
		PagedDataCollection<CasePresentation> cases = null;
		Map<Long, Integer> procInstCase = null;
		if (!ListUtil.isEmpty(casesIds)) {
			if (usePaging && noSortingOptions) {
				//	No need to load all the cases, just for one page
				casesIds = getSubList(casesIds, startIndex, count, totalCount);
				cases = getCasesRetrievalManager().getCasesByIds(casesIds, locale);
			} else if (usePaging) {

				List<String> variablesToLoad = new ArrayList<String>();
				List<String> defaultVariables = new ArrayList<String>();
				for (Method method : Case.class.getMethods()){
					defaultVariables.add(method.getName());
				}

				for (AdvancedProperty sortingOption: criterias.getSortingOptions()) {
					String variableName = sortingOption.getId();
					VariablesPreloader preloader = null;
					if (defaultVariables.contains(variableName)){
						preLoadCases = true;
					} else if ("getCaseStatusLocalized".equals(variableName)) {
						preLoadStatus = true;
						preLoadCases = true;
					}
					else {
						try {
							preloader = ELUtil.getInstance().getBean(VariablesPreloader.BEAN_NAME_PREFIX + variableName);
						} catch (Exception e) {}
						if (preloader != null){
							preloader.preloadForCaseIds(casesIds);
						}
						variablesToLoad.add(variableName);
					}
				}

				try {
					procInstCase = casesBPMDAO.getProcessInstancesAndCasesIdsByCasesIds(casesIds);
				} catch (Exception e) {}

				List<Integer> caseIds = new ArrayList<Integer>();
				Map<Integer, Long> caseProcInst = new HashMap<>();
				for (Long key : procInstCase.keySet()){
					caseProcInst.put(procInstCase.get(key), key);
					caseIds.add(procInstCase.get(key));
				}

				getCasesListVariableCache().addCache(CASE_PROC_ID_BIND, caseProcInst);

				if (preLoadCases) {
					preLoadCases(caseProcInst);
				}

				if (preLoadStatus){
					preLoadStatuses();
				}

				if (!ListUtil.isEmpty(variablesToLoad)) {
					preLoadVariablesForCases(procInstCase, variablesToLoad);
				}

				List<Integer> sortedIds = new ArrayList<Integer>(caseIds);

				if (!criterias.isNoOrdering()) {
					CaseComparator comparator = getCaseComparator(criterias, locale);
					getLogger().info("Sorting cases by comparator: " + comparator + " and sorting options: " + criterias.getSortingOptions());
					Collections.sort(sortedIds, comparator);
				}

				if (usePaging) {
					sortedIds = getSubList(sortedIds, startIndex, count, totalCount);
				}

				cases = getCasesRetrievalManager().getCasesByIds(sortedIds, locale);
				if (!criterias.isNoOrdering()) {
					List<CasePresentation> casesTmp = new ArrayList<CasePresentation>();
					for (Integer id: sortedIds){
						for (CasePresentation cp : cases.getCollection()){
							if (id.equals(cp.getPrimaryKey())) {
								casesTmp.add(cp);
								cases.getCollection().remove(cp);
								break;
							}
						}
					}
					cases = new PagedDataCollection<CasePresentation>(casesTmp);
				}
			} else {
				cases = getCasesRetrievalManager().getCasesByIds(casesIds, locale);
			}
		}
		if (cases == null || ListUtil.isEmpty(cases.getCollection())) {
			return null;
		}

		cases.setTotalCount(new Long(totalCount));
		cases.setPageSize(criterias.getPageSize());
		cases.setPage(criterias.getPage());
		LOGGER.info("Sorting and paging was executed in " + (System.currentTimeMillis() - start) + " ms");
		return cases;
	}

	private void preLoadStatuses() {
		try {
			List<String> statuses = getCasesBusiness(getIWApplicationContext()).getAllCasesStatuses();
			Map<String, String> statusMap = new HashMap<>();
			for (String statusKey : statuses){
				String key = ProcessConstants.CASE_STATUS_KEY + CoreConstants.DOT + statusKey;
				IWResourceBundle iwrb = getIWApplicationContext().getIWMainApplication().getBundle("is.idega.idegaweb.egov.cases").getResourceBundle(IWContext.getCurrentInstance().getCurrentLocale());
				statusMap.put(statusKey, iwrb.getLocalizedString(key, statusKey));
			}
			getCasesListVariableCache().addCache(STATUS_MAP_CACHE, statusMap);
		} catch (Exception e) {
		}

	}

	private void preLoadVariablesForCases(Map<Long, Integer> procInstCase, List<String> variables) {
		Map<Long, Map<String, VariableInstanceInfo>> vars = getVariableInstanceQuerier().getVariablesByNamesAndValuesAndExpressionsByProcesses(
				null,
				variables,
				null,
				new ArrayList<>(procInstCase.keySet()),
				null
		);
		getCasesListVariableCache().addCache(VARIABLE_INFO_CACHE_NAME, vars);
	}

	private void preLoadCases(Map<Integer, Long> caseProcInst) {
		Collection<Case> cases = getCasesBusiness(getIWApplicationContext()).getCasesByIds(caseProcInst.keySet());
		Map<Integer, Case> caseProc = new HashMap<Integer, Case>();
		for (Case cs : cases){
			caseProc.put(Integer.parseInt(cs.getId()), cs);
		}
		getCasesListVariableCache().addCache(CASE_DATA_CACHE_NAME, caseProc);
	}

	private <T> List<T> getSubList(List<T> list, int startIndex, int count, int totalCount) {
		if (startIndex + count < totalCount) {
			return list.subList(startIndex, (startIndex + count));
		} else {
			return list.subList(startIndex, totalCount);
		}
	}

	private CaseComparator getCaseComparator(CasesListSearchCriteriaBean criterias, Locale locale) {
		return (criterias == null || ListUtil.isEmpty(criterias.getSortingOptions())) ?
				new CaseComparator() :
				new BPMCaseComparator(locale, criterias);
	}

	private BuilderLogicWrapper getBuilderLogic() {
		return builderLogic;
	}

	private GeneralCasesListBuilder getCasesListBuilder() {
		return casesListBuilder;
	}

	private CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		} catch (IBOLookupException ile) {
			LOGGER.log(Level.SEVERE, "Error getting CasesBusiness", ile);
		}

		return null;
	}

	private CasesBPMProcessView getCasesBPMProcessView() {
		return casesBPMProcessView;
	}

	@Override
	public RenderedComponent getVariablesWindow(String processDefinitionId) {
		RenderedComponent fake = new RenderedComponent();
		fake.setErrorMessage("There are no variables for selected process!");

		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return fake;
		}

		List<String> resources = new ArrayList<String>();
		resources.add(web2.getBundleUriToHumanizedMessagesStyleSheet());
		resources.add(jQuery.getBundleURIToJQueryLib());
		resources.add(web2.getBundleUriToHumanizedMessagesScript());

		resources.add(getJQuery().getBundleURIToJQueryUILib(JQueryUIType.UI_CORE));
		resources.add(getJQuery().getBundleURIToJQueryUILib(JQueryUIType.UI_WIDGET));
		resources.add(getJQuery().getBundleURIToJQueryUILib(JQueryUIType.UI_MOUSE));
		resources.add(getJQuery().getBundleURIToJQueryUILib(JQueryUIType.UI_SORTABLE));

		fake.setResources(resources);

		IWResourceBundle iwrb = getResourceBundle(iwc);
		fake.setErrorMessage(iwrb.getLocalizedString("cases_search.there_are_no_variables_for_selected_process", fake.getErrorMessage()));

		if (!iwc.isLoggedOn()) {
			LOGGER.warning("User must be logged!");
			return fake;
		}

		if (StringUtil.isEmpty(processDefinitionId)) {
			return fake;
		}
		Long pdId = null;
		try {
			pdId = Long.valueOf(processDefinitionId);
		} catch(NumberFormatException e) {
			LOGGER.severe("Unable to convert to Long: " + processDefinitionId);
		}
		if (pdId == null) {
			return fake;
		}

		BPMProcessVariablesBean variablesBean = null;
		try {
			variablesBean = ELUtil.getInstance().getBean(BPMProcessVariablesBean.SPRING_BEAN_IDENTIFIER);
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "Error getting bean: " + BPMProcessVariablesBean.class.getName(), e);
		}
		if (variablesBean == null) {
			return fake;
		}

		variablesBean.setProcessDefinitionId(pdId);

		return getBuilderLogic().getBuilderService(iwc).getRenderedComponentByClassName(UIProcessVariables.class.getName(), null);
	}

	private CasesSearchResultsHolder getSearchResultsHolder() throws NullPointerException {
		CasesSearchResultsHolder resultsHolder = null;
		try {
			resultsHolder = ELUtil.getInstance().getBean(CasesSearchResultsHolder.SPRING_BEAN_IDENTIFIER);
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting bean for search results holder: " + CasesSearchResultsHolder.class.getName(), e);
		}

		if (resultsHolder == null) {
			throw new NullPointerException();
		}

		return resultsHolder;
	}

	private boolean setSearchResults(IWContext iwc, Collection<CasePresentation> cases, CasesListSearchCriteriaBean criterias) {
		iwc.setSessionAttribute(GeneralCasesListBuilder.USER_CASES_SEARCH_SETTINGS_ATTRIBUTE, criterias);

		CasesSearchResultsHolder resultsHolder = null;
		try {
			resultsHolder = getSearchResultsHolder();
		} catch(NullPointerException e) {
			return false;
		}

		String id = criterias.getId();
		resultsHolder.setSearchResults(id, new CasesSearchResults(id, cases, criterias));
		return true;
	}

	private String getType(BuilderLogic builder, String pageKey, String instanceId) {
		String type = getProperty(builder, pageKey, instanceId, ":method:1:implied:void:setType:java.lang.String:");
		return StringUtil.isEmpty(type) ? CasesRetrievalManager.CASE_LIST_TYPE_OPEN : type;
	}

	private List<String> getCodes(BuilderLogic builder, String pageKey, String instanceId) {
		String codes = getProperty(builder, pageKey, instanceId, ":method:1:implied:void:setCaseCodes:java.lang.String:");
		return StringUtil.isEmpty(codes) ? null : new ArrayList<String>(Arrays.asList(codes.split(CoreConstants.COMMA)));
	}

	private List<String> getStatusesToHide(BuilderLogic builder, String pageKey, String instanceId) {
		String hide = getProperty(builder, pageKey, instanceId, ":method:1:implied:void:setCaseStatusesToHide:java.lang.String:");
		return StringUtil.isEmpty(hide) ? null : new ArrayList<String>(Arrays.asList(hide.split(CoreConstants.COMMA)));
	}

	private List<String> getStatusesToShow(BuilderLogic builder, String pageKey, String instanceId) {
		String show = getProperty(builder, pageKey, instanceId, ":method:1:implied:void:setCaseStatusesToShow:java.lang.String:");
		return StringUtil.isEmpty(show) ? null : new ArrayList<String>(Arrays.asList(show.split(CoreConstants.COMMA)));
	}

	private boolean isShowSubscribedOnly(BuilderLogic builder, String pageKey, String instanceId) {
		String subscribedOnly = getProperty(builder, pageKey, instanceId, ":method:1:implied:void:setOnlySubscribedCases:boolean:");
		return StringUtil.isEmpty(subscribedOnly) ? false : "T".equals(subscribedOnly) || Boolean.TRUE.toString().equals(subscribedOnly);
	}

	private String getProperty(BuilderLogic builder, String pageKey, String instanceId, String property) {
		return builder.getProperty(pageKey, instanceId, property);
	}

	@Override
	public AdvancedProperty getExportedCases(String instanceId, String uri,
			Boolean exportContacts, Boolean showCompany) {
		if (StringUtil.isEmpty(instanceId)) {
			return null;
		}

		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null || !iwc.isLoggedOn()) {
			return null;
		}

		BuilderLogic builder = BuilderLogic.getInstance();
		String pageKey = builder.getPageKeyByURI(uri, iwc.getDomain());

		User user = iwc.getCurrentUser();

		String type = getType(builder, pageKey, instanceId);
		List<String> caseCodes = getCodes(builder, pageKey, instanceId);
		List<String> statusesToHide = getStatusesToHide(builder, pageKey, instanceId);
		List<String> statusesToShow = getStatusesToShow(builder, pageKey, instanceId);
		boolean onlySubscribedCases = isShowSubscribedOnly(builder, pageKey, instanceId);

		PagedDataCollection<CasePresentation> cases = getCaseManagersProvider().getCaseManager().getCases(
				user,
				type,
				iwc.getCurrentLocale(),
				caseCodes,
				statusesToHide,
				statusesToShow,
				Integer.MAX_VALUE,
				-1,
				onlySubscribedCases,
				true
		);

		AdvancedProperty result = new AdvancedProperty(Boolean.FALSE.toString());
		String errorMessage = getResourceBundle(iwc).getLocalizedString("unable_to_export_all_cases", "Sorry, unable to export cases to Excel");

		if (cases == null || ListUtil.isEmpty(cases.getCollection())) {
			result.setValue(errorMessage);
			return result;
		}

		CasesSearchResultsHolder resultsHolder = null;
		try {
			resultsHolder = getSearchResultsHolder();
		} catch (NullPointerException e) {
			result.setValue(errorMessage);
			return result;
		}

		List<CasePresentation> casesToExport = new ArrayList<CasePresentation>(cases.getCollection());
		if (!resultsHolder.setCasesToExport(instanceId, casesToExport)) {
			result.setValue(errorMessage);
			return result;
		}

		result.setId(Boolean.TRUE.toString());
		URIUtil uriUtil = new URIUtil(iwc.getIWMainApplication().getMediaServletURI());
		uriUtil.setParameter(MediaWritable.PRM_WRITABLE_CLASS, IWMainApplication.getEncryptedClassName(CasesSearchResultsExporter.class));
		uriUtil.setParameter(CasesSearchResultsExporter.ALL_CASES_DATA, instanceId);
		uriUtil.setParameter(CasesSearchResultsExporter.EXPORT_CONTACTS, (exportContacts != null) && (exportContacts.equals(Boolean.TRUE)) ? "y" : "n");
		uriUtil.setParameter(CasesSearchResultsExporter.SHOW_USER_COMPANY, (showCompany != null) && (showCompany.equals(Boolean.TRUE)) ? "y" : "n");
		result.setValue(uriUtil.getUri());

		return result;
	}

	@Override
	public AdvancedProperty getExportedSearchResults(String id, boolean exportContacts, boolean showCompany) {
		return getSearchResults(id, exportContacts, showCompany, null);
	}

	@Override
	public <T extends MediaWritable> AdvancedProperty getSearchResultsWithExporter(String id, Class<T> exporter) {
		return getSearchResults(id, false, false, exporter);
	}

	private <T extends MediaWritable> AdvancedProperty getSearchResults(String pageURI, boolean exportContacts, boolean showCompany, Class<T> exporter) {
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}

		AdvancedProperty result = new AdvancedProperty(Boolean.FALSE.toString());

		CasesSearchResultsHolder resultsHolder = null;
		try {
			resultsHolder = getSearchResultsHolder();
		} catch (NullPointerException e) {
			result.setValue(getResourceBundle(iwc).getLocalizedString("unable_to_export_search_results", "Sorry, unable to export search results to Excel"));
			return result;
		}

		CasesSearchCriteriaBean criterias = resultsHolder.getSearchCriteria(pageURI);
		if (criterias != null) {
			criterias.setPage(-1);
			criterias.setPageSize(Integer.MAX_VALUE);
		}
		if (!resultsHolder.isSearchResultStored(pageURI) || !resultsHolder.isAllDataLoaded(pageURI)) {
			Collection<CasePresentation> cases = getReLoadedCases(resultsHolder, criterias, pageURI);
			if (ListUtil.isEmpty(cases)) {
				result.setValue(getResourceBundle(iwc).getLocalizedString("no_search_results_to_export", "There are no search results to export!"));
				return result;
			} else if (criterias != null) {
				criterias.setPageSize(cases.size());
				criterias.setAllDataLoaded(Boolean.TRUE);
			}
		}

		getExternalSearchResults(resultsHolder, pageURI);
		if (exporter == null) {
			if (!resultsHolder.doExport(pageURI, exportContacts, showCompany)) {
				result.setValue(getResourceBundle(iwc).getLocalizedString("unable_to_export_search_results", "Sorry, unable to export search results to Excel"));
				return result;
			}
		}

		result.setId(Boolean.TRUE.toString());
		URIUtil uriUtil = new URIUtil(iwc.getIWMainApplication().getMediaServletURI());
		uriUtil.setParameter(MediaWritable.PRM_WRITABLE_CLASS, IWMainApplication.getEncryptedClassName(exporter == null ? CasesSearchResultsExporter.class : exporter));
		uriUtil.setParameter(CasesSearchResultsExporter.ID_PARAMETER, pageURI);
		result.setValue(uriUtil.getUri());

		return result;
	}

	private CaseManagersProvider getCaseManagersProvider() {
		return caseManagersProvider;
	}

	@Override
	public boolean clearSearchResults(String id) {
		IWContext iwc = CoreUtil.getIWContext();
		iwc.removeSessionAttribute(GeneralCasesListBuilder.USER_CASES_SEARCH_QUERY_BEAN_ATTRIBUTE);
		iwc.removeSessionAttribute(GeneralCasesListBuilder.USER_CASES_SEARCH_SETTINGS_ATTRIBUTE);
		setCasesPagerAttributes(-1, -1);
		return getSearchResultsHolder().clearSearchResults(id);
	}

	@Override
	public List<AdvancedProperty> getDefaultSortingOptions(IWContext iwc) {
		List<AdvancedProperty> defaultSortingOptions = new ArrayList<AdvancedProperty>();

		IWResourceBundle iwrb = iwc.getIWMainApplication().getBundle(CasesConstants.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc);

		defaultSortingOptions.add(new AdvancedProperty("getCaseIdentifier", iwrb.getLocalizedString("case_nr", "Case nr.")));
		defaultSortingOptions.add(new AdvancedProperty("getSubject", iwrb.getLocalizedString("description", "Description")));
		defaultSortingOptions.add(new AdvancedProperty("getOwnerName", iwrb.getLocalizedString("sender", "Sender")));
		defaultSortingOptions.add(new AdvancedProperty("getCreated", iwrb.getLocalizedString("created_date", "Created date")));
		defaultSortingOptions.add(new AdvancedProperty("getCaseStatusLocalized", iwrb.getLocalizedString("status", "Status")));

		return defaultSortingOptions;
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof CaseModifiedEvent) {
			Thread cacheReseter = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						getCache(DefaultCasesListSearchFilter.SEARCH_FILTER_CACHE_NAME, DefaultCasesListSearchFilter.SEARCH_FILTER_CACHE_TTL).clear();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			cacheReseter.start();
		} else if (event instanceof UserHasLoggedInEvent) {
			doLoadCases(((UserHasLoggedInEvent) event).getUserId());
		} else if (event instanceof RepositoryStartedEvent) {
			doLoadCases(((RepositoryStartedEvent) event).getIWMA());
		}
	}

	private void doLoadCases(IWMainApplication app) {
		String groupId = app.getSettings().getProperty("load_cases_for_group");
		if (StringUtil.isEmpty(groupId)) {
			return;
		}

		try {
			GroupBusiness groupBusiness = IBOLookup.getServiceInstance(app.getIWApplicationContext(), GroupBusiness.class);
			Group group = groupBusiness.getGroupByGroupID(Integer.valueOf(groupId));
			doLoadCases(app, group);

			if (app.getSettings().getBoolean("load_cases_for_group_children", Boolean.FALSE)) {
				Collection<Group> children = group.getChildren();
				if (ListUtil.isEmpty(children)) {
					LOGGER.warning("There are no children groups for group " + group + ", ID: " + group.getId());
					return;
				}

				for (Group child: children) {
					doLoadCases(app, child);
				}
			}
		} catch (FinderException e) {
			LOGGER.warning("Group by ID '" + groupId + "' does not exist!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void doLoadCases(IWMainApplication app, Group group) {
		if (group == null) {
			LOGGER.warning("Group is not provided");
			return;
		}

		try {
			UserBusiness userBusiness = IBOLookup.getServiceInstance(app.getIWApplicationContext(), UserBusiness.class);
			Collection<User> users = userBusiness.getUsersInGroup(group);
			if (ListUtil.isEmpty(users)) {
				LOGGER.warning("There are no users in group " + group + ", ID: " + group.getId());
				return;
			}

			LOGGER.info("Will load cases for users: " + users);
			for (final User user: users) {
				Thread loader = new Thread(new Runnable() {

					@Override
					public void run() {
						doLoadCases(user);
					}
				});
				loader.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void doLoadCases(User user) {
		if (user == null) {
			getLogger().warning("User is not provided!");
			return;
		}

		getLogger().info("Loading cases for " + user);

		BuilderLogic builder = BuilderLogic.getInstance();

		List<Class<? extends CaseBlock>> classes = Arrays.asList(
				OpenCases.class,
				ClosedCases.class,
				MyCases.class,
				PublicCases.class,
				UserCases.class
		);
		for (Class<? extends CaseBlock> theClass: classes) {
			Collection<ICObjectInstance> instances = null;
			try {
				ICObjectInstanceHome instanceHome = (ICObjectInstanceHome) IDOLookup.getHome(ICObjectInstance.class);
				instances = instanceHome.getByClassName(theClass);
			} catch (FinderException e) {
			} catch (Exception e) {}
			if (ListUtil.isEmpty(instances)) {
				continue;
			}

			for (ICObjectInstance instance: instances) {
				if (instance == null) {
					continue;
				}
				String uniqueId = instance.getUniqueId();
				if (StringUtil.isEmpty(uniqueId)) {
					continue;
				}

				String instanceId = ICObjectBusiness.UUID_PREFIX.concat(uniqueId);
				ICPage page = builder.findPageForModule(getApplication(), instanceId);
				if (page == null || page.getDeleted()) {
					continue;
				}
				String pageKey = page.getId();

				String type = getType(builder, pageKey, instanceId);
				List<String> caseCodes = getCodes(builder, pageKey, instanceId);
				List<String> statusesToHide = getStatusesToHide(builder, pageKey, instanceId);
				List<String> statusesToShow = getStatusesToShow(builder, pageKey, instanceId);
				boolean onlySubscribedCases = isShowSubscribedOnly(builder, pageKey, instanceId);

				try {
					getCaseManagersProvider().getCaseManager().getCaseIds(user, type, caseCodes, statusesToHide, statusesToShow, onlySubscribedCases, false, 0, Integer.MAX_VALUE);
				} catch (Exception e) {
					getLogger().log(Level.WARNING, "Error loading cases for list " + theClass.getName() + " for user " + user +
							" after login event", e);
				}
			}
		}
	}

	private void doLoadCases(final Integer userId) {
		if (!getApplication().getSettings().getBoolean("load_cases_on_user_login", Boolean.TRUE)) {
			return;
		}

		if (userId == null || userId < 0) {
			return;
		}

		Thread loader = new Thread(new Runnable() {

			@Override
			public void run() {
				UserBusiness userBusiness = getServiceInstance(UserBusiness.class);
				User user = null;
				try {
					user = userBusiness.getUser(userId);
				} catch (Exception e) {}
				doLoadCases(user);
			}
		});
		loader.start();
	}

	@Override
	public Collection<CasePresentation> getReLoadedCases(CasesSearchCriteriaBean criterias) {
		Collection<CasePresentation> reLoadedCases = getReLoadedCases(null, criterias, null);
		if (!ListUtil.isEmpty(reLoadedCases)) {
			criterias.setAllDataLoaded(Boolean.FALSE);
		}
		return reLoadedCases;
	}

	private Collection<CasePresentation> getReLoadedCases(CasesSearchResultsHolder resultsHolder, CasesSearchCriteriaBean criterias, String id) {
		if (criterias instanceof CasesListSearchCriteriaBean) {
			IWContext iwc = CoreUtil.getIWContext();
			CasesListSearchCriteriaBean listCriterias = (CasesListSearchCriteriaBean) criterias;

			PagedDataCollection<CasePresentation> cases = getCasesByQuery(iwc, listCriterias);
			if (cases == null) {
				cases = new PagedDataCollection<CasePresentation>(new ArrayList<CasePresentation>());
			}

			listCriterias.setAllDataLoaded(Boolean.TRUE);
			listCriterias.setPageSize(cases.getTotalCount().intValue());
			setSearchResults(iwc, cases.getCollection(), listCriterias);

			if (resultsHolder != null && id != null) {
				Collection<CasePresentation> externalData = getExternalSearchResults(resultsHolder, id);
				if (ListUtil.isEmpty(cases.getCollection())) {
					return externalData;
				}
			}

			return cases.getCollection();
		}

		getLogger().warning("Unable to get cases by query " + criterias + " because it is not instance of " +
				CasesListSearchCriteriaBean.class.getName() + ". Actual implementation: " +
				(criterias == null ? "null " : criterias.getClass().getName()));
		return null;
	}

	private Collection<CasePresentation> getExternalSearchResults(CasesSearchResultsHolder resultsHolder, String id) {
		Map<String, ? extends ExternalCasesDataExporter> externalExporters = getBeansOfType(ExternalCasesDataExporter.class);
		if (externalExporters == null || externalExporters.isEmpty()) {
			return null;
		}

		List<CasePresentation> data = new ArrayList<CasePresentation>();
		for (ExternalCasesDataExporter externalExporter: externalExporters.values()) {
			List<CasePresentation> externalData = externalExporter.getExternalData(id);
			resultsHolder.concatExternalData(id, externalData);

			if (!ListUtil.isEmpty(externalData)) {
				data.addAll(externalData);
			}
		}

		return data;
	}

	@Override
	public String getCaseStatus(Long processInstanceId) {
		if (processInstanceId == null) {
			LOGGER.warning("Process instance id is not provided");
			return null;
		}

		CaseProcInstBind bind = null;
		try {
			bind = casesBPMDAO.getCaseProcInstBindByProcessInstanceId(processInstanceId);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error while getting instance of " + CaseProcInstBind.class.getName() + " by process instance: " +
					processInstanceId, e);
		}
		if (bind == null) {
			return null;
		}

		CasePresentation theCase = null;
		try {
			theCase = caseManagersProvider.getCaseManager().getCaseByIdLazily(bind.getCaseId());
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting case by id: " + bind.getCaseId(), e);
		}

		return theCase == null ? null : theCase.getLocalizedStatus();
	}

	@Override
	public boolean setCasesPagerAttributes(int page, int pageSize) {
		page = page <= 0 ? 1 : page;
		pageSize = pageSize <= 0 ? 20 : pageSize;

		String key = "userCases";
		IWContext iwc = CoreUtil.getIWContext();
		iwc.setSessionAttribute(ListNavigator.PARAMETER_CURRENT_PAGE + "_" + key, page);
		iwc.setSessionAttribute(ListNavigator.PARAMETER_NUMBER_OF_ENTRIES + "_" + key, pageSize);

		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.cases.business.CasesEngine#getAvailableProcesses(com.idega.presentation.IWContext)
	 */
	@Override
	public List<AdvancedProperty> getAvailableProcesses(IWContext iwc) {
		ApplicationBusiness appBusiness = null;
		try {
			appBusiness = IBOLookup.getServiceInstance(iwc, ApplicationBusiness.class);
		} catch (IBOLookupException e) {
			e.printStackTrace();
		}

		if (appBusiness == null) {
			return null;
		}

		Map<ApplicationType, Collection<Application>> appsByType = new HashMap<>();
		List<AdvancedProperty> allProcesses = new ArrayList<AdvancedProperty>();
		List<String> appTypeNames = Arrays.asList("appTypeBPM", "bpm2CamundaAppManager");
		for (String appTypeName: appTypeNames) {
			ApplicationType appType = null;
			try {
				appType = ELUtil.getInstance().getBean(appTypeName);
			} catch(Exception e) {}
			if (appType == null) {
				continue;
			}

			Collection<Application> bpmApps = appBusiness.getApplicationsByType(appType.getType());
			if (!ListUtil.isEmpty(bpmApps)) {
				appsByType.put(appType, bpmApps);
			}
		}
		try {
			Map<String, CasesRetrievalManager> managers = WebApplicationContextUtils.getWebApplicationContext(getApplication().getServletContext()).getBeansOfType(CasesRetrievalManager.class);
			if (MapUtil.isEmpty(managers)) {
				return null;
			}

			Locale locale = iwc.getCurrentLocale();
			for (CasesRetrievalManager caseManager: managers.values()) {
				String processId = null;
				String processName = null;
				String localizedName = null;

				for (ApplicationType appType: appsByType.keySet()) {
					Collection<Application> apps = appsByType.get(appType);
					if (ListUtil.isEmpty(apps)) {
						continue;
					}

					for (Application app: apps) {
						processId = null;
						processName = app.getUrl();
						localizedName = processName;

						if (appType.isVisible(app)) {

							if (StringUtil.isEmpty(processId)) {
								Serializable id = caseManager.getLatestProcessDefinitionIdByProcessName(processName);
								if (id != null) {
									processId = String.valueOf(id);
								}
							}

							localizedName = caseManager.getProcessName(processName, locale);

							if (!StringUtil.isEmpty(processId) && !StringUtil.isEmpty(localizedName)) {
								allProcesses.add(new AdvancedProperty(processId, localizedName));
							}
						}
						else {
							LOGGER.warning(new StringBuilder("Application '").append(app.getName()).append("' is not accessible")
									.append((iwc.isLoggedOn() ? " for user: " + iwc.getCurrentUser() : ": user must be logged in!")).toString());
						}
					}
				}
			}

			if (ListUtil.isEmpty(allProcesses)) {
				return null;
			}

			Collections.sort(allProcesses, new AdvancedPropertyComparator(iwc.getCurrentLocale()));
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting names of BPM processes for " + appsByType, e);
		}

		return allProcesses;
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.cases.business.CasesEngine#isResolverExist(java.lang.String)
	 */
	@Override
	public String isResolverExist(String beanName) {
		Map<String, String> cache = getResolversCache();
		if (cache == null) {
			getLogger().log(Level.WARNING, "Unable to get cache!");
			return null;
		}

		if (cache.containsKey(beanName)) {
			return cache.get(beanName);
		}

		try {
			MultipleSelectionVariablesResolver resolver = ELUtil.getInstance().getBean(beanName);

			if (resolver != null) {
				String className = resolver.getPresentationClass().getName();
				cache.put(beanName, className);
				return className;
			}
		} catch (Throwable e) {}

		cache.put(beanName, CoreConstants.EMPTY);
		return CoreConstants.EMPTY;
	}

	private Map<String, String> getResolversCache() {
		return getCache("BEAN_NAMES_FOR_BPM_PROCESS_SEARCH");
	}

	@Override
	public boolean showCaseAssets() {
		try {
			CasesBPMAssetsState assetsState = ELUtil.getInstance().getBean(CasesBPMAssetsState.beanIdentifier);
			assetsState.showAssets();
			return true;
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error while setting instruction to show assets", e);
		}
		return false;
	}

	@Override
	public AdvancedProperty getExportedCasesToPDF(CasesExportParams params) {
		IWResourceBundle iwrb = getResourceBundle(getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER));
		AdvancedProperty result = new AdvancedProperty(
				Boolean.FALSE.toString(),
				iwrb.getLocalizedString("failed_to_export_cases", "Failed to export cases"),
				String.valueOf(0)
		);

		if (params == null) {
			return result;
		}

		String id = params.getId();
		Map<String, List<Integer>> idsCache = getCache("cases_to_export_to_pdf_cache", 86400, 100);

		Map<String, Map<String, String>> statusesCache = getCache("cases_to_export_to_pdf_statuses_cache", 86400, 100);
		Map<String, String> exportStatuses = statusesCache.get(id);
		if (exportStatuses != null) {
			exportStatuses.remove(id);
		}

		Long processDefinitionId = params.getProcessDefinitionId();
		if (processDefinitionId == null) {
			return result;
		}

		IWTimestamp from = null, to = null;
		Date tmp = IWDatePickerHandler.getParsedDate(params.getDateFrom());
		if (tmp != null) {
			from = new IWTimestamp(tmp);
		}
		tmp = null;
		tmp = IWDatePickerHandler.getParsedDate(params.getDateTo());
		if (tmp != null) {
			to = new IWTimestamp(tmp);
		}

		List<Integer> casesIds = casesBPMDAO.getCaseIdsByProcessDefinitionIdAndStatusAndDateRange(processDefinitionId, params.getStatus(), from, to);
		if (ListUtil.isEmpty(casesIds)) {
			result.setValue(iwrb.getLocalizedString("there_are_no_cases_to_export", "There are no cases to export"));
			return result;
		}

		idsCache.put(id, casesIds);

		result.setId(id);
		int seconds = casesIds.size() * 10;
		int minutes = ((seconds / 60) % 60);
		int hours   = ((seconds / (60*60)) % 24);
		String label = iwrb.getLocalizedString("found_cases_to_export", "Found applications to export") + ": " + casesIds.size() + ". " +
			iwrb.getLocalizedString("export_process_may_take_time", "Approximately it may take") + CoreConstants.SPACE +
			(hours > 0 ? hours + CoreConstants.SPACE + iwrb.getLocalizedString("hours", "hour(s)") + CoreConstants.SPACE : CoreConstants.EMPTY) +
			(minutes > 0 ? minutes + CoreConstants.SPACE + iwrb.getLocalizedString("minutes", "minute(s)") + CoreConstants.SPACE : CoreConstants.EMPTY) +
			(seconds % 60 > 0 ? (seconds % 60) + CoreConstants.SPACE + iwrb.getLocalizedString("seconds", "second(s)") + CoreConstants.SPACE : CoreConstants.EMPTY) +
			iwrb.getLocalizedString("to_export_do_you_want_to_continue", "to export. Do you want to continue?");
		result.setValue(label);
		result.setName(String.valueOf(casesIds.size()));
		return result;
	}

	@Override
	public AdvancedProperty doActualExport(String id) {
		IWResourceBundle iwrb = getResourceBundle(getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER));
		AdvancedProperty result = new AdvancedProperty(
				Boolean.FALSE.toString(),
				iwrb.getLocalizedString("failed_to_export_cases", "Failed to export cases")
		);

		Map<String, String> exportStatuses = null;
		try {

			if (StringUtil.isEmpty(id)) {
				return result;
			}

			Map<String, List<Integer>> idsCache = getCache("cases_to_export_to_pdf_cache", 86400, 100);
			List<Integer> casesIds = idsCache.get(id);

			Map<String, Map<String, String>> statusesCache = getCache("cases_to_export_to_pdf_statuses_cache", 86400, 100);
			exportStatuses = statusesCache.get(id);
			if (exportStatuses == null) {
				exportStatuses = new HashMap<String, String>();
				statusesCache.put(id, exportStatuses);
			}

			File baseDir = CasesExporter.getDirectory(id);

			int i = 0;
			for (Iterator<Integer> casesIdsIter = casesIds.iterator(); casesIdsIter.hasNext();) {
				Integer caseId = casesIdsIter.next();

				i++;
				exportStatuses.put(
					id,
					iwrb.getLocalizedString("exporting", "Exporting") + CoreConstants.SPACE + i+ CoreConstants.SPACE +
							iwrb.getLocalizedString("of", "of") + CoreConstants.SPACE + casesIds.size()
				);

				List<CasePDF> casePDFs = null;
				try {
					casePDFs = caseConverter.getPDFsAndAttachmentsForCase(caseId);
				} catch (Exception e) {
					getLogger().log(Level.WARNING, "Error exporting case (ID: " + caseId + ") to PDFs", e);
				}
				if (ListUtil.isEmpty(casePDFs)) {
					continue;
				}

				File caseFolder = new File(baseDir.getAbsolutePath() + File.separator + casePDFs.get(0).getIdentifier());
				if (!caseFolder.exists()) {
					caseFolder.mkdir();
				}
				for (CasePDF casePDF: casePDFs) {
					if (casePDF == null) {
						continue;
					}
					byte[] pdfData = casePDF.getBytes();
					if (pdfData == null || pdfData.length <= 0) {
						continue;
					}

					File pdf = new File(caseFolder.getAbsoluteFile() + File.separator + casePDF.getName());
					if (!pdf.exists()) {
						pdf.createNewFile();
					}

					FileUtil.streamToFile(new ByteArrayInputStream(pdfData), pdf);

					List<CaseAttachment> attachments = casePDF.getAttachments();
					if (!ListUtil.isEmpty(attachments)) {
						File attachmentsFolder = new File(caseFolder.getAbsoluteFile() + File.separator + "attachments");
						if (!attachmentsFolder.exists()) {
							attachmentsFolder.mkdir();
						}

						for (CaseAttachment attachment: attachments) {
							if (attachment == null) {
								continue;
							}
							byte[] attachmentData = attachment.getBytes();
							if (attachmentData == null || attachmentData.length <= 0) {
								continue;
							}

							File attachmentFile = new File(attachmentsFolder.getAbsoluteFile() + File.separator + attachment.getName());
							if (!attachmentFile.exists()) {
								attachmentFile.createNewFile();
							}

							FileUtil.streamToFile(new ByteArrayInputStream(attachmentData), attachmentFile);
						}
					}
				}
			}
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error exporting cases to PDFs", e);
			if (exportStatuses != null) {
				exportStatuses.remove(id);
			}
			return result;
		}

		result.setId(Boolean.TRUE.toString());
		result.setValue(iwrb.getLocalizedString("cases_were_successfully_exported", "Cases were successfully exported"));
		result.setName(id);
		if (exportStatuses != null) {
			exportStatuses.remove(id);
		}
		return result;
	}

	@Override
	public AdvancedProperty getStatusOfExport(String id) {
		if (StringUtil.isEmpty(id)) {
			return new AdvancedProperty(Boolean.FALSE.toString());
		}

		Map<String, Map<String, String>> statusesCache = getCache("cases_to_export_to_pdf_statuses_cache", 86400, 100);
		Map<String, String> exportStatuses = statusesCache.get(id);
		if (exportStatuses == null) {
			return null;
		}
		String status = exportStatuses.remove(id);
		if (StringUtil.isEmpty(status)) {
			return null;
		}

		return new AdvancedProperty(Boolean.TRUE.toString(), status);
	}

	@Override
	public Boolean doRemoveFromMemory(String id) {
		if (StringUtil.isEmpty(id)) {
			return false;
		}

		Map<String, List<Integer>> idsCache = getCache("cases_to_export_to_pdf_cache", 86400, 100);
		idsCache.remove(id);
		return true;
	}

	@Override
	public AdvancedProperty getLinkForZippedCases(String id, List<String> casesIdentifiers) {
		IWResourceBundle iwrb = getResourceBundle(getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER));
		AdvancedProperty result = new AdvancedProperty(
				Boolean.FALSE.toString(),
				iwrb.getLocalizedString("unable_to_download_zipped_cases", "Unable to download exported case(s)")
		);

		if (StringUtil.isEmpty(id)) {
			return result;
		}

		File baseDir = CasesExporter.getDirectory(id);
		if (baseDir == null || !baseDir.exists() || !baseDir.canRead() || !baseDir.isDirectory()) {
			return result;
		}

		Collection<File> toZip = null;
		String name = "Exported_cases.zip";
		if (ListUtil.isEmpty(casesIdentifiers)) {
			File[] casesFolders = baseDir.listFiles();
			if (ArrayUtil.isEmpty(casesFolders)) {
				return result;
			}
			toZip = Arrays.asList(casesFolders);
		} else {
			toZip = new ArrayList<File>();
			for (String identifier: casesIdentifiers) {
				File caseFolderToZip = new File(baseDir.getAbsoluteFile() + File.separator + identifier);
				if (!caseFolderToZip.exists() || !caseFolderToZip.canRead() || !caseFolderToZip.isDirectory()) {
					continue;
				}
				toZip.add(caseFolderToZip);
			}
			if (casesIdentifiers.size() == 1) {
				name = "Exported_case_" + casesIdentifiers.get(0) + ".zip";
			}
		}

		if (ListUtil.isEmpty(toZip)) {
			return result;
		}

		File zippedFile = null;
		try {
			zippedFile = FileUtil.getZippedFiles(toZip, name, false);
		} catch (IOException e) {
			getLogger().log(Level.WARNING, "Error zipping content at " + baseDir.getAbsolutePath(), e);
		}

		if (zippedFile == null || !zippedFile.exists() || !zippedFile.canRead()) {
			return result;
		}

		result.setId(Boolean.TRUE.toString());
		URIUtil uri = new URIUtil(getApplication().getMediaServletURI());
		uri.setParameter(MediaWritable.PRM_WRITABLE_CLASS, IWMainApplication.getEncryptedClassName(DownloadWriter.class));
		uri.setParameter(DownloadWriter.PRM_ABSOLUTE_FILE_PATH, zippedFile.getAbsolutePath());
		result.setValue(uri.getUri());
		result.setName(iwrb.getLocalizedString("downloading_exported_data", "Downloading exported data"));
		return result;
	}

	@Override
	public Long startWorkingOnCase(Integer caseId){
		if (caseId==null) {
			return -1L;
		}
		TSOCManager tsocManager = ELUtil.getInstance().getBean(TimeSpentOnCaseManager.BEAN_NAME);
		Integer userId = IWContext.getInstance().getCurrentUserId();
		if (userId < 1) {
			return -1L;
		}
		return tsocManager.startWorkingOnCase(userId, caseId);
	}

	@Override
	public Boolean stopWorkingOnCase(Integer caseId){
		if (caseId==null) {
			return false;
		}
		TSOCManager tsocManager = ELUtil.getInstance().getBean(TimeSpentOnCaseManager.BEAN_NAME);
		Integer userId = IWContext.getInstance().getCurrentUserId();
		if (userId < 1) {
			return false;
		}
		tsocManager.stopWorkingOnCase(userId, caseId);
		return true;
	}

	@Override
	public void stopWorkingOnAllCases(){
		IWContext iwc = IWContext.getCurrentInstance();
		if (iwc.isLoggedOn()) {
			stopWorkingOnAllCases(iwc.getCurrentUserId());
		}
	}

	@Override
	public void stopWorkingOnAllCases(Integer userId){
		TSOCManager tsocManager = ELUtil.getInstance().getBean(TimeSpentOnCaseManager.BEAN_NAME);
		tsocManager.stopWorkingOnAllCases(userId);
	}

	private JQuery getJQuery() {
		if (jQuery == null) {
			ELUtil.getInstance().autowire(this);
		}
		return jQuery;
	}

	public CaseListVariableCache getCasesListVariableCache() {
		return ELUtil.getInstance().getBean(CaseListVariableCache.NAME);
	}

	public VariableInstanceQuerier getVariableInstanceQuerier() {
		if (variableInstanceQuerier == null) {
			ELUtil.getInstance().autowire(this);
		}
		return variableInstanceQuerier;
	}

	public void setVariableInstanceQuerier(VariableInstanceQuerier variableInstanceQuerier) {
		this.variableInstanceQuerier = variableInstanceQuerier;
	}
}