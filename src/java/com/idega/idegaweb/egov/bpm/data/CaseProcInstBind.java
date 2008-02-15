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
 * Last modified: $Date: 2008/02/15 12:37:22 $ by $Author: civilis $
 */
@Entity
@Table(name="BPM_CASE_PROCINST")
@NamedQueries(
		{
			@NamedQuery(name=CaseProcInstBind.BIND_BY_CASEID_QUERY_NAME, query="from CaseProcInstBind bind where bind.caseId = :"+CaseProcInstBind.caseIdParam)
		}
)
public class CaseProcInstBind implements Serializable {
	
	private static final long serialVersionUID = -335682330238243547L;
	
	public static final String BIND_BY_CASEID_QUERY_NAME = "CaseProcInstBind.bindByCaseIdQuery";
	public static final String caseIdParam = "caseId";

	@Id
	@Column(name="process_instance_id", nullable=false)
    private Long procInstId;
	
	@Column(name="case_id", nullable=false, unique=true)
	private Integer caseId;

	public CaseProcInstBind() { }

	public Long getProcInstId() {
		return procInstId;
	}

	public void setProcInstId(Long procInstId) {
		this.procInstId = procInstId;
	}

	public Integer getCaseId() {
		return caseId;
	}

	public void setCaseId(Integer caseId) {
		this.caseId = caseId;
	}
}