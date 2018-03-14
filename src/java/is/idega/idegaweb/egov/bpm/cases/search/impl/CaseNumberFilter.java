package is.idega.idegaweb.egov.bpm.cases.search.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.ejb.FinderException;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.business.CasesRetrievalManager;
import com.idega.block.process.data.Case;
import com.idega.block.process.data.CaseHome;
import com.idega.data.IDOLookup;
import com.idega.data.IDOLookupException;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

import is.idega.idegaweb.egov.cases.data.GeneralCase;
import is.idega.idegaweb.egov.cases.data.GeneralCaseHome;

@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CaseNumberFilter extends DefaultCasesListSearchFilter {

	@Override
	public List<Integer> getSearchResults(List<Integer> casesIds) {
		String caseNumber = getCaseNumber();
		String loweredCaseNumber = caseNumber.toLowerCase(CoreUtil.getIWContext().getCurrentLocale());
		Set<Integer> casesByNumberIds = new HashSet<Integer>();

		//	"BPM" cases
		List<Long> bpmCases = null;
		try {
			bpmCases = getCasesBPMDAO().getCaseIdsByCaseNumber(loweredCaseNumber);
		} catch(Exception e) {
			getLogger().log(Level.WARNING, "Exception while resolving case ids by case number = " + loweredCaseNumber, e);
		}
		if (ListUtil.isEmpty(bpmCases)) {
			getLogger().info("No BPM cases found by number: " + caseNumber);
		} else {
			getLogger().info("BPM cases by number '" + caseNumber + "': " + bpmCases);
			casesByNumberIds.addAll(getConvertedFromNumbers(bpmCases));
		}

		//	Old cases
		List<Integer> simpleCases = CasesRetrievalManager.CASE_LIST_TYPE_USER.equals(getCaseListType()) ?
				getUserCasesByNumber(loweredCaseNumber) :
				getGeneralCasesByNumber(loweredCaseNumber);
		if (ListUtil.isEmpty(simpleCases)) {
			getLogger().info("No simple cases found by number: " + caseNumber);
		} else {
			getLogger().info("Simple cases by number '" + caseNumber + "': " + simpleCases);
			casesByNumberIds.addAll(simpleCases);
		}

		if (ListUtil.isEmpty(casesByNumberIds)) {
			getLogger().info("No cases found by number: " + caseNumber);
		} else {
			getLogger().info("Cases found by number '" + caseNumber + "': " + casesByNumberIds);
		}
		return new ArrayList<>(casesByNumberIds);
	}

	@Override
	protected String getInfo() {
		return "Looking for cases by number: " + getCaseNumber();
	}

	private List<Integer> getGeneralCasesByNumber(String caseNumber) {
		GeneralCaseHome caseHome = null;
		try {
			caseHome = (GeneralCaseHome) IDOLookup.getHome(GeneralCase.class);
		} catch (IDOLookupException e) {
			e.printStackTrace();
		}
		if (caseHome == null) {
			return null;	//	Unable to search for general cases
		}

		Collection<Integer> casesByNumber = null;
		try {
			casesByNumber = caseHome.getCasesIDsByCriteria(caseNumber, null, null, null, null, null, null, null, true);
		} catch (FinderException e) {
			e.printStackTrace();
		}
		if (ListUtil.isEmpty(casesByNumber)) {
			return null;	//	No results
		}

		return getUniqueIds(casesByNumber);
	}

	private List<Integer> getUserCasesByNumber(String number) {
		CaseHome caseHome = getCaseHome();
		if (caseHome == null) {
			return null;
		}

		Collection<Integer> casesByNumber = null;
		try {
			casesByNumber = caseHome.findIDsByCriteria(number, null, null, null, null, null, null, null, true);
		} catch (FinderException e) {
			e.printStackTrace();
		}

		return getUniqueIds(casesByNumber);
	}

	private CaseHome getCaseHome() {
		try {
			return (CaseHome) IDOLookup.getHome(Case.class);
		} catch (IDOLookupException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected String getFilterKey() {
		return getCaseNumber();
	}

	@Override
	protected boolean isFilterKeyDefined() {
		String caseNumber = getCaseNumber();
		if (StringUtil.isEmpty(caseNumber)) {
			getLogger().log(Level.INFO, "Case number is undefined, not filtering by it!");
			return false;
		}
		return true;
	}
}