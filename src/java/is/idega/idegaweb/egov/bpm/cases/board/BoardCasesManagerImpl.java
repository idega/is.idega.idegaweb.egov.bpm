package is.idega.idegaweb.egov.bpm.cases.board;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.business.TaskViewerHelper;
import is.idega.idegaweb.egov.bpm.cases.CaseProcessInstanceRelationImpl;
import is.idega.idegaweb.egov.bpm.cases.actionhandlers.CaseHandlerAssignmentHandler;
import is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManagerImpl;
import is.idega.idegaweb.egov.bpm.cases.presentation.beans.BPMProcessVariablesBean;
import is.idega.idegaweb.egov.cases.business.BoardCasesComparator;
import is.idega.idegaweb.egov.cases.business.BoardCasesManager;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;
import is.idega.idegaweb.egov.cases.presentation.CasesBoardViewCustomizer;
import is.idega.idegaweb.egov.cases.presentation.CasesBoardViewer;
import is.idega.idegaweb.egov.cases.presentation.beans.CaseBoardBean;
import is.idega.idegaweb.egov.cases.presentation.beans.CaseBoardTableBean;
import is.idega.idegaweb.egov.cases.presentation.beans.CaseBoardTableBodyRowBean;
import is.idega.idegaweb.egov.cases.util.CasesConstants;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.process.business.CasesRetrievalManager;
import com.idega.block.process.business.ProcessConstants;
import com.idega.block.process.data.Case;
import com.idega.bpm.xformsview.converters.ObjectCollectionConverter;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.core.contact.data.Email;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.jbpm.bean.VariableByteArrayInstance;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.utils.JBPMConstants;
import com.idega.presentation.IWContext;
import com.idega.user.business.NoEmailFoundException;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.expression.ELUtil;

@Service(BoardCasesManager.BEAN_NAME)
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class BoardCasesManagerImpl implements BoardCasesManager {

	protected static final List<String> GRADING_VARIABLES = Collections.unmodifiableList(Arrays.asList(
	        		"string_ownerInnovationalValue",			//	0
	        		"string_ownerCompetitionValue",				//	1
	        		"string_ownerEntrepreneursValue",			//	2
	        		"string_ownerPossibleDevelopments",			//	3
	        		"string_ownerNatureStatus",					//	4
	        		"string_ownerApplication",					//	5
	        		"string_ownerOverturn",						//	6
	        		"string_ownerProceeds",						//	7
	        		"string_ownerEconomist",					//	8
	        		"string_ownerEmployees",					//	9
	        		"string_ownerForsvarsmenn",					//	10
	        		"string_ownerConstant",						//	11
	        		"string_ownerNewConstant",					//	12
	        		"string_ownerBusiness",						//	13
	        		"string_ownerProject",						//	14
	        		"string_ownerCostValue",					//	15
	        		"string_ownerProjectedSize",				//	16
	            	"string_ownerEntrepreneurCompany",			//	17
	            	"string_expectedResultDescriptionValue",	//	18
	            	"string_possibleImpactValue",				//	19
	            	"string_financeDescriptionValue",			//	20
	            	"string_costAndMainTasksGrade",				//	21
	            	"string_planForFundingGrade",				//	22
	            	"string_evaluationOfOtherGrantsGrade"		//	23
	 ));

	protected static final Logger LOGGER = Logger.getLogger(BoardCasesManagerImpl.class.getName());

	public static final String BOARD_CASES_LIST_SORTING_PREFERENCES = "boardCasesListSortingPreferencesAttribute";

	private CasesRetrievalManager caseManager;

	@Autowired
	private CaseProcessInstanceRelationImpl caseProcessInstanceRelation;

	@Autowired
	private VariableInstanceQuerier variablesQuerier;

	@Autowired
	private TaskViewerHelper taskViewer;

	private List<String> variables;

	@Override
	public List<CaseBoardBean> getAllSortedCases(IWContext iwc,
			IWResourceBundle iwrb, Collection<String> caseStatus,
			String processName, String uuid) {
		Collection<GeneralCase> cases = getCases(iwc, caseStatus, processName);
		if (ListUtil.isEmpty(cases))
			return null;

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

		List<CaseBoardBean> boardCases = getFilledBoardCaseWithInfo(casesIdsAndHandlers, uuid);
		if (ListUtil.isEmpty(boardCases))
			return null;

		sortBoardCases(iwc, boardCases);

		return boardCases;
	}

	protected List<CaseBoardBean> getFilledBoardCaseWithInfo(Map<Integer, User> casesIdsAndHandlers, String uuid) {
		List<String> variablesToQuery = new ArrayList<String>(getVariables(uuid));
		if (variablesToQuery.contains(CasesBoardViewCustomizer.FINANCING_TABLE_COLUMN)) {
			variablesToQuery.remove(CasesBoardViewCustomizer.FINANCING_TABLE_COLUMN);
			variablesToQuery.add(ProcessConstants.FINANCING_OF_THE_TASKS);
		}
		variablesToQuery.add(ProcessConstants.BOARD_FINANCING_SUGGESTION);
		variablesToQuery.add(ProcessConstants.BOARD_FINANCING_DECISION);
		List<String> allVariables = new ArrayList<String>(variablesToQuery);
		allVariables.addAll(getGradingVariables());

		List<CaseBoardView> boardViews = getVariablesValuesByNamesForCases(casesIdsAndHandlers, allVariables);
		if (ListUtil.isEmpty(boardViews))
			return null;

		List<String> numberVariables = Arrays.asList(
				CasesBoardViewer.CASE_FIELDS.get(7).getId(),
				CasesBoardViewer.CASE_FIELDS.get(8).getId(),

				ProcessConstants.BOARD_FINANCING_SUGGESTION,
				ProcessConstants.BOARD_FINANCING_DECISION
		);

		List<CaseBoardBean> boardCases = new ArrayList<CaseBoardBean>();
		for (CaseBoardView view: boardViews) {
			CaseBoardBean boardCase = new CaseBoardBean(view.getCaseId(), view.getProcessInstanceId());

			boardCase.setApplicantName(view.getValue(CasesBoardViewer.CASE_FIELDS.get(0).getId()));
			boardCase.setCaseIdentifier(view.getValue(CasesBoardViewer.CASE_FIELDS.get(5).getId()));

			String[] gradingSums = getGradingSum(view);

			boardCase.setCategory(view.getValue(CasesBoardViewer.CASE_FIELDS.get(12).getId()));

			long boardDecision = getNumberValue(view.getValue(ProcessConstants.BOARD_FINANCING_DECISION), false);
			boardCase.setBoardAmount(boardDecision);
			long boardSuggestion = getNumberValue(view.getValue(ProcessConstants.BOARD_FINANCING_SUGGESTION), false);
			boardCase.setGrantAmountSuggestion(boardSuggestion);

			boardCase.setHandler(view.getHandler());

			for (String variable: variablesToQuery) {
				String value = view.getValue(variable);
				if (numberVariables.contains(variable)) {
					if (variable.equals(ProcessConstants.BOARD_FINANCING_DECISION) || variable.equals(ProcessConstants.BOARD_FINANCING_SUGGESTION))
						value = String.valueOf(getNumberValue(value, false));
					else
						value = String.valueOf(getNumberValue(value, true));
				}
				boardCase.addValue(variable, value);
			}
			boardCase.addValue(CasesBoardViewer.CASE_FIELDS.get(10).getId(), gradingSums[1]);
			boardCase.addValue(CasesBoardViewer.CASE_FIELDS.get(11).getId(), gradingSums[0]);

			boardCase.setFinancingOfTheTasks(view.getFinancingOfTheTasks());

			boardCases.add(boardCase);
		}

		return boardCases;
	}

	private CaseBoardView getCaseView(List<CaseBoardView> views, Long processInstanceId) {
		if (ListUtil.isEmpty(views) || processInstanceId == null)
			return null;

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
	protected List<CaseBoardView> getVariablesValuesByNamesForCases(Map<Integer, User> casesIdsAndHandlers, List<String> variablesNames) {
		Map<Integer, Long> processes = getCaseProcessInstanceRelation().getCasesProcessInstancesIds(casesIdsAndHandlers.keySet());

		Collection<VariableInstanceInfo> variables = getVariablesQuerier()
				.getVariablesByProcessInstanceIdAndVariablesNames(variablesNames, processes.values(), true, false, false);
		if (ListUtil.isEmpty(variables)) {
			LOGGER.warning("Didn't find any variables values for processes " + processes.values() + " and variables names " + variablesNames);
			return null;
		}

		List<CaseBoardView> views = new ArrayList<CaseBoardView>();
		for (VariableInstanceInfo variable: variables) {
			Serializable value = variable.getValue();
			if (variable.getName() != null && value != null && variable.getProcessInstanceId() != null) {
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
					continue;
				}

				if (variable instanceof VariableByteArrayInstance) {
					if (ProcessConstants.FINANCING_OF_THE_TASKS.equals(variable.getName())) {
						List<Map<String, String>> obValue = getObjectValue((VariableByteArrayInstance) variable);

						List<Map<String, String>> financing = view.getFinancingOfTheTasks();
						if (financing == null)
							view.setFinancingOfTheTasks(obValue);
						else {
							int taskIndex = 0;
							for (Map<String, String> taskInfo: obValue) {
								if (MapUtil.isEmpty(taskInfo))
									continue;

								String taskName = taskInfo.get("task");
								if (StringUtil.isEmpty(taskName))
									continue;

								String estimatedCost = taskInfo.get("cost_estimate");

								Map<String, String> taskInfoFromBoard = taskIndex < financing.size() ? financing.get(taskIndex) : null;
								if (taskInfoFromBoard == null) {
									taskInfoFromBoard = new HashMap<String, String>();
									financing.add(taskInfoFromBoard);
								}

								taskInfoFromBoard.put(CasesBoardViewer.WORK_ITEM, taskName);
								taskInfoFromBoard.put(CasesBoardViewer.ESTIMATED_COST, estimatedCost);

								String tmp = taskInfoFromBoard.get(ProcessConstants.BOARD_FINANCING_SUGGESTION);
								if (StringUtil.isEmpty(tmp))
									taskInfoFromBoard.put(ProcessConstants.BOARD_FINANCING_SUGGESTION, CoreConstants.MINUS);
								tmp = taskInfoFromBoard.get(ProcessConstants.BOARD_FINANCING_DECISION);
								if (StringUtil.isEmpty(tmp))
									taskInfoFromBoard.put(ProcessConstants.BOARD_FINANCING_DECISION, CoreConstants.MINUS);

								taskIndex++;
							}
						}
					} else if (ProcessConstants.BOARD_FINANCING_SUGGESTION.equals(variable.getName())) {
						fillWithBoardInfoOnTheTasks(variable, view, CasesBoardViewer.BOARD_SUGGESTION);
					} else if (ProcessConstants.BOARD_FINANCING_DECISION.equals(variable.getName())) {
						fillWithBoardInfoOnTheTasks(variable, view, CasesBoardViewer.BOARD_DECISION);
					}
				} else if (ProcessConstants.BOARD_FINANCING_SUGGESTION.equals(variable.getName())) {
					fillWithBoardInfoOnTheTasks(variable, view, CasesBoardViewer.BOARD_SUGGESTION);
					view.addVariable(variable.getName(), value.toString());
				} else if (ProcessConstants.BOARD_FINANCING_DECISION.equals(variable.getName())) {
					fillWithBoardInfoOnTheTasks(variable, view, CasesBoardViewer.BOARD_DECISION);
					view.addVariable(variable.getName(), value.toString());
				} else
					view.addVariable(variable.getName(), value.toString());
			} else {
				LOGGER.warning(variable + " can not be added to board view!");
			}
		}

		return views;
	}

	private void fillWithBoardInfoOnTheTasks(VariableInstanceInfo variable, CaseBoardView view, String key) {
		if (variable instanceof VariableByteArrayInstance) {
			Object tmpValue = variable.getValue();
			if (tmpValue instanceof Collection<?>) {
				List<Map<String, String>> financing = view.getFinancingOfTheTasks();
				if (!ListUtil.isEmpty(financing)) {
					Collection<?> info = (Collection<?>) tmpValue;
					int index = 0;
					for (Object infoItem: info) {
						Map<String, String> cells = financing.get(index);
						if (cells == null)
							continue;

						cells.put(key, infoItem.toString());
						index++;
					}
				}
			}

			return;
		}

		Serializable value = variable.getValue();
		if (value == null)
			return;

		List<Map<String, String>> financing = view.getFinancingOfTheTasks();
		if (financing == null) {
			financing = new ArrayList<Map<String,String>>();
			view.setFinancingOfTheTasks(financing);
		}

		String[] amounts = value.toString().split(CoreConstants.HASH);
		if (ArrayUtil.isEmpty(amounts))
			return;

		int index = 0;
		for (String amount: amounts) {
			Map<String, String> cells = index < financing.size() ? financing.get(index) : null;
			if (cells == null) {
				cells = new HashMap<String, String>();
				financing.add(index, cells);
			}

			cells.put(key, amount);
			index++;
		}
	}

	private List<Map<String, String>> getObjectValue(VariableByteArrayInstance variable) {
		Serializable value = variable.getValue();
		if (value == null)
			return Collections.emptyList();

		List<Map<String, String>> object = new ArrayList<Map<String,String>>();
		if (value instanceof Collection<?>) {
			Collection<?> jsonParts = (Collection<?>) value;
			for (Object jsonPart: jsonParts) {
				Map<String, String> genericValue = ObjectCollectionConverter.JSONToObj(jsonPart.toString());
				if (genericValue != null)
					object.add(genericValue);
			}
		}

		return object;
	}

	@Override
	public Long getNumberValue(String value) {
		return getNumberValue(value, false);
	}

	protected Long getNumberValue(String value, boolean dropThousands) {
		if (StringUtil.isEmpty(getStringValue(value)))
			return Long.valueOf(0);

		String originalValue = value;

		value = value.replaceAll(CoreConstants.SPACE, CoreConstants.EMPTY);
		value = value.replace(CoreConstants.DOT, CoreConstants.EMPTY);
		value = value.replace("þús", CoreConstants.EMPTY);
		value = value.replaceAll("kr", CoreConstants.EMPTY);
		value = StringHandler.replace(value, "d", CoreConstants.EMPTY);
		value = StringHandler.replace(value, CoreConstants.QOUTE_SINGLE_MARK, CoreConstants.EMPTY);

		if (StringUtil.isEmpty(value))
			return Long.valueOf(0);

		long total = 0;
		String amounts[] = value.split(CoreConstants.HASH);
		boolean logInfo = amounts.length > 2;
		for (String amount: amounts) {
			amount = StringHandler.replace(amount, CoreConstants.HASH, CoreConstants.EMPTY);

			Double numberValue = null;
			try {
				numberValue = Double.valueOf(amount);

				if (dropThousands)
					numberValue = Double.valueOf(numberValue.doubleValue() / 1000);

				total += numberValue.longValue();
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error getting number value from: " + value);
				return Long.valueOf(0);
			}
		}

		if (logInfo) {
			LOGGER.info("Computed total value " + total + " from '" + originalValue + "'");
		}

		return total;
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
		if (ListUtil.isEmpty(boardCases))
			return;

		List<String> sortingPreferences = null;
		Object o = iwc.getSessionAttribute(BOARD_CASES_LIST_SORTING_PREFERENCES);
		if (o instanceof List)
			sortingPreferences = (List<String>) o;

		Collections.sort(boardCases, new BoardCasesComparator(iwc.getLocale(), sortingPreferences));
	}

	protected Collection<GeneralCase> getCases(IWApplicationContext iwac, Collection<String> caseStatus, String processName) {
		Collection<Case> allCases = null;
		if (!StringUtil.isEmpty(processName)) {
			// Getting cases by application
			allCases = getCasesByProcessAndCaseStatus(iwac, caseStatus, processName);
		} else {
			// Getting cases by case status
			if (ListUtil.isEmpty(caseStatus)) {
				LOGGER.warning("Case status is unkown - terminating!");
				return null;
			}
			CasesBusiness casesBusiness = getCasesBusiness(iwac);
			if (casesBusiness == null) {
				LOGGER.warning(CasesBusiness.class + " is null!");
				return null;
			}
			try {
				allCases = casesBusiness.getCasesByCriteria(
						null, null, null, null,
						caseStatus.toArray(new String[caseStatus.size()]),
						null, null, null, null, false, false);
			} catch (RemoteException e) {
				LOGGER.log(Level.SEVERE, "Error getting cases by cases status: " + caseStatus, e);
			}
		}

		if (ListUtil.isEmpty(allCases))
			return null;

		Collection<GeneralCase> bpmCases = new ArrayList<GeneralCase>();
		for (Case theCase : allCases) {
			if (theCase instanceof GeneralCase) {
				bpmCases.add((GeneralCase) theCase);
			}
		}

		return bpmCases;
	}

	protected Collection<Case> getCasesByProcessAndCaseStatus(
			IWApplicationContext iwac, Collection<String> caseStatus,
			String processName) {
		CasesRetrievalManager caseManager = getCaseManager();
		if (caseManager == null) {
			LOGGER.severe(CasesRetrievalManager.class + " bean was not initialized!");
			return null;
		}

		Collection<Long> casesIdsByProcessDefinition = caseManager
				.getCasesIdsByProcessDefinitionName(processName);
		if (ListUtil.isEmpty(casesIdsByProcessDefinition))
			return null;

		List<Integer> ids = new ArrayList<Integer>(casesIdsByProcessDefinition.size());
		for (Long id : casesIdsByProcessDefinition) {
			ids.add(id.intValue());
		}

		Collection<Case> cases = getCasesBusiness(iwac).getCasesByIds(ids);
		if (ListUtil.isEmpty(cases))
			return null;

		if (ListUtil.isEmpty(caseStatus))
			return cases;

		Collection<Case> casesByProcessDefinitionAndStatus = new ArrayList<Case>();
		for (Case theCase : cases) {
			for (String status : caseStatus) {
				if (status.equals(theCase.getStatus())) {
					casesByProcessDefinitionAndStatus.add(theCase);
				}
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
				caseManager = ELUtil.getInstance().getBean(BPMCasesRetrievalManagerImpl.beanIdentifier);
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Error getting Spring bean for: " + CasesRetrievalManager.class, e);
			}
		}
		return caseManager;
	}

	protected List<String> getVariables(String uuid) {
		if (variables == null) {
			List<String> customColumns = getCustomColumns(uuid);
			if (ListUtil.isEmpty(customColumns)) {
				variables = new ArrayList<String>(CasesBoardViewer.CASE_FIELDS.size());
				for (AdvancedProperty variable : CasesBoardViewer.CASE_FIELDS) {
					variables.add(variable.getId());
				}
			} else
				variables = new ArrayList<String>(customColumns);
		}
		return variables;
	}

	protected List<String> getGradingVariables() {
		return GRADING_VARIABLES;
	}

	/**
	 *
	 * @param view
	 * @return returns String array, 0 element contains sum of positive grade values,
	 * 1 element contains sum of negative grade values
	 */
	public String [] getGradingSum(CaseBoardView view) {
		List<String> gradingValues = view.getValues(getGradingVariables());
		String[] gradings = new String[] {String.valueOf(0), String.valueOf(0)};
		if (ListUtil.isEmpty(gradingValues))
			return gradings;

		long sum = 0;
		long negativeSum = 0;
		Long gradeValue = null;
		for (String value: gradingValues) {
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
			if (value.indexOf("c") != -1) {
				value = value.substring(0, value.indexOf("c"));
			}

			gradeValue = null;
			try {
				gradeValue = Long.valueOf(value.trim());
			} catch (Exception e) {
				LOGGER.warning("Unable to convert '" + value + "' to number!");
			}

			if (gradeValue != null) {
				long longValue = gradeValue.longValue();
				sum += longValue;
				if (longValue < 0)
					negativeSum += longValue;
			}
		}

		gradings[0] = String.valueOf(sum);
		gradings[1] = String.valueOf(negativeSum);

		return gradings;
	}

	@Override
	public boolean isColumnOfDomain(String currentColumn, String columnOfDomain) {
		return !StringUtil.isEmpty(currentColumn) && !StringUtil.isEmpty(columnOfDomain) && currentColumn.equals(columnOfDomain);
	}

	@Override
	public CaseBoardTableBean getTableData(IWContext iwc, Collection<String> caseStatus,
			String processName, String uuid) {
		if (iwc == null)
			return null;

		IWBundle bundle = iwc.getIWMainApplication().getBundle(CasesConstants.IW_BUNDLE_IDENTIFIER);
		IWResourceBundle iwrb = bundle.getResourceBundle(iwc);
		CaseBoardTableBean data = new CaseBoardTableBean();

		List<CaseBoardBean> boardCases = getAllSortedCases(iwc, iwrb, caseStatus, processName, uuid);
		if (ListUtil.isEmpty(boardCases)) {
			data.setErrorMessage(iwrb.getLocalizedString("cases_board_viewer.no_cases_found", "There are no cases!"));
			return data;
		}

		// Header
		data.setHeaderLabels(getTableHeaders(iwrb, uuid));

		// Body
		Map<Integer, List<AdvancedProperty>> columns = getColumns(iwrb, uuid);

		long boardAmountTotal = 0;
		long grantAmountSuggestionTotal = 0;
		boolean financingTableAdded = false;
		String uniqueCaseId = "uniqueCaseId";
		List<CaseBoardTableBodyRowBean> bodyRows = new ArrayList<CaseBoardTableBodyRowBean>(boardCases.size());
		for (CaseBoardBean caseBoard: boardCases) {
			CaseBoardTableBodyRowBean rowBean = new CaseBoardTableBodyRowBean(caseBoard.getCaseId(), caseBoard.getProcessInstanceId());
			rowBean.setId(new StringBuilder(uniqueCaseId).append(caseBoard.getCaseId()).toString());
			rowBean.setCaseIdentifier(caseBoard.getCaseIdentifier());
			rowBean.setHandler(caseBoard.getHandler());

			//	Table of financing
			updateTasksInfo(caseBoard);

			int index = 0;
			Map<Integer, List<AdvancedProperty>> rowValues = new TreeMap<Integer, List<AdvancedProperty>>();
			for (Integer key: columns.keySet()) {
				List<AdvancedProperty> columnLabels = columns.get(key);

				for (AdvancedProperty column: columnLabels) {
					if (isColumnOfDomain(column.getId(), CasesBoardViewer.CASE_FIELDS.get(5).getId()))
						// Link to grading task
						rowValues.put(index, Arrays.asList(new AdvancedProperty(column.getId(), caseBoard.getCaseIdentifier())));
					else if (column.getId().equals(CaseHandlerAssignmentHandler.handlerUserIdVarName)) {
						//	Handler
						rowValues.put(index, Arrays.asList(new AdvancedProperty(CaseHandlerAssignmentHandler.handlerUserIdVarName,
								caseBoard.getHandler() == null ? String.valueOf(-1) : caseBoard.getHandler().getId())));

					//	Financing table
					} else if (isColumnOfDomain(column.getId(), CasesBoardViewer.WORK_ITEM)) {
						financingTableAdded = true;
						rowBean.setFinancingInfo(caseBoard.getFinancingOfTheTasks());
						rowValues.put(index, Arrays.asList(new AdvancedProperty(ProcessConstants.FINANCING_OF_THE_TASKS,
								CoreConstants.EMPTY)));
					} else if (isColumnOfDomain(column.getId(), CasesBoardViewer.ESTIMATED_COST)) {
					} else if (isColumnOfDomain(column.getId(), CasesBoardViewer.BOARD_SUGGESTION)) {
					} else if (isColumnOfDomain(column.getId(), CasesBoardViewer.BOARD_DECISION)) {

					//	Other value
					} else
						rowValues.put(index, Arrays.asList(new AdvancedProperty(column.getId(), caseBoard.getValue(column.getId()))));

					//	Calculations
					if (isColumnOfDomain(column.getId(), ProcessConstants.BOARD_FINANCING_DECISION))
						// Calculating board amounts
						boardAmountTotal += caseBoard.getBoardAmount();
					else if (isColumnOfDomain(column.getId(), ProcessConstants.BOARD_FINANCING_SUGGESTION))
						// Calculating grant amount suggestions
						grantAmountSuggestionTotal += caseBoard.getGrantAmountSuggestion();
				}

				index++;
			}

			rowBean.setValues(rowValues);
			bodyRows.add(rowBean);
		}
		data.setBodyBeans(bodyRows);

		// Footer
		data.setFooterValues(getFooterValues(iwrb, data.getBodyBeans().get(0).getValues().keySet().size() + (financingTableAdded ? 3 : 0),
				grantAmountSuggestionTotal, boardAmountTotal, uuid));

		// Everything is OK
		data.setFilledWithData(Boolean.TRUE);

		return data;
	}

	@Override
	public AdvancedProperty getHandlerInfo(IWContext iwc, User handler) {
		if (handler == null)
			return null;

		UserBusiness userBusiness = null;
		try {
			userBusiness = IBOLookup.getServiceInstance(iwc, UserBusiness.class);
		} catch(RemoteException e) {
			LOGGER.log(Level.WARNING, "Error getting " + UserBusiness.class, e);
		}
		if (userBusiness == null)
			return null;

		AdvancedProperty info = new AdvancedProperty(handler.getName());

		Email email = null;
		try {
			email = userBusiness.getUsersMainEmail(handler);
		} catch (RemoteException e) {
			LOGGER.log(Level.WARNING, "Error getting email for user: " + handler, e);
		} catch (NoEmailFoundException e) {}

		if (email != null)
			info.setValue(new StringBuilder("mailto:").append(email.getEmailAddress()).toString());

		return info;
	}

	protected static final String LOCALIZATION_PREFIX = "case_board_viewer.";

	@Override
	public List<String> getCustomColumns(String uuid) {
		if (StringUtil.isEmpty(uuid))
			return Collections.emptyList();

		IWContext iwc = CoreUtil.getIWContext();
		Object customColumns = iwc.getSessionAttribute(CasesBoardViewer.PARAMETER_CUSTOM_COLUMNS + uuid);
		if (customColumns instanceof List<?>) {
			@SuppressWarnings("unchecked")
			List<String> columns = (List<String>) customColumns;
			return columns;
		}
		return null;
	}

	@Override
	public Map<Integer, List<AdvancedProperty>> getColumns(IWResourceBundle iwrb, String uuid) {
		Map<Integer, List<AdvancedProperty>> columns = new TreeMap<Integer, List<AdvancedProperty>>();
		int index = 1;

		List<String> customColumns = getCustomColumns(uuid);
		if (ListUtil.isEmpty(customColumns)) {
			for (AdvancedProperty header: CasesBoardViewer.CASE_FIELDS) {
				if (index == 14) {
					columns.put(index, Arrays.asList(
							new AdvancedProperty(CasesBoardViewer.WORK_ITEM, iwrb.getLocalizedString(CasesBoardViewer.WORK_ITEM, "Work item")),
							new AdvancedProperty(CasesBoardViewer.ESTIMATED_COST, iwrb.getLocalizedString(CasesBoardViewer.ESTIMATED_COST,
									"Estimated cost")),
							new AdvancedProperty(CasesBoardViewer.BOARD_SUGGESTION, iwrb.getLocalizedString(CasesBoardViewer.BOARD_SUGGESTION,
									"Board suggestion")),
							new AdvancedProperty(CasesBoardViewer.BOARD_DECISION, iwrb.getLocalizedString(CasesBoardViewer.BOARD_DECISION,
									"Board decision"))
					));
				} else {
					columns.put(index, Arrays.asList(new AdvancedProperty(header.getId(),
						iwrb.getLocalizedString(new StringBuilder(LOCALIZATION_PREFIX).append(header.getId()).toString(), header.getValue()))));
				}
				index++;
			}
			columns.put(index, Arrays.asList(new AdvancedProperty(CaseHandlerAssignmentHandler.handlerUserIdVarName,
					iwrb.getLocalizedString(new StringBuilder(LOCALIZATION_PREFIX)
					.append(CaseHandlerAssignmentHandler.handlerUserIdVarName).toString(), "Case handler"))));
		} else {
			String localized = null;
			IWContext iwc = CoreUtil.getIWContext();
			IWResourceBundle bpmIWRB = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc);
			for (String column: customColumns) {
				if (CasesBoardViewCustomizer.FINANCING_TABLE_COLUMN.equals(column)) {
					columns.put(index, Arrays.asList(
							new AdvancedProperty(CasesBoardViewer.WORK_ITEM, iwrb.getLocalizedString(CasesBoardViewer.WORK_ITEM, "Work item")),
							new AdvancedProperty(CasesBoardViewer.ESTIMATED_COST, iwrb.getLocalizedString(CasesBoardViewer.ESTIMATED_COST,
									"Estimated cost")),
							new AdvancedProperty(CasesBoardViewer.BOARD_SUGGESTION, iwrb.getLocalizedString(CasesBoardViewer.BOARD_SUGGESTION,
									"Board suggestion")),
							new AdvancedProperty(CasesBoardViewer.BOARD_DECISION, iwrb.getLocalizedString(CasesBoardViewer.BOARD_DECISION,
									"Board decision"))
					));
				} else if (CasesBoardViewer.ESTIMATED_COST.equals(column) || CasesBoardViewer.BOARD_SUGGESTION.equals(column) ||
						CasesBoardViewer.BOARD_DECISION.equals(column))
					continue;
				else {
					localized = iwrb.getLocalizedString(LOCALIZATION_PREFIX.concat(column), column);
					if (column.equals(localized))
						localized = bpmIWRB.getLocalizedString(JBPMConstants.VARIABLE_LOCALIZATION_PREFIX.concat(column), column);
					if (column.equals(localized)) {
						LOGGER.warning("Variable " + column + " is not localized");
						continue;
					}

					columns.put(index, Arrays.asList(new AdvancedProperty(column, localized)));
				}

				index++;
			}
		}

		return columns;
	}

	protected Map<Integer, List<AdvancedProperty>> getTableHeaders(IWResourceBundle iwrb, String uuid) {
		return getColumns(iwrb, uuid);
	}

	protected List<String> getFooterValues(IWResourceBundle iwrb, int numberOfColumns, long grantAmountSuggestionTotal, long boardAmountTotal,
			String uuid) {

		List<String> values = new ArrayList<String>();

		int indexOfTotal = getIndexOfColumn(ProcessConstants.FINANCING_OF_THE_TASKS, uuid);
		if (indexOfTotal < 0)
			indexOfTotal = getIndexOfColumn(CasesBoardViewCustomizer.FINANCING_TABLE_COLUMN, uuid);
		indexOfTotal++;
		int indexOfSuggestion = indexOfTotal + 1;
		int indexOfDecision = indexOfSuggestion + 1;

		String total = iwrb.getLocalizedString("case_board_viewer.total_sum", "Total").concat(CoreConstants.COLON).toString();
		for (int i = 0; i < numberOfColumns; i++) {
			if (indexOfTotal > -1 && indexOfTotal == i) {
				// SUMs label
				values.add(total);
			} else if (i == indexOfSuggestion) {
				// Grant amount suggestions
				values.add(String.valueOf(grantAmountSuggestionTotal));
			} else if (i == indexOfDecision) {
				// Board amount
				values.add(String.valueOf(boardAmountTotal));
			} else
				values.add(CoreConstants.EMPTY);
		}
		if (values.size() <= indexOfTotal)
			values.add(total);
		if (values.size() <= indexOfSuggestion)
			values.add(String.valueOf(grantAmountSuggestionTotal));
		if (values.size() <= indexOfDecision)
			values.add(String.valueOf(boardAmountTotal));

		values.add(CoreConstants.EMPTY);

		return values;
	}

	@Override
	public int getIndexOfColumn(String column, String uuid) {
		List<String> columns = getVariables(uuid);
		return columns.indexOf(column);
	}

	CaseProcessInstanceRelationImpl getCaseProcessInstanceRelation() {
		return caseProcessInstanceRelation;
	}

	private String getStringValue(String value) {
		if (StringUtil.isEmpty(value) || "no_value".equals(value) || CoreConstants.MINUS.equals(value))
			return CoreConstants.EMPTY;

		return value;
	}

	protected class CaseBoardView {
		private String caseId;
		private Long processInstanceId;

		private User handler;

		private List<AdvancedProperty> variables = new ArrayList<AdvancedProperty>();

		private List<Map<String, String>> financingOfTheTasks;

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
				LOGGER.warning("Variable value or name (name=" + name + ", value=" +value+ ", case=" + caseId + ", piId=" + processInstanceId +
						") is undefined!");
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

		public List<Map<String, String>> getFinancingOfTheTasks() {
			return financingOfTheTasks;
		}

		public void setFinancingOfTheTasks(List<Map<String, String>> financingOfTheTasks) {
			this.financingOfTheTasks = financingOfTheTasks;
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
		if (taskViewer == null)
			ELUtil.getInstance().autowire(this);
		return taskViewer;
	}

	public void setTaskViewer(TaskViewerHelper taskViewer) {
		this.taskViewer = taskViewer;
	}

	public void setCaseManager(CasesRetrievalManager caseManager) {
		this.caseManager = caseManager;
	}

	public void setCaseProcessInstanceRelation(CaseProcessInstanceRelationImpl caseProcessInstanceRelation) {
		this.caseProcessInstanceRelation = caseProcessInstanceRelation;
	}

	@Override
	public String getLinkToTheTaskRedirector(IWContext iwc, String basePage, String caseId, Long processInstanceId, String backPage,
			String taskName) {
		return getTaskViewer().getLinkToTheTaskRedirector(iwc, basePage, caseId, processInstanceId, backPage, taskName);
	}

	public VariableInstanceQuerier getVariablesQuerier() {
		if (variablesQuerier == null)
			ELUtil.getInstance().autowire(this);
		return variablesQuerier;
	}

	public void setVariablesQuerier(VariableInstanceQuerier variablesQuerier) {
		this.variablesQuerier = variablesQuerier;
	}

	@Override
	public List<AdvancedProperty> getAvailableVariables(String processName) {
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null)
			return null;

		Collection<VariableInstanceInfo> variables = getVariablesQuerier().getVariablesByProcessDefinition(processName);
		BPMProcessVariablesBean variablesProvider = ELUtil.getInstance().getBean(BPMProcessVariablesBean.SPRING_BEAN_IDENTIFIER);
		return variablesProvider.getAvailableVariables(variables, iwc.getCurrentLocale(), iwc.isSuperAdmin(), false);
	}

	protected IWContext getIWContext() {
		return CoreUtil.getIWContext();
	}

	protected IWResourceBundle getIWResourceBundle(IWContext iwc) {
		if (iwc == null) {
			return null;
		}

		IWMainApplication application = IWMainApplication.getIWMainApplication(iwc);
		if (application == null) {
			return null;
		}

		IWBundle bundle = application.getBundle(
				getBundleIdentifier()
				);
		if (bundle == null) {
			return null;
		}

		return bundle.getResourceBundle(iwc);
	}

	protected String getBundleIdentifier() {
		return IWBundleStarter.IW_BUNDLE_IDENTIFIER;
	}

	protected void updateTasksInfo(CaseBoardBean caseBoard) {
		if (caseBoard == null) {
			return;
		}

		List<Map<String, String>> tasksInfo = caseBoard.getFinancingOfTheTasks();
		if (!ListUtil.isEmpty(tasksInfo)) {
			int tasksIndex = 0;
			Map<Integer, Map<String, String>> valuesToReplace = new TreeMap<Integer, Map<String,String>>();
			for (Map<String, String> taskInfo: tasksInfo) {
				if (MapUtil.isEmpty(taskInfo))
					continue;

				String taskName = taskInfo.get("task");
				if (StringUtil.isEmpty(taskName))
					continue;

				Long cost = getNumberValue(taskInfo.get("cost_estimate"), Boolean.FALSE);

				Map<String, String> cells = new HashMap<String, String>();
				cells.put(CasesBoardViewer.WORK_ITEM, taskName);
				cells.put(CasesBoardViewer.ESTIMATED_COST, String.valueOf(cost));

				String suggestion = taskInfo.get(CasesBoardViewer.BOARD_SUGGESTION);
				cells.put(CasesBoardViewer.BOARD_SUGGESTION, StringUtil.isEmpty(suggestion) ? CoreConstants.MINUS : suggestion);

				String decision = taskInfo.get(CasesBoardViewer.BOARD_DECISION);
				cells.put(CasesBoardViewer.BOARD_DECISION, StringUtil.isEmpty(decision) ? CoreConstants.MINUS : decision);

				valuesToReplace.put(tasksIndex, cells);
				tasksIndex++;
			}
			tasksInfo = new ArrayList<Map<String,String>>();
			for (Map<String, String> infoToReplace: valuesToReplace.values()) {
				tasksInfo.add(infoToReplace);
			}
			caseBoard.setFinancingOfTheTasks(tasksInfo);
		}
	}
}