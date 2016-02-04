package is.idega.idegaweb.egov.bpm.cases.presentation;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import com.idega.builder.business.BuilderLogicWrapper;
import com.idega.core.business.GeneralCompanyBusiness;
import com.idega.idegaweb.egov.bpm.data.CaseState;
import com.idega.idegaweb.egov.bpm.data.CaseStateInstance;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.artifacts.presentation.GridEntriesBean;
import com.idega.jbpm.artifacts.presentation.ProcessArtifacts;
import com.idega.jbpm.artifacts.presentation.ProcessArtifactsParamsBean;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.exe.BPMDocument;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.identity.permission.PermissionsFactory;
import com.idega.jbpm.presentation.xml.ProcessArtifactsListRow;
import com.idega.jbpm.presentation.xml.ProcessArtifactsListRows;
import com.idega.jbpm.signing.SigningHandler;
import com.idega.jbpm.utils.JBPMUtil;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.CoreUtil;
import com.idega.util.expression.ELUtil;

@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service(UICaseStates.SPRING_BEAN_NAME)
public class UICaseStates {

	public static final String SPRING_BEAN_NAME = "UICaseStates";
	
	@Autowired
	private CasesBPMDAO casesBPMDAO;
	
	@Autowired
	private BPMFactory bpmFactory;

	@Autowired
	private BPMContext idegaJbpmContext;

	@Autowired
	private VariablesHandler variablesHandler;

	@Autowired
	private PermissionsFactory permissionsFactory;

	@Autowired
	private BuilderLogicWrapper builderLogicWrapper;

	@Autowired(required = false)
	private SigningHandler signingHandler;

	@Autowired(required = false)
	private VariableInstanceQuerier variablesQuerier;

	@Autowired(required = false)
	private GeneralCompanyBusiness generalCompanyBusiness;
	
	private static final Logger LOGGER = Logger.getLogger(ProcessArtifacts.class.getName());
	
	public Document getProcessStateList(ProcessArtifactsParamsBean params) {
		

			Long processInstanceId = params.getPiId();

			if (processInstanceId == null || processInstanceId < 0) {
				ProcessArtifactsListRows rows = new ProcessArtifactsListRows();
				rows.setTotal(0);
				rows.setPage(0);

				try {
					return rows.getDocument();
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Exception while creating empty grid entries", e);
				}
			}

			IWContext iwc = getIWContext(true);
			if (iwc == null) {
				return null;
			}

			User loggedInUser = getBpmFactory().getBpmUserFactory().getCurrentBPMUser().getUserToUse();
			Locale userLocale = iwc.getCurrentLocale();

			ProcessInstanceW pi = getBpmFactory().getProcessManagerByProcessInstanceId(processInstanceId)
					.getProcessInstance(processInstanceId);

			List<CaseStateInstance> states = getCasesBPMDAO().getStateInstancesForProcess(pi.getProcessInstanceId());
			if (states==null) return null;
			
			ProcessArtifactsListRows rows = new ProcessArtifactsListRows();
			rows.setTotal(states.size());
			
			for (CaseStateInstance state: states){
				ProcessArtifactsListRow row = new ProcessArtifactsListRow();
				rows.addRow(row);
				row.setId(state.getId().toString());
				
				CaseState stateDef = getCasesBPMDAO().getCaseStateByProcessDefinitionNameAndStateName(pi.getProcessDefinitionW().getProcessDefinition().getName(),state.getStateName());
				row.addCell(stateDef.getStateDefaultLocalizedName());
				
				
				
				if (state.getStateExpectedStartDate() != null ) row.addCell(state.getStateExpectedStartDate().toString());
				else row.addCell("");
				
				if (state.getStateExpectedEndDate() != null ) row.addCell(state.getStateExpectedEndDate().toString());
				else row.addCell("");
				
				if (state.getStateStartDate() != null ) row.addCell(state.getStateStartDate().toString());
				else row.addCell("");
				
				if (state.getStateEndDate() != null ) row.addCell(state.getStateEndDate().toString());
				else row.addCell("");
				
			}

			
			try {
				return rows.getDocument();
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Exception while creating empty grid entries", e);
			}
			return null;

	}

	public CasesBPMDAO getCasesBPMDAO() {
		ELUtil.getInstance().autowire(this);
		return casesBPMDAO;
	}

	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}

	public BPMFactory getBpmFactory() {
		ELUtil.getInstance().autowire(this);
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	public BPMContext getIdegaJbpmContext() {
		ELUtil.getInstance().autowire(this);
		return idegaJbpmContext;
	}

	public void setIdegaJbpmContext(BPMContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public VariablesHandler getVariablesHandler() {
		ELUtil.getInstance().autowire(this);
		return variablesHandler;
	}

	public void setVariablesHandler(VariablesHandler variablesHandler) {
		this.variablesHandler = variablesHandler;
	}

	public PermissionsFactory getPermissionsFactory() {
		ELUtil.getInstance().autowire(this);
		return permissionsFactory;
	}

	public void setPermissionsFactory(PermissionsFactory permissionsFactory) {
		this.permissionsFactory = permissionsFactory;
	}

	public BuilderLogicWrapper getBuilderLogicWrapper() {
		ELUtil.getInstance().autowire(this);
		return builderLogicWrapper;
	}

	public void setBuilderLogicWrapper(BuilderLogicWrapper builderLogicWrapper) {
		this.builderLogicWrapper = builderLogicWrapper;
	}

	public SigningHandler getSigningHandler() {
		ELUtil.getInstance().autowire(this);
		return signingHandler;
	}

	public void setSigningHandler(SigningHandler signingHandler) {
		this.signingHandler = signingHandler;
	}

	public VariableInstanceQuerier getVariablesQuerier() {
		ELUtil.getInstance().autowire(this);
		return variablesQuerier;
	}

	public void setVariablesQuerier(VariableInstanceQuerier variablesQuerier) {
		this.variablesQuerier = variablesQuerier;
	}

	public GeneralCompanyBusiness getGeneralCompanyBusiness() {
		ELUtil.getInstance().autowire(this);
		return generalCompanyBusiness;
	}

	public void setGeneralCompanyBusiness(GeneralCompanyBusiness generalCompanyBusiness) {
		this.generalCompanyBusiness = generalCompanyBusiness;
	}

	private IWContext getIWContext(boolean checkIfLogged) {
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			LOGGER.warning("IWContext is unavailable!");
		}
		return iwc;
	}
	
}
