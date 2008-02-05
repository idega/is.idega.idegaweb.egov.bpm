package com.idega.idegaweb.egov.bpm.cases.data;


import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/02/05 19:32:16 $ by $Author: civilis $
 */
@Entity
@Table(name="CASES_JBPM_BINDINGS")
@NamedQueries(
		{
			@NamedQuery(name=CasesBPMBind.CASES_PROCESSES_DEFINITIONS_QUERY_NAME, query="select pd.id, pd.name from org.jbpm.graph.def.ProcessDefinition pd, CasesBPMBind cb where pd.id = cb.procDefId"),
			@NamedQuery(name=CasesBPMBind.CASES_PROCESSES_GET_ALL_QUERY_NAME, query="from CasesBPMBind")
		}
)
public class CasesBPMBind implements Serializable {
	
	private static final long serialVersionUID = -3222584305636229751L;
	
	public static final String CASES_PROCESSES_DEFINITIONS_QUERY_NAME = "CasesBPMBind.simpleCasesProcessesDefinitionsQuery";
	public static final String CASES_PROCESSES_GET_ALL_QUERY_NAME = "CasesBPMBind.getAllQuery";

	@Id
	@Column(name="process_definition_id")
    private Long procDefId;
	
	@Column(name="cases_category_id")
	private Long casesCategoryId;
	
	@Column(name="cases_type_id")
	private Long casesTypeId;
	
	@Column(name="init_task_name")
	private String initTaskName;
	
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

	public CasesBPMBind() { }

	public Long getProcDefId() {
		return procDefId;
	}

	public void setProcDefId(Long procDefId) {
		this.procDefId = procDefId;
	}

	public String getInitTaskName() {
		return initTaskName;
	}

	public void setInitTaskName(String initTaskName) {
		this.initTaskName = initTaskName;
	}
}