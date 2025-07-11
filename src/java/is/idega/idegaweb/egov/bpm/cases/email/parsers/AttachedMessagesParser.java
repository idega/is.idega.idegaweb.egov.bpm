package is.idega.idegaweb.egov.bpm.cases.email.parsers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Message;

import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.email.bean.FoundMessagesInfo;
import com.idega.block.email.bean.MessageParserType;
import com.idega.block.email.client.business.ApplicationEmailEvent;
import com.idega.block.email.client.business.EmailParams;
import com.idega.block.email.parser.DefaultMessageParser;
import com.idega.block.email.parser.EmailParser;
import com.idega.core.messaging.EmailMessage;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;

import is.idega.idegaweb.egov.bpm.cases.email.bean.BPMEmailMessage;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class AttachedMessagesParser extends DefaultMessageParser implements EmailParser {

	private static final Logger LOGGER = Logger.getLogger(AttachedMessagesParser.class.getName());

	@Autowired
	private CasesBPMDAO casesBPMDAO;

	@Override
	protected EmailMessage getNewMessage() {
		return new BPMEmailMessage();
	}

	@Override
	public Map<String, Collection<? extends EmailMessage>> getParsedMessages(Map<String, FoundMessagesInfo> messagesInfo, EmailParams params) {
		if (messagesInfo == null || messagesInfo.isEmpty()) {
			return null;
		}

		List<String> identifiersToResolve = new ArrayList<String>();
		for (String identifier: messagesInfo.keySet()) {
			if (!StringUtil.isEmpty(identifier) && !identifiersToResolve.contains(identifier)) {
				identifiersToResolve.add(identifier);
			}
		}
		if (ListUtil.isEmpty(identifiersToResolve)) {
			return null;
		}

		Map<String, String> identifiers = getResolvedProcessesInstances(identifiersToResolve);

		if (identifiers == null || identifiers.isEmpty()) {
			return null;
		}

		Map<String, Collection<? extends EmailMessage>> parsedMessages = new HashMap<String, Collection<? extends EmailMessage>>();

		for (Entry<String, FoundMessagesInfo> messagesEntry: messagesInfo.entrySet()) {
			String identifier = messagesEntry.getKey();
			FoundMessagesInfo info = messagesEntry.getValue();
			if (info.getParserType() != getMessageParserType()) {
				continue;
			}

			for (Message message: info.getMessages()) {
				EmailMessage parsedMessage = null;
				try {
					parsedMessage = getParsedMessage(message, params);
				} catch(Exception e) {
					LOGGER.log(Level.WARNING, "Error parsing message", e);
				}
				if (parsedMessage instanceof BPMEmailMessage) {
					BPMEmailMessage bpmMessage = (BPMEmailMessage) parsedMessage;
					String procInstIdentifier = identifiers.get(identifier);
					if (!StringUtil.isEmpty(procInstIdentifier)) {
						if (StringHandler.isNumeric(procInstIdentifier)) {
							bpmMessage.setProcessInstanceId(Long.valueOf(procInstIdentifier));
						} else {
							bpmMessage.setProcessInstanceUUID(procInstIdentifier);
						}
					}

					@SuppressWarnings("unchecked")
					Collection<BPMEmailMessage> messagesByProcess = (Collection<BPMEmailMessage>) parsedMessages.get(identifier);
					if (messagesByProcess == null) {
						messagesByProcess = new ArrayList<BPMEmailMessage>();
						messagesByProcess.add(bpmMessage);
						parsedMessages.put(identifier, messagesByProcess);
					} else {
						messagesByProcess.add(bpmMessage);
					}
				}
			}
		}

		return parsedMessages;
	}

	private Map<String, String> getResolvedProcessesInstances(List<String> identifiers) {
		if (ListUtil.isEmpty(identifiers)) {
			return null;
		}

		Map<String, String> resolvedIdentifiers = new HashMap<>();

		Integer bpmVersion = IWMainApplication.getDefaultIWMainApplication().getSettings().getInt(is.idega.idegaweb.egov.bpm.BPMConstants.APP_PROPERTY_BPM_VERSION, 2);

		if (bpmVersion.intValue() == 1) {
			List<Object[]> cps = getCasesBPMDAO().getCaseProcInstBindProcessInstanceByCaseIdentifier(identifiers);
			if (ListUtil.isEmpty(cps)) {
				return null;
			}

			for (Object[] objects : cps) {
				CaseProcInstBind cp = (CaseProcInstBind) objects[0];
				ProcessInstance pi = (ProcessInstance) objects[1];

				if (pi != null) {
					resolvedIdentifiers.put(cp.getCaseIdentifier(), String.valueOf(pi.getId()));
				}
			}
		} else {
			List<Object[]> cps = getCasesBPMDAO().getCaseProcInstBindProcessInstanceByCaseIdentifierSimple(identifiers);
			if (ListUtil.isEmpty(cps)) {
				return null;
			}

			for (Object[] objects : cps) {
				String caseIdentifier = (String) objects[0];
				//Long processInstanceId = (Long) objects[1];
				String processInstanceUUID = (String) objects[2];

				resolvedIdentifiers.put(caseIdentifier, processInstanceUUID);
			}
		}

		return resolvedIdentifiers;
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}

	@Override
	public MessageParserType getMessageParserType() {
		return MessageParserType.BPM;
	}

	@Override
	public Collection<? extends EmailMessage> getParsedMessages(ApplicationEmailEvent emailEvent) {
		return null;
	}

}