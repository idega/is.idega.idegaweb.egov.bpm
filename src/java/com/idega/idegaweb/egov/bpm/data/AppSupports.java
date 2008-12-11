package com.idega.idegaweb.egov.bpm.data;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Binds process with roles, that can start process.
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 * 
 *          Last modified: $Date: 2008/12/11 19:24:45 $ by $Author: civilis $
 */
@Entity
@Table(name = "BPM_APPLICATIONS_SUPPORTS")
@NamedQueries( { @NamedQuery(name = AppSupports.getSetByProcessNameApplicationId, query = "from AppSupports asup where asup."
		+ AppSupports.processNameProperty
		+ " = :"
		+ AppSupports.processNameProperty
		+ " and asup."
		+ AppSupports.applicationIdProperty
		+ " = :"
		+ AppSupports.applicationIdProperty) })
public class AppSupports implements Serializable {

	private static final long serialVersionUID = -1937482511922158268L;

	public static final String getSetByProcessNameApplicationId = "AppSupports.getSetByProcessNameApplicationId";

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID_")
	private Long id;

	public static final String processNameProperty = "processName";
	@Column(name = "PROCESS_NAME", nullable = false)
	private String processName;

	public static final String applicationIdProperty = "applicationId";
	@Column(name = "APPLICATION_ID", nullable = false)
	private Integer applicationId;

	public static final String roleKeyProperty = "roleKey";
	@Column(name = "ROLE_KEY", nullable = false)
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

	@Override
	public String toString() {

		return "application id=" + getApplicationId() + " role key="
				+ getRoleKey();
	}

	public String getProcessName() {
		return processName;
	}

	public void setProcessName(String processName) {
		this.processName = processName;
	}
}