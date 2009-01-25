package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.bundle.ProcessBundleCasesImpl;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.CaseCategory;
import is.idega.idegaweb.egov.cases.data.CaseCategoryHome;
import is.idega.idegaweb.egov.cases.data.GeneralCase;
import is.idega.idegaweb.egov.cases.presentation.CasesStatistics;
import is.idega.idegaweb.egov.cases.util.CasesConstants;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import org.jbpm.context.exe.VariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.process.business.CaseManager;
import com.idega.block.process.data.Case;
import com.idega.block.process.variables.VariableDataType;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.data.IDOLookup;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.io.MemoryFileBuffer;
import com.idega.io.MemoryOutputStream;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.presentation.IWContext;
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

	private static final long serialVersionUID = -8865395805034867319L;
	private static final Logger LOGGER = Logger.getLogger(CasesSearchResultsHolderImpl.class.getName());
	private static final short DEFAULT_CELL_WIDTH = (short) (40 * 256);
	
	private Collection<Case> cases;
	private MemoryFileBuffer memory;

	@Autowired
	private CaseManager caseManager;
	@Autowired
	private CasesBPMDAO casesBinder;
	@Autowired
	private BPMFactory bpmFactory;
	
	public void setSearchResults(Collection<Case> cases) {
		this.cases = cases;	
	}

	public boolean doExport() {
		if (ListUtil.isEmpty(cases)) {
			return false;
		}
		
		memory = getExportedData();
		
		return memory == null ? false : true;
	}
	
	private String getSheetName(Locale locale, String categoryId) {
		if (categoryId.startsWith(ProcessBundleCasesImpl.defaultCaseCategoryName)) {
			String processName = categoryId.replaceFirst(ProcessBundleCasesImpl.defaultCaseCategoryName, CoreConstants.EMPTY);
			return getCaseManager().getProcessName(processName, locale);
		}
		
		return getCategoryName(locale, categoryId.equals(CasesStatistics.UNKOWN_CATEGORY_ID) ? null : getCaseCategory(categoryId));
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
	
	private User getCaseOwner(Case theCase) {
		return theCase.getOwner();
	}
	
	private String getCaseCreator(Case theCase) {
		User owner = getCaseOwner(theCase);
		
		String name = null;
		if (owner != null) {
			name = owner.getName();
		}
		if (StringUtil.isEmpty(name)) {
			name = getResourceBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getLocalizedString("cases.unknown_owner", "Unkown");
		}
		
		return name;
	}
	
	private String getCaseCreatorPersonalId(Case theCase) {
		User owner = getCaseOwner(theCase);
		
		String personalId = null;
		if (owner != null) {
			personalId = owner.getPersonalID();
		}
		if (StringUtil.isEmpty(personalId)) {
			personalId = getResourceBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getLocalizedString("cases.unknown_owner_personal_id", "Unkown");
		}
		
		return personalId;
	}
	
	private String getCaseIdentifier(Case theCase) {
		CaseManager caseManager = null;
		if (theCase.getCaseManagerType() != null) {
			caseManager = getCasesBusiness().getCaseHandlersProvider().getCaseManager(theCase.getCaseManagerType());
		}
		
		String caseIdentifier = caseManager == null ? theCase.getPrimaryKey().toString() : caseManager.getProcessIdentifier(theCase);
		if (StringUtil.isEmpty(caseIdentifier)) {
			caseIdentifier = theCase.getPrimaryKey().toString();
		}
		
		return caseIdentifier;
	}
	
	private CasesBusiness getCasesBusiness() {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), CasesBusiness.class);
		} catch (IBOLookupException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	@Transactional(readOnly=true)
	private List<AdvancedProperty> getAvailableVariablesByProcessDefinition(Locale locale, String processDefinition, boolean isAdmin) {
		List<VariableInstance> variablesByProcessDefinition = null;
		try {
			variablesByProcessDefinition = processDefinition.startsWith(ProcessBundleCasesImpl.defaultCaseCategoryName)
			? getCasesBinder().getVariablesByProcessDefinition(processDefinition.replaceFirst(ProcessBundleCasesImpl.defaultCaseCategoryName, CoreConstants.EMPTY))
			: null;
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting variables for process: " + processDefinition, e);
		}
		
		return getAvailableVariables(variablesByProcessDefinition, locale, isAdmin, false);
	}
	
	@Transactional(readOnly=true)
	private List<AdvancedProperty> getAvailableVariablesByProcessInstanceId(Locale locale, Long processInstanceId, boolean isAdmin) {
		List<VariableInstance> variablesByProcessInstance = null;
		try {
			variablesByProcessInstance = getCasesBinder().getVariablesByProcessInstanceId(processInstanceId);
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting variables for process instance: " + processInstanceId, e);
		}
		
		return getAvailableVariables(variablesByProcessInstance, locale, isAdmin, true);
	}
	
	@Transactional(readOnly=true)
	private List<AdvancedProperty> getAvailableVariables(List<VariableInstance> variables, Locale locale, boolean isAdmin, boolean useRealValue) {
		if (ListUtil.isEmpty(variables)) {
			return null;
		}
		
		BPMProcessVariablesBean variablesProvider = ELUtil.getInstance().getBean(BPMProcessVariablesBean.SPRING_BEAN_IDENTIFIER);
		return variablesProvider.getAvailableVariables(variables, locale, isAdmin, useRealValue);
	}
	
	private List<AdvancedProperty> createHeaders(HSSFSheet sheet, HSSFCellStyle bigStyle, Locale locale, String categoryId, boolean isAdmin) {
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
		
		List<AdvancedProperty> availableVariables = getAvailableVariablesByProcessDefinition(locale, categoryId, isAdmin);
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
	
	private void addVariables(List<AdvancedProperty> variablesByProcessDefinition, Case theCase, HSSFRow row, HSSFSheet sheet, HSSFCellStyle bigStyle,
			Locale locale, boolean isAdmin, short cellIndex, List<Integer> fileCellsIndexes, String localizedFileLabel) {
		if (ListUtil.isEmpty(variablesByProcessDefinition)) {
			return;
		}
		
		Long processInstanceId = null;
		try {
			processInstanceId = getCaseManager().getProcessInstanceId(theCase);
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
		
		ProcessInstanceW pi = null;
		try {
			pi = getBpmFactory().getProcessManagerByProcessInstanceId(processInstanceId).getProcessInstance(processInstanceId);
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting " + ProcessInstanceW.class + " by ID: " + processInstanceId, e);
		}
		if (pi == null) {
			return;
		}
		
		List<TaskInstanceW> submittedTasks = pi.getSubmittedTaskInstances();
		if (ListUtil.isEmpty(submittedTasks)) {
			return;
		}
		List<BinaryVariable> attachments = null;
		for (TaskInstanceW task: submittedTasks) {
			attachments = task.getAttachments();
			if (!ListUtil.isEmpty(attachments)) {
				for (BinaryVariable attachment: attachments) {
					VariableDataType dataType = attachment.getVariable().getDataType();
					if (dataType.equals(VariableDataType.FILES) || dataType.equals(VariableDataType.FILE)) {
						if (ListUtil.isEmpty(fileCellsIndexes) || !fileCellsIndexes.contains(Integer.valueOf(cellIndex))) {
							//	Header row
							sheet.setColumnWidth(cellIndex, DEFAULT_CELL_WIDTH);
							HSSFCell cell = sheet.getRow(0).createCell(cellIndex);
							cell.setCellValue(localizedFileLabel);
							cell.setCellStyle(bigStyle);
							
							fileCellsIndexes.add(Integer.valueOf(cellIndex));
						}
						
						//	Body row
						row.createCell(cellIndex).setCellValue(attachment.getFileName());
						cellIndex++;
					}
				}
			}
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
	
	private MemoryFileBuffer getExportedData() {
		Map<String, List<Case>> casesByCategories = getCasesByCategories();
		if (casesByCategories == null || ListUtil.isEmpty(casesByCategories.values())) {
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
		List<Case> cases = null;
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
		for (String categoryId: casesByCategories.keySet()) {
			cases = casesByCategories.get(categoryId);
			
			HSSFSheet sheet = workBook.createSheet(StringHandler.shortenToLength(getSheetName(locale, categoryId), 30));
			List<AdvancedProperty> variablesByProcessDefinition = createHeaders(sheet, bigStyle, locale, categoryId, isAdmin);
			List<Integer> fileCellsIndexes = null;
			int rowNumber = 1;
			
			for (Case theCase: cases) {
				fileCellsIndexes = new ArrayList<Integer>();
				HSSFRow row = sheet.createRow(rowNumber++);
				short cellIndex = 0;

				//	Default header values
				row.createCell(cellIndex++).setCellValue(getCaseIdentifier(theCase));
				row.createCell(cellIndex++).setCellValue(getCaseCreator(theCase));
				row.createCell(cellIndex++).setCellValue(getCaseCreatorPersonalId(theCase));
				
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
	
	private IWResourceBundle getResourceBundle(String bundleIdentifier) {
		IWContext iwc = CoreUtil.getIWContext();
		try {
			return iwc.getIWMainApplication().getBundle(bundleIdentifier).getResourceBundle(iwc);
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting resource bundle for: " + bundleIdentifier, e);
		}
		return null;
	}

	public MemoryFileBuffer getExportedSearchResults() {
		if (memory != null) {
			return memory;
		}
		
		if (doExport()) {
			return memory;
		}
		
		return null;
	}

	public boolean isSearchResultStored() {
		return ListUtil.isEmpty(cases) ? Boolean.FALSE : Boolean.TRUE;
	}
	
	private Map<String, List<Case>> getCasesByCategories() {
		if (ListUtil.isEmpty(cases)) {
			return null;
		}
		
		boolean putToMap = false;
		String categoryId = null;
		CaseCategory caseCategory = null;
		Map<String, List<Case>> casesByCategories = new HashMap<String, List<Case>>();
		for (Case theCase: cases) {
			categoryId = null;
			caseCategory = null;
			putToMap = false;
			
			if (theCase instanceof GeneralCase) {
				caseCategory = ((GeneralCase) theCase).getCaseCategory();
				if (caseCategory != null) {
					if (ProcessBundleCasesImpl.defaultCaseCategoryName.equals(caseCategory.getName())) {
						categoryId = new StringBuilder(ProcessBundleCasesImpl.defaultCaseCategoryName).append(getCaseManager().getProcessDefinitionName(theCase))
										.toString();	//	BPM case
					} else { 
						categoryId = caseCategory.getPrimaryKey().toString();
					}
				}
			}
			if (StringUtil.isEmpty(categoryId)) {
				categoryId = CasesStatistics.UNKOWN_CATEGORY_ID;
			}
		
			List<Case> cases = casesByCategories.get(categoryId);
			if (ListUtil.isEmpty(cases)) {
				cases = new ArrayList<Case>();
			}
			if (!cases.contains(theCase)) {
				cases.add(theCase);
				putToMap = true;
			}
			if (putToMap) {
				casesByCategories.put(categoryId, cases);
			}
		}
		
		return casesByCategories;
	}

	public CaseManager getCaseManager() {
		return caseManager;
	}

	public void setCaseManager(CaseManager caseManager) {
		this.caseManager = caseManager;
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

}