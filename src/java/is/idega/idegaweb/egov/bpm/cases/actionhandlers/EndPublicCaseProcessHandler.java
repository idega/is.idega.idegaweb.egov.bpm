package is.idega.idegaweb.egov.bpm.cases.actionhandlers;


import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.bpm.BPMConstants;
import com.idega.business.IBOLookup;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Service("endPublicCaseProcessHandler")
public class EndPublicCaseProcessHandler extends EndCaseProcessHandler {

	private static final long serialVersionUID = -8580700849037592781L;

	@Override
	protected User getCurrentUser(IWContext iwc, ExecutionContext executionContext) {
		if (iwc != null && iwc.isLoggedOn())
			return iwc.getCurrentUser();

		Object userId = executionContext.getVariable(BPMConstants.USER_ID);
		if (userId == null)
			return null;

		try {
			UserBusiness userBusiness = IBOLookup.getServiceInstance(iwc, UserBusiness.class);
			return userBusiness.getUser(Integer.valueOf(userId.toString()));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

}