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
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/04/12 01:53:48 $ by $Author: civilis $
 */
@Entity
@Table(name="BPM_CASETYPES_PROCDEFS")
@NamedQueries(
		{
			@NamedQuery(name=CaseTypesProcDefBind.CASES_PROCESSES_GET_ALL, query="from CaseTypesProcDefBind"),
			@NamedQuery(name=CaseTypesProcDefBind.CASES_PROCESSES_GET_BY_PDNAME, query="from CaseTypesProcDefBind ctpd where ctpd."+CaseTypesProcDefBind.procDefNamePropName+" = :"+CaseTypesProcDefBind.procDefNamePropName)
		}
)
public class CaseTypesProcDefBind implements Serializable {
	
	private static final long serialVersionUID = -3222584305636229751L;
	
	public static final String CASES_PROCESSES_GET_ALL = "CaseTypesProcDefBind.getAll";
	public static final String CASES_PROCESSES_GET_BY_PDNAME = "CaseTypesProcDefBind.getByPDName";

	public static final String procDefNamePropName = "processDefinitionName";
	@Id
	@Column(name="process_definition_name", nullable=false)
    private String processDefinitionName;
	
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

	public String getProcessDefinitionName() {
		return processDefinitionName;
	}

	public void setProcessDefinitionName(String processDefinitionName) {
		this.processDefinitionName = processDefinitionName;
	}
}