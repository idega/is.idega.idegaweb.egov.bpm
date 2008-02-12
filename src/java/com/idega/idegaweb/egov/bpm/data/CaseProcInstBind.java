package com.idega.idegaweb.egov.bpm.data;


import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.Table;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/02/12 14:37:23 $ by $Author: civilis $
 */
@Entity
@Table(name="BPM_CASE_PROCINST")
@NamedQueries(
		{
		}
)
public class CaseProcInstBind implements Serializable {
	
	private static final long serialVersionUID = -335682330238243547L;

	@Id
	@Column(name="process_instance_id", nullable=false)
    private Long procDefId;
	
	@Column(name="case_id", nullable=false, unique=true)
	private Integer caseId;

	public CaseProcInstBind() { }

	public Long getProcDefId() {
		return procDefId;
	}

	public void setProcDefId(Long procDefId) {
		this.procDefId = procDefId;
	}

	public Integer getCaseId() {
		return caseId;
	}

	public void setCaseId(Integer caseId) {
		this.caseId = caseId;
	}
}