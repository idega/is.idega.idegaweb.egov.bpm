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
 * Last modified: $Date: 2008/04/06 17:53:12 $ by $Author: civilis $
 */
@Entity
@Table(name="BPM_APP_PROCDEF")
@NamedQueries(
		{
			@NamedQuery(name=AppProcDefBind.findProcessDefByAppId, query="select pd from org.jbpm.graph.def.ProcessDefinition pd, AppProcDefBind apd where apd."+AppProcDefBind.applicationIdProp+" = :"+AppProcDefBind.applicationIdProp+" and apd."+AppProcDefBind.procDefIdProp+" = pd.id")
		}
)
public class AppProcDefBind implements Serializable {
	
	private static final long serialVersionUID = -3413662786833844673L;
	public static final String findProcessDefByAppId = "AppProcDefBind.findProcessDefByAppId";

	public static final String procDefIdProp = "procDefId";
	@Column(name="process_definition_id", nullable=false)
    private Long procDefId;
	
	public static final String applicationIdProp = "applicationId";
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