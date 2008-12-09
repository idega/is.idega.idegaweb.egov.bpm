package com.idega.idegaweb.egov.bpm.data;


import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * TODO: should relate process name!
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/12/09 02:49:13 $ by $Author: civilis $
 */
@Entity
@Table(name="BPM_APPLICATIONS_SUPPORTS")
public class AppSupports implements Serializable {
	
	private static final long serialVersionUID = -6856253288624404131L;
	
	@Id @GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ID_")
    private Long id;
	
	public static final String appProcBindDefinitionIdProperty = "appProcBindDefinitionId";
	@Column(name="APP_PROC_DEFINITION_ID", nullable=false)
	private Long appProcBindDefinitionId;
	
	public static final String applicationIdProperty = "applicationId";
	@Column(name="APPLICATION_ID", nullable=false)
	private Integer applicationId;
	
	public static final String roleKeyProperty = "roleKey";
	@Column(name="ROLE_KEY")
	private String roleKey;

	public Long getId() {
		return id;
	}

	public Integer getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(Integer applicationId) {
		this.applicationId = applicationId;
	}

	public String getRoleKey() {
		return roleKey;
	}

	public void setRoleKey(String roleKey) {
		this.roleKey = roleKey;
	}

	public Long getAppProcBindDefinitionId() {
		return appProcBindDefinitionId;
	}

	public void setAppProcBindDefinitionId(Long appProcBindDefinitionId) {
		this.appProcBindDefinitionId = appProcBindDefinitionId;
	}
}