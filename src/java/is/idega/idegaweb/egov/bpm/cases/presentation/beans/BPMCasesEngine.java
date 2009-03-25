package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import is.idega.idegaweb.egov.bpm.cases.search.CasesListSearchCriteriaBean;
import is.idega.idegaweb.egov.cases.business.CasesEngine;

import org.jdom.Document;

import com.idega.bpm.bean.CasesBPMAssetProperties;
import com.idega.core.component.bean.RenderedComponent;

public interface BPMCasesEngine extends CasesEngine {

	public Document getCaseManagerView(CasesBPMAssetProperties properties);
	
	public Document getCasesListByUserQuery(CasesListSearchCriteriaBean criteriaBean);
	
	public Long getProcessInstanceId(String caseId);
	
	public RenderedComponent getVariablesWindow(String processDefinitionId);
	
}
