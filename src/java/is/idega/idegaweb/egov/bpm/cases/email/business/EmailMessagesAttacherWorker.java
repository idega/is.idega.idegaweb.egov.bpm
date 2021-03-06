package is.idega.idegaweb.egov.bpm.cases.email.business;

import is.idega.idegaweb.egov.bpm.cases.email.bean.BPMEmailMessage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.context.exe.VariableInstance;
import org.jbpm.context.exe.variableinstance.StringInstance;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.idega.block.email.bean.FoundMessagesInfo;
import com.idega.block.email.bean.MessageParserType;
import com.idega.block.email.business.EmailsParsersProvider;
import com.idega.block.email.client.business.ApplicationEmailEvent;
import com.idega.block.email.client.business.EmailParams;
import com.idega.block.email.parser.EmailParser;
import com.idega.block.process.variables.Variable;
import com.idega.block.process.variables.VariableDataType;
import com.idega.bpm.BPMConstants;
import com.idega.core.converter.util.StringConverterUtility;
import com.idega.core.messaging.EmailMessage;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWMainApplicationSettings;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewSubmission;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.FileUtil;
import com.idega.util.IOUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.expression.ELUtil;

/**
 * Resolves messages to attach and attaches
 *
 * @author <a href="mailto:valdas@idega.com">Valdas Žemaitis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/04/22 12:56:21 $ by $Author: valdas $
 */

@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class EmailMessagesAttacherWorker implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(EmailMessagesAttacherWorker.class.getName());

	private static final String FETCH_EMAILS_TASK_NAME = "Email";

	@Autowired
	private BPMContext idegaJbpmContext;
	@Autowired
	private BPMFactory bpmFactory;

	private ApplicationEmailEvent emailEvent;

	public EmailMessagesAttacherWorker(ApplicationEmailEvent emailEvent) {
		this.emailEvent = emailEvent;

		ELUtil.getInstance().autowire(this);
	}

	@Override
	public void run() {
		parseAndAttachMessages();
	}

	private void parseAndAttachMessages() {
		Map<String, FoundMessagesInfo> messagesToParse = new HashMap<String, FoundMessagesInfo>();

		Map<String, FoundMessagesInfo> messagesInfo = emailEvent.getMessages();
		if (messagesInfo != null && !messagesInfo.isEmpty()) {
			for (Entry<String, FoundMessagesInfo> entry: messagesInfo.entrySet()) {
				if (entry.getValue().getParserType() == MessageParserType.BPM) {
					messagesToParse.put(entry.getKey(), entry.getValue());
				}
			}
		}

		Map<?, ?> parsersProviders = null;
		try {
			parsersProviders = WebApplicationContextUtils.getWebApplicationContext(IWMainApplication.getDefaultIWMainApplication()
					.getServletContext()).getBeansOfType(EmailsParsersProvider.class);
		} catch(BeansException e) {
			LOGGER.log(Level.SEVERE, "Error getting beans of type: " + EmailsParsersProvider.class, e);
		}
		if (parsersProviders == null || parsersProviders.isEmpty()) {
			return;
		}

		if (!IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("bpm.attach_emails_to_case", Boolean.TRUE)) {
			return;
		}

		EmailParams params = emailEvent.getEmailParams();

		Collection<BPMEmailMessage> allParsedMessages = new ArrayList<BPMEmailMessage>();
		for (Object bean: parsersProviders.values()) {
			if (bean instanceof EmailsParsersProvider) {
				for (EmailParser parser: ((EmailsParsersProvider) bean).getAllParsers()) {
					Collection<? extends EmailMessage> parsedMessages = parser.getParsedMessagesCollection(messagesToParse, params);
					addParsedMessages(allParsedMessages, parsedMessages);

					parsedMessages = parser.getParsedMessages(emailEvent);
					addParsedMessages(allParsedMessages, parsedMessages);
				}
			}
		}

		if (ListUtil.isEmpty(allParsedMessages)) {
			LOGGER.info("No emails were parsed");
			return;
		}

		for (final BPMEmailMessage message: allParsedMessages) {
			try {
				if (!doAttachMessageIfNeeded(message)) {
					//	TODO: save message and try attach later?
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error attaching message " + message, e);
			}
		}
	}

	private void addParsedMessages(Collection<BPMEmailMessage> allParsedMessages, Collection<? extends EmailMessage> parsedMessages) {
		if (ListUtil.isEmpty(parsedMessages))
			return;

		for (EmailMessage message: parsedMessages) {
			if (message instanceof BPMEmailMessage && !allParsedMessages.contains(message)) {
				allParsedMessages.add((BPMEmailMessage) message);
			}
		}
	}

	private Map<Long, Map<Long, Map<String, String>>> getEmailsValues(List<Long> subProcInstIds, List<String> names) {
		if (ListUtil.isEmpty(subProcInstIds) || ListUtil.isEmpty(names)) {
			return null;
		}

		//	Proc. inst. ID -> task inst. ID -> name -> value
		Map<Long, Map<Long, Map<String, String>>> results = new HashMap<Long, Map<Long, Map<String, String>>>();
		for (Long subProcInstId: subProcInstIds) {
			List<Long> tiIds = getBpmFactory().getBPMDAO().getIdsOfFinishedTaskInstancesForTask(subProcInstId, FETCH_EMAILS_TASK_NAME);
			if (ListUtil.isEmpty(tiIds)) {
				continue;
			}

			Map<Long, Map<String, String>> dataForSubProcInst = new HashMap<Long, Map<String, String>>();
			for (Long tiId: tiIds) {
				for (String name: names) {
					String value = getVariableValue(tiId, name);
					if (StringUtil.isEmpty(value)) {
						continue;
					}

					Map<String, String> taskData = dataForSubProcInst.get(tiId);
					if (taskData == null) {
						taskData = new HashMap<String, String>();
						dataForSubProcInst.put(tiId, taskData);
					}
					taskData.put(name, value);
				}
			}

			if (MapUtil.isEmpty(dataForSubProcInst)) {
				continue;
			}

			results.put(subProcInstId, dataForSubProcInst);
		}

		return results;
	}

	private String getVariableValue(final Long tiId, final String name) {
		return getIdegaJbpmContext().execute(new JbpmCallback() {
			@Override
			public String doInJbpm(JbpmContext context) throws JbpmException {
				TaskInstance ti = context.getTaskInstance(tiId);
				Object value = ti.getVariableLocally(name);
				if (value instanceof String) {
					return (String) value;
				}

				value = ti.getVariable(name);
				if (value instanceof String) {
					return (String) value;
				}

				VariableInstance variable = ti.getVariableInstance(name);
				if (variable instanceof StringInstance) {
					return (String) ((StringInstance) variable).getValue();
				}

				return null;
			}
		});
	}

	public boolean doAttachMessageIfNeeded(BPMEmailMessage message) {
		if (message == null || message.isParsed()) {
			return true;
		}

		Long procInstId = message.getProcessInstanceId();
		if (procInstId == null) {
			LOGGER.warning("Proc. inst. ID is unknown for message " + message);
			return false;
		}

		IWMainApplicationSettings settings = IWMainApplication.getDefaultIWMainApplication().getSettings();

		List<Long> subProcInstIds = null;
		if (settings.getBoolean("bpm.email_find_all_sub_proc", Boolean.TRUE)) {
			ProcessInstanceW piW = getBpmFactory().getProcessInstanceW(procInstId);
			subProcInstIds = piW.getIdsOfSubProcesses(procInstId);
		} else {
			subProcInstIds = getBpmFactory().getBPMDAO().getSubProcInstIdsByParentProcInstIdAndProcDefName(
					procInstId,
					EmailMessagesAttacher.email_fetch_process_name
			);
		}
		if (ListUtil.isEmpty(subProcInstIds)) {
			LOGGER.warning("No sub-proc. inst. IDs were found for proc. inst. ID " + procInstId + " and proc. def. name: " + EmailMessagesAttacher.email_fetch_process_name + ". Can not verify if message (" + message +
					") is already attached");
			return false;
		}

		String subject = message.getSubject();
		String text = message.getBody();
		if (text == null) {
			text = CoreConstants.EMPTY;
		}
		String senderPersonalName = message.getSenderName();
		String fromAddress = message.getFromAddress();

		//	Proc. inst. ID -> task inst. ID -> variable name -> variable value
		Map<Long, Map<Long, Map<String, String>>> groupedVars = getEmailsValues(
				subProcInstIds,
				Arrays.asList(BPMConstants.VAR_SUBJECT, BPMConstants.VAR_TEXT, BPMConstants.VAR_FROM, BPMConstants.VAR_FROM_ADDRESS)
		);

		boolean foundExisting = false;
		if (!MapUtil.isEmpty(groupedVars)) {
			//	Checking if current message is not attached already

			Map<String, Boolean> 	subjectsComparisons = new HashMap<String, Boolean>(),
					fromComparisons = new HashMap<String, Boolean>(),
					addressesComparisons = new HashMap<String, Boolean>();

			String[] patterns = settings.getProperty("bpm.emails_cont_rep_patt", "…" + CoreConstants.COMMA + "¿").split(CoreConstants.COMMA);
			String[] encodedPatterns = settings.getProperty("bpm.emails_cont_rep_enc_patt", "u2026" + CoreConstants.COMMA + "u00BF").split(CoreConstants.COMMA);
			boolean printComparison = settings.getBoolean("bpm.emails_print_comparison", Boolean.FALSE);

			try {
				for (Iterator<Long> subProcInstIdsIter = groupedVars.keySet().iterator(); (subProcInstIdsIter.hasNext() && !foundExisting);) {
					Long subProcInstId = subProcInstIdsIter.next();
					Map<Long, Map<String, String>> tasksVariables = groupedVars.get(subProcInstId);
					if (MapUtil.isEmpty(tasksVariables)) {
						continue;
					}

					for (Iterator<Long> taskInstIdsIter = tasksVariables.keySet().iterator(); (taskInstIdsIter.hasNext() && !foundExisting);) {
						Long tiId = taskInstIdsIter.next();
						Map<String, String> existingValues = tasksVariables.get(tiId);
						if (MapUtil.isEmpty(existingValues)) {
							continue;
						}

						String subjectVarValue = existingValues.get(BPMConstants.VAR_SUBJECT);
						String textVarValue = existingValues.get(BPMConstants.VAR_TEXT);
						String fromVarValue = existingValues.get(BPMConstants.VAR_FROM);
						String fromAddressVarValue = existingValues.get(BPMConstants.VAR_FROM_ADDRESS);

						boolean subjectsMatch = !StringUtil.isEmpty(subjectVarValue) && subject.equals(subjectVarValue);
						boolean textsMatch = !StringUtil.isEmpty(textVarValue) && text.equals(textVarValue);
						boolean fromMatch = (fromVarValue == null && senderPersonalName == null) ||
								(!StringUtil.isEmpty(fromVarValue) && !StringUtil.isEmpty(senderPersonalName) && senderPersonalName.equals(fromVarValue));
						boolean addressesMatch = !StringUtil.isEmpty(fromAddressVarValue) && fromAddress.equals(fromAddressVarValue);
						if (subjectsMatch && textsMatch && fromMatch && addressesMatch) {
							message.setParsed(true);
							foundExisting = true;
							return true;
						} else {
							subjectsComparisons.put(tiId + "_:_ '" + subjectVarValue + "'", subjectsMatch);
							if (fromVarValue != null) {
								fromComparisons.put(tiId + "_:_ '" + fromVarValue + "'", fromMatch);
							}
							addressesComparisons.put(tiId + "_:_ '" + fromAddressVarValue + "'", addressesMatch);

							if (subjectsMatch && fromMatch && addressesMatch) {
								LOGGER.info("Will compare texts again for '" + subject + "', because all other fields match. Proc. inst. ID: " +
										procInstId + ", sub-proc. inst. ID: " + subProcInstId + ", task inst. ID: " + tiId);
								//	Will write texts to files and will compare content of these files
								try {
									File dir = new File(System.getProperty("java.io.tmpdir") + File.separator + "bpm_emails");
									if (!dir.exists()) {
										dir.mkdir();
									}

									String toAttachName = "to_attach_" + subject + "_" + tiId + ".txt";
									toAttachName = StringHandler.removeWhiteSpace(toAttachName);
									File toAttach = new File(dir.getAbsolutePath() + File.separator + toAttachName);
									if (!toAttach.exists()) {
										toAttach.createNewFile();
									}
									FileUtil.streamToFile(StringHandler.getStreamFromString(text), toAttach);

									String toCompareName = "to_compare_" + subjectVarValue + "_" + tiId + ".txt";
									toCompareName = StringHandler.removeWhiteSpace(toCompareName);
									File toCompare = new File(dir.getAbsolutePath() + File.separator + toCompareName);
									if (!toCompare.exists()) {
										toCompare.createNewFile();
									}
									FileUtil.streamToFile(StringHandler.getStreamFromString(textVarValue), toCompare);

									textsMatch = isContentOfFilesEqual(toAttach, toCompare, patterns, encodedPatterns, subject, printComparison);
									if (textsMatch) {
										message.setParsed(true);
										foundExisting = true;
										return true;
									} else {
										String msg = "Wrote files " + toAttach.getName() + " and " + toCompare.getName() + " to " + dir.getAbsolutePath() +
												". Content is not the same of these files while subject ('" + subject +
												"'), sender address and name are the same. " +
												"Proc. inst. ID: " + procInstId + ", sub-proc. inst. ID: " + subProcInstId + ", task inst. ID: " + tiId;
										LOGGER.warning(msg);
										if (settings.getBoolean("bpm.email_send_comparisons", false)) {
											CoreUtil.sendExceptionNotification(msg, null, toAttach, toCompare);
										}
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
					}
				}
			} finally {
				if (foundExisting) {
					message.setParsed(true);
					return true;
				} else {
					LOGGER.info("Email with subject '" + subject + "' (comparisons:\n" + subjectsComparisons + ")\nsender: '" + senderPersonalName +
							"' (comparisons:\n" + fromComparisons + ")\nfrom: '" + fromAddress + "' (comparisons:\n" + addressesComparisons +
							")\n is not attached to proc. inst. ID: " + procInstId + ", sub-proc. inst IDs: " + subProcInstIds + ", need to attach it");
				}
			}
		}

		if (foundExisting) {
			message.setParsed(true);
			return true;
		}

		List<Long> fetchEmailsSubProcInstIds = getBpmFactory().getBPMDAO().getSubProcInstIdsByParentProcInstIdAndProcDefName(
				procInstId,
				EmailMessagesAttacher.email_fetch_process_name
		);
		if (ListUtil.isEmpty(fetchEmailsSubProcInstIds)) {
			LOGGER.warning("No sub-proc. inst. IDs were found for proc. inst. ID " + procInstId + " and proc. def. name: " + EmailMessagesAttacher.email_fetch_process_name + ". Do not know where to attach message " + message);
			return false;
		}

		Map<String, InputStream> attachments = message.getAttachments();
		if (attachments == null) {
			attachments = new HashMap<String, InputStream>(1);
		}
		boolean result = doAttachEmailToProcess(
				fetchEmailsSubProcInstIds.get(0),
				subject,
				text,
				senderPersonalName,
				fromAddress,
				attachments,
				message.getAttachedFiles()
		);
		message.setParsed(result);
		return result;
	}

	private boolean isContentOfLinesEqual(String identifier, List<String> lines1, List<String> lines2, boolean printComparison) {
		if (ListUtil.isEmpty(lines1)) {
			LOGGER.warning("Lines1 are not provided");
			return false;
		}
		if (ListUtil.isEmpty(lines2)) {
			LOGGER.warning("Lines2 are not provided");
			return false;
		}

		boolean numberOfLinesIsTheSame = lines1.size() == lines2.size();
		if (!numberOfLinesIsTheSame) {
			LOGGER.warning("Number of lines is not the same: lines1: " + lines1.size() + " vs. lines2: " + lines2.size());
			return false;
		}

		for (int i = 0; i < lines1.size(); i++) {
			String line1 = lines1.get(i);
			String line2 = lines2.get(i);
			if (!line1.equals(line2)) {
				if (printComparison) {
					LOGGER.warning("Line number: " + i + ": 'Line 1' (length: " + line1.length() + "):\n'" + line1+ "'\n'Line 2' (length: " + line2.length() +
						"):\n'" + line2 + "'\n. They are not equal. Identifier: " + identifier);
				}
				return false;
			}
		}
		return true;
	}

	private boolean isContentOfFilesEqual(File file1, File file2, String[] patterns, String[] encodedPatterns, String identifier, boolean printComparison) {
		boolean sameContent = false;
		BufferedReader bfr1 = null, bfr2 = null;
		List<String> content1 = null, content2 = null;
		try {
			bfr1 = new BufferedReader(new FileReader(file1));

			String content = null;
			content1 = new ArrayList<String>();
			while ((content = bfr1.readLine()) != null) {
				content = getRidOfInvalidSymbols(content, patterns, encodedPatterns);
				content1.add(content);
			}

			bfr2 = new BufferedReader(new FileReader(file2));
			content2 = new ArrayList<String>();
			while ((content = bfr2.readLine()) != null) {
				content = getRidOfInvalidSymbols(content, patterns, encodedPatterns);
				content2.add(content);
			}

			sameContent = isContentOfLinesEqual(identifier, content1, content2, printComparison);
			return sameContent;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error while comparing content of files " + file1 + " and " + file2, e);
		} finally {
			if (sameContent) {
				if (file1 != null) {
					file1.delete();
				}
				if (file2 != null) {
					file2.delete();
				}
			} else if (content1 != null && content2 != null && printComparison) {
				LOGGER.warning("Content 1 (lines: " + content1.size() + "):\n" + content1 + "\nis not the same as content 2 (lines: " +
						content2.size() + ")\n" + content2);
			}

			IOUtil.close(bfr1);
			IOUtil.close(bfr2);
		}
		return false;
	}

	private String getRidOfInvalidSymbols(String content, String[] patterns, String[] encodedPatterns) {
		if (content == null || ArrayUtil.isEmpty(patterns)) {
			return content;
		}

		String tmp = content;
		for (String pattern: patterns) {
			content = StringHandler.replace(content, pattern, CoreConstants.EMPTY);
		}
		if (tmp.equals(content)) {
			String encoded = StringConverterUtility.saveConvert(content, false);
			for (String encodedPattern: encodedPatterns) {
				encoded = StringHandler.replace(encoded, CoreConstants.BACK_SLASH + encodedPattern, CoreConstants.EMPTY);
			}
			content = StringConverterUtility.loadConvert(encoded);
		}
		return content;
	}

	@Transactional
	private Boolean doAttachEmailToProcess(
			final Long subProcInstId,
			final String subject,
			final String text,
			final String senderPersonalName,
			final String fromAddress,
			final Map<String, InputStream> attachments,
			final Collection<File> attachedFiles
	) {
		Boolean result = getIdegaJbpmContext().execute(new JbpmCallback() {

			@Override
			public Boolean doInJbpm(JbpmContext context) throws JbpmException {
				try {
					ProcessInstance subPI = context.getProcessInstance(subProcInstId);
					TaskInstance ti = subPI.getTaskMgmtInstance().createStartTaskInstance();
					ti.setName(subject);

					Map<String, Object> newVars = new HashMap<String, Object>(2);
					newVars.put(BPMConstants.VAR_SUBJECT, subject);
					newVars.put(BPMConstants.VAR_TEXT, text);
					newVars.put(BPMConstants.VAR_FROM, senderPersonalName);
					newVars.put(BPMConstants.VAR_FROM_ADDRESS, fromAddress);

					// taking here view for new task instance
					long tiId = ti.getId();
					View view = getBpmFactory().takeView(tiId, false, null);
					if (view == null) {
						return Boolean.FALSE;
					}

					long pdId = ti.getProcessInstance().getProcessDefinition().getId();

					ViewSubmission emailViewSubmission = getBpmFactory().getViewSubmission();
					emailViewSubmission.populateVariables(newVars);

					TaskInstanceW taskInstance = bpmFactory.getProcessManager(pdId).getTaskInstance(ti.getId());
					taskInstance.submit(emailViewSubmission, false);
					LOGGER.info("Task instance ID for email message to attach: " + tiId + "\nSubmitted task instance " +
							taskInstance.getTaskInstanceId() + " with data from email message (sender: " + fromAddress +
							", subject: " + subject + ") for process instance: " + taskInstance.getProcessInstanceW().getProcessInstanceId());

					if (!ListUtil.isEmpty(attachedFiles)) {
						for (File attachedFile: attachedFiles) {
							if (attachedFile != null) {
								attachments.put(attachedFile.getName(), new FileInputStream(attachedFile));
							}
						}
					}
					if (!MapUtil.isEmpty(attachments)) {
						Variable variable = new Variable("attachments", VariableDataType.FILES);

						InputStream fileStream = null;
						for (String fileName: attachments.keySet()) {
							fileStream = attachments.get(fileName);
							try {
								taskInstance.addAttachment(variable, fileName, fileName, fileStream);
							} catch (Exception e) {
								LOGGER.log(Level.SEVERE, "Unable to set binary variable for task instance: " + ti.getId(), e);
							} finally {
								IOUtil.closeInputStream(fileStream);
								if (!ListUtil.isEmpty(attachedFiles)) {
									for (File attachedFile: attachedFiles) {
										attachedFile.delete();
									}
								}
							}
						}
					}

					return true;
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Exception while attaching email msg (subject: " + subject + ", text: " +
							text + "). Sub-process ID: " + subProcInstId, e);
				}
				return false;
			}
		});
		return result;
	}

	public BPMContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	public void setIdegaJbpmContext(BPMContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

}