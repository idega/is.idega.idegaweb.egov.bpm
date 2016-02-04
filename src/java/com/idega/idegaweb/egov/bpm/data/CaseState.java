package com.idega.idegaweb.egov.bpm.data;

import java.io.Serializable;

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
@Table(name = "BPM_CASE_STATE", indexes = { @Index(columnList = "PROCESS_DEFINITION_NAME")})
@NamedQueries( 
		{ 
			@NamedQuery(name = CaseState.getSetByProcessName, query = "from CaseState cs where cs." + CaseState.processDefinitionNameProperty + " = :" + CaseState.processDefinitionNameProperty + " order by " + CaseState.stateSequenceIdProperty),
			@NamedQuery(name = CaseState.getByProcessNameAndStateName, query = "from CaseState cs where cs." + CaseState.processDefinitionNameProperty + " = :" + CaseState.processDefinitionNameProperty + " and " + CaseState.stateNameProperty + " = :" + CaseState.stateNameProperty),
			@NamedQuery(name = CaseState.getSet, query = "from CaseState cs")
		}
)
public class CaseState implements Serializable{

	private static final long serialVersionUID = 9194845371842268865L;

	public static final String getByProcessNameAndStateName = "CaseState.getByProcessNameAndStateName";
	public static final String getSetByProcessName = "CaseState.getSetByProcessName";
	public static final String getSet = "CaseState.getSet";
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID_")
	private Long id;
	
	public static final String processDefinitionNameProperty = "processDefinitionName";
	@Column(name = "PROCESS_DEFINITION_NAME", nullable = false)
	private String processDefinitionName;
	
	public static final String stateNameProperty = "stateName";
	@Column(name= "STATE_NAME", nullable = false)
	private String stateName;
	
	public static final String stateLocalizationKeyProperty = "stateLocalizationKey";
	@Column(name= "STATE_LOCALIZATION_KEY", nullable = false)
	private String stateLocalizationKey;
	
	public static final String stateDefaultLocalizedNameProperty = "stateDefaultLocalizedName";
	@Column(name= "STATE_DEFAULT_LOCALIZED_NAME", nullable = false)
	private String stateDefaultLocalizedName;
	
	public static final String stateSLAProperty = "stateSLA";
	@Column(name= "STATE_SLA", nullable = true)
	private Long stateSLA;
	
	public static final String stateInactiveSLAProperty = "stateInactiveSLA";
	@Column(name= "STATE_INACTIVE_SLA", nullable = true)
	private Long stateInactiveSLA;
	
	public static final String stateSLABreachReminderIntervalProperty = "stateSLABreachReminderInterval";
	@Column(name= "STATE_SLA_REMINDER_INTERVAL", nullable = true)
	private Long stateSLABreachReminderInterval;
	
	public static final String stateSequenceIdProperty = "stateSequenceId";
	@Column(name = "STATE_SEQUENCE_ID", nullable = false)
	private Long stateSequenceId;
	
	public Long getStateSLA() {
		return stateSLA;
	}

	public void setStateSLA(Long stateSLA) {
		this.stateSLA = stateSLA;
	}

	public Long getStateInactiveSLA() {
		return stateInactiveSLA;
	}

	public void setStateInactiveSLA(Long stateInactiveSLA) {
		this.stateInactiveSLA = stateInactiveSLA;
	}
	
	public Long getStateSLABreachReminderInterval() {
		return stateSLABreachReminderInterval;
	}

	public void setStateSLABreachReminderInterval(Long stateSLABreachReminderInterval) {
		this.stateSLABreachReminderInterval = stateSLABreachReminderInterval;
	}
	
	public Long getStateSequenceId() {
		return stateSequenceId;
	}

	public void setStateSequenceId(Long stateSequenceId) {
		this.stateSequenceId = stateSequenceId;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getProcessDefinitionName() {
		return processDefinitionName;
	}

	public void setProcessDefinitionName(String processDefinitionName) {
		this.processDefinitionName = processDefinitionName;
	}

	public String getStateName() {
		return stateName;
	}

	public void setStateName(String stateName) {
		this.stateName = stateName;
	}

	public String getStateLocalizationKey() {
		return stateLocalizationKey;
	}

	public void setStateLocalizationKey(String stateLocalizationKey) {
		this.stateLocalizationKey = stateLocalizationKey;
	}

	public String getStateDefaultLocalizedName() {
		return stateDefaultLocalizedName;
	}

	public void setStateDefaultLocalizedName(String stateDefaultLocalizedName) {
		this.stateDefaultLocalizedName = stateDefaultLocalizedName;
	}
	
}
