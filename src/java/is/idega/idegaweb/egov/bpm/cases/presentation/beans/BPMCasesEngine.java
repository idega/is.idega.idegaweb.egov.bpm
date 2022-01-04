package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import java.util.Collection;
import java.util.List;

import org.jdom2.Document;

import com.idega.block.process.presentation.beans.CasePresentation;
import com.idega.block.process.presentation.beans.CasesSearchCriteriaBean;
import com.idega.bpm.bean.CasesBPMAssetProperties;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.core.component.bean.RenderedComponent;
import com.idega.presentation.IWContext;

import is.idega.idegaweb.egov.bpm.cases.search.CasesListSearchCriteriaBean;
import is.idega.idegaweb.egov.cases.business.CasesEngine;

public interface BPMCasesEngine extends CasesEngine {

	public abstract Document getCaseManagerView(CasesBPMAssetProperties properties);

	public abstract Document getCasesListByUserQuery(CasesListSearchCriteriaBean criteriaBean);

	public abstract Long getProcessInstanceId(String caseId);

	public abstract RenderedComponent getVariablesWindow(String processDefinitionId);

	public abstract Collection<CasePresentation> getReLoadedCases(CasesSearchCriteriaBean criterias);

	public void addSearchQueryToSession(IWContext iwc, CasesListSearchCriteriaBean criterias);

	public boolean setSearchResults(IWContext iwc, Collection<CasePresentation> cases, CasesListSearchCriteriaBean criterias);

	public boolean clearSearchResults(String id, IWContext iwc);

	public List<AdvancedProperty> getSearchFields(IWContext iwc, CasesListSearchCriteriaBean criterias);

}