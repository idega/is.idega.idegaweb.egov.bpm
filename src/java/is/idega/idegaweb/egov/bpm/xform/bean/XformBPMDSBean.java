package is.idega.idegaweb.egov.bpm.xform.bean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.contact.data.Email;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.IWTimestamp;
import com.idega.util.text.Item;

/**
 * @author <a href="mailto:arunas@idega.com">Arūnas Vasmanas</a>
 * @version $Revision: 1.9 $ Last modified: $Date: 2009/06/04 12:29:55 $ by $Author: valdas $
 */

@Scope("singleton")
@Service("xformBPM")
public class XformBPMDSBean implements XformBPM {

	@Autowired
	private BPMFactory bpmFactory;

	protected BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	protected void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	@Override
	public List<Item> getUsersConnectedToProcess(String pid) {

		List<User> users = getUsersConnectedList(new Long(pid));

		List<Item> usersItem = new ArrayList<Item>();

		for (User user : users) {
			usersItem.add(new Item(user.getId(), user.getName()));
		}

		return usersItem;
	}

	public List<Item> getUsersNamesConnectedToProcess(String pid) {

		List<User> users = getUsersConnectedList(new Long(pid));

		List<Item> usersItem = new ArrayList<Item>();

		for (User user : users) {
			usersItem.add(new Item(user.getName(), user.getName()));
		}

		return usersItem;
	}

	@Override
	public List<Item> getUsersConnectedToProcessEmails(String pid) {

		List<User> users = getUsersConnectedList(new Long(pid));

		List<Item> usersItem = new ArrayList<Item>();

		for (User user : users) {
			usersItem.add(new Item(getUserEmails(user.getEmails()), user
			        .getName()));
		}

		return usersItem;
	}

	private List<User> getUsersConnectedList(Long pid) {

		ProcessInstanceW piw = getProcessInstanceW(pid);

		Collection<User> peopleConnectedToProcess = piw
		        .getUsersConnectedToProcess();

		List<User> uniqueUsers = new ArrayList<User>();

		if (peopleConnectedToProcess != null) {
			for (User user : peopleConnectedToProcess) {
				if (!uniqueUsers.contains(user)) {
					uniqueUsers.add(user);
				}
			}
		}
		return uniqueUsers;
	}

	private String getUserEmails(Collection<Email> emails) {

		StringBuilder userEmails = new StringBuilder();

		for (Email email : emails) {
			userEmails.append(email.getEmailAddress());
		}

		return userEmails.toString();

	}

	@Override
	public List<Item> getProcessAttachments(String pid) {
		long processInstanceId = Long.valueOf(pid);
		ProcessInstanceW piw = getProcessInstanceW(processInstanceId);

		List<Item> attachments = new ArrayList<Item>();

		Locale locale = CoreUtil.getCurrentLocale();
		IWContext iwc = CoreUtil.getIWContext();
		for (BinaryVariable binaryVariable : piw.getAttachments(iwc)) {
			if (binaryVariable.getHidden() == null || binaryVariable.getHidden().equals(false)) {
				TaskInstance ti = getBpmFactory().getTaskInstanceW(binaryVariable.getTaskInstanceId()).getTaskInstance();
				attachments.add(
						new Item(
								binaryVariable.getTaskInstanceId() + ";" + binaryVariable.getHash(),
								binaryVariable.getFileName()  + " - " + new IWTimestamp(ti.getEnd()).getLocaleDateAndTime(locale, IWTimestamp.SHORT, IWTimestamp.SHORT)
						)
				);
			}
		}

		return attachments;
	}

	@Override
	public boolean hasProcessAttachments(String pid) {

		if (pid.equals(CoreConstants.EMPTY)) {
			return Boolean.FALSE;
		}

		long processInstanceId = Long.valueOf(pid);
		ProcessInstanceW piw = getProcessInstanceW(processInstanceId);

		IWContext iwc = CoreUtil.getIWContext();
		return piw.getAttachments(iwc).size() != 0 ? Boolean.TRUE : Boolean.FALSE;
	}

	private ProcessInstanceW getProcessInstanceW(Long pid) {

		ProcessInstanceW piw = getBpmFactory()
		        .getProcessManagerByProcessInstanceId(pid).getProcessInstance(
		            pid);

		return piw;
	}

}
