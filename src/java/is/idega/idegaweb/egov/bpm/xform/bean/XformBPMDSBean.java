package is.idega.idegaweb.egov.bpm.xform.bean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.contact.data.Email;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.user.data.User;
import com.idega.util.text.Item;

/**
 * @author <a href="mailto:arunas@idega.com">Arūnas Vasmanas</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2009/02/09 11:54:53 $ by $Author: arunas $
 */

@Scope("singleton")
@Service("xformBPM")
public class XformBPMDSBean implements XformBPM{

	@Autowired private BPMFactory bpmFactory;
		
	protected BPMFactory getBpmFactory() {		
		return bpmFactory;
	}

	protected void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
	
	public List<Item> getUsersConnectedToProcess(String pid) {
		
		List<User> users = getUsersConnectedList(new Long(pid));
		
		List<Item> usersItem = new ArrayList<Item>();
		
		for(User user: users) 
			usersItem.add(new Item(user.getId(), user.getName()));
			
		return usersItem;
	}
	
	public List<Item> getUsersNamesConnectedToProcess(String pid) {
		
		List<User> users = getUsersConnectedList(new Long(pid));
		
		List<Item> usersItem = new ArrayList<Item>();
		
		for(User user: users) 
			usersItem.add(new Item(user.getName(), user.getName()));
			
		return usersItem;
	}
	
	@SuppressWarnings("unchecked")
	public List<Item> getUsersConnectedToProcessEmails(String pid) {
		
		List<User> users = getUsersConnectedList(new Long(pid));
		
		List<Item> usersItem = new ArrayList<Item>();
		
		for(User user: users) 
			usersItem.add(new Item(getUserEmails(user.getEmails()),user.getName()));
			
		return usersItem;
	}
	
	private List<User> getUsersConnectedList(Long pid) {
		
		ProcessInstanceW piw = getBpmFactory()
		.getProcessManagerByProcessInstanceId(pid)
		.getProcessInstance(pid);
		
		Collection<User> peopleConnectedToProcess = piw.getUsersConnectedToProcess();
		
		List<User> uniqueUsers = new ArrayList<User>();
		
		if (peopleConnectedToProcess != null) {
			for(User user: peopleConnectedToProcess) {
				if (!uniqueUsers.contains(user)) {
					uniqueUsers.add(user);
				}
			}
		}
		return uniqueUsers;
	}
	
	private String getUserEmails(Collection<Email> emails) {
		
		StringBuilder userEmails = new StringBuilder();
		
		for (Email email : emails) 
			userEmails.append(email.getEmailAddress());
		
		return userEmails.toString();
		
	}

}
