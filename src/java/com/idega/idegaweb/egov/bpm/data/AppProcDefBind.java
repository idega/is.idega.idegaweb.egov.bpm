package com.idega.idegaweb.egov.bpm.data;


import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/04/12 01:53:48 $ by $Author: civilis $
 */
@Entity
@Table(name="BPM_APPL_PROCDEF")
public class AppProcDefBind implements Serializable {
	
	private static final long serialVersionUID = -817229691946827690L;

	@Column(name="process_definition_name", nullable=false)
    private String processDefinitionName;
	
	@Id
	@Column(name="application_id", nullable=false)
	private Integer applicationId;
	
	public AppProcDefBind() { }

	public Integer getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(Integer applicationId) {
		this.applicationId = applicationId;
	}

	public String getProcessDefinitionName() {
		return processDefinitionName;
	}

	public void setProcessDefinitionName(String processDefinitionName) {
		this.processDefinitionName = processDefinitionName;
	}
}