package com.idega.idegaweb.egov.bpm.data;


import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2008/05/16 18:17:08 $ by $Author: civilis $
 */
@Entity
@Table(name=ProcessUserBind.TABLE_NAME)
@NamedQueries(
		{
			@NamedQuery(name=ProcessUserBind.byUserIdNPID, query="from ProcessUserBind pub where pub.userId = :"+ProcessUserBind.userIdParam+ " and pub.caseProcessBind.procInstId = :"+ProcessUserBind.pidParam),
			@NamedQuery(name=ProcessUserBind.byUserIdAndCaseId, query="select pub from ProcessUserBind pub, com.idega.idegaweb.egov.bpm.data.CaseProcInstBind cpib where pub.userId = :"+ProcessUserBind.userIdParam+ " and cpib.caseId in(:"+ProcessUserBind.casesIdsParam+") and pub.caseProcessBind = cpib.procInstId"),
			@NamedQuery(
					name=ProcessUserBind.byPID, 
					query="FROM ProcessUserBind pub "
							+ "WHERE pub.caseProcessBind.procInstId = :" + ProcessUserBind.pidParam)
		}
)
public class ProcessUserBind implements Serializable {
	
	private static final long serialVersionUID = 4023013648108184230L;
	
	public enum Status {
		
		PROCESS_WATCHED,
		NO_STATUS
	}
	
	public static final String TABLE_NAME = "BPM_PROCESSES_USERS";
	
	public static final String byUserIdNPID = "ProcessUserBind.byUserIdNPID";
	public static final String byPID = "ProcessUserBind.byPID";
	public static final String byUserIdAndCaseId = "ProcessUserBind.byUserIdNCaseId";
	public static final String casesIdsParam = "casesIdsParam";
	public static final String userIdParam = "userIdParam";
	public static final String pidParam = "pidParam";

	@Id @GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ID_")
    private Long id;
	
	@Column(name="user_id", nullable=false)
    private Integer userId;
	
	public static final String statusProp = "status";
	@Column(name="user_status")
	@Enumerated(EnumType.STRING)
	private Status status;

	public static final String caseProcessBindProp = "caseProcessBind";
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "process_instance_id", referencedColumnName = CaseProcInstBind.procInstIdColumnName, nullable=false)
	private CaseProcInstBind caseProcessBind;

	public CaseProcInstBind getCaseProcessBind() {
		return caseProcessBind;
	}

	public void setCaseProcessBind(CaseProcInstBind caseProcessBind) {
		this.caseProcessBind = caseProcessBind;
	}

	public ProcessUserBind() { }

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

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
}