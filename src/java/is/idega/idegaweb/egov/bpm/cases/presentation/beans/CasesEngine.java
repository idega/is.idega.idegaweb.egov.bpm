package is.idega.idegaweb.egov.bpm.cases.presentation.beans;


import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessView;
import is.idega.idegaweb.egov.bpm.cases.business.CasesListSearchCriteriaBean;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.util.CasesConstants;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.FinderException;
import javax.faces.component.UIComponent;

import org.jdom.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.business.CaseBusiness;
import com.idega.block.process.business.CaseManagersProvider;
import com.idega.block.process.data.Case;
import com.idega.block.process.presentation.UserCases;
import com.idega.block.process.presentation.beans.GeneralCasesListBuilder;
import com.idega.builder.business.BuilderLogicWrapper;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.core.accesscontrol.business.NotLoggedOnException;
import com.idega.core.builder.business.ICBuilderConstants;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.ProcessUserBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.identity.RolesManager;
import com.idega.presentation.IWContext;
import com.idega.presentation.ui.handlers.IWDatePickerHandler;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

@Service("casesEngineDWR")
@Scope("singleton")
public class CasesEngine {
	
	private BPMFactory bpmFactory;
	private CaseManagersProvider caseManagersProvider;
	private CasesBPMProcessView casesBPMProcessView;
	private BuilderLogicWrapper builderLogic;
	private GeneralCasesListBuilder casesListBuilder;
	private CasesBPMDAO casesBPMDAO;
	private RolesManager rolesManager;
	
	public static final String FILE_DOWNLOAD_LINK_STYLE_CLASS = "casesBPMAttachmentDownloader";
	public static final String PDF_GENERATOR_AND_DOWNLOAD_LINK_STYLE_CLASS = "casesBPMPDFGeneratorAndDownloader";
	
	private static final Logger logger = Logger.getLogger(CasesEngine.class.getName());
	
	public Long getProcessInstanceId(String caseId) {
		
		if (caseId != null) {
			
			Long processInstanceId = getCasesBPMProcessView().getProcessInstanceId(caseId);
			return processInstanceId;
		}
		
		return null;
	}
	
	public Document getCaseManagerView(String caseIdStr) {
		IWContext iwc = CoreUtil.getIWContext();
		
		if (caseIdStr == null || CoreConstants.EMPTY.equals(caseIdStr) || iwc == null) {
			logger.log(Level.WARNING, "Either not provided:\n caseId="+caseIdStr+", iwc="+iwc);
			return null;
		}
		
		try {
			Integer caseId = new Integer(caseIdStr);
			UIComponent caseAssets = getCasesBPMProcessView().getCaseManagerView(iwc, null, caseId);
			
			Document rendered = getBuilderLogic().getBuilderService(iwc).getRenderedComponent(iwc, caseAssets, true);
			
			return rendered;
			
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception while resolving rendered component for case assets view", e);
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
			e.printStackTrace();
		}
		if (caseBusiness == null) {
			return false;
		}
		
		Case theCase = null;
		try {
			theCase = caseBusiness.getCase(caseId);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (FinderException e) {
			e.printStackTrace();
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
			return null;
		}
		
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}
		
		Collection<Case> cases = getCasesByQuery(iwc, criteriaBean);
		UIComponent component = null;
		if (UserCases.TYPE.equals(criteriaBean.getCaseListType())) {
			component = getCasesListBuilder().getUserCasesList(iwc, cases, null, CasesConstants.CASE_LIST_TYPE_SEARCH_RESULTS, false);
		}
		else {
			component = getCasesListBuilder().getCasesList(iwc, cases, CasesConstants.CASE_LIST_TYPE_SEARCH_RESULTS, false);
		}
		if (component == null) {
			return null;
		}
		
		return getBuilderLogic().getBuilderService(iwc).getRenderedComponent(iwc, component, true);
	}
	
	private List<Integer> getCasesByContactQuery(IWContext iwc, String contact) {
		if (StringUtil.isEmpty(contact)) {
			return null;
		}
		
		Collection<User> usersByContactInfo = getUserBusiness(iwc).getUsersByNameOrEmailOrPhone(contact);
		
		if (ListUtil.isEmpty(usersByContactInfo)) {
			return null;
		}
		
		List<Integer> casesByContact = null;
		List<Integer> casesByContactPerson = null;
		for (User contactPerson: usersByContactInfo) {
			try {
				casesByContactPerson = getCasesBPMDAO().getCaseIdsByProcessInstanceIds(getRolesManager().getProcessInstancesIdsForUser(iwc, contactPerson, false));
			} catch(Exception e) {
				logger.log(Level.SEVERE, "Error getting case IDs from contact query: " + contact, e);
			}
			
			if (ListUtil.isEmpty(casesByContactPerson)) {
				return null;
			}
			
			if (ListUtil.isEmpty(casesByContact)) {
				casesByContact = new ArrayList<Integer>();
			}
			
			for (Integer caseId: casesByContactPerson) {
				if (!casesByContact.contains(caseId)) {
					casesByContact.add(caseId);
				}
			}
		}
		
		return casesByContact;
	}
	
	@SuppressWarnings("unchecked")
	private Collection<Case> getCasesByQuery(IWContext iwc, CasesListSearchCriteriaBean criteriaBean) {
		CasesBusiness casesBusiness = getCasesBusiness(iwc);
		if (casesBusiness == null) {
			return null;
		}
		
		User currentUser = null;
		try {
			currentUser = iwc.getCurrentUser();
		} catch(NotLoggedOnException e) {
			e.printStackTrace();
		}
		if (currentUser == null) {
			return null;
		}
		boolean isCaseSuperAdmin = iwc.getIWMainApplication().getAccessController().hasRole(CasesConstants.ROLE_CASES_SUPER_ADMIN, iwc);
		
		Locale locale = iwc.getCurrentLocale();
		boolean useBPMQueries = true;
		
		//	Number
		List<Integer> casesByNumber = null;
		String caseNumber = criteriaBean.getCaseNumber();
		if (caseNumber != null) {
			try {
				casesByNumber = getCasesBPMDAO().getCaseIdsByCaseNumber(caseNumber.toLowerCase(locale));
			} catch(Exception e) {
				e.printStackTrace();
			}
			
			if (ListUtil.isEmpty(casesByNumber)) {
				useBPMQueries = false;
			}
		}
		
		//	Description
		String description = criteriaBean.getDescription();
		if (description != null) {
			useBPMQueries = false;
			description = description.toLowerCase(locale);
		}
		
		//	Name
		String name = criteriaBean.getName();
		
		//	Personal ID
		String personalId = criteriaBean.getPersonalId();
		
		//	Contact
		boolean checkSimpleCases = true;
		String contact = criteriaBean.getContact();
		List<Integer> casesByContact = getCasesByContactQuery(iwc, contact);
		if (!StringUtil.isEmpty(contact)) {
			if (ListUtil.isEmpty(casesByContact)) {
				return null;
			}
			
			checkSimpleCases = false;
		}
		
		//	Process
		List<Integer> casesByProcessDefinition = null;
		String processDefinitionId = criteriaBean.getProcessId();
		if (processDefinitionId != null) {
			Long procDefId = null;
			try {
				procDefId = Long.valueOf(processDefinitionId);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
			try {
				casesByProcessDefinition = getCasesBPMDAO().getCaseIdsByProcessDefinitionId(procDefId);
			} catch(Exception e) {
				e.printStackTrace();
			}
			
			if (ListUtil.isEmpty(casesByProcessDefinition)) {
				logger.log(Level.INFO, "No cases found by process definition ID: " + processDefinitionId);
				return null;	//	No cases found by process - terminating search
			}
			
			checkSimpleCases = false;
		}

		//	Status
		List<String> allStatuses = null;
		List<Integer> casesByStatus = null;
		boolean searchingByMyCases = false;
		boolean searchingByUserCases = false;
		String statusAsProperty = criteriaBean.getCaseListType();
		if (statusAsProperty != null && !CoreConstants.EMPTY.equals(statusAsProperty)) {
			allStatuses = StringUtil.getValuesFromString(statusAsProperty, ICBuilderConstants.BUILDER_MODULE_PROPERTY_VALUES_SEPARATOR);
			
			//	My cases?
			if (Arrays.deepEquals(casesBusiness.getStatusesForMyCases(), ArrayUtil.convertListToArray(allStatuses))) {
				searchingByMyCases = true;
				try {
					casesByStatus = getCasesBPMDAO().getCaseIdsByProcessUserStatus(ProcessUserBind.Status.PROCESS_WATCHED.toString());
				} catch(Exception e) {
					e.printStackTrace();
				}
				
				if (casesByStatus == null || casesByStatus.isEmpty()) {
					return null;	//	No cases found for current user, terminating search
				}
			}
			
			//	User cases?
			searchingByUserCases = statusAsProperty.equals(UserCases.TYPE);
		}
		String selectedStatus = criteriaBean.getStatusId();
		if (selectedStatus != null) {
			if (searchingByMyCases) {
				if (allStatuses == null) {
					allStatuses = new ArrayList<String>();
				}
				
				if (!allStatuses.contains(selectedStatus)) {
					allStatuses.add(selectedStatus);
				}
			}
			else {
				allStatuses = new ArrayList<String>();
				allStatuses.add(selectedStatus);
			}
		}
		String[] statuses = null;
		if (!searchingByMyCases && !searchingByUserCases) {
			statuses = ArrayUtil.convertListToArray(allStatuses);
			if (casesByStatus == null) {
				try {
					casesByStatus = getCasesBPMDAO().getCaseIdsByCaseStatus(statuses);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		//	Cases by user
		List<Integer> casesByUser = null;
		if (searchingByUserCases || !isCaseSuperAdmin) {
			try {
				casesByUser = getCasesBPMDAO().getCaseIdsByUserIds(currentUser.getId());
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		//	Date range
		IWTimestamp dateFrom = null;
		IWTimestamp dateTo = null;
		List<Integer> casesByDate = null;
		String dateRange = criteriaBean.getDateRange();
		if (dateRange != null) {
			String splitter = " - ";
			if (dateRange.indexOf(splitter) == -1) {
				Date date = IWDatePickerHandler.getParsedDate(dateRange, locale);
				dateFrom = date == null ? null : new IWTimestamp(date);
			}
			else {
				String[] dateRangeParts = dateRange.split(splitter);
				
				Date date = IWDatePickerHandler.getParsedDate(dateRangeParts[0], locale);
				dateFrom = date == null ? null : new IWTimestamp(date);
				date = IWDatePickerHandler.getParsedDate(dateRangeParts[1], locale);
				dateTo = date == null ? null : new IWTimestamp(date);
				if (dateTo != null) {
					dateTo.setHour(23);
					dateTo.setMinute(59);
					dateTo.setSecond(59);
					dateTo.setMilliSecond(999);
					
					try {
						casesByDate = getCasesBPMDAO().getCaseIdsByDateRange(dateFrom, dateTo);
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		//	Cases that MUST be selected
		List<QueryResultsBean> results = new ArrayList<QueryResultsBean>();
		if (!StringUtil.isEmpty(caseNumber)) {
			results.add(new QueryResultsBean(caseNumber, casesByNumber));
		}
		if (!StringUtil.isEmpty(processDefinitionId)) {
			results.add(new QueryResultsBean(processDefinitionId, casesByProcessDefinition));
		}
		if (statuses != null && (!isCaseSuperAdmin || !StringUtil.isEmpty(criteriaBean.getStatusId()))) {
			results.add(new QueryResultsBean(statuses, casesByStatus));
		}
		if (searchingByUserCases || !isCaseSuperAdmin) {
			results.add(new QueryResultsBean(UserCases.TYPE, casesByUser));
		}
		if (!StringUtil.isEmpty(dateRange)) {
			results.add(new QueryResultsBean(dateRange, casesByDate));
		}
		if (!StringUtil.isEmpty(contact)) {
			results.add(new QueryResultsBean("contactInfo", casesByContact));
		}
		List<Integer> casesToSelect = checkBPMCasesIds(results);
		logger.log(Level.INFO, "BPM cases to select: " + casesToSelect);
		
		Collection<Group> handlersGroups = null;
		if (!searchingByUserCases) {
			if (!isCaseSuperAdmin) {
				UserBusiness userBusiness = getUserBusiness(iwc);
				if (userBusiness == null) {
					return null;
				}
				try {
					handlersGroups = userBusiness.getUserGroupsDirectlyRelated(currentUser);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
		
		Collection<Case> cases = null;
		if (checkSimpleCases) {
			cases = casesBusiness.getCasesByCriteria(caseNumber, description, name, personalId, statuses, dateFrom, dateTo,
					((searchingByUserCases || !useBPMQueries) && !isCaseSuperAdmin) ? currentUser : null, useBPMQueries ? handlersGroups: null);
		}
		else {
			cases = new ArrayList<Case>();
		}
		logger.log(Level.INFO, "Cases by query: " + cases);
		
		if (useBPMQueries) {
			//	Checking if not missing cases
			cases = checkIfNotMissingCases(cases, casesToSelect);
			
			//	Checking if all cases can be displayed
			cases = checkIfAllCanBeDisplayed(cases, casesToSelect);
		}
		
		logger.log(Level.INFO, "Final list: " + cases);
		return cases;
	}
	
	private List<Integer> checkBPMCasesIds(List<QueryResultsBean> results) {
		if (ListUtil.isEmpty(results)) {
			return new ArrayList<Integer>(0);
		}
		
		List<Integer> resultIds = null;
		List<Integer> allIds = new ArrayList<Integer>();
		List<List<Integer>> allResultsLists = new ArrayList<List<Integer>>();
		for (QueryResultsBean result: results) {
			if (result.getQuery() != null) {
				resultIds = result.getResults();
				if (ListUtil.isEmpty(resultIds)) {
					return new ArrayList<Integer>(0);	//	At least one query doesn't have results
				}
				
				for (Integer id: resultIds) {
					if (!allIds.contains(id)) {
						allIds.add(id);
					}
				}
				allResultsLists.add(resultIds);
			}
		}
		if (ListUtil.isEmpty(allIds)) {
			return new ArrayList<Integer>(0);
		}
		
		List<Integer> sameIdsInAllLists = new ArrayList<Integer>();
		for (Integer id: allIds) {
			boolean hasThisValueAllLists = true;
			List<Integer> ids = null;
			for (int i = 0; (i < allResultsLists.size() && hasThisValueAllLists); i++) {
				ids = allResultsLists.get(i);
				
				if (!ids.contains(id)) {
					hasThisValueAllLists = false;
				}
			}
			
			if (hasThisValueAllLists) {
				sameIdsInAllLists.add(id);
			}
		}
		
		return sameIdsInAllLists;
	}
	
	private Collection<Case> checkIfAllCanBeDisplayed(Collection<Case> cases, List<Integer> casesToSelect) {
		if (ListUtil.isEmpty(cases)) {
			return null;
		}
		
		if (ListUtil.isEmpty(casesToSelect)) {
			return cases;
		}
		
		Integer caseId = null;
		List<Case> casesToRemove = new ArrayList<Case>();
		for (Case theCase: cases) {
			if (theCase.getCaseManagerType() != null) {
				caseId = null;
				try {
					caseId = Integer.valueOf(theCase.getId());
				} catch(NumberFormatException e) {
					e.printStackTrace();
				}
				
				if (caseId != null && !casesToSelect.contains(caseId)) {
					casesToRemove.add(theCase);
				}
			}
		}
		for (Case theCase: casesToRemove) {
			cases.remove(theCase);
		}
		
		return cases;
	}
	
	private Collection<Case> checkIfNotMissingCases(Collection<Case> cases, List<Integer> casesToSelect) {
		if (ListUtil.isEmpty(casesToSelect)) {
			return cases;
		}
		
		List<String> currentCasesIds = new ArrayList<String>(cases == null ? 0 : cases.size());
		if (cases != null) {
			for (Case theCase: cases) {
				currentCasesIds.add(theCase.getId());
			}
		}
		
		String caseKey = null;
		List<Integer> missingCasesIds = new ArrayList<Integer>();
		for (Integer caseToSelectId: casesToSelect) {
			caseKey = String.valueOf(caseToSelectId);
			if (!currentCasesIds.contains(caseKey)) {
				missingCasesIds.add(caseToSelectId);
			}
		}
		if (missingCasesIds.isEmpty()) {
			return cases;
		}
		
		Collection<Case> missingCases = getCasesBusiness(IWMainApplication.getDefaultIWApplicationContext()).getCasesByIds(missingCasesIds);
		if (ListUtil.isEmpty(missingCases)) {
			return cases;
		}
		
		if (cases == null) {
			cases = new ArrayList<Case>();
		}
		
		cases.addAll(missingCases);
		
		return cases;
	}

	public BuilderLogicWrapper getBuilderLogic() {
		return builderLogic;
	}

	@Autowired
	public void setBuilderLogic(BuilderLogicWrapper builderLogic) {
		this.builderLogic = builderLogic;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public GeneralCasesListBuilder getCasesListBuilder() {
		return casesListBuilder;
	}

	@Autowired
	public void setCasesListBuilder(GeneralCasesListBuilder casesListBuilder) {
		this.casesListBuilder = casesListBuilder;
	}

	@Autowired
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	private CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		}
		catch (IBOLookupException ile) {
			ile.printStackTrace();
		}
		
		return null;
	}
	
	private UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(iwac, UserBusiness.class);
		} catch (IBOLookupException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public CaseManagersProvider getCaseManagersProvider() {
		return caseManagersProvider;
	}

	@Autowired
	public void setCaseManagersProvider(CaseManagersProvider caseManagersProvider) {
		this.caseManagersProvider = caseManagersProvider;
	}

	public CasesBPMProcessView getCasesBPMProcessView() {
		return casesBPMProcessView;
	}

	@Autowired
	public void setCasesBPMProcessView(CasesBPMProcessView casesBPMProcessView) {
		this.casesBPMProcessView = casesBPMProcessView;
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	@Autowired
	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}
	
	private class QueryResultsBean {
		private Object query = null;
		private List<Integer> results = null;
		
		private QueryResultsBean(Object query, List<Integer> results) {
			this.query = query;
			this.results = results;
		}

		private Object getQuery() {
			return query;
		}

		private List<Integer> getResults() {
			return results;
		}
		
	}

	public RolesManager getRolesManager() {
		return rolesManager;
	}

	@Autowired
	public void setRolesManager(RolesManager rolesManager) {
		this.rolesManager = rolesManager;
	}

}