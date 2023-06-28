package com.idega.idegaweb.egov.bpm.data;


import java.io.Serializable;
import java.util.Date;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.idega.block.process.business.ProcessConstants;
import com.idega.block.process.data.CaseBMPBean;
import com.idega.block.process.data.bean.Case;
import com.idega.jbpm.data.BPMVariableData;

import is.idega.idegaweb.egov.bpm.cases.CasesBPMProcessConstants;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.27 $
 *
 * Last modified: $Date: 2009/07/09 15:09:11 $ by $Author: valdas $
 */
@Entity
@Table(name = CaseProcInstBind.TABLE_NAME, indexes = {
		@Index(name = "procInstIdIndex", columnList = CaseProcInstBind.procInstIdColumnName),
		@Index(name = "caseIdIndex", columnList = CaseProcInstBind.caseIdColumnName)
})
@NamedQueries({
	@NamedQuery(name=CaseProcInstBind.BIND_BY_CASEID_QUERY_NAME, query="from CaseProcInstBind bind where bind.caseId = :"+CaseProcInstBind.caseIdParam),
	@NamedQuery(name=CaseProcInstBind.BIND_BY_CASES_IDS_QUERY_NAME, query="from CaseProcInstBind bind where bind.caseId in (:"+CaseProcInstBind.casesIdsParam + ")"),
	@NamedQuery(name=CaseProcInstBind.getProcInstIds_BY_CASES_IDS_QUERY_NAME, query="select cp." + CaseProcInstBind.procInstIdProp + " from CaseProcInstBind cp where cp.caseId in (:"+CaseProcInstBind.casesIdsParam + ")"),
	@NamedQuery(name=CaseProcInstBind.BIND_BY_PROCESSES_IDS_QUERY_NAME, query="from CaseProcInstBind bind where bind.procInstId in (:"+CaseProcInstBind.procInstIdsParam + ")"),
	@NamedQuery(name=CaseProcInstBind.getLatestByDateQN, query="from CaseProcInstBind cp where cp."+CaseProcInstBind.dateCreatedProp+" = :"+CaseProcInstBind.dateCreatedProp+" and cp."+CaseProcInstBind.caseIdentierIDProp+" = (select max(cp2."+CaseProcInstBind.caseIdentierIDProp+") from CaseProcInstBind cp2 where cp2."+CaseProcInstBind.dateCreatedProp+" = cp."+CaseProcInstBind.dateCreatedProp+")"),
	@NamedQuery(name=CaseProcInstBind.getLastCreatedCase, query="from CaseProcInstBind cp where cp."+CaseProcInstBind.dateCreatedProp+" = (select max(cp2."+CaseProcInstBind.dateCreatedProp+") from CaseProcInstBind cp2) order by cp."+CaseProcInstBind.dateCreatedProp + ", cp."+CaseProcInstBind.caseIdentierIDProp),
	@NamedQuery(
			name = CaseProcInstBind.getFirstBindForPrefix,
			query = "from CaseProcInstBind cp where cp." + CaseProcInstBind.caseIdentifierProp + " like :prefix order by cp." + CaseProcInstBind.dateCreatedProp
	),
	@NamedQuery(
			name = CaseProcInstBind.getLatestBindForPrefix,
			query = "from CaseProcInstBind cp where cp." + CaseProcInstBind.dateCreatedProp + " = (select max(cp2." + CaseProcInstBind.dateCreatedProp + ") from CaseProcInstBind cp2) and cp." +
					CaseProcInstBind.caseIdentifierProp + " like :prefix order by cp." + CaseProcInstBind.dateCreatedProp + ", cp." + CaseProcInstBind.caseIdentierIDProp
	),
	@NamedQuery(name=CaseProcInstBind.getByDateCreatedAndCaseIdentifierId, query="select cp, pi from CaseProcInstBind cp, org.jbpm.graph.exe.ProcessInstance pi where cp."+CaseProcInstBind.dateCreatedProp+" in(:"+CaseProcInstBind.dateCreatedProp+") and cp."+CaseProcInstBind.caseIdentierIDProp+" in(:"+CaseProcInstBind.caseIdentierIDProp+") and pi.id = cp."+CaseProcInstBind.procInstIdProp),
	@NamedQuery(name=CaseProcInstBind.getByCaseIdentifier, query="select cp, pi from CaseProcInstBind cp, org.jbpm.graph.exe.ProcessInstance pi where cp."+CaseProcInstBind.caseIdentifierProp +" in(:"+CaseProcInstBind.caseIdentifierProp+") and pi.id = cp."+CaseProcInstBind.procInstIdProp),
	@NamedQuery(name=CaseProcInstBind.getCaseIdByProcessInstanceId, query="select cp." + CaseProcInstBind.caseIdProp + " from CaseProcInstBind cp where cp."+ CaseProcInstBind.procInstIdProp + " = :" + CaseProcInstBind.procInstIdProp),
	@NamedQuery(name=CaseProcInstBind.getCaseIdsByProcessInstanceIds, query = "select cp." + CaseProcInstBind.caseIdProp + " from CaseProcInstBind cp where cp." + CaseProcInstBind.procInstIdProp + " in (:" + CaseProcInstBind.processInstanceIdsProp + ") group by cp." + CaseProcInstBind.caseIdProp + " order by cp." + CaseProcInstBind.dateCreatedProp + " desc"),
	@NamedQuery(name=CaseProcInstBind.getSubprocessTokensByPI, query="select tkn from org.jbpm.graph.exe.Token tkn where tkn.processInstance = :"+CaseProcInstBind.procInstIdProp+" and tkn.subProcessInstance is not null"),
	@NamedQuery(
			name = CaseProcInstBind.QUERY_FIND_PROC_INST_IDS_BY_PROC_DEF_NAMES_AND_CASE_STATUSES,
			query = "select cp.procInstId from com.idega.idegaweb.egov.bpm.data.CaseProcInstBind cp, com.idega.block.process.data.bean.Case pc, org.jbpm.graph.exe.ProcessInstance pi," +
					"org.jbpm.graph.def.ProcessDefinition pd where pd.name in (:procDefNames) and pd.id = pi.processDefinition.id and pi.id = cp.procInstId and pc.caseStatus in (:statuses) and pc.id = cp.caseId" +
					" order by pc.created desc"
	),
	@NamedQuery(
			name = CaseProcInstBind.QUERY_FIND_CASE_IDS_BY_PROC_DEF_NAMES_AND_CASE_STATUSES,
			query = "select cp.caseId from com.idega.idegaweb.egov.bpm.data.CaseProcInstBind cp, com.idega.block.process.data.bean.Case pc, org.jbpm.graph.exe.ProcessInstance pi," +
					"org.jbpm.graph.def.ProcessDefinition pd where pd.name in (:procDefNames) and pd.id = pi.processDefinition.id and pi.id = cp.procInstId and pc.caseStatus in (:statuses) and pc.id = cp.caseId" +
					" order by pc.created desc"
	),
	@NamedQuery(name=CaseProcInstBind.QUERY_GET_CASE_ID_BY_PROC_INST_ID, query="select cp." + CaseProcInstBind.caseIdProp + " from CaseProcInstBind cp where cp.procInstId  = :" + CaseProcInstBind.procInstIdProp),
	@NamedQuery(
			name = CaseProcInstBind.QUERY_FIND_ALL_UUIDS,
			query = "select cp.uuid from CaseProcInstBind cp where cp.uuid is not null"
	)
})

@SqlResultSetMappings({
	@SqlResultSetMapping(name="caseId", columns=@ColumnResult(name="caseId")),
	@SqlResultSetMapping(name=CaseProcInstBind.procInstIdProp, columns=@ColumnResult(name=CaseProcInstBind.procInstIdProp)),
	@SqlResultSetMapping(name="case_proc_bind", entities=@EntityResult(entityClass=CaseProcInstBind.class))
})
@NamedNativeQueries({
			@NamedNativeQuery(name=CaseProcInstBind.getProcInstIdsByCaseStatusesAndProcDefNames, resultSetMapping=CaseProcInstBind.procInstIdProp,
					query= "select cp." + CaseProcInstBind.procInstIdColumnName + " " + CaseProcInstBind.procInstIdProp + " from " + CaseProcInstBind.TABLE_NAME + " cp " +
					"inner join " + CaseBMPBean.TABLE_NAME + " pc on cp.case_id = pc.PROC_CASE_ID inner join JBPM_PROCESSINSTANCE pi on cp." + CaseProcInstBind.procInstIdColumnName + " = pi.id_ " +
					"inner join JBPM_PROCESSDEFINITION pd on pi.PROCESSDEFINITION_ = pd.id_ where pd.name_ in (:" + CaseProcInstBind.processDefinitionNameProp +
					") and pc.CASE_STATUS in (:" + CaseProcInstBind.caseStatusParam + ")"
			),
			@NamedNativeQuery(name=CaseProcInstBind.getProcInstIdsByCaseStatusesAndProcDefNamesNewestOnTop, resultSetMapping=CaseProcInstBind.procInstIdProp,
					query= "select cp.procInstId from " + CaseProcInstBind.TABLE_NAME + " cp, " + Case.ENTITY_NAME + " pc, jbpm_processinstance pi, " +
					"jbpm_processdefinition pd where pd.name in (:" + CaseProcInstBind.processDefinitionNameProp + ") and pc.caseStatus in (:" + CaseProcInstBind.caseStatusParam +
					") and pi.processDefinition.id = pd.id and pi.id = cp.procInstId and cp.caseId = pc.id order by pc.created desc"
			),

			@NamedNativeQuery(name=CaseProcInstBind.getCountedProcInstIdsByCaseStatusesAndProcDefNames,
					query= "select count(cp." + CaseProcInstBind.procInstIdColumnName + ") from " + CaseProcInstBind.TABLE_NAME + " cp " +
					"inner join " + CaseBMPBean.TABLE_NAME + " pc on cp.case_id = pc.PROC_CASE_ID inner join JBPM_PROCESSINSTANCE pi on cp." + CaseProcInstBind.procInstIdColumnName + " = pi.id_ " +
					"inner join JBPM_PROCESSDEFINITION pd on pi.PROCESSDEFINITION_ = pd.id_ where pd.name_ in (:" + CaseProcInstBind.processDefinitionNameProp +
					") and pc.CASE_STATUS in (:" + CaseProcInstBind.caseStatusParam + ")"
			),

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
				"inner join "+ ProcessUserBind.TABLE_NAME+" pu " +
				"on cp."+CaseProcInstBind.procInstIdColumnName+" = pu.process_instance_id " +
				"where cp."+CaseProcInstBind.procInstIdColumnName+" in (:"+CaseProcInstBind.procInstIdProp+") and pu.user_status = :" +
				ProcessUserBind.statusProp
			),
			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByProcessUserStatus, resultSetMapping="caseId",
					query=
				"select cp.case_id caseId from " + CaseProcInstBind.TABLE_NAME+" cp inner join " + ProcessUserBind.TABLE_NAME + " pu on cp." +
				CaseProcInstBind.procInstIdColumnName + " = pu.process_instance_id where pu.user_status = :" + ProcessUserBind.statusProp
			),
			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndName, resultSetMapping="caseId",
					query=
				"select cp.case_id caseId from JBPM_PROCESSINSTANCE pi inner join JBPM_PROCESSDEFINITION pd on pi.PROCESSDEFINITION_ = pd.id_ inner join " +
				CaseProcInstBind.TABLE_NAME + " cp on pi.ID_ = cp." + CaseProcInstBind.procInstIdColumnName + " where pd.NAME_ = :" + CaseProcInstBind.processDefinitionNameProp +
				" group by cp.case_id"
			),
			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByProcessDefinitionName, resultSetMapping="caseId",
					query=
				"select cp.case_id caseId from JBPM_PROCESSINSTANCE pi inner join JBPM_PROCESSDEFINITION pd on pi.PROCESSDEFINITION_ = pd.id_ inner join " +
				CaseProcInstBind.TABLE_NAME + " cp on pi.ID_ = cp." + CaseProcInstBind.procInstIdColumnName + " where pd.NAME_ = :" +
				CaseProcInstBind.processDefinitionNameProp + " group by cp.case_id"
			),

			/** Variable queries start **/
			//	Query to case IDs by DATE variables
			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndNameAndDateVariables, resultSetMapping="caseId",
					query=
				"select cp.case_id caseId from JBPM_PROCESSINSTANCE pi inner join JBPM_PROCESSDEFINITION pd on pi.PROCESSDEFINITION_ = pd.id_ inner join " +
				CaseProcInstBind.TABLE_NAME + " cp on pi.ID_ = cp." + CaseProcInstBind.procInstIdColumnName + " inner join JBPM_VARIABLEINSTANCE var on pi.ID_ =" +
				" var.PROCESSINSTANCE_ where pd.NAME_ = :" + CaseProcInstBind.processDefinitionNameProp + " and var.CLASS_ in (:" + CaseProcInstBind.variablesTypesProp +
				") and var.NAME_ = :" + CaseProcInstBind.variablesNamesProp + " and var.DATEVALUE_ between :" + CaseProcInstBind.variablesValuesProp + " and :" +
				CaseProcInstBind.variablesValuesPropEnd + " group by cp.case_id"
			),
			//	Query to case IDs by DOUBLE variables
			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndNameAndDoubleVariables, resultSetMapping="caseId",
					query=
				"select cp.case_id caseId from JBPM_PROCESSINSTANCE pi inner join JBPM_PROCESSDEFINITION pd on pi.PROCESSDEFINITION_ = pd.id_ inner join " +
				CaseProcInstBind.TABLE_NAME + " cp on pi.ID_ = cp." + CaseProcInstBind.procInstIdColumnName + " inner join JBPM_VARIABLEINSTANCE var on pi.ID_ =" +
				" var.PROCESSINSTANCE_ where pd.NAME_ = :" + CaseProcInstBind.processDefinitionNameProp + " and var.CLASS_ in (:" + CaseProcInstBind.variablesTypesProp +
				") and var.NAME_ = :" + CaseProcInstBind.variablesNamesProp + " and var.DOUBLEVALUE_ = :" + CaseProcInstBind.variablesValuesProp + " group by cp.case_id"
			),
			//	Query to case IDs by LONG variables
			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndNameAndLongVariables, resultSetMapping="caseId",
					query=
				"select cp.case_id caseId from JBPM_PROCESSINSTANCE pi inner join JBPM_PROCESSDEFINITION pd on pi.PROCESSDEFINITION_ = pd.id_ inner join " +
				CaseProcInstBind.TABLE_NAME + " cp on pi.ID_ = cp." + CaseProcInstBind.procInstIdColumnName + " inner join JBPM_VARIABLEINSTANCE var on pi.ID_ =" +
				" var.PROCESSINSTANCE_ where pd.NAME_ = :" + CaseProcInstBind.processDefinitionNameProp + " and var.CLASS_ in (:" + CaseProcInstBind.variablesTypesProp +
				") and var.NAME_ = :" + CaseProcInstBind.variablesNamesProp + " and var.LONGVALUE_ = :" + CaseProcInstBind.variablesValuesProp + " group by cp.case_id"
			),
			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndNameAndLongVariablesLastValue, resultSetMapping="caseId",
			query=
				"select cp.case_id caseId from JBPM_PROCESSINSTANCE pi inner join JBPM_PROCESSDEFINITION pd on pi.PROCESSDEFINITION_ = pd.id_ inner join " +
				CaseProcInstBind.TABLE_NAME + " cp on pi.ID_ = cp." + CaseProcInstBind.procInstIdColumnName + " inner join JBPM_VARIABLEINSTANCE var on pi.ID_ =" +
				" var.PROCESSINSTANCE_ left join JBPM_VARIABLEINSTANCE var2 on var.PROCESSINSTANCE_ = var2.PROCESSINSTANCE_ and var.NAME_ = var2.NAME_ and var.ID_ < var2.ID_ " +
				"where pd.NAME_ = :" + CaseProcInstBind.processDefinitionNameProp + " and var.CLASS_ in (:" + CaseProcInstBind.variablesTypesProp +
				") and var.NAME_ = :" + CaseProcInstBind.variablesNamesProp + " and var.LONGVALUE_ = :" + CaseProcInstBind.variablesValuesProp + " and var2.ID_ is null group by cp.case_id"
			),
			//	Queries to case IDs by STRING variables
			//	Using mirrow table
			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndNameAndStringVariables, resultSetMapping="caseId",
					query=
				"select cp.case_id caseId from " + CaseProcInstBind.TABLE_NAME + " cp inner join JBPM_PROCESSINSTANCE pi on cp." +
				CaseProcInstBind.procInstIdColumnName + " = pi.ID_ inner join JBPM_PROCESSDEFINITION pd on pi.PROCESSDEFINITION_ = pd.ID_ " +
				"inner join JBPM_VARIABLEINSTANCE var on pi.ID_ = var.PROCESSINSTANCE_ inner join " + BPMVariableData.TABLE_NAME + " vdata on var.ID_ = " +
				"vdata.variable_id where pd.NAME_ = :" + CaseProcInstBind.processDefinitionNameProp + " and var.CLASS_ in (:" +	CaseProcInstBind.variablesTypesProp +
				") and var.NAME_ = :" + CaseProcInstBind.variablesNamesProp + " and lower(vdata.stringvalue) like :" + CaseProcInstBind.variablesValuesProp +
				" group by cp.case_id"
			),
			//	Not using a mirrow table
			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndNameAndStringVariablesNoMirrow, resultSetMapping="caseId",
					query=
				"select cp.case_id caseId from " + CaseProcInstBind.TABLE_NAME + " cp inner join JBPM_PROCESSINSTANCE pi on cp." +
				CaseProcInstBind.procInstIdColumnName + " = pi.ID_ inner join JBPM_PROCESSDEFINITION pd on pi.PROCESSDEFINITION_ = pd.ID_ " +
				"inner join JBPM_VARIABLEINSTANCE var on pi.ID_ = var.PROCESSINSTANCE_ where pd.NAME_ = :" + CaseProcInstBind.processDefinitionNameProp +
				" and var.CLASS_ in (:" +	CaseProcInstBind.variablesTypesProp + ") and var.NAME_ = :" + CaseProcInstBind.variablesNamesProp +
				" and lower(var.STRINGVALUE_) like :" + CaseProcInstBind.variablesValuesProp + " group by cp.case_id"
			),
			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndNameAndStringVariablesNoMirrowLast, resultSetMapping="caseId",
			query=
				"select cp.case_id caseId from " + CaseProcInstBind.TABLE_NAME + " cp inner join JBPM_PROCESSINSTANCE pi on cp." +
				CaseProcInstBind.procInstIdColumnName + " = pi.ID_ inner join JBPM_PROCESSDEFINITION pd on pi.PROCESSDEFINITION_ = pd.ID_ " +
				"inner join JBPM_VARIABLEINSTANCE var on pi.ID_ = var.PROCESSINSTANCE_ left join JBPM_VARIABLEINSTANCE var2 on var.PROCESSINSTANCE_ = var2.PROCESSINSTANCE_ and var.NAME_ = var2.NAME_ and var.ID_ < var2.ID_ " +
				" where pd.NAME_ = :" + CaseProcInstBind.processDefinitionNameProp +
				" and var.CLASS_ in (:" +	CaseProcInstBind.variablesTypesProp + ") and var.NAME_ = :" + CaseProcInstBind.variablesNamesProp +
				" and lower(var.STRINGVALUE_) like :" + CaseProcInstBind.variablesValuesProp + " and var2.ID_ is null group by cp.case_id"
			),
			/** Variable queries end **/

			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByCaseNumber, resultSetMapping="caseId",
					query=
				"select cp.case_id caseId from " + CaseProcInstBind.TABLE_NAME + " cp inner join JBPM_VARIABLEINSTANCE var on cp." +
				CaseProcInstBind.procInstIdColumnName + " = var.PROCESSINSTANCE_ inner join " + BPMVariableData.TABLE_NAME + " vdata on var.ID_ = vdata.variable_id " +
				"where lower(vdata.stringvalue) like :" + CaseProcInstBind.caseNumberProp + " and var.NAME_ = '" + ProcessConstants.CASE_IDENTIFIER + "' " +
				"group by cp.case_id"
			),
			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByCaseNumberNoMirrow, resultSetMapping="caseId",
					query=
				"select cp.case_id caseId from " + CaseProcInstBind.TABLE_NAME + " cp inner join JBPM_VARIABLEINSTANCE var on cp." +
				CaseProcInstBind.procInstIdColumnName + " = var.PROCESSINSTANCE_ where lower(var.STRINGVALUE_) like :" + CaseProcInstBind.caseNumberProp + " and var.NAME_ = '" +
				ProcessConstants.CASE_IDENTIFIER + "' group by cp.case_id"
			),

			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByCaseStatus, resultSetMapping="caseId",
					query=
				"select cp.case_id caseId from " + CaseProcInstBind.TABLE_NAME + " cp inner join JBPM_VARIABLEINSTANCE var on cp." +
				CaseProcInstBind.procInstIdColumnName + " = var.PROCESSINSTANCE_ inner join " + BPMVariableData.TABLE_NAME + " vdata on var.ID_ = vdata.variable_id"+
				" where var.NAME_ = '" + CasesBPMProcessConstants.caseStatusVariableName + "' and vdata.stringvalue in (:" + CaseProcInstBind.caseStatusesProp +
				") group by cp.case_id"
			),
			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByCaseStatusNoMirrow, resultSetMapping="caseId",
					query=
				"select cp.case_id caseId from " + CaseProcInstBind.TABLE_NAME + " cp inner join JBPM_VARIABLEINSTANCE var on cp." +
				CaseProcInstBind.procInstIdColumnName + " = var.PROCESSINSTANCE_ where var.NAME_ = '" + CasesBPMProcessConstants.caseStatusVariableName +
				"' and var.STRINGVALUE_ in (:" + CaseProcInstBind.caseStatusesProp + ") group by cp.case_id"
			),

			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByUserIds, resultSetMapping="caseId",
					query=
				"select cp.case_id caseId from " + CaseProcInstBind.TABLE_NAME + " cp inner join JBPM_TASKINSTANCE ti on cp." +
				CaseProcInstBind.procInstIdColumnName + " = ti.PROCINST_ where ti.ACTORID_ in (:" + ProcessUserBind.userIdParam + ") and ti.END_ is not null group" +
				" by cp.case_id"
			),
			@NamedNativeQuery(name=CaseProcInstBind.getCaseIdsByDateRange, resultSetMapping="caseId",
					query=
				"select cp.case_id caseId from " + CaseProcInstBind.TABLE_NAME + " cp inner join JBPM_PROCESSINSTANCE pi on cp." +
				CaseProcInstBind.procInstIdColumnName + " = pi.id_ where pi.start_ between :" + CaseProcInstBind.caseStartDateProp + " and :" +
				CaseProcInstBind.caseEndDateProp
			)
	}
)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class CaseProcInstBind implements Serializable {

	private static final long serialVersionUID = -335682330238243547L;

	public static final String BIND_BY_CASEID_QUERY_NAME = "CaseProcInstBind.bindByCaseIdQuery";
	public static final String BIND_BY_CASES_IDS_QUERY_NAME = "CaseProcInstBind.bindByCasesIdsQuery",
								getProcInstIds_BY_CASES_IDS_QUERY_NAME = "CaseProcInstBind.getProcInstIdsByCasesIdsQuery",
								BIND_BY_PROCESSES_IDS_QUERY_NAME = "CaseProcInstBind.bindByProcInstIdsQuery";
	public static final String getLatestByDateQN = "CaseProcInstBind.getLatestByDate";
	public static final String getLastCreatedCase = "CaseProcInstBind.getLastCreatedCase";
	public static final String getFirstBindForPrefix = "CaseProcInstBind.getFirstBindForPrefix";
	public static final String getLatestBindForPrefix = "CaseProcInstBind.getLatestBindForPrefix";
	public static final String getCaseIdByProcessInstanceId="CaseProcInstBind.getCaseIdByPIID";
	public static final String getCaseIdsByProcessInstanceIds = "CaseProcInstBind.getCaseIdsByPIIDs";
	public static final String getByDateCreatedAndCaseIdentifierId = "CaseProcInstBind.getByDateCreatedAndCaseIdentifierId";
	public static final String getSubprocessTokensByPI = "CaseProcInstBind.getSubprocessTokensByPI";
	public static final String QUERY_FIND_PROC_INST_IDS_BY_PROC_DEF_NAMES_AND_CASE_STATUSES = "CaseProcInstBind.findProcInstIdsByProcDefNamesAndCaseStatuses";
	public static final String QUERY_FIND_CASE_IDS_BY_PROC_DEF_NAMES_AND_CASE_STATUSES = "CaseProcInstBind.findCaseIdsByProcDefNamesAndCaseStatuses";
	public static final String getCaseIdsByProcessInstanceIdsProcessInstanceEnded = "CaseProcInstBind.getCaseIdsByProcessInstanceIdsProcessInstanceEnded";
	public static final String getCaseIdsByProcessInstanceIdsProcessInstanceNotEnded = "CaseProcInstBind.getCaseIdsByProcessInstanceIdsProcessInstanceNotEnded";
	public static final String getCaseIdsByProcessInstanceIdsAndProcessUserStatus = "CaseProcInstBind.getCaseIdsByProcessInstanceIdsAndProcessUserStatus";
	public static final String getCaseIdsByProcessDefinitionIdsAndName = "CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndName";
	public static final String getCaseIdsByProcessDefinitionName = "CaseProcInstBind.getCaseIdsByProcessDefinitionName";
	public static final String getCaseIdsByProcessDefinitionIdsAndNameAndStringVariables = "CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndNameAndStringVariables";
	public static final String getCaseIdsByProcessDefinitionIdsAndNameAndStringVariablesNoMirrow = getCaseIdsByProcessDefinitionIdsAndNameAndStringVariables + "NoMirrow";
	public static final String getCaseIdsByProcessDefinitionIdsAndNameAndStringVariablesNoMirrowLast = getCaseIdsByProcessDefinitionIdsAndNameAndStringVariablesNoMirrow + "Last";
	public static final String getCaseIdsByProcessDefinitionIdsAndNameAndDateVariables = "CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndNameAndDateVariables";
	public static final String getCaseIdsByProcessDefinitionIdsAndNameAndDoubleVariables = "CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndNameAndDoubleVariables";
	public static final String getCaseIdsByProcessDefinitionIdsAndNameAndLongVariables = "CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndNameAndLongVariables";
	public static final String getCaseIdsByProcessDefinitionIdsAndNameAndLongVariablesLastValue = "CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndNameAndLongVariablesLastValue";
	public static final String getCaseIdsByCaseNumber = "CaseProcInstBind.getCaseIdsByCaseNumber";
	public static final String getCaseIdsByCaseNumberNoMirrow = getCaseIdsByCaseNumber + "NoMirrow";
	public static final String getCaseIdsByCaseStatus = "CaseProcInstBind.getCaseIdsByCaseStatus";
	public static final String getCaseIdsByCaseStatusNoMirrow = getCaseIdsByCaseStatus + "NoMirrow";
	public static final String getCaseIdsByProcessUserStatus = "CaseProcInstBind.getCaseIdsByProcessUserStatus";
	public static final String getCaseIdsByUserIds = "CaseProcInstBind.getCaseIdsByUserIds";
	public static final String getCaseIdsByDateRange = "CaseProcInstBind.getCaseIdsByDateRange";
	public static final String getByCaseIdentifier = "CaseProcInstBind.getByCaseIdentifier";
	public static final String getProcInstIdsByCaseStatusesAndProcDefNames = "CaseProcInstBind.getProcInstIdsByCaseStatus";
	public static final String getProcInstIdsByCaseStatusesAndProcDefNamesNewestOnTop = "CaseProcInstBind.getProcInstIdsByCaseStatusNewestOnTop";
	public static final String getCountedProcInstIdsByCaseStatusesAndProcDefNames = "CaseProcInstBind.getCountedProcInstIdsByCaseStatus",
								QUERY_GET_CASE_ID_BY_PROC_INST_ID = "CaseProcInstBind.getCaseIdByProcInstId",
								QUERY_FIND_ALL_UUIDS = "CaseProcInstBind.findAllUUIDs";

	public static final String subProcessNameParam = "subProcessName";
	public static final String caseIdParam = "caseId";
	public static final String casesIdsParam = "casesIds",
								procInstIdsParam = "procInstIds";
	public static final String caseStatusParam = "caseStatus";

	public static final String TABLE_NAME = "BPM_CASES_PROCESSINSTANCES";

	public static final String	procInstIdColumnName = "process_instance_id",
								caseIdColumnName = "case_id";

	public static final String procInstIdProp = "procInstId";
	@Id
	@Column(name=procInstIdColumnName)
    private Long procInstId;

	public static final String caseIdProp = "caseId";
	@Column(name=caseIdColumnName, nullable=false, unique=true)
	private Integer caseId;

	public static final String caseIdentierIDProp = "caseIdentierID";
	@Column(name="case_identifier_id", unique=false)
	private Integer caseIdentierID;

	public static final String dateCreatedProp = "dateCreated";

	/**
	 * this date means identifier creation date, NOT PROCESS/case creation date.
	 */
	@Column(name="date_created")
	@Temporal(TemporalType.DATE)
	private Date dateCreated;

	public static final String caseIdentifierProp = "caseIdentifier";
	@Column(name="case_identifier", unique=false)
	private String caseIdentifier;

	public static final String uuidProp = "uuid";
	public static final String COLUMN_UUID = "UUID";
	@Column(name = COLUMN_UUID,
			unique = true,
			nullable = true)
	private String uuid;

	public static final String processDefinitionIdsProp = "processDefinitionIds";
	public static final String processDefinitionNameProp = "processDefinitionName";
	public static final String caseNumberProp = "caseNumber";
	public static final String caseStatusesProp = "caseStatuses";
	public static final String caseStartDateProp = "caseStartDateProp";
	public static final String caseEndDateProp = "caseEndDateProp";
	public static final String processInstanceIdsProp = "processInstanceIdsProp";
	public static final String processInstanceIdProp = "processInstanceIdProp";
	public static final String variablesNamesProp = "variablesSelectProp";
	public static final String variablesValuesProp = "variablesValuesProp";
	public static final String variablesValuesPropEnd = "variablesValuesPropEnd";
	public static final String variablesTypesProp = "variablesTypesProp";

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

	/**
	 * this date means identifier creation date, NOT PROCESS/case creation date.
	 */
	public Date getDateCreated() {
		return dateCreated;
	}

	/**
	 * this date means identifier creation date, NOT PROCESS/case creation date.
	 */
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	public String getCaseIdentifier() {
    	return caseIdentifier;
    }

	public void setCaseIdentifier(String caseIdentifier) {
    	this.caseIdentifier = caseIdentifier;
    }

	/**
	 * @return process instance id in Camunda or <code>null</code> if BPM 1 was used
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * @param uuid is process instance id in Camunda or <code>null</code> if BPM 1 was used
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	@Override
	public String toString() {
		return "Case ID: " + getCaseId() + ", proc. inst. ID: " + getProcInstId() + ", UUID: " + getUuid();
	}
}