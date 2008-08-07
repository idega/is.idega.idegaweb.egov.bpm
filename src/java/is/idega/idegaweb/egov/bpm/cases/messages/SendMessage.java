package is.idega.idegaweb.egov.bpm.cases.messages;

import is.idega.idegaweb.egov.bpm.cases.messages.SendCaseMessagesHandler.LocalizedMessages;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/08/07 09:38:43 $ by $Author: civilis $
 */
public interface SendMessage {

	public abstract void send(final ProcessInstance pi, final Integer caseId, final LocalizedMessages msgs, final Token tkn, final String sendToRoles);
}