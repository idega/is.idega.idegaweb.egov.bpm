package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.process.business.messages.MessageValueContext;
import com.idega.jbpm.process.business.messages.MessageValueHandler;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2008/08/05 07:09:17 $ by $Author: civilis $
 */
@Service(SendCaseMessagesHandlerBean.beanIdentifier)
public class SendCaseMessagesHandlerBean {

	public static final String beanIdentifier = "egovBPM_SendCaseMessagesHandlerBean";
	private RolesManager rolesManager;
	private MessageValueHandler messageValueHandler;
	
	
	public String getFormattedMessage(String unformattedMessage, String messageValues, Token tkn, MessageValueContext mvCtx) {
		
		return getMessageValueHandler().getFormattedMessage(unformattedMessage, messageValues, tkn, mvCtx);
	}
	
	public Collection<User> getUsersToSendMessageTo(IWContext iwc, String rolesNamesAggr, ProcessInstance pi) {
		
		Collection<User> allUsers;
		
		if(rolesNamesAggr != null) {
		
			String[] rolesNames = rolesNamesAggr.trim().split(CoreConstants.SPACE);
			
			HashSet<String> rolesNamesSet = new HashSet<String>(rolesNames.length);
			
			for (int i = 0; i < rolesNames.length; i++)
				rolesNamesSet.add(rolesNames[i]);
			
			allUsers = getRolesManager().getAllUsersForRoles(rolesNamesSet, pi.getId());
		} else
			allUsers = new ArrayList<User>(0);
		
		return allUsers;
	}

	public RolesManager getRolesManager() {
		return rolesManager;
	}

	@Autowired
	public void setRolesManager(RolesManager rolesManager) {
		this.rolesManager = rolesManager;
	}
	
	public MessageValueHandler getMessageValueHandler() {
		return messageValueHandler;
	}

	@Autowired
	public void setMessageValueHandler(MessageValueHandler messageValueHandler) {
		this.messageValueHandler = messageValueHandler;
	}
}