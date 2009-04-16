package com.idega.idegaweb.egov.bpm.data.dao.impl;

import is.idega.idegaweb.egov.bpm.cases.presentation.beans.BPMProcessVariable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.context.exe.VariableInstance;
import org.jbpm.graph.exe.Token;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.persistence.Param;
import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.core.user.data.User;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.CaseTypesProcDefBind;
import com.idega.idegaweb.egov.bpm.data.ProcessUserBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.presentation.ui.handlers.IWDatePickerHandler;
import com.idega.util.CoreConstants;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.42 $ Last modified: $Date: 2009/04/16 13:28:29 $ by $Author: civilis $
 */
@Scope("singleton")
@Repository("casesBPMDAO")
@Transactional(readOnly = true)
public class CasesBPMDAOImpl extends GenericDaoImpl implements CasesBPMDAO {
	
	private static final Logger LOGGER = Logger.getLogger(CasesBPMDAOImpl.class
	        .getName());
	
	public List<CaseTypesProcDefBind> getAllCaseTypes() {
		
		@SuppressWarnings("unchecked")
		List<CaseTypesProcDefBind> casesProcesses = getEntityManager()
		        .createNamedQuery(CaseTypesProcDefBind.CASES_PROCESSES_GET_ALL)
		        .getResultList();
		
		return casesProcesses;
	}
	
	public CaseProcInstBind getCaseProcInstBindByCaseId(Integer caseId) {
		
		@SuppressWarnings("unchecked")
		List<CaseProcInstBind> l = getEntityManager().createNamedQuery(
		    CaseProcInstBind.BIND_BY_CASEID_QUERY_NAME).setParameter(
		    CaseProcInstBind.caseIdParam, caseId).getResultList();
		
		if (l.isEmpty())
			return null;
		
		return l.iterator().next();
	}
	
	public List<CaseProcInstBind> getCasesProcInstBindsByCasesIds(
	        List<Integer> casesIds) {
		List<CaseProcInstBind> binds = getResultList(
		    CaseProcInstBind.BIND_BY_CASES_IDS_QUERY_NAME,
		    CaseProcInstBind.class, new Param(CaseProcInstBind.casesIdsParam,
		            casesIds));
		
		return binds;
	}
	
	@Transactional(readOnly = false)
	public ProcessUserBind getProcessUserBind(long processInstanceId,
	        int userId, boolean createIfNotFound) {
		
		@SuppressWarnings("unchecked")
		List<ProcessUserBind> u = getEntityManager().createNamedQuery(
		    ProcessUserBind.byUserIdNPID).setParameter(
		    ProcessUserBind.pidParam, processInstanceId).setParameter(
		    ProcessUserBind.userIdParam, userId).getResultList();
		
		if (u.isEmpty() && createIfNotFound) {
			
			CaseProcInstBind bind = find(CaseProcInstBind.class,
			    processInstanceId);
			
			if (bind != null) {
				
				ProcessUserBind cu = new ProcessUserBind();
				cu.setCaseProcessBind(bind);
				cu.setUserId(userId);
				persist(cu);
				return cu;
				
			} else
				throw new IllegalStateException(
				        "Case not bound to process instance");
			
		} else if (!u.isEmpty()) {
			
			return u.iterator().next();
			
		} else
			return null;
	}
	
	public List<ProcessUserBind> getProcessUserBinds(int userId,
	        Collection<Integer> casesIds) {
		
		if (casesIds.isEmpty())
			return new ArrayList<ProcessUserBind>(0);
		
		@SuppressWarnings("unchecked")
		List<ProcessUserBind> u = getEntityManager().createNamedQuery(
		    ProcessUserBind.byUserIdAndCaseId).setParameter(
		    ProcessUserBind.userIdParam, userId).setParameter(
		    ProcessUserBind.casesIdsParam, casesIds).getResultList();
		
		return u;
	}
	
	public CaseTypesProcDefBind getCaseTypesProcDefBindByPDName(String pdName) {
		
		@SuppressWarnings("unchecked")
		List<CaseTypesProcDefBind> u = getEntityManager().createNamedQuery(
		    CaseTypesProcDefBind.CASES_PROCESSES_GET_BY_PDNAME).setParameter(
		    CaseTypesProcDefBind.procDefNamePropName, pdName).getResultList();
		
		if (!u.isEmpty())
			return u.iterator().next();
		
		return null;
	}
	
	@Transactional(readOnly = false)
	public void updateCaseTypesProcDefBind(CaseTypesProcDefBind bind) {
		
		getEntityManager().merge(bind);
	}
	
	public CaseProcInstBind getCaseProcInstBindLatestByDateQN(Date date) {
		
		CaseProcInstBind b = null;
		
		if (date != null) {
			
			@SuppressWarnings("unchecked")
			List<CaseProcInstBind> u = getEntityManager().createNamedQuery(
			    CaseProcInstBind.getLatestByDateQN).setParameter(
			    CaseProcInstBind.dateCreatedProp, date).getResultList();
			
			if (!u.isEmpty())
				b = u.iterator().next();
		}
		
		return b;
	}
	
	public List<Object[]> getCaseProcInstBindProcessInstanceByDateCreatedAndCaseIdentifierId(
	        Collection<Date> dates, Collection<Integer> identifierIDs) {
		
		List<Object[]> cps = null;
		
		if (dates != null && !dates.isEmpty() && identifierIDs != null
		        && !identifierIDs.isEmpty()) {
			
			@SuppressWarnings("unchecked")
			List<Object[]> u = getEntityManager().createNamedQuery(
			    CaseProcInstBind.getByDateCreatedAndCaseIdentifierId)
			        .setParameter(CaseProcInstBind.dateCreatedProp, dates)
			        .setParameter(CaseProcInstBind.caseIdentierIDProp,
			            identifierIDs).getResultList();
			
			cps = u;
		} else
			cps = new ArrayList<Object[]>(0);
		
		return cps;
	}
	
	public List<Object[]> getCaseProcInstBindProcessInstanceByCaseIdentifier(
	        Collection<String> identifiers) {
		
		List<Object[]> cps = null;
		
		if (identifiers != null && !identifiers.isEmpty()) {
			
			@SuppressWarnings("unchecked")
			List<Object[]> u = getEntityManager().createNamedQuery(
			    CaseProcInstBind.getByCaseIdentifier).setParameter(
			    CaseProcInstBind.caseIdentifierProp, identifiers)
			        .getResultList();
			
			cps = u;
		} else
			cps = new ArrayList<Object[]>(0);
		
		return cps;
	}
	
	public List<Token> getCaseProcInstBindSubprocessBySubprocessName(
	        Long processInstanceId) {
		
		if (processInstanceId != null) {
			
			@SuppressWarnings("unchecked")
			List<Token> u = getEntityManager().createNamedQuery(
			    CaseProcInstBind.getSubprocessTokensByPI).setParameter(
			    CaseProcInstBind.procInstIdProp, processInstanceId)
			        .getResultList();
			
			return u;
			
		} else
			return new ArrayList<Token>(0);
	}
	
	public List<Long> getCaseIdsByProcessDefinitionIdsAndNameAndVariables(
	        List<Long> processDefinitionIds, String processDefinitionName,
	        List<BPMProcessVariable> variables) {
		if (ListUtil.isEmpty(processDefinitionIds)
		        || StringUtil.isEmpty(processDefinitionName)) {
			return null;
		}
		
		if (ListUtil.isEmpty(variables)) {
			return getResultList(
			    CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndName,
			    Long.class, new Param(
			            CaseProcInstBind.processDefinitionIdsProp,
			            processDefinitionIds), new Param(
			            CaseProcInstBind.processDefinitionNameProp,
			            processDefinitionName));
		}
		
		List<Long> allResults = null;
		List<Long> variableResults = null;
		for (BPMProcessVariable variable : variables) {
			variableResults = null;
			Object value = getVariableValue(variable);
			
			// Date
			if (variable.isDateType()) {
				if (value instanceof Date) {
					variableResults = getCaseIdsByVariable(
					    CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndNameAndDateVariables,
					    processDefinitionIds, processDefinitionName, variable
					            .getName(), value,
					    BPMProcessVariable.DATE_TYPES);
				}
				// Double
			} else if (variable.isDoubleType()) {
				if (value instanceof Double) {
					variableResults = getCaseIdsByVariable(
					    CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndNameAndDoubleVariables,
					    processDefinitionIds, processDefinitionName, variable
					            .getName(), value,
					    BPMProcessVariable.DOUBLE_TYPES);
				}
				// Long
			} else if (variable.isLongType()) {
				if (value instanceof Long) {
					variableResults = getCaseIdsByVariable(
					    CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndNameAndLongVariables,
					    processDefinitionIds, processDefinitionName, variable
					            .getName(), value,
					    BPMProcessVariable.LONG_TYPES);
				}
				// String
			} else if (variable.isStringType()) {
				if (value instanceof String) {
					variableResults = getCaseIdsByVariable(
					    CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndNameAndStringVariables,
					    processDefinitionIds, processDefinitionName, variable
					            .getName(), value,
					    BPMProcessVariable.STRING_TYPES);
				}
				// Unsupported variable
			} else {
				LOGGER.warning(new StringBuilder("Unsupported variable: ")
				        .append(variable).append(", terminating search!")
				        .toString());
				return null; // Unsupported variable!
			}
			
			if (ListUtil.isEmpty(variableResults)) {
				LOGGER.warning(new StringBuilder("No results by variable: ")
				        .append(variable).append(", terminating search!")
				        .toString());
				return null; // To keep AND
			}
			
			if (ListUtil.isEmpty(allResults)) {
				allResults = new ArrayList<Long>(variableResults);
			} else {
				allResults.retainAll(variableResults);
			}
			
			if (ListUtil.isEmpty(allResults)) {
				return null;
			}
		}
		
		return allResults;
	}
	
	public List<Long> getCaseIdsByProcessDefinition(String processDefinitionName) {
		if (StringUtil.isEmpty(processDefinitionName)) {
			return null;
		}
		
		return getResultList(
		    CaseProcInstBind.getCaseIdsByProcessDefinitionName, Long.class,
		    new Param(CaseProcInstBind.processDefinitionNameProp,
		            processDefinitionName));
	}
	
	private List<Long> getCaseIdsByVariable(String queryName,
	        List<Long> processDefinitionIds, String processDefinitionName,
	        String variableName, Object value, List<String> types) {
		return getResultList(queryName, Long.class,
		    new Param(CaseProcInstBind.processDefinitionIdsProp,
		            processDefinitionIds), new Param(
		            CaseProcInstBind.processDefinitionNameProp,
		            processDefinitionName), new Param(
		            CaseProcInstBind.variablesNamesProp, variableName),
		    new Param(CaseProcInstBind.variablesValuesProp, value), new Param(
		            CaseProcInstBind.variablesTypesProp, new HashSet<String>(
		                    types)));
	}
	
	private Object getVariableValue(BPMProcessVariable variable) {
		if (variable.isStringType()) {
			return variable.getValue();
		}
		
		if (variable.isDateType()) {
			return IWDatePickerHandler.getParsedDateByCurrentLocale(variable
			        .getValue());
		}
		
		if (variable.isDoubleType()) {
			try {
				return Double.valueOf(variable.getValue());
			} catch (NumberFormatException e) {
				LOGGER.log(Level.WARNING, "Error converting string to double: "
				        + variable.getValue(), e);
			}
		}
		
		if (variable.isLongType()) {
			try {
				return Long.valueOf(variable.getValue());
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error converting string to long: "
				        + variable.getValue(), e);
			}
		}
		
		return null;
	}
	
	public List<Long> getCaseIdsByCaseNumber(String caseNumber) {
		if (caseNumber == null || CoreConstants.EMPTY.equals(caseNumber)) {
			return new ArrayList<Long>(0);
		}
		
		if (!caseNumber.startsWith(CoreConstants.PERCENT)) {
			caseNumber = CoreConstants.PERCENT + caseNumber;
		}
		if (!caseNumber.endsWith(CoreConstants.PERCENT)) {
			caseNumber = caseNumber + CoreConstants.PERCENT;
		}
		
		return getResultList(CaseProcInstBind.getCaseIdsByCaseNumber,
		    Long.class, new Param(CaseProcInstBind.caseNumberProp, caseNumber));
	}
	
	public List<Long> getCaseIdsByProcessUserStatus(String status) {
		if (status == null || CoreConstants.EMPTY.equals(status)) {
			return null;
		}
		
		return getResultList(CaseProcInstBind.getCaseIdsByProcessUserStatus,
		    Long.class, new Param(ProcessUserBind.statusProp, status));
	}
	
	public List<Long> getCaseIdsByCaseStatus(String[] statuses) {
		if (statuses == null || statuses.length == 0) {
			return null;
		}
		
		HashSet<String> statusesInSet = new HashSet<String>(statuses.length);
		for (int i = 0; i < statuses.length; i++) {
			statusesInSet.add(statuses[i]);
		}
		
		return getResultList(CaseProcInstBind.getCaseIdsByCaseStatus,
		    Long.class, new Param(CaseProcInstBind.caseStatusesProp,
		            statusesInSet));
	}
	
	public List<Long> getCaseIdsByUserIds(String userId) {
		if (StringUtil.isEmpty(userId)) {
			return null;
		}
		
		return getResultList(CaseProcInstBind.getCaseIdsByUserIds, Long.class,
		    new Param(ProcessUserBind.userIdParam, userId));
	}
	
	public List<Long> getCaseIdsByDateRange(IWTimestamp dateFrom,
	        IWTimestamp dateTo) {
		if (dateFrom == null || dateTo == null) {
			return null;
		}
		
		return getResultList(CaseProcInstBind.getCaseIdsByDateRange,
		    Long.class, new Param(CaseProcInstBind.caseStartDateProp, dateFrom
		            .getTimestamp().toString()), new Param(
		            CaseProcInstBind.caseEndDateProp, dateTo.getTimestamp()
		                    .toString()));
	}
	
	public List<Long> getCaseIdsByProcessInstanceIds(
	        List<Long> processInstanceIds) {
		if (ListUtil.isEmpty(processInstanceIds)) {
			return null;
		}
		
		return getResultList(CaseProcInstBind.getCaseIdsByProcessInstanceIds,
		    Long.class, new Param(CaseProcInstBind.processInstanceIdsProp,
		            processInstanceIds));
	}
	
	public List<VariableInstance> getVariablesByProcessDefinition(
	        String processDefinitionName) {
		if (StringUtil.isEmpty(processDefinitionName)) {
			return null;
		}
		
		return getResultList(
		    CaseProcInstBind.getVariablesByProcessDefinitionName,
		    VariableInstance.class, new Param(
		            CaseProcInstBind.processDefinitionNameProp,
		            processDefinitionName));
	}
	
	public List<VariableInstance> getVariablesByProcessInstanceId(
	        Long processInstanceId) {
		if (processInstanceId == null) {
			return null;
		}
		
		return getResultList(CaseProcInstBind.getVariablesByProcessInstanceId,
		    VariableInstance.class, new Param(
		            CaseProcInstBind.processInstanceIdProp, processInstanceId));
	}
	
	public List<VariableInstance> getVariablesByProcessInstanceIdAndVariablesNames(
	        Collection<Long> processInstanceIds, List<String> variables) {
		if (ListUtil.isEmpty(processInstanceIds) || ListUtil.isEmpty(variables)) {
			return null;
		}
		
		return getResultList(
		    CaseProcInstBind.getVariablesByProcessInstancesIdsAndVariablesNames,
		    VariableInstance.class,
		    new Param(CaseProcInstBind.processInstanceIdsProp,
		            processInstanceIds), new Param(
		            CaseProcInstBind.variablesNamesProp, variables));
	}
	
	// TODO: those queries are very similar, make some general query, and just append queries/joins
	// in more special use cases
	public List<Integer> getMyCasesIds(User user,
	        List<String> caseStatusesToShow, List<String> caseStatusesToHide) {
		List<Param> params = new ArrayList<Param>();
		params.add(new Param("caseStatusToShow", caseStatusesToShow));
		params.add(new Param(NativeIdentityBind.identityIdProperty, user
		        .getPrimaryKey().toString()));
		params.add(new Param("userStatus",
		        ProcessUserBind.Status.PROCESS_WATCHED.toString()));
		if (!ListUtil.isEmpty(caseStatusesToHide)) {
			params.add(new Param("statusesToHide", caseStatusesToHide));
		}
		
		StringBuilder builder = new StringBuilder(1000);
		builder
		        .append(
		            "(select distinct comm_case.comm_case_id as caseId, proc_case.created as Created from comm_case ")
		        .append(
		            "inner join bpm_cases_processinstances cp on cp.case_id = comm_case.comm_case_id ")
		        .append(
		            "inner join jbpm_processinstance pi on pi.id_ = cp.process_instance_id ")
		        .append(
		            "inner join proc_case on comm_case.comm_case_id = proc_case.proc_case_id ")
		        .append("left join ").append(ProcessUserBind.TABLE_NAME)
		        .append(" pu on cp.").append(
		            CaseProcInstBind.procInstIdColumnName).append(
		            " = pu.process_instance_id ").append("where ");
		
		builder.append("pi.end_ is null and ");
		builder.append("(comm_case.handler = :"
		        + NativeIdentityBind.identityIdProperty + " or (pu.user_id = :"
		        + NativeIdentityBind.identityIdProperty
		        + " and pu.user_status = :userStatus)) ");
		
		if (!ListUtil.isEmpty(caseStatusesToHide)) {
			builder
			        .append("and proc_case.case_status not in (:statusesToHide) ");
		}
		builder
		        .append(") UNION (select distinct comm_case.comm_case_id as caseId, proc_case.created as Created from comm_case "
		                + "inner join proc_case on proc_case.proc_case_id = comm_case.comm_case_id where ");
		
		builder.append("comm_case.handler = :"
		        + NativeIdentityBind.identityIdProperty + " ");
		if (!ListUtil.isEmpty(caseStatusesToHide)) {
			builder
			        .append("and proc_case.case_status not in (:statusesToHide) ");
		}
		builder
		        .append("and proc_case.case_status in (:caseStatusToShow) "
		                + "and proc_case.case_manager_type is null) order by Created desc");
		
		return getQueryNativeInline(builder.toString()).getResultList(
		    Integer.class, "caseId", params.toArray(new Param[params.size()]));
	}
	
	public List<Integer> getOpenCasesIds(User user,
	        List<String> caseStatusesToShow, List<String> caseStatusesToHide,
	        Collection<Integer> groups, Collection<String> roles) {
		
		List<Param> params = new ArrayList<Param>();
		params.add(new Param("statusesToShow", caseStatusesToShow));
		params.add(new Param(NativeIdentityBind.identityIdProperty, user
		        .getPrimaryKey().toString()));
		params.add(new Param(NativeIdentityBind.identityTypeProperty,
		        IdentityType.USER.toString()));
		if (!ListUtil.isEmpty(caseStatusesToHide)) {
			params.add(new Param("statusesToHide", caseStatusesToHide));
		}
		StringBuilder builder = new StringBuilder(1000);
		builder
		        .append("(select distinct comm_case.comm_case_id as caseId, proc_case.created as Created from comm_case "
		                + "inner join proc_case on comm_case.comm_case_id = proc_case.proc_case_id "
		                + "inner join bpm_cases_processinstances cp on cp.case_id = comm_case.comm_case_id "
		                + "inner join bpm_actors act on act.process_instance_id = cp.process_instance_id "
		                + "inner join jbpm_processinstance pi on pi.id_ = cp.process_instance_id "
		                + "left join bpm_native_identities ni on act.actor_id = ni.actor_fk "
		                + "where (");
		if (!ListUtil.isEmpty(roles)) {
			builder
			        .append("(act.role_name in (:roles) or (ni.identity_type = :identityTypeRole and ni.identity_id in(:roles))) or ");
			params.add(new Param("roles", roles));
			params.add(new Param("identityTypeRole", IdentityType.ROLE
			        .toString()));
		}
		builder
		        .append("ni.identity_id = :identityId  and  ni.identity_type = :identityType) ");
		builder
		        .append("and act.process_instance_id is not null and pi.end_ is null ");
		if (!ListUtil.isEmpty(caseStatusesToHide)) {
			builder
			        .append("and proc_case.case_status not in (:statusesToHide)");
		}
		builder
		        .append(") union"
		                + "(select distinct comm_case.comm_case_id as caseId, proc_case.created as Created from comm_case "
		                + "inner join proc_case on proc_case.proc_case_id = comm_case.comm_case_id where proc_case.case_status in (:statusesToShow) ");
		if (!ListUtil.isEmpty(groups)) {
			builder.append("and proc_case.handler_group_id in (:groups) ");
			params.add(new Param("groups", groups));
		}
		builder
		        .append("and proc_case.case_manager_type is null) order by Created desc");
		
		return getQueryNativeInline(builder.toString()).getResultList(
		    Integer.class, "caseId", params.toArray(new Param[params.size()]));
	}
	
	public List<Integer> getOpenCasesIdsForAdmin(
	        List<String> caseStatusesToShow, List<String> caseStatusesToHide) {
		
		List<Param> params = new ArrayList<Param>();
		params.add(new Param("statusesToShow", caseStatusesToShow));
		if (!ListUtil.isEmpty(caseStatusesToHide)) {
			params.add(new Param("statusesToHide", caseStatusesToHide));
		}
		StringBuilder builder = new StringBuilder(1000);
		builder
		        .append("(select distinct comm_case.comm_case_id as caseId, proc_case.created as Created from comm_case "
		                + "inner join proc_case on comm_case.comm_case_id = proc_case.proc_case_id "
		                + "inner join bpm_cases_processinstances cp on cp.case_id = comm_case.comm_case_id "
		                + "inner join bpm_actors act on act.process_instance_id = cp.process_instance_id "
		                + "inner join jbpm_processinstance pi on pi.id_ = cp.process_instance_id "
		                + "left join bpm_native_identities ni on act.actor_id = ni.actor_fk "
		                + "where ");
		builder
		        .append(" act.process_instance_id is not null and pi.end_ is null ");
		if (!ListUtil.isEmpty(caseStatusesToHide)) {
			builder
			        .append("and proc_case.case_status not in (:statusesToHide)");
		}
		builder
		        .append(") union"
		                + "(select distinct comm_case.comm_case_id as caseId, proc_case.created as Created from comm_case "
		                + "inner join proc_case on proc_case.proc_case_id = comm_case.comm_case_id where proc_case.case_status in (:statusesToShow) ");
		builder
		        .append("and proc_case.case_manager_type is null) order by Created desc");
		
		return getQueryNativeInline(builder.toString()).getResultList(
		    Integer.class, "caseId", params.toArray(new Param[params.size()]));
	}
	
	public List<Integer> getClosedCasesIds(User user,
	        List<String> caseStatusesToShow, List<String> caseStatusesToHide,
	        Collection<Integer> groups, Collection<String> roles) {
		
		List<Param> params = new ArrayList<Param>();
		params.add(new Param("statusesToShow", caseStatusesToShow));
		params.add(new Param(NativeIdentityBind.identityIdProperty, user
		        .getPrimaryKey().toString()));
		params.add(new Param(NativeIdentityBind.identityTypeProperty,
		        NativeIdentityBind.IdentityType.USER.toString()));
		if (!ListUtil.isEmpty(caseStatusesToHide)) {
			params.add(new Param("statusesToHide", caseStatusesToHide));
		}
		StringBuilder builder = new StringBuilder(1000);
		builder
		        .append("(select distinct comm_case.comm_case_id as caseId, proc_case.created as Created from comm_case "
		                + "inner join proc_case on comm_case.comm_case_id = proc_case.proc_case_id "
		                + "inner join bpm_cases_processinstances cp on cp.case_id = comm_case.comm_case_id "
		                + "inner join bpm_actors act on act.process_instance_id = cp.process_instance_id "
		                + "inner join jbpm_processinstance pi on pi.id_ = cp.process_instance_id "
		                + "left join bpm_native_identities ni on act.actor_id = ni.actor_fk "
		                + "where (");
		
		if (!ListUtil.isEmpty(roles)) {
			builder
			        .append("(act.role_name in (:roles) or (ni.identity_type = :identityTypeRole and ni.identity_id in(:roles))) or ");
			params.add(new Param("roles", roles));
			params.add(new Param("identityTypeRole", IdentityType.ROLE
			        .toString()));
		}
		
		builder
		        .append("ni.identity_id = :identityId and ni.identity_type = :identityType) ");
		builder
		        .append("and act.process_instance_id is not null and (pi.end_ is not null or proc_case.case_status in (:statusesToShow)) ");
		if (!ListUtil.isEmpty(caseStatusesToHide)) {
			builder
			        .append("and proc_case.case_status not in (:statusesToHide)");
		}
		builder
		        .append(") union"
		                + "(select distinct comm_case.comm_case_id as caseId, proc_case.created as Created from comm_case "
		                + "inner join proc_case on proc_case.proc_case_id = comm_case.comm_case_id where proc_case.case_status in (:statusesToShow) ");
		if (!ListUtil.isEmpty(groups)) {
			builder.append("and proc_case.handler_group_id in (:groups) ");
			params.add(new Param("groups", groups));
		}
		if (!ListUtil.isEmpty(caseStatusesToHide)) {
			builder
			        .append("and proc_case.case_status not in (:statusesToHide) ");
		}
		builder
		        .append("and proc_case.case_manager_type is null) order by Created desc");
		
		return getQueryNativeInline(builder.toString()).getResultList(
		    Integer.class, "caseId", params.toArray(new Param[params.size()]));
	}
	
	public List<Integer> getClosedCasesIdsForAdmin(
	        List<String> caseStatusesToShow, List<String> caseStatusesToHide) {
		
		List<Param> params = new ArrayList<Param>();
		params.add(new Param("statusesToShow", caseStatusesToShow));
		if (!ListUtil.isEmpty(caseStatusesToHide)) {
			params.add(new Param("statusesToHide", caseStatusesToHide));
		}
		StringBuilder builder = new StringBuilder(1000);
		builder
		        .append("(select distinct comm_case.comm_case_id as caseId, proc_case.created as Created from comm_case "
		                + "inner join proc_case on comm_case.comm_case_id = proc_case.proc_case_id "
		                + "inner join bpm_cases_processinstances cp on cp.case_id = comm_case.comm_case_id "
		                + "inner join bpm_actors act on act.process_instance_id = cp.process_instance_id "
		                + "inner join jbpm_processinstance pi on pi.id_ = cp.process_instance_id "
		                + "left join bpm_native_identities ni on act.actor_id = ni.actor_fk "
		                + "where ");
		
		builder
		        .append("act.process_instance_id is not null and (pi.end_ is not null or proc_case.case_status in (:statusesToShow))");
		if (!ListUtil.isEmpty(caseStatusesToHide)) {
			builder
			        .append("and proc_case.case_status not in (:statusesToHide) ");
		}
		builder
		        .append(") union"
		                + "(select distinct comm_case.comm_case_id as caseId, proc_case.created as Created from comm_case "
		                + "inner join proc_case on proc_case.proc_case_id = comm_case.comm_case_id where proc_case.case_status in (:statusesToShow) ");
		if (!ListUtil.isEmpty(caseStatusesToHide)) {
			builder
			        .append("and proc_case.case_status not in (:statusesToHide) ");
		}
		builder
		        .append("and proc_case.case_manager_type is null) order by Created desc");
		
		return getQueryNativeInline(builder.toString()).getResultList(
		    Integer.class, "caseId", params.toArray(new Param[params.size()]));
	}
	
	public List<Integer> getUserCasesIds(User user,
	        List<String> caseStatusesToShow, List<String> caseStatusesToHide,
	        List<String> caseCodes, Collection<String> roles) {
		List<Param> params = new ArrayList<Param>();
		params.add(new Param("caseCodes", caseCodes));
		if (!ListUtil.isEmpty(caseStatusesToHide)) {
			params.add(new Param("statusesToHide", caseStatusesToHide));
		}
		if (!ListUtil.isEmpty(caseStatusesToShow)) {
			params.add(new Param("statusesToShow", caseStatusesToShow));
		}
		params.add(new Param(NativeIdentityBind.identityIdProperty, user
		        .getPrimaryKey().toString()));
		params.add(new Param(NativeIdentityBind.identityTypeProperty,
		        NativeIdentityBind.IdentityType.USER.toString()));
		StringBuilder builder = new StringBuilder(1000);
		builder
		        .append("(select distinct proc_case.proc_case_id as caseId, proc_case.created as Created from proc_case "
		                + "inner join bpm_cases_processinstances cp on cp.case_id = proc_case.proc_case_id "
		                + "inner join bpm_actors act on act.process_instance_id = cp.process_instance_id "
		                + "left join bpm_native_identities ni on act.actor_id = ni.actor_fk where (");
		
		if (!ListUtil.isEmpty(roles)) {
			builder
			        .append("(act.role_name in (:roles) or (ni.identity_type = :identityTypeRole and ni.identity_id in(:roles))) or ");
			params.add(new Param("roles", roles));
			params.add(new Param("identityTypeRole", IdentityType.ROLE
			        .toString()));
		}
		builder
		        .append("ni.identity_id = :identityId and ni.identity_type = :identityType) ");
		builder
		        .append("and act.process_instance_id is not null and proc_case.case_code not in (:caseCodes) ");
		if (!ListUtil.isEmpty(caseStatusesToHide)) {
			builder
			        .append("and proc_case.case_status not in (:statusesToHide) ");
		}
		
		// Is thir realy right???
		if (!ListUtil.isEmpty(caseStatusesToShow)) {
			builder
			        .append("and proc_case.case_status not in (:statusesToShow) ");
		}
		builder
		        .append(") union (select distinct proc_case.proc_case_id as caseId, proc_case.created as Created from proc_case "
		                + "where user_id=:identityId and proc_case.case_code not in (:caseCodes) ");
		if (!ListUtil.isEmpty(caseStatusesToHide)) {
			builder
			        .append("and proc_case.case_status not in (:statusesToHide)");
		}
		if (!ListUtil.isEmpty(caseStatusesToShow)) {
			builder
			        .append("and proc_case.case_status not in (:statusesToShow) ");
		}
		builder.append(") order by Created desc");
		
		return getQueryNativeInline(builder.toString()).getResultList(
		    Integer.class, "caseId", params.toArray(new Param[params.size()]));
	}
	
	public List<Integer> getCasesIdsByStatusForAdmin(
	        List<String> caseStatusesToShow, List<String> caseStatusesToHide) {
		StringBuilder builder = new StringBuilder(200);
		List<Param> params = new ArrayList<Param>();
		params.add(new Param("statusToShow", caseStatusesToShow));
		builder
		        .append("select comm_case.comm_case_id as caseId from comm_case "
		                + "inner join proc_case on comm_case.comm_case_id = proc_case.proc_case_id "
		                + "where proc_case.case_status in(:statusToShow) ");
		if (!ListUtil.isEmpty(caseStatusesToHide)) {
			builder.append("and proc_case.case_status in(:statusesToHide) ");
			params.add(new Param("statusesToHide", caseStatusesToHide));
		}
		builder.append("order by proc_case.created desc");
		
		return getQueryNativeInline(builder.toString()).getResultList(
		    Integer.class, "caseId", params.toArray(new Param[params.size()]));
	}
}