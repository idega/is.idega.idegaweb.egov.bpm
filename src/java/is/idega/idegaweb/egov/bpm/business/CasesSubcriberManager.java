package is.idega.idegaweb.egov.bpm.business;

import com.idega.user.data.User;

public interface CasesSubcriberManager {

	public boolean doEnsureUserIsSubscribed(User user);

}