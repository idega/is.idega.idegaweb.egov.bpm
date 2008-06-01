package is.idega.idegaweb.egov.bpm.cases.exe;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.util.CoreConstants;
import com.idega.util.IWTimestamp;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/06/01 17:02:33 $ by $Author: civilis $
 */
public abstract class CasesBPMResources {
	
	public static final String IDENTIFIER_PREFIX = "P";
	
	private IdegaJbpmContext idegaJbpmContext;
	private VariablesHandler variablesHandler;
	private CasesBPMDAO casesBPMDAO;
	private BPMFactory bpmFactory;
	private BPMDAO bpmBindsDAO;
	
	protected abstract ProcessDefinitionW createPDW();
	
	public ProcessDefinitionW createProcessDefinition(long pdId) {
		
		
		ProcessDefinitionW pdw = createPDW();
		pdw.setProcessDefinitionId(pdId);
		
		return pdw;
	}

	protected abstract ProcessInstanceW createPIW();
	
	public ProcessInstanceW createProcessInstance(long piId) {
		
		ProcessInstanceW piw = createPIW();
		piw.setProcessInstanceId(piId);
		
		return piw;
	}
	
	protected abstract TaskInstanceW createTIW();
	
	public TaskInstanceW createTaskInstance(long tiId) {
		
		TaskInstanceW tiw = createTIW();
		tiw.setTaskInstanceId(tiId);
		
		return tiw;
	}

	public BPMDAO getBpmBindsDAO() {
		return bpmBindsDAO;
	}

	@Autowired
	public void setBpmBindsDAO(BPMDAO bpmBindsDAO) {
		this.bpmBindsDAO = bpmBindsDAO;
	}

	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public VariablesHandler getVariablesHandler() {
		return variablesHandler;
	}

	@Autowired
	public void setVariablesHandler(VariablesHandler variablesHandler) {
		this.variablesHandler = variablesHandler;
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	@Autowired
	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	@Autowired
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
	
	class CaseIdentifier {
		
		IWTimestamp time;
		Integer number;
		
		String generate() {
			
			String nr = String.valueOf(++number);
			
			while(nr.length() < 4)
				nr = "0"+nr;
			
			return new StringBuffer(IDENTIFIER_PREFIX)
			.append(CoreConstants.MINUS)
			.append(time.getYear())
			.append(CoreConstants.MINUS)
			.append(time.getMonth() < 10 ? "0"+time.getMonth() : time.getMonth())
			.append(CoreConstants.MINUS)
			.append(time.getDay() < 10 ? "0"+time.getDay() : time.getDay())
			.append(CoreConstants.MINUS)
			.append(nr)
			.toString();
		}
	}
	
	private CaseIdentifier lastCaseIdentifierNumber;
	
	public synchronized Object[] generateNewCaseIdentifier() {
		
		IWTimestamp currentTime = new IWTimestamp();
		
		if(lastCaseIdentifierNumber == null || !currentTime.equals(lastCaseIdentifierNumber.time)) {

			lastCaseIdentifierNumber = new CaseIdentifier();
			
			CaseProcInstBind b = getCasesBPMDAO().getCaseProcInstBindLatestByDateQN(new Date());
			
			if(b != null && b.getDateCreated() != null && b.getCaseIdentierID() != null) {
				
				lastCaseIdentifierNumber.time = new IWTimestamp(b.getDateCreated());
				lastCaseIdentifierNumber.number = b.getCaseIdentierID();
			} else {
			
				lastCaseIdentifierNumber.time = currentTime;
				lastCaseIdentifierNumber.number = 0;
			}
		}
		
		String generated = lastCaseIdentifierNumber.generate();
		
		return new Object[] {lastCaseIdentifierNumber.number, generated};
	}
}