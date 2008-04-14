package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessConstants;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;
import is.idega.idegaweb.egov.message.business.CommuneMessageBusiness;

import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;

import com.idega.block.process.message.data.Message;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWBundle;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.webface.WFUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/04/14 23:03:46 $ by $Author: civilis $
 */
public class SendCaseMessagesHandler implements ActionHandler {

	private static final long serialVersionUID = 1212382470685233437L;
	
	private String subjectKey;
	private String messageKey;
	private String sendToRoles;
	
	public String getSendToRoles() {
		return sendToRoles;
	}

	public void setSendToRoles(String sendToRoles) {
		this.sendToRoles = sendToRoles;
	}

	public void execute(ExecutionContext ctx) throws Exception {
		
		FacesContext fctx = FacesContext.getCurrentInstance();
		final IWContext iwc = IWContext.getIWContext(fctx);
		final String sendToRoles = getSendToRoles();
		final ProcessInstance pi = ctx.getProcessInstance();
		
		CasesBusiness casesBusiness = getCasesBusiness(iwc);
		String caseIdStr = (String)ctx.getVariable(CasesBPMProcessConstants.caseIdVariableName);
		final GeneralCase theCase = casesBusiness.getGeneralCase(new Integer(caseIdStr));
		final CommuneMessageBusiness messageBusiness = getCommuneMessageBusiness(iwc);
		
		final String subjectKey = getSubjectKey();
		final String msgKey = getMessageKey();
		
		new Thread(new Runnable() {

			public void run() {
				
				SendCaseMessagesHandlerBean bean = (SendCaseMessagesHandlerBean)WFUtil.getBeanInstance(iwc, SendCaseMessagesHandlerBean.beanIdentifier);
				Collection<User> users = bean.getUsersToSendMessageTo(iwc, sendToRoles, pi);
				
				IWBundle bndl = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
				
				final String subject = bndl.getResourceBundle(iwc).getLocalizedString(subjectKey, CoreConstants.EMPTY);
				final String msg = bndl.getResourceBundle(iwc).getLocalizedString(msgKey, CoreConstants.EMPTY);
				
				if(subject != null && msg != null) {
				
					try {
						for (User user : users) {
							
							String mzg = MessageFormat.format(msg, new Object[] {user.getName()});
							
							Message message = messageBusiness.createUserMessage(theCase, user, null, null, subject, mzg, mzg, null, false, null, false, true);
							message.store();
						}
						
					} catch (RemoteException e) {
						Logger.getLogger(SendCaseMessagesHandler.class.getName()).log(Level.SEVERE, "Exception while sending user message, some messages might be not sent", e);
					}
				}
			}
			
		}).start();
	}
	
	protected CommuneMessageBusiness getCommuneMessageBusiness(IWApplicationContext iwac) {
		try {
			return (CommuneMessageBusiness)IBOLookup.getServiceInstance(iwac, CommuneMessageBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	public String getSubjectKey() {
		return subjectKey;
	}

	public void setSubjectKey(String subjectKey) {
		this.subjectKey = subjectKey;
	}

	public String getMessageKey() {
		return messageKey;
	}

	public void setMessageKey(String messageKey) {
		this.messageKey = messageKey;
	}
}