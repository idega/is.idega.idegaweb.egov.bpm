package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FastByteArrayOutputStream;

import com.idega.block.process.business.CaseConstants;
import com.idega.block.process.business.CaseManagersProvider;
import com.idega.block.process.business.ProcessConstants;
import com.idega.block.process.presentation.beans.CasePresentation;
import com.idega.block.process.presentation.beans.CasesSearchCriteriaBean;
import com.idega.block.process.presentation.beans.CasesSearchResults;
import com.idega.block.process.presentation.beans.CasesSearchResultsHolder;
import com.idega.block.process.variables.VisibleVariablesBean;
import com.idega.bpm.BPMConstants;
import com.idega.bpm.model.VariableInstance;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.core.business.GeneralCompanyBusiness;
import com.idega.core.company.bean.GeneralCompany;
import com.idega.core.contact.data.Email;
import com.idega.core.contact.data.Phone;
import com.idega.data.IDOLookup;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWMainApplicationSettings;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.artifacts.presentation.ProcessArtifacts;
import com.idega.jbpm.bean.BPMProcessVariable;
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
import com.idega.util.LocaleUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.WebUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.expression.ELUtil;
import com.idega.util.text.TextSoap;

import is.idega.idegaweb.egov.application.ApplicationUtil;
import is.idega.idegaweb.egov.application.data.Application;
import is.idega.idegaweb.egov.application.data.ApplicationHome;
import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.search.CasesListSearchCriteriaBean;
import is.idega.idegaweb.egov.cases.data.CaseCategory;
import is.idega.idegaweb.egov.cases.data.CaseCategoryHome;
import is.idega.idegaweb.egov.cases.presentation.CasesBoardViewer;
import is.idega.idegaweb.egov.cases.presentation.CasesStatistics;
import is.idega.idegaweb.egov.cases.util.CasesConstants;

@Scope("session")
@Service(CasesSearchResultsHolder.SPRING_BEAN_IDENTIFIER)
public class CasesSearchResultsHolderImpl implements CasesSearchResultsHolder {

	private static final Logger LOGGER = Logger.getLogger(CasesSearchResultsHolderImpl.class.getName());
	private static final int DEFAULT_CELL_WIDTH = 40 * 256;

	private Map<String, CasesSearchResults> allResults = new HashMap<>();
	private Map<String, List<CasePresentation>> externalData = new HashMap<>();
	private Map<String, Boolean> availableResolvers = new HashMap<>();

	private List<String> concatenatedData = new ArrayList<>();

	private byte[] memory;

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
		long start = System.currentTimeMillis();
		try {
			CasesSearchResults results = allResults.get(id);
			Collection<CasePresentation> data = results == null ? null : results.getCases();

			if (loadExternalData) {
				List<CasePresentation> externalData = this.externalData.get(id);

				if (externalData != null) {
					if (data == null) {
						return externalData;
					} else {
						List<CasePresentation> allData = new ArrayList<>(data);
						allData.addAll(externalData);
						return allData;
					}
				}
			}

			return data;
		} finally {
			CoreUtil.doDebug(start, System.currentTimeMillis(), "CasesSearchResultsHolderImpl.getCases");
		}
	}

	@Override
	public boolean doExport(String id) {
		return doExport(id, false, false, false);
	}

	@Override
	public boolean doExport(String id, boolean exportContacts, boolean showCompany) {
		return doExport(id, exportContacts, showCompany, false);
	}

	@Override
	public boolean doExport(String id, boolean exportContacts, boolean showCompany, boolean addDefaultFields) {
		Collection<CasePresentation> cases = getCases(id, true);
		if (ListUtil.isEmpty(cases)) {
			return false;
		}

		memory = getExportedData(id, exportContacts, showCompany, addDefaultFields, null);

		return memory == null ? false : true;
	}

	private AdvancedProperty getSheetNameByCaseCode(String processNameOrCategoryId, Map<String, CasePresentation> cases) {
		if (MapUtil.isEmpty(cases)) {
			return null;
		}

		CasePresentation presentation = cases.values().iterator().next();
		String code = presentation.getCode();
		if (StringUtil.isEmpty(code)) {
			return null;
		}

		List<AdvancedProperty> variables = presentation.getExternalData();
		if (ListUtil.isEmpty(variables)) {
			return null;
		}

		IWMainApplicationSettings settings = IWMainApplication.getDefaultIWMainApplication().getSettings();
		String variable = settings.getProperty("cases.exp_sheet_".concat(code));
		if (StringUtil.isEmpty(variable)) {
			return null;
		}

		for (AdvancedProperty var: variables) {
			String name = var == null ? null : var.getName();
			if (!StringUtil.isEmpty(name) && name.equals(variable) && !StringUtil.isEmpty(var.getId())) {
				return new AdvancedProperty(processNameOrCategoryId, var.getId());
			}
		}

		return null;
	}

	private String getApplicationName(String processNameOrCategoryId) {
		if (StringUtil.isEmpty(processNameOrCategoryId)) {
			return null;
		}

		try {
			ApplicationHome appHome = (ApplicationHome) IDOLookup.getHome(Application.class);
			Application app = appHome.findByCaseCode(processNameOrCategoryId);
			return app == null ?
					null :
					ApplicationUtil.getLocalizedName(app);
		} catch (Exception e) {}
		return null;
	}

	private AdvancedProperty getSheetName(Locale locale, String processNameOrCategoryId, Map<String, CasePresentation> cases) {
		AdvancedProperty nameByCaseCodeAndVariable = getSheetNameByCaseCode(processNameOrCategoryId, cases);
		if (nameByCaseCodeAndVariable != null) {
			return nameByCaseCodeAndVariable;
		}

		if (locale == null || StringUtil.isEmpty(processNameOrCategoryId)) {
			return new AdvancedProperty(CoreConstants.MINUS, CoreConstants.MINUS);
		}

		String sheetName = null;
		Collection<IWResourceBundle> resources = LocaleUtil.getEnabledResources(
				IWMainApplication.getDefaultIWMainApplication(),
				locale,
				is.idega.idegaweb.egov.bpm.BPMConstants.IW_BUNDLE_IDENTIFIER
		);
		if (!ListUtil.isEmpty(resources)) {
			for (Iterator<IWResourceBundle> iter = resources.iterator(); ((StringUtil.isEmpty(sheetName) || processNameOrCategoryId.equals(sheetName)) && iter.hasNext());) {
				String name = iter.next().getLocalizedString(processNameOrCategoryId, processNameOrCategoryId);
				if (!StringUtil.isEmpty(name) && !processNameOrCategoryId.equals(name)) {
					sheetName = name;
				}
			}
		}
		if (!StringUtil.isEmpty(sheetName)) {
			return new AdvancedProperty(sheetName, sheetName);
		}

		Integer id = getNumber(processNameOrCategoryId);
		if (id == null) {
			sheetName = getCaseManagersProvider().getCaseManager().getProcessName(processNameOrCategoryId, locale);
		}
		if (StringUtil.isEmpty(sheetName)) {
			sheetName = getApplicationName(processNameOrCategoryId);
		}
		if (sheetName == null) {
			sheetName = getCategoryName(locale, processNameOrCategoryId.equals(CasesStatistics.UNKOWN_CATEGORY_ID) ? null : getCaseCategory(id));
		}
		return StringUtil.isEmpty(sheetName) ?
				new AdvancedProperty(CoreConstants.MINUS, CoreConstants.MINUS) :
				new AdvancedProperty(sheetName, sheetName);
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
		long start = System.currentTimeMillis();
		try {
			Collection<VariableInstance> variablesByProcessDefinition = null;
			try {
				variablesByProcessDefinition = getNumber(processDefinition) == null ? getVariablesQuerier()
						.getVariablesByProcessDefinition(processDefinition) : null;
			} catch(Exception e) {
				LOGGER.log(Level.WARNING, "Error getting variables for process: " + processDefinition, e);
			}

			return getAvailableVariables(variablesByProcessDefinition, locale, isAdmin, false);
		} finally {
			CoreUtil.doDebug(start, System.currentTimeMillis(), "CasesSearchResultsHolderImpl.getAvailableVariablesByProcessDefinition");
		}
	}

	@Transactional(readOnly=true)
	private List<AdvancedProperty> getAvailableVariablesByProcessInstanceId(List<AdvancedProperty> variablesByProcessDefinition, Locale locale, Serializable processInstanceId, boolean isAdmin) {
		Collection<VariableInstance> variablesByProcessInstance = null;

		if (processInstanceId instanceof String && !ListUtil.isEmpty(variablesByProcessDefinition)) {
			List<String> variablesNames = new ArrayList<>();
			for (AdvancedProperty var: variablesByProcessDefinition) {
				variablesNames.add(var.getId());
			}
			ProcessInstanceW piW = getBpmFactory().getProcessInstanceW(processInstanceId);
			variablesByProcessInstance = piW.getVariables(variablesNames);
		}

		if (processInstanceId instanceof Number) {
			try {
				variablesByProcessInstance = getVariablesQuerier().getFullVariablesByProcessInstanceId(((Number) processInstanceId).longValue(), false);
			} catch(Exception e) {
				LOGGER.log(Level.WARNING, "Error getting variables for process instance: " + processInstanceId, e);
			}
		}

		return getAvailableVariables(variablesByProcessInstance, locale, isAdmin, true);
	}

	@Transactional(readOnly = true)
	private List<AdvancedProperty> getAvailableVariables(Collection<VariableInstance> variables, Locale locale, boolean isAdmin, boolean useRealValue) {
		if (ListUtil.isEmpty(variables)) {
			return null;
		}

		BPMProcessVariablesBean variablesProvider = ELUtil.getInstance().getBean(BPMProcessVariablesBean.SPRING_BEAN_IDENTIFIER);
		List<AdvancedProperty> results = variablesProvider.getAvailableVariables(variables, locale, isAdmin, useRealValue);
		return results;
	}

	private int doCreateHeaders(int rowNumber, XSSFSheet sheet, XSSFCellStyle bigStyle, List<String> columns, Locale locale, String process) {
		long start = System.currentTimeMillis();
		try {
			IWResourceBundle iwrb = getResourceBundle(CasesConstants.IW_BUNDLE_IDENTIFIER);
			BPMProcessVariablesBean variablesBean = ELUtil.getInstance().getBean(BPMProcessVariablesBean.SPRING_BEAN_IDENTIFIER);

			XSSFRow row = sheet.createRow(rowNumber);
			rowNumber++;

			int cellIndex = 0;
			for (String column: columns) {
				XSSFCell cell = row.createCell(cellIndex++);

				String value = null;
				if (column != null) {
					if (column.startsWith(CaseConstants.CASE_PREFIX)) {
						if (CaseConstants.CASE_IDENTIFIER.equals(column)) {
							value = iwrb.getLocalizedString("case_nr", "Case nr.");
						} else if (CaseConstants.CASE_CREATION_DATE.equals(column)) {
							value = iwrb.getLocalizedString("created_date", "Created date");
						} else if (CaseConstants.CASE_STATUS.equals(column)) {
							value = iwrb.getLocalizedString("status", "Status");
						} else if (CaseConstants.CASE_BODY.equals(column)) {
							value = iwrb.getLocalizedString(StringUtil.isEmpty(process) ? CaseConstants.CASE_BODY : process + CoreConstants.DOT + CaseConstants.CASE_BODY, "Message");
						} else {
							value = iwrb.getLocalizedString(column, column);
						}
					} else if (column.equalsIgnoreCase("date_range") || column.equalsIgnoreCase("dateRange")) {
						value = localizeCases("date_range", "Date range");
					} else {
						value = variablesBean.getVariableLocalizedName(column, locale);
					}
				}
				cell.setCellValue(value);
				cell.setCellStyle(bigStyle);
			}

			for (int i = 0; i < cellIndex; i++) {
				sheet.setColumnWidth(i, DEFAULT_CELL_WIDTH);
			}

			return rowNumber;
		} finally {
			CoreUtil.doDebug(start, System.currentTimeMillis(), "CasesSearchResultsHolderImpl.doCreateHeaders: created headers for " + columns);
		}
	}

	private int createHeaders(
			int rowNumber,
			XSSFSheet sheet,
			XSSFCellStyle bigStyle,
			String processName,
			boolean isAdmin,
			List<String> standardFieldsInfo,
			List<AdvancedProperty> availableVariables,
			boolean addDefaultFields
	) {
		long start = System.currentTimeMillis();
		try {
			XSSFRow row = sheet.createRow(rowNumber);
			rowNumber++;

			int cellIndex = 0;
			XSSFCell cell = null;
			if (addDefaultFields) {
				//	Default header labels
				cell = row.createCell(cellIndex++);
				cell.setCellValue(localizeCases("case_nr", "Case nr."));
				cell.setCellStyle(bigStyle);

				cell = row.createCell(cellIndex++);
				cell.setCellValue(localizeCases("status", "Status"));
				cell.setCellStyle(bigStyle);

				cell = row.createCell(cellIndex++);
				cell.setCellValue(localizeCases("created_date", "Created date"));
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
			}

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

			for (int i = 0; i < cellIndex; i++) {
				sheet.setColumnWidth(i, DEFAULT_CELL_WIDTH);
			}

			return rowNumber;
		} finally {
			CoreUtil.doDebug(start, System.currentTimeMillis(), "CasesSearchResultsHolderImpl.createHeaders 533");
		}
	}

	private Integer getNumber(String value) {
		if (StringUtil.isEmpty(value))
			return null;

		try {
			return Integer.valueOf(value);
		} catch(Exception e) {}

		return null;
	}

	private List<AdvancedProperty> getVariablesForCase(List<AdvancedProperty> variablesByProcessDefinition, CasePresentation theCase, Locale locale, boolean isAdmin) {
		long start = System.currentTimeMillis();
		try {
			List<AdvancedProperty> vars = theCase.getExternalData();
			if (ListUtil.isEmpty(vars)) {
				Serializable processInstanceId = null;
				try {
					CaseProcInstBind bind = getCasesBinder().getCaseProcInstBindByCaseId(Integer.valueOf(theCase.getId()));
					processInstanceId = bind.getUuid();
					if (processInstanceId == null) {
						processInstanceId = bind.getProcInstId();
					}
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Error getting process instance for case: " + theCase);
				}
				if (processInstanceId == null)
					return null;

				vars = getAvailableVariablesByProcessInstanceId(variablesByProcessDefinition, locale, processInstanceId, isAdmin);
			}

			return vars;
		} finally {
			CoreUtil.doDebug(start, System.currentTimeMillis(), "CasesSearchResultsHolderImpl.getVariablesForCase: got variables " + variablesByProcessDefinition + " for caee " + theCase);
		}
	}

	private int addVariables(
			List<AdvancedProperty> variablesByProcessDefinition,
			CasePresentation theCase,
			XSSFRow row,
			XSSFSheet sheet,
			XSSFCellStyle bigStyle,
			Locale locale,
			boolean isAdmin,
			int cellIndex,
			List<Integer> fileCellsIndexes,
			String localizedFileLabel,
			XSSFCellStyle normalStyle,
			int rowNumber
	) {
		if (ListUtil.isEmpty(variablesByProcessDefinition)) {
			return cellIndex;
		}

		long start = System.currentTimeMillis();
		try {
			AdvancedProperty variable = null;
			List<AdvancedProperty> variablesByProcessInstance = getVariablesForCase(variablesByProcessDefinition, theCase, locale, isAdmin);
			if (ListUtil.isEmpty(variablesByProcessInstance)) {
				return cellIndex;
			}

			int numberOfRows = -1;
			List<Serializable> values = new ArrayList<>();

			for (AdvancedProperty procDefVariable: variablesByProcessDefinition) {
				variable = getVariableByValue(variablesByProcessInstance, procDefVariable.getValue());
				if (variable == null && procDefVariable.getId() != null) {
					for (Iterator<AdvancedProperty> iter = variablesByProcessInstance.iterator(); (iter.hasNext() && variable == null);) {
						AdvancedProperty procInstVar = iter.next();
						if (procInstVar.getName() != null && procDefVariable.getId().equals(procInstVar.getName())) {
							variable = procInstVar;
						}
					}
				}
				String value = getVariableValue(procDefVariable.getId(), variable, null, null);
				if (
						StringHandler.isNumeric(value) &&
						(	variable.getValue() != null && variable.getValue().startsWith(BPMConstants.GROUP_LOC_NAME_PREFIX) ||
							variable.getName() != null && CasesBoardViewer.VARIABLE_PROJECT_NATURE.equals(variable.getName())
						)
				) {
					value = LocaleUtil.getLocalizedGroupName(IWMainApplication.getDefaultIWMainApplication(), locale, IWBundleStarter.IW_BUNDLE_IDENTIFIER, value);
				}

				if (StringUtil.isEmpty(value) || CoreConstants.MINUS.equals(value)) {
					String name = variable.getName();
					if (!StringUtil.isEmpty(name)) {
						switch (name) {
						case ProcessConstants.CASE_IDENTIFIER:
							value = theCase.getCaseIdentifier();
							break;

						case "date_payForParkingDate":
							Timestamp created = theCase.getCreated();
							if (created != null) {
								value = new IWTimestamp(created).getLocaleDateAndTime(locale, DateFormat.MEDIUM, DateFormat.MEDIUM);
							}
							break;

						default:
							break;
						}
					}
				}

				List<String> valuesForVar = ListUtil.getListFromJSON(value);
				if (valuesForVar == null) {
					values.add(value);
				} else {
					int size = valuesForVar.size();
					if (size > numberOfRows) {
						numberOfRows = size;
					}
					values.add(new ArrayList<>(valuesForVar));
				}
			}

			Map<Integer, XSSFRow> rows = new HashMap<>();
			for (int i = 0; i < values.size(); i++) {
				Serializable value = values.get(i);
				boolean list = value instanceof List<?>;
				if (!list && numberOfRows >= 0) {
					List<Serializable> tmp = new ArrayList<>();
					tmp.add(value == null ? null : value.toString());
					list = true;
					value = new ArrayList<>(tmp);
				}

				if (list) {
					@SuppressWarnings("unchecked")
					List<String> varValues = (List<String>) value;
					if (ListUtil.isEmpty(varValues)) {
						continue;
					}

					int tmpCell = i;
					for (int j = 0; j < varValues.size(); j++) {
						int rowIndex = j + rowNumber;
						XSSFRow tmpRow = rows.get(rowIndex);
						if (tmpRow == null) {
							tmpRow = sheet.createRow(rowIndex);
							rows.put(rowIndex, tmpRow);
						}

						XSSFCell cell = tmpRow.createCell(tmpCell);
						cell.setCellStyle(normalStyle);
						cell.setCellValue(varValues.get(j));

					}
				} else {
					XSSFCell cell = row.createCell(cellIndex++);
					cell.setCellStyle(normalStyle);
					cell.setCellValue(getRealValue(value.toString()));
				}
			}
			return cellIndex;
		} finally {
			CoreUtil.doDebug(start, System.currentTimeMillis(), "CasesSearchResultsHolderImpl.addVariables: added variables " + variablesByProcessDefinition + " for case " + theCase);
		}
	}

	private String getRealValue(String value) {
		if (StringUtil.isEmpty(value)) {
			return value;
		}

		int forSeparator = value.indexOf(is.idega.idegaweb.egov.bpm.BPMConstants.FOR);
		if (forSeparator > 0) {
			value = value.substring(0, forSeparator);
		}

		return value;
	}

	private MultipleSelectionVariablesResolver getResolver(String name) {
		if (StringUtil.isEmpty(name)) {
			return null;
		}

		try {
			Boolean exists = availableResolvers.get(name);
			if (exists != null && !exists) {
				return null;
			}

			MultipleSelectionVariablesResolver resolver = ELUtil.getInstance().getBean(MultipleSelectionVariablesResolver.BEAN_NAME_PREFIX + name);
			if (resolver == null) {
				availableResolvers.put(name, Boolean.FALSE);
			} else {
				availableResolvers.put(name, Boolean.TRUE);
			}
			return resolver;
		} catch (Exception e) {}

		availableResolvers.put(name, Boolean.FALSE);
		return null;
	}

	private String getVariableValue(String beanName, AdvancedProperty variable, Map<String, VariableInstance> processData, CasesSearchCriteriaBean searchCriteria) {
		if (variable == null) {
			return CoreConstants.EMPTY;
		}

		long start = System.currentTimeMillis();
		try {
			MultipleSelectionVariablesResolver resolver =
					StringUtil.isEmpty(beanName) ||
					beanName.indexOf(CoreConstants.AT) == -1 ||
					(StringUtil.isEmpty(variable.getName()) || variable.getName().toLowerCase().indexOf("mail") != -1) ?
							null :
							getResolver(beanName.split(CoreConstants.AT)[0]);
			if (resolver == null) {
				return variable.getId();
			}

			try {
				resolver.setProcessData(processData);
				if (searchCriteria instanceof CasesListSearchCriteriaBean) {
					CasesListSearchCriteriaBean criteria = (CasesListSearchCriteriaBean) searchCriteria;
					resolver.setSearchByVariables(criteria.getProcessVariables());
				}
				String value = resolver.isValueUsedForExport() ?
						resolver.getPresentation(variable.getName(), variable.getId(), variable.getExternalId()) :
						resolver.getKeyPresentation(variable.getExternalId(), variable.getId());
				return value;
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error resolving value for variable" + variable + " and using resolver " + resolver, e);
			}

			return CoreConstants.EMPTY;
		} finally {
			CoreUtil.doDebug(start, System.currentTimeMillis(), "CasesSearchResultsHolderImpl.getVariableValue: got value for " + variable);
		}
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

	private byte[] getExportedData(String id, boolean exportContacts, boolean showCompany, boolean addDefaultFields, String category) {
		CasesSearchCriteriaBean criteria = getSearchCriteria(id);
		return getExportedData(
				getCasesByProcessDefinition(id, category),
				id,
				criteria == null ? null : criteria.getExportColumns(),
				exportContacts,
				showCompany,
				addDefaultFields,
				category
		);
	}

	private byte[] getExportedData(
			Map<String, Map<String, CasePresentation>> casesByProcessDefinition,
			String id,
			List<String> exportColumns,
			boolean exportContacts,
			boolean showCompany,
			boolean addDefaultFields,
			String category
	) {
		if (casesByProcessDefinition == null || ListUtil.isEmpty(casesByProcessDefinition.values())) {
			return null;
		}

		long start = System.currentTimeMillis();
		try {
			long s = System.currentTimeMillis();
			FastByteArrayOutputStream streamOut = new FastByteArrayOutputStream();
			XSSFWorkbook workBook = new XSSFWorkbook();

			XSSFFont bigFont = workBook.createFont();
			bigFont.setBold(true);
			bigFont.setFontHeightInPoints((short) 16);
			XSSFCellStyle bigStyle = workBook.createCellStyle();
			bigStyle.setFont(bigFont);

			XSSFFont normalFont = workBook.createFont();
			XSSFCellStyle normalStyle = workBook.createCellStyle();
			normalStyle.setFont(normalFont);

			boolean isAdmin = false;
			Map<String, CasePresentation> cases = null;
			Locale locale = null;
			String fileNameLabel = localizeBPM("cases_bpm.file_name", "File name");

			IWContext iwc = CoreUtil.getIWContext();
			if (iwc != null) {
				locale = iwc.getCurrentLocale();
				isAdmin = iwc.isSuperAdmin();
			}

			if (locale == null) {
				locale = Locale.ENGLISH;
			}

			CasesSearchCriteriaBean searchCriteria = getSearchCriteria(id);
			List<String> standardFieldsLabels = getStandardFieldsLabels(id);
			Map<String, Boolean> createdSheets = new HashMap<>();
			CoreUtil.doDebug(s, System.currentTimeMillis(), "CasesSearchResultsHolderImpl.getExportedData: prepared");

			for (String processName: casesByProcessDefinition.keySet()) {
				if (processName == null) {
					LOGGER.warning("Process name is unknown");
					continue;
				}

				s = System.currentTimeMillis();
				cases = casesByProcessDefinition.get(processName);

				AdvancedProperty sheetProps = getSheetName(locale, processName, cases);
				String sheetName = sheetProps.getValue();
				if (!sheetName.equals(sheetProps.getId())) {
					int index = 0;
					String originalSheetName = sheetName;
					while (createdSheets.containsKey(sheetName)) {
						index++;
						sheetName = originalSheetName.concat(CoreConstants.SPACE).concat(String.valueOf(index));
					}
				}
				sheetName = TextSoap.encodeToValidExcelSheetName(StringHandler.shortenToLength(sheetName, 30));
				XSSFSheet sheet = createdSheets.containsKey(sheetName) ?
						workBook.getSheet(sheetName) :
						workBook.createSheet(sheetName);
				createdSheets.put(sheetName, Boolean.TRUE);
				CoreUtil.doDebug(s, System.currentTimeMillis(), "CasesSearchResultsHolderImpl.getExportedData: created sheet name");

				if (ListUtil.isEmpty(exportColumns) && searchCriteria != null && !StringUtil.isEmpty(searchCriteria.getInstanceId())) {
					Collection<String> visibleVariables = getVisibleVariablesBean().getVariablesByComponentId(searchCriteria.getInstanceId().substring(5));
					if (!ListUtil.isEmpty(visibleVariables)) {
						exportColumns = new ArrayList<>(visibleVariables);
					}
				}

				if (ListUtil.isEmpty(exportColumns)) {
					List<AdvancedProperty> availableVariables = getAvailableVariablesByProcessDefinition(locale, processName, isAdmin);
					if (ListUtil.isEmpty(availableVariables)) {
						Map<String, CasePresentation> theCases = casesByProcessDefinition.get(processName);
						if (!MapUtil.isEmpty(theCases)) {
							availableVariables = theCases.values().iterator().next().getExternalData();
						}
					}
					if (ListUtil.isEmpty(availableVariables) && searchCriteria != null && !ListUtil.isEmpty(searchCriteria.getCustomColumns())) {
						s = System.currentTimeMillis();
						availableVariables = new ArrayList<>();
						List<String> customColumns = searchCriteria.getCustomColumns();
						Collection<IWResourceBundle> resources = LocaleUtil.getEnabledResources(iwc.getIWMainApplication(), locale, IWBundleStarter.IW_BUNDLE_IDENTIFIER);
						if (!ListUtil.isEmpty(resources)) {
							String prefix = BPMConstants.BPMN_VARIABLE_PREFIX;
							for (String customColumn: customColumns) {
								String key = prefix.concat(customColumn);
								String localized = key;
								for (
										Iterator<IWResourceBundle> iter = resources.iterator();
										(iter.hasNext() && (StringUtil.isEmpty(localized) || localized.equals(key)));
								) {
									IWResourceBundle iwrb = iter.next();
									localized = iwrb.getLocalizedString(key, key);
								}
								if (!StringUtil.isEmpty(localized) && !localized.equals(key)) {
									availableVariables.add(new AdvancedProperty(customColumn, localized));
								}
							}
						}
						CoreUtil.doDebug(s, System.currentTimeMillis(), "CasesSearchResultsHolderImpl.getExportedData 903: resolved available variables");
					}

					int rowNumber = 0;
					if (!exportContacts) {
						rowNumber = createHeaders(rowNumber, sheet, bigStyle, processName, isAdmin, standardFieldsLabels, availableVariables, addDefaultFields);
					}
					List<Integer> fileCellsIndexes = null;

					int index = 1;
					int total = cases.size();
					for (CasePresentation theCase: cases.values()) {
						long cs = System.currentTimeMillis();
						LOGGER.info("Writing case no. " + index + " out of " + total + " to Excel");
						index++;
						if (exportContacts) {
							rowNumber = createHeaders(rowNumber, sheet, bigStyle, processName, isAdmin, standardFieldsLabels, availableVariables, addDefaultFields);
						}
						fileCellsIndexes = new ArrayList<>();
						XSSFRow row = sheet.createRow(rowNumber);
						int cellIndex = 0;

						if (addDefaultFields) {
							//	Default header values
							XSSFCell cell = row.createCell(cellIndex++);
							cell.setCellStyle(normalStyle);
							cell.setCellValue(theCase.getCaseIdentifier());

							cell = row.createCell(cellIndex++);
							cell.setCellStyle(normalStyle);
							cell.setCellValue(theCase.getCaseStatusLocalized());

							cell = row.createCell(cellIndex++);
							cell.setCellStyle(normalStyle);
							IWTimestamp created = new IWTimestamp(theCase.getCreated());
							cell.setCellValue(created.getLocaleDateAndTime(locale, DateFormat.SHORT, DateFormat.SHORT));

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
						}

						//	Variable values
						cellIndex = addVariables(availableVariables, theCase, row, sheet, bigStyle, locale, isAdmin, cellIndex, fileCellsIndexes, fileNameLabel, normalStyle, rowNumber);
						rowNumber++;

						if (exportContacts) {
							s = System.currentTimeMillis();
							CaseProcInstBind bind = getCasesBinder().getCaseProcInstBindByCaseId(Integer.valueOf(theCase.getId()));
							Long processInstanceId = bind.getProcInstId();
							ProcessManager processManager = bpmFactory.getProcessManagerByProcessInstanceId(processInstanceId);
							ProcessInstanceW piw = processManager.getProcessInstance(processInstanceId);
							Collection<User> users = processArtifacts.getUsersConnectedToProces(piw);
							addUsersToSheet(workBook, sheet, users, showCompany);
							rowNumber = (short) (sheet.getLastRowNum() + 2);
							CoreUtil.doDebug(s, System.currentTimeMillis(), "CasesSearchResultsHolderImpl.getExportedData 975: added users to sheet");
						}

						CoreUtil.doDebug(cs, System.currentTimeMillis(), "CasesSearchResultsHolderImpl.getExportedData 981: exported case " + theCase);
					}
				} else {
					int rowNumber = 0;
					if (!exportContacts) {
						rowNumber = doCreateHeaders(rowNumber, sheet, bigStyle, exportColumns, locale, processName);
					}

					int index = 1;
					int total = cases.size();
					for (CasePresentation theCase: cases.values()) {
						long cs = System.currentTimeMillis();
						LOGGER.info("Writing case no. " + index + " out of " + total + " to Excel");
						index++;
						if (exportContacts) {
							rowNumber = doCreateHeaders(
									rowNumber,
									sheet,
									bigStyle,
									exportColumns,
									locale,
									StringUtil.isEmpty(theCase.getProcessName()) ? processName : theCase.getProcessName()
							);
						}

						XSSFRow row = sheet.createRow(rowNumber);
						rowNumber++;
						int cellIndex = 0;

						List<AdvancedProperty> varsForCase = getVariablesForCase(null, theCase, locale, isAdmin);
						Map<String, VariableInstance> processData = null;
						if (!ListUtil.isEmpty(varsForCase)) {
							processData = new HashMap<>();
							for (AdvancedProperty caseVar: varsForCase) {
								String name = caseVar == null ? null : caseVar.getName();
								if (StringUtil.isEmpty(name)) {
									continue;
								}

								processData.put(name, new BPMProcessVariable(name, caseVar.getId(), null));
							}
						}

						for (String column: exportColumns) {
							String value = null;
							s = System.currentTimeMillis();
							if (column.startsWith(CaseConstants.CASE_PREFIX)) {
								if (CaseConstants.CASE_IDENTIFIER.equals(column)) {
									value = theCase.getCaseIdentifier();

								} else if (CaseConstants.CASE_CREATION_DATE.equals(column)) {
									IWTimestamp created = new IWTimestamp(theCase.getCreated());
									value = created.getLocaleDateAndTime(locale, DateFormat.SHORT, DateFormat.SHORT);

								} else if (CaseConstants.CASE_STATUS.equals(column)) {
									value = theCase.getCaseStatusLocalized();

								} else if (CaseConstants.CASE_BODY.equals(column)) {
									value = theCase.getBody();

								} else {
									LOGGER.warning("Do not know how to resolve value for column " + column);
									value = CoreConstants.MINUS;
								}
							} else {
								AdvancedProperty variable = null;
								if (column.equals("string_violatorPostalCode"))
									variable = getVariableByName(varsForCase, "string_ticketStreetAddress");

								else if (column.equals("string_ticketType") || column.equals("string_industryMainGroup") || column.equals("string_industry")) {
									variable = getVariableByName(varsForCase, column);
									if (variable == null) {
										value = getResolver(column).getPresentation(column, theCase.getId());
									}

								} else if (
										column.equals("string_ticketMeterNumber") ||
										column.equals("string_ticketStreetDescription") ||
										column.equals("list_ticketViolationsNumbers")
								) {
									variable = getVariableByName(varsForCase, column);
									if (variable == null || StringUtil.isEmpty(variable.getValue())) {
										MultipleSelectionVariablesResolver resolver = getResolver(column);
										value = resolver.isValueUsedForExport() ?
												resolver.getPresentation(column, theCase.getId()) :
												resolver.getKeyPresentation(Integer.valueOf(theCase.getId()), null);
									}
								} else if (ProcessConstants.CASE_STATUS.equals(column)) {
									value = theCase.getCaseStatusLocalized();

								} else if (column.equalsIgnoreCase("date_range") || column.equalsIgnoreCase("dateRange")) {
									CasesSearchCriteriaBean criteria = getSearchCriteria(id);
									if (criteria != null && !StringUtil.isEmpty(criteria.getDateRange())) {
										value = criteria.getDateRange();
									}

								} else {
									variable = getVariableByName(varsForCase, column);
								}

								if (value == null) {
									value = getVariableValue(column, variable, processData, searchCriteria);
								}
								if ("string_ownerGender".equals(column)) {
									value = localizeBPM(value, value);
								}
							}
							CoreUtil.doDebug(s, System.currentTimeMillis(), "CasesSearchResultsHolderImpl.getExportedData: got variable " + column + " and value " + value);

							XSSFCell cell = row.createCell(cellIndex++);
							cell.setCellStyle(normalStyle);
							cell.setCellValue(value);
							if (exportContacts) {
								s = System.currentTimeMillis();
								CaseProcInstBind bind = getCasesBinder().getCaseProcInstBindByCaseId(Integer.valueOf(theCase.getId()));
								Long processInstanceId = bind.getProcInstId();
								ProcessManager processManager = bpmFactory.getProcessManagerByProcessInstanceId(processInstanceId);
								ProcessInstanceW piw = processManager.getProcessInstance(processInstanceId);
								Collection<User> users = processArtifacts.getUsersConnectedToProces(piw);
								addUsersToSheet(workBook, sheet, users, showCompany);
								CoreUtil.doDebug(s, System.currentTimeMillis(), "CasesSearchResultsHolderImpl.getExportedData: addded users to sheet");
							}
						}

						CoreUtil.doDebug(cs, System.currentTimeMillis(), "CasesSearchResultsHolderImpl.getExportedData 1094: exported case " + theCase);
					}
				}
			}

			try {
				s = System.currentTimeMillis();
				workBook.write(streamOut);
				workBook.close();
				CoreUtil.doDebug(s, System.currentTimeMillis(), "CasesSearchResultsHolderImpl.getExportedData: wrote workboot data to output stream");

				s = System.currentTimeMillis();
				streamOut.flush();
				memory = streamOut.toByteArray();
				CoreUtil.doDebug(s, System.currentTimeMillis(), "CasesSearchResultsHolderImpl.getExportedData: got byte array from output stream");
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Error writing search results to Excel!", e);
				return null;
			} finally {
				IOUtil.closeOutputStream(streamOut);
			}

			return memory;
		} finally {
			CoreUtil.doDebug(start, System.currentTimeMillis(), "CasesSearchResultsHolderImpl.getExportedData: finished");
		}
	}

	private void addUsersToSheet(
			XSSFWorkbook workBook,
			XSSFSheet sheet,
			Collection<User> users,
			boolean showUserCompany
	) {
		XSSFFont bigFont = workBook.createFont();
		bigFont.setBold(true);
		bigFont.setFontHeightInPoints((short) 16);
		XSSFCellStyle bigStyle = workBook.createCellStyle();
		bigStyle.setFont(bigFont);

		XSSFFont normalFont = workBook.createFont();
		normalFont.setFontHeightInPoints((short) 16);
		XSSFCellStyle normalStyle = workBook.createCellStyle();
		normalStyle.setFont(normalFont);

		int columnWidth = DEFAULT_CELL_WIDTH;
		int rowNum = sheet.getLastRowNum()+1;
		XSSFRow row = sheet.createRow(rowNum++);

		int column = 0;

		sheet.setColumnWidth(column, columnWidth);
		XSSFCell cell = row.createCell(column++);
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
	public byte[] getUsersExport(Collection<User> users,Locale locale,boolean showUserCompany){
		FastByteArrayOutputStream streamOut = new FastByteArrayOutputStream();
		XSSFWorkbook workBook = new XSSFWorkbook();

		XSSFSheet sheet = workBook.createSheet();
		addUsersToSheet(workBook, sheet, users, showUserCompany);
		try {
			workBook.write(streamOut);
			workBook.close();
			streamOut.flush();
			memory = streamOut.toByteArray();
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

		List<String> labels = new ArrayList<>();
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

		List<String> values = new ArrayList<>();
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
	public byte[] getExportedSearchResults(String id, boolean exportContacts, boolean showCompany) {
		return getExportedSearchResults(id, exportContacts, showCompany, true);
	}

	@Override
	public byte[] getExportedSearchResults(String id, boolean exportContacts, boolean showCompany, boolean addDefaultFields) {
		return getExportedSearchResults(id, exportContacts, showCompany, addDefaultFields, null);
	}

	@Override
	public byte[] getExportedSearchResults(String id, boolean exportContacts, boolean showCompany, boolean addDefaultFields, String category) {
		if (memory != null)
			return memory;

		if (doExport(id, exportContacts, showCompany, addDefaultFields)) {
			return memory;
		}

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
	public List<CasePresentation> getAllCases(String id) {
		if (StringUtil.isEmpty(id)) {
			LOGGER.warning("Key is not provided");
			return null;
		}

		return externalData.remove(id);
	}

	@Override
	public byte[] getExportedCases(String id, boolean exportContacts, boolean showCompany, boolean addDefaultFields) {
		return getExportedCases(id, exportContacts, showCompany, addDefaultFields);
	}

	@Override
	public byte[] getExportedCases(String id, boolean exportContacts, boolean showCompany, boolean addDefaultFields, String category) {
		if (StringUtil.isEmpty(id)) {
			LOGGER.warning("Key is not provided");
			return null;
		}

		List<CasePresentation> cases = getAllCases(id);
		if (ListUtil.isEmpty(cases)) {
			LOGGER.warning("No cases found by key: " + id);
			return null;
		}

		Map<String, Map<String, CasePresentation>> casesByProcDef = getCasesByProcessDefinition(cases, category);
		CasesSearchCriteriaBean bean = getSearchCriteria(id);

		return getExportedData(casesByProcDef, null, bean == null ? null : bean.getExportColumns(), exportContacts, showCompany, addDefaultFields, category);
	}

	private Map<String, Map<String, CasePresentation>> getCasesByProcessDefinition(String id, String defaultCategory) {
		if (!isSearchResultStored(id, true)) {
			return null;
		}

		Collection<CasePresentation> cases = getCases(id, true);
		return getCasesByProcessDefinition(cases, defaultCategory);
	}

	private Map<String, Map<String, CasePresentation>> getCasesByProcessDefinition(Collection<CasePresentation> cases, String defaultCategory) {
		long start = System.currentTimeMillis();
		try {
			Map<String, Map<String, CasePresentation>> casesByCategories = new HashMap<>();
			IWMainApplicationSettings settings = IWMainApplication.getDefaultIWMainApplication().getSettings();
			for (CasePresentation theCase: cases) {
				boolean byCode = false;
				String caseCode = theCase.getCode();
				if (!StringUtil.isEmpty(caseCode) && settings.getBoolean("cases.exp_group_c_".concat(caseCode))) {
					byCode = true;
				}

				String bpmProcessName = theCase.getProcessName();
				String processName = theCase.isBpm() ? bpmProcessName : theCase.getCategoryId();
				if (byCode) {
					processName = StringUtil.isEmpty(bpmProcessName) ? caseCode : bpmProcessName;

				} else {
					if (StringUtil.isEmpty(processName) && !StringUtil.isEmpty(defaultCategory)) {
						processName = defaultCategory;
					}

					if (StringUtil.isEmpty(processName)) {
						processName = CasesStatistics.UNKOWN_CATEGORY_ID;
					}
				}

				Map<String, CasePresentation> casesByProcessDefinition = casesByCategories.get(processName);
				if (casesByProcessDefinition == null) {
					casesByProcessDefinition = new LinkedHashMap<>();
					casesByCategories.put(processName, casesByProcessDefinition);
				}
				casesByProcessDefinition.put(theCase.getId(), theCase);
			}

			return casesByCategories;
		} finally {
			CoreUtil.doDebug(start, System.currentTimeMillis(), "CasesSearchResultsHolderImpl.getCasesByProcessDefinition");
		}
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
			LOGGER.warning("Unkown current case's id or no search results stored!");
			return null;
		}

		Collection<CasePresentation> cases = getCases(id);
		CasePresentation nextCase = null;

		if (!StringUtil.isEmpty(processDefinitionName)) {
			List<CasePresentation> casesFromTheSameProcessDefinition = new ArrayList<>();
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
			data = new ArrayList<>(externalData);
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