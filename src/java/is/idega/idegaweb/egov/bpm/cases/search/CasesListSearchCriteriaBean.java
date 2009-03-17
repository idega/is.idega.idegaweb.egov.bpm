package is.idega.idegaweb.egov.bpm.cases.search;

import is.idega.idegaweb.egov.bpm.cases.presentation.beans.BPMProcessVariable;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;
import is.idega.idegaweb.egov.cases.data.GeneralCaseHome;
import is.idega.idegaweb.egov.cases.util.CasesConstants;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.FinderException;

import org.jbpm.graph.def.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.process.business.CasesRetrievalManager;
import com.idega.block.process.data.Case;
import com.idega.block.process.data.CaseHome;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.data.IDOLookup;
import com.idega.data.IDOLookupException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.identity.RolesManager;
import com.idega.presentation.IWContext;
import com.idega.presentation.ui.handlers.IWDatePickerHandler;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreUtil;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

public class CasesListSearchCriteriaBean {
	
	private static final Logger logger = Logger.getLogger(CasesListSearchCriteriaBean.class.getName());
	
	private String caseNumber;
	private String description;
	private String name;
	private String personalId;
	private String processId;
	private String statusId;
	private String dateRange;
	private String caseListType;
	private String contact;
	private IWTimestamp dateFrom;
	private IWTimestamp dateTo;
	private String[] statuses;
	private List<BPMProcessVariable> processVariables;
	
	@Autowired private CasesBPMDAO casesBPMDAO;
	@Autowired private RolesManager rolesManager;
	@Autowired private BPMFactory bpmFactory;
	
	private boolean usePDFDownloadColumn = true;
	private boolean allowPDFSigning = true;
	private boolean showStatistics;
	private boolean hideEmptySection;
	private boolean showCaseNumberColumn = true;
	private boolean showCreationTimeInDateColumn = true;
	
	private String id;
	private String instanceId;
	
	public String getCaseNumber() {
		return caseNumber;
	}
	
	public CasesListSearchFilter getCaseNumberFilter() {
		
		return new CasesListSearchFilter() {

			public List<Integer> doFilter(List<Integer> casesIds) {
				
				if (ListUtil.isEmpty(casesIds)) {
					return casesIds;
				}
				
				String caseNumber = getCaseNumber();
				if (StringUtil.isEmpty(caseNumber)) {
					logger.log(Level.INFO, "Case number is undefined, not filtering by it!");
					return casesIds;
				}
				
//				logger.log(Level.INFO, "IDs BEFORE filtering by number: " + casesIds);
				
				String loweredCaseNumber = caseNumber.toLowerCase(CoreUtil.getIWContext().getCurrentLocale());
				List<Integer> casesByNumberIds = new ArrayList<Integer>();
				
				//	"BPM" cases
				List<Long> bpmCases = null;
				try {
					bpmCases = getCasesBPMDAO().getCaseIdsByCaseNumber(loweredCaseNumber);
				} catch(Exception e) {
					logger.log(Level.WARNING, "Exception while resolving case ids by case number = " + loweredCaseNumber, e);
				}
				if (ListUtil.isEmpty(bpmCases)) {
					logger.log(Level.INFO, "No BPM cases found by number: " + caseNumber);
				}
				else {
					logger.log(Level.INFO, "BPM cases by number (" + caseNumber + "): " + bpmCases);
					casesByNumberIds.addAll(getConvertedFromLongs(bpmCases));
				}
				
				//	Old cases
				List<Integer> simpleCases = CasesRetrievalManager.CASE_LIST_TYPE_USER.equals(getCaseListType()) ? getUserCasesByNumber(loweredCaseNumber) :
					getGeneralCasesByNumber(loweredCaseNumber);
				if (ListUtil.isEmpty(simpleCases)) {
					logger.log(Level.INFO, "No simple cases found by number: " + caseNumber);
				}
				else {
					logger.log(Level.INFO, "Simple cases by number (" + caseNumber + "): " + simpleCases);
					casesByNumberIds.addAll(simpleCases);
				}
				
				if (ListUtil.isEmpty(casesByNumberIds)) {
					logger.log(Level.INFO, "No cases found by number: " + caseNumber);
					//casesIds.clear();	//	No results
				}
				else {
					logger.log(Level.INFO, "Cases found by number (" + caseNumber + "): " + casesByNumberIds);
					//	"Narrowing" results
					//casesIds.retainAll(casesByNumberIds);
				}
				casesIds = getNarrowedResults(casesIds, casesByNumberIds);
				
//				logger.log(Level.INFO, "IDs AFTER filtering by number (" + caseNumber + "): " + casesIds);
				return casesIds;
			}
		};
	}
	
	public CasesListSearchFilter getGeneralCasesFilter() {
		return new CasesListSearchFilter() {

			public List<Integer> doFilter(List<Integer> casesIds) {
				
				if (ListUtil.isEmpty(casesIds)) {
					return casesIds;
				}
				
				if (StringUtil.isEmpty(getDescription()) && StringUtil.isEmpty(getName()) && StringUtil.isEmpty(getPersonalId()) &&
						ArrayUtil.isEmpty(getStatuses()) && getDateFrom() == null && getDateTo() == null) {
					logger.log(Level.INFO, "None of criterias are defined, not filtering by it!");
					return casesIds;
				}
				
//				logger.log(Level.INFO, "IDs BEFORE filtering by criterias: " + casesIds);
					
				IWContext iwc = CoreUtil.getIWContext();
				CasesBusiness casesBusiness = getCasesBusiness();
				
				String description = getDescription() == null ? null : getDescription().toLowerCase(iwc.getCurrentLocale());
				
				Collection<Case> cases = null;
				try {
					cases = casesBusiness.getCasesByCriteria(null, description, getName(), getPersonalId(), getStatuses(), getDateFrom(),
							getDateTo(), null, null, false, CasesRetrievalManager.CASE_LIST_TYPE_USER.equals(getCaseListType()));
				}
				catch (RemoteException e) {
					e.printStackTrace();
				}

				List<Integer> casesByCriteria = null;
				if (cases != null && ListUtil.isEmpty(cases)) {
					logger.log(Level.INFO, new StringBuilder("No cases found by criterias: description: ").append(getDescription()).append(", name: ")
							.append(getName()).append(", personalId: ").append(getPersonalId()).append(", statuses: ").append(getStatuses())
							.append(", dateRange: ").append(getDateRange())
					.toString());
//					casesIds.clear();
				}
				else {	
					casesByCriteria = getCasesIds(cases);
					logger.log(Level.INFO, "Cases by criterias: " + casesByCriteria);
					
//					casesIds.retainAll(casesByCriteria);
				}
				casesIds = getNarrowedResults(casesIds, casesByCriteria);
				
//				logger.log(Level.INFO, "IDs AFTER filtering by criterias: " + casesIds);
				return casesIds;
			}
		};
	}
	
	public CasesListSearchFilter getContactFilter() {
		
		return new CasesListSearchFilter() {

			public List<Integer> doFilter(List<Integer> casesIds) {
				
				if (ListUtil.isEmpty(casesIds)) {
					return casesIds;
				}
				String contact = getContact();
				if (StringUtil.isEmpty(contact)) {
					logger.log(Level.INFO, "Contact is undefined, not filtering by it!");
					return casesIds;
				}
				
//				logger.log(Level.INFO, "IDs BEFORE filtering by contact (" + contact + "): " + casesIds);
				
				List<Integer> casesByContact = getCasesByContactQuery(CoreUtil.getIWContext(), contact);	
				if (ListUtil.isEmpty(casesByContact)) {
					logger.log(Level.INFO, "No BPM cases found by contact: " + contact);
//					casesIds.clear();
				}
				else {
					logger.log(Level.INFO, "Found BPM cases by contact: " + contact);
//					casesIds.retainAll(casesByContact);
				}
				casesIds = getNarrowedResults(casesIds, casesByContact);
				
//				logger.log(Level.INFO, "IDs AFTER filtering by contact (" + contact + "): " + casesIds);
				return casesIds;
			}
			
		};
	}
	
	public CasesListSearchFilter getProcessFilter() {
		
		return new CasesListSearchFilter() {

			public List<Integer> doFilter(List<Integer> casesIds) {
				
				if (ListUtil.isEmpty(casesIds)) {
					return casesIds;
				}
					
				String processDefinitionId = getProcessId();
				if (StringUtil.isEmpty(processDefinitionId)) {
					logger.log(Level.INFO, "Process definition id is undefined, not filtering by it!");
					return casesIds;
				}
				
//				logger.log(Level.INFO, "IDs BEFORE filtering by process definition (" + processDefinitionId + "): " + casesIds);
				
				List<Integer> casesByProcessDefinition = null;
				if (CasesConstants.GENERAL_CASES_TYPE.equals(processDefinitionId)) {
					//	Getting ONLY none "BPM" cases
					try {
						casesByProcessDefinition = getCasesBusiness().getFilteredProcesslessCasesIds(casesIds, CasesRetrievalManager.CASE_LIST_TYPE_USER.equals(getCaseListType()));
					}
					catch (RemoteException e) {
						e.printStackTrace();
					}
				}
				else {
					//	Getting "BPM" cases
					casesByProcessDefinition = getConvertedFromLongs(getCasesByProcessDefinition(processDefinitionId, getProcessVariables()));
				}
				
				if (ListUtil.isEmpty(casesByProcessDefinition)) {
					logger.log(Level.INFO, "No cases found by process definition id: " + processDefinitionId);
//					casesIds.clear();
				}
				else {
					logger.log(Level.INFO, "Cases by process definition (" + processDefinitionId + "): " + casesByProcessDefinition);
//					casesIds.retainAll(casesByProcessDefinition);
				}
				casesIds = getNarrowedResults(casesIds, casesByProcessDefinition);
				
//				logger.log(Level.INFO, "IDs AFTER filtering by process definition (" + processDefinitionId + "): " + casesIds);
				return casesIds;
			}
		};
	}
	
	private List<Integer> getConvertedFromLongs(List<Long> values) {
		if (ListUtil.isEmpty(values)) {
			return null;
		}
		
		List<Integer> convertedValues = new ArrayList<Integer>();
		for (Object o: values) {
			if (o instanceof Long) {
				convertedValues.add(Integer.valueOf(((Long) o).intValue()));
			}
			else {
				logger.log(Level.WARNING, "Object is not type of Long: " + o);
			}
		}
		
		return convertedValues;
	}
	
	private List<Integer> getNarrowedResults(List<Integer> casesIds, List<Integer> filterResults) {
		if (ListUtil.isEmpty(casesIds)) {
			logger.log(Level.INFO, "There are no start data, emptying IDs");
			return null;
		}
		if (ListUtil.isEmpty(filterResults)) {
			logger.log(Level.INFO, "No results found, emptying IDs");
			return null;
		}
		
		Integer id = null;
		List<Integer> ids = new ArrayList<Integer>();
		for (Object o: filterResults) {
			if (o instanceof Integer) {
				id = (Integer) o;
				if (casesIds.contains(id)) {
//					logger.log(Level.INFO, "HAS ID: " + id);
					ids.add(id);
				}
//				else {
//					logger.log(Level.INFO, "NO ID: " + id + " in: " + casesIds);
//				}
			}
			else {
				logger.log(Level.WARNING, "ID is not type of Integer: " + o);
			}
		}
		
		return ids;
	}
	
	private CaseHome getCaseHome() {
		try {
			return (CaseHome) IDOLookup.getHome(Case.class);
		} catch (IDOLookupException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private List<Integer> getUserCasesByNumber(String number) {
		CaseHome caseHome = getCaseHome();
		if (caseHome == null) {
			return null;
		}
		
		Collection<Case> cases = null;
		try {
			cases = caseHome.findByCriteria(number, null, null, null, null, null, null, null, true);
		} catch (FinderException e) {
			e.printStackTrace();
		}
		
		return getCasesIds(cases);
	}
	
	private List<Integer> getCasesIds(Collection<Case> cases) {
		if (ListUtil.isEmpty(cases)) {
			return null;
		}
		
		Integer id = null;
		List<Integer> ids = new ArrayList<Integer>(cases.size());
		for (Case theCase: cases) {
			try {
				id = Integer.valueOf(theCase.getId());
			} catch(NumberFormatException e) {
				e.printStackTrace();
			}
			
			if (id != null && !ids.contains(id)) {
				ids.add(id);
			}
		}
		
		return ids;
	}
	
	private List<Integer> getGeneralCasesByNumber(String caseNumber) {
		GeneralCaseHome caseHome = null;
		try {
			caseHome = (GeneralCaseHome) IDOLookup.getHome(GeneralCase.class);
		} catch (IDOLookupException e) {
			e.printStackTrace();
		}
		if (caseHome == null) {
			return null;	//	Unable to search for general cases
		}
		
		Collection<Case> casesByNumber = null;
		try {
			casesByNumber = caseHome.getCasesByCriteria(caseNumber, null, null, null, null, null, null, null, false);
		} catch (FinderException e) {
			e.printStackTrace();
		}
		if (ListUtil.isEmpty(casesByNumber)) {
			return null;	//	No results
		}
		
		List<Integer> generalCases = new ArrayList<Integer>(casesByNumber.size());
		for (Case casse: casesByNumber) {
			try {
				generalCases.add(Integer.valueOf(casse.getId()));
			} catch(NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return generalCases;
	}
		
	private List<Long> getCasesByProcessDefinition(String processDefinitionId, List<BPMProcessVariable> variables) {
		
		if (StringUtil.isEmpty(processDefinitionId))
			return null;
		
		final Long procDefId;
		try {
			procDefId = Long.valueOf(processDefinitionId);
		} catch (NumberFormatException e) {
			logger.log(Level.SEVERE, "Process definition id provided ("+processDefinitionId+") was incorrect", e);
			return null;
		}
		
		List<Long> processDefinitionIds = new ArrayList<Long>(1);
		processDefinitionIds.add(procDefId);
		
		try {
			final ProcessDefinition processDefinition = getBpmFactory().getProcessManager(procDefId).getProcessDefinition(procDefId).getProcessDefinition();
			return getCasesBPMDAO().getCaseIdsByProcessDefinitionIdsAndNameAndVariables(processDefinitionIds, processDefinition.getName(), variables);
			
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Exception while resolving cases ids by process definition id and process name. Process definition id = "+
					processDefinitionId, e);
		}
		
		return null;
	}
	
	public void setCaseNumber(String caseNumber) {
		this.caseNumber = caseNumber;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPersonalId() {
		return personalId;
	}
	public void setPersonalId(String personalId) {
		this.personalId = personalId;
	}
	public String getProcessId() {
		return processId;
	}
	public void setProcessId(String processId) {
		this.processId = processId;
	}
	public String getStatusId() {
		return statusId;
	}
	public void setStatusId(String statusId) {
		this.statusId = statusId;
	}
	public String getDateRange() {
		return dateRange;
	}
	public void setDateRange(String dateRange) {
		this.dateRange = dateRange;
	}
	public String getCaseListType() {
		return caseListType;
	}
	public void setCaseListType(String caseListType) {
		this.caseListType = caseListType;
	}
	public String getContact() {
		return contact;
	}
	public void setContact(String contact) {
		this.contact = contact;
	}

	public List<CasesListSearchFilter> getFilters() {
		
		List<CasesListSearchFilter> filters = new ArrayList<CasesListSearchFilter>();
		
		filters.add(getCaseNumberFilter());
		filters.add(getGeneralCasesFilter());
		filters.add(getProcessFilter());
		filters.add(getContactFilter());
		
		return filters;
	}

	public CasesBPMDAO getCasesBPMDAO() {
		
		if(casesBPMDAO == null)
			ELUtil.getInstance().autowire(this);
		
		return casesBPMDAO;
	}

	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}

	public IWTimestamp getDateFrom() {
		
		if(dateFrom == null)
			parseDateString();
		
		return dateFrom;
	}

	public void setDateFrom(IWTimestamp dateFrom) {
		this.dateFrom = dateFrom;
	}

	public IWTimestamp getDateTo() {
		
		if(dateTo == null)
			parseDateString();
		
		return dateTo;
	}

	public void setDateTo(IWTimestamp dateTo) {
		this.dateTo = dateTo;
	}
	
	private void parseDateString() {
		
		Locale locale = IWContext.getCurrentInstance().getCurrentLocale();
		
		String dateRange = getDateRange();
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
				}
			}
		}
	}
	
	private CasesBusiness getCasesBusiness() {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), CasesBusiness.class);
		}
		catch (IBOLookupException e) {
			throw new IBORuntimeException(e);
		}
	}
	
	private UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(iwac, UserBusiness.class);
		} catch (IBOLookupException e) {
			throw new IBORuntimeException(e);
		}
	}
	
	private List<Integer> getCasesByContactQuery(IWContext iwc, String contact) {
		
		if (StringUtil.isEmpty(contact))
			return null;
		
		Collection<User> usersByContactInfo = getUserBusiness(iwc).getUsersByNameOrEmailOrPhone(contact);
		if (ListUtil.isEmpty(usersByContactInfo)) {
			return null;
		}

		List<Integer> casesByContactPerson = null;
		final List<Integer> casesByContact = new ArrayList<Integer>();
			
		for (User contactPerson: usersByContactInfo) {
			
			try {
				casesByContactPerson = getConvertedFromLongs(getCasesBPMDAO().getCaseIdsByProcessInstanceIds(getRolesManager().getProcessInstancesIdsForUser(iwc,
																																contactPerson, false)));
			} catch(Exception e) {
				logger.log(Level.SEVERE, "Error getting case IDs from contact query: " + contact, e);
			}
			
			if (!ListUtil.isEmpty(casesByContactPerson)) {
				for (Integer caseId: casesByContactPerson) {
					if (!casesByContact.contains(caseId)) {
						casesByContact.add(caseId);
					}
				}
			}
		}
		
		return casesByContact;
	}

	public RolesManager getRolesManager() {
		
		if(rolesManager == null)
			ELUtil.getInstance().autowire(this);
		
		return rolesManager;
	}

	public void setRolesManager(RolesManager rolesManager) {
		this.rolesManager = rolesManager;
	}

	public BPMFactory getBpmFactory() {
		
		if(bpmFactory == null)
			ELUtil.getInstance().autowire(this);
		
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	public String[] getStatuses() {
		return statuses;
	}

	public void setStatuses(String[] statuses) {
		this.statuses = statuses;
	}

	public boolean isUsePDFDownloadColumn() {
		return usePDFDownloadColumn;
	}

	public void setUsePDFDownloadColumn(boolean usePDFDownloadColumn) {
		this.usePDFDownloadColumn = usePDFDownloadColumn;
	}

	public boolean isAllowPDFSigning() {
		return allowPDFSigning;
	}

	public void setAllowPDFSigning(boolean allowPDFSigning) {
		this.allowPDFSigning = allowPDFSigning;
	}

	public boolean isShowStatistics() {
		return showStatistics;
	}

	public void setShowStatistics(boolean showStatistics) {
		this.showStatistics = showStatistics;
	}

	public List<BPMProcessVariable> getProcessVariables() {
		return processVariables;
	}

	public void setProcessVariables(List<BPMProcessVariable> processVariables) {
		this.processVariables = processVariables;
	}

	public boolean isHideEmptySection() {
		return hideEmptySection;
	}

	public void setHideEmptySection(boolean hideEmptySection) {
		this.hideEmptySection = hideEmptySection;
	}

	public boolean isShowCaseNumberColumn() {
		return showCaseNumberColumn;
	}

	public void setShowCaseNumberColumn(boolean showCaseNumberColumn) {
		this.showCaseNumberColumn = showCaseNumberColumn;
	}

	public boolean isShowCreationTimeInDateColumn() {
		return showCreationTimeInDateColumn;
	}

	public void setShowCreationTimeInDateColumn(boolean showCreationTimeInDateColumn) {
		this.showCreationTimeInDateColumn = showCreationTimeInDateColumn;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}
	
}
