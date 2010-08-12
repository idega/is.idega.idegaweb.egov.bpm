package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import java.util.Collection;

import is.idega.idegaweb.egov.bpm.cases.search.CasesListSearchCriteriaBean;
import is.idega.idegaweb.egov.cases.business.CasesEngine;

import org.jdom.Document;

import com.idega.block.process.presentation.beans.CasePresentation;
import com.idega.block.process.presentation.beans.CasesSearchCriteriaBean;
import com.idega.bpm.bean.CasesBPMAssetProperties;
import com.idega.core.component.bean.RenderedComponent;

public interface BPMCasesEngine extends CasesEngine {

	public abstract Document getCaseManagerView(CasesBPMAssetProperties properties);
	
	public abstract Document getCasesListByUserQuery(CasesListSearchCriteriaBean criteriaBean);
	
	public abstract Long getProcessInstanceId(String caseId);
	
	public abstract RenderedComponent getVariablesWindow(String processDefinitionId);
	
	public abstract Collection<CasePresentation> getReLoadedCases(CasesSearchCriteriaBean criterias);
}