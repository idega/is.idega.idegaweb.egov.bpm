package is.idega.idegaweb.egov.bpm.cases.messages;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.identity.BPMUserFactory;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2009/01/22 17:29:22 $ by $Author: anton $
 */
@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class CaseUserFactory {
	
	@Autowired private BPMFactory bpmFactory;
	@Autowired private BPMUserFactory bpmUserFactory;
	
	public CaseUserImpl getCaseUser(User user, ProcessInstanceW piw) {
		
//		TODO: use some caching or smth
		return createCaseUser(user, piw);
	}
	
	private CaseUserImpl createCaseUser(User user, ProcessInstanceW piw) {
		
		CaseUserImpl caseUser = new CaseUserImpl(user, piw);
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