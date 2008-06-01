package is.idega.idegaweb.egov.bpm.cases.exe;

import com.idega.jbpm.exe.BPMManagersFactory;
import com.idega.jbpm.exe.ProcessManager;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/06/01 17:02:33 $ by $Author: civilis $
 */
public class CasesBPMManagersCreator implements BPMManagersFactory {
	
	public static final String MANAGERS_TYPE = "cases";
	private static final String BEAN_IDENTIFIER = "casesBPMManagersCreator";
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

	public void setProcessManager(ProcessManager processManager) {
		this.processManager = processManager;
	}
}