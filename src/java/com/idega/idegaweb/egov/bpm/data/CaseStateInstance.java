package com.idega.idegaweb.egov.bpm.data;

import java.io.Serializable;
import java.sql.Date;
import java.util.Calendar;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;


@Entity
@Table(name = "BPM_CASE_STATE_INSTANCE", indexes = { @Index(columnList = "PROCESS_ID"), @Index(columnList = "CASE_ID") })
@NamedQueries(
		{
			@NamedQuery(name = CaseStateInstance.getSetByProcessId, query = "from CaseStateInstance cs where cs." + CaseStateInstance.processIdProperty + " = :" + CaseStateInstance.processIdProperty),
			@NamedQuery(name = CaseStateInstance.getSetByProcessIdAndName, query = "from CaseStateInstance cs where cs." + CaseStateInstance.processIdProperty + " = :" + CaseStateInstance.processIdProperty + " and " + CaseStateInstance.stateNameProperty + " in ( :" + CaseStateInstance.stateNameProperty + ")"),
			@NamedQuery(name = CaseStateInstance.getSetByCaseId, query = "from CaseStateInstance cs where cs." + CaseStateInstance.caseIdProperty + " = :" + CaseStateInstance.caseIdProperty)
		}
)
public class CaseStateInstance implements Serializable {

	private static final long serialVersionUID = -5982301697065406830L;

	public static final String getSetByProcessId = "CaseStateInstance.getSetByProcessId";
	public static final String getSetByProcessIdAndName = "CaseStateInstance.getSetByProcessIdAndName";
	public CaseState getState() {
		return state;
	}

	public void setState(CaseState state) {
		this.state = state;
	}

	public static final String getSetByCaseId = "CaseStateInstance.getSetByCaseId";

	public enum State {
		RECEIVED,
	    ACTIVE,
	    FINISHED,
	    CANCELLED,
	    SUSPENDED,
	    INQUEUE
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID_")
	private Long id;

	public static final String stateProperty = "state";
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "CASE_STATE_ID", referencedColumnName="ID_", nullable = false)
	private CaseState state;

	public static final String processIdProperty = "processId";
	@Column(name = "PROCESS_ID", nullable = false)
	private Long processId;

	public static final String caseIdProperty = "caseId";
	@Column(name = "CASE_ID", nullable = false)
	private String caseId;

	public static final String stateNameProperty = "stateName";
	@Column(name= "STATE_NAME", nullable = false)
	private String stateName;

	public static final String stateStateProperty = "stateState";
	@Column(name= "STATE_STATE", nullable = false)
	@Enumerated(EnumType.STRING)
	private State stateState;

	public static final String stateStartDateProperty = "stateStartDate";
	@Column(name= "STATE_START_DATE", nullable = true)
	private Date stateStartDate;

	public static final String stateEndDateProperty = "stateEndDate";
	@Column(name= "STATE_END_DATE", nullable = true)
	private Date stateEndDate;

	public static final String stateExpectedStartDateProperty = "stateExpectedStartDate";
	@Column(name= "STATE_EXPECTED_START_DATE", nullable = true)
	private Date stateExpectedStartDate;

	public static final String stateExpectedEndDateProperty = "stateExpectedEndDate";
	@Column(name= "STATE_EXPECTED_END_DATE", nullable = true)
	private Date stateExpectedEndDate;

	public Long getId() {
		return id;
	}

	public Long getProcessId() {
		return processId;
	}

	public void setProcessId(Long processId) {
		this.processId = processId;
	}

	public String getStateName() {
		return stateName;
	}

	public void setStateName(String stateName) {
		this.stateName = stateName;
	}

	public State getStateState() {
		return stateState;
	}

	public void setStateState(State stateState) {
		this.stateState = stateState;
	}

	public Date getStateStartDate() {
		return stateStartDate;
	}

	public void setStateStartDate(Date stateStartDate) {
		this.stateStartDate = stateStartDate;
	}

	public Date getStateEndDate() {
		return stateEndDate;
	}

	public void setStateEndDate(Date stateEndDate) {
		this.stateEndDate = stateEndDate;
	}

	public Date getStateExpectedStartDate() {
		return stateExpectedStartDate;
	}

	public void setStateExpectedStartDate(Date stateExpectedStartDate) {
		this.stateExpectedStartDate = stateExpectedStartDate;
	}

	public Date getStateExpectedEndDate() {
		return stateExpectedEndDate;
	}

	public void setStateExpectedEndDate(Date stateExpectedEndDate) {
		this.stateExpectedEndDate = stateExpectedEndDate;
	}

	public Date getStateEndDate90(){
		Calendar c = Calendar.getInstance();
		c.setTime(stateEndDate);
		c.add(Calendar.DATE, 90);
		return new Date(c.getTimeInMillis());
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCaseId() {
		return caseId;
	}

	public void setCaseId(String caseId) {
		this.caseId = caseId;
	}

	@Override
	public String toString() {

		return "id=" + getId() + " sate name="
		        + getStateName();
	}


}

