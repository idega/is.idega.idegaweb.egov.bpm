package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import is.idega.idegaweb.egov.bpm.cases.messages.SendCaseMessagesHandler;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;

import java.rmi.RemoteException;
import java.util.Map;

import javax.ejb.FinderException;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.exe.ProcessWatch;
import com.idega.jbpm.exe.ProcessWatchType;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/08/07 09:34:20 $ by $Author: civilis $
 */
public class CaseHandlerAssignmentHandler implements ActionHandler {

	private static final long serialVersionUID = -6642593190563557536L;
	
	private String subjectKey;
	private String subjectValues;
	private String messageKey;
	private String messageValues;
	private String messagesBundle;
	private String sendToRoles;
	private Map<String, String> inlineSubject;
	private Map<String, String> inlineMessage;
	@Autowired
	private CasesBPMDAO casesBPMDAO;
	@Autowired
	@ProcessWatchType("cases")
	private ProcessWatch processWatcher;
	
	public static final String assignHandlerEventType = 		"handlerAssignedToCase";
	public static final String unassignHandlerEventType = 		"handlerUnassignedFromCase";
	public static final String handlerUserIdVarName = 			"handlerUserId";
	public static final String performerUserIdVarName = 		"performerUserId";
	
	public void execute(ExecutionContext ectx) throws Exception {
		
		ELUtil.getInstance().autowire(this);
		
		Long processInstanceId = ectx.getProcessInstance().getId();
		CaseProcInstBind cpi = getCasesBPMDAO().find(CaseProcInstBind.class, processInstanceId);
		
		String event = ectx.getEvent().getEventType();
		Integer handlerUserId = (Integer)ectx.getVariable(handlerUserIdVarName);
		Integer performerUserId = (Integer)ectx.getVariable(performerUserIdVarName);
		Integer caseId = cpi.getCaseId();
		
		try {
			IWApplicationContext iwac = getIWAC();
			CasesBusiness casesBusiness = getCasesBusiness(iwac);
			GeneralCase genCase = casesBusiness.getGeneralCase(caseId);
			
			if(assignHandlerEventType.equals(event)) {
				
				User currentHandler = genCase.getHandledBy();
				
				if(currentHandler == null || !String.valueOf(handlerUserId).equals(String.valueOf(currentHandler.getPrimaryKey()))) {
					
					UserBusiness userBusiness = getUserBusiness(iwac);
					
					IWContext iwc = IWContext.getCurrentInstance();
					
					User handler = userBusiness.getUser(handlerUserId);
					User performer = userBusiness.getUser(performerUserId);
					
					casesBusiness.takeCase(genCase, handler, iwc, performer, true, false);
					
					getProcessWatcher().assignWatch(processInstanceId, handlerUserId);
				}
				
				sendMessages(ectx, true);
				
			} else if(unassignHandlerEventType.equals(event)) {
				
				User currentHandler = genCase.getHandledBy();
				
				if(currentHandler != null) {
				
					getProcessWatcher().removeWatch(processInstanceId, new Integer(currentHandler.getPrimaryKey().toString()));
				}
				
				casesBusiness.untakeCase(genCase);
				
				sendMessages(ectx, false);
				
			} else
				throw new IllegalArgumentException("Illegal event type provided="+ectx.getEvent().getEventType());
			
		} catch (RemoteException e) {
			throw new IBORuntimeException(e);
		} catch (FinderException e) {
			throw new IBORuntimeException(e);
		}
	}
	
	private void sendMessages(ExecutionContext ectx, boolean assigned) throws Exception {
		
		SendCaseMessagesHandler msgHan = new SendCaseMessagesHandler();
		msgHan.setMessageKey(getMessageKey());
		msgHan.setMessagesBundle(getMessagesBundle());
		msgHan.setMessageValues(getMessageValues());
		msgHan.setSendToRoles(getSendToRoles());
		msgHan.setSubjectKey(getSubjectKey());
		msgHan.setSubjectValues(getSubjectValues());
		msgHan.setInlineSubject(getInlineSubject());
		msgHan.setInlineMessage(getInlineMessage());
		
		msgHan.execute(ectx);
	}
	
	protected CasesBusiness getCasesBusiness(IWApplicationContext iwac) {
		try {
			return (CasesBusiness) IBOLookup.getServiceInstance(iwac, CasesBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	protected UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(iwac, UserBusiness.class);
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

	public String getSubjectValues() {
		return subjectValues;
	}

	public void setSubjectValues(String subjectValues) {
		this.subjectValues = subjectValues;
	}

	public String getMessageKey() {
		return messageKey;
	}

	public void setMessageKey(String messageKey) {
		this.messageKey = messageKey;
	}

	public String getMessageValues() {
		return messageValues;
	}

	public void setMessageValues(String messageValues) {
		this.messageValues = messageValues;
	}

	public String getMessagesBundle() {
		return messagesBundle;
	}

	public void setMessagesBundle(String messagesBundle) {
		this.messagesBundle = messagesBundle;
	}

	public String getSendToRoles() {
		return sendToRoles;
	}

	public void setSendToRoles(String sendToRoles) {
		this.sendToRoles = sendToRoles;
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	public ProcessWatch getProcessWatcher() {
		return processWatcher;
	}
	
	private IWApplicationContext getIWAC() {
		
		final IWContext iwc = IWContext.getCurrentInstance();
		final IWApplicationContext iwac;
//		trying to get iwma from iwc, if available, downgrading to default iwma, if not
		
		if(iwc != null) {
			
			iwac = iwc;
			
		} else {
			iwac = IWMainApplication.getDefaultIWApplicationContext();
		}
		
		return iwac;
	}

	public Map<String, String> getInlineSubject() {
		return inlineSubject;
	}

	public void setInlineSubject(Map<String, String> inlineSubject) {
		this.inlineSubject = inlineSubject;
	}

	public Map<String, String> getInlineMessage() {
		return inlineMessage;
	}

	public void setInlineMessage(Map<String, String> inlineMessage) {
		this.inlineMessage = inlineMessage;
	}
}