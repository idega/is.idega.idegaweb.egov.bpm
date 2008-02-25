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
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/02/25 16:16:26 $ by $Author: civilis $
 */
@Entity
@Table(name="BPM_PROCESS_USER")
@NamedQueries(
		{
			@NamedQuery(name=CaseUser.byUserIdNPID, query="from CaseUser cu where cu.userId = :"+CaseUser.userIdParam+ " and cu.processInstanceId = :"+CaseUser.pidParam)
		}
)
public class CaseUser implements Serializable {
	
	private static final long serialVersionUID = 4023013648108184230L;
	
	public static final String PROCESS_WATCHED_STATUS = "WATCHD";
	
	public static final String byUserIdNPID = "CaseUser.byUserIdNPID";
	public static final String userIdParam = "userIdParam";
	public static final String pidParam = "pidParam";

	@Id @GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ID")
    private Long id;
	
	@Column(name="user_id", nullable=false)
    private Integer userId;
	
	@Column(name="process_instance_id", nullable=false)
	private Long processInstanceId;
	
	@Column(name="status")
	private String watchStatus;

	public CaseUser() { }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public String getStatus() {
		return watchStatus;
	}

	public void setStatus(String status) {
		this.watchStatus = status;
	}

	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}
}