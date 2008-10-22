package is.idega.idegaweb.egov.bpm.cases.messages;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.identity.BPMUserFactory;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/10/22 15:02:00 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class CaseUserFactory {
	
	@Autowired private BPMFactory bpmFactory;
	@Autowired private BPMUserFactory bpmUserFactory;
	
	public CaseUserImpl getCaseUser(User user, ProcessInstanceW piw) {
		
//		TODO: use some caching or smth
		return createCaseUser(user, piw, IWContext.getCurrentInstance());
	}
	
	public CaseUserImpl getCaseUser(User user, ProcessInstanceW piw, IWContext iwc) {
		
		return createCaseUser(user, piw, iwc);
	}
	
	private CaseUserImpl createCaseUser(User user, ProcessInstanceW piw, IWContext iwc) {
		
		CaseUserImpl caseUser = new CaseUserImpl(user, piw, iwc);
		caseUser.setBpmFactory(getBpmFactory());
		caseUser.setBpmUserFactory(getBpmUserFactory());
		return caseUser;
	}

	BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	BPMUserFactory getBpmUserFactory() {
		return bpmUserFactory;
	}

	void setBpmUserFactory(BPMUserFactory bpmUserFactory) {
		this.bpmUserFactory = bpmUserFactory;
	}
}