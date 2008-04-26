package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.idega.jbpm.identity.RolesManager;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/04/26 02:32:11 $ by $Author: civilis $
 */
@Service(SendCaseMessagesHandlerBean.beanIdentifier)
public class SendCaseMessagesHandlerBean {

	public static final String beanIdentifier = "egovBPM_SendCaseMessagesHandlerBean";
	private RolesManager rolesManager;
	
	public Collection<User> getUsersToSendMessageTo(IWContext iwc, String rolesNamesAggr, ProcessInstance pi) {
		
		Collection<User> allUsers;
		
		if(rolesNamesAggr != null) {
		
			String[] rolesNames = rolesNamesAggr.trim().split(CoreConstants.SPACE);
			
			HashSet<String> rolesNamesSet = new HashSet<String>(rolesNames.length);
			
			for (int i = 0; i < rolesNames.length; i++)
				rolesNamesSet.add(rolesNames[i]);
			
			allUsers = getRolesManager().getAllUsersForRoles(rolesNamesSet, pi);
		} else
			allUsers = new ArrayList<User>(0);
		
		return allUsers;
			
//			try {
//				UserBusiness userBusiness = getUserBusiness(iwc);
//				
//				for (List<NativeIdentityBind> binds : identities.values()) {
//					
//					for (NativeIdentityBind identity : binds) {
//						
//						if(identity.getIdentityType() == IdentityType.USER) {
//							
//							User user = userBusiness.getUser(new Integer(identity.getIdentityId()));
//							users.put(user.getPrimaryKey().toString(), user);
//							
//						} else if(identity.getIdentityType() == IdentityType.GROUP) {
//							
//							@SuppressWarnings("unchecked")
//							Collection<User> groupUsers = userBusiness.getUsersInGroup(new Integer(identity.getIdentityId()));
//							
//							for (User user : groupUsers)
//								users.put(user.getPrimaryKey().toString(), user);
//						}
//					}
//				}
//				
//			} catch (RemoteException e) {
//				throw new IBORuntimeException(e);
//			}
//		}
//		
//		return users.values();
	}
	
//	protected UserBusiness getUserBusiness(IWContext iwc) {
//		try {
//			return (UserBusiness) IBOLookup.getServiceInstance(iwc, UserBusiness.class);
//		}
//		catch (IBOLookupException ile) {
//			throw new IBORuntimeException(ile);
//		}
//	}

	public RolesManager getRolesManager() {
		return rolesManager;
	}

	@Autowired
	public void setRolesManager(RolesManager rolesManager) {
		this.rolesManager = rolesManager;
	}
}