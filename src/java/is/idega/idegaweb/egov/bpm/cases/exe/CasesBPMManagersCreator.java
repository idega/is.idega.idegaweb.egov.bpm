package is.idega.idegaweb.egov.bpm.cases.exe;

import javax.annotation.Resource;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.exe.BPMManagersFactory;
import com.idega.jbpm.exe.ProcessManager;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/09/17 13:18:09 $ by $Author: civilis $
 */
@Scope("singleton")
@Service(CasesBPMManagersCreator.BEAN_IDENTIFIER)
public class CasesBPMManagersCreator implements BPMManagersFactory {
	
	public static final String MANAGERS_TYPE = "cases";
	static final String BEAN_IDENTIFIER = "casesBPMManagersCreator";
	private ProcessManager processManager;
	
	public ProcessManager getProcessManager() {
		
		return processManager;
	}
	
	public String getManagersType() {
		
		return MANAGERS_TYPE; 
	}
	
	public String getBeanIdentifier() {

		return BEAN_IDENTIFIER;
	}

	@Resource(name="casesBpmProcessManager")
	public void setProcessManager(ProcessManager processManager) {
		this.processManager = processManager;
	}
}