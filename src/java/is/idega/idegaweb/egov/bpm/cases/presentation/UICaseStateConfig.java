package is.idega.idegaweb.egov.bpm.cases.presentation;

import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.facelets.ui.FaceletComponent;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.idegaweb.egov.bpm.data.CaseState;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.presentation.IWBaseComponent;
import com.idega.presentation.IWContext;
import com.idega.util.PresentationUtil;
import com.idega.util.expression.ELUtil;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.presentation.beans.CaseStateConfigBean;

import org.jbpm.graph.def.ProcessDefinition;

public class UICaseStateConfig  extends IWBaseComponent {

	public static final String SAVE_STATE_PARAMETER = "iw_save_case_state";
	public static final String ADD_STATE_PARAMETER = "iw_add_case_state";
	
	public static final String STATE_NAME_PARAMETER = "iw_state_name";
	public static final String STATE_PROCESS_NAME_PARAMETER = "iw_process_name";
	public static final String STATE_LOCALIZATION_KEY_PARAMETER = "iw_localization_key_name";
	public static final String STATE_DEFAULT_LOCALIZED_NAME_PARAMETER = "iw_default_Localized_name_name";
	public static final String STATE_SLA_PARAMETER = "iw_state_sla";
	public static final String STATE_INACTIVE_SLA_PARAMETER = "iw_inactive_sla_name";
	public static final String STATE_SLA_REMINDER_INTERVAL_PARAMETER = "iw_state_sla_reminder_interval";
	public static final String STATE_SEQUENCE_ID_PARAMETER = "iw_state_sequence_id";
	
	
	@Autowired
	private BPMContext bpmContext;
	
	@Autowired
	private CasesBPMDAO casesBPMDAO;
	
	private CaseStateConfigBean caseStateConfigBean;
	
	private int error = 0;
	
	@Override
	protected void initializeComponent(FacesContext context) {
		
		super.initializeComponent(context);
		
		IWContext iwc = IWContext.getIWContext(context);
		
		try {
			if (iwc.isParameterSet(ADD_STATE_PARAMETER)){
				addState(iwc);
			}
			
			if (iwc.isParameterSet(SAVE_STATE_PARAMETER)){
				updateState(iwc);
			}
		} catch (Exception e) {
			setError(1);
			e.printStackTrace();
		}
		
		this.caseStateConfigBean = ELUtil.getInstance().getBean(CaseStateConfigBean.NAME);
		
		List<ProcessDefinition> processDefinitions = getBpmContext()
				.execute(new JbpmCallback<List<ProcessDefinition>>() {
					@SuppressWarnings("unchecked")
					@Override
					public List<ProcessDefinition> doInJbpm(JbpmContext context) throws JbpmException {
						return (List<ProcessDefinition>) context.getGraphSession().findAllProcessDefinitions();
					}
				});
		
		List<String> procDefNames = new ArrayList<String>();
		for (ProcessDefinition processDefinition: processDefinitions){
			if (!procDefNames.contains(processDefinition.getName())) procDefNames.add(processDefinition.getName());
		}
		
		List<CaseState> caseStates = getCasesBPMDAO().getCaseStates();
		
		this.caseStateConfigBean.setCaseStates(caseStates);
		this.caseStateConfigBean.setProcessDefinitions(procDefNames);
		
		IWBundle bundle = getBundle(context, IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		FaceletComponent facelet = (FaceletComponent)context.getApplication().createComponent(FaceletComponent.COMPONENT_TYPE);
		facelet.setFaceletURI(bundle.getFaceletURI("UICasesStatusConfig.xhtml"));
		
		PresentationUtil.addJavaScriptSourceLineToHeader(iwc, bundle.getVirtualPathWithFileNameString("javascript/CaseStateConfig.js")); 
		
		this.add(facelet);
	}

	private void updateState(IWContext iwc) {
		if ((iwc.isParameterSet(STATE_NAME_PARAMETER)) && (iwc.isParameterSet(STATE_PROCESS_NAME_PARAMETER))){
			CaseState caseState = getCasesBPMDAO().getCaseStateByProcessDefinitionNameAndStateName(iwc.getParameter(STATE_PROCESS_NAME_PARAMETER), iwc.getParameter(STATE_NAME_PARAMETER));
			if (iwc.isParameterSet(STATE_DEFAULT_LOCALIZED_NAME_PARAMETER)) caseState.setStateDefaultLocalizedName(iwc.getParameter(STATE_DEFAULT_LOCALIZED_NAME_PARAMETER));
			if (iwc.isParameterSet(STATE_INACTIVE_SLA_PARAMETER)) caseState.setStateInactiveSLA(Long.valueOf(iwc.getParameter(STATE_INACTIVE_SLA_PARAMETER)));
			if (iwc.isParameterSet(STATE_LOCALIZATION_KEY_PARAMETER)) caseState.setStateLocalizationKey(iwc.getParameter(STATE_LOCALIZATION_KEY_PARAMETER));
			if (iwc.isParameterSet(STATE_SEQUENCE_ID_PARAMETER)) caseState.setStateSequenceId(Long.valueOf(iwc.getParameter(STATE_SEQUENCE_ID_PARAMETER)));
			if (iwc.isParameterSet(STATE_SLA_PARAMETER)) caseState.setStateSLA(Long.valueOf(iwc.getParameter(STATE_SLA_PARAMETER)));
			if (iwc.isParameterSet(STATE_SLA_REMINDER_INTERVAL_PARAMETER)) caseState.setStateSLABreachReminderInterval(Long.valueOf(iwc.getParameter(STATE_SLA_REMINDER_INTERVAL_PARAMETER)));
			getCasesBPMDAO().saveCasesState(caseState);
		}
	}

	private void addState(IWContext iwc) {
		if ((iwc.isParameterSet(STATE_NAME_PARAMETER)) && (iwc.isParameterSet(STATE_PROCESS_NAME_PARAMETER))){
			CaseState caseState = new CaseState();
			caseState.setProcessDefinitionName(iwc.getParameter(STATE_PROCESS_NAME_PARAMETER));
			caseState.setStateName(iwc.getParameter(STATE_NAME_PARAMETER));
			if (iwc.isParameterSet(STATE_DEFAULT_LOCALIZED_NAME_PARAMETER)) caseState.setStateDefaultLocalizedName(iwc.getParameter(STATE_DEFAULT_LOCALIZED_NAME_PARAMETER));
			if (iwc.isParameterSet(STATE_INACTIVE_SLA_PARAMETER)) caseState.setStateInactiveSLA(Long.valueOf(iwc.getParameter(STATE_INACTIVE_SLA_PARAMETER)));
			if (iwc.isParameterSet(STATE_LOCALIZATION_KEY_PARAMETER)) caseState.setStateLocalizationKey(iwc.getParameter(STATE_LOCALIZATION_KEY_PARAMETER));
			if (iwc.isParameterSet(STATE_SEQUENCE_ID_PARAMETER)) caseState.setStateSequenceId(Long.valueOf(iwc.getParameter(STATE_SEQUENCE_ID_PARAMETER)));
			if (iwc.isParameterSet(STATE_SLA_PARAMETER)) caseState.setStateSLA(Long.valueOf(iwc.getParameter(STATE_SLA_PARAMETER)));
			if (iwc.isParameterSet(STATE_SLA_REMINDER_INTERVAL_PARAMETER)) caseState.setStateSLABreachReminderInterval(Long.valueOf(iwc.getParameter(STATE_SLA_REMINDER_INTERVAL_PARAMETER)));
			getCasesBPMDAO().saveCasesState(caseState);
		}
	}

	public CaseStateConfigBean getCaseStateConfigBean() {
		return caseStateConfigBean;
	}

	public void setCaseStateConfigBean(CaseStateConfigBean caseStateConfigBean) {
		this.caseStateConfigBean = caseStateConfigBean;
	}

	public BPMContext getBpmContext() {
		if (bpmContext==null){
			ELUtil.getInstance().autowire(this);
		}
		return bpmContext;
	}

	public void setBpmContext(BPMContext bpmContext) {
		this.bpmContext = bpmContext;
	}

	public CasesBPMDAO getCasesBPMDAO() {
		if (casesBPMDAO==null){
			ELUtil.getInstance().autowire(this);
		}
		return casesBPMDAO;
	}

	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}

	public int getError() {
		return error;
	}

	public void setError(int error) {
		this.error = error;
	}
	
}
