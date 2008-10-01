package is.idega.idegaweb.egov.bpm.cases.search;

import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.util.CasesConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.graph.def.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.process.data.Case;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.identity.RolesManager;
import com.idega.presentation.IWContext;
import com.idega.presentation.ui.handlers.IWDatePickerHandler;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

public class CasesListSearchCriteriaBean {
	
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
	@Autowired private CasesBPMDAO casesBPMDAO;
	@Autowired private RolesManager rolesManager;
	@Autowired private BPMFactory bpmFactory;
	
	private boolean usePDFDownloadColumn = true;
	private boolean allowPDFSigning = true;
	
	public String getCaseNumber() {
		return caseNumber;
	}
	
	public CasesListSearchFilter getCaseNumberFilter() {
		
		return new CasesListSearchFilter() {

			public List<Integer> doFilter(List<Integer> casesIds) {
				
				if(casesIds != null && !casesIds.isEmpty()) {

					String caseNumber = getCaseNumber();
					
					if (!StringUtil.isEmpty(caseNumber)) {
						
						Locale locale = IWContext.getCurrentInstance().getCurrentLocale();
						
						try {
							List<Integer> casesByNumber = getCasesBPMDAO().getCaseIdsByCaseNumber(caseNumber.toLowerCase(locale));
							
							if(casesByNumber != null && !casesByNumber.isEmpty())
								casesIds.retainAll(casesByNumber);
							
						} catch(Exception e) {
							Logger.getLogger(getClass().getName()).log(Level.WARNING, "Exception while resolving case ids by case number = "+caseNumber, e);
						}
					}
				}
				
				return casesIds;
			}
			
		};
	}
	
	/**
	 * TODO: non process cases are not searched by case nr. Implement that in casenumberfilter perhaps
	 * @return
	 */
	public CasesListSearchFilter getGeneralCasesFilter() {
		
		return new CasesListSearchFilter() {

			public List<Integer> doFilter(List<Integer> casesIds) {
				
				if(casesIds != null && !casesIds.isEmpty()) {
					
					if(!StringUtil.isEmpty(getDescription()) || !StringUtil.isEmpty(getName()) 
							|| !StringUtil.isEmpty(getPersonalId()) || getStatuses() != null || getDateFrom() != null || getDateTo() != null) {
					
						
						IWContext iwc = IWContext.getCurrentInstance();
						CasesBusiness casesBusiness = getCasesBusiness(iwc);
						
						String description = getDescription() != null ? getDescription().toLowerCase(iwc.getCurrentLocale()) : null;
						
						Collection<Case> cases = casesBusiness.getCasesByCriteria(null, description, getName(), getPersonalId(), getStatuses(), getDateFrom(), getDateTo(),
								null, null, false);
						
						if(cases != null) {
							
							ArrayList<Integer> casesByCriteria = new ArrayList<Integer>(cases.size());
							
							for (Case cs : cases) {
								
								casesByCriteria.add(new Integer(cs.getPrimaryKey().toString()));
							}
							
							casesIds.retainAll(casesByCriteria);
						} else
							casesIds.clear();
					}
				}
				
				return casesIds;
			}
			
		};
	}
	
	public CasesListSearchFilter getContactFilter() {
		
		return new CasesListSearchFilter() {

			public List<Integer> doFilter(List<Integer> casesIds) {
				
				if(casesIds != null && !casesIds.isEmpty()) {
					
					IWContext iwc = IWContext.getCurrentInstance();
					
					String contact = getContact();

					if (!StringUtil.isEmpty(contact)) {
					
						List<Integer> casesByContact = getCasesByContactQuery(iwc, contact);
						
						if(casesByContact != null && !casesByContact.isEmpty())
							casesIds.retainAll(casesByContact);
					}
				}
				
				return casesIds;
			}
			
		};
	}
	
	public CasesListSearchFilter getProcessFilter() {
		
		return new CasesListSearchFilter() {

			public List<Integer> doFilter(List<Integer> casesIds) {
				
				if(casesIds != null && !casesIds.isEmpty()) {
					
					String processDefinitionId = getProcessId();
					
					if (!CasesConstants.GENERAL_CASES_TYPE.equals(processDefinitionId)) {
						
						if (!StringUtil.isEmpty(processDefinitionId)) {
						
							List<Integer> casesByProcessDefinition = getCasesByProcessDefinition(processDefinitionId);
							
							if (!ListUtil.isEmpty(casesByProcessDefinition)) {
							
								casesIds.retainAll(casesByProcessDefinition);
							}
						}
					}
				}
				
				return casesIds;
			}
		};
	}
		
	private List<Integer> getCasesByProcessDefinition(String processDefinitionId) {
		
		if (StringUtil.isEmpty(processDefinitionId))
			return null;
		
		final Long procDefId;
		try {
			procDefId = Long.valueOf(processDefinitionId);
		} catch (NumberFormatException e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Process definition id provided ("+processDefinitionId+") was incorrect", e);
			return null;
		}
		
		List<Long> processDefinitionIds = new ArrayList<Long>(1);
		processDefinitionIds.add(procDefId);
		
		try {
			final ProcessDefinition processDefinition = getBpmFactory().getProcessManager(procDefId).getProcessDefinition(procDefId).getProcessDefinition();
			return getCasesBPMDAO().getCaseIdsByProcessDefinitionIdsAndName(processDefinitionIds, processDefinition.getName());
			
		} catch(Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while resolving cases ids by process definition id and process name. Process definition id = "+processDefinitionId, e);
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
		
		ArrayList<CasesListSearchFilter> filters = new ArrayList<CasesListSearchFilter>();
		
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
	
	private CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
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
		final List<Integer> casesByContact;
		
		if (!ListUtil.isEmpty(usersByContactInfo)) {

			List<Integer> casesByContactPerson = null;
			casesByContact = new ArrayList<Integer>();
			
			for (User contactPerson: usersByContactInfo) {
				
				try {
					casesByContactPerson = getCasesBPMDAO().getCaseIdsByProcessInstanceIds(getRolesManager().getProcessInstancesIdsForUser(iwc, contactPerson, false));
				} catch(Exception e) {
					Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error getting case IDs from contact query: " + contact, e);
				}
				
				if (!ListUtil.isEmpty(casesByContactPerson)) {

					for (Integer caseId: casesByContactPerson) {
						if (!casesByContact.contains(caseId)) {
							casesByContact.add(caseId);
						}
					}
				}
			}
		} else
			casesByContact = null;
		
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
	
}
