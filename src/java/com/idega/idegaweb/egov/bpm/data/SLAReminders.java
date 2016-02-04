package com.idega.idegaweb.egov.bpm.data;


import java.io.Serializable;
import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "BPM_SLA_REMINDERS", indexes = { @Index(columnList = "PROCESS_ID"), @Index(columnList = "CASE_ID"), @Index(columnList = "CASE_STATE_ID") })
@NamedQueries( 
		{ 
		@NamedQuery(name = SLAReminders.getSetByCaseStateId, query = "select slar from SLAReminders slar where slar."
				+ SLAReminders.caseStateIdProperty + " = :" + SLAReminders.caseStateIdProperty + " and "
				+ SLAReminders.sendDateProperty + " = " + "(select max(slar." + SLAReminders.sendDateProperty
				+ ") from SLAReminders slar where slar." + SLAReminders.caseStateIdProperty + " = :"
				+ SLAReminders.caseStateIdProperty + ")")
		}
)
public class SLAReminders implements Serializable{

	private static final long serialVersionUID = -2689171864414927153L;

	public static final String getSetByCaseStateId = "SLAReminders.getSetByCaseStateId";
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID_")
	private Long id;
	
	public static final String caseStateIdProperty = "caseStateId";
	@Column(name = "CASE_STATE_ID", nullable = false)
	private Long caseStateId;
	
	public static final String processIdProperty = "processId";
	@Column(name = "PROCESS_ID", nullable = false)
	private Long processId;
	
	public static final String caseIdProperty = "caseId";
	@Column(name = "CASE_ID", nullable = false)
	private String caseId;
	
	public static final String mailToProperty = "mailTo";
	@Column(name= "MAIL_TO", nullable = false)
	private String mailTo;
	
	public static final String sendDateProperty = "sendDate";
	@Column(name= "SEND_DATE", nullable = false)
	private Date sendDate;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getCaseStateId() {
		return caseStateId;
	}
	public void setCaseStateId(Long caseStateId) {
		this.caseStateId = caseStateId;
	}
	public Long getProcessId() {
		return processId;
	}
	public void setProcessId(Long processId) {
		this.processId = processId;
	}
	public String getCaseId() {
		return caseId;
	}
	public void setCaseId(String caseId) {
		this.caseId = caseId;
	}
	public String getMailTo() {
		return mailTo;
	}
	public void setMailTo(String mailTo) {
		this.mailTo = mailTo;
	}
	public Date getSendDate() {
		return sendDate;
	}
	public void setSendDate(Date sendDate) {
		this.sendDate = sendDate;
	}
	
	@Override
	public String toString(){
		return "id = " + getId();
	}
	
}
