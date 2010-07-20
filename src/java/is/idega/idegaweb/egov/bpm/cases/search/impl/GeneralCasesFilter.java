package is.idega.idegaweb.egov.bpm.cases.search.impl;

import is.idega.idegaweb.egov.cases.business.CasesBusiness;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.business.CasesRetrievalManager;
import com.idega.block.process.data.Case;
import com.idega.presentation.IWContext;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GeneralCasesFilter extends DefaultCasesListSearchFilter {

	@Override
	public List<Integer> getSearchResults(List<Integer> casesIds) {
		IWContext iwc = CoreUtil.getIWContext();
		CasesBusiness casesBusiness = getCasesBusiness();
		
		String description = getDescription() == null ? null : getDescription().toLowerCase(iwc.getCurrentLocale());
		
		Collection<Case> cases = null;
		try {
			cases = casesBusiness.getCasesByCriteria(null, description, getName(), getPersonalId(), getStatuses(), getDateFrom(),
					getDateTo(), null, null, false, CasesRetrievalManager.CASE_LIST_TYPE_USER.equals(getCaseListType()));
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		List<Integer> casesByCriteria = null;
		if (cases != null && ListUtil.isEmpty(cases)) {
			getLogger().log(Level.INFO, new StringBuilder("No cases found by criterias: description: ").append(getDescription()).append(", name: ")
					.append(getName()).append(", personalId: ").append(getPersonalId()).append(", statuses: ").append(getStatuses())
					.append(", dateRange: ").append(getDateRange())
			.toString());
		} else {	
			casesByCriteria = getCasesIds(cases);
			getLogger().log(Level.INFO, "Cases by criterias: " + casesByCriteria);
		}
		
		return casesByCriteria;
	}
	
	@Override
	protected String getInfo() {
		return "Looking for cases by criteria: " + getDescription();
	}

	@Override
	protected String getFilterKey() {
		return new StringBuffer().append(getDescription() == null ? CoreConstants.MINUS : getDescription())
								.append(getName() == null ? CoreConstants.MINUS : getName())
								.append(getPersonalId() == null ? CoreConstants.MINUS : getPersonalId())
								.append(getStatuses() == null ? CoreConstants.MINUS : Arrays.asList(getStatuses()))
								.append(getDateFrom() == null ? CoreConstants.MINUS : getDateFrom())
								.append(getDateTo() == null ? CoreConstants.MINUS : getDateTo())
			.toString();
	}

	@Override
	protected boolean isFilterKeyDefined() {
		if (StringUtil.isEmpty(getDescription()) && StringUtil.isEmpty(getName()) && StringUtil.isEmpty(getPersonalId()) &&
				ArrayUtil.isEmpty(getStatuses()) && getDateFrom() == null && getDateTo() == null) {
			getLogger().log(Level.INFO, "None of general criterias (description, name, personal ID, statuses, dates) are defined, not filtering by it!");
			return false;
		}
		return true;
	}
}