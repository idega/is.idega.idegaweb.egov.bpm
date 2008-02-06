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
 * Last modified: $Date: 2008/02/06 11:49:26 $ by $Author: civilis $
 */
@Entity
@Table(name="APP_BPM_BINDINGS")
public class AppBPMBind implements Serializable {
	
	private static final long serialVersionUID = -3413662786833844673L;

	
	@Column(name="process_definition_id", nullable=false)
    private Long procDefId;
	
	@Id
	@Column(name="application_id", nullable=false)
	private Integer applicationId;
	
	public AppBPMBind() { }

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