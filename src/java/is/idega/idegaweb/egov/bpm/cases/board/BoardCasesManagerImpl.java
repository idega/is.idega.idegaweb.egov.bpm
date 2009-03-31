package is.idega.idegaweb.egov.bpm.cases.board;

import is.idega.idegaweb.egov.bpm.cases.CaseProcessInstanceRelationImpl;
import is.idega.idegaweb.egov.bpm.cases.presentation.beans.CasesBPMAssetsState;
import is.idega.idegaweb.egov.cases.business.BoardCasesComparator;
import is.idega.idegaweb.egov.cases.business.BoardCasesManager;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;
import is.idega.idegaweb.egov.cases.presentation.CaseViewer;
import is.idega.idegaweb.egov.cases.presentation.CasesBoardViewer;
import is.idega.idegaweb.egov.cases.presentation.CasesProcessor;
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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.context.exe.TokenVariableMap;
import org.jbpm.context.exe.VariableInstance;
import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.process.business.CasesRetrievalManager;
import com.idega.block.process.data.Case;
import com.idega.block.process.presentation.UserCases;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.builder.business.BuilderLogicWrapper;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.core.contact.data.Email;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.identity.BPMUser;
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
import com.idega.util.URIUtil;
import com.idega.util.expression.ELUtil;

@Scope("singleton")
@Service
@Transactional/*(propagation = Propagation.SUPPORTS)*/
public class BoardCasesManagerImpl implements BoardCasesManager {
	
	private static final List<String> GRADING_VARIABLES = Collections
	        .unmodifiableList(Arrays.asList("string_ownerInnovationalValue",
	            "string_ownerCompetitionValue",
	            "string_ownerEntrepreneursValue",
	            "string_ownerPossibleDevelopments", "string_ownerNatureStatus",
	            "string_ownerApplication", "string_ownerOverturn",
	            "string_ownerProceeds", "string_ownerEconomist",
	            "string_ownerEmployees", "string_ownerForsvarsmenn",
	            "string_ownerConstant", "string_ownerNewConstant",
	            "string_ownerBusiness", "string_ownerProject",
	            "string_ownerCostValue", "string_ownerProjectedSize",
	            "string_ownerEntrepreneurCompany"));
	
	private static final Logger LOGGER = Logger
	        .getLogger(BoardCasesManagerImpl.class.getName());
	
	public static final String BOARD_CASES_LIST_SORTING_PREFERENCES = "boardCasesListSortingPreferencesAttribute";
	
	private CasesRetrievalManager caseManager;
	
	@Autowired
	private BPMFactory bpmFactory;
	
	@Autowired
	private CaseProcessInstanceRelationImpl caseProcessInstanceRelation;
	
	@Autowired
	private CasesBPMDAO casesBPMDAO;
	
	@Autowired
	private BuilderLogicWrapper builderLogicWrapper;
	
	private List<String> variables;
	
	public List<CaseBoardBean> getAllSortedCases(IWContext iwc,
	        IWResourceBundle iwrb, String caseStatus, String processName) {
		Collection<GeneralCase> cases = getCases(iwc, caseStatus, processName);
		if (ListUtil.isEmpty(cases)) {
			return null;
		}
		
		List<CaseBoardBean> boardBeans = new ArrayList<CaseBoardBean>();
		for (GeneralCase theCase : cases) {
			if (isCaseAvailableForBoard(theCase)) {
				CaseBoardBean boardCase = getFilledBoardCaseWithInfo(theCase);
				boardBeans.add(boardCase);
			}
		}
		
		sortBoardCases(iwc, boardBeans);
		
		return boardBeans;
	}
	
	private CaseBoardBean getFilledBoardCaseWithInfo(GeneralCase theCase) {
		CasesRetrievalManager caseManager = getCaseManager();
		if (caseManager == null) {
			return null;
		}
		
		List<String> values = getStringVariablesValuesByVariablesNamesForProcessInstance(
		    new Integer(theCase.getPrimaryKey().toString()), getVariables());
		
		if (ListUtil.isEmpty(values)) {
			return null;
		}
		
		CaseBoardBean boardCase = new CaseBoardBean();
		boardCase.setCaseId(theCase.getPrimaryKey().toString());
		
		boardCase.setApplicantName(getStringValue(values.get(0)));
		boardCase.setPostalCode(getStringValue(values.get(1)));
		boardCase.setCaseIdentifier(getStringValue(values.get(2)));
		boardCase.setCaseDescription(getStringValue(values.get(3)));
		
		boardCase.setTotalCost(String.valueOf(getNumberValue(values.get(4),
		    true)));
		boardCase.setAppliedAmount(String.valueOf(getNumberValue(values.get(5),
		    true)));
		
		boardCase.setNutshell(getStringValue(values.get(6)));
		// Grading sums should be the 7th
		boardCase.setCategory(getStringValue(values.get(8)));
		
		boardCase.setComment(getStringValue(values.get(9)));
		boardCase
		        .setGrantAmountSuggestion(getNumberValue(values.get(10), false));
		boardCase.setBoardAmount(getNumberValue(values.get(11), false));
		boardCase.setRestrictions(getStringValue(values.get(12)));
		
		return boardCase;
	}
	
	private String getStringValue(String value) {
		if (StringUtil.isEmpty(value) || "no_value".equals(value)) {
			return CoreConstants.EMPTY;
		}
		
		return value;
	}
	
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	List<String> getStringVariablesValuesByVariablesNamesForProcessInstance(
	        Integer caseId, List<String> variablesNames) {
		Long processInstanceId = getCaseProcessInstanceRelation()
		        .getCaseProcessInstanceId(caseId);
		
		ProcessInstanceW piw = getBpmFactory()
		        .getProcessManagerByProcessInstanceId(processInstanceId)
		        .getProcessInstance(processInstanceId);
		
		ProcessInstance pi = piw.getProcessInstance();
		TokenVariableMap tvarmap = pi.getContextInstance().getTokenVariableMap(
		    pi.getRootToken());
		@SuppressWarnings("unchecked")
		Map<String, VariableInstance> variableInstances = tvarmap
		        .getVariableInstances();
		
		String noValueKeyword = "no_value";
		List<String> values = new ArrayList<String>();
		
		for (String name : variablesNames) {
			boolean addedValue = false;
			
			VariableInstance variableInstance = variableInstances.get(name);
			Object value = variableInstance == null ? null : variableInstance
			        .getValue();
			
			if (value == null) {
				value = noValueKeyword;
			}
			values.add(value.toString());
			addedValue = Boolean.TRUE;
			
			if (!addedValue) {
				values.add(noValueKeyword);
			}
		}
		
		return values;
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
				return Long.valueOf(Double.valueOf(
				    numberValue.doubleValue() / 1000).longValue());
			}
			
			return Long.valueOf(numberValue.longValue());
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting number value from: "
			        + value);
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
		Object o = iwc
		        .getSessionAttribute(BOARD_CASES_LIST_SORTING_PREFERENCES);
		if (o instanceof List) {
			sortingPreferences = (List<String>) o;
		}
		
		Collections.sort(boardCases, new BoardCasesComparator(iwc.getLocale(),
		        sortingPreferences));
	}
	
	@SuppressWarnings("unchecked")
	private Collection<GeneralCase> getCases(IWApplicationContext iwac,
	        String caseStatus, String processName) {
		Collection<Case> allCases = null;
		if (!StringUtil.isEmpty(processName)) {
			// Getting cases by application
			allCases = getCasesByProcessAndCaseStatus(iwac, caseStatus,
			    processName);
		} else {
			// Getting cases by case status
			if (StringUtil.isEmpty(caseStatus)) {
				LOGGER.warning("Case status is unkown - terminating!");
				return null;
			}
			CasesBusiness casesBusiness = getCasesBusiness(iwac);
			if (casesBusiness == null) {
				return null;
			}
			try {
				allCases = casesBusiness.getCasesByCriteria(null, null, null,
				    casesBusiness.getCaseStatus(caseStatus), false);
			} catch (RemoteException e) {
				LOGGER.log(Level.SEVERE,
				    "Error getting cases by cases status: " + caseStatus, e);
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
	
	private Collection<Case> getCasesByProcessAndCaseStatus(
	        IWApplicationContext iwac, String caseStatus, String processName) {
		CasesRetrievalManager caseManager = getCaseManager();
		if (caseManager == null) {
			LOGGER.severe(CasesRetrievalManager.class
			        + " bean was not initialized!");
			return null;
		}
		
		Collection<Long> casesIdsByProcessDefinition = caseManager
		        .getCasesIdsByProcessDefinitionName(processName);
		if (ListUtil.isEmpty(casesIdsByProcessDefinition)) {
			return null;
		}
		
		List<Integer> ids = new ArrayList<Integer>(casesIdsByProcessDefinition
		        .size());
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
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac,
			    CasesBusiness.class);
		} catch (IBOLookupException e) {
			LOGGER.log(Level.SEVERE, "Error getting " + CasesBusiness.class, e);
		}
		
		return null;
	}
	
	private CasesRetrievalManager getCaseManager() {
		if (caseManager == null) {
			try {
				caseManager = ELUtil.getInstance().getBean(
				    "casesBPMCaseHandler");
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Error getting Spring bean for: "
				        + CasesRetrievalManager.class, e);
			}
		}
		return caseManager;
	}
	
	private List<String> getVariables() {
		if (variables == null) {
			variables = new ArrayList<String>(CasesBoardViewer.CASE_FIELDS
			        .size());
			for (AdvancedProperty variable : CasesBoardViewer.CASE_FIELDS) {
				variables.add(variable.getId());
			}
		}
		return variables;
	}
	
	@Transactional(propagation = Propagation.REQUIRED)
	public AdvancedProperty setCaseVariableValue(Integer caseId,
	        String variableName, String value, String role, String backPage) {
		if (caseId == null || StringUtil.isEmpty(variableName)
		        || StringUtil.isEmpty(value)) {
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
			
			Long processInstanceId = getCaseProcessInstanceRelation()
			        .getCaseProcessInstanceId(caseId);
			
			ProcessInstanceW piw = getBpmFactory()
			        .getProcessManagerByProcessInstanceId(processInstanceId)
			        .getProcessInstance(processInstanceId);
			
			String taskName = "Grading";
			List<TaskInstanceW> allTasks = piw
			        .getUnfinishedTaskInstancesForTask(taskName);
			
			if (ListUtil.isEmpty(allTasks)) {
				LOGGER.log(Level.WARNING,
				    "No tasks instances were found for task = " + taskName
				            + " by process instance: " + processInstanceId);
				return null;
			}
			
			// should be only one task instance
			if (allTasks.size() > 1)
				LOGGER.log(Level.WARNING,
				    "More than one task instance found for task = " + taskName
				            + " when only one expected");
			
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
			
			TaskInstanceW viewTIW = getBpmFactory()
			        .getProcessManagerByTaskInstanceId(viewTaskInstanceId)
			        .getTaskInstance(viewTaskInstanceId);
			
			viewTIW.submit(viewSubmission);
			
			return new AdvancedProperty(value, getLinkToTheTask(iwc, caseId
			        .toString(), getPageUriForTaskViewer(iwc),
			    sharedTaskInstanceId.toString(), backPage));
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error saving variable '" + variableName
			        + "' with value '" + value + "' for case: " + caseId, e);
		}
		
		return null;
	}
	
	public String getLinkToTheTask(IWContext iwc, String caseId, String basePage, String backPage) {
		if (iwc == null || StringUtil.isEmpty(caseId)
		        || StringUtil.isEmpty(basePage)) {
			return null;
		}
		
		try {
			String taskId = getTaskInstanceIdForGradingTask(iwc, new Integer(
			        caseId));
			if (StringUtil.isEmpty(taskId)) {
				return iwc.getRequestURI();
			}
			
			return getLinkToTheTask(iwc, caseId, basePage, taskId, backPage);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Can't get uri to the task: ", e);
		}
		
		return iwc.getRequestURI();
	}
	
	private String getLinkToTheTask(IWContext iwc, String caseId, String basePage, String taskId, String backPage) {
		URIUtil uriUtil = new URIUtil(basePage);
		
		uriUtil.setParameter(CasesProcessor.PARAMETER_ACTION, String
		        .valueOf(UserCases.ACTION_CASE_MANAGER_VIEW));
		uriUtil.setParameter(CaseViewer.PARAMETER_CASE_PK, caseId);
		uriUtil.setParameter("tiId", taskId);
		if (!StringUtil.isEmpty(backPage)) {
			uriUtil.setParameter(CasesBPMAssetsState.CASES_ASSETS_SPECIAL_BACK_PAGE_PARAMETER, backPage);
		}
		
		return iwc.getIWMainApplication().getTranslatedURIWithContext(
		    uriUtil.getUri());
	}
	
	private String getTaskInstanceIdForGradingTask(IWContext iwc, Integer caseId) {
		
		try {
			
			Long processInstanceId = getCaseProcessInstanceRelation()
			        .getCaseProcessInstanceId(caseId);
			ProcessInstanceW piw = getBpmFactory()
			        .getProcessManagerByProcessInstanceId(processInstanceId)
			        .getProcessInstance(processInstanceId);
			
			TaskInstanceW gradingTIW = piw
			        .getSingleUnfinishedTaskInstanceForTask("Grading");
			
			if (gradingTIW != null)
				return String.valueOf(gradingTIW.getTaskInstanceId());
			else {
				LOGGER.log(Level.WARNING,
				    "No grading task found for processInstance="
				            + processInstanceId);
				
				return null;
			}
			
		} catch (Exception e) {
			LOGGER.log(Level.WARNING,
			    "Error getting grading task instance for caseId=" + caseId, e);
			return null;
		}
		
		// try {
		// taskId = getCaseManager().getTaskInstanceIdForTask(
		// getCasesBusiness(iwc).getCase(caseId), "Grading");
		// } catch (Exception e) {
		// LOGGER.log(Level.WARNING, "Error getting task instance for case: "
		// + caseId, e);
		// }
		// if (taskId == null) {
		// return null;
		// }
		// return String.valueOf(taskId.longValue());
	}
	
	public String getGradingSum(IWContext iwc, CaseBoardBean boardCase) {
		List<String> gradingValues = null;
		try {
			gradingValues = getStringVariablesValuesByVariablesNamesForProcessInstance(
			    new Integer(boardCase.getCaseId()), GRADING_VARIABLES);
			
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting grading values for case: "
			        + boardCase.getCaseId(), e);
		}
		if (ListUtil.isEmpty(gradingValues)) {
			return String.valueOf(0);
		}
		
		long sum = 0;
		Long gradeValue = null;
		for (String value : gradingValues) {
			if (StringUtil.isEmpty(getStringValue(value))) {
				continue;
			}
			
			gradeValue = null;
			try {
				gradeValue = Long.valueOf(value);
			} catch (Exception e) {
				LOGGER.warning("Unable to convert '" + value + "' to number!");
			}
			
			if (gradeValue != null) {
				sum += gradeValue.longValue();
			}
		}
		
		return String.valueOf(sum);
	}
	
	public String getPageUriForTaskViewer(IWContext iwc) {
		String uri = builderLogicWrapper.getBuilderService(iwc)
		        .getFullPageUrlByPageType(iwc,
		            BPMUser.defaultAssetsViewPageType, true);
		return StringUtil.isEmpty(uri) ? iwc.getRequestURI() : uri;
	}
	
	public CaseBoardTableBean getTableData(IWContext iwc, String caseStatus,
	        String processName) {
		if (iwc == null) {
			return null;
		}
		
		IWBundle bundle = iwc.getIWMainApplication().getBundle(
		    CasesConstants.IW_BUNDLE_IDENTIFIER);
		IWResourceBundle iwrb = bundle.getResourceBundle(iwc);
		CaseBoardTableBean data = new CaseBoardTableBean();
		
		List<CaseBoardBean> boardCases = getAllSortedCases(iwc, iwrb,
		    caseStatus, processName);
		if (ListUtil.isEmpty(boardCases)) {
			data.setErrorMessage(iwrb.getLocalizedString(
			    "cases_board_viewer.no_cases_found", "There are no cases!"));
			return data;
		}
		
		// Header
		data.setHeaderLabels(getTableHeaders(iwrb));
		
		// Body
		long grantAmountSuggestionTotal = 0;
		long boardAmountTotal = 0;
		List<CaseBoardTableBodyRowBean> bodyRows = new ArrayList<CaseBoardTableBodyRowBean>(
		        boardCases.size());
		for (CaseBoardBean caseBoard : boardCases) {
			CaseBoardTableBodyRowBean rowBean = new CaseBoardTableBodyRowBean();
			rowBean.setId(new StringBuilder("uniqueCaseId").append(
			    caseBoard.getCaseId()).toString());
			rowBean.setCaseId(caseBoard.getCaseId());
			rowBean.setCaseIdentifier(caseBoard.getCaseIdentifier());
			
			int index = 0;
			List<String> allValues = caseBoard.getAllValues();
			List<String> rowValues = new ArrayList<String>(allValues.size());
			for (String value : allValues) {
				if (index == 2) {
					// Link to grading task
					rowValues.add(caseBoard.getCaseIdentifier());
				} else if (index == 7) {
					// SUMs for grading variables
					rowValues.add(getGradingSum(iwc, caseBoard));
				} else {
					rowValues.add(value);
				}
				
				if (index == allValues.size() - 3) {
					// Calculating grant amount suggestions
					grantAmountSuggestionTotal += caseBoard
					        .getGrantAmountSuggestion();
				} else if (index == allValues.size() - 2) {
					// Calculating board amounts
					boardAmountTotal += caseBoard.getBoardAmount();
				}
				
				index++;
			}
			rowValues.add(getHandlerId(caseBoard));
			
			rowBean.setValues(rowValues);
			bodyRows.add(rowBean);
		}
		data.setBodyBeans(bodyRows);
		
		// Footer
		data.setFooterValues(getFooterValues(iwrb, grantAmountSuggestionTotal,
		    boardAmountTotal));
		
		// Everything is OK
		data.setFilledWithData(Boolean.TRUE);
		return data;
	}
	
	private String getHandlerId(CaseBoardBean caseBoard) {
		if (caseBoard == null || StringUtil.isEmpty(caseBoard.getCaseId())) {
			return null;
		}
		
		Integer handlerId = null;
		try {
			Long processInstanceId = getCasesBPMDAO().getCaseProcInstBindByCaseId(Integer.valueOf(caseBoard.getCaseId())).getProcInstId();
			ProcessInstanceW piw = getBpmFactory().getProcessInstanceW(processInstanceId);
			handlerId = piw.getHandlerId();
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting handler for case: " + caseBoard.getCaseId(), e);
		}
		
		return handlerId == null ? null : handlerId.toString();
	}
	
	public AdvancedProperty getHandlerInfo(IWContext iwc, String userId) {
		if (StringUtil.isEmpty(userId)) {
			return null;
		}
		
		UserBusiness userBusiness = null;
		try {
			userBusiness = (UserBusiness) IBOLookup.getServiceInstance(iwc, UserBusiness.class);
		} catch(RemoteException e) {
			LOGGER.log(Level.WARNING, "Error getting " + UserBusiness.class, e);
		}
		if (userBusiness == null) {
			return null;
		}
		
		User handler = null;
		try {
			handler = userBusiness.getUser(Integer.valueOf(userId));
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting user by ID: " + userId, e);
		}
		if (handler == null) {
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
		List<String> headers = new ArrayList<String>(
		        CasesBoardViewer.CASE_FIELDS.size());
		for (AdvancedProperty header : CasesBoardViewer.CASE_FIELDS) {
			headers.add(iwrb.getLocalizedString(new StringBuilder(prefix)
			        .append(header.getId()).toString(), header.getValue()));
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
}
