package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import is.idega.idegaweb.egov.bpm.cases.messages.SendCaseMessagesHandler;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;
import is.idega.idegaweb.egov.cases.data.GeneralCase;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.FinderException;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessWatch;
import com.idega.jbpm.exe.ProcessWatchType;
import com.idega.jbpm.identity.JSONExpHandler;
import com.idega.jbpm.identity.Role;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 * Last modified: $Date: 2008/11/30 08:21:04 $ by $Author: civilis $
 */
@Service("caseHandlerAssignmentHandler")
@Scope("prototype")
public class CaseHandlerAssignmentHandler implements ActionHandler {

	private static final long serialVersionUID = -6642593190563557536L;
	
	private String caseHandlerRoleExp;
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
	@Autowired
	private BPMFactory bpmFactory;
	
	public static final String assignHandlerEventType = 		"handlerAssignedToCase";
	public static final String unassignHandlerEventType = 		"handlerUnassignedFromCase";
	public static final String handlerUserIdVarName = 			"handlerUserId";
	public static final String performerUserIdVarName = 		"performerUserId";
	
	public void execute(ExecutionContext ectx) throws Exception {
		
		ProcessInstance pi = ectx.getProcessInstance();
		Long processInstanceId = pi.getId();
		CaseProcInstBind cpi = getCasesBPMDAO().find(CaseProcInstBind.class, processInstanceId);
		
		String event = ectx.getEvent().getEventType();
		
		Integer caseId = cpi.getCaseId();
		
		try {
			IWApplicationContext iwac = getIWAC();
			CasesBusiness casesBusiness = getCasesBusiness(iwac);
			GeneralCase genCase = casesBusiness.getGeneralCase(caseId);
			
			final Role caseHandlerRole;
			
			if(getCaseHandlerRoleExp() != null) {
				
				caseHandlerRole = JSONExpHandler.resolveRoleFromJSONExpression(getCaseHandlerRoleExp());
			} else {

				String defaultCaseHandlerRoleName = "bpm_caseHandler";
				Logger.getLogger(getClass().getName()).log(Level.INFO, "No caseHandler role expression found, using default="+defaultCaseHandlerRoleName);
				caseHandlerRole = new Role(defaultCaseHandlerRoleName);
			}
			
			if(assignHandlerEventType.equals(event)) {

				unassign(genCase, processInstanceId, casesBusiness, ectx, caseHandlerRole);
				assign(genCase, pi, casesBusiness, ectx, iwac, caseHandlerRole);
				
			} else if(unassignHandlerEventType.equals(event)) {
				
				unassign(genCase, processInstanceId, casesBusiness, ectx, caseHandlerRole);
				
			} else
				throw new IllegalArgumentException("Illegal event type provided="+ectx.getEvent().getEventType());
			
			sendMessages(ectx);
			
		} catch (RemoteException e) {
			throw new IBORuntimeException(e);
		} catch (FinderException e) {
			throw new IBORuntimeException(e);
		}
	}
	
	private void unassign(GeneralCase genCase, Long processInstanceId, CasesBusiness casesBusiness, ExecutionContext ectx, Role caseHandlerRole) throws Exception {
		
		User currentHandler = genCase.getHandledBy();
		
		if(currentHandler != null) {
		
			getProcessWatcher().removeWatch(processInstanceId, new Integer(currentHandler.getPrimaryKey().toString()));
			getBpmFactory().getRolesManager().removeIdentitiesForRoles(Arrays.asList(new Role[] {caseHandlerRole}), currentHandler.getPrimaryKey().toString(), IdentityType.USER, processInstanceId);
		}
		
		casesBusiness.untakeCase(genCase);
	}
	
	private void assign(GeneralCase genCase, ProcessInstance pi, CasesBusiness casesBusiness, ExecutionContext ectx, IWApplicationContext iwac, Role caseHandlerRole) throws Exception {
		
		Integer handlerUserId = (Integer)ectx.getVariable(handlerUserIdVarName);
		Integer performerUserId = (Integer)ectx.getVariable(performerUserIdVarName);
		
//		assigning handler
		User currentHandler = genCase.getHandledBy();
		
		if(currentHandler == null || !String.valueOf(handlerUserId).equals(String.valueOf(currentHandler.getPrimaryKey()))) {
			
			UserBusiness userBusiness = getUserBusiness(iwac);
			
			IWContext iwc = IWContext.getCurrentInstance();
			
			User handler = userBusiness.getUser(handlerUserId);
			User performer = userBusiness.getUser(performerUserId);
			
//			statistics and status change. also keeping handlerId there
			casesBusiness.takeCase(genCase, handler, iwc, performer, true, false);
			
//			the case now appears in handler's mycases list
			getProcessWatcher().assignWatch(pi.getId(), handlerUserId);
		}
		
//		creating case handler role and assigning handler user to this, so that 'ordinary' users could see their contacts etc (they have the permission to see caseHandler contacts)
		List<Role> roles = Arrays.asList(new Role[] {caseHandlerRole});
		getBpmFactory().getRolesManager().createProcessActors(roles, pi);
		getBpmFactory().getRolesManager().createIdentitiesForRoles(roles, handlerUserId.toString(), IdentityType.USER, pi.getId());
	}
	
	private void sendMessages(ExecutionContext ectx) throws Exception {
		
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

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	public String getCaseHandlerRoleExp() {
		return caseHandlerRoleExp;
	}

	public void setCaseHandlerRoleExp(String caseHandlerRoleExp) {
		this.caseHandlerRoleExp = caseHandlerRoleExp;
	}
}