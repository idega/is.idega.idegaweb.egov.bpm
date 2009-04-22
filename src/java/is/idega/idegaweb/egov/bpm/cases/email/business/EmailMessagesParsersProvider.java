package is.idega.idegaweb.egov.bpm.cases.email.business;

import is.idega.idegaweb.egov.bpm.cases.email.bean.BPMEmailMessage;
import is.idega.idegaweb.egov.bpm.cases.exe.CaseIdentifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.email.bean.MessageParameters;
import com.idega.block.email.business.EmailParser;
import com.idega.block.email.business.EmailsParsersProvider;
import com.idega.block.email.client.business.ApplicationEmailEvent;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.core.messaging.EmailMessage;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.artifacts.presentation.ProcessArtifacts;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.sun.mail.imap.IMAPNestedMessage;

/**
 * Provides e-mails' parsers
 * 
 * @author <a href="mailto:valdas@idega.com">Valdas Žemaitis</a>
 * @version $Revision: 1.3 $ Last modified: $Date: 2009/04/22 14:44:40 $ by $Author: valdas $
 */

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class EmailMessagesParsersProvider implements EmailsParsersProvider {

	private static final Logger LOGGER = Logger.getLogger(EmailMessagesParsersProvider.class.getName());
	
	@Autowired
	private CasesBPMDAO casesBPMDAO;
	
	private EmailParser getAttachedMessagesParser() {
		return new EmailParser() {
			private static final String TEXT_PLAIN_TYPE = "text/plain";
			private static final String MULTIPART_MIXED_TYPE = "multipart/Mixed";
			private static final String TEXT_HTML_TYPE = "text/html";
			private static final String MULTI_ALTERNATIVE_TYPE = "multipart/alternative";
			private static final String MESSAGE_RFC822_TYPE = "message/rfc822";
			
			public List<? extends EmailMessage> getParsedMessages(ApplicationEmailEvent emailEvent) {
				return getParsedMessages(emailEvent.getMessages());
			}
			
			private List<? extends EmailMessage> getParsedMessages(Map<String, List<Message>> messages) {
				if (messages == null || messages.isEmpty()) {
					return null;
				}
				
				List<String> identifiersToResolve = new ArrayList<String>();
				for (String identifier: messages.keySet()) {
					if (!StringUtil.isEmpty(identifier) && identifier.startsWith(CaseIdentifier.IDENTIFIER_PREFIX) && !identifiersToResolve.contains(identifier)) {
						identifiersToResolve.add(identifier);
					}
				}
				if (ListUtil.isEmpty(identifiersToResolve)) {
					return null;
				}
				
				Map<String, Long> identifiers = getResolvedProcessesInstances(identifiersToResolve);
				if (identifiers == null || identifiers.isEmpty()) {
					return null;
				}
				
				List<BPMEmailMessage> parsedMessages = new ArrayList<BPMEmailMessage>();
				
				BPMEmailMessage parsedMessage = null;
				for (Entry<String, List<Message>> messagesEntry: messages.entrySet()) {
					String identifier = messagesEntry.getKey();
					
					if (identifier.startsWith(CaseIdentifier.IDENTIFIER_PREFIX)) {
						for (Message message: messagesEntry.getValue()) {
							parsedMessage = null;
							try {
								parsedMessage = getParsedMessage(message, identifiers.get(identifier));
							} catch(Exception e) {
								LOGGER.log(Level.WARNING, "Error parsing message", e);
							}
							if (parsedMessage != null && !parsedMessages.contains(parsedMessage)) {
								parsedMessages.add(parsedMessage);
							}
						}
					}
				}
				
				return parsedMessages;
			}
			
			private BPMEmailMessage getParsedMessage(Message message, Long processInstanceId) throws Exception {
				if (message == null || processInstanceId == null) {
					return null;
				}
				
				BPMEmailMessage parsedMessage = new BPMEmailMessage(processInstanceId);
				
				parsedMessage.setSubject(message.getSubject());
				
				Object[] msgAndAttachments = parseContent(message);
				
				String body = (String) msgAndAttachments[0];
				if (body == null)
					body = CoreConstants.EMPTY;
				parsedMessage.setBody(body);
				
				Address[] froms = message.getFrom();
				String senderName = null;
				String fromAddress = null;
				for (Address address : froms) {
					if (address instanceof InternetAddress) {
						InternetAddress iaddr = (InternetAddress) address;
						fromAddress = iaddr.getAddress();
						senderName = iaddr.getPersonal();
						break;
					}
				}
				parsedMessage.setSenderName(senderName);
				parsedMessage.setFromAddress(fromAddress);
				
				@SuppressWarnings("unchecked")
				Map<String, InputStream> files = (Map<String, InputStream>) msgAndAttachments[1];
				parsedMessage.setAttachments(files);
				
				return parsedMessage;
			}
			
			private Object[] parseContent(Message msg) {
				
				String messageTxt = "";
				
				Object[] msgAndAttachments = new Object[2];
				try {
					Object content = msg.getContent();
					Map<String, InputStream> attachemntMap = new HashMap<String, InputStream>();
					msgAndAttachments[1] = attachemntMap;
					msgAndAttachments[0] = messageTxt;
					if (msg.isMimeType(TEXT_PLAIN_TYPE)) {
						
						if (content instanceof String)
							msgAndAttachments[0] = parsePlainTextMessage((String) content);
						
					} else if (msg.isMimeType(TEXT_HTML_TYPE)) {
						
						if (content instanceof String)
							msgAndAttachments[0] = parseHTMLMessage((String) content);
						
					} else if (msg.isMimeType(MULTIPART_MIXED_TYPE)) {
						msgAndAttachments = parseMultipart((Multipart) content);
					} else if (msg.isMimeType(MULTI_ALTERNATIVE_TYPE)) {
						msgAndAttachments[0] = parseMultipartAlternative((MimeMultipart) msg.getContent());
					} else if (msg.isMimeType(MESSAGE_RFC822_TYPE)) {
						IMAPNestedMessage nestedMessage = (IMAPNestedMessage) msg.getContent();
						msgAndAttachments = parseRFC822(nestedMessage);
					}
					
				} catch (MessagingException e) {
					LOGGER.log(Level.SEVERE, "Exception while resolving content text from email msg", e);
				} catch (IOException e) {
					LOGGER.log(Level.SEVERE, "Exception while resolving content text from email msg", e);
				}
				return msgAndAttachments;
			}
			
			@SuppressWarnings("unchecked")
			private Object[] parseMultipart(Multipart messageMultipart) throws MessagingException, IOException {
				
				String msg = "";
				Object[] msgAndAttachements = new Object[2];
				Map<String, InputStream> attachemntMap = new HashMap<String, InputStream>();
				msgAndAttachements[1] = attachemntMap;
				for (int i = 0; i < messageMultipart.getCount(); i++) {
					
					Part messagePart = messageMultipart.getBodyPart(i);
					String disposition = messagePart.getDisposition();
					// it is attachment
					if ((disposition != null)
					        && (!messagePart.isMimeType(MESSAGE_RFC822_TYPE))
					        && ((disposition.equals(Part.ATTACHMENT) || disposition
					                .equals(Part.INLINE)))) {
						
						InputStream input = messagePart.getInputStream();
						
						String fileName = messagePart.getFileName();
						if (fileName != null) {
							fileName = MimeUtility.decodeText(fileName);
						} else if (messagePart.getContentType().indexOf("name*=") != -1) {
							// When attachments send from evolution mail client,
							// there is errors so we do what we can.
							fileName = messagePart.getContentType().substring(
							    messagePart.getContentType().indexOf("name*=") + 6);
							// maybe we are lucky to decode it, if not, well
							// better something then nothing.
							fileName = MimeUtility.decodeText(fileName);
							
						} else {
							// well not much can be done then can it?:)
							fileName = "UnknownFile";
						}
						attachemntMap.put(fileName, input);
						// It's a message body
					} else if (messagePart.getContent() instanceof String) {
						if (messagePart.isMimeType(TEXT_HTML_TYPE))
							msg += parseHTMLMessage((String) messagePart.getContent());
						// it's plain text
						else
							msg += (String) messagePart.getContent();
						
						// "multipart/Mixed" can have multipart/alternative sub
						// type.
					} else if (messagePart.getContent() instanceof MimeMultipart
					        && messagePart.isMimeType(MULTI_ALTERNATIVE_TYPE)) {
						msg += parseMultipartAlternative((MimeMultipart) messagePart
						        .getContent());
					} else if (messagePart.isMimeType(MESSAGE_RFC822_TYPE)) {
						IMAPNestedMessage nestedMessage = (IMAPNestedMessage) messagePart
						        .getContent();
						
						Object[] parsedMsg = parseRFC822(nestedMessage);
						
						msg += parsedMsg[0];
						attachemntMap.putAll((Map) parsedMsg[1]);
						
					}
				}
				msgAndAttachements[0] = msg;
				return msgAndAttachements;
			}
			
			@SuppressWarnings("unchecked")
			private Object[] parseRFC822(IMAPNestedMessage part) throws MessagingException, IOException {
				
				String msg = "";
				
				Object[] msgAndAttachements = new Object[2];
				Map<String, InputStream> attachemntMap = new HashMap<String, InputStream>();
				msgAndAttachements[1] = attachemntMap;
				
				if (part.isMimeType(TEXT_PLAIN_TYPE)) {
					
					if (part.getContent() instanceof String)
						msg += parsePlainTextMessage((String) part.getContent());
					
					msgAndAttachements[0] = msg;
				} else if (part.isMimeType(TEXT_HTML_TYPE)) {
					
					if (part.getContent() instanceof String)
						msg += parseHTMLMessage((String) part.getContent());
					
					msgAndAttachements[0] = msg;
				} else if (part.isMimeType(MULTIPART_MIXED_TYPE)) {
					
					msgAndAttachements = parseMultipart((Multipart) part.getContent());
				} else if (part.isMimeType(MULTI_ALTERNATIVE_TYPE)) {
					msg += parseMultipartAlternative((MimeMultipart) part.getContent());
					msgAndAttachements[0] = msg;
				} else if (part.isMimeType(MESSAGE_RFC822_TYPE)) {
					IMAPNestedMessage nestedMessage = (IMAPNestedMessage) part
					        .getContent();
					
					Object[] parsedMsg = parseRFC822(nestedMessage);
					msg += parsedMsg[0];
					
					attachemntMap.putAll((Map) parsedMsg[1]);
				}
				
				return msgAndAttachements;
			}
			
			private String parseMultipartAlternative(MimeMultipart multipart) throws MessagingException, IOException {
				
				String returnStr = null;
				for (int i = 0; i < multipart.getCount(); i++) {
					Part part = multipart.getBodyPart(i);
					if (part.isMimeType(TEXT_HTML_TYPE)) {
						return parseHTMLMessage((String) part.getContent());
					} else if (part.isMimeType(TEXT_PLAIN_TYPE)) {
						returnStr = parsePlainTextMessage((String) part.getContent());
					}
				}
				
				return returnStr;
			}
			
			private String parseHTMLMessage(String message) {
				return message;// "<[!CDATA ["+ message+"]]>";
			}
			
			private String parsePlainTextMessage(String message) {
				String msgWithEscapedHTMLChars = StringUtil.escapeHTMLSpecialChars(message);
				// replacing all new line characktes to <br/> so it will
				// be displayed in html as it should
				return msgWithEscapedHTMLChars.replaceAll("\n", "<br/>");
			}
			
			private Map<String, Long> getResolvedProcessesInstances(List<String> identifiers) {
				if (ListUtil.isEmpty(identifiers)) {
					return null;
				}
				
				List<Object[]> cps = getCasesBPMDAO().getCaseProcInstBindProcessInstanceByCaseIdentifier(identifiers);
				if (ListUtil.isEmpty(cps)) {
					return null;
				}
				
				Map<String, Long> resolvedIdentifiers = new HashMap<String, Long>(cps.size());
				for (Object[] objects : cps) {
					CaseProcInstBind cp = (CaseProcInstBind) objects[0];
					ProcessInstance pi = (ProcessInstance) objects[1];
					
					resolvedIdentifiers.put(cp.getCaseIdentifier(), pi.getId());
				}
				
				return resolvedIdentifiers;
			}
		};	
	}
	
	private EmailParser getMessagesAttributesParser() {
		return new EmailParser() {

			public List<? extends EmailMessage> getParsedMessages(ApplicationEmailEvent emailEvent) {
				return getParsedMessages(emailEvent.getParameters());
			}
			
			private List<? extends EmailMessage> getParsedMessages(MessageParameters message) {
				if (message == null || ListUtil.isEmpty(message.getProperties())) {
					return null;
				}
				
				Long processInstanceId = getValue(message.getProperties(), ProcessArtifacts.PROCESS_INSTANCE_ID_PARAMETER);
				if (processInstanceId == null) {
					return null;
				}
				Long taskInstanceId = getValue(message.getProperties(), ProcessArtifacts.TASK_INSTANCE_ID_PARAMETER);
				
				BPMEmailMessage bpmMessage = new BPMEmailMessage(processInstanceId, taskInstanceId);
				
				bpmMessage.setSenderName(message.getSenderName());
				bpmMessage.setFromAddress(message.getFrom());
				
				bpmMessage.setReplyToAddress(message.getReplyTo());
				
				bpmMessage.setCcAddress(message.getRecipientCc());
				bpmMessage.setBccAddress(message.getRecipientBcc());
				
				bpmMessage.setSubject(message.getSubject());
				bpmMessage.setBody(message.getMessage());
				
				bpmMessage.setAttachedFile(message.getAttachment());
				
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
			
		};
	}
	
	public List<EmailParser> getAllParsers() {
		return Arrays.asList(
				getAttachedMessagesParser(),
				getMessagesAttributesParser()
		);
	}

	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}
	
}
