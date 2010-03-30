package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.cases.data.CaseCategory;
import is.idega.idegaweb.egov.cases.data.CaseCategoryHome;
import is.idega.idegaweb.egov.cases.presentation.CasesStatistics;
import is.idega.idegaweb.egov.cases.util.CasesConstants;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.process.business.CaseManagersProvider;
import com.idega.block.process.presentation.beans.CasePresentation;
import com.idega.block.process.presentation.beans.CasesSearchCriteriaBean;
import com.idega.block.process.presentation.beans.CasesSearchResultsHolder;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.core.contact.data.Email;
import com.idega.core.contact.data.Phone;
import com.idega.data.IDOLookup;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.io.MemoryFileBuffer;
import com.idega.io.MemoryOutputStream;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.identity.RolesManager;
import com.idega.presentation.IWContext;
import com.idega.user.business.NoEmailFoundException;
import com.idega.user.business.NoPhoneFoundException;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.IOUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

@Scope("session")
@Service(CasesSearchResultsHolder.SPRING_BEAN_IDENTIFIER)
public class CasesSearchResultsHolderImpl implements CasesSearchResultsHolder {

	private static final Logger LOGGER = Logger.getLogger(CasesSearchResultsHolderImpl.class.getName());
	private static final short DEFAULT_CELL_WIDTH = (short) (40 * 256);
	
	private Map<String, Collection<CasePresentation>> cases = new HashMap<String, Collection<CasePresentation>>();
	private Map<String, CasesSearchCriteriaBean> criterias = new HashMap<String, CasesSearchCriteriaBean>();
	
	private MemoryFileBuffer memory;

	@Autowired
	private CaseManagersProvider caseManagersProvider;
	@Autowired
	private CasesBPMDAO casesBinder;
	@Autowired
	private BPMFactory bpmFactory;
	@Autowired
	private RolesManager rolesManager;
	@Autowired
	private VariableInstanceQuerier variablesQuerier;
	
	public void setSearchResults(String id, Collection<CasePresentation> cases, CasesSearchCriteriaBean criteriaBean) {
		this.cases.put(id, cases);
		this.criterias.put(id, criteriaBean);
	}

	public boolean doExport(String id) {
		Collection<CasePresentation> cases = this.cases.get(id);
		if (ListUtil.isEmpty(cases)) {
			return false;
		}
		
		memory = getExportedData(id);
		
		return memory == null ? false : true;
	}
	
	private String getSheetName(Locale locale, String processNameOrCategoryId) {
		if (locale == null || StringUtil.isEmpty(processNameOrCategoryId)) {
			return null;
		}
		
		Integer id = getNumber(processNameOrCategoryId);
		if (id == null) {
			return getCaseManagersProvider().getCaseManager().getProcessName(processNameOrCategoryId, locale);
		}
		
		return getCategoryName(locale, processNameOrCategoryId.equals(CasesStatistics.UNKOWN_CATEGORY_ID) ? null : getCaseCategory(id));
	}
	
	private CaseCategory getCaseCategory(Object primaryKey) {
		try {
			CaseCategoryHome caseHome = (CaseCategoryHome) IDOLookup.getHome(CaseCategory.class);
			return caseHome.findByPrimaryKey(primaryKey);
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting category by: " + primaryKey);
		}
		return null;
	}
	
	private String getCategoryName(Locale locale, CaseCategory caseCategory) {
		if (caseCategory == null) {
			return getResourceBundle(CasesConstants.IW_BUNDLE_IDENTIFIER).getLocalizedString(CasesStatistics.UNKOWN_CATEGORY_ID, "Unkown category");
		}
		
		return caseCategory.getLocalizedCategoryName(locale);
	}
		
	private String getCaseCreator(CasePresentation caze) {
		String name = null;
		if (caze.getOwner() != null) {
			name = caze.getOwner().getName();
		}
		if (StringUtil.isEmpty(name)) {
			name = getResourceBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getLocalizedString("cases.unknown_owner", "Unkown");
		}
		
		return name;
	}
	
	private String getCaseCreatorPersonalId(CasePresentation caze) {
		String personalId = null;
		if (caze.getOwner() != null) {
			personalId = caze.getOwner().getPersonalID();
		}
		if (StringUtil.isEmpty(personalId)) {
			personalId = getResourceBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getLocalizedString("cases.unknown_owner_personal_id", "Unkown");
		}
		
		return personalId;
	}
	
	private String getCaseCreatorEmail(CasePresentation theCase) {
		String emailAddress = null;
		
		User owner = theCase.getOwner();
		if (owner != null) {
			try {
				UserBusiness userBusiness = getUserBusiness();
				Email email = userBusiness.getUsersMainEmail(owner);
				emailAddress = email == null ? null : email.getEmailAddress();
			} catch (NoEmailFoundException e) {
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error getting main email for user: " + owner, e);
			}
		}
		
		return StringUtil.isEmpty(emailAddress) ?
				getResourceBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getLocalizedString("cases.unknown_owner_email", "Unkown") : emailAddress;
	}
	
	private UserBusiness getUserBusiness() {
		try {
			return IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), UserBusiness.class);
		} catch (IBOLookupException e) {
			LOGGER.log(Level.WARNING, "Error getting " + UserBusiness.class, e);
		}
		return null;
	}
	
	@Transactional(readOnly=true)
	private List<AdvancedProperty> getAvailableVariablesByProcessDefinition(Locale locale, String processDefinition, boolean isAdmin) {
		Collection<VariableInstanceInfo> variablesByProcessDefinition = null;
		try {
			variablesByProcessDefinition = getNumber(processDefinition) == null ? getVariablesQuerier().getVariablesByProcessDefinition(processDefinition) : null;
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting variables for process: " + processDefinition, e);
		}
		
		return getAvailableVariables(variablesByProcessDefinition, locale, isAdmin, false);
	}
	
	@Transactional(readOnly=true)
	private List<AdvancedProperty> getAvailableVariablesByProcessInstanceId(Locale locale, Long processInstanceId, boolean isAdmin) {
		Collection<VariableInstanceInfo> variablesByProcessInstance = null;
		try {
			variablesByProcessInstance = getVariablesQuerier().getFullVariablesByProcessInstanceId(processInstanceId);
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting variables for process instance: " + processInstanceId, e);
		}
		
		return getAvailableVariables(variablesByProcessInstance, locale, isAdmin, true);
	}
	
	@Transactional(readOnly=true)
	private List<AdvancedProperty> getAvailableVariables(Collection<VariableInstanceInfo> variables, Locale locale, boolean isAdmin, boolean useRealValue) {
		if (ListUtil.isEmpty(variables)) {
			return null;
		}
		
		BPMProcessVariablesBean variablesProvider = ELUtil.getInstance().getBean(BPMProcessVariablesBean.SPRING_BEAN_IDENTIFIER);
		return variablesProvider.getAvailableVariables(variables, locale, isAdmin, useRealValue);
	}
	
	private List<AdvancedProperty> createHeaders(HSSFSheet sheet, HSSFCellStyle bigStyle, Locale locale, String processName, boolean isAdmin,
			List<String> standardFieldsInfo) {
		IWResourceBundle iwrb = getResourceBundle(CasesConstants.IW_BUNDLE_IDENTIFIER);
		
		short cellIndexInRow = 0;
		
		sheet.setColumnWidth(cellIndexInRow++, DEFAULT_CELL_WIDTH);
		sheet.setColumnWidth(cellIndexInRow++, DEFAULT_CELL_WIDTH);
		sheet.setColumnWidth(cellIndexInRow++, DEFAULT_CELL_WIDTH);
		
		int cellRow = 0;
		short cellIndex = 0;
		
		//	Default header labels
		HSSFRow row = sheet.createRow(cellRow++);
		HSSFCell cell = row.createCell(cellIndex++);
		cell.setCellValue(iwrb.getLocalizedString("case_nr", "Case nr."));
		cell.setCellStyle(bigStyle);
		
		cell = row.createCell(cellIndex++);
		cell.setCellValue(iwrb.getLocalizedString("sender", "Sender"));
		cell.setCellStyle(bigStyle);
		
		cell = row.createCell(cellIndex++);
		cell.setCellValue(iwrb.getLocalizedString("personal_id", "Personal ID"));
		cell.setCellStyle(bigStyle);
		
		cell = row.createCell(cellIndex++);
		cell.setCellValue(iwrb.getLocalizedString("sender_e-mail", "E-mail"));
		cell.setCellStyle(bigStyle);
		
		if (!ListUtil.isEmpty(standardFieldsInfo)) {
			for (String standardFieldLabel: standardFieldsInfo) {
				cell = row.createCell(cellIndex++);
				cell.setCellValue(standardFieldLabel);
				cell.setCellStyle(bigStyle);
			}
		}
		
		List<AdvancedProperty> availableVariables = getAvailableVariablesByProcessDefinition(locale, processName, isAdmin);
		if (!ListUtil.isEmpty(availableVariables)) {
			for (AdvancedProperty variable: availableVariables) {
				sheet.setColumnWidth(cellIndexInRow++, DEFAULT_CELL_WIDTH);
				
				cell = row.createCell(cellIndex++);
				cell.setCellValue(variable.getValue());
				cell.setCellStyle(bigStyle);
			}
		}
		
		return availableVariables;
	}
	
	private Integer getNumber(String value) {
		if (StringUtil.isEmpty(value)) {
			return null;
		}
		
		try {
			return Integer.valueOf(value);
		} catch(Exception e) {}
		
		return null;
	}
	
	private void addVariables(List<AdvancedProperty> variablesByProcessDefinition, CasePresentation theCase, HSSFRow row, HSSFSheet sheet, HSSFCellStyle bigStyle,
			Locale locale, boolean isAdmin, short cellIndex, List<Integer> fileCellsIndexes, String localizedFileLabel) {
		if (ListUtil.isEmpty(variablesByProcessDefinition)) {
			return;
		}
		
		Long processInstanceId = null;
		try {
			processInstanceId = getCaseManagersProvider().getCaseManager().getProcessInstanceIdByCaseId(theCase.getId());
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting process instance for case: " + theCase);
		}
		if (processInstanceId == null) {
			return;
		}
		
		AdvancedProperty variable = null;
		List<AdvancedProperty> variablesByProcessInstance = getAvailableVariablesByProcessInstanceId(locale, processInstanceId, isAdmin);
		for (AdvancedProperty processVariable: variablesByProcessDefinition) {
			variable = getVariableByValue(variablesByProcessInstance, processVariable.getValue());
			row.createCell(cellIndex++).setCellValue(variable == null ? CoreConstants.EMPTY : variable.getId());
		}
	}
	
	private AdvancedProperty getVariableByValue(List<AdvancedProperty> variables, String value) {
		if (ListUtil.isEmpty(variables) || StringUtil.isEmpty(value)) {
			return null;
		}
		
		for (AdvancedProperty variable: variables) {
			if (value.equals(variable.getValue())) {
				return variable;
			}
		}
		
		return null;
	}
	
	private MemoryFileBuffer getExportedData(String id) {
		Map<String, List<CasePresentation>> casesByProcessDefinition = getCasesByProcessDefinition(id);
		if (casesByProcessDefinition == null || ListUtil.isEmpty(casesByProcessDefinition.values())) {
			return null;
		}
		
		MemoryFileBuffer memory = new MemoryFileBuffer();
		OutputStream streamOut = new MemoryOutputStream(memory);
		HSSFWorkbook workBook = new HSSFWorkbook();

		HSSFFont bigFont = workBook.createFont();
		bigFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		bigFont.setFontHeightInPoints((short) 13);
		HSSFCellStyle bigStyle = workBook.createCellStyle();
		bigStyle.setFont(bigFont);
		
		boolean isAdmin = false;
		List<CasePresentation> cases = null;
		Locale locale = null;
		IWContext iwc = CoreUtil.getIWContext();
		String fileNameLabel = "File name";
		if (iwc != null) {
			locale = iwc.getCurrentLocale();
			isAdmin = iwc.isSuperAdmin();
			fileNameLabel = getResourceBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getLocalizedString("cases_bpm.file_name", fileNameLabel);
		}
		if (locale == null) {
			locale = Locale.ENGLISH;
		}
		
		List<String> standardFieldsLabels = getStandardFieldsLabels(id, locale);
		
		for (String processName: casesByProcessDefinition.keySet()) {
			cases = casesByProcessDefinition.get(processName);
			
			HSSFSheet sheet = workBook.createSheet(StringHandler.shortenToLength(getSheetName(locale, processName), 30));
			List<AdvancedProperty> variablesByProcessDefinition = createHeaders(sheet, bigStyle, locale, processName, isAdmin, standardFieldsLabels);
			List<Integer> fileCellsIndexes = null;
			int rowNumber = 1;
			
			for (CasePresentation theCase: cases) {
				fileCellsIndexes = new ArrayList<Integer>();
				HSSFRow row = sheet.createRow(rowNumber++);
				short cellIndex = 0;

				//	Default header values
				row.createCell(cellIndex++).setCellValue(theCase.getCaseIdentifier());
				row.createCell(cellIndex++).setCellValue(getCaseCreator(theCase));
				row.createCell(cellIndex++).setCellValue(getCaseCreatorPersonalId(theCase));
				row.createCell(cellIndex++).setCellValue(getCaseCreatorEmail(theCase));
				
				if (!ListUtil.isEmpty(standardFieldsLabels)) {
					List<String> standardFieldsValues = getStandardFieldsValues(id, theCase);
					if (!ListUtil.isEmpty(standardFieldsValues)) {
						for (String standardFieldValue: standardFieldsValues) {
							row.createCell(cellIndex++).setCellValue(standardFieldValue);
						}
					}
				}
				
				//	Variable values
				addVariables(variablesByProcessDefinition, theCase, row, sheet, bigStyle, locale, isAdmin, cellIndex, fileCellsIndexes, fileNameLabel);
			}
		}
		
		try {
			workBook.write(streamOut);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error writing search results to Excel!", e);
			return null;
		} finally {
			IOUtil.closeOutputStream(streamOut);
		}
		
		return memory;
	}
	
	private List<String> getStandardFieldsLabels(String id, Locale locale) {
		CasesSearchCriteriaBean criteria = getSearchCriteria(id);
		if (criteria == null) {
			return null;
		}
		
		IWResourceBundle iwrb = null;
		List<String> labels = new ArrayList<String>();
		if (!StringUtil.isEmpty(criteria.getDescription())) {
			iwrb = getResourceBundle(locale, CasesConstants.IW_BUNDLE_IDENTIFIER);
			labels.add(iwrb.getLocalizedString("description", "Description"));
		}
		
		if (!StringUtil.isEmpty(criteria.getContact())) {
			iwrb = iwrb == null ? getResourceBundle(locale, CasesConstants.IW_BUNDLE_IDENTIFIER) : iwrb;
			labels.add(iwrb.getLocalizedString("contact", "Contact"));
		}
		
		if (!StringUtil.isEmpty(criteria.getStatusId())) {
			iwrb = iwrb == null ? getResourceBundle(locale, CasesConstants.IW_BUNDLE_IDENTIFIER) : iwrb;
			labels.add(iwrb.getLocalizedString("status", "Status"));
		}
		
		if (!StringUtil.isEmpty(criteria.getDateRange())) {
			iwrb = iwrb == null ? getResourceBundle(locale, CasesConstants.IW_BUNDLE_IDENTIFIER) : iwrb;
			labels.add(iwrb.getLocalizedString("date_range", "Date range"));
		}
		
		return ListUtil.isEmpty(labels) ? null : labels;
	}
	
	private List<String> getStandardFieldsValues(String id, CasePresentation theCase) {
		CasesSearchCriteriaBean criteria = getSearchCriteria(id);
		if (criteria == null) {
			return null;
		}
		
		List<String> values = new ArrayList<String>();
		if (!StringUtil.isEmpty(criteria.getDescription())) {
			values.add(theCase.getSubject());
		}
		
		if (!StringUtil.isEmpty(criteria.getContact())) {
			Collection<User> usersByContactInfo = null;
			UserBusiness userBusiness = getUserBusiness();
			if (userBusiness != null) {
				try {
					usersByContactInfo = userBusiness.getUsersByNameOrEmailOrPhone(criteria.getContact());
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Error getting users by name, email or phone by fraze: " + criteria.getContact(), e);
				}
			}
			
			if (ListUtil.isEmpty(usersByContactInfo)) {
				values.add(CoreConstants.MINUS);
			} else {
				StringBuilder info = new StringBuilder();
				for (Iterator<User> usersIter = usersByContactInfo.iterator(); usersIter.hasNext();) {
					User user = usersIter.next();
					
					if (isUserConnectedToCase(user, theCase)) {
						info.append(user.getName());
						
						Email email = null;
						try {
							email = userBusiness.getUsersMainEmail(user);
						} catch (NoEmailFoundException e) {
						} catch (Exception e) {
							LOGGER.log(Level.WARNING, "Error getting main email for: " + user, e);
						}
						String emailAddress = email == null ? null : email.getEmailAddress();
						if (!StringUtil.isEmpty(emailAddress)) {
							info.append(CoreConstants.SPACE).append(emailAddress);
						}
						
						Phone workPhone = null;
						try {
							workPhone = userBusiness.getUsersWorkPhone(user);
						} catch (NoPhoneFoundException e) {
						} catch (Exception e) {
							LOGGER.log(Level.WARNING, "Error getting work phone for user: " + user, e);
						}
						String workPhoneNumber = workPhone == null ? null : workPhone.getNumber();
						if (!StringUtil.isEmpty(workPhoneNumber)) {
							info.append(CoreConstants.SPACE).append(workPhoneNumber);
						}
						
						if (usersIter.hasNext()) {
							info.append(CoreConstants.COMMA);
						}
					}
				}
				values.add(StringUtil.isEmpty(info.toString()) ? CoreConstants.MINUS : info.toString());
			}
		}
		
		if (!StringUtil.isEmpty(criteria.getStatusId())) {
			values.add(theCase.getLocalizedStatus());
		}
		
		if (!StringUtil.isEmpty(criteria.getDateRange())) {
			values.add(criteria.getDateRange());
		}
		
		return ListUtil.isEmpty(values) ? null : values;
	}
	
	private boolean isUserConnectedToCase(User user, CasePresentation theCase) {
		List<Long> ids = null;
		try {
			ids = getCasesBinder().getCaseIdsByProcessInstanceIds(getRolesManager().getProcessInstancesIdsForUser(null, user, false));
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "Error getting case IDs for user: " + user, e);
		}
		
		if (ListUtil.isEmpty(ids)) {
			return false;
		}
		
		for (Object id: ids) {
			if (theCase.getId().equals(id.toString())) {
				return true;
			}
		}
		
		return false;
	}
	
	private IWResourceBundle getResourceBundle(Locale locale, String bundleIdentifier) {
		return IWMainApplication.getDefaultIWMainApplication().getBundle(bundleIdentifier).getResourceBundle(locale);
	}
	
	private IWResourceBundle getResourceBundle(String bundleIdentifier) {
		IWContext iwc = CoreUtil.getIWContext();
		try {
			return iwc.getIWMainApplication().getBundle(bundleIdentifier).getResourceBundle(iwc);
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting resource bundle for: " + bundleIdentifier, e);
		}
		return null;
	}

	public MemoryFileBuffer getExportedSearchResults(String id) {
		if (memory != null) {
			return memory;
		}
		
		if (doExport(id)) {
			return memory;
		}
		
		return null;
	}

	public boolean isSearchResultStored(String id) {
		Collection<CasePresentation> cases = this.cases.get(id);
		return ListUtil.isEmpty(cases) ? Boolean.FALSE : Boolean.TRUE;
	}
	
	private Map<String, List<CasePresentation>> getCasesByProcessDefinition(String id) {
		if (!isSearchResultStored(id)) {
			return null;
		}
		
		Collection<CasePresentation> cases = this.cases.get(id);
		
		boolean putToMap = false;
		Map<String, List<CasePresentation>> casesByCategories = new HashMap<String, List<CasePresentation>>();
		for (CasePresentation theCase: cases) {
			String processName = theCase.isBpm() ? theCase.getProcessName() : theCase.getCategoryId();
			putToMap = false;
			
			if (StringUtil.isEmpty(processName)) {
				processName = CasesStatistics.UNKOWN_CATEGORY_ID;
			}
		
			List<CasePresentation> casesByProcessDefinition = casesByCategories.get(processName);
			if (ListUtil.isEmpty(casesByProcessDefinition)) {
				casesByProcessDefinition = new ArrayList<CasePresentation>();
			}
			if (!casesByProcessDefinition.contains(theCase)) {
				casesByProcessDefinition.add(theCase);
				putToMap = true;
			}
			if (putToMap) {
				casesByCategories.put(processName, casesByProcessDefinition);
			}
		}
		
		return casesByCategories;
	}

	public CasesBPMDAO getCasesBinder() {
		return casesBinder;
	}

	public void setCasesBinder(CasesBPMDAO casesBinder) {
		this.casesBinder = casesBinder;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	public CaseManagersProvider getCaseManagersProvider() {
		return caseManagersProvider;
	}

	public void setCaseManagersProvider(CaseManagersProvider caseManagersProvider) {
		this.caseManagersProvider = caseManagersProvider;
	}
	
	public Integer getNextCaseId(String id, Integer currentId, String processDefinitionName) {
		if (currentId == null || !isSearchResultStored(id)) {
			LOGGER.info("Unkown current case's id or no search results stored!");
			return null;
		}
		
		Collection<CasePresentation> cases = this.cases.get(id);
		CasePresentation nextCase = null;
		
		if (!StringUtil.isEmpty(processDefinitionName)) {
			List<CasePresentation> casesFromTheSameProcessDefinition = new ArrayList<CasePresentation>();
			for (CasePresentation theCase: cases) {
				if (processDefinitionName.equals(theCase.getProcessName())) {
					casesFromTheSameProcessDefinition.add(theCase);
				}
			}
			cases = casesFromTheSameProcessDefinition;
		}
		
		for (Iterator<CasePresentation> casesIter = cases.iterator(); (casesIter.hasNext() && nextCase == null);) {
			nextCase = casesIter.next();
			
			if (nextCase.getPrimaryKey().intValue() == currentId.intValue() && casesIter.hasNext()) {
				nextCase = casesIter.next();
			}
			else {
				nextCase = null;
			}
		}
	
		LOGGER.info("Next case id: " + (nextCase == null ? "unknown" : nextCase.getPrimaryKey()) + " in: " + cases + ", for current case: " + currentId);
		return nextCase == null ? null : nextCase.getPrimaryKey();
	}
	
	public Integer getNextCaseId(String id, Integer currentId) {
		return getNextCaseId(id, currentId, null);
	}

	public boolean clearSearchResults(String id) {
		setSearchResults(id, null, null);
		return Boolean.TRUE;
	}

	public Collection<CasePresentation> getSearchResults(String id) {
		return cases.get(id);
	}
	
	public CasesSearchCriteriaBean getSearchCriteria(String id) {
		return criterias.get(id);
	}

	public RolesManager getRolesManager() {
		return rolesManager;
	}

	public void setRolesManager(RolesManager rolesManager) {
		this.rolesManager = rolesManager;
	}

	public VariableInstanceQuerier getVariablesQuerier() {
		return variablesQuerier;
	}

	public void setVariablesQuerier(VariableInstanceQuerier variablesQuerier) {
		this.variablesQuerier = variablesQuerier;
	}

}