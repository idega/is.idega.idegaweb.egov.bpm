package is.idega.idegaweb.egov.bpm.application;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/04/29 13:39:10 $ by $Author: civilis $
 */
public interface AppSupportsManager {
	
	@Transactional(readOnly = true)
	public abstract List<String> getRolesCanStartProcess();
	
	@Transactional
	public abstract void updateRolesCanStartProcess(List<String> rolesKeys);
	
	public abstract void setProcessName(String processName);
	
	public abstract void setApplicationId(Integer applicationId);
}