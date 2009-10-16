package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessView;
import is.idega.idegaweb.egov.bpm.cases.exe.CasesBPMProcessDefinitionW;
import is.idega.idegaweb.egov.bpm.cases.presentation.UIProcessVariables;
import is.idega.idegaweb.egov.bpm.cases.search.CasesListSearchCriteriaBean;
import is.idega.idegaweb.egov.bpm.cases.search.CasesListSearchFilter;
import is.idega.idegaweb.egov.bpm.media.CasesSearchResultsExporter;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.util.CasesConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.component.UIComponent;

import org.jdom.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.business.CaseBusiness;
import com.idega.block.process.business.CaseManagersProvider;
import com.idega.block.process.business.CasesRetrievalManager;
import com.idega.block.process.business.ProcessConstants;
import com.idega.block.process.data.Case;
import com.idega.block.process.presentation.beans.CaseListPropertiesBean;
import com.idega.block.process.presentation.beans.CasePresentation;
import com.idega.block.process.presentation.beans.CasePresentationComparator;
import com.idega.block.process.presentation.beans.CasesSearchCriteriaBean;
import com.idega.block.process.presentation.beans.CasesSearchResultsHolder;
import com.idega.block.process.presentation.beans.GeneralCasesListBuilder;
import com.idega.block.web2.business.JQuery;
import com.idega.block.web2.business.Web2Business;
import com.idega.bpm.bean.CasesBPMAssetProperties;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.builder.business.BuilderLogicWrapper;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.core.component.bean.RenderedComponent;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.io.MediaWritable;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.presentation.IWContext;
import com.idega.presentation.paging.PagedDataCollection;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.URIUtil;
import com.idega.util.expression.ELUtil;
import com.idega.webface.WFUtil;

@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service("casesEngineDWR")
public class CasesEngineImp implements BPMCasesEngine {
	
	@Autowired
	private CasesBPMProcessView casesBPMProcessView;
	
	@Autowired
	private BuilderLogicWrapper builderLogic;
	
	@Autowired
	private GeneralCasesListBuilder casesListBuilder;
	
	@Autowired
	private CaseManagersProvider caseManagersProvider;
	
	@Autowired
	private Web2Business web2;
	
	@Autowired
	private JQuery jQuery;
	
	public static final String FILE_DOWNLOAD_LINK_STYLE_CLASS = "casesBPMAttachmentDownloader";
	public static final String PDF_GENERATOR_AND_DOWNLOAD_LINK_STYLE_CLASS = "casesBPMPDFGeneratorAndDownloader";
	
	private static final Logger LOGGER = Logger.getLogger(CasesEngineImp.class.getName());
	
	public Long getProcessInstanceId(String caseId) {
		
		if (caseId != null) {
			
			Long processInstanceId = getCasesBPMProcessView().getProcessInstanceId(caseId);
			return processInstanceId;
		}
		
		return null;
	}
	
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
		
		try {
			CasesBPMAssetsState stateBean = (CasesBPMAssetsState) WFUtil.getBeanInstance(CasesBPMAssetsState.beanIdentifier);
			if (stateBean != null) {
				stateBean.setUsePDFDownloadColumn(properties.isUsePDFDownloadColumn());
				stateBean.setAllowPDFSigning(properties.isAllowPDFSigning());
				stateBean.setHideEmptySection(properties.isHideEmptySection());
				stateBean.setCommentsPersistenceManagerIdentifier(properties.getCommentsPersistenceManagerIdentifier());
				stateBean.setShowAttachmentStatistics(properties.isShowAttachmentStatistics());
				stateBean.setShowOnlyCreatorInContacts(properties.isShowOnlyCreatorInContacts());
				stateBean.setAutoShowComments(properties.isAutoShowComments());
			}
			
			Integer caseId = new Integer(caseIdStr);
			UIComponent caseAssets = getCasesBPMProcessView().getCaseManagerView(iwc, null, caseId, properties.getProcessorType());
			
			Document rendered = getBuilderLogic().getBuilderService(iwc).getRenderedComponent(iwc, caseAssets, true);
			
			return rendered;
			
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception while resolving rendered component for case assets view", e);
		}
		
		return null;
	}
	
	public boolean setCaseSubject(String caseId, String subject) {
		if (caseId == null || subject == null) {
			return false;
		}
	
		CaseBusiness caseBusiness = null;
		try {
			caseBusiness = (CaseBusiness) IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), CaseBusiness.class);
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
	
	public Document getCasesListByUserQuery(CasesListSearchCriteriaBean criteriaBean) {
		if (criteriaBean == null) {
			LOGGER.log(Level.SEVERE, "Can not execute search - search criterias unknown");
			return null;
		}
		
		//	Clearing search result before new search
		clearSearchResults(criteriaBean.getId());
		
		LOGGER.log(Level.INFO, new StringBuilder("Search query: caseNumber: ").append(criteriaBean.getCaseNumber()).append(", description: ")
				.append(criteriaBean.getDescription()).append(", name: ").append(criteriaBean.getName()).append(", personalId: ")
				.append(criteriaBean.getPersonalId()).append(", processId: ").append(criteriaBean.getProcessId()).append(", statusId: ")
				.append(criteriaBean.getStatusId()).append(", dateRange: ").append(criteriaBean.getDateRange()).append(", casesListType: ")
				.append(criteriaBean.getCaseListType()).append(", contact: ").append(criteriaBean.getContact()).append(", variables provided: ")
				.append(ListUtil.isEmpty(criteriaBean.getProcessVariables()) ? "none" : criteriaBean.getProcessVariables().size())
		.toString());
		
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			LOGGER.warning(IWContext.class + " is unavailable!");
			return null;
		}
		
		addSearchQueryToSession(iwc, criteriaBean);
		
		PagedDataCollection<CasePresentation> cases = getCasesByQuery(iwc, criteriaBean);
		if (cases != null) {
			setSearchResults(iwc, cases.getCollection(), criteriaBean);
		}
		
		CaseListPropertiesBean properties = new CaseListPropertiesBean();
		properties.setType(ProcessConstants.CASE_LIST_TYPE_SEARCH_RESULTS);
		properties.setUsePDFDownloadColumn(criteriaBean.isUsePDFDownloadColumn());
		properties.setAllowPDFSigning(criteriaBean.isAllowPDFSigning());
		properties.setShowStatistics(criteriaBean.isShowStatistics());
		properties.setHideEmptySection(criteriaBean.isHideEmptySection());
		properties.setPageSize(0);
		properties.setPage(0);
		properties.setInstanceId(criteriaBean.getInstanceId());
		properties.setShowCaseNumberColumn(criteriaBean.isShowCaseNumberColumn());
		properties.setShowCreationTimeInDateColumn(criteriaBean.isShowCreationTimeInDateColumn());
		properties.setCaseCodes(criteriaBean.getCaseCodesInList());
		properties.setStatusesToShow(criteriaBean.getStatusesToShowInList());
		properties.setStatusesToHide(criteriaBean.getStatusesToHideInList());
		properties.setOnlySubscribedCases(criteriaBean.isOnlySubscribedCases());
		UIComponent component = null;
		if (CasesRetrievalManager.CASE_LIST_TYPE_USER.equals(criteriaBean.getCaseListType())) {
			properties.setAddCredentialsToExernalUrls(false);
			component = getCasesListBuilder().getUserCasesList(iwc, cases, null, properties);
		}
		else {
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
	
	private void addSearchQueryToSession(IWContext iwc, CasesListSearchCriteriaBean bean) {
		List<AdvancedProperty> searchFields = new ArrayList<AdvancedProperty>();
		
		Locale locale = iwc.getCurrentLocale();
		IWResourceBundle iwrb = getResourceBundle(iwc);
		
		if (!StringUtil.isEmpty(bean.getCaseNumber())) {
			searchFields.add(new AdvancedProperty("case_nr", bean.getCaseNumber()));
		}
		if (!StringUtil.isEmpty(bean.getDescription())) {
			searchFields.add(new AdvancedProperty("description", bean.getDescription()));
		}
		if (!StringUtil.isEmpty(bean.getName())) {
			searchFields.add(new AdvancedProperty("name", bean.getName()));
		}
		if (!StringUtil.isEmpty(bean.getPersonalId())) {
			searchFields.add(new AdvancedProperty("personal_id", bean.getPersonalId()));
		}
		if (!StringUtil.isEmpty(bean.getContact())) {
			searchFields.add(new AdvancedProperty("contact", bean.getContact()));
		}
		if (!StringUtil.isEmpty(bean.getProcessId())) {
			String processName = null;
			try {
				ProcessDefinitionW bpmCasesManager = ELUtil.getInstance().getBean(CasesBPMProcessDefinitionW.SPRING_BEAN_IDENTIFIER);
				bpmCasesManager.setProcessDefinitionId(Long.valueOf(bean.getProcessId()));
				processName = bpmCasesManager.getProcessName(locale);
			} catch(Exception e) {
				LOGGER.log(Level.WARNING, "Error getting process name by: " + bean.getProcessId());
			}
			searchFields.add(new AdvancedProperty("cases_search_select_process", StringUtil.isEmpty(processName) ? "general_cases" : processName));
		}
		if (!StringUtil.isEmpty(bean.getStatusId())) {
			String status = null;
			try {
				status = getCasesBusiness(iwc).getLocalizedCaseStatusDescription(null, getCasesBusiness(iwc).getCaseStatus(bean.getStatusId()), locale);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error getting status name by: " + bean.getStatusId(), e);
			}
			searchFields.add(new AdvancedProperty("status", StringUtil.isEmpty(status) ? iwrb.getLocalizedString("unknown_status", "Unknown") : status));
		}
		if (!StringUtil.isEmpty(bean.getDateRange())) {
			searchFields.add(new AdvancedProperty("date_range", bean.getDateRange()));
		}
		if (!ListUtil.isEmpty(bean.getProcessVariables())) {
			for (BPMProcessVariable variable: bean.getProcessVariables()) {
				searchFields.add(new AdvancedProperty(iwrb.getLocalizedString(new StringBuilder("bpm_variable.").append(variable.getName()).toString(),
																												variable.getName()), variable.getValue()));
			}
		}
		iwc.setSessionAttribute(GeneralCasesListBuilder.USER_CASES_SEARCH_QUERY_BEAN_ATTRIBUTE, searchFields);
	}
	
	private PagedDataCollection<CasePresentation> getCasesByQuery(IWContext iwc, CasesListSearchCriteriaBean criteriaBean) {
		
		final User currentUser;
		
		if(!iwc.isLoggedOn() || (currentUser = iwc.getCurrentUser()) == null) {
			
			LOGGER.log(Level.INFO, "Not logged in, skipping searching");
			return null;
		}
		
		String casesProcessorType = criteriaBean.getCaseListType() == null ? CasesRetrievalManager.CASE_LIST_TYPE_MY : criteriaBean.getCaseListType();
		List<Integer> caseIdsByUser = null;
		try {
			caseIdsByUser = getCaseManagersProvider().getCaseManager().getCaseIds(currentUser, casesProcessorType, criteriaBean.getCaseCodesInList(), 
					criteriaBean.getStatusesToHideInList(), criteriaBean.getStatusesToShowInList(), criteriaBean.isOnlySubscribedCases());
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Some error occured getting cases by criterias: " + criteriaBean, e);
		}
		if (ListUtil.isEmpty(caseIdsByUser)) {
			return null;
		}
		
		if (criteriaBean.getStatusId() != null) {
			criteriaBean.setStatuses(new String[] {criteriaBean.getStatusId()});
		}
		
		PagedDataCollection<CasePresentation> cases = null;
		
		List<CasesListSearchFilter> filters = criteriaBean.getFilters();
		if (filters != null) {
			for (CasesListSearchFilter filt : filters) {
				caseIdsByUser = filt.doFilter(caseIdsByUser);
			}
		}
		
		Locale locale = iwc.getCurrentLocale();
		if (!ListUtil.isEmpty(caseIdsByUser)) {
			cases = getCaseManagersProvider().getCaseManager().getCasesByIds(caseIdsByUser, locale);
		}
		
		if (cases == null || ListUtil.isEmpty(cases.getCollection())) {
			return null;
		}
		
		List<CasePresentation> casesToSort = new ArrayList<CasePresentation>(cases.getCollection());
		Collections.sort(casesToSort, getCasePresentationComparator(criteriaBean, locale));
		cases = new PagedDataCollection<CasePresentation>(casesToSort);
		
		return cases;
	}
	
	private CasePresentationComparator getCasePresentationComparator(CasesListSearchCriteriaBean searchCriterias, Locale locale) {
		return (searchCriterias == null || ListUtil.isEmpty(searchCriterias.getSortingOptions())) ?
				new CasePresentationComparator() :
				new BPMCasePresentationComparator(locale, searchCriterias);
	}

	private BuilderLogicWrapper getBuilderLogic() {
		return builderLogic;
	}

	private GeneralCasesListBuilder getCasesListBuilder() {
		return casesListBuilder;
	}

	private CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		}
		catch (IBOLookupException ile) {
			LOGGER.log(Level.SEVERE, "Error getting CasesBusiness", ile);
		}
		
		return null;
	}

	private CasesBPMProcessView getCasesBPMProcessView() {
		return casesBPMProcessView;
	}
	
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
	
	private boolean setSearchResults(IWContext iwc, Collection<CasePresentation> cases, CasesSearchCriteriaBean criteriaBean) {
		CasesSearchResultsHolder resultsHolder = null;
		try {
			resultsHolder = getSearchResultsHolder();
		} catch(NullPointerException e) {
			return false;
		}
		
		String id = criteriaBean.getId();
		resultsHolder.setSearchResults(id, cases, criteriaBean);
		return true;
	}
	
	public AdvancedProperty getExportedSearchResults(String id) {
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}
		
		AdvancedProperty result = new AdvancedProperty(Boolean.FALSE.toString());
		
		CasesSearchResultsHolder resultsHolder = null;
		try {
			resultsHolder = getSearchResultsHolder();
		} catch(NullPointerException e) {
			result.setValue(getResourceBundle(iwc).getLocalizedString("unable_to_export_search_results", "Sorry, unable to export search results to Excel"));
			return result;
		}
		
		if (!resultsHolder.isSearchResultStored(id)) {
			result.setValue(getResourceBundle(iwc).getLocalizedString("no_search_results_to_export", "There are no search results to export!"));
			return result;
		}
		
		if (!resultsHolder.doExport(id)) {
			result.setValue(getResourceBundle(iwc).getLocalizedString("unable_to_export_search_results", "Sorry, unable to export search results to Excel"));
			return result;
		}
		
		result.setId(Boolean.TRUE.toString());
		URIUtil uriUtil = new URIUtil(iwc.getIWMainApplication().getMediaServletURI());
		uriUtil.setParameter(MediaWritable.PRM_WRITABLE_CLASS, IWMainApplication.getEncryptedClassName(CasesSearchResultsExporter.class));
		uriUtil.setParameter(CasesSearchResultsExporter.ID_PARAMETER, id);
		result.setValue(uriUtil.getUri());
		
		return result;
	}

	private CaseManagersProvider getCaseManagersProvider() {
		return caseManagersProvider;
	}
	
	public boolean clearSearchResults(String id) {
		IWContext iwc = CoreUtil.getIWContext();
		iwc.removeSessionAttribute(GeneralCasesListBuilder.USER_CASES_SEARCH_QUERY_BEAN_ATTRIBUTE);
		
		return getSearchResultsHolder().clearSearchResults(id);
	}

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

}