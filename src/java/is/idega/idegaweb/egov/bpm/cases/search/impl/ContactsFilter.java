package is.idega.idegaweb.egov.bpm.cases.search.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ContactsFilter extends DefaultCasesListSearchFilter {

	@Override
	public List<Integer> getSearchResults(List<Integer> casesIds) {
		String contact = getContact();
		List<Integer> casesByContact = getCasesByContactQuery(CoreUtil.getIWContext(), contact);	
		if (ListUtil.isEmpty(casesByContact)) {
			getLogger().log(Level.INFO, "No BPM cases found by contact: " + contact);
		} else {
			getLogger().log(Level.INFO, "Found BPM cases by contact: " + contact);
		}
			
		return casesByContact;
	}

	@Override
	protected String getInfo() {
		return "Looking for cases by contact: " + getContact();
	}
	
	private List<Integer> getCasesByContactQuery(IWContext iwc, String contact) {
		if (StringUtil.isEmpty(contact))
			return null;
		
		Collection<User> usersByContactInfo = getUserBusiness().getUsersByNameOrEmailOrPhone(contact);
		if (ListUtil.isEmpty(usersByContactInfo)) {
			return null;
		}

		List<Integer> casesByContactPerson = null;
		final List<Integer> casesByContact = new ArrayList<Integer>();
			
		for (User contactPerson: usersByContactInfo) {
			try {
				casesByContactPerson = getConvertedFromLongs(getCasesBPMDAO().getCaseIdsByProcessInstanceIds(getRolesManager().getProcessInstancesIdsForUser(iwc,
																																contactPerson, false)));
			} catch(Exception e) {
				getLogger().log(Level.SEVERE, "Error getting case IDs from contact query: " + contact, e);
			}
			
			if (!ListUtil.isEmpty(casesByContactPerson)) {
				for (Integer caseId: casesByContactPerson) {
					if (!casesByContact.contains(caseId)) {
						casesByContact.add(caseId);
					}
				}
			}
		}
		
		return casesByContact;
	}
	
	@Override
	protected String getFilterKey() {
		return getContact();
	}

	@Override
	protected boolean isFilterKeyDefined() {
		String contact = getContact();
		if (StringUtil.isEmpty(contact)) {
			getLogger().info("Contact query is not defined, not filtering by it!");
			return false;
		}
		return true;
	}
}