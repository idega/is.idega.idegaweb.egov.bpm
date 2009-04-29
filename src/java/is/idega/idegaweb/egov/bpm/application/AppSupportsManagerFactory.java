package is.idega.idegaweb.egov.bpm.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/04/29 13:39:10 $ by $Author: civilis $
 */
@Service
@Scope("prototype")
public class AppSupportsManagerFactory {
	
	@Autowired
	private AppSupportsManager appSupportsManager;
	
	public AppSupportsManager getAppSupportsManager(Integer applicationId,
	        String processName) {
		
		appSupportsManager.setApplicationId(applicationId);
		appSupportsManager.setProcessName(processName);
		
		return appSupportsManager;
	}
}