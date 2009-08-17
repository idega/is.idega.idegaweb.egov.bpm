package is.idega.idegaweb.egov.bpm.cases.email.parsers;

import is.idega.idegaweb.egov.bpm.cases.email.bean.BPMEmailMessage;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Message;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.email.bean.FoundMessagesInfo;
import com.idega.block.email.bean.MessageParameters;
import com.idega.block.email.bean.MessageParserType;
import com.idega.block.email.client.business.ApplicationEmailEvent;
import com.idega.block.email.client.business.EmailParams;
import com.idega.block.email.parser.DefaultMessageParser;
import com.idega.block.email.parser.EmailParser;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.core.messaging.EmailMessage;
import com.idega.jbpm.artifacts.presentation.ProcessArtifacts;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class MessageAttributesParser extends DefaultMessageParser implements EmailParser {

	private static final Logger LOGGER = Logger.getLogger(MessageAttributesParser.class.getName());
	
	@Override
	protected EmailMessage getNewMessage() {
		EmailMessage message = new BPMEmailMessage();
		message.setAutoDeletedAttachments(Boolean.FALSE);
		return message;
	}
	
	@Override
	public Collection<? extends EmailMessage> getParsedMessages(ApplicationEmailEvent emailEvent) {
		return getParsedMessages(emailEvent.getParameters());
	}
	
	private Collection<? extends EmailMessage> getParsedMessages(MessageParameters message) {
		if (message == null || ListUtil.isEmpty(message.getProperties())) {
			return null;
		}
		
		Long processInstanceId = getValue(message.getProperties(), ProcessArtifacts.PROCESS_INSTANCE_ID_PARAMETER);
		if (processInstanceId == null) {
			return null;
		}
		Long taskInstanceId = getValue(message.getProperties(), ProcessArtifacts.TASK_INSTANCE_ID_PARAMETER);
		
		BPMEmailMessage bpmMessage = (BPMEmailMessage) getNewMessage();
		bpmMessage.setProcessInstanceId(processInstanceId);
		bpmMessage.setTaskInstanceId(taskInstanceId);
		
		bpmMessage.setSenderName(message.getSenderName());
		bpmMessage.setFromAddress(message.getFrom());
		
		bpmMessage.setReplyToAddress(message.getReplyTo());
		
		bpmMessage.setCcAddress(message.getRecipientCc());
		bpmMessage.setBccAddress(message.getRecipientBcc());
		
		bpmMessage.setSubject(message.getSubject());
		bpmMessage.setBody(message.getMessage());
		
		bpmMessage.addAttachment(message.getAttachment());
		
		return Arrays.asList(bpmMessage);
	}
	
	private Long getValue(List<AdvancedProperty> properties, String name) {
		if (ListUtil.isEmpty(properties) || StringUtil.isEmpty(name)) {
			return null;
		}
		
		Long value = null;
		AdvancedProperty property = null;
		for (Iterator<AdvancedProperty> propertiesIter = properties.iterator(); (propertiesIter.hasNext() && value == null);) {
			property = propertiesIter.next();
			
			if (name.equals(property.getId())) {
				try {
					value = Long.valueOf(property.getValue());
				} catch(Exception e) {
					LOGGER.log(Level.WARNING, "Error getting Long value from: " + property.getValue(), e);
				}
			}
		}
		
		return value;
	}

	public MessageParserType getMessageParserType() {
		return MessageParserType.BPM;
	}
	
	@Override
	public EmailMessage getParsedMessage(Message message, EmailParams parmas) throws Exception {
		return null;
	}
	
	@Override
	public Map<String, Collection<? extends EmailMessage>> getParsedMessages(Map<String, FoundMessagesInfo> messages, EmailParams params) {
		return null;
	}
	
	@Override
	public Collection<? extends EmailMessage> getParsedMessagesCollection(Map<String, FoundMessagesInfo> messages, EmailParams params) {
		return null;
	}
}