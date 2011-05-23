package is.idega.idegaweb.egov.bpm.cases.board;

import is.idega.idegaweb.egov.bpm.business.TaskViewerHelper;
import is.idega.idegaweb.egov.bpm.cases.CaseProcessInstanceRelationImpl;
import is.idega.idegaweb.egov.cases.business.BoardCasesComparator;
import is.idega.idegaweb.egov.cases.business.BoardCasesManager;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;
import is.idega.idegaweb.egov.cases.presentation.CasesBoardViewer;
import is.idega.idegaweb.egov.cases.presentation.beans.CaseBoardBean;
import is.idega.idegaweb.egov.cases.presentation.beans.CaseBoardTableBean;
import is.idega.idegaweb.egov.cases.presentation.beans.CaseBoardTableBodyRowBean;
import is.idega.idegaweb.egov.cases.util.CasesConstants;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.process.business.CasesRetrievalManager;
import com.idega.block.process.data.Case;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.core.contact.data.Email;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.bean.VariableStringInstance;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewSubmission;
import com.idega.presentation.IWContext;
import com.idega.user.business.NoEmailFoundException;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service
@Transactional
public class BoardCasesManagerImpl implements BoardCasesManager {

	private static final List<String> GRADING_VARIABLES = Collections
	        .unmodifiableList(Arrays.asList(
	        		"string_ownerInnovationalValue",	//	0
	        		"string_ownerCompetitionValue",		//	1
	        		"string_ownerEntrepreneursValue",	//	2
	        		"string_ownerPossibleDevelopments",	//	3
	        		"string_ownerNatureStatus",			//	4
	        		"string_ownerApplication",			//	5
	        		"string_ownerOverturn",				//	6
	        		"string_ownerProceeds",				//	7
	        		"string_ownerEconomist",			//	8
	        		"string_ownerEmployees",			//	9
	        		"string_ownerForsvarsmenn",			//	10
	        		"string_ownerConstant",				//	11
	        		"string_ownerNewConstant",			//	12
	        		"string_ownerBusiness",				//	13
	        		"string_ownerProject",				//	14
	        		"string_ownerCostValue",			//	15
	        		"string_ownerProjectedSize",		//	16
	            	"string_ownerEntrepreneurCompany",	//	17
	            	"string_expectedResultDescriptionValue" // 18
	 ));

	private static final Logger LOGGER = Logger.getLogger(BoardCasesManagerImpl.class.getName());

	public static final String BOARD_CASES_LIST_SORTING_PREFERENCES = "boardCasesListSortingPreferencesAttribute";

	private CasesRetrievalManager caseManager;

	@Autowired
	private BPMFactory bpmFactory;

	@Autowired
	private CaseProcessInstanceRelationImpl caseProcessInstanceRelation;

	@Autowired
	private CasesBPMDAO casesBPMDAO;

	@Autowired
	private VariableInstanceQuerier variablesQuerier;

	@Autowired
	private TaskViewerHelper taskViewer;

	private List<String> variables;

	@Override
	public List<CaseBoardBean> getAllSortedCases(IWContext iwc, IWResourceBundle iwrb, String caseStatus, String processName) {
		Collection<GeneralCase> cases = getCases(iwc, caseStatus, processName);

		if (ListUtil.isEmpty(cases)) {
			return null;
		}

		Map<Integer, User> casesIdsAndHandlers = new HashMap<Integer, User>();
		for (GeneralCase theCase : cases) {
			if (isCaseAvailableForBoard(theCase)) {
				try {
					casesIdsAndHandlers.put(Integer.valueOf(theCase.getPrimaryKey().toString()), theCase.getHandledBy());
				} catch(NumberFormatException e) {
					LOGGER.warning("Cann't convert to integer: " + theCase);
				}
			}
		}

		List<CaseBoardBean> boardCases = getFilledBoardCaseWithInfo(casesIdsAndHandlers);
		if (ListUtil.isEmpty(boardCases)) {
			return null;
		}

		sortBoardCases(iwc, boardCases);

		return boardCases;
	}

	private List<CaseBoardBean> getFilledBoardCaseWithInfo(Map<Integer, User> casesIdsAndHandlers) {
		List<String> allVariables = new ArrayList<String>(getVariables());
		allVariables.addAll(GRADING_VARIABLES);
		List<CaseBoardView> boardViews = getStringVariablesValuesByVariablesNamesForCases(casesIdsAndHandlers, allVariables);
		if (ListUtil.isEmpty(boardViews)) {
			return null;
		}

		List<CaseBoardBean> boardCases = new ArrayList<CaseBoardBean>();
		for (CaseBoardView view: boardViews) {
			CaseBoardBean boardCase = new CaseBoardBean(view.getCaseId(), view.getProcessInstanceId());

			boardCase.setApplicantName(view.getValue(CasesBoardViewer.CASE_FIELDS.get(0).getId()));
			boardCase.setAddress(view.getValue(CasesBoardViewer.CASE_FIELDS.get(1).getId()));
			boardCase.setPostalCode(view.getValue(CasesBoardViewer.CASE_FIELDS.get(2).getId()));
			boardCase.setCaseIdentifier(view.getValue(CasesBoardViewer.CASE_FIELDS.get(3).getId()));
			boardCase.setCaseDescription(view.getValue(CasesBoardViewer.CASE_FIELDS.get(4).getId()));

			boardCase.setTotalCost(String.valueOf(getNumberValue(view.getValue(CasesBoardViewer.CASE_FIELDS.get(5).getId()), true)));
			boardCase.setAppliedAmount(String.valueOf(getNumberValue(view.getValue(CasesBoardViewer.CASE_FIELDS.get(6).getId()), true)));

			boardCase.setNutshell(view.getValue(CasesBoardViewer.CASE_FIELDS.get(7).getId()));

			boardCase.setGradingSum(getGradingSum(view)[0]);
			boardCase.setNegativeGradingSum(getGradingSum(view)[1]);

			boardCase.setCategory(view.getValue(CasesBoardViewer.CASE_FIELDS.get(10).getId()));
			boardCase.setComment(view.getValue(CasesBoardViewer.CASE_FIELDS.get(11).getId()));

			boardCase.setGrantAmountSuggestion(getNumberValue(view.getValue(CasesBoardViewer.CASE_FIELDS.get(12).getId()), false));
			boardCase.setBoardAmount(getNumberValue(view.getValue(CasesBoardViewer.CASE_FIELDS.get(13).getId()), false));
			boardCase.setRestrictions(view.getValue(CasesBoardViewer.CASE_FIELDS.get(14).getId()));

			boardCase.setHandler(view.getHandler());

			if (StringUtil.isEmpty(boardCase.getApplicantName())) {
				LOGGER.warning("Applicant name is unknown: ".concat(boardCase.toString()).concat(". Probably some error occured gathering data."));
			}

			boardCases.add(boardCase);
		}

		return boardCases;
	}

	private CaseBoardView getCaseView(List<CaseBoardView> views, Long processInstanceId) {
		if (ListUtil.isEmpty(views) || processInstanceId == null) {
			return null;
		}

		for (CaseBoardView view: views) {
			if (processInstanceId.longValue() == view.getProcessInstanceId().longValue()) {
				return view;
			}
		}

		return null;
	}

	private Integer getMapedCaseId(Map<Integer, Long> processMap, Long processInstanceId) {
		for (Entry<Integer, Long> processBind: processMap.entrySet()) {
			if (processBind.getValue().longValue() == processInstanceId.longValue()) {
				return processBind.getKey();
			}
		}
		return null;
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	private List<CaseBoardView> getStringVariablesValuesByVariablesNamesForCases(Map<Integer, User> casesIdsAndHandlers, List<String> variablesNames) {
		Map<Integer, Long> processes = getCaseProcessInstanceRelation().getCasesProcessInstancesIds(casesIdsAndHandlers.keySet());

		Collection<VariableInstanceInfo> variables = getVariablesQuerier()
			.getVariablesByProcessInstanceIdAndVariablesNames(variablesNames, processes.values(), true, false, false);
		if (ListUtil.isEmpty(variables)) {
			LOGGER.warning("Didn't find any variables values for processes " + processes.values() + " and variables names " + variablesNames);
			return null;
		}

		List<CaseBoardView> views = new ArrayList<CaseBoardView>();
		for (VariableInstanceInfo variable: variables) {
			if (variable instanceof VariableStringInstance && (variable.getName() != null && variable.getValue() != null && variable.getProcessInstanceId() != null)) {
				Long processInstanceId = variable.getProcessInstanceId();
				CaseBoardView view = getCaseView(views, processInstanceId);
				if (view == null) {
					Integer caseId = getMapedCaseId(processes, processInstanceId);
					if (caseId == null) {
						LOGGER.warning("Case ID was not found in " + processes + " for process instance ID: " + processInstanceId);
					} else {
						view = new CaseBoardView(caseId.toString(), processInstanceId);
						view.setHandler(casesIdsAndHandlers.get(caseId));
						views.add(view);
					}
				}

				if (view == null) {
					LOGGER.warning("Couldn't get view bean for process: " + processInstanceId + ": " + processes);
				} else {
					view.addVariable(variable.getName(), variable.getValue().toString());
				}
			} else {
				LOGGER.warning(variable + " can not be added to board view!");
			}
		}

		return views;
	}

	private Long getNumberValue(String value, boolean dropThousands) {
		if (StringUtil.isEmpty(getStringValue(value))) {
			return Long.valueOf(0);
		}

		value = value.replaceAll(CoreConstants.SPACE, CoreConstants.EMPTY);
		value = value.replace(CoreConstants.DOT, CoreConstants.EMPTY);
		value = value.replace("þús", CoreConstants.EMPTY);
		value = value.replaceAll("kr", CoreConstants.EMPTY);

		if (StringUtil.isEmpty(value)) {
			return Long.valueOf(0);
		}

		Double numberValue = null;
		try {
			numberValue = Double.valueOf(value);

			if (dropThousands) {
				return Long.valueOf(Double.valueOf(numberValue.doubleValue() / 1000).longValue());
			}

			return Long.valueOf(numberValue.longValue());
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting number value from: " + value);
		}

		return Long.valueOf(0);
	}

	private boolean isCaseAvailableForBoard(GeneralCase theCase) {
		String managerType = theCase.getCaseManagerType();
		if (StringUtil.isEmpty(managerType) || !managerType.equals("CasesBPM")) {
			return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private void sortBoardCases(IWContext iwc, List<CaseBoardBean> boardCases) {
		if (ListUtil.isEmpty(boardCases)) {
			return;
		}

		List<String> sortingPreferences = null;
		Object o = iwc.getSessionAttribute(BOARD_CASES_LIST_SORTING_PREFERENCES);
		if (o instanceof List) {
			sortingPreferences = (List<String>) o;
		}

		Collections.sort(boardCases, new BoardCasesComparator(iwc.getLocale(), sortingPreferences));
	}

	@SuppressWarnings("unchecked")
	private Collection<GeneralCase> getCases(IWApplicationContext iwac, String caseStatus, String processName) {
		Collection<Case> allCases = null;
		if (!StringUtil.isEmpty(processName)) {
			// Getting cases by application
			allCases = getCasesByProcessAndCaseStatus(iwac, caseStatus, processName);
		} else {
			// Getting cases by case status
			if (StringUtil.isEmpty(caseStatus)) {
				LOGGER.warning("Case status is unkown - terminating!");
				return null;
			}
			CasesBusiness casesBusiness = getCasesBusiness(iwac);
			if (casesBusiness == null) {
				LOGGER.warning(CasesBusiness.class + " is null!");
				return null;
			}
			try {
				allCases = casesBusiness.getCasesByCriteria(null, null, null, casesBusiness.getCaseStatus(caseStatus), false);
			} catch (RemoteException e) {
				LOGGER.log(Level.SEVERE, "Error getting cases by cases status: " + caseStatus, e);
			}
		}

		if (ListUtil.isEmpty(allCases)) {
			return null;
		}

		Collection<GeneralCase> bpmCases = new ArrayList<GeneralCase>();
		for (Case theCase : allCases) {
			if (theCase instanceof GeneralCase) {
				bpmCases.add((GeneralCase) theCase);
			}
		}

		return bpmCases;
	}

	private Collection<Case> getCasesByProcessAndCaseStatus(IWApplicationContext iwac, String caseStatus, String processName) {
		CasesRetrievalManager caseManager = getCaseManager();
		if (caseManager == null) {
			LOGGER.severe(CasesRetrievalManager.class + " bean was not initialized!");
			return null;
		}

		Collection<Long> casesIdsByProcessDefinition = caseManager.getCasesIdsByProcessDefinitionName(processName);
		if (ListUtil.isEmpty(casesIdsByProcessDefinition)) {
			return null;
		}

		List<Integer> ids = new ArrayList<Integer>(casesIdsByProcessDefinition.size());
		for (Long id : casesIdsByProcessDefinition) {
			ids.add(id.intValue());
		}

		Collection<Case> cases = getCasesBusiness(iwac).getCasesByIds(ids);
		if (ListUtil.isEmpty(cases)) {
			return null;
		}

		if (StringUtil.isEmpty(caseStatus)) {
			return cases;
		}

		Collection<Case> casesByProcessDefinitionAndStatus = new ArrayList<Case>();
		for (Case theCase : cases) {
			if (caseStatus.equals(theCase.getStatus())) {
				casesByProcessDefinitionAndStatus.add(theCase);
			}
		}

		return casesByProcessDefinitionAndStatus;
	}

	private CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		} catch (IBOLookupException e) {
			LOGGER.log(Level.SEVERE, "Error getting " + CasesBusiness.class, e);
		}

		return null;
	}

	private CasesRetrievalManager getCaseManager() {
		if (caseManager == null) {
			try {
				caseManager = ELUtil.getInstance().getBean("casesBPMCaseHandler");
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Error getting Spring bean for: " + CasesRetrievalManager.class, e);
			}
		}
		return caseManager;
	}

	private List<String> getVariables() {
		if (variables == null) {
			variables = new ArrayList<String>(CasesBoardViewer.CASE_FIELDS.size());
			for (AdvancedProperty variable : CasesBoardViewer.CASE_FIELDS) {
				variables.add(variable.getId());
			}
		}
		return variables;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public AdvancedProperty setCaseVariableValue(Integer caseId, String variableName, String value, String role, String backPage) {
		if (caseId == null || StringUtil.isEmpty(variableName) || StringUtil.isEmpty(value)) {
			return null;
		}

		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null || !iwc.isLoggedOn()) {
			return null;
		}
		if (!StringUtil.isEmpty(role) && !iwc.hasRole(role)) {
			return null;
		}

		try {
			if (value.equals("no_value")) {
				value = CoreConstants.EMPTY;
			}

			Long processInstanceId = getCaseProcessInstanceRelation().getCaseProcessInstanceId(caseId);

			ProcessInstanceW piw = getBpmFactory().getProcessInstanceW(processInstanceId);

			String taskName = "Grading";
			List<TaskInstanceW> allTasks = piw.getUnfinishedTaskInstancesForTask(taskName);

			if (ListUtil.isEmpty(allTasks)) {
				LOGGER.warning("No tasks instances were found for task = " + taskName + " by process instance: " + processInstanceId);
				return null;
			}

			// should be only one task instance
			if (allTasks.size() > 1)
				LOGGER.warning("More than one task instance found for task = " + taskName + " when only one expected");

			TaskInstanceW sharedTIW = allTasks.iterator().next();
			Long sharedTaskInstanceId = sharedTIW.getTaskInstanceId();
			View view = sharedTIW.loadView();

			// TODO: move getViewSubmission to view too
			// TODO: add addVariable and so to the viewSubmission
			ViewSubmission viewSubmission = getBpmFactory().getViewSubmission();
			Map<String, Object> variables = view.resolveVariables();
			if (variables == null)
				variables = new HashMap<String, Object>();
			variables.put(variableName, value);

			viewSubmission.populateParameters(view.resolveParameters());
			viewSubmission.populateVariables(variables);

			Long viewTaskInstanceId = view.getTaskInstanceId();

			TaskInstanceW viewTIW = getBpmFactory().getProcessManagerByTaskInstanceId(viewTaskInstanceId).getTaskInstance(viewTaskInstanceId);

			viewTIW.submit(viewSubmission);

			return new AdvancedProperty(value, getTaskViewer().getLinkToTheTask(iwc, caseId.toString(), sharedTaskInstanceId.toString(), backPage));
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error saving variable '" + variableName + "' with value '" + value + "' for case: " + caseId, e);
		}

		return null;
	}

	/**
	 *
	 * @param view
	 * @return returns String array, 0 element contains sum of positive grade values,
	 * 1 element contains sum of negative grade values
	 */
	public String [] getGradingSum(CaseBoardView view) {
		List<String> gradingValues = view.getValues(GRADING_VARIABLES);
		String [] returnValue = new String[2];
		if (ListUtil.isEmpty(gradingValues)) {
			returnValue[0] = String.valueOf(0);
			returnValue[1] = String.valueOf(0);
			return returnValue;
		}

		long sum = 0;
		long negativeSum = 0;
		Long gradeValue = null;
		for (String value : gradingValues) {
			if (StringUtil.isEmpty(getStringValue(value))) {
				continue;
			}

			if (value.indexOf("_") != -1) {
				value = value.substring(0, value.indexOf("_"));
			}
			if (value.indexOf("a") != -1) {
				value = value.substring(0, value.indexOf("a"));
			}
			if (value.indexOf("b") != -1) {
				value = value.substring(0, value.indexOf("b"));
			}

			gradeValue = null;
			try {
				gradeValue = Long.valueOf(value);
			} catch (Exception e) {
				LOGGER.warning("Unable to convert '" + value + "' to number!");
			}

			if (gradeValue != null) {
				long longValue = gradeValue.longValue();
				sum += longValue;
				if(longValue < 0){
					negativeSum += longValue;
				}
			}
		}

		returnValue[0] = String.valueOf(sum);
		returnValue[1] = String.valueOf(negativeSum);

		return returnValue;
	}

	@Override
	public CaseBoardTableBean getTableData(IWContext iwc, String caseStatus, String processName) {
		if (iwc == null) {
			return null;
		}

		IWBundle bundle = iwc.getIWMainApplication().getBundle(CasesConstants.IW_BUNDLE_IDENTIFIER);
		IWResourceBundle iwrb = bundle.getResourceBundle(iwc);
		CaseBoardTableBean data = new CaseBoardTableBean();

		List<CaseBoardBean> boardCases = getAllSortedCases(iwc, iwrb, caseStatus, processName);
		if (ListUtil.isEmpty(boardCases)) {
			data.setErrorMessage(iwrb.getLocalizedString("cases_board_viewer.no_cases_found", "There are no cases!"));
			return data;
		}

		// Header
		data.setHeaderLabels(getTableHeaders(iwrb));

		// Body
		long grantAmountSuggestionTotal = 0;
		long boardAmountTotal = 0;
		List<CaseBoardTableBodyRowBean> bodyRows = new ArrayList<CaseBoardTableBodyRowBean>(boardCases.size());
		for (CaseBoardBean caseBoard : boardCases) {
			CaseBoardTableBodyRowBean rowBean = new CaseBoardTableBodyRowBean(caseBoard.getCaseId(), caseBoard.getProcessInstanceId());
			rowBean.setId(new StringBuilder("uniqueCaseId").append(caseBoard.getCaseId()).toString());
			rowBean.setCaseIdentifier(caseBoard.getCaseIdentifier());

			int index = 0;
			List<String> allValues = caseBoard.getAllValues();
			List<String> rowValues = new ArrayList<String>(allValues.size());
			for (String value : allValues) {
				if (index == 3) {
					// Link to grading task
					rowValues.add(caseBoard.getCaseIdentifier());
				} else {
					rowValues.add(value);
				}

				if (index == allValues.size() - 4) {
					// Calculating grant amount suggestions
					grantAmountSuggestionTotal += caseBoard.getGrantAmountSuggestion();
				} else if (index == allValues.size() - 3) {
					// Calculating board amounts
					boardAmountTotal += caseBoard.getBoardAmount();
				}

				index++;
			}

			rowBean.setHandler(caseBoard.getHandler());
			rowValues.add(caseBoard.getHandler() == null ? String.valueOf(-1) : caseBoard.getHandler().getId());

			rowBean.setValues(rowValues);
			bodyRows.add(rowBean);
		}
		data.setBodyBeans(bodyRows);

		// Footer
		data.setFooterValues(getFooterValues(iwrb, grantAmountSuggestionTotal, boardAmountTotal));

		// Everything is OK
		data.setFilledWithData(Boolean.TRUE);

		return data;
	}

	@Override
	public AdvancedProperty getHandlerInfo(IWContext iwc, User handler) {
		if (handler == null) {
			return null;
		}

		UserBusiness userBusiness = null;
		try {
			userBusiness = IBOLookup.getServiceInstance(iwc, UserBusiness.class);
		} catch(RemoteException e) {
			LOGGER.log(Level.WARNING, "Error getting " + UserBusiness.class, e);
		}
		if (userBusiness == null) {
			return null;
		}

		AdvancedProperty info = new AdvancedProperty(handler.getName());

		Email email = null;
		try {
			email = userBusiness.getUsersMainEmail(handler);
		} catch (RemoteException e) {
			LOGGER.log(Level.WARNING, "Error getting email for user: " + handler, e);
		} catch (NoEmailFoundException e) {
		}

		if (email != null) {
			info.setValue(new StringBuilder("mailto:").append(email.getEmailAddress()).toString());
		}

		return info;
	}

	private List<String> getTableHeaders(IWResourceBundle iwrb) {
		String prefix = "case_board_viewer.";
		List<String> headers = new ArrayList<String>(CasesBoardViewer.CASE_FIELDS.size());
		for (AdvancedProperty header : CasesBoardViewer.CASE_FIELDS) {
			headers.add(iwrb.getLocalizedString(new StringBuilder(prefix).append(header.getId()).toString(), header.getValue()));
		}

		headers.add(iwrb.getLocalizedString(new StringBuilder(prefix).append("case_handler").toString(), "Case handler"));

		return headers;
	}

	private List<String> getFooterValues(IWResourceBundle iwrb,
	        long grantAmountSuggestionTotal, long boardAmountTotal) {
		List<String> values = new ArrayList<String>(
		        CasesBoardViewer.CASE_FIELDS.size());

		for (int i = 0; i < CasesBoardViewer.CASE_FIELDS.size(); i++) {
			if (i == CasesBoardViewer.CASE_FIELDS.size() - 4) {
				// SUMs label
				values.add(new StringBuilder(iwrb.getLocalizedString(
				    "case_board_viewer.total_sum", "Total")).append(
				    CoreConstants.COLON).toString());
			} else if (i == CasesBoardViewer.CASE_FIELDS.size() - 3) {
				// Grant amount suggestions
				values.add(String.valueOf(grantAmountSuggestionTotal));
			} else if (i == CasesBoardViewer.CASE_FIELDS.size() - 2) {
				// Board amount
				values.add(String.valueOf(boardAmountTotal));
			} else {
				values.add(CoreConstants.EMPTY);
			}
		}

		values.add(CoreConstants.EMPTY);

		return values;
	}

	BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	CaseProcessInstanceRelationImpl getCaseProcessInstanceRelation() {
		return caseProcessInstanceRelation;
	}

	CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	private String getStringValue(String value) {
		if (StringUtil.isEmpty(value) || "no_value".equals(value)) {
			return CoreConstants.EMPTY;
		}

		return value;
	}

	private class CaseBoardView {
		private String caseId;
		private Long processInstanceId;

		private User handler;

		private List<AdvancedProperty> variables = new ArrayList<AdvancedProperty>();

		private CaseBoardView(String caseId, Long processInstanceId) {
			this.caseId = caseId;
			this.processInstanceId = processInstanceId;
		}

		public String getCaseId() {
			return caseId;
		}

		public Long getProcessInstanceId() {
			return processInstanceId;
		}

		public List<AdvancedProperty> getVariables() {
			return variables;
		}

		public void addVariable(String name, String value) {
			if (StringUtil.isEmpty(name) || StringUtil.isEmpty(value)) {
				LOGGER.warning("Variable value or name (name=" + name + ", value=" +value+ ", case=" + caseId + ", piId=" + processInstanceId + ") is undefined!");
				return;
			}

			AdvancedProperty variable = getVariable(getVariables(), name);
			if (variable == null) {
				getVariables().add(new AdvancedProperty(name, value));
				return;
			}

			if (value.equals(variable.getValue())) {
				return;
			}

			variable.setValue(value);
			return;
		}

		public String getValue(String variableName) {
			AdvancedProperty variable = getVariable(getVariables(), variableName);
			return getStringValue(variable == null ? null : variable.getValue());
		}

		public List<String> getValues(List<String> variablesNames) {
			if (ListUtil.isEmpty(variablesNames)) {
				return null;
			}

			List<String> values = new ArrayList<String>();
			for (String variableName: variablesNames) {
				values.add(getValue(variableName));
			}
			return values;
		}

		public User getHandler() {
			return handler;
		}

		public void setHandler(User handler) {
			this.handler = handler;
		}

		@Override
		public String toString() {
			return "CaseBoardView: case ID: " + caseId + ", process instance ID: " + processInstanceId;
		}
	}

	private AdvancedProperty getVariable(List<AdvancedProperty> variables, String name) {
		if (StringUtil.isEmpty(name)) {
			return null;
		}

		for (AdvancedProperty variable: variables) {
			if (name.equals(variable.getId())) {
				return variable;
			}
		}

		return null;
	}

	public TaskViewerHelper getTaskViewer() {
		return taskViewer;
	}

	public void setTaskViewer(TaskViewerHelper taskViewer) {
		this.taskViewer = taskViewer;
	}

	public void setCaseManager(CasesRetrievalManager caseManager) {
		this.caseManager = caseManager;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	public void setCaseProcessInstanceRelation(
			CaseProcessInstanceRelationImpl caseProcessInstanceRelation) {
		this.caseProcessInstanceRelation = caseProcessInstanceRelation;
	}

	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}

	@Override
	public String getLinkToTheTaskRedirector(IWContext iwc, String basePage, String caseId, Long processInstanceId, String backPage, String taskName) {
		return getTaskViewer().getLinkToTheTaskRedirector(iwc, basePage, caseId, processInstanceId, backPage, taskName);
	}

	public VariableInstanceQuerier getVariablesQuerier() {
		return variablesQuerier;
	}

	public void setVariablesQuerier(VariableInstanceQuerier variablesQuerier) {
		this.variablesQuerier = variablesQuerier;
	}
}