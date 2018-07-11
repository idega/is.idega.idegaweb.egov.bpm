package is.idega.idegaweb.egov.bpm.cases.search;

import java.util.List;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/09/11 10:52:57 $ by $Author: civilis $
 */
public interface CasesListSearchFilter {

	public void setCriterias(CasesListSearchCriteriaBean criterias);

	public List<Integer> doFilter(List<Integer> casesIds);
}