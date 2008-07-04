package com.idega.idegaweb.egov.bpm.data;


import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessConstants;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.14 $
 *
 * Last modified: $Date: 2008/07/04 15:19:49 $ by $Author: valdas $
 */
@Entity
@Table(name=CaseProcInstBind.TABLE_NAME)
@NamedQueries(
		{
			@NamedQuery(name=CaseProcInstBind.BIND_BY_CASEID_QUERY_NAME, query="from CaseProcInstBind bind where bind.caseId = :"+CaseProcInstBind.caseIdParam),
			@NamedQuery(name=CaseProcInstBind.getLatestByDateQN, query="from CaseProcInstBind cp where cp."+CaseProcInstBind.dateCreatedProp+" = :"+CaseProcInstBind.dateCreatedProp+" and cp."+CaseProcInstBind.caseIdentierIDProp+" = (select max(cp2."+CaseProcInstBind.caseIdentierIDProp+") from CaseProcInstBind cp2 where cp2."+CaseProcInstBind.dateCreatedProp+" = cp."+CaseProcInstBind.dateCreatedProp+")"),
			@NamedQuery(name=CaseProcInstBind.getByDateCreatedAndCaseIdentifierId, query="select cp, pi from CaseProcInstBind cp, org.jbpm.graph.exe.ProcessInstance pi where cp."+CaseProcInstBind.dateCreatedProp+" in(:"+CaseProcInstBind.dateCreatedProp+") and cp."+CaseProcInstBind.caseIdentierIDProp+" in(:"+CaseProcInstBind.caseIdentierIDProp+") and pi.id = cp."+CaseProcInstBind.procInstIdProp),
			@NamedQuery(name=CaseProcInstBind.getCaseIdByProcessInstanceId, query="select cp." + CaseProcInstBind.caseIdProp + " from CaseProcInstBind cp where cp."+ CaseProcInstBind.procInstIdProp + " = :" + CaseProcInstBind.procInstIdProp),
			@NamedQuery(name=CaseProcInstBind.getCaseIdsByProcessInstanceIds, query = "select cp." + CaseProcInstBind.caseIdProp + " from CaseProcInstBind cp where cp." + CaseProcInstBind.procInstIdProp + " in (:" + CaseProcInstBind.processInstanceIdsProp + ") group by cp." + CaseProcInstBind.caseIdProp),
//			some stupid hibernate bug, following doesn't work
//			@NamedQuery(name=CaseProcInstBind.getSubprocessByName, query="select pi.version from org.jbpm.graph.exe.ProcessInstance pi, org.jbpm.graph.exe.Token tkn where tkn.processInstance = :"+CaseProcInstBind.procInstIdProp+" and pi.version = tkn.subProcessInstance and :"+CaseProcInstBind.subProcessNameParam+" = (select epd.name from org.jbpm.graph.def.ProcessDefinition epd where epd.id = pi.processDefinition)")
			
			@NamedQuery(name=CaseProcInstBind.getSubprocessTokensByPI, query="select tkn from org.jbpm.graph.exe.Token tkn where tkn.processInstance = :"+CaseProcInstBind.procInstIdProp+" and tkn.subProcessInstance is not null")
		}
)
			/*
			@NamedQuery(name=CaseProcInstBind.getCaseIdsByProcessInstanceIdsProcessInstanceEnded, query=
				"select cp."+CaseProcInstBind.caseIdProp+" from CaseProcInstBind as cp " +
						"inner join org.jbpm.graph.exe.ProcessInstance as pi " +
						"on cp."+CaseProcInstBind.procInstIdProp+" = pi.id " +
						"where cp."+CaseProcInstBind.procInstIdProp+" in (:"+CaseProcInstBind.procInstIdProp+")"
			),
			@NamedQuery(name=CaseProcInstBind.getCaseIdsByProcessInstanceIdsProcessInstanceNotEnded, query=
				"select cp."+CaseProcInstBind.caseIdProp+" from CaseProcInstBind cp " +
				"inner join org.jbpm.graph.exe.ProcessInstance pi " +
				"on cp."+CaseProcInstBind.procInstIdProp+" = pi.id " +
				"where cp."+CaseProcInstBind.procInstIdProp+" in (:"+CaseProcInstBind.procInstIdProp+") and pi.end is null"
			),
			@NamedQuery(name=CaseProcInstBind.getCaseIdsByProcessInstanceIdsAndProcessUserStatus, query=
				"select cp."+CaseProcInstBind.caseIdProp+" from CaseProcInstBind cp " +
				"inner join com.idega.idegaweb.egov.bpm.data.ProcessUserBind pu " +
				"on cp."+CaseProcInstBind.procInstIdColumnName+" = pu."+ProcessUserBind.caseProcessBindProp+" " +
				"where cp."+CaseProcInstBind.procInstIdProp+" in (:"+CaseProcInstBind.procInstIdProp+") and pu."+ProcessUserBind.statusProp+" = :"+ProcessUserBind.statusProp
			)*/
		 

@SqlResultSetMapping(name="caseId", columns=@ColumnResult(name="caseId"))
@NamedNativeQueries(
		{
			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByProcessInstanceIdsProcessInstanceEnded, resultSetMapping="caseId",
							query=
				"select cp.case_id caseId from "+CaseProcInstBind.TABLE_NAME+" cp " +
						"inner join JBPM_PROCESSINSTANCE pi " +
						"on cp."+CaseProcInstBind.procInstIdColumnName+" = pi.id_ " +
						"where cp."+CaseProcInstBind.procInstIdColumnName+" in (:"+CaseProcInstBind.procInstIdProp+") and pi.end_ is not null"
			),
			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByProcessInstanceIdsProcessInstanceNotEnded, resultSetMapping="caseId",
					query=
				"select cp.case_id caseId from "+CaseProcInstBind.TABLE_NAME+" cp " +
				"inner join JBPM_PROCESSINSTANCE pi " +
				"on cp."+CaseProcInstBind.procInstIdColumnName+" = pi.id_ " +
				"where cp."+CaseProcInstBind.procInstIdColumnName+" in (:"+CaseProcInstBind.procInstIdProp+") and pi.end_ is null"
			),
			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByProcessInstanceIdsAndProcessUserStatus, resultSetMapping="caseId", 
					query=
				"select cp.case_id caseId from "+CaseProcInstBind.TABLE_NAME+" cp " +
				"inner join "+ProcessUserBind.TABLE_NAME+" pu " +
				"on cp."+CaseProcInstBind.procInstIdColumnName+" = pu.process_instance_id " +
				"where cp."+CaseProcInstBind.procInstIdColumnName+" in (:"+CaseProcInstBind.procInstIdProp+") and pu.user_status = :"+ProcessUserBind.statusProp
			),
			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByProcessUserStatus, resultSetMapping="caseId", 
					query=
				"select cp.case_id caseId from " + CaseProcInstBind.TABLE_NAME+" cp inner join " + ProcessUserBind.TABLE_NAME + " pu on cp." + 
				CaseProcInstBind.procInstIdColumnName + " = pu.process_instance_id where pu.user_status = :" + ProcessUserBind.statusProp
			),
			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByProcessDefinitionId, resultSetMapping="caseId", 
					query=
				"select cp.case_id caseId from JBPM_PROCESSINSTANCE pi inner join JBPM_PROCESSDEFINITION pd on pi.PROCESSDEFINITION_ = pd.id_ inner join " +
				CaseProcInstBind.TABLE_NAME + " cp on pi.ID_ = cp." + CaseProcInstBind.procInstIdColumnName + " where pi.PROCESSDEFINITION_ in (:" + 
				CaseProcInstBind.processDefinitionIdProp + ")"
			),
			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByCaseNumber, resultSetMapping="caseId", 
					query=
				"select cp.case_id caseId from " + CaseProcInstBind.TABLE_NAME + " cp inner join JBPM_VARIABLEINSTANCE pv on cp." +
				CaseProcInstBind.procInstIdColumnName + " = pv.PROCESSINSTANCE_ where pv.NAME_ = '" + CasesBPMProcessConstants.caseIdentifier + "' and " +
				"lower(pv.STRINGVALUE_) like :" + CaseProcInstBind.caseNumberProp + " group by cp.case_id"
			),
			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByCaseStatus, resultSetMapping="caseId", 
					query=
				"select cp.case_id caseId from " + CaseProcInstBind.TABLE_NAME + " cp inner join JBPM_VARIABLEINSTANCE pv on cp." +
				CaseProcInstBind.procInstIdColumnName + " = pv.PROCESSINSTANCE_ where pv.NAME_ = '" + CasesBPMProcessConstants.caseStatusVariableName + "' and " +
				"pv.STRINGVALUE_ in (:" + CaseProcInstBind.caseStatusesProp + ") group by cp.case_id"
			),
			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByUserIds, resultSetMapping="caseId", 
					query=
				"select cp.case_id caseId from " + CaseProcInstBind.TABLE_NAME + " cp inner join JBPM_TASKINSTANCE ti on cp." + 
				CaseProcInstBind.procInstIdColumnName + " = ti.PROCINST_ where ti.ACTORID_ in (:" + ProcessUserBind.userIdParam + ") and ti.END_ is not null group by " +
				"cp.case_id"
			),
			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByDateRange, resultSetMapping="caseId",
					query=
				"select cp.case_id caseId from " + CaseProcInstBind.TABLE_NAME + " cp inner join JBPM_PROCESSINSTANCE pi on cp." + 
				CaseProcInstBind.procInstIdColumnName + " = pi.id_ where pi.start_ between :" + CaseProcInstBind.caseStartDateProp + " and :" +
				CaseProcInstBind.caseEndDateProp
			)
		}
)
public class CaseProcInstBind implements Serializable {
	
	private static final long serialVersionUID = -335682330238243547L;
	
	public static final String BIND_BY_CASEID_QUERY_NAME = "CaseProcInstBind.bindByCaseIdQuery";
	public static final String getLatestByDateQN = "CaseProcInstBind.getLatestByDate";
	public static final String getCaseIdByProcessInstanceId="CaseProcInstBind.getCaseIdByPIID";
	public static final String getCaseIdsByProcessInstanceIds = "CaseProcInstBind.getCaseIdsByPIIDs";
	public static final String getByDateCreatedAndCaseIdentifierId = "CaseProcInstBind.getByDateCreatedAndCaseIdentifierId";
	public static final String getSubprocessTokensByPI = "CaseProcInstBind.getSubprocessTokensByPI";
	public static final String getCaseIdsByProcessInstanceIdsProcessInstanceEnded = "CaseProcInstBind.getCaseIdsByProcessInstanceIdsProcessInstanceEnded";
	public static final String getCaseIdsByProcessInstanceIdsProcessInstanceNotEnded = "CaseProcInstBind.getCaseIdsByProcessInstanceIdsProcessInstanceNotEnded";
	public static final String getCaseIdsByProcessInstanceIdsAndProcessUserStatus = "CaseProcInstBind.getCaseIdsByProcessInstanceIdsAndProcessUserStatus";
	public static final String getCaseIdsByProcessDefinitionId = "CaseProcInstBind.getCaseIdsByProcessDefinitionId";
	public static final String getCaseIdsByCaseNumber = "CaseProcInstBind.getCaseIdsByCaseNumber";
	public static final String getCaseIdsByCaseStatus = "CaseProcInstBind.getCaseIdsByCaseStatus";
	public static final String getCaseIdsByProcessUserStatus = "CaseProcInstBind.getCaseIdsByProcessUserStatus";
	public static final String getCaseIdsByUserIds = "CaseProcInstBind.getCaseIdsByUserIds";
	public static final String getCaseIdsByDateRange = "CaseProcInstBind.getCaseIdsByDateRange";
	
	public static final String subProcessNameParam = "subProcessName";
	public static final String caseIdParam = "caseId";
	
	public static final String TABLE_NAME = "BPM_CASES_PROCESSINSTANCES";
	
	public static final String procInstIdColumnName = "process_instance_id";

	public static final String procInstIdProp = "procInstId";
	@Id
	@Column(name=procInstIdColumnName)
    private Long procInstId;
	
	public static final String caseIdProp = "caseId";
	@Column(name="case_id", nullable=false, unique=true)
	private Integer caseId;
	
	public static final String caseIdentierIDProp = "caseIdentierID";
	@Column(name="case_identifier_id", unique=false)
	private Integer caseIdentierID;
	
	public static final String dateCreatedProp = "dateCreated";
	@Column(name="date_created")
	@Temporal(TemporalType.DATE)
	private Date dateCreated;
	
	public static final String processDefinitionIdProp = "processDefinitionId";
	public static final String caseNumberProp = "caseNumber";
	public static final String caseStatusesProp = "caseStatuses";
	public static final String caseStartDateProp = "caseStartDateProp";
	public static final String caseEndDateProp = "caseEndDateProp";
	public static final String processInstanceIdsProp = "processInstanceIdsProp";

	public CaseProcInstBind() { }

	public Long getProcInstId() {
		return procInstId;
	}

	public void setProcInstId(Long procInstId) {
		this.procInstId = procInstId;
	}

	public Integer getCaseId() {
		return caseId;
	}

	public void setCaseId(Integer caseId) {
		this.caseId = caseId;
	}

	public Integer getCaseIdentierID() {
		return caseIdentierID;
	}

	public void setCaseIdentierID(Integer caseIdentierID) {
		this.caseIdentierID = caseIdentierID;
	}

	public Date getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
}