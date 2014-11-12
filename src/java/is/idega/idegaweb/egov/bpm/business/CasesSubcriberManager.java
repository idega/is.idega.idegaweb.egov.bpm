package is.idega.idegaweb.egov.bpm.business;

import java.util.List;

import com.idega.block.process.presentation.beans.CasesSearchCriteriaBean;
import com.idega.user.data.User;

public interface CasesSubcriberManager {

	public boolean doEnsureUserIsSubscribed(User user);

	public List<Integer> getSuperHandlersGroups(User user);

	public List<Integer> getSubscribedCasesByQuery(User user, CasesSearchCriteriaBean criterias);

}