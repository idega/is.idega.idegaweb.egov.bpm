package com.idega.idegaweb.egov.bpm.data;


import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/02/26 14:59:12 $ by $Author: civilis $
 */
@Entity
@Table(name="BPM_CASETYPES_PROCDEF")
@NamedQueries(
		{
			@NamedQuery(name=CaseTypesProcDefBind.CASES_PROCESSES_DEFINITIONS_QUERY_NAME, query="select pd.id, pd.name from org.jbpm.graph.def.ProcessDefinition pd, CaseTypesProcDefBind ctpdb where pd.id = ctpdb.procDefId"),
			@NamedQuery(name=CaseTypesProcDefBind.CASES_PROCESSES_GET_ALL_QUERY_NAME, query="from CaseTypesProcDefBind")
		}
)
public class CaseTypesProcDefBind implements Serializable {
	
	private static final long serialVersionUID = -3222584305636229751L;
	
	public static final String CASES_PROCESSES_DEFINITIONS_QUERY_NAME = "CaseTypesProcDefBind.simpleCasesProcessesDefinitionsQuery";
	public static final String CASES_PROCESSES_GET_ALL_QUERY_NAME = "CaseTypesProcDefBind.getAllQuery";

//	TODO: try to use ProcessDefinition entity here instead of just Long id, check if it will work correctly without resolving it from jbpmContext
	@Id
	@Column(name="process_definition_id", nullable=false)
    private Long procDefId;
	
	@Column(name="cases_category_id", nullable=false)
	private Long casesCategoryId;
	
	@Column(name="cases_type_id", nullable=false)
	private Long casesTypeId;
	
	public Long getCasesCategoryId() {
		return casesCategoryId;
	}

	public void setCasesCategoryId(Long casesCategoryId) {
		this.casesCategoryId = casesCategoryId;
	}

	public Long getCasesTypeId() {
		return casesTypeId;
	}

	public void setCasesTypeId(Long casesTypeId) {
		this.casesTypeId = casesTypeId;
	}

	public CaseTypesProcDefBind() { }

	public Long getProcDefId() {
		return procDefId;
	}

	public void setProcDefId(Long procDefId) {
		this.procDefId = procDefId;
	}
}