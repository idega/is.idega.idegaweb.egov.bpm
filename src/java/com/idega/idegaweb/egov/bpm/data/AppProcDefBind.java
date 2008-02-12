package com.idega.idegaweb.egov.bpm.data;


import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/02/12 14:37:23 $ by $Author: civilis $
 */
@Entity
@Table(name="BPM_APP_PROCDEF")
public class AppProcDefBind implements Serializable {
	
	private static final long serialVersionUID = -3413662786833844673L;

	@Column(name="process_definition_id", nullable=false)
    private Long procDefId;
	
	@Id
	@Column(name="application_id", nullable=false)
	private Integer applicationId;
	
	public AppProcDefBind() { }

	public Long getProcDefId() {
		return procDefId;
	}

	public void setProcDefId(Long procDefId) {
		this.procDefId = procDefId;
	}

	public Integer getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(Integer applicationId) {
		this.applicationId = applicationId;
	}
}