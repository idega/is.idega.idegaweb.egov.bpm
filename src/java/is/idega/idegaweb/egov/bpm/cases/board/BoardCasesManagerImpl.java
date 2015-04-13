package is.idega.idegaweb.egov.bpm.cases.board;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.business.TaskViewerHelper;
import is.idega.idegaweb.egov.bpm.cases.CaseProcessInstanceRelationImpl;
import is.idega.idegaweb.egov.bpm.cases.actionhandlers.CaseHandlerAssignmentHandler;
import is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManager;
import is.idega.idegaweb.egov.bpm.cases.presentation.beans.BPMProcessVariablesBean;
import is.idega.idegaweb.egov.cases.business.BoardCasesComparator;
import is.idega.idegaweb.egov.cases.business.BoardCasesManager;
import is.idega.idegaweb.egov.cases.data.GeneralCase;
import is.idega.idegaweb.egov.cases.data.GeneralCaseBMPBean;
import is.idega.idegaweb.egov.cases.presentation.CasesBoardViewCustomizer;
import is.idega.idegaweb.egov.cases.presentation.CasesBoardViewer;
import is.idega.idegaweb.egov.cases.presentation.beans.CaseBoardBean;
import is.idega.idegaweb.egov.cases.presentation.beans.CaseBoardTableBean;
import is.idega.idegaweb.egov.cases.presentation.beans.CaseBoardTableBodyRowBean;
import is.idega.idegaweb.egov.cases.util.CasesConstants;

import java.io.Serializable;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.process.business.ProcessConstants;
import com.idega.block.process.data.Case;
import com.idega.block.process.data.CaseBMPBean;
import com.idega.block.process.data.CaseHome;
import com.idega.bpm.xformsview.converters.ObjectCollectionConverter;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.business.IBOLookup;
import com.idega.core.business.DefaultSpringBean;
import com.idega.core.contact.data.Email;
import com.idega.data.IDOLookup;
import com.idega.data.IDOLookupException;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.jbpm.bean.VariableByteArrayInstance;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMFactory;
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
import com.idega.util.WebUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.expression.ELUtil;

@Service(BoardCasesManager.BEAN_NAME)
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class BoardCasesManagerImpl extends DefaultSpringBean implements BoardCasesManager {

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
	            	"string_evaluationOfOtherGrantsGrade",		//	23
	            	"string_specialProjectValue",				//	24
	            	"string_estimatedSaleValue",				//	25
	            	"string_aspectOfTheDesignProjectGrade"		//	26
	 ));

	public static final String BOARD_CASES_LIST_SORTING_PREFERENCES = "boardCasesListSortingPreferencesAttribute";

	@Autowired
	private BPMCasesRetrievalManager caseManager;

	@Autowired
	private CaseProcessInstanceRelationImpl caseProcessInstanceRelation;

	@Autowired
	private VariableInstanceQuerier variablesQuerier;

	@Autowired
	private TaskViewerHelper taskViewer;

	private List<String> variables;

	private CaseHome caseHome = null;

	@Autowired
	private BPMDAO bpmDAO;

	protected BPMDAO getBPMDAO() {
		if (this.bpmDAO == null) {
			ELUtil.getInstance().autowire(this);
		}

		return this.bpmDAO;
	}

	protected CaseHome getCaseHome() {
		if (this.caseHome == null) {
			try {
				this.caseHome = (CaseHome) IDOLookup.getHome(Case.class);
			} catch (IDOLookupException e) {
				getLogger().log(Level.WARNING, "Failed to get home for " + Case.class, e);
			}
		}

		return this.caseHome;
	}

	@Override
	public List<CaseBoardBean> getAllSortedCases(
			Collection<String> caseStatuses,
			String processName,
			String uuid,
			boolean isSubscribedOnly,
			boolean backPage,
			String taskName,
			Date dateFrom,
			Date dateTo
	) {
		//	Getting cases by the configuration
		Collection<GeneralCase> cases = getCases(
				caseStatuses,
				processName,
				isSubscribedOnly,
				ProcessConstants.BPM_CASE,
				dateFrom,
				dateTo);
		if (ListUtil.isEmpty(cases)) {
			return null;
		}

		//	Filling beans with info
		List<CaseBoardBean> boardCases = getFilledBoardCaseWithInfo(
				cases,
				uuid,
				backPage,
				taskName
		);
		if (ListUtil.isEmpty(boardCases)) {
			return null;
		}

		sortBoardCases(boardCases);

		return boardCases;
	}

	protected List<CaseBoardBean> getFilledBoardCaseWithInfo(
			Collection<? extends GeneralCase> casesIdsAndHandlers,
			String uuid,
			boolean backPage,
			String taskName
	) {
		List<String> variablesToQuery = new ArrayList<String>(getVariables(uuid));
		if (variablesToQuery.contains(CasesBoardViewCustomizer.FINANCING_TABLE_COLUMN)) {
			variablesToQuery.remove(CasesBoardViewCustomizer.FINANCING_TABLE_COLUMN);
			variablesToQuery.add(ProcessConstants.FINANCING_OF_THE_TASKS);
		}
		variablesToQuery.add(ProcessConstants.BOARD_FINANCING_SUGGESTION);
		variablesToQuery.add(ProcessConstants.BOARD_FINANCING_DECISION);
		List<String> allVariables = new ArrayList<String>(variablesToQuery);
		allVariables.addAll(getGradingVariables());

		List<CaseBoardView> boardViews = getVariablesValuesByNamesForCases(casesIdsAndHandlers, allVariables, taskName);
		if (ListUtil.isEmpty(boardViews)) {
			return null;
		}

		List<String> numberVariables = Arrays.asList(
				CaseBoardBean.CASE_OWNER_TOTAL_COST,
				CasesConstants.APPLIED_GRANT_AMOUNT_VARIABLE,
				ProcessConstants.BOARD_FINANCING_SUGGESTION,
				ProcessConstants.BOARD_FINANCING_DECISION
		);

		/* Formatting links */
		Map<Long, ProcessInstance> relations = new HashMap<Long, ProcessInstance>();
		for (CaseBoardView view: boardViews) {
			relations.put(Long.valueOf(view.getCaseId()), view.getProcessInstance());
		}

		IWContext iwc = CoreUtil.getIWContext();
		Map<Long, String> links = getTaskViewer().getLinksToTheTaskRedirector(iwc, relations, backPage, taskName);

		Locale locale = iwc.getCurrentLocale();

		/* Filling board cases */
		List<CaseBoardBean> boardCases = new ArrayList<CaseBoardBean>();
		for (CaseBoardView view: boardViews) {
			CaseBoardBean boardCase = new CaseBoardBean(view.getCaseId(), view.getProcessInstanceId());
			boardCase.setApplicantName(view.getValue(CaseBoardBean.CASE_OWNER_FULL_NAME));
			boardCase.setCaseIdentifier(view.getValue(ProcessConstants.CASE_IDENTIFIER));
			boardCase.setCategory(view.getValue(CaseBoardBean.CASE_CATEGORY));
			boardCase.setHandler(view.getHandler());
			boardCase.setFinancingOfTheTasks(view.getFinancingOfTheTasks());

			boardCase.setLinkToCase(links.get(Long.valueOf(view.getCaseId())));

			long boardDecision = getNumberValue(view.getValue(ProcessConstants.BOARD_FINANCING_DECISION), false);
			boardCase.setBoardAmount(boardDecision);

			long boardSuggestion = getNumberValue(view.getValue(ProcessConstants.BOARD_FINANCING_SUGGESTION), false);
			boardCase.setGrantAmountSuggestion(boardSuggestion);

			Map<String, BigDecimal> gradingSums = getGradingSum(view, getGradingVariables());
			boardCase.addValues(gradingSums, locale);

			for (String variable: variablesToQuery) {
				String value = view.getValue(variable);
				if (numberVariables.contains(variable)) {
					if (variable.equals(ProcessConstants.BOARD_FINANCING_DECISION) || variable.equals(ProcessConstants.BOARD_FINANCING_SUGGESTION)) {
						value = String.valueOf(getNumberValue(value, false));
					} else {
						value = String.valueOf(getNumberValue(value, true));
					}
				}

				boardCase.addValue(variable, value);
			}

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

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	protected List<CaseBoardView> getVariablesValuesByNamesForCases(
			Collection<? extends GeneralCase> cases,
			List<String> variablesNames,
			String gradingTaskName
	) {
		gradingTaskName = StringUtil.isEmpty(gradingTaskName) ? "Grading" : gradingTaskName;

		/* Getting relations between cases and process instances */
		Map<ProcessInstance, Case> casesAndProcesses = getCaseProcessInstanceRelation().getCasesAndProcessInstances(cases);

		/* Getting variables */
		Collection<VariableInstanceInfo> variables = getVariablesQuerier().getVariables(
				variablesNames,
				casesAndProcesses.keySet(),
				Boolean.FALSE,
				Boolean.TRUE,
				Boolean.FALSE
		);
		if (ListUtil.isEmpty(variables)) {
			getLogger().warning("Didn't find any variables values for processes " + casesAndProcesses.values() + " and variables names " + variablesNames);
			return null;
		}

		List<CaseBoardView> views = new ArrayList<CaseBoardView>();
		for (VariableInstanceInfo variable: variables) {
			Serializable value = variable.getValue();
			if (variable.getName() != null && value != null && variable.getProcessInstanceId() != null) {
				Long processInstanceId = variable.getProcessInstanceId();
				CaseBoardView view = getCaseView(views, processInstanceId);
				if (view == null) {
					GeneralCase theCase = getCaseProcessInstanceRelation().getCase(casesAndProcesses, processInstanceId);
					if (theCase == null) {
						getLogger().warning("Case ID was not found in " + casesAndProcesses + " for process instance ID: " + processInstanceId);
						getLogger().warning("Couldn't get view bean for process: " + processInstanceId + ": " + casesAndProcesses);
						continue;
					}

					view = new CaseBoardView(theCase.getPrimaryKey().toString(), processInstanceId);
					view.setHandler(theCase.getHandledBy());
					view.setProcessInstance(getCaseProcessInstanceRelation().getProcessInstance(casesAndProcesses, processInstanceId));
					views.add(view);
				}

				List<String> gradingVariables = new ArrayList<String>();

				if (variable instanceof VariableByteArrayInstance) {
					if (ProcessConstants.FINANCING_OF_THE_TASKS.equals(variable.getName())) {
						List<Map<String, String>> obValue = getObjectValue((VariableByteArrayInstance) variable);

						List<Map<String, String>> financing = view.getFinancingOfTheTasks();
						if (financing == null) {
							view.setFinancingOfTheTasks(obValue);
						} else {
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
								if (StringUtil.isEmpty(tmp)) {
									taskInfoFromBoard.put(ProcessConstants.BOARD_FINANCING_SUGGESTION, CoreConstants.MINUS);
								}
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
				} else if (GRADING_VARIABLES.contains(variable.getName())) {
					gradingVariables.add(variable.getName());
				} else {
					view.addVariable(variable.getName(), value.toString());
				}

				if (!ListUtil.isEmpty(gradingVariables)) {
					Map<String, String> data = getVariablesLatestValues(processInstanceId, gradingTaskName, gradingVariables);
					if (!MapUtil.isEmpty(data)) {
						for (String varName: data.keySet()) {
							view.addVariable(varName, data.get(varName));
						}
					}
				}
			} else {
				getLogger().warning(variable + " can not be added to board view!");
			}
		}

		return views;
	}

	@Autowired(required = false)
	private BPMFactory bpmFactory;

	private BPMFactory getBPMFactory() {
		if (bpmFactory == null) {
			ELUtil.getInstance().autowire(this);
		}
		return bpmFactory;
	}

	private Map<String, String> getVariablesLatestValues(Long piId, String taskName, List<String> variablesNames) {
		if (piId == null || StringUtil.isEmpty(taskName) || ListUtil.isEmpty(variablesNames)) {
			return null;
		}

		if (IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("cases_board_latest_value_db", true)) {
			Map<Long, Map<String, VariableInstanceInfo>> data = getVariablesQuerier().getGroupedData(
					getVariablesQuerier().getVariablesByProcessInstanceIdAndVariablesNames(variablesNames, Arrays.asList(piId), false, true, false)
			);
			if (!MapUtil.isEmpty(data)) {
				Map<String, VariableInstanceInfo> vars = data.get(piId);
				if (!MapUtil.isEmpty(vars)) {
					Map<String, String> results = new HashMap<String, String>();
					for (String varName: variablesNames) {
						VariableInstanceInfo variable = vars.get(varName);
						if (variable != null) {
							String value = variable.getValue();
							results.put(varName, StringUtil.isEmpty(value) ? CoreConstants.EMPTY : value);
						}
					}
					return results;
				}
			}
		} else {
			try {
				return getBPMFactory().getProcessInstanceW(piId).getValuesForTaskInstance(StringUtil.isEmpty(taskName) ? "Grading" : taskName, variablesNames);
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Error getting latest values for " + variablesNames + ", proc. inst. ID: " + piId + ", task: " + taskName, e);
			}
		}

		return null;
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
				getLogger().log(Level.WARNING, "Error getting number value from: " + value);
				return Long.valueOf(0);
			}
		}

		if (logInfo) {
			getLogger().info("Computed total value " + total + " from '" + originalValue + "'");
		}

		return total;
	}

	@SuppressWarnings("unchecked")
	private void sortBoardCases(List<CaseBoardBean> boardCases) {
		if (ListUtil.isEmpty(boardCases))
			return;

		List<String> sortingPreferences = null;
		Object o = CoreUtil.getIWContext().getSessionAttribute(BOARD_CASES_LIST_SORTING_PREFERENCES);
		if (o instanceof List)
			sortingPreferences = (List<String>) o;

		Collections.sort(boardCases, new BoardCasesComparator(
				CoreUtil.getCurrentLocale(), sortingPreferences));
	}

	/**
	 *
	 * @param caseStatuses is {@link Collection} of {@link Case#getStatus()},
	 * skipped if <code>null</code>;
	 * @param processName is {@link ProcessDefinition#getName()},
	 * skipped if <code>null</code>
	 * @param subscribedOnly
	 * @param caseManagerType
	 * @param dateCreatedFrom is floor of {@link Case#getCreated()},
	 * skipped if <code>null</code>;
	 * @param dateCreatedTo is ceiling of {@link Case#getCreated()},
	 * skipped if <code>null</code>;
	 * @return entities by criteria or {@link Collections#emptyList()} on failure;
	 */
	protected Collection<GeneralCase> getCases(
			Collection<String> caseStatuses,
			String processName,
			boolean subscribedOnly,
			String caseManagerType,
			Date dateCreatedFrom,
			Date dateCreatedTo) {
		Collection<Case> allCases = getCaseManager().getCases(
				Arrays.asList(processName),
				null,
				caseStatuses,
				subscribedOnly ? Arrays.asList(getIWContext().getCurrentUser()): null,
				Arrays.asList(caseManagerType),
				dateCreatedFrom,
				dateCreatedTo);
		if (ListUtil.isEmpty(allCases)) {
			return null;
		}

		Collection<GeneralCase> bpmCases = new ArrayList<GeneralCase>();
		for (Case theCase: allCases) {
			if (!(theCase instanceof GeneralCase) || theCase.isClosed()) {
				continue;
			}

			bpmCases.add((GeneralCase) theCase);
		}

		return bpmCases;
	}

	/**
	 *
	 * <p>Checks if {@link GeneralCaseBMPBean} and {@link CaseBMPBean}
	 * has same primary keys, if so, then <code>true</code> is returned.</p>
	 * @param cases where to search for specified {@link Case} in,
	 * not <code>null</code>;
	 * @param theCase to search for, not <code>null</code>;
	 * @return <code>true</code> if {@link Case} found, <code>false</code>
	 * otherwise.
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	protected boolean contains(Collection<Case> cases, Case theCase) {
		if (ListUtil.isEmpty(cases) || theCase == null) {
			return Boolean.FALSE;
		}

		for (Case c : cases) {
			if (theCase.getPrimaryKey().equals(c.getPrimaryKey())) {
				return Boolean.TRUE;
			}
		}

		return Boolean.FALSE;
	}

	protected BPMCasesRetrievalManager getCaseManager() {
		if (caseManager == null) {
			ELUtil.getInstance().autowire(this);
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

	private String getCleanedValue(String value) {
		if (StringUtil.isEmpty(value)) {
			return value;
		}

		String[] symbolsToRemove = getApplication().getSettings().getProperty("cases_board_view_clean_symbols", "_,a,b,c,d").split(CoreConstants.COMMA);
		for (String symbol: symbolsToRemove) {
			if (StringUtil.isEmpty(symbol)) {
				continue;
			}

			if (value.contains(symbol)) {
				value = value.substring(0, value.indexOf(symbol));
			}
		}

		return value;
	}

	/**
	 *
	 * @return {@link Map} of variable name and sum of grading values;
	 */
	private Map<String, BigDecimal> getGradingSum(
			CaseBoardView view,
			List<String> variables
	) {
		Map<String, BigDecimal> gradings = new HashMap<String, BigDecimal>(2);

		List<String> gradingValues = view.getValues(variables);
		if (ListUtil.isEmpty(gradingValues)) {
			return gradings;
		}

		BigDecimal sum = new BigDecimal(0);
		BigDecimal negativeSum = new BigDecimal(0);

		for (String value: gradingValues) {
			if (StringUtil.isEmpty(getStringValue(value))) {
				continue;
			}

			value = getCleanedValue(value);
			if (StringUtil.isEmpty(value)) {
				continue;
			}

			Long gradeValue = null;
			try {
				gradeValue = Long.valueOf(value.trim());
			} catch (Exception e) {
				getLogger().warning("Unable to convert '" + value + "' to number!");
			}

			if (gradeValue != null) {
				sum = sum.add(BigDecimal.valueOf(gradeValue));
				if (gradeValue < 0) {
					negativeSum = negativeSum.add(BigDecimal.valueOf(gradeValue));
				}
			}
		}

		gradings.put(CaseBoardBean.CASE_SUM_ALL_GRADES, sum);
		gradings.put(CaseBoardBean.CASE_SUM_OF_NEGATIVE_GRADES, negativeSum);

		return gradings;
	}

	@Override
	public boolean isEqual(String currentColumn, String columnOfDomain) {
		return !StringUtil.isEmpty(currentColumn) &&
				!StringUtil.isEmpty(columnOfDomain) &&
				currentColumn.equals(columnOfDomain);
	}

	@Override
	public CaseBoardTableBean getTableData(
			Date dateFrom,
			Date dateTo,
			Collection<String> caseStatuses,
			String processName,
			String uuid,
			boolean isSubscribedOnly,
			boolean useBasePage,
			String taskName
	) {
		CaseBoardTableBean data = new CaseBoardTableBean();

		List<CaseBoardBean> boardCases = getAllSortedCases(
				caseStatuses,
				processName,
				uuid,
				isSubscribedOnly,
				useBasePage,
				taskName,
				dateFrom,
				dateTo
		);
		if (ListUtil.isEmpty(boardCases)) {
			data.setErrorMessage(localize("cases_board_viewer.no_cases_found", "There are no cases!"));
			return data;
		}

		getLogger().info("Got data: " + boardCases);	//	TODO

		// Header
		data.setHeaderLabels(getTableHeaders(uuid));

		// Body
		Map<Integer, List<AdvancedProperty>> columns = getColumns(uuid);

		BigDecimal boardAmountTotal = new BigDecimal(0);
		BigDecimal grantAmountSuggestionTotal = new BigDecimal(0);
		boolean financingTableAdded = false;
		String uniqueCaseId = "uniqueCaseId";
		List<CaseBoardTableBodyRowBean> bodyRows = new ArrayList<CaseBoardTableBodyRowBean>(boardCases.size());
		for (CaseBoardBean caseBoard: boardCases) {
			CaseBoardTableBodyRowBean rowBean = new CaseBoardTableBodyRowBean(
					caseBoard.getCaseId(),
					caseBoard.getProcessInstanceId()
			);
			rowBean.setId(new StringBuilder(uniqueCaseId).append(caseBoard.getCaseId()).toString());
			rowBean.setCaseIdentifier(caseBoard.getCaseIdentifier());
			rowBean.setHandler(caseBoard.getHandler());
			rowBean.setLinkToCase(caseBoard.getLinkToCase());

			//	Table of financing
			updateTasksInfo(caseBoard);

			int index = 0;
			Map<Integer, List<AdvancedProperty>> rowValues = new TreeMap<Integer, List<AdvancedProperty>>();
			for (Integer key: columns.keySet()) {
				List<AdvancedProperty> columnLabels = columns.get(key);

				for (AdvancedProperty column: columnLabels) {
					if (isEqual(column.getId(), ProcessConstants.CASE_IDENTIFIER)) {
						// Link to grading task
						rowValues.put(index, Arrays.asList(new AdvancedProperty(column.getId(), caseBoard.getCaseIdentifier())));

					} else if (column.getId().equals(CaseHandlerAssignmentHandler.handlerUserIdVarName)) {
						//	Handler
						rowValues.put(index,
								Arrays.asList(
										new AdvancedProperty(
												CaseHandlerAssignmentHandler.handlerUserIdVarName,
												caseBoard.getHandler() == null ? String.valueOf(-1) : caseBoard.getHandler().getId()
										)
								)
						);

					} else if (isEqual(column.getId(), CasesBoardViewer.WORK_ITEM)) {
						//	Financing table
						financingTableAdded = true;
						rowBean.setFinancingInfo(caseBoard.getFinancingOfTheTasks());
						rowValues.put(index, Arrays.asList(new AdvancedProperty(ProcessConstants.FINANCING_OF_THE_TASKS, CoreConstants.EMPTY)));
					} else if (isEqual(column.getId(), CasesBoardViewer.ESTIMATED_COST)) {
					} else if (isEqual(column.getId(), CasesBoardViewer.BOARD_SUGGESTION)) {
					} else if (isEqual(column.getId(), CasesBoardViewer.BOARD_DECISION)) {
					} else if (isEqual(column.getId(), CasesBoardViewer.BOARD_PROPOSAL_FOR_GRANT)) {

					} else if (isEqual(column.getId(), CaseBoardBean.CASE_OWNER_GENDER)) {
						//	Gender
						String value = caseBoard.getValue(CaseBoardBean.CASE_OWNER_GENDER);
						rowValues.put(index, Arrays.asList(new AdvancedProperty(CaseBoardBean.CASE_OWNER_GENDER, localize(value, value))));

					} else {
						//	Other value
						String columnKey = column.getId();
						String value = caseBoard.getValue(columnKey);
						if (StringUtil.isEmpty(value)) {
							if (CaseBoardBean.CASE_SUM_ALL_GRADES.equals(columnKey)) {
								value = caseBoard.getGradingSum();
							} else if (CaseBoardBean.CASE_SUM_OF_NEGATIVE_GRADES.equals(columnKey)) {
								value = caseBoard.getNegativeGradingSum();
							}
						}
						rowValues.put(index, Arrays.asList(new AdvancedProperty(columnKey, value)));
					}

					//	Calculations
					if (isEqual(column.getId(), ProcessConstants.BOARD_FINANCING_DECISION)) {
						// Calculating board amounts
						boardAmountTotal = boardAmountTotal.add(caseBoard.getBoardAmount());
					} else if (isEqual(column.getId(), ProcessConstants.BOARD_FINANCING_SUGGESTION)) {
						// Calculating grant amount suggestions
						grantAmountSuggestionTotal = grantAmountSuggestionTotal.add(caseBoard.getGrantAmountSuggestion());
					}
				}

				index++;
			}

			rowBean.setValues(rowValues);
			bodyRows.add(rowBean);
		}

		data.setBodyBeans(bodyRows);

		// Footer
		data.setFooterValues(getFooterValues(data.getBodyBeans().get(0).getValues().keySet().size() + (financingTableAdded ? 3 : 0), grantAmountSuggestionTotal, boardAmountTotal, uuid));

		// Everything is OK
		data.setFilledWithData(Boolean.TRUE);

		return data;
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.cases.business.BoardCasesManager#getTableData(com.idega.presentation.IWContext, java.util.Collection, java.lang.String, java.lang.String, boolean, boolean, java.lang.String)
	 */
	@Override
	public CaseBoardTableBean getTableData(
			Collection<String> caseStatuses,
			String processName,
			String uuid,
			boolean isSubscribedOnly,
			boolean backPage,
			String taskName) {
		return getTableData(null, null, caseStatuses, processName, uuid, isSubscribedOnly, backPage, taskName);
	}

	@Override
	public AdvancedProperty getHandlerInfo(IWContext iwc, User handler) {
		if (handler == null)
			return null;

		UserBusiness userBusiness = null;
		try {
			userBusiness = IBOLookup.getServiceInstance(iwc, UserBusiness.class);
		} catch(RemoteException e) {
			getLogger().log(Level.WARNING, "Error getting " + UserBusiness.class, e);
		}
		if (userBusiness == null)
			return null;

		AdvancedProperty info = new AdvancedProperty(handler.getName());

		Email email = null;
		try {
			email = userBusiness.getUsersMainEmail(handler);
		} catch (RemoteException e) {
			getLogger().log(Level.WARNING, "Error getting email for user: " + handler, e);
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

	private List<AdvancedProperty> getFinancingTableHeaders() {
		return Arrays.asList(
				new AdvancedProperty(CasesBoardViewer.WORK_ITEM, localize(CasesBoardViewer.WORK_ITEM, "Work item")),
				new AdvancedProperty(CasesBoardViewer.ESTIMATED_COST, localize(CasesBoardViewer.ESTIMATED_COST, "Estimated cost")),
				new AdvancedProperty(CasesBoardViewer.BOARD_PROPOSAL_FOR_GRANT, localize(CasesBoardViewer.BOARD_PROPOSAL_FOR_GRANT, "Proposed funding")),
				new AdvancedProperty(CasesBoardViewer.BOARD_SUGGESTION, localize(CasesBoardViewer.BOARD_SUGGESTION.toLowerCase(), "Handler suggestions")),
				new AdvancedProperty(CasesBoardViewer.BOARD_DECISION, localize(CasesBoardViewer.BOARD_DECISION.toLowerCase(), "Board decision"))
		);
	}

	@Override
	public Map<Integer, List<AdvancedProperty>> getColumns(String uuid) {
		Map<Integer, List<AdvancedProperty>> columns = new TreeMap<Integer, List<AdvancedProperty>>();
		int index = 1;

		List<String> customColumns = getCustomColumns(uuid);
		getLogger().info("Custom header columns: " + customColumns);	//	TODO
		if (ListUtil.isEmpty(customColumns)) {
			for (AdvancedProperty header: CasesBoardViewer.CASE_FIELDS) {
				if (ProcessConstants.FINANCING_OF_THE_TASKS.equals(header.getId())) {
					columns.put(index, getFinancingTableHeaders());
				} else {
					columns.put(index, Arrays.asList(
							new AdvancedProperty(
									header.getId(),
									localize(new StringBuilder(LOCALIZATION_PREFIX).append(header.getId()).toString(), header.getValue())
								)
							)
					);
				}
				index++;
			}
			columns.put(index, Arrays.asList(
					new AdvancedProperty(CaseHandlerAssignmentHandler.handlerUserIdVarName, localize(LOCALIZATION_PREFIX + CaseHandlerAssignmentHandler.handlerUserIdVarName, "Case handler"))
			));
		} else {
			String localized = null;
			IWContext iwc = CoreUtil.getIWContext();
			IWResourceBundle bpmIWRB = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc);
			for (String column: customColumns) {
				if (CasesBoardViewCustomizer.FINANCING_TABLE_COLUMN.equals(column)) {
					columns.put(index, getFinancingTableHeaders());
				} else if (
						CasesBoardViewer.ESTIMATED_COST.equals(column) ||
						CasesBoardViewer.BOARD_PROPOSAL_FOR_GRANT.equals(column) ||
						CasesBoardViewer.BOARD_SUGGESTION.equals(column) ||
						CasesBoardViewer.BOARD_DECISION.equals(column)
				) {
					continue;
				} else {
					localized = localize(LOCALIZATION_PREFIX.concat(column), column);
					if (column.equals(localized))
						localized = bpmIWRB.getLocalizedString(JBPMConstants.VARIABLE_LOCALIZATION_PREFIX.concat(column), column);
					if (column.equals(localized)) {
						getLogger().warning("Variable " + column + " is not localized");
						continue;
					}

					columns.put(index, Arrays.asList(new AdvancedProperty(column, localized)));
				}

				index++;
			}
		}

		getLogger().info("Header: " + columns + ", size: " + columns.size());	//	TODO
		return columns;
	}

	protected Map<Integer, List<AdvancedProperty>> getTableHeaders(String uuid) {
		return getColumns(uuid);
	}

	protected List<String> getFooterValues(
			int numberOfColumns,
			BigDecimal grantAmountSuggestionTotal,
			BigDecimal boardAmountTotal,
			String uuid
	) {
		List<String> values = new ArrayList<String>();

		int indexOfTotal = getIndexOfColumn(ProcessConstants.FINANCING_OF_THE_TASKS, uuid);
		if (indexOfTotal < 0) {
			indexOfTotal = getIndexOfColumn(CasesBoardViewCustomizer.FINANCING_TABLE_COLUMN, uuid);
		}
		int indexOfSuggestion = indexOfTotal + 3;
		int indexOfDecision = indexOfSuggestion + 1;

		String total = localize("case_board_viewer.total_sum", "Total").concat(CoreConstants.COLON).toString();
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
		if (values.size() <= indexOfTotal) {
			values.add(total);
		}
		if (values.size() <= indexOfSuggestion) {
			values.add(String.valueOf(grantAmountSuggestionTotal));
		}
		if (values.size() <= indexOfDecision) {
			values.add(String.valueOf(boardAmountTotal));
		}
		values.add(CoreConstants.EMPTY);

		return values;
	}

	@Override
	public int getIndexOfColumn(String column, String uuid) {
		List<String> columns = getVariables(uuid);
		return columns.indexOf(column);
	}

	protected CaseProcessInstanceRelationImpl getCaseProcessInstanceRelation() {
		return caseProcessInstanceRelation;
	}

	private String getStringValue(String value) {
		if (StringUtil.isEmpty(value) || "no_value".equals(value) || CoreConstants.MINUS.equals(value)) {
			return CoreConstants.EMPTY;
		}

		return value;
	}

	protected class CaseBoardView {
		private String caseId;
		private Long processInstanceId;
		private ProcessInstance processInstance;

		private User handler;

		private List<AdvancedProperty> variables = new ArrayList<AdvancedProperty>();

		private List<Map<String, String>> financingOfTheTasks;

		private CaseBoardView(String caseId, Long processInstanceId) {
			this.caseId = caseId;
			this.processInstanceId = processInstanceId;
		}

		public ProcessInstance getProcessInstance() {
			return processInstance;
		}

		public void setProcessInstance(ProcessInstance processInstance) {
			this.processInstance = processInstance;
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
				getLogger().warning("Variable value or name (name=" + name + ", value=" +value+ ", case=" + caseId + ", piId=" + processInstanceId +
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
				String value = getValue(variableName);
				values.add(value);
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
			return "CaseBoardView: case ID: " + caseId + ", process instance ID: " + processInstanceId + ", variables: " + getVariables() + ", financing of other tasks: " + getFinancingOfTheTasks();
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

	public void setCaseProcessInstanceRelation(CaseProcessInstanceRelationImpl caseProcessInstanceRelation) {
		this.caseProcessInstanceRelation = caseProcessInstanceRelation;
	}

	@Override
	public String getLinkToTheTaskRedirector(IWContext iwc, String basePage, String caseId, Long processInstanceId, String backPage, String taskName) {
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
		if (iwc == null) {
			return null;
		}

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

		IWBundle bundle = application.getBundle(getBundleIdentifier());
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
		if (ListUtil.isEmpty(tasksInfo)) {
			return;
		}

		int tasksIndex = 0;
		Map<Integer, Map<String, String>> valuesToReplace = new TreeMap<Integer, Map<String,String>>();
		for (Map<String, String> taskInfo: tasksInfo) {
			if (MapUtil.isEmpty(taskInfo)) {
				continue;
			}

			String taskName = taskInfo.get("task");
			if (StringUtil.isEmpty(taskName)) {
				taskName = CoreConstants.MINUS;
			}

			Long cost = getNumberValue(taskInfo.get("cost_estimate"), Boolean.FALSE);

			Map<String, String> cells = new HashMap<String, String>();
			cells.put(CasesBoardViewer.WORK_ITEM, taskName);
			cells.put(CasesBoardViewer.ESTIMATED_COST, String.valueOf(cost));

			String suggestion = taskInfo.get(CasesBoardViewer.BOARD_SUGGESTION);
			cells.put(CasesBoardViewer.BOARD_SUGGESTION, StringUtil.isEmpty(suggestion) ? CoreConstants.MINUS : suggestion);

			String decision = taskInfo.get(CasesBoardViewer.BOARD_DECISION);
			cells.put(CasesBoardViewer.BOARD_DECISION, StringUtil.isEmpty(decision) ? CoreConstants.MINUS : decision);

			String proposal = taskInfo.get(CasesBoardViewer.BOARD_PROPOSAL_FOR_GRANT);
			cells.put(CasesBoardViewer.BOARD_PROPOSAL_FOR_GRANT, StringUtil.isEmpty(proposal) ? CoreConstants.MINUS : proposal);

			valuesToReplace.put(tasksIndex, cells);
			tasksIndex++;
		}

		tasksInfo = new ArrayList<Map<String,String>>();
		for (Map<String, String> infoToReplace: valuesToReplace.values()) {
			tasksInfo.add(infoToReplace);
		}
		caseBoard.setFinancingOfTheTasks(tasksInfo);
	}

	protected String localize(String key, String value) {
		return getWebUtil().getLocalizedString(CasesConstants.IW_BUNDLE_IDENTIFIER, key, value);
	}

	@Autowired
	private WebUtil webUtil = null;

	protected WebUtil getWebUtil() {
		if (this.webUtil == null) {
			ELUtil.getInstance().autowire(this);
		}

		return this.webUtil;
	}
}