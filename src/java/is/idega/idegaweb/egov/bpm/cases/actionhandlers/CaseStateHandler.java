package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import java.sql.Date;
import java.util.List;
import java.util.Map;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.business.DefaultSpringBean;
import com.idega.idegaweb.egov.bpm.data.CaseState;
import com.idega.idegaweb.egov.bpm.data.CaseStateInstance;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.util.ListUtil;
import com.idega.util.expression.ELUtil;


@Service("caseStateHandler")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CaseStateHandler extends DefaultSpringBean implements ActionHandler{

	private static final long serialVersionUID = 7151295362387542252L;

	public static final String ADD_STATES_ACTION = "addStates";
	public static final String SET_STATE_RECEIVED_ACTION = "setStateReceived";
	public static final String SET_STATE_ACTIVE_ACTION = "setStateActive";
	public static final String SET_STATE_FINISHED_ACTION = "setStateFinished";
	public static final String SET_STATE_CANCELLED_ACTION = "setStateCancelled";
	public static final String SET_STATE_SUSPENDED_ACTION = "setStateSuspended";
	public static final String SET_STATE_INQUEUE_ACTION = "setStateInqueue";
	public static final String SET_ALL_STATES_CANCELLED_ACTION = "setAllStatesCancelled";
	public static final String SET_ALL_STATES_FINISHED_ACTION = "setAllStatesFinished";
	
	public static final String CASE_ID_VARIABLE_NAME = "caseIdentifier";
	public static final String DEFAULT_CASE_ID_VARIABLE_NAME = "string_caseIdentifier";
	
	public static final long MILLISECONDS_PER_DAY = 1000L * 60 * 60 * 24;
	
	private Map<String, List<String>> actionMap;
	
	@Autowired
	private CasesBPMDAO casesBPMDAO;
	
	
	@Override
	public void execute(ExecutionContext ctx) throws Exception {
		if (this.actionMap==null) return;
		java.util.Date currentTime = new java.util.Date();
		if (this.actionMap.containsKey(ADD_STATES_ACTION)){
			List<CaseState> states  = getCasesBPMDAO().getCaseStatesByProcessDefinitionName(ctx.getProcessDefinition().getName());
			long addDays = 0;
			for (CaseState state: states){
				CaseStateInstance caseState = new CaseStateInstance();
				caseState.setProcessId(ctx.getProcessInstance().getId());
				if (this.actionMap.containsKey(CASE_ID_VARIABLE_NAME)){
					List<String> caseId = this.actionMap.get(CASE_ID_VARIABLE_NAME);
					if (!ListUtil.isEmpty(caseId)){
						caseState.setCaseId(ctx.getVariable(caseId.get(0)).toString());
					}
				}
				else {
					caseState.setCaseId(ctx.getVariable(DEFAULT_CASE_ID_VARIABLE_NAME).toString());
				}
				caseState.setStateName(state.getStateName());
				caseState.setState(state);
				caseState.setStateExpectedStartDate(new Date(currentTime.getTime() + (MILLISECONDS_PER_DAY * addDays)));
				addDays += state.getStateSLA();
				caseState.setStateExpectedEndDate(new Date(currentTime.getTime() + (MILLISECONDS_PER_DAY * addDays)));
				caseState.setStateState(CaseStateInstance.State.INQUEUE);
				getCasesBPMDAO().saveCasesStateInstance(caseState);
			}
		}

		if (this.actionMap.containsKey(SET_STATE_RECEIVED_ACTION)){
			List<String> stateList = this.actionMap.get(SET_STATE_RECEIVED_ACTION);
			if (!ListUtil.isEmpty(stateList)){
				List<CaseStateInstance> caseStates = getCasesBPMDAO().getStateInstancesForProcessByName(ctx.getProcessInstance().getId(), stateList);
				for (CaseStateInstance state : caseStates){
					state.setStateState(CaseStateInstance.State.RECEIVED);
					getCasesBPMDAO().saveCasesStateInstance(state);
				}
			}
		}
		
		if (this.actionMap.containsKey(SET_STATE_ACTIVE_ACTION)){
			List<String> stateList = this.actionMap.get(SET_STATE_ACTIVE_ACTION);
			if (!ListUtil.isEmpty(stateList)){
				List<CaseStateInstance> caseStates = getCasesBPMDAO().getStateInstancesForProcessByName(ctx.getProcessInstance().getId(), stateList);
				for (CaseStateInstance state : caseStates){
					state.setStateState(CaseStateInstance.State.ACTIVE);
					state.setStateStartDate(new Date(currentTime.getTime()));
					getCasesBPMDAO().saveCasesStateInstance(state);
				}
			}
		}
		
		if (this.actionMap.containsKey(SET_STATE_CANCELLED_ACTION)){
			List<String> stateList = this.actionMap.get(SET_STATE_CANCELLED_ACTION);
			if (!ListUtil.isEmpty(stateList)){
				List<CaseStateInstance> caseStates = getCasesBPMDAO().getStateInstancesForProcessByName(ctx.getProcessInstance().getId(), stateList);
				for (CaseStateInstance state : caseStates){
					state.setStateState(CaseStateInstance.State.CANCELLED);
					state.setStateEndDate(new Date(currentTime.getTime()));
					getCasesBPMDAO().saveCasesStateInstance(state);
				}
			}
		}
		
		if (this.actionMap.containsKey(SET_STATE_SUSPENDED_ACTION)){
			List<String> stateList = this.actionMap.get(SET_STATE_SUSPENDED_ACTION);
			if (!ListUtil.isEmpty(stateList)){
				List<CaseStateInstance> caseStates = getCasesBPMDAO().getStateInstancesForProcessByName(ctx.getProcessInstance().getId(), stateList);
				for (CaseStateInstance state : caseStates){
					state.setStateState(CaseStateInstance.State.SUSPENDED);
					getCasesBPMDAO().saveCasesStateInstance(state);
				}
			}
		}
		
		if (this.actionMap.containsKey(SET_STATE_INQUEUE_ACTION)){
			List<String> stateList = this.actionMap.get(SET_STATE_INQUEUE_ACTION);
			if (!ListUtil.isEmpty(stateList)){
				List<CaseStateInstance> caseStates = getCasesBPMDAO().getStateInstancesForProcessByName(ctx.getProcessInstance().getId(), stateList);
				for (CaseStateInstance state : caseStates){
					state.setStateState(CaseStateInstance.State.INQUEUE);
					getCasesBPMDAO().saveCasesStateInstance(state);
				}
			}
		}
		
		if (this.actionMap.containsKey(SET_STATE_FINISHED_ACTION)){
			List<String> stateList = this.actionMap.get(SET_STATE_FINISHED_ACTION);
			if (!ListUtil.isEmpty(stateList)){
				List<CaseStateInstance> caseStates = getCasesBPMDAO().getStateInstancesForProcessByName(ctx.getProcessInstance().getId(), stateList);
				for (CaseStateInstance state : caseStates){
					state.setStateState(CaseStateInstance.State.FINISHED);
					state.setStateEndDate(new Date(currentTime.getTime()));
					getCasesBPMDAO().saveCasesStateInstance(state);
				}
			}
		}
		
		if (this.actionMap.containsKey(SET_ALL_STATES_CANCELLED_ACTION)){
			List<CaseStateInstance> caseStates = getCasesBPMDAO().getStateInstancesForProcess(ctx.getProcessInstance().getId());
			for (CaseStateInstance state : caseStates){
				state.setStateState(CaseStateInstance.State.CANCELLED);
				state.setStateEndDate(new Date(currentTime.getTime()));
				getCasesBPMDAO().saveCasesStateInstance(state);
			}
		}
		
		if (this.actionMap.containsKey(SET_ALL_STATES_FINISHED_ACTION)){
			List<CaseStateInstance> caseStates = getCasesBPMDAO().getStateInstancesForProcess(ctx.getProcessInstance().getId());
			for (CaseStateInstance state : caseStates){
				state.setStateState(CaseStateInstance.State.FINISHED);
				state.setStateEndDate(new Date(currentTime.getTime()));
				getCasesBPMDAO().saveCasesStateInstance(state);
			}
		}
	}

	public Map<String, List<String>> getActionMap() {
		return actionMap;
	}

	public void setActionMap(Map<String, List<String>> actionMap) {
		this.actionMap = actionMap;
	}

	public CasesBPMDAO getCasesBPMDAO() {
		if (casesBPMDAO==null) {
			ELUtil.getInstance().autowire(this);
		}
		return casesBPMDAO;
	}

	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}

}
