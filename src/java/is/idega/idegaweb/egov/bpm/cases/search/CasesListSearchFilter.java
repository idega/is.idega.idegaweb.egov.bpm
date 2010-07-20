package is.idega.idegaweb.egov.bpm.cases.search;

import java.util.List;

import com.idega.block.process.presentation.beans.CasesSearchCriteriaBean;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/09/11 10:52:57 $ by $Author: civilis $
 */
public interface CasesListSearchFilter {
	
	public void setCriterias(CasesSearchCriteriaBean criterias);
	
	public List<Integer> doFilter(List<Integer> casesIds);
}