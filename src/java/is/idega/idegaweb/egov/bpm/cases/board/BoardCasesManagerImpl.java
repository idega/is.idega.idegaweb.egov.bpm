package is.idega.idegaweb.egov.bpm.cases.board;

import java.io.Serializable;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

import com.idega.block.process.business.CasesRetrievalManager;
import com.idega.block.process.business.ProcessConstants;
import com.idega.block.process.data.Case;
import com.idega.block.process.data.CaseBMPBean;
import com.idega.block.process.data.CaseHome;
import com.idega.bpm.model.VariableInstance;
import com.idega.bpm.xformsview.converters.ObjectCollectionConverter;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.business.IBOLookup;
import com.idega.core.business.DefaultSpringBean;
import com.idega.core.contact.data.Email;
import com.idega.data.IDOLookup;
import com.idega.data.IDOLookupException;
import com.idega.idegaweb.IWMainApplicationSettings;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.jbpm.bean.VariableByteArrayInstance;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.utils.JBPMConstants;
import com.idega.presentation.IWContext;
import com.idega.user.business.NoEmailFoundException;
import com.idega.user.business.UserBusiness;
import com.idega.user.dao.GroupDAO;
import com.idega.user.data.Group;
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

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.business.TaskViewerHelper;
import is.idega.idegaweb.egov.bpm.cases.CaseProcessInstanceRelationImpl;
import is.idega.idegaweb.egov.bpm.cases.actionhandlers.CaseHandlerAssignmentHandler;
import is.idega.idegaweb.egov.bpm.cases.manager.BPMCasesRetrievalManagerImpl;
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
	            	"string_aspectOfTheDesignProjectGrade",		//	26
	            	"string_usefulnessToSociety",				//	27
	            	"string_importanceOfProject"				//	28
	 ));

	public static final String BOARD_CASES_LIST_SORTING_PREFERENCES = "boardCasesListSortingPreferencesAttribute";

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

	@Autowired
	private GroupDAO groupDAO;

	protected BPMDAO getBPMDAO() {
		if (this.bpmDAO == null) {
			ELUtil.getInstance().autowire(this);
		}

		return this.bpmDAO;
	}

	protected GroupDAO getGroupDAO() {
		if (this.groupDAO == null) {
			ELUtil.getInstance().autowire(this);
		}

		return this.groupDAO;
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
	public <K extends Serializable> List<CaseBoardBean> getAllSortedCases(
			User currentUser,
			Collection<String> caseStatuses,
			String processName,
			String uuid,
			boolean isSubscribedOnly,
			boolean backPage,
			String taskName,
			Date dateFrom,
			Date dateTo,
			String casesType,
			Class<K> type
	) {
		//	Getting cases by the configuration
		Collection<GeneralCase> cases = getCases(
				currentUser,
				caseStatuses,
				processName,
				isSubscribedOnly,
				StringUtil.isEmpty(casesType) ? ProcessConstants.BPM_CASE : casesType,
				dateFrom,
				dateTo,
				casesType
		);
		if (ListUtil.isEmpty(cases)) {
			return null;
		}

		//	Filling beans with info
		List<CaseBoardBean> boardCases = getFilledBoardCaseWithInfo(
				cases,
				uuid,
				backPage,
				taskName,
				StringUtil.isEmpty(casesType) ? ProcessConstants.BPM_CASE : casesType,
				type
		);
		if (ListUtil.isEmpty(boardCases)) {
			return null;
		}

		List<String> sortingPreferences = null;
		Object o = CoreUtil.getIWContext().getSessionAttribute(BOARD_CASES_LIST_SORTING_PREFERENCES);
		if (o instanceof List) {
			@SuppressWarnings("unchecked")
			List<String> prefs = (List<String>) o;
			sortingPreferences = prefs;
		}
		if (ListUtil.isEmpty(sortingPreferences)) {
			IWContext iwc = CoreUtil.getIWContext();
			if (iwc != null && iwc.isParameterSet("sorting")) {
				sortingPreferences = Arrays.asList(iwc.getParameter("sorting").split(CoreConstants.HASH));
			}
		}

		sortBoardCases(boardCases, sortingPreferences);

		return boardCases;
	}

	/**
	 *
	 * @param <K> expected {@link Long} or {@link String}
	 * @param casesIdsAndHandlers
	 * @param uuid
	 * @param backPage
	 * @param taskName
	 * @param type
	 * @return
	 */
	protected <K extends Serializable> List<CaseBoardBean> getFilledBoardCaseWithInfo(
			Collection<? extends GeneralCase> casesIdsAndHandlers,
			String uuid,
			boolean backPage,
			String taskName,
			String casesType,
			Class<K> type
	) {
		List<String> variablesToQuery = new ArrayList<>(getVariables(uuid, casesType));
		if (variablesToQuery.contains(CasesBoardViewCustomizer.FINANCING_TABLE_COLUMN)) {
			variablesToQuery.remove(CasesBoardViewCustomizer.FINANCING_TABLE_COLUMN);
			if (StringUtil.isEmpty(casesType) || ProcessConstants.BPM_CASE.equals(casesType)) {
				variablesToQuery.add(ProcessConstants.FINANCING_OF_THE_TASKS);
			} else {
				variablesToQuery.add(ProcessConstants.FINANCING_OF_THE_TASKS_STRING);
				variablesToQuery.add(ProcessConstants.FINANCING_OF_THE_TASKS_ESTIMATED_EXPENSES);
				variablesToQuery.add(ProcessConstants.FINANCING_OF_THE_TASKS_YEARLY_EXPENSES);
			}
		}
		variablesToQuery.add(ProcessConstants.BOARD_FINANCING_SUGGESTION);
		variablesToQuery.add(ProcessConstants.BOARD_FINANCING_DECISION);
		List<String> allVariables = new ArrayList<>(variablesToQuery);
		allVariables.addAll(getGradingVariables());

		List<CaseBoardView> boardViews = getVariablesValuesByNamesForCases(casesIdsAndHandlers, allVariables, taskName, type);
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
		Map<Long, ProcessInstance> relations = new HashMap<>();
		for (CaseBoardView view: boardViews) {
			relations.put(Long.valueOf(view.getCaseId()), view.getProcessInstance());
		}

		IWContext iwc = CoreUtil.getIWContext();
		Map<Long, String> links = getTaskViewer().getLinksToTheTaskRedirector(iwc, relations, backPage, taskName);

		Locale locale = iwc.getCurrentLocale();

		/* Filling board cases */
		List<CaseBoardBean> boardCases = new ArrayList<>();
		for (CaseBoardView view: boardViews) {
			Serializable procInstId = view.getProcessInstanceId();
			CaseBoardBean boardCase = new CaseBoardBean(
					view.getCaseId(),
					procInstId instanceof Number ? ((Number) procInstId).longValue() : Integer.valueOf(procInstId.toString().hashCode()).longValue()
			);
			boardCase.setApplicantName(view.getValue(CaseBoardBean.CASE_OWNER_FULL_NAME));
			boardCase.setCaseIdentifier(view.getValue(ProcessConstants.CASE_IDENTIFIER));
			boardCase.setCategory(view.getValue(CaseBoardBean.CASE_CATEGORY));
			boardCase.setHandler(view.getHandler());
			boardCase.setFinancingOfTheTasks(view.getFinancingOfTheTasks());

			boardCase.setProjectNature(view.getValue(CaseBoardBean.PROJECT_NATURE));

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
						value = String.valueOf(getNumberValue(value, false));
					}
				}

				boardCase.addValue(variable, value);
			}

			boardCases.add(boardCase);
		}

		return boardCases;
	}

	private <K extends Serializable> CaseBoardView getCaseView(List<CaseBoardView> views, K processInstanceId) {
		if (ListUtil.isEmpty(views) || processInstanceId == null) {
			return null;
		}

		Long id = processInstanceId instanceof Number ? ((Number) processInstanceId).longValue() : Integer.valueOf(processInstanceId.toString().hashCode()).longValue();
		for (CaseBoardView view: views) {
			if (id == null) {
				if (processInstanceId.toString().equals(view.getProcessInstanceId().toString())) {
					return view;
				}
			} else {
				if (
						processInstanceId.toString().equals(view.getProcessInstanceId()) ||
						id.longValue() == Integer.valueOf(view.getProcessInstanceId().toString().hashCode()).longValue() ||
						(view.getProcessInstanceId() instanceof Number && id.longValue() == ((Long) view.getProcessInstanceId()).longValue())
				) {
					return view;
				}
			}
		}

		return null;
	}

	protected <T extends VariableInstance, K extends Serializable> Collection<T> getVariables(Collection<String> variablesNames, Set<K> keys) {
		if (ListUtil.isEmpty(keys)) {
			return null;
		}

		Set<Long> procInstIds = new HashSet<>();
		for (K key: keys) {
			if (key instanceof ProcessInstance) {
				procInstIds.add(((ProcessInstance) key).getId());
			} else if (key instanceof Number) {
				procInstIds.add(((Number) key).longValue());
			}
		}
		if (ListUtil.isEmpty(procInstIds)) {
			return null;
		}

		@SuppressWarnings("unchecked")
		List<T> variables = (List<T>) getVariablesQuerier().getVariables(
				variablesNames,
				procInstIds,
				Boolean.FALSE,
				Boolean.FALSE,
				Boolean.FALSE
		);
		return variables;
	}

	/**
	 *
	 * @param <K> expected {@link Long} or {@link String}
	 * @param cases
	 * @param variablesNames
	 * @param gradingTaskName
	 * @param type
	 * @return
	 */
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	protected <K extends Serializable> List<CaseBoardView> getVariablesValuesByNamesForCases(
			Collection<? extends GeneralCase> cases,
			List<String> variablesNames,
			String gradingTaskName,
			Class<K> type
	) {
		/* Getting relations between cases and process instances */
		Map<K, Case> processesIdsAndCases = getCaseProcessInstanceRelation().getCasesAndProcessInstancesIds(cases, type);
		if (MapUtil.isEmpty(processesIdsAndCases)) {
			return null;
		}

		/* Getting variables */
		Collection<VariableInstance> variables = getVariables(variablesNames, processesIdsAndCases.keySet());
		if (ListUtil.isEmpty(variables)) {
			getLogger().warning("Didn't find any variables values for processes " + processesIdsAndCases.keySet() + " and variables names " + variablesNames);
			return null;
		}

		List<CaseBoardView> views = new ArrayList<>();
		for (VariableInstance variable: variables) {
			Serializable value = variable.getVariableValue();
			if (variable.getName() != null && value != null && variable.getProcessInstanceId() != null) {
				K processInstanceId = variable.getProcessInstanceId();
				CaseBoardView view = getCaseView(views, processInstanceId);
				if (view == null) {
					GeneralCase theCase = getCaseProcessInstanceRelation().getCase(processesIdsAndCases, processInstanceId);
					if (theCase == null) {
						getLogger().warning("Case ID was not found in " + processesIdsAndCases + " for process instance ID: " + processInstanceId);
						getLogger().warning("Couldn't get view bean for process: " + processInstanceId + ": " + processesIdsAndCases);
						continue;
					}

					view = new CaseBoardView(theCase.getPrimaryKey().toString(), processInstanceId);

					//Set the handler
					User handlerUser = theCase.getHandledBy();
					if (handlerUser == null) {
						Collection<User> handlers = null;

						Group handlerGroup = theCase.getHandler();
						if (handlerGroup == null) {
							getLogger().warning("Unknown handler group for case " + theCase);
						} else {
							try {
								handlers = getUserBusiness().getUsersInGroup(handlerGroup);
								if (!ListUtil.isEmpty(handlers)) {
									handlerUser = handlers.iterator().next();
								}
							} catch (Exception ehg) {
								getLogger().log(Level.WARNING, "Could not find the handlers by handler group: " + handlerGroup, ehg);
							}
						}
					}
					view.setHandler(handlerUser);

					Serializable procInst = getCaseProcessInstanceRelation().getProcessInstance(processesIdsAndCases, processInstanceId);
					if (procInst instanceof ProcessInstance) {
						view.setProcessInstance((ProcessInstance) procInst);
					} else {
						view.setProcessInstanceId(procInst);
					}
					views.add(view);
				}

				List<String> gradingVariables = new ArrayList<>();

				if (ProcessConstants.FINANCING_OF_THE_TASKS_VARIABLES.contains(variable.getName()) || variable instanceof VariableByteArrayInstance) {
					List<Map<String, String>> obValue = null;
					if (variable instanceof VariableByteArrayInstance) {
						obValue = getObjectValue((VariableByteArrayInstance) variable);
					} else {
						obValue = getObjectValue(processInstanceId.toString(), variables);
					}

					if (obValue != null) {
						List<Map<String, String>> financing = view.getFinancingOfTheTasks();
						if (financing == null) {
							view.setFinancingOfTheTasks(obValue);
						} else {
							int taskIndex = 0;
							for (Map<String, String> taskInfo: obValue) {
								if (MapUtil.isEmpty(taskInfo)) {
									continue;
								}

								String taskName = taskInfo.get("task");
								if (StringUtil.isEmpty(taskName)) {
									continue;
								}

								String estimatedCost = taskInfo.get("cost_estimate");

								Map<String, String> taskInfoFromBoard = taskIndex < financing.size() ? financing.get(taskIndex) : null;
								if (taskInfoFromBoard == null) {
									taskInfoFromBoard = new HashMap<>();
									financing.add(taskInfoFromBoard);
								}

								taskInfoFromBoard.put(CasesBoardViewer.WORK_ITEM, taskName);
								taskInfoFromBoard.put(CasesBoardViewer.ESTIMATED_COST, estimatedCost);

								String tmp = taskInfoFromBoard.get(ProcessConstants.BOARD_FINANCING_SUGGESTION);
								if (StringUtil.isEmpty(tmp)) {
									taskInfoFromBoard.put(ProcessConstants.BOARD_FINANCING_SUGGESTION, CoreConstants.MINUS);
								}
								tmp = taskInfoFromBoard.get(ProcessConstants.BOARD_FINANCING_DECISION);
								if (StringUtil.isEmpty(tmp)) {
									taskInfoFromBoard.put(ProcessConstants.BOARD_FINANCING_DECISION, CoreConstants.MINUS);
								}

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
					gradingTaskName = StringUtil.isEmpty(gradingTaskName) ? "Grading" : gradingTaskName;
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

	protected <K extends Serializable> Map<String, String> getVariablesLatestValues(K piId, String taskName, List<String> variablesNames) {
		if (piId == null || StringUtil.isEmpty(taskName) || ListUtil.isEmpty(variablesNames)) {
			return null;
		}

		if (piId instanceof Number) {
			if (getSettings().getBoolean("cases_board_latest_value_db", true)) {
				Map<Long, Map<String, VariableInstance>> data = getVariablesQuerier().getGroupedData(
						getVariablesQuerier().getVariablesByProcessInstanceIdAndVariablesNames(variablesNames, Arrays.asList(((Number) piId).longValue()), false, true, false)
				);
				if (!MapUtil.isEmpty(data)) {
					Map<String, VariableInstance> vars = data.get(piId);
					if (!MapUtil.isEmpty(vars)) {
						Map<String, String> results = new HashMap<>();
						for (String varName: variablesNames) {
							VariableInstance variable = vars.get(varName);
							if (variable != null) {
								String value = variable.getVariableValue();
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

		} else if (piId instanceof String) {
			Set<String> keys = new HashSet<>();
			keys.add(piId.toString());
			Collection<VariableInstance> variables = getVariables(variablesNames, keys);
			if (ListUtil.isEmpty(variables)) {
				return null;
			}

			Map<String, String> results = new HashMap<>();
			for (VariableInstance variable: variables) {
				String varName = variable == null ? null : variable.getName();
				if (StringUtil.isEmpty(varName) || !variablesNames.contains(varName)) {
					continue;
				}
				String value = variable.getVariableValue();
				results.put(varName, StringUtil.isEmpty(value) ? CoreConstants.EMPTY : value);
			}
			return results;
		}

		return null;
	}

	private void fillWithBoardInfoOnTheTasks(VariableInstance variable, CaseBoardView view, String key) {
		if (variable instanceof VariableByteArrayInstance) {
			Object tmpValue = variable.getVariableValue();
			if (tmpValue instanceof Collection<?>) {
				List<Map<String, String>> financing = view.getFinancingOfTheTasks();
				if (!ListUtil.isEmpty(financing)) {
					Collection<?> info = (Collection<?>) tmpValue;
					int index = 0;
					for (Object infoItem: info) {
						Map<String, String> cells = financing.get(index);
						if (cells == null) {
							continue;
						}

						cells.put(key, infoItem.toString());
						index++;
					}
				}
			}

			return;
		}

		Serializable value = variable.getVariableValue();
		if (value == null) {
			return;
		}

		List<Map<String, String>> financing = view.getFinancingOfTheTasks();
		if (financing == null) {
			financing = new ArrayList<>();
			view.setFinancingOfTheTasks(financing);
		}

		String[] amounts = value.toString().split(CoreConstants.HASH);
		if (ArrayUtil.isEmpty(amounts)) {
			return;
		}

		int index = 0;
		for (String amount: amounts) {
			Map<String, String> cells = index < financing.size() ? financing.get(index) : null;
			if (cells == null) {
				cells = new HashMap<>();
				financing.add(index, cells);
			}

			cells.put(key, amount);
			index++;
		}
	}

	private List<Map<String, String>> getObjectValue(String processInstanceId, Collection<VariableInstance> variables) {
		if (StringUtil.isEmpty(processInstanceId) || ListUtil.isEmpty(variables)) {
			return null;
		}

		Map<String, String> values = new HashMap<>();
		List<Map<String, String>> object = new ArrayList<>();
		for (VariableInstance variable: variables) {
			String procInstId = variable.getProcessInstanceId();
			if (!processInstanceId.equals(procInstId)) {
				continue;
			}

			String name = variable.getName();
			if (StringUtil.isEmpty(name)) {
				continue;
			}

			String value = variable.getVariableValue();
			if (StringUtil.isEmpty(value)) {
				continue;
			}

			if (ProcessConstants.FINANCING_OF_THE_TASKS_ESTIMATED_EXPENSES.equals(name)) {
				values.put(name, value);
			} else if (ProcessConstants.FINANCING_OF_THE_TASKS_YEARLY_EXPENSES.equals(name)) {
				values.put(name, value);
			}
		}
		if (!MapUtil.isEmpty(values)) {
			object.add(values);
		}
		return object;
	}

	private List<Map<String, String>> getObjectValue(VariableByteArrayInstance variable) {
		Serializable value = variable.getValue();
		if (value == null) {
			return Collections.emptyList();
		}

		List<Map<String, String>> object = new ArrayList<>();
		if (value instanceof Collection<?>) {
			Collection<?> jsonParts = (Collection<?>) value;
			for (Object jsonPart: jsonParts) {
				Map<String, String> genericValue = ObjectCollectionConverter.JSONToObj(jsonPart.toString());
				if (genericValue != null) {
					object.add(genericValue);
				}
			}
		}

		return object;
	}

	@Override
	public Long getNumberValue(String value) {
		return getNumberValue(value, false);
	}

	protected Long getNumberValue(String value, boolean dropThousands) {
		if (StringUtil.isEmpty(getStringValue(value))) {
			return Long.valueOf(0);
		}

		String originalValue = value;

		value = value.replaceAll(CoreConstants.SPACE, CoreConstants.EMPTY);
		value = value.replace(CoreConstants.DOT, CoreConstants.EMPTY);
		value = value.replace("þús", CoreConstants.EMPTY);
		value = value.replaceAll("kr", CoreConstants.EMPTY);
		value = StringHandler.replace(value, "d", CoreConstants.EMPTY);
		value = StringHandler.replace(value, CoreConstants.QOUTE_SINGLE_MARK, CoreConstants.EMPTY);

		if (StringUtil.isEmpty(value)) {
			return Long.valueOf(0);
		}

		long total = 0;
		String amounts[] = value.split(CoreConstants.HASH);
		boolean logInfo = amounts.length > 2;
		for (String amount: amounts) {
			amount = StringHandler.replace(amount, CoreConstants.HASH, CoreConstants.EMPTY);

			Double numberValue = null;
			try {
				numberValue = Double.valueOf(amount);

				if (dropThousands) {
					numberValue = Double.valueOf(numberValue.doubleValue() / 1000);
				}

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

	private void sortBoardCases(List<CaseBoardBean> boardCases, List<String> sortingPreferences) {
		if (ListUtil.isEmpty(boardCases)) {
			return;
		}

		Collections.sort(boardCases, new BoardCasesComparator(CoreUtil.getCurrentLocale(), sortingPreferences));
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
			User currentUser,
			Collection<String> caseStatuses,
			String processName,
			boolean subscribedOnly,
			String caseManagerType,
			Date dateCreatedFrom,
			Date dateCreatedTo,
			String casesType
	) {
		Collection<Case> allCases = getCaseManager(casesType).getCases(
				StringUtil.isEmpty(processName) ? null : Arrays.asList(processName),
				null,
				caseStatuses,
				subscribedOnly && currentUser != null ? Arrays.asList(currentUser): null,
				StringUtil.isEmpty(caseManagerType) ? null : Arrays.asList(caseManagerType),
				dateCreatedFrom,
				dateCreatedTo
		);
		if (ListUtil.isEmpty(allCases)) {
			return null;
		}

		Collection<GeneralCase> bpmCases = new ArrayList<>();
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

	protected CasesRetrievalManager getCaseManager(String casesType) {
		return ELUtil.getInstance().getBean(BPMCasesRetrievalManagerImpl.beanIdentifier);
	}

	protected List<String> getVariables(String uuid, String casesType) {
		if (variables == null) {
			List<String> customColumns = getCustomColumns(uuid);
			if (ListUtil.isEmpty(customColumns)) {
				variables = new ArrayList<>();
				List<AdvancedProperty> variables = getStructure(casesType);
				for (AdvancedProperty variable : variables) {
					this.variables.add(variable.getId());
				}
			} else {
				variables = new ArrayList<>(customColumns);
			}
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

		String[] symbolsToRemove = getSettings().getProperty("cases_board_view_clean_symbols", "_,a,b,c,d").split(CoreConstants.COMMA);
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
		Map<String, BigDecimal> gradings = new HashMap<>(2);

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
		return !StringUtil.isEmpty(currentColumn) && !StringUtil.isEmpty(columnOfDomain) && currentColumn.equals(columnOfDomain);
	}

	@Override
	public <K extends Serializable> CaseBoardTableBean getTableData(
			User currentUser,
			Date dateFrom,
			Date dateTo,
			Collection<String> caseStatuses,
			String processName,
			String uuid,
			boolean isSubscribedOnly,
			boolean useBasePage,
			String taskName,
			String casesType,
			Class<K> type
	) {
		CaseBoardTableBean data = new CaseBoardTableBean();

		@SuppressWarnings("unchecked")
		Class<K> keyType = type == null ? (Class<K>) ProcessInstance.class : type;
		List<CaseBoardBean> boardCases = getAllSortedCases(
				currentUser,
				caseStatuses,
				processName,
				uuid,
				isSubscribedOnly,
				useBasePage,
				taskName,
				dateFrom,
				dateTo,
				casesType,
				keyType
		);
		if (ListUtil.isEmpty(boardCases)) {
			data.setErrorMessage(localize("cases_board_viewer.no_cases_found", "There are no cases!"));
			return data;
		}

		// Header
		data.setHeaderLabels(getTableHeaders(uuid, casesType));

		// Body
		Map<Integer, List<AdvancedProperty>> columns = getColumns(uuid, casesType);

		BigDecimal boardAmountTotal = new BigDecimal(0);
		BigDecimal grantAmountSuggestionTotal = new BigDecimal(0);
		boolean financingTableAdded = false;
		String uniqueCaseId = "uniqueCaseId";
		List<CaseBoardTableBodyRowBean> bodyRows = new ArrayList<>(boardCases.size());

		IWMainApplicationSettings settings = getSettings();
		boolean addBoardSuggestion = settings.getBoolean("cases_board_add_board_suggestion", false);
		boolean addBoardDecision = settings.getBoolean("cases_board_add_board_descision", false);
		boolean useAutomaticBoardDecisionAndSuggestion = settings.getBoolean("cases_board_automatic_board", false);
		int boardDecisionSubstractIndex = 1;
		Integer indexOfSugesstion = null, indexOfDecision = null;

		List<String> customColumns = getCustomColumns(uuid);
		boolean addToIndexOfSuggestion = false;
		if (ListUtil.isEmpty(customColumns)) {
			String boardDecisionSubstractIndexStr = settings.getProperty("board_decision_substract_index", "2");
			if (!StringUtil.isEmpty(boardDecisionSubstractIndexStr)) {
				boardDecisionSubstractIndex = Integer.valueOf(boardDecisionSubstractIndexStr).intValue();
			}
		} else {
			addToIndexOfSuggestion = true;
			boardDecisionSubstractIndex = 0;
		}

		Locale locale = getCurrentLocale();
		List<String> numbersWithDot = StringUtil.getValuesFromString(
				getApplicationProperty(
						"cases_board.numbers_with_dots",
						StringUtil.getValue(
								Arrays.asList(
										CaseBoardBean.CASE_OWNER_TOTAL_COST,
										ProcessConstants.CASE_APPLIED_AMOUNT,
										ProcessConstants.BOARD_FINANCING_SUGGESTION,
										ProcessConstants.BOARD_FINANCING_DECISION
								)
						)
				),
				CoreConstants.COMMA
		);
		for (CaseBoardBean caseBoard: boardCases) {
			Serializable procInstId = caseBoard.getProcessInstanceId();
			CaseBoardTableBodyRowBean rowBean = new CaseBoardTableBodyRowBean(
					caseBoard.getCaseId(),
					procInstId instanceof Number ? ((Number) procInstId).longValue() : Integer.valueOf(procInstId.toString().hashCode()).longValue()
			);
			rowBean.setId(new StringBuilder(uniqueCaseId).append(caseBoard.getCaseId()).toString());
			rowBean.setCaseIdentifier(caseBoard.getCaseIdentifier());
			rowBean.setHandler(caseBoard.getHandler());
			rowBean.setLinkToCase(caseBoard.getLinkToCase());

			//	Table of financing
			updateTasksInfo(caseBoard);

			int index = 0;
			List<AdvancedProperty> columnLabels = null;
			Map<Integer, List<AdvancedProperty>> rowValues = new TreeMap<>();
			for (Integer key: columns.keySet()) {
				columnLabels = columns.get(key);

				for (AdvancedProperty column: columnLabels) {
					String id = column.getId();

					if (isEqual(id, ProcessConstants.CASE_IDENTIFIER)) {
						// Link to grading task
						rowValues.put(index, Arrays.asList(new AdvancedProperty(id, caseBoard.getCaseIdentifier())));

					} else if (id.equals(CaseHandlerAssignmentHandler.handlerUserIdVarName)) {
						//	Handler
						rowValues.put(index,
								Arrays.asList(
										new AdvancedProperty(
												CaseHandlerAssignmentHandler.handlerUserIdVarName,
												caseBoard.getHandler() == null ? String.valueOf(-1) : caseBoard.getHandler().getId()
										)
								)
						);

						//Get the group name for the project nature
						} else if (isEqual(id, CasesBoardViewer.VARIABLE_PROJECT_NATURE)) {
							String projectNatureGroupName = CoreConstants.EMPTY;
							String projectNatureGroupNameValue = caseBoard.getValue(column.getId());
							if (StringHandler.isNumeric(projectNatureGroupNameValue)) {
								try {
									com.idega.user.data.bean.Group projectNatureGroup = getGroupDAO().findGroup(Integer.valueOf(projectNatureGroupNameValue));
									if (projectNatureGroup != null) {
										projectNatureGroupName = projectNatureGroup.getName();
									}
								} catch (Exception ePNG) {
									getLogger().log(Level.WARNING, "Could not get the group for the project nature with group id: " + projectNatureGroupNameValue, ePNG);
								}
							}

							rowValues.put(
									index,
									Arrays.asList(new AdvancedProperty(
											column.getId(),
											projectNatureGroupName
										)
									)
							);

					} else if (isEqual(id, CasesBoardViewer.WORK_ITEM)) {
						//	Financing table
						financingTableAdded = true;
						rowBean.setFinancingInfo(caseBoard.getFinancingOfTheTasks());
						rowValues.put(index, Arrays.asList(new AdvancedProperty(
								StringUtil.isEmpty(casesType) || ProcessConstants.BPM_CASE.equals(casesType) ?
										ProcessConstants.FINANCING_OF_THE_TASKS : ProcessConstants.FINANCING_OF_THE_TASKS_STRING,
								CoreConstants.EMPTY
						)));

					} else if (isEqual(id, CasesBoardViewer.ESTIMATED_COST)) {
						//	Will be added with financing table (work item)

					} else if (isEqual(id, CasesBoardViewer.BOARD_SUGGESTION) && addBoardSuggestion && !useAutomaticBoardDecisionAndSuggestion) {
						if (addToIndexOfSuggestion) {
							indexOfSugesstion = index;
						} else {
							indexOfSugesstion = index - 1;
						}
						String boardSuggestionValue = caseBoard.getValue(CasesBoardViewer.BOARD_SUGGESTION);
						if (!StringUtil.isEmpty(boardSuggestionValue) && numbersWithDot.contains(CasesBoardViewer.BOARD_SUGGESTION)) {
							boardSuggestionValue = getNumberWithDots(boardSuggestionValue, locale, CasesBoardViewer.BOARD_SUGGESTION);
						}
						rowValues.put(
								index,
								Arrays.asList(
										new AdvancedProperty(
												CasesBoardViewer.BOARD_SUGGESTION,
												boardSuggestionValue
										)
								)
						);

					} else if (isEqual(id, CasesBoardViewer.BOARD_DECISION) && addBoardDecision && !useAutomaticBoardDecisionAndSuggestion) {
						indexOfDecision = index - boardDecisionSubstractIndex;
						String boardDecisionValue = caseBoard.getValue(CasesBoardViewer.BOARD_DECISION);
						if (!StringUtil.isEmpty(boardDecisionValue) && numbersWithDot.contains(CasesBoardViewer.BOARD_DECISION)) {
							boardDecisionValue = getNumberWithDots(boardDecisionValue, locale, CasesBoardViewer.BOARD_DECISION);
						}
						rowValues.put(
								index,
								Arrays.asList(
										new AdvancedProperty(
												CasesBoardViewer.BOARD_DECISION,
												boardDecisionValue
										)
								)
						);

					} else if (isEqual(id, CasesBoardViewer.BOARD_PROPOSAL_FOR_GRANT)) {
						//	Will be added with financing table (work item)

					} else if (isEqual(id, CaseBoardBean.CASE_OWNER_GENDER)) {
						//	Gender
						String value = caseBoard.getValue(CaseBoardBean.CASE_OWNER_GENDER);
						if (StringUtil.isEmpty(value)) {
							value = caseBoard.getValue("string_responsiblePersonGender");
						}
						rowValues.put(index, Arrays.asList(new AdvancedProperty(CaseBoardBean.CASE_OWNER_GENDER, localize(value, value))));

					} else {
						//	Other value
						String columnKey = id;
						String value = caseBoard.getValue(columnKey);
						if (StringUtil.isEmpty(value)) {
							if (CaseBoardBean.CASE_SUM_ALL_GRADES.equals(columnKey)) {
								value = caseBoard.getGradingSum();
							} else if (CaseBoardBean.CASE_SUM_OF_NEGATIVE_GRADES.equals(columnKey)) {
								value = caseBoard.getNegativeGradingSum();
							}
						}

						//Fix the amount - add dots for string_appliedAmount
						if (!StringUtil.isEmpty(value) && numbersWithDot.contains(id)) {
							value = getNumberWithDots(value, locale, id);
						}

						rowValues.put(index, Arrays.asList(new AdvancedProperty(columnKey, value)));
					}

					//	Calculations
					if (isEqual(id, ProcessConstants.BOARD_FINANCING_DECISION)) {
						// Calculating board amounts
						boardAmountTotal = boardAmountTotal.add(caseBoard.getBoardAmount());
					} else if (isEqual(id, ProcessConstants.BOARD_FINANCING_SUGGESTION)) {
						// Calculating grant amount suggestions
						grantAmountSuggestionTotal = grantAmountSuggestionTotal.add(caseBoard.getGrantAmountSuggestion());
					}

					index++;
				}
			}

			rowBean.setValues(rowValues);
			bodyRows.add(rowBean);
		}

		data.setBodyBeans(bodyRows);

		// Footer
		data.setFooterValues(
				getFooterValues(
						data.getBodyBeans().get(0).getValues().keySet().size() + (financingTableAdded ? 3 : 0),
						null,
						indexOfSugesstion,
						indexOfDecision,
						grantAmountSuggestionTotal,
						boardAmountTotal,
						uuid,
						casesType
				)
		);

		// Everything is OK
		data.setFilledWithData(Boolean.TRUE);

		return data;
	}

	private String getNumberWithDots(String value, Locale locale, String id) {
		if (StringUtil.isEmpty(value)) {
			return value;
		}

		NumberFormat formatter = NumberFormat.getInstance(locale);
		String convertedAmount = value;
		String strippedAmount = null;
		try {
			strippedAmount = value.trim().replace(CoreConstants.DOT, CoreConstants.EMPTY).replace(CoreConstants.COMMA, CoreConstants.EMPTY);
			if (StringHandler.isNumeric(strippedAmount)) {
				convertedAmount = formatter.format(Long.valueOf(strippedAmount));
			}
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error formatting '" + strippedAmount + "' for " + id, e);
		}

		//Putting dots
		if (!StringUtil.isEmpty(convertedAmount) && !convertedAmount.contains(CoreConstants.DOT)) {
			try {
				try {
					convertedAmount = String.format("%,d", Integer.valueOf(convertedAmount)).replace(CoreConstants.COMMA, CoreConstants.DOT);
				} catch (Exception eDotsIn) {
					convertedAmount = NumberFormat.getInstance(locale).format(Double.parseDouble(convertedAmount)).replace(CoreConstants.COMMA, CoreConstants.DOT);
				}
			} catch (Exception eDots) {
				getLogger().log(Level.WARNING, "Could not add the dot separators in the amount '" + value + "' for " + id, eDots);
			}
		}

		return convertedAmount;
	}

	/*
	 * (non-Javadoc)
	 * @see is.idega.idegaweb.egov.cases.business.BoardCasesManager#getTableData(com.idega.presentation.IWContext, java.util.Collection, java.lang.String, java.lang.String, boolean, boolean, java.lang.String)
	 */
	@Override
	public CaseBoardTableBean getTableData(
			User currentUser,
			Collection<String> caseStatuses,
			String processName,
			String uuid,
			boolean isSubscribedOnly,
			boolean backPage,
			String taskName,
			String casesType
	) {
		return getTableData(
				currentUser,
				null,
				null,
				caseStatuses,
				processName,
				uuid,
				isSubscribedOnly,
				backPage,
				taskName,
				StringUtil.isEmpty(casesType) ? ProcessConstants.BPM_CASE : casesType,
				Long.class
		);
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
			getLogger().log(Level.WARNING, "Error getting " + UserBusiness.class, e);
		}
		if (userBusiness == null) {
			return null;
		}

		AdvancedProperty info = new AdvancedProperty(handler.getName());

		Email email = null;
		try {
			email = userBusiness.getUsersMainEmail(handler);
		} catch (RemoteException e) {
			getLogger().log(Level.WARNING, "Error getting email for user: " + handler, e);
		} catch (NoEmailFoundException e) {}

		if (email != null) {
			info.setValue(new StringBuilder("mailto:").append(email.getEmailAddress()).toString());
		}

		return info;
	}

	protected static final String LOCALIZATION_PREFIX = "case_board_viewer.";

	@Override
	public List<String> getCustomColumns(String uuid) {
		if (StringUtil.isEmpty(uuid)) {
			return Collections.emptyList();
		}

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
				new AdvancedProperty(CasesBoardViewer.BOARD_SUGGESTION, localize(CasesBoardViewer.BOARD_SUGGESTION.toLowerCase(), "Handler suggestions")),
				new AdvancedProperty(CasesBoardViewer.BOARD_PROPOSAL_FOR_GRANT, localize(CasesBoardViewer.BOARD_PROPOSAL_FOR_GRANT, "Proposed funding")),
				new AdvancedProperty(CasesBoardViewer.BOARD_DECISION, localize(CasesBoardViewer.BOARD_DECISION.toLowerCase(), "Board decision"))
		);
	}

	protected List<AdvancedProperty> getStructure(String casesType) {
		return Arrays.asList(
				new AdvancedProperty(CaseBoardBean.CASE_OWNER_FULL_NAME, "Applicant"),						//	0
				new AdvancedProperty(CaseBoardBean.CASE_OWNER_GENDER, "Gender"),							//	1
				new AdvancedProperty("string_ownerKennitala", "Personal ID"),								//	2
				new AdvancedProperty("string_ownerAddress", "Address"),										//	3
				new AdvancedProperty("string_ownerPostCode", "Zip"),										//	4
				new AdvancedProperty("string_ownerMunicipality", "Municipality"),							//	5
				new AdvancedProperty(ProcessConstants.CASE_IDENTIFIER, "Case nr."),							//	6
				new AdvancedProperty(ProcessConstants.CASE_DESCRIPTION, "Description"),						//	7

				new AdvancedProperty(CaseBoardBean.CASE_OWNER_BUSINESS_CONCEPT, "In a nutshell"),			//	8

				new AdvancedProperty(CaseBoardBean.CASE_SUM_OF_NEGATIVE_GRADES, "Negative grade"),			//	11
				new AdvancedProperty(CaseBoardBean.CASE_SUM_ALL_GRADES, "Grade"),							//	12

				new AdvancedProperty(CaseBoardBean.CASE_CATEGORY, "Category"),								//	13,	EDITABLE, select

				new AdvancedProperty(CaseBoardBean.CASE_OWNER_GRADE, "Comment"),							//	14
				new AdvancedProperty(CaseBoardBean.CASE_OWNER_TOTAL_COST, "Total cost"),					//	15
				new AdvancedProperty(ProcessConstants.CASE_APPLIED_AMOUNT, "Applied amount"),				//	16

				new AdvancedProperty(																		//	17, table of 5 columns
						casesType == null || ProcessConstants.BPM_CASE.equals(casesType) ?
								ProcessConstants.FINANCING_OF_THE_TASKS :
								ProcessConstants.FINANCING_OF_THE_TASKS_STRING,
						"Financing of the tasks"
				),

				new AdvancedProperty(CaseBoardBean.CASE_OWNER_ANSWER, "Restrictions"),						//	20, EDITABLE, text area
				new AdvancedProperty(CaseBoardBean.PROJECT_NATURE, "Project nature")						//  21
																											//	22 is handler by default (if no custom columns provided)
		);
	}

	@Override
	public Map<Integer, List<AdvancedProperty>> getColumns(String uuid, String casesType) {
		Map<Integer, List<AdvancedProperty>> columns = new TreeMap<>();
		int index = 1;

		List<String> customColumns = getCustomColumns(uuid);
		if (ListUtil.isEmpty(customColumns)) {
			List<AdvancedProperty> variables = getStructure(casesType);
			for (AdvancedProperty header: variables) {
				if (ProcessConstants.FINANCING_OF_THE_TASKS_VARIABLES.contains(header.getId())) {
					List<AdvancedProperty> financingTableHeaders = getFinancingTableHeaders();
					columns.put(index, financingTableHeaders);
					index += financingTableHeaders.size();

				} else {
					columns.put(index, Arrays.asList(
							new AdvancedProperty(
									header.getId(),
									localize(new StringBuilder(LOCALIZATION_PREFIX).append(header.getId()).toString(), header.getValue())
								)
							)
					);
					index++;
				}
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
					if (column.equals(localized)) {
						localized = bpmIWRB.getLocalizedString(JBPMConstants.VARIABLE_LOCALIZATION_PREFIX.concat(column), column);
					}
					if (column.equals(localized)) {
						getLogger().warning("Variable " + column + " is not localized");
						continue;
					}

					columns.put(index, Arrays.asList(new AdvancedProperty(column, localized)));
				}

				index++;
			}
		}

		return columns;
	}

	protected Map<Integer, List<AdvancedProperty>> getTableHeaders(String uuid, String casesType) {
		return getColumns(uuid, casesType);
	}

	protected List<String> getFooterValues(
			int numberOfColumns,
			Integer indexOfTotal,
			Integer indexOfSuggestion,
			Integer indexOfDecision,
			BigDecimal grantAmountSuggestionTotal,
			BigDecimal boardAmountTotal,
			String uuid,
			String casesType
	) {
		List<String> values = new ArrayList<>();

		indexOfTotal = indexOfTotal == null ?
				getIndexOfColumn(
						StringUtil.isEmpty(casesType) || ProcessConstants.BPM_CASE.equals(casesType) ?
								ProcessConstants.FINANCING_OF_THE_TASKS : ProcessConstants.FINANCING_OF_THE_TASKS_STRING,
						uuid,
						casesType
				) : indexOfTotal;
		if (indexOfTotal < 0) {
			indexOfTotal = getIndexOfColumn(CasesBoardViewCustomizer.FINANCING_TABLE_COLUMN, uuid, casesType);
		}
		indexOfSuggestion = indexOfSuggestion == null ? (indexOfTotal + 1) : indexOfSuggestion; //FIXME: Was (indexOfTotal + 3), which is not correctly set. Something is worng here.
		indexOfDecision = indexOfDecision == null ? (indexOfSuggestion + 1) : indexOfDecision;
		if (indexOfTotal < 0) {
			indexOfTotal = indexOfSuggestion - 1;
		}

		//If custom columns exist, totals index should be calculated in another way
		List<String> customColumns = getCustomColumns(uuid);
		if (!ListUtil.isEmpty(customColumns)) {
			indexOfTotal = 0;
			if (indexOfSuggestion != null) {
				indexOfTotal = indexOfSuggestion - 1;
			}
			if (indexOfDecision != null) {
				if (
						indexOfTotal >= indexOfDecision
						||
						(indexOfTotal == 0  && (indexOfSuggestion == null || indexOfSuggestion > indexOfDecision))
				) {
					indexOfTotal = indexOfDecision - 1;
				}
			}
			if (indexOfTotal < 0) {
				indexOfTotal = 0;
			}
			if (
					(indexOfSuggestion != null && indexOfTotal == indexOfSuggestion)
					||
					(indexOfDecision != null && indexOfTotal == indexOfDecision)
			) {
				indexOfTotal = -1;
			}
		}

		Locale locale = getCurrentLocale();
		String total = localize("case_board_viewer.total_sum", "Total").concat(CoreConstants.COLON).toString();
		for (int i = 0; i < numberOfColumns; i++) {
			if (indexOfTotal > -1 && indexOfTotal == i) {
				// SUMs label
				values.add(total);
			} else if (i == indexOfSuggestion) {
				// Grant amount suggestions
				values.add(getNumberWithDots(String.valueOf(grantAmountSuggestionTotal), locale, "grantAmountSuggestionTotal"));
			} else if (i == indexOfDecision) {
				// Board amount
				values.add(getNumberWithDots(String.valueOf(boardAmountTotal), locale, "boardAmountTotal"));
			} else {
				values.add(CoreConstants.EMPTY);
			}
		}
		if (values.size() <= indexOfTotal) {
			values.add(total);
		}
		if (values.size() <= indexOfSuggestion) {
			values.add(getNumberWithDots(String.valueOf(grantAmountSuggestionTotal), locale, "grantAmountSuggestionTotal"));
		}
		if (values.size() <= indexOfDecision) {
			values.add(getNumberWithDots(String.valueOf(boardAmountTotal), locale, "boardAmountTotal"));
		}
		values.add(CoreConstants.EMPTY);

		return values;
	}

	@Override
	public int getIndexOfColumn(String column, String uuid, String casesType) {
		List<String> columns = getVariables(uuid, casesType);
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
		private Serializable processInstanceId;
		private ProcessInstance processInstance;

		private User handler;

		private List<AdvancedProperty> variables = new ArrayList<>();

		private List<Map<String, String>> financingOfTheTasks;

		private CaseBoardView(String caseId, Serializable processInstanceId) {
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

		public <T extends Serializable> T getProcessInstanceId() {
			@SuppressWarnings("unchecked")
			T result = (T) processInstanceId;
			return result;
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

			List<String> values = new ArrayList<>();
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

		public void setProcessInstanceId(Serializable processInstanceId) {
			this.processInstanceId = processInstanceId;
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
		if (taskViewer == null) {
			ELUtil.getInstance().autowire(this);
		}
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
		if (variablesQuerier == null) {
			ELUtil.getInstance().autowire(this);
		}
		return variablesQuerier;
	}

	public void setVariablesQuerier(VariableInstanceQuerier variablesQuerier) {
		this.variablesQuerier = variablesQuerier;
	}

	@Override
	public List<AdvancedProperty> getAvailableVariables(String processName, String casesType) {
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}

		Collection<VariableInstance> variables = getVariablesQuerier().getVariablesByProcessDefinition(processName);
		BPMProcessVariablesBean variablesProvider = ELUtil.getInstance().getBean(BPMProcessVariablesBean.SPRING_BEAN_IDENTIFIER);
		return variablesProvider.getAvailableVariables(variables, iwc.getCurrentLocale(), iwc.isSuperAdmin(), false);
	}

	protected IWResourceBundle getIWResourceBundle() {
		return getResourceBundle(getBundle(getBundleIdentifier()));
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
			getLogger().info("No info about the financing tasks. Case identifier: " + caseBoard.getCaseIdentifier());
			return;
		}

		int tasksIndex = 0;
		Map<Integer, Map<String, String>> valuesToReplace = new TreeMap<>();
		for (Map<String, String> taskInfo: tasksInfo) {
			if (MapUtil.isEmpty(taskInfo)) {
				continue;
			}

			String taskName = taskInfo.get("task");
			if (StringUtil.isEmpty(taskName)) {
				taskName = CoreConstants.MINUS;
			}

			Long cost = getNumberValue(taskInfo.get("cost_estimate"), Boolean.FALSE);

			Map<String, String> cells = new HashMap<>();
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

		tasksInfo = new ArrayList<>();
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

	@Override
	public boolean hasCustomColumns(String uuid) {
		if (StringUtil.isEmpty(uuid)) {
			return false;
		}

		List<String> customColumns = getCustomColumns(uuid);
		return !ListUtil.isEmpty(customColumns);
	}

	private UserBusiness getUserBusiness() {
		return getServiceInstance(UserBusiness.class);
	}

}