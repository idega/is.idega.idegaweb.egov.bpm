package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.identity.RolesManager;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/04/14 23:03:46 $ by $Author: civilis $
 */
@Service(SendCaseMessagesHandlerBean.beanIdentifier)
public class SendCaseMessagesHandlerBean {

	public static final String beanIdentifier = "egovBPM_SendCaseMessagesHandlerBean";
	private RolesManager rolesManager;
	
	public Collection<User> getUsersToSendMessageTo(IWContext iwc, String rolesNamesAggr, ProcessInstance pi) {
		
		HashMap<String, User> users = new HashMap<String, User>();
		
		if(rolesNamesAggr != null) {
		
			String[] rolesNames = rolesNamesAggr.trim().split(CoreConstants.SPACE);
			
			HashSet<String> rolesNamesSet = new HashSet<String>(rolesNames.length);
			
			for (int i = 0; i < rolesNames.length; i++)
				rolesNamesSet.add(rolesNames[i]);
			
			Map<String, List<NativeIdentityBind>> identities = getRolesManager().getIdentitiesForRoles(rolesNamesSet, pi.getId());
			
			try {
				UserBusiness userBusiness = getUserBusiness(iwc);
				
				for (List<NativeIdentityBind> binds : identities.values()) {
					
					for (NativeIdentityBind identity : binds) {
						
						if(identity.getIdentityType() == IdentityType.USER) {
							
							User user = userBusiness.getUser(new Integer(identity.getIdentityId()));
							users.put(user.getPrimaryKey().toString(), user);
							
						} else if(identity.getIdentityType() == IdentityType.GROUP) {
							
							@SuppressWarnings("unchecked")
							Collection<User> groupUsers = userBusiness.getUsersInGroup(new Integer(identity.getIdentityId()));
							
							for (User user : groupUsers)
								users.put(user.getPrimaryKey().toString(), user);
						}
					}
				}
				
			} catch (RemoteException e) {
				throw new IBORuntimeException(e);
			}
		}
		
		return users.values();
	}
	
	protected UserBusiness getUserBusiness(IWContext iwc) {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(iwc, UserBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	public RolesManager getRolesManager() {
		return rolesManager;
	}

	@Autowired
	public void setRolesManager(RolesManager rolesManager) {
		this.rolesManager = rolesManager;
	}
}