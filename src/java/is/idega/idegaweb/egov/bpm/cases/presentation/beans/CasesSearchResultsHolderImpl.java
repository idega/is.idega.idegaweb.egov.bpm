package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.cases.data.CaseCategory;
import is.idega.idegaweb.egov.cases.data.CaseCategoryHome;
import is.idega.idegaweb.egov.cases.presentation.CasesStatistics;
import is.idega.idegaweb.egov.cases.util.CasesConstants;

import java.io.OutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.process.business.CaseConstants;
import com.idega.block.process.business.CaseManagersProvider;
import com.idega.block.process.business.ProcessConstants;
import com.idega.block.process.presentation.beans.CasePresentation;
import com.idega.block.process.presentation.beans.CasesSearchCriteriaBean;
import com.idega.block.process.presentation.beans.CasesSearchResults;
import com.idega.block.process.presentation.beans.CasesSearchResultsHolder;
import com.idega.block.process.variables.VisibleVariablesBean;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.core.business.GeneralCompanyBusiness;
import com.idega.core.company.bean.GeneralCompany;
import com.idega.core.contact.data.Email;
import com.idega.core.contact.data.Phone;
import com.idega.data.IDOLookup;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.io.MemoryFileBuffer;
import com.idega.io.MemoryOutputStream;
import com.idega.jbpm.artifacts.presentation.ProcessArtifacts;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.variables.MultipleSelectionVariablesResolver;
import com.idega.presentation.IWContext;
import com.idega.user.business.NoEmailFoundException;
import com.idega.user.business.NoPhoneFoundException;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.IOUtil;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.WebUtil;
import com.idega.util.expression.ELUtil;

@Scope("session")
@Service(CasesSearchResultsHolder.SPRING_BEAN_IDENTIFIER)
public class CasesSearchResultsHolderImpl implements CasesSearchResultsHolder {

	private static final Logger LOGGER = Logger.getLogger(CasesSearchResultsHolderImpl.class.getName());
	private static final int DEFAULT_CELL_WIDTH = 40 * 256;

	private Map<String, CasesSearchResults> allResults = new HashMap<String, CasesSearchResults>();
	private Map<String, List<CasePresentation>> externalData = new HashMap<String, List<CasePresentation>>();

	private List<String> concatenatedData = new ArrayList<String>();

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
	@Autowired
	private ProcessArtifacts processArtifacts;

	@Autowired
	private VisibleVariablesBean visibleVariablesBean = null;

	protected VisibleVariablesBean getVisibleVariablesBean() {
		if (this.visibleVariablesBean == null) {
			ELUtil.getInstance().autowire(this);
		}

		return this.visibleVariablesBean;
	}

	protected String localizeBPM(String key, String value) {
		return getWebUtil().getLocalizedString(
				IWBundleStarter.IW_BUNDLE_IDENTIFIER, key, value);
	}

	protected String localizeCases(String key, String value) {
		return getWebUtil().getLocalizedString(
				CasesConstants.IW_BUNDLE_IDENTIFIER, key, value);
	}

	@Autowired
	private WebUtil webUtil = null;

	protected WebUtil getWebUtil() {
		if (this.webUtil == null) {
			ELUtil.getInstance().autowire(this);
		}

		return this.webUtil;
	}


	@Autowired(required = false)
	private GeneralCompanyBusiness generalCompanyBusiness;

	private GeneralCompanyBusiness getGeneralCompanyBusiness() {
		if (generalCompanyBusiness == null) {
			try {
				generalCompanyBusiness = ELUtil.getInstance().getBean(GeneralCompanyBusiness.BEAN_NAME);
			} catch (Exception e) {
				LOGGER.warning("There is no implementation for " + GeneralCompanyBusiness.class.getName());
			}
		}
		return generalCompanyBusiness;
	}

	@Override
	public void setSearchResults(String id, CasesSearchResults results) {
		allResults.put(id, results);
	}

	private Collection<CasePresentation> getCases(String id) {
		return getCases(id, false);
	}

	private Collection<CasePresentation> getCases(String id, boolean loadExternalData) {
		CasesSearchResults results = allResults.get(id);
		Collection<CasePresentation> data = results == null ? null : results.getCases();

		if (loadExternalData) {
			List<CasePresentation> externalData = this.externalData.get(id);

			if (externalData != null) {
				if (data == null) {
					return externalData;
				} else {
					List<CasePresentation> allData = new ArrayList<CasePresentation>(data);
					allData.addAll(externalData);
					return allData;
				}
			}
		}

		return data;
	}

	@Override
	public boolean doExport(String id,boolean exportContacts, boolean showCompany) {
		Collection<CasePresentation> cases = getCases(id, true);
		if (ListUtil.isEmpty(cases))
			return false;

		memory = getExportedData(id,exportContacts,showCompany);

		return memory == null ? false : true;
	}

	private String getSheetName(Locale locale, String processNameOrCategoryId) {
		if (locale == null || StringUtil.isEmpty(processNameOrCategoryId))
			return CoreConstants.MINUS;

		String sheetName = null;
		Integer id = getNumber(processNameOrCategoryId);
		if (id == null)
			sheetName = getCaseManagersProvider().getCaseManager().getProcessName(processNameOrCategoryId, locale);

		sheetName = getCategoryName(locale, processNameOrCategoryId.equals(CasesStatistics.UNKOWN_CATEGORY_ID) ? null : getCaseCategory(id));
		return StringUtil.isEmpty(sheetName) ? CoreConstants.MINUS : sheetName;
	}

	private CaseCategory getCaseCategory(Object primaryKey) {
		if (primaryKey == null)
			return null;

		try {
			CaseCategoryHome caseHome = (CaseCategoryHome) IDOLookup.getHome(CaseCategory.class);
			return caseHome.findByPrimaryKey(primaryKey);
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting category by: " + primaryKey);
		}
		return null;
	}

	private String getCategoryName(Locale locale, CaseCategory caseCategory) {
		if (caseCategory == null)
			return getResourceBundle(CasesConstants.IW_BUNDLE_IDENTIFIER).getLocalizedString(CasesStatistics.UNKOWN_CATEGORY_ID, "Unkown category");

		return caseCategory.getLocalizedCategoryName(locale);
	}

	private String getCaseCreator(CasePresentation theCase) {
		String name = null;
		if (theCase.getOwner() != null)
			name = theCase.getOwner().getName();

		if (StringUtil.isEmpty(name))
			name = getResourceBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getLocalizedString("cases.unknown_owner", "Unkown");

		return name;
	}

	private String getCaseCreatorPersonalId(CasePresentation theCase) {
		String personalId = null;
		if (theCase.getOwner() != null)
			personalId = theCase.getOwner().getPersonalID();

		if (StringUtil.isEmpty(personalId))
			personalId = getResourceBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getLocalizedString("cases.unknown_owner_personal_id", "Unkown");

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
			variablesByProcessDefinition = getNumber(processDefinition) == null ? getVariablesQuerier()
					.getVariablesByProcessDefinition(processDefinition) : null;
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting variables for process: " + processDefinition, e);
		}

		return getAvailableVariables(variablesByProcessDefinition, locale, isAdmin, false);
	}

	@Transactional(readOnly=true)
	private List<AdvancedProperty> getAvailableVariablesByProcessInstanceId(Locale locale, Long processInstanceId, boolean isAdmin) {
		Collection<VariableInstanceInfo> variablesByProcessInstance = null;
		try {
			variablesByProcessInstance = getVariablesQuerier().getFullVariablesByProcessInstanceId(processInstanceId, false);
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting variables for process instance: " + processInstanceId, e);
		}

		return getAvailableVariables(variablesByProcessInstance, locale, isAdmin, true);
	}

	@Transactional(readOnly=true)
	private List<AdvancedProperty> getAvailableVariables(Collection<VariableInstanceInfo> variables, Locale locale, boolean isAdmin,
			boolean useRealValue) {
		if (ListUtil.isEmpty(variables))
			return null;

		BPMProcessVariablesBean variablesProvider = ELUtil.getInstance().getBean(BPMProcessVariablesBean.SPRING_BEAN_IDENTIFIER);
		return variablesProvider.getAvailableVariables(variables, locale, isAdmin, useRealValue);
	}

	private void doCreateHeaders(HSSFSheet sheet, HSSFCellStyle bigStyle, List<String> columns, Locale locale) {
		IWResourceBundle iwrb = getResourceBundle(CasesConstants.IW_BUNDLE_IDENTIFIER);
		BPMProcessVariablesBean variablesBean = ELUtil.getInstance().getBean(BPMProcessVariablesBean.SPRING_BEAN_IDENTIFIER);

		short cellIndexInRow = 0;
		for (int i = 0; i < columns.size(); i++)
			sheet.setColumnWidth(cellIndexInRow++, DEFAULT_CELL_WIDTH);

		int cellRow = sheet.getLastRowNum()+2;
		if(cellRow == 2){// First row is 0
			cellRow = 0;
		}
		int cellIndex = 0;
		HSSFRow row = sheet.createRow(cellRow++);
		for (String column: columns) {
			HSSFCell cell = row.createCell(cellIndex++);

			String value = null;
			if (column.startsWith(CaseConstants.CASE_PREFIX)) {
				if (CaseConstants.CASE_IDENTIFIER.equals(column))
					value = iwrb.getLocalizedString("case_nr", "Case nr.");
				else if (CaseConstants.CASE_CREATION_DATE.equals(column))
					value = iwrb.getLocalizedString("created_date", "Created date");
				else if (CaseConstants.CASE_STATUS.equals(column))
					value = iwrb.getLocalizedString("status", "Status");
				else
					value = iwrb.getLocalizedString(column, column);
			} else
				value = variablesBean.getVariableLocalizedName(column, locale);
			cell.setCellValue(value);

			cell.setCellStyle(bigStyle);
		}
	}

	private void createHeaders(
			HSSFSheet sheet, 
			HSSFCellStyle bigStyle, 
			String processName, 
			boolean isAdmin,
			List<String> standardFieldsInfo,
			List<AdvancedProperty> availableVariables) {
		int cellRow = sheet.getLastRowNum()+2;
		if(cellRow == 2){// First row is 0
			cellRow = 0;
		}
		int cellIndex = 0;

		//	Default header labels
		HSSFRow row = sheet.createRow(cellRow++);
		HSSFCell cell = row.createCell(cellIndex++);
		cell.setCellValue(localizeCases("case_nr", "Case nr."));
		cell.setCellStyle(bigStyle);

		cell = row.createCell(cellIndex++);
		cell.setCellValue(localizeCases("status", "Status"));
		cell.setCellStyle(bigStyle);

		cell = row.createCell(cellIndex++);
		cell.setCellValue(localizeCases("sender", "Sender"));
		cell.setCellStyle(bigStyle);

		cell = row.createCell(cellIndex++);
		cell.setCellValue(localizeCases("personal_id", "Personal ID"));
		cell.setCellStyle(bigStyle);

		cell = row.createCell(cellIndex++);
		cell.setCellValue(localizeCases("sender_e-mail", "E-mail"));
		cell.setCellStyle(bigStyle);

		if (!ListUtil.isEmpty(standardFieldsInfo)) {
			for (String standardFieldLabel: standardFieldsInfo) {
				cell = row.createCell(cellIndex++);
				cell.setCellValue(standardFieldLabel);
				cell.setCellStyle(bigStyle);
			}
		}

		//	Labels of variables
		if (!ListUtil.isEmpty(availableVariables)) {
			for (AdvancedProperty variable: availableVariables) {
				cell = row.createCell(cellIndex++);
				cell.setCellValue(variable.getValue());
				cell.setCellStyle(bigStyle);
			}
		}
		for (short cellIndexTmp = 0; cellIndexTmp < cellIndex; cellIndexTmp++)
			sheet.setColumnWidth(cellIndexTmp++, DEFAULT_CELL_WIDTH);

	}

	private Integer getNumber(String value) {
		if (StringUtil.isEmpty(value))
			return null;

		try {
			return Integer.valueOf(value);
		} catch(Exception e) {}

		return null;
	}

	private List<AdvancedProperty> getVariablesForCase(CasePresentation theCase, Locale locale, boolean isAdmin) {
		List<AdvancedProperty> vars = theCase.getExternalData();
		if (ListUtil.isEmpty(vars)) {
			Long processInstanceId = null;
			try {
				processInstanceId = getCaseManagersProvider().getCaseManager().getProcessInstanceIdByCaseId(theCase.getId());
			} catch(Exception e) {
				LOGGER.log(Level.WARNING, "Error getting process instance for case: " + theCase);
			}
			if (processInstanceId == null)
				return null;

			vars = getAvailableVariablesByProcessInstanceId(locale, processInstanceId, isAdmin);
		}

		return vars;
	}

	private void addVariables(List<AdvancedProperty> variablesByProcessDefinition, CasePresentation theCase, HSSFRow row, HSSFSheet sheet,
			HSSFCellStyle bigStyle, Locale locale, boolean isAdmin, int cellIndex, List<Integer> fileCellsIndexes, String localizedFileLabel,HSSFCellStyle normalStyle) {
		if (ListUtil.isEmpty(variablesByProcessDefinition))
			return;

		AdvancedProperty variable = null;
		List<AdvancedProperty> variablesByProcessInstance = getVariablesForCase(theCase, locale, isAdmin);
		if (ListUtil.isEmpty(variablesByProcessInstance))
			return;

		for (AdvancedProperty processVariable: variablesByProcessDefinition) {
			variable = getVariableByValue(variablesByProcessInstance, processVariable.getValue());
			String value = getVariableValue(processVariable.getId(), variable);
			HSSFCell cell = row.createCell(cellIndex++);
			cell.setCellStyle(normalStyle);
			cell.setCellValue(value);
		}
	}

	private MultipleSelectionVariablesResolver getResolver(String name) {
		try {
			return ELUtil.getInstance().getBean(MultipleSelectionVariablesResolver.BEAN_NAME_PREFIX + name);
		} catch (Exception e) {}
		return null;
	}

	private String getVariableValue(String beanName, AdvancedProperty variable) {
		if (variable == null)
			return CoreConstants.EMPTY;

		MultipleSelectionVariablesResolver resolver = getResolver(beanName.split(CoreConstants.AT)[0]);
		if (resolver == null)
			return variable.getId();

		try {
			return resolver.isValueUsedForExport() ?
					resolver.getPresentation(variable.getName(), variable.getId(), variable.getExternalId()) :
					resolver.getKeyPresentation(variable.getExternalId(), variable.getId());
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error resolving value for variable" + variable + " and using resolver " + resolver, e);
		}

		return CoreConstants.EMPTY;
	}

	private AdvancedProperty getVariableByValue(List<AdvancedProperty> variables, String value) {
		if (ListUtil.isEmpty(variables) || StringUtil.isEmpty(value))
			return null;

		for (AdvancedProperty variable: variables) {
			if (value.equals(variable.getValue())) {
				return variable;
			}
		}

		return null;
	}

	private AdvancedProperty getVariableByName(List<AdvancedProperty> variables, String name) {
		if (ListUtil.isEmpty(variables) || StringUtil.isEmpty(name))
			return null;

		for (AdvancedProperty variable: variables) {
			if (name.equals(variable.getName())) {
				return variable;
			}
		}

		return null;
	}

	private MemoryFileBuffer getExportedData(String id,boolean exportContacts, boolean showCompany) {
		return getExportedData(
				getCasesByProcessDefinition(id), 
				id, 
				getSearchCriteria(id).getExportColumns(),
				exportContacts,showCompany);
	}

	private MemoryFileBuffer getExportedData(
			Map<String, List<CasePresentation>> casesByProcessDefinition,
			String id,
			List<String> exportColumns,
			boolean exportContacts, 
			boolean showCompany) {
		if (casesByProcessDefinition == null || ListUtil.isEmpty(casesByProcessDefinition.values()))
			return null;

		MemoryFileBuffer memory = new MemoryFileBuffer();
		OutputStream streamOut = new MemoryOutputStream(memory);
		HSSFWorkbook workBook = new HSSFWorkbook();

		HSSFFont bigFont = workBook.createFont();
		bigFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		bigFont.setFontHeightInPoints((short) 16);
		HSSFCellStyle bigStyle = workBook.createCellStyle();
		bigStyle.setFont(bigFont);

		HSSFFont normalFont = workBook.createFont();
//		normalFont.setFontHeightInPoints((short) 16);
		HSSFCellStyle normalStyle = workBook.createCellStyle();
		normalStyle.setFont(normalFont);

		boolean isAdmin = false;
		List<CasePresentation> cases = null;
		Locale locale = null;
		String fileNameLabel = localizeBPM("cases_bpm.file_name", "File name");

		IWContext iwc = CoreUtil.getIWContext();
		if (iwc != null) {
			locale = iwc.getCurrentLocale();
			isAdmin = iwc.isSuperAdmin();
		}

		if (locale == null)
			locale = Locale.ENGLISH;

		CasesSearchCriteriaBean searchCriteria = getSearchCriteria(id);
		List<String> standardFieldsLabels = getStandardFieldsLabels(id);
		List<String> createdSheets = new ArrayList<String>();

		for (String processName: casesByProcessDefinition.keySet()) {
			if (processName == null)
				continue;

			cases = casesByProcessDefinition.get(processName);

			String sheetName = StringHandler.shortenToLength(getSheetName(locale, processName), 30);
			HSSFSheet sheet = createdSheets.contains(sheetName) ? workBook.getSheet(sheetName) : workBook.createSheet(sheetName);
			createdSheets.add(sheetName);

			if (ListUtil.isEmpty(exportColumns) && searchCriteria != null) {
				Collection<String> visibleVariables = getVisibleVariablesBean()
						.getVariablesByComponentId(searchCriteria.getInstanceId().substring(5));
				if (!ListUtil.isEmpty(visibleVariables)) {
					exportColumns = new ArrayList<String>(visibleVariables);
				}
			}

			int lastCellNumber = 0;
			if (ListUtil.isEmpty(exportColumns)) {
				List<AdvancedProperty> availableVariables = getAvailableVariablesByProcessDefinition(locale, processName, isAdmin);
				if(!exportContacts){
					createHeaders(sheet, bigStyle, processName, isAdmin,standardFieldsLabels,availableVariables);
				}
				List<Integer> fileCellsIndexes = null;
				int rowNumber = 0;

				for (CasePresentation theCase: cases) {
					if(exportContacts){
						createHeaders(sheet, bigStyle, processName, isAdmin,standardFieldsLabels,availableVariables);
					}
					fileCellsIndexes = new ArrayList<Integer>();
					HSSFRow row = sheet.createRow(++rowNumber);
					int cellIndex = 0;

//					Default header values
					HSSFCell cell = row.createCell(cellIndex++);
					cell.setCellStyle(normalStyle);
					cell.setCellValue(theCase.getCaseIdentifier());

					cell = row.createCell(cellIndex++);
					cell.setCellStyle(normalStyle);
					cell.setCellValue(theCase.getCaseStatusLocalized());

					cell = row.createCell(cellIndex++);
					cell.setCellStyle(normalStyle);
					cell.setCellValue(getCaseCreator(theCase));

					cell = row.createCell(cellIndex++);
					cell.setCellStyle(normalStyle);
					cell.setCellValue(getCaseCreatorPersonalId(theCase));

					cell = row.createCell(cellIndex++);
					cell.setCellStyle(normalStyle);
					cell.setCellValue(getCaseCreatorEmail(theCase));


					if (!ListUtil.isEmpty(standardFieldsLabels)) {
						List<String> standardFieldsValues = getStandardFieldsValues(id, theCase);
						if (!ListUtil.isEmpty(standardFieldsValues)) {
							for (String standardFieldValue: standardFieldsValues) {
								cell = row.createCell(cellIndex++);
								cell.setCellStyle(normalStyle);
								cell.setCellValue(standardFieldValue);
							}
						}
					}

					//	Variable values
					addVariables(availableVariables, theCase, row, sheet, bigStyle, locale, isAdmin, cellIndex, fileCellsIndexes,
							fileNameLabel,normalStyle);

					if(exportContacts){
						CaseProcInstBind bind = getCasesBinder().getCaseProcInstBindByCaseId(Integer.valueOf(theCase.getId()));
						Long processInstanceId = bind.getProcInstId();
						ProcessManager processManager = bpmFactory.getProcessManagerByProcessInstanceId(processInstanceId);
						ProcessInstanceW piw = processManager.getProcessInstance(processInstanceId);
						Collection<User> users = processArtifacts.getUsersConnectedToProces(piw);
						addUsersToSheet(workBook, sheet, users, showCompany);
						rowNumber = (short) (sheet.getLastRowNum()+2);
					}

					lastCellNumber = row.getLastCellNum();
				}
			} else {
				if(!exportContacts){
					doCreateHeaders(sheet, bigStyle, exportColumns, locale);
				}
				int rowNumber = 0;

				for (CasePresentation theCase: cases) {
					if(exportContacts){
						doCreateHeaders(sheet, bigStyle, exportColumns, locale);
					}
					HSSFRow row = sheet.createRow(++rowNumber);
					int cellIndex = 0;

					List<AdvancedProperty> varsForCase = getVariablesForCase(theCase, locale, isAdmin);

					for (String column: exportColumns) {
						String value = null;
						if (column.startsWith(CaseConstants.CASE_PREFIX)) {
							if (CaseConstants.CASE_IDENTIFIER.equals(column))
								value = theCase.getCaseIdentifier();
							else if (CaseConstants.CASE_CREATION_DATE.equals(column)) {
								IWTimestamp created = new IWTimestamp(theCase.getCreated());
								value = created.getLocaleDateAndTime(locale, DateFormat.SHORT, DateFormat.SHORT);
							} else if (CaseConstants.CASE_STATUS.equals(column))
								value = theCase.getCaseStatusLocalized();
							else {
								LOGGER.warning("Do not know how to resolve value for column " + column);
								value = CoreConstants.MINUS;
							}
						} else {
							AdvancedProperty variable = null;
							if (column.equals("string_violatorPostalCode"))
								variable = getVariableByName(varsForCase, "string_ticketStreetAddress");
							else if (column.equals("string_ticketType")) {
								variable = getVariableByName(varsForCase, column);
								if (variable == null)
									value = getResolver(column).getPresentation(column, theCase.getId());
							} else if (column.equals("string_ticketMeterNumber")) {
								variable = getVariableByName(varsForCase, column);
								if (variable == null || StringUtil.isEmpty(variable.getValue())) {
									MultipleSelectionVariablesResolver resolver = getResolver(column);
									value = resolver.isValueUsedForExport() ?
											resolver.getPresentation(column, theCase.getId()) :
											resolver.getKeyPresentation(Integer.valueOf(theCase.getId()), null);
								}
							} else if ("string_caseStatus".equals(column)) {
								value = theCase.getCaseStatusLocalized();
							} else
								variable = getVariableByName(varsForCase, column);

							if (value == null)
								value = getVariableValue(column, variable);

							if ("string_ownerGender".equals(column)) {
								value = localizeBPM(value, value);
							}
						}

						HSSFCell cell = row.createCell(cellIndex++);
						cell.setCellStyle(normalStyle);
						cell.setCellValue(value);
						if(exportContacts){
							CaseProcInstBind bind = getCasesBinder().getCaseProcInstBindByCaseId(Integer.valueOf(theCase.getId()));
							Long processInstanceId = bind.getProcInstId();
							ProcessManager processManager = bpmFactory.getProcessManagerByProcessInstanceId(processInstanceId);
							ProcessInstanceW piw = processManager.getProcessInstance(processInstanceId);
							Collection<User> users = processArtifacts.getUsersConnectedToProces(piw);
							addUsersToSheet(workBook, sheet, users, showCompany);
						}
					}

					lastCellNumber = row.getLastCellNum();
				}
			}

			if (lastCellNumber > 0)
				for (int i = 0; i < lastCellNumber; i++)
					sheet.autoSizeColumn(i);

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

	private void addUsersToSheet(
			HSSFWorkbook workBook,
			HSSFSheet sheet,Collection<User> users,
			boolean showUserCompany){
		HSSFFont bigFont = workBook.createFont();
		bigFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		bigFont.setFontHeightInPoints((short) 16);
		HSSFCellStyle bigStyle = workBook.createCellStyle();
		bigStyle.setFont(bigFont);

		HSSFFont normalFont = workBook.createFont();
		normalFont.setFontHeightInPoints((short) 16);
		HSSFCellStyle normalStyle = workBook.createCellStyle();
		normalStyle.setFont(normalFont);

		int columnWidth = DEFAULT_CELL_WIDTH;
		int rowNum = sheet.getLastRowNum()+1;
		HSSFRow row = sheet.createRow(rowNum++);

		int column = 0;

		sheet.setColumnWidth(column, columnWidth);
		HSSFCell cell = row.createCell(column++);
		cell.setCellStyle(bigStyle);
		cell.setCellValue(localizeBPM("name", "Name"));

		if(showUserCompany){
			sheet.setColumnWidth(column, columnWidth);
			cell = row.createCell(column++);
			cell.setCellStyle(bigStyle);
			cell.setCellValue(localizeBPM("cases_bpm.company", "Company"));
		}

		sheet.setColumnWidth(column, columnWidth);
		cell = row.createCell(column++);
		cell.setCellStyle(bigStyle);
		cell.setCellValue(localizeBPM("email_address", "E-mail address"));

		sheet.setColumnWidth(column, columnWidth);
		cell = row.createCell(column++);
		cell.setCellStyle(bigStyle);
		cell.setCellValue(localizeBPM("phone_number", "Phone number"));

		for(User user : users){
			row = sheet.createRow(rowNum++);

			column = 0;

			cell = row.createCell(column++);
			cell.setCellStyle(normalStyle);
			cell.setCellValue(user.getName());

			if (showUserCompany) {
				GeneralCompanyBusiness generalCompanyBusiness = getGeneralCompanyBusiness();
				if (generalCompanyBusiness != null) {
					cell = row.createCell(column++);
					cell.setCellStyle(normalStyle);
					Collection<GeneralCompany> companies = generalCompanyBusiness.getCompaniesForUser(user);
					String companyName;
					if(!ListUtil.isEmpty(companies)){
						GeneralCompany company = companies.iterator().next();
						companyName = company.getName();
					}else{
						companyName = CoreConstants.MINUS;
					}
					cell.setCellValue(companyName);
				}
			}


			cell = row.createCell(column++);
			cell.setCellStyle(normalStyle);
			Collection<Email> emails =  user.getEmails();
			if(!ListUtil.isEmpty(emails)){
				StringBuilder builder = new StringBuilder();
				boolean added = false;
				for(Email email : emails){
					String emailAddress = email.getEmailAddress();
					if(StringUtil.isEmpty(emailAddress)){
						continue;
					}
					if(added){
						builder.append(", ");
					}else{
						added = true;
					}
					builder.append(emailAddress);
				}
				cell.setCellValue(builder.toString());
			}

			Collection<Phone> phones = user.getPhones();
			StringBuilder userPhones = new StringBuilder();
			for(Phone phone : phones){
				String number = phone.getNumber();
				if(StringUtil.isEmpty(number)){
					continue;
				}
				userPhones.append(number).append("; ");
			}
			cell = row.createCell(column++);
			cell.setCellStyle(normalStyle);
			cell.setCellValue(userPhones.toString());

		}
	}
	@Override
	public MemoryFileBuffer getUsersExport(Collection<User> users,Locale locale,boolean showUserCompany){
		MemoryFileBuffer memory = new MemoryFileBuffer();
		OutputStream streamOut = new MemoryOutputStream(memory);
		HSSFWorkbook workBook = new HSSFWorkbook();

		HSSFSheet sheet = workBook.createSheet();
		addUsersToSheet(workBook, sheet, users, showUserCompany);
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

	private List<String> getStandardFieldsLabels(String id) {
		if (StringUtil.isEmpty(id))
			return Collections.emptyList();

		CasesSearchCriteriaBean criteria = getSearchCriteria(id);
		if (criteria == null)
			return null;

		List<String> labels = new ArrayList<String>();
		if (!StringUtil.isEmpty(criteria.getDescription())) {
			labels.add(localizeCases("description", "Description"));
		}

		if (!StringUtil.isEmpty(criteria.getContact())) {
			labels.add(localizeCases("contact", "Contact"));
		}

		if (!StringUtil.isEmpty(criteria.getStatusId())) {
			labels.add(localizeCases("status", "Status"));
		}

		if (!StringUtil.isEmpty(criteria.getDateRange())) {
			labels.add(localizeCases("date_range", "Date range"));
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

	private IWResourceBundle getResourceBundle(String bundleIdentifier) {
		IWContext iwc = CoreUtil.getIWContext();
		try {
			return iwc.getIWMainApplication().getBundle(bundleIdentifier).getResourceBundle(iwc);
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting resource bundle for: " + bundleIdentifier, e);
		}
		return null;
	}

	@Override
	public MemoryFileBuffer getExportedSearchResults(String id,boolean exportContacts, boolean showCompany) {
		if (memory != null)
			return memory;

		if (doExport(id,exportContacts,showCompany))
			return memory;

		return null;
	}

	@Override
	public boolean isSearchResultStored(String id) {
		return isSearchResultStored(id, false);
	}

	private boolean isSearchResultStored(String id, boolean loadExternalData) {
		if (StringUtil.isEmpty(id)) {
			return false;
		}
		Collection<CasePresentation> cases = getCases(id, loadExternalData);
		return ListUtil.isEmpty(cases) ? Boolean.FALSE : Boolean.TRUE;
	}

	@Override
	public MemoryFileBuffer getExportedCases(String id,boolean exportContacts, boolean showCompany) {
		if (StringUtil.isEmpty(id)) {
			LOGGER.warning("Key is not provided");
			return null;
		}

		List<CasePresentation> cases = externalData.remove(id);
		if (ListUtil.isEmpty(cases)) {
			LOGGER.warning("No cases found by key: " + id);
			return null;
		}

		Map<String, List<CasePresentation>> casesByProcDef = getCasesByProcessDefinition(cases);
		CasesSearchCriteriaBean bean = getSearchCriteria(id);

		return getExportedData(casesByProcDef, null, bean == null ? null : bean.getExportColumns(),exportContacts,showCompany);
	}

	private Map<String, List<CasePresentation>> getCasesByProcessDefinition(String id) {
		if (!isSearchResultStored(id, true)) {
			return null;
		}

		Collection<CasePresentation> cases = getCases(id, true);
		return getCasesByProcessDefinition(cases);
	}

	private Map<String, List<CasePresentation>> getCasesByProcessDefinition(Collection<CasePresentation> cases) {
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

	@Override
	public Integer getNextCaseId(String id, Integer currentId, String processDefinitionName) {
		if (currentId == null || !isSearchResultStored(id)) {
			LOGGER.info("Unkown current case's id or no search results stored!");
			return null;
		}

		Collection<CasePresentation> cases = getCases(id);
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

	@Override
	public Integer getNextCaseId(String id, Integer currentId) {
		return getNextCaseId(id, currentId, null);
	}

	@Override
	public boolean clearSearchResults(String id) {
		allResults.remove(id);
		externalData.remove(id);
		concatenatedData.remove(id);
		return Boolean.TRUE;
	}

	@Override
	public Collection<CasePresentation> getSearchResults(String id) {
		if (StringUtil.isEmpty(id)) {
			return null;
		}

		return getCases(id);
	}

	@Override
	public CasesSearchCriteriaBean getSearchCriteria(String id) {
		if (StringUtil.isEmpty(id)) {
			return null;
		}

		CasesSearchResults results = allResults.get(id);
		return results == null ? null : results.getCriterias();
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

	@Override
	public boolean isAllDataLoaded(String id) {
		CasesSearchCriteriaBean criterias = getSearchCriteria(id);
		return criterias == null ? Boolean.FALSE : criterias.isAllDataLoaded();
	}

	@Override
	public void concatExternalData(String id, List<CasePresentation> externalData) {
		if (StringUtil.isEmpty(id) || ListUtil.isEmpty(externalData) || concatenatedData.contains(id)) {
			return;
		}

		concatenatedData.add(id);
		List<CasePresentation> data = this.externalData.get(id);
		if (data == null) {
			data = new ArrayList<CasePresentation>(externalData);
			this.externalData.put(id, data);
		} else {
			data.addAll(externalData);
		}
	}

	@Override
	public boolean setCasesToExport(String id, List<CasePresentation> cases) {
		if (StringUtil.isEmpty(id) || ListUtil.isEmpty(cases))
			return false;

		externalData.put(id, cases);
		return true;
	}

}