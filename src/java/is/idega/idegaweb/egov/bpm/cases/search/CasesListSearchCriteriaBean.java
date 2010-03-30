package is.idega.idegaweb.egov.bpm.cases.search;

import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;
import is.idega.idegaweb.egov.cases.data.GeneralCaseHome;
import is.idega.idegaweb.egov.cases.util.CasesConstants;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.FinderException;

import org.jbpm.graph.def.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.process.business.CasesRetrievalManager;
import com.idega.block.process.data.Case;
import com.idega.block.process.data.CaseHome;
import com.idega.block.process.presentation.beans.CasesSearchCriteriaBean;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.data.IDOLookup;
import com.idega.data.IDOLookupException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.bean.BPMProcessVariable;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.identity.RolesManager;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

public class CasesListSearchCriteriaBean extends CasesSearchCriteriaBean {
	
	private static final long serialVersionUID = 8071978111646904945L;

	private static final Logger LOGGER = Logger.getLogger(CasesListSearchCriteriaBean.class.getName());
	
	private String processId;
	private String caseListType;
	
	private List<BPMProcessVariable> processVariables;
	
	@Autowired
	private CasesBPMDAO casesBPMDAO;
	@Autowired
	private RolesManager rolesManager;
	@Autowired
	private BPMFactory bpmFactory;
	
	private boolean usePDFDownloadColumn = true;
	private boolean allowPDFSigning = true;
	private boolean showStatistics;
	private boolean hideEmptySection;
	private boolean showCaseNumberColumn = true;
	private boolean showCreationTimeInDateColumn = true;
	private boolean onlySubscribedCases;
	
	private String caseCodes;
	private String statusesToShow;
	private String statusesToHide;
	
	public CasesListSearchFilter getCaseNumberFilter() {
		return new DefaultCasesListSearchFilter() {
			public List<Integer> doFilter(List<Integer> casesIds) {
				startFiltering();
				try {
					if (ListUtil.isEmpty(casesIds)) {
						return casesIds;
					}
					
					String caseNumber = getCaseNumber();
					if (StringUtil.isEmpty(caseNumber)) {
						LOGGER.log(Level.INFO, "Case number is undefined, not filtering by it!");
						return casesIds;
					}
					
					String loweredCaseNumber = caseNumber.toLowerCase(CoreUtil.getIWContext().getCurrentLocale());
					List<Integer> casesByNumberIds = new ArrayList<Integer>();
					
					//	"BPM" cases
					List<Long> bpmCases = null;
					try {
						bpmCases = getCasesBPMDAO().getCaseIdsByCaseNumber(loweredCaseNumber);
					} catch(Exception e) {
						LOGGER.log(Level.WARNING, "Exception while resolving case ids by case number = " + loweredCaseNumber, e);
					}
					if (ListUtil.isEmpty(bpmCases)) {
						LOGGER.log(Level.INFO, "No BPM cases found by number: " + caseNumber);
					}
					else {
						LOGGER.log(Level.INFO, "BPM cases by number (" + caseNumber + "): " + bpmCases);
						casesByNumberIds.addAll(getConvertedFromLongs(bpmCases));
					}
					
					//	Old cases
					List<Integer> simpleCases = CasesRetrievalManager.CASE_LIST_TYPE_USER.equals(getCaseListType()) ? getUserCasesByNumber(loweredCaseNumber) :
						getGeneralCasesByNumber(loweredCaseNumber);
					if (ListUtil.isEmpty(simpleCases)) {
						LOGGER.log(Level.INFO, "No simple cases found by number: " + caseNumber);
					} else {
						LOGGER.log(Level.INFO, "Simple cases by number (" + caseNumber + "): " + simpleCases);
						for (Integer id: simpleCases) {
							if (!casesByNumberIds.contains(id)) {
								casesByNumberIds.add(id);
							}
						}
					}
					
					if (ListUtil.isEmpty(casesByNumberIds)) {
						LOGGER.log(Level.INFO, "No cases found by number: " + caseNumber);
					} else {
						LOGGER.log(Level.INFO, "Cases found by number (" + caseNumber + "): " + casesByNumberIds);
					}
					casesIds = getNarrowedResults(casesIds, casesByNumberIds);
					
					return casesIds;
				} finally {
					endFiltering("Looking for cases by number: " + getCaseNumber());
				}
			}
		};
	}
	
	public CasesListSearchFilter getGeneralCasesFilter() {
		return new DefaultCasesListSearchFilter() {
			public List<Integer> doFilter(List<Integer> casesIds) {
				startFiltering();
				try {
					if (ListUtil.isEmpty(casesIds)) {
						return casesIds;
					}
					
					if (StringUtil.isEmpty(getDescription()) && StringUtil.isEmpty(getName()) && StringUtil.isEmpty(getPersonalId()) &&
							ArrayUtil.isEmpty(getStatuses()) && getDateFrom() == null && getDateTo() == null) {
						LOGGER.log(Level.INFO, "None of general criterias (description, name, personal ID, statuses, dates) are defined, not filtering by it!");
						return casesIds;
					}
						
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
						LOGGER.log(Level.INFO, new StringBuilder("No cases found by criterias: description: ").append(getDescription()).append(", name: ")
								.append(getName()).append(", personalId: ").append(getPersonalId()).append(", statuses: ").append(getStatuses())
								.append(", dateRange: ").append(getDateRange())
						.toString());
					}
					else {	
						casesByCriteria = getCasesIds(cases);
						LOGGER.log(Level.INFO, "Cases by criterias: " + casesByCriteria);
					}
					casesIds = getNarrowedResults(casesIds, casesByCriteria);
					
					return casesIds;
				} finally {
					endFiltering("Looking for cases by criteria: " + getDescription());
				}
			}
		};
	}
	
	public CasesListSearchFilter getContactFilter() {
		return new DefaultCasesListSearchFilter() {
			public List<Integer> doFilter(List<Integer> casesIds) {
				startFiltering();
				try {
					if (ListUtil.isEmpty(casesIds)) {
						return casesIds;
					}
					
					String contact = getContact();
					if (StringUtil.isEmpty(contact)) {
						return casesIds;
					}
					
					List<Integer> casesByContact = getCasesByContactQuery(CoreUtil.getIWContext(), contact);	
					if (ListUtil.isEmpty(casesByContact)) {
						LOGGER.log(Level.INFO, "No BPM cases found by contact: " + contact);
					}
					else {
						LOGGER.log(Level.INFO, "Found BPM cases by contact: " + contact);
					}
					casesIds = getNarrowedResults(casesIds, casesByContact);
					
					return casesIds;
				} finally {
					endFiltering("Looking for cases by contact: " + getContact());
				}
			}
		};
	}
	
	public CasesListSearchFilter getProcessFilter() {
		return new DefaultCasesListSearchFilter() {
			public List<Integer> doFilter(List<Integer> casesIds) {
				startFiltering();
				try {
					if (ListUtil.isEmpty(casesIds)) {
						return casesIds;
					}
						
					String processDefinitionId = getProcessId();
					if (StringUtil.isEmpty(processDefinitionId)) {
						return casesIds;
					}
					
					List<Integer> casesByProcessDefinition = null;
					if (CasesConstants.GENERAL_CASES_TYPE.equals(processDefinitionId)) {
						//	Getting ONLY none "BPM" cases
						try {
							casesByProcessDefinition = getCasesBusiness().getFilteredProcesslessCasesIds(casesIds,
									CasesRetrievalManager.CASE_LIST_TYPE_USER.equals(getCaseListType()));
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
						LOGGER.log(Level.INFO, "No cases found by process definition id: " + processDefinitionId);
					}
					else {
						LOGGER.log(Level.INFO, "Found cases by process definition (" + processDefinitionId + "): " + casesByProcessDefinition);
					}
					casesIds = getNarrowedResults(casesIds, casesByProcessDefinition);
					
					return casesIds;
				} finally {
					endFiltering("Looking for cases by process definition: " + getProcessId());
				}
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
				LOGGER.log(Level.WARNING, "Object is not type of Long: " + o);
			}
		}
		
		return convertedValues;
	}
	
	private List<Integer> getNarrowedResults(List<Integer> casesIds, List<Integer> filterResults) {
		if (ListUtil.isEmpty(casesIds)) {
			LOGGER.info("There are no start data, emptying IDs");
			return null;
		}
		if (ListUtil.isEmpty(filterResults)) {
			LOGGER.info("No results found, emptying IDs");
			return null;
		}
		
		Integer id = null;
		List<Integer> ids = new ArrayList<Integer>();
		for (Object o: filterResults) {
			if (o instanceof Integer) {
				id = (Integer) o;
				if (casesIds.contains(id)) {
					ids.add(id);
				}
			}
			else {
				LOGGER.log(Level.WARNING, "ID is not type of Integer: " + o);
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
			casesByNumber = caseHome.getCasesByCriteria(caseNumber, null, null, null, null, null, null, null, true);
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
			LOGGER.log(Level.SEVERE, "Process definition id provided ("+processDefinitionId+") was incorrect", e);
			return null;
		}
		
		List<Long> processDefinitionIds = new ArrayList<Long>(1);
		processDefinitionIds.add(procDefId);
		
		try {
			final ProcessDefinition processDefinition = getBpmFactory().getProcessManager(procDefId).getProcessDefinition(procDefId).getProcessDefinition();
			return getCasesBPMDAO().getCaseIdsByProcessDefinitionIdsAndNameAndVariables(processDefinitionIds, processDefinition.getName(), variables);
			
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "Exception while resolving cases ids by process definition id and process name. Process definition id = "+
					processDefinitionId, e);
		}
		
		return null;
	}
	
	public String getProcessId() {
		return processId;
	}
	public void setProcessId(String processId) {
		this.processId = processId;
	}
	public String getCaseListType() {
		return caseListType;
	}
	public void setCaseListType(String caseListType) {
		this.caseListType = caseListType;
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
		if (casesBPMDAO == null)
			ELUtil.getInstance().autowire(this);
		
		return casesBPMDAO;
	}

	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}

	private CasesBusiness getCasesBusiness() {
		try {
			return IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), CasesBusiness.class);
		}
		catch (IBOLookupException e) {
			throw new IBORuntimeException(e);
		}
	}
	
	private UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return IBOLookup.getServiceInstance(iwac, UserBusiness.class);
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
				LOGGER.log(Level.SEVERE, "Error getting case IDs from contact query: " + contact, e);
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
		if (rolesManager == null)
			ELUtil.getInstance().autowire(this);
		
		return rolesManager;
	}

	public void setRolesManager(RolesManager rolesManager) {
		this.rolesManager = rolesManager;
	}

	public BPMFactory getBpmFactory() {
		if (bpmFactory == null)
			ELUtil.getInstance().autowire(this);
		
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
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

	public List<String> getStatusesToShowInList() {
		return statusesToShow == null ? null : StringUtil.getValuesFromString(statusesToShow, CoreConstants.COMMA);
	}

	public void setStatusesToShow(String statusesToShow) {
		this.statusesToShow = statusesToShow;
	}

	public List<String> getStatusesToHideInList() {
		return statusesToHide == null ? null : StringUtil.getValuesFromString(statusesToHide, CoreConstants.COMMA);
	}
	
	public void setStatusesToHide(String statusesToHide) {
		this.statusesToHide = statusesToHide;
	}

	public String getStatusesToShow() {
		return statusesToShow;
	}

	public String getStatusesToHide() {
		return statusesToHide;
	}
	
	public List<String> getCaseCodesInList() {
		return caseCodes == null ? null : StringUtil.getValuesFromString(caseCodes, CoreConstants.COMMA);
	}

	public void setCaseCodes(String caseCodes) {
		this.caseCodes = caseCodes;
	}

	public String getCaseCodes() {
		return caseCodes;
	}

	public boolean isOnlySubscribedCases() {
		return onlySubscribedCases;
	}

	public void setOnlySubscribedCases(boolean onlySubscribedCases) {
		this.onlySubscribedCases = onlySubscribedCases;
	}

	@Override
	public String toString() {
		return new StringBuilder("Case number: " + getCaseNumber()).append("\n")
			.append("Description: " + getDescription()).append("\n")
			.append("Name: " + getName()).append("\n")
			.append("Personal ID: " + getPersonalId()).append("\n")
			.append("Process ID: " + processId).append("\n")
			.append("Status ID: " + getStatusId()).append("\n")
			.append("Date range: " + getDateRange()).append("\n")
			.append("Case list type: " + caseListType).append("\n")
			.append("Contact: " + getContact()).append("\n")
			.append("Date from: " + getDateFrom()).append("\n")
			.append("Date to: " + getDateTo()).append("\n")
			.append("Statuses: " + getStatuses()).append("\n")
			.append("Case codes: " + caseCodes).append("\n")
			.append("Statuses to show: " + statusesToShow).append("\n")
			.append("Statuses to hide: " + statusesToHide).append("\n")
		.toString();
	}
	
	private abstract class DefaultCasesListSearchFilter implements CasesListSearchFilter {
		private long start;
		private long end;
		private boolean measure;
		
		public void endFiltering(String info) {
			if (!measure) {
				return;
			}
			
			end = System.currentTimeMillis();
			LOGGER.info("Query (" + info + ") executed in: " + (end - start) + " ms");
		}

		public void startFiltering() {
			measure = CoreUtil.isSQLMeasurementOn();
			if (measure) {
				start = System.currentTimeMillis();
			}
		}
		
	}
}