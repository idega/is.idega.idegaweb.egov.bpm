package com.idega.idegaweb.egov.bpm.data.dao.impl;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.TypedQuery;

import org.hibernate.HibernateException;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.process.business.CaseBusiness;
import com.idega.block.process.business.ProcessConstants;
import com.idega.block.process.data.Case;
import com.idega.block.process.data.CaseBMPBean;
import com.idega.business.IBOLookup;
import com.idega.core.persistence.Param;
import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.core.user.data.User;
import com.idega.data.MetaDataBMPBean;
import com.idega.data.SimpleQuerier;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.CaseTypesProcDefBind;
import com.idega.idegaweb.egov.bpm.data.ProcessUserBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.bean.BPMProcessVariable;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.bean.VariableInstanceType;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.data.impl.VariableInstanceQuerierImpl;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.DBUtil;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.50 $ Last modified: $Date: 2009/07/07 12:14:10 $ by $Author: valdas $
 */
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Repository(CasesBPMDAO.REPOSITORY_NAME)
@Transactional(readOnly = true)
public class CasesBPMDAOImpl extends GenericDaoImpl implements CasesBPMDAO {

	private static final Logger LOGGER = Logger.getLogger(CasesBPMDAOImpl.class.getName());

	@Autowired(required = false)
	private VariableInstanceQuerier querier;

	@Autowired
	private BPMContext bpmContext;

	@Autowired
	private BPMFactory bpmFactory;

	@Override
	public List<CaseTypesProcDefBind> getAllCaseTypes() {

		@SuppressWarnings("unchecked")
		List<CaseTypesProcDefBind> casesProcesses = getEntityManager()
		        .createNamedQuery(CaseTypesProcDefBind.CASES_PROCESSES_GET_ALL)
		        .getResultList();

		return casesProcesses;
	}

	@Override
	public CaseProcInstBind getCaseProcInstBindByCaseId(Integer caseId) {

		@SuppressWarnings("unchecked")
		List<CaseProcInstBind> l = getEntityManager().createNamedQuery(
		    CaseProcInstBind.BIND_BY_CASEID_QUERY_NAME).setParameter(
		    CaseProcInstBind.caseIdParam, caseId).getResultList();

		if (l.isEmpty())
			return null;

		return l.iterator().next();
	}

	@Override
	@Transactional(readOnly = true)
	public CaseProcInstBind getCaseProcInstBindByProcessInstanceId(final Long processInstanceId) {
		CaseProcInstBind caseProcInstBind = find(CaseProcInstBind.class, processInstanceId);
		if (caseProcInstBind != null) {
			return caseProcInstBind;
		}

		caseProcInstBind = bpmContext.execute(new JbpmCallback<CaseProcInstBind>() {
			@Override
			public CaseProcInstBind doInJbpm(JbpmContext context) throws JbpmException {
				ProcessInstance currentProcess = context.getProcessInstance(processInstanceId);
				if (currentProcess == null) {
					return null;
				}

				Long mainProcessId = null;
		    	Token superToken = currentProcess.getSuperProcessToken();
		    	while (superToken != null) {
		    		superToken = DBUtil.getInstance().initializeAndUnproxy(superToken);
		    		ProcessInstance processInstance = superToken.getProcessInstance();
					mainProcessId = processInstance.getId();
		    		superToken = processInstance.getSuperProcessToken();
		    	}

		    	if (mainProcessId == null) {
					return null;
				}
				return find(CaseProcInstBind.class, mainProcessId);
			}
		});
		return caseProcInstBind;
	}

	@Override
	public List<Integer> getCasesIdsByProcInstIds(List<Long> procInstIds) {
		if (ListUtil.isEmpty(procInstIds))
			return null;

		List<Long> casesIds = getCaseIdsByProcessInstanceIds(procInstIds);
		if (ListUtil.isEmpty(casesIds))
			return null;

		List<Integer> ids = new ArrayList<Integer>();
		for (Long caseId: casesIds) {
			Integer id = caseId.intValue();
			ids.add(id);
		}

		return ids;
	}

	@Override
	public List<CaseProcInstBind> getCasesProcInstBindsByCasesIds(List<Integer> casesIds) {
		if (ListUtil.isEmpty(casesIds)) {
			return Collections.emptyList();
		}

		List<CaseProcInstBind> binds = getResultList(
		    CaseProcInstBind.BIND_BY_CASES_IDS_QUERY_NAME,
		    CaseProcInstBind.class, new Param(CaseProcInstBind.casesIdsParam, casesIds));

		return binds;
	}

	@Override
	public List<CaseProcInstBind> getCasesProcInstBindsByProcInstIds(List<Long> procInstIds) {
		List<CaseProcInstBind> binds = getResultList(
		    CaseProcInstBind.BIND_BY_PROCESSES_IDS_QUERY_NAME,
		    CaseProcInstBind.class, new Param(CaseProcInstBind.procInstIdsParam, procInstIds));

		return binds;
	}

	@Override
	@Transactional(readOnly = false)
	public ProcessUserBind getProcessUserBind(long processInstanceId, int userId, boolean createIfNotFound) {

		@SuppressWarnings("unchecked")
		List<ProcessUserBind> u = getEntityManager().createNamedQuery(
		    ProcessUserBind.byUserIdNPID).setParameter(
		    ProcessUserBind.pidParam, processInstanceId).setParameter(
		    ProcessUserBind.userIdParam, userId).getResultList();

		if (u.isEmpty() && createIfNotFound) {

			CaseProcInstBind bind = find(CaseProcInstBind.class, processInstanceId);

			if (bind != null) {

				ProcessUserBind cu = new ProcessUserBind();
				cu.setCaseProcessBind(bind);
				cu.setUserId(userId);
				persist(cu);
				return cu;

			} else
				throw new IllegalStateException("Case not bound to process instance");

		} else if (!u.isEmpty()) {
			return u.iterator().next();
		} else
			return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO#getProcessUserBind(long)
	 */
	@Override
	public List<ProcessUserBind> getProcessUserBind(long processInstanceId) {
		 TypedQuery<ProcessUserBind> query = getEntityManager().createNamedQuery(
				 ProcessUserBind.byPID, ProcessUserBind.class);
		 query = query.setParameter(ProcessUserBind.pidParam, processInstanceId);
		 return query.getResultList();
	}

	/*
	 * (non-Javadoc)
	 * @see com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO#removeProcessUserBinds(long)
	 */
	@Override
	public void removeProcessUserBinds(long processInstanceId) {
		List<ProcessUserBind> users = getProcessUserBind(processInstanceId);
		for (ProcessUserBind user : users) {
			remove(user);
		}
	}

	@Override
	public List<ProcessUserBind> getProcessUserBinds(int userId, Collection<Integer> casesIds) {

		if (casesIds.isEmpty())
			return new ArrayList<ProcessUserBind>(0);

		@SuppressWarnings("unchecked")
		List<ProcessUserBind> u = getEntityManager().createNamedQuery(
		    ProcessUserBind.byUserIdAndCaseId).setParameter(
		    ProcessUserBind.userIdParam, userId).setParameter(
		    ProcessUserBind.casesIdsParam, casesIds).getResultList();

		return u;
	}

	@Override
	public CaseTypesProcDefBind getCaseTypesProcDefBindByPDName(String pdName) {

		@SuppressWarnings("unchecked")
		List<CaseTypesProcDefBind> u = getEntityManager().createNamedQuery(
		    CaseTypesProcDefBind.CASES_PROCESSES_GET_BY_PDNAME).setParameter(
		    CaseTypesProcDefBind.procDefNamePropName, pdName).getResultList();

		if (!u.isEmpty())
			return u.iterator().next();

		return null;
	}

	@Override
	@Transactional(readOnly = false)
	public void updateCaseTypesProcDefBind(CaseTypesProcDefBind bind) {
		getEntityManager().merge(bind);
	}

	@Override
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

	@Override
	@SuppressWarnings("unchecked")
	public CaseProcInstBind getLastCreatedCaseProcInstBind() {
		List<CaseProcInstBind> binds = getEntityManager().createNamedQuery(CaseProcInstBind.getLastCreatedCase).getResultList();
		CaseProcInstBind bind = ListUtil.isEmpty(binds) ? null : binds.get(binds.size() - 1);
		return bind;
	}

	@Override
	public List<Object[]> getCaseProcInstBindProcessInstanceByDateCreatedAndCaseIdentifierId(
			Collection<Date> dates,
			Collection<Integer> identifierIDs
	) {
		List<Object[]> cps = null;

		if (!ListUtil.isEmpty(dates) && !ListUtil.isEmpty(identifierIDs)) {

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

	@Override
	public List<Object[]> getCaseProcInstBindProcessInstanceByCaseIdentifier(Collection<String> identifiers) {
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

	@Override
	public List<Token> getCaseProcInstBindSubprocessBySubprocessName(Long processInstanceId) {
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

	@Override
	public List<Long> getCaseIdsByProcessDefinitionNameAndVariables(String processDefinitionName, List<BPMProcessVariable> variables) {

		if (StringUtil.isEmpty(processDefinitionName)) {
			return null;
		}

		if (ListUtil.isEmpty(variables)) {
			return getResultList(
					CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndName,
					Long.class,
					new Param(CaseProcInstBind.processDefinitionNameProp, processDefinitionName)
			);
		}

		Locale locale = CoreUtil.getCurrentLocale();

		List<Long> allResults = null;
		List<Long> variableResults = null;
		for (BPMProcessVariable variable : variables) {
			variableResults = null;
			Object value = variable.getRealValue(locale);

			// Date
			if (variable.isDateType()) {
				if (value instanceof Timestamp) {
					IWTimestamp valueStart = new IWTimestamp((Timestamp) value);
					valueStart.setHour(0);
					valueStart.setMinute(0);
					valueStart.setSecond(0);
					valueStart.setMilliSecond(0);
					IWTimestamp valueEnd = new IWTimestamp((Timestamp) value);
					valueEnd.setHour(23);
					valueEnd.setMinute(59);
					valueEnd.setSecond(59);
					valueEnd.setMilliSecond(999);
					variableResults = getResultList(CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndNameAndDateVariables, Long.class,
						    new Param(CaseProcInstBind.processDefinitionNameProp, processDefinitionName),
						    new Param(CaseProcInstBind.variablesNamesProp, variable.getName()),
						    new Param(CaseProcInstBind.variablesValuesProp, valueStart.getTimestamp()),
						    new Param(CaseProcInstBind.variablesValuesPropEnd, valueEnd.getTimestamp()),
						    new Param(CaseProcInstBind.variablesTypesProp, new HashSet<String>(BPMProcessVariable.DATE_TYPES))
					);
				}

			// Double
			} else if (variable.isDoubleType()) {
				if (value instanceof Double) {
					variableResults = getCaseIdsByVariable(
							CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndNameAndDoubleVariables,
							processDefinitionName,
							variable.getName(),
							value,
							BPMProcessVariable.DOUBLE_TYPES
					);
				}

			// Long
			} else if (variable.isLongType()) {
				if (value instanceof Long) {
					variableResults = getCaseIdsByVariable(
							CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndNameAndLongVariables,
							processDefinitionName,
							variable.getName(),
							value,
							BPMProcessVariable.LONG_TYPES
					);
				}

			// String
			} else if (variable.isStringType()) {
				if (value instanceof String) {
					String query = VariableInstanceQuerierImpl.isDataMirrowed() ?
							CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndNameAndStringVariables :
							CaseProcInstBind.getCaseIdsByProcessDefinitionIdsAndNameAndStringVariablesNoMirrow;
					variableResults = getCaseIdsByVariable(query, processDefinitionName, variable.getName(),
							CoreConstants.PERCENT.concat((String) value).concat(CoreConstants.PERCENT), BPMProcessVariable.STRING_TYPES);
				}

			// Unsupported variable
			} else {
				LOGGER.warning(new StringBuilder("Unsupported variable: ").append(variable).append(", terminating search!").toString());
				return null; // Unsupported variable!
			}

			if (ListUtil.isEmpty(variableResults)) {
				LOGGER.warning(new StringBuilder("No results by variable: ").append(variable).append(", terminating search!").toString());
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

	@Override
	public List<Long> getCaseIdsByProcessDefinition(String processDefinitionName) {
		if (StringUtil.isEmpty(processDefinitionName))
			return null;

		return getResultList(CaseProcInstBind.getCaseIdsByProcessDefinitionName, Long.class,
				new Param(CaseProcInstBind.processDefinitionNameProp, processDefinitionName));
	}

	private List<Long> getCaseIdsByVariable(String queryName, String processDefinitionName, String variableName, Object value,
			List<String> types) {

		return getResultList(queryName, Long.class,
			    new Param(CaseProcInstBind.processDefinitionNameProp, processDefinitionName),
			    new Param(CaseProcInstBind.variablesNamesProp, variableName),
			    new Param(CaseProcInstBind.variablesValuesProp, value),
			    new Param(CaseProcInstBind.variablesTypesProp, new HashSet<String>(types))
		);
	}

	@Override
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

		String query = "select " + CaseBMPBean.PK_COLUMN + " from " + CaseBMPBean.TABLE_NAME + " where " + CaseBMPBean.COLUMN_CASE_MANAGER_TYPE +
				" = '" + ProcessConstants.BPM_CASE + "' and lower(" + CaseBMPBean.COLUMN_CASE_IDENTIFIER + ") LIKE '" + caseNumber + "'";
		List<Serializable[]> data = null;
		try {
			data = SimpleQuerier.executeQuery(query, 1);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error executing query: " + query, e);
		}
		if (ListUtil.isEmpty(data)) {
			return new ArrayList<Long>(0);
		}

		List<Integer> ids = new ArrayList<Integer>();
		for (Serializable[] temp: data) {
			if (ArrayUtil.isEmpty(temp)) {
				continue;
			}

			Serializable id = temp[0];
			if (id instanceof Number) {
				ids.add(((Number) id).intValue());
			}
		}

		List<CaseProcInstBind> binds = getCasesProcInstBindsByCasesIds(ids);
		if (ListUtil.isEmpty(binds)) {
			return new ArrayList<Long>(0);
		}

		List<Long> results = new ArrayList<Long>();
		for (CaseProcInstBind bind: binds) {
			results.add(Long.valueOf(bind.getCaseId()));
		}
		return results;
	}

	@Override
	public List<Long> getCaseIdsByProcessUserStatus(String status) {
		if (status == null || CoreConstants.EMPTY.equals(status)) {
			return null;
		}

		return getResultList(CaseProcInstBind.getCaseIdsByProcessUserStatus, Long.class, new Param(ProcessUserBind.statusProp, status));
	}

	@Override
	public List<Long> getCaseIdsByCaseStatus(String[] statuses) {
		if (statuses == null || statuses.length == 0) {
			return null;
		}

		Set<String> statusesInSet = new HashSet<String>(statuses.length);
		for (int i = 0; i < statuses.length; i++) {
			statusesInSet.add(statuses[i]);
		}

		String query = VariableInstanceQuerierImpl.isDataMirrowed() ?
				CaseProcInstBind.getCaseIdsByCaseStatus : CaseProcInstBind.getCaseIdsByCaseStatusNoMirrow;
		return getResultList(query, Long.class, new Param(CaseProcInstBind.caseStatusesProp, statusesInSet));
	}

	@Override
	public List<Long> getCaseIdsByUserIds(String userId) {
		if (StringUtil.isEmpty(userId)) {
			return null;
		}

		return getResultList(CaseProcInstBind.getCaseIdsByUserIds, Long.class, new Param(ProcessUserBind.userIdParam, userId));
	}

	@Override
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

	@Override
	public List<Long> getCaseIdsByProcessInstanceIds(List<Long> processInstanceIds) {
		if (ListUtil.isEmpty(processInstanceIds))
			return null;

		if (IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("cases_bpm_load_from_bind", Boolean.FALSE))
			return getResultList(CaseProcInstBind.getCaseIdsByProcessInstanceIds, Long.class,
					new Param(CaseProcInstBind.processInstanceIdsProp, processInstanceIds));

		long start = System.currentTimeMillis();
		List<Long> ids = getCasesIds(processInstanceIds, null);
		LOGGER.info("Cases IDs were loaded and sorted by process instance IDs (total " + processInstanceIds.size() + ") in " +
				(System.currentTimeMillis() - start) + " ms");
		return ids;
	}

	private class CaseResult {
		private Long id;
		private Timestamp created;

		private CaseResult(Long id, Timestamp created) {
			this.id = id;
			this.created = created;
		}

		@Override
		public String toString() {
			return id + ": " + created;
		}
	}

	private List<Long> getCasesIds(List<Long> procInstIds, List<CaseResult> cases) {
		if (ListUtil.isEmpty(procInstIds)) {
			if (ListUtil.isEmpty(cases))
				return null;

			Comparator<CaseResult> comparator = new Comparator<CasesBPMDAOImpl.CaseResult>() {
				@Override
				public int compare(CaseResult r1, CaseResult r2) {
					return -1 * (r1.created.compareTo(r2.created));
				}
			};
			long start = System.currentTimeMillis();
			Collections.sort(cases, comparator);
			LOGGER.info("Cases IDs (total " + cases.size() + ") were sorted in " + (System.currentTimeMillis() - start) + " ms");
			List<Long> results = new ArrayList<Long>();
			for (CaseResult theCase: cases)
				results.add(theCase.id);
			return results;
		}

		if (cases == null)
			cases = new ArrayList<CasesBPMDAOImpl.CaseResult>();

		List<Long> usedIds = null;
		if (procInstIds.size() > 1000) {
			usedIds = new ArrayList<Long>(procInstIds.subList(0, 1000));
			procInstIds = new ArrayList<Long>(procInstIds.subList(1000,	procInstIds.size()));
		} else {
			usedIds = new ArrayList<Long>(procInstIds);
			procInstIds = null;
		}

		StringBuilder ids = new StringBuilder();
		for (Iterator<Long> idsIter = usedIds.iterator(); idsIter.hasNext();) {
			ids.append(idsIter.next());
			if (idsIter.hasNext())
				ids.append(", ");
		}
		String query = "select b." + CaseProcInstBind.caseIdColumnName + ", c." + CaseBMPBean.COLUMN_CREATED + " from " +
				CaseProcInstBind.TABLE_NAME + " b, " + CaseBMPBean.TABLE_NAME + " c where b." + CaseProcInstBind.procInstIdColumnName +
				" in (" + ids.toString() +") and b." + CaseProcInstBind.caseIdColumnName + " = c.proc_case_id";
		List<Serializable[]> data = null;
		try {
			data = SimpleQuerier.executeQuery(query, 2);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: " + query, e);
		}
		if (!ListUtil.isEmpty(data)) {
			for (Serializable[] theCase: data) {
				if (ArrayUtil.isEmpty(theCase) || theCase.length < 2)
					continue;

				Serializable id = theCase[0];
				Serializable created = theCase[1];
				if (id instanceof Number && created instanceof Date)
					cases.add(new CaseResult(((Number) id).longValue(), new IWTimestamp(((Date) created).getTime()).getTimestamp()));
				else
					LOGGER.warning("ID (" + id + (id == null ? "" : ", class: " + id.getClass()) +
							") is not Number and/or creation date (" + created + (created == null ? "" : ", class: " +
							created.getClass()) + ") is not Timestamp");
			}
		}

		return getCasesIds(procInstIds, cases);
	}

	@Override
	public Map<Integer, Date> getMyCasesIds(
			User user,
			List<String> caseStatusesToShow,
			List<String> caseStatusesToHide,
			boolean onlySubscribedCases,
			Integer caseId,
			List<Long> procInstIds,
			Collection<? extends Number> subscriberGroupIDs,
	        Timestamp from,
	        Timestamp to
	) {
		List<Param> params = new ArrayList<Param>();
		params.add(new Param(NativeIdentityBind.identityIdProperty, user.getPrimaryKey().toString()));
		params.add(new Param("userStatus", ProcessUserBind.Status.PROCESS_WATCHED.toString()));

		StringBuilder builder = new StringBuilder(1000);
		builder.append("(select distinct comm_case.comm_case_id as caseId, proc_case.created as Created from comm_case ")
				.append("inner join " + CaseProcInstBind.TABLE_NAME + " cp on cp.case_id = comm_case.comm_case_id ")
		        .append("inner join jbpm_processinstance pi on pi.id_ = cp.process_instance_id ")
		        .append("inner join proc_case on comm_case.comm_case_id = proc_case.proc_case_id ");

		if (onlySubscribedCases || !ListUtil.isEmpty(subscriberGroupIDs)) {
			builder.append("inner join proc_case_subscribers on proc_case.proc_case_id = proc_case_subscribers.proc_case_id ");

			if (!ListUtil.isEmpty(subscriberGroupIDs)) {
				builder.append("JOIN ic_user ON ic_user.IC_USER_ID = proc_case_subscribers.IC_USER_ID ");
				builder.append("AND ic_user.PRIMARY_GROUP IN (").append(toStringNumbers(subscriberGroupIDs)).append(") ");
			}
		}

		builder.append("left join ").append(ProcessUserBind.TABLE_NAME)
		        .append(" pu on cp.").append(CaseProcInstBind.procInstIdColumnName).append(" = pu.process_instance_id ").append("where ");

		builder.append(getConditionForCaseId(params, caseId, "comm_case.comm_case_id"));
		builder.append(getConditionForFromAndTo(params, from, to));
		builder.append(getConditionForProcInstIds(params, procInstIds, "cp." + CaseProcInstBind.procInstIdColumnName));

		builder.append("pi.end_ is null and ");
		builder.append("(comm_case.handler = :"
		        + NativeIdentityBind.identityIdProperty + " or (pu.user_id = :"
		        + NativeIdentityBind.identityIdProperty
		        + " and pu.user_status = :userStatus)) ");

		builder.append(getConditionForCaseStatuses(params, caseStatusesToShow, caseStatusesToHide));

		if (onlySubscribedCases) {
			builder.append(" and (proc_case.user_id = :caseAuthor or proc_case_subscribers.ic_user_id = :subscriber) ");
			params.add(new Param("subscriber", user.getPrimaryKey()));
			params.add(new Param("caseAuthor", user.getPrimaryKey()));
		}
		builder.append(") UNION (select distinct comm_case.comm_case_id as caseId, proc_case.created as Created from comm_case ")
						.append("inner join proc_case on proc_case.proc_case_id = comm_case.comm_case_id ");
		if (!ListUtil.isEmpty(procInstIds))
			builder.append("inner join " + CaseProcInstBind.TABLE_NAME + " cp on cp.case_id = comm_case.comm_case_id ");

		if (!ListUtil.isEmpty(subscriberGroupIDs)) {
			builder.append("inner join proc_case_subscribers on proc_case.proc_case_id = proc_case_subscribers.proc_case_id ")
			.append("JOIN ic_user ON ic_user.IC_USER_ID = proc_case_subscribers.IC_USER_ID ")
			.append("AND ic_user.PRIMARY_GROUP IN (").append(toStringNumbers(subscriberGroupIDs)).append(") ");
		}

		builder.append(" where ");

		builder.append(getConditionForCaseId(params, caseId, "comm_case.comm_case_id"));
		builder.append(getConditionForFromAndTo(params, from, to));
		builder.append(getConditionForProcInstIds(params, procInstIds, "cp." + CaseProcInstBind.procInstIdColumnName));

		builder.append(" comm_case.handler = :").append(NativeIdentityBind.identityIdProperty);
		builder.append(getConditionForCaseStatuses(params, caseStatusesToShow, caseStatusesToHide));
		builder.append(" and proc_case.case_manager_type is null) order by Created desc");

		String query = builder.toString();
		try {
			return getResults(query, params);
		} catch (HibernateException e) {
			LOGGER.log(Level.WARNING, "Error executing query:\n" + query, e);
			throw new RuntimeException(e);
		}
	}

	private String getConditionForCaseId(List<Param> params, Integer caseId, String caseColumn) {
		if (caseId == null || caseId < 0)
			return " " + caseColumn + " = " + caseColumn + " and ";

		String caseIdParam = "caseIdParam";
		params.add(new Param(caseIdParam, caseId));
		return " " + caseColumn + " = :".concat(caseIdParam).concat(" and ");
	}

	private String getConditionForProcInstIds(List<Param> params, List<Long> procInstIds, String columnName) {
		if (ListUtil.isEmpty(procInstIds))
			return CoreConstants.SPACE;

		String procInstIdsParam = "procInstIds";
		params.add(new Param(procInstIdsParam, procInstIds));
		return " " + columnName + " in (:".concat(procInstIdsParam).concat(") and ");
	}

	private String getConditionForCaseStatuses(List<Param> params, List<String> caseStatusesToShow, List<String> caseStatusesToHide) {
		return getConditionForCaseStatuses(params, caseStatusesToShow, caseStatusesToHide, false);
	}
	private String getConditionForCaseStatuses(
			String columnName,
			List<Param> params,
			List<String> caseStatusesToShow,
			List<String> caseStatusesToHide,
			boolean notIn
	) {
		//	Using statuses to show by default
		if (ListUtil.isEmpty(caseStatusesToShow)) {
			if (!ListUtil.isEmpty(caseStatusesToHide)) {
				Param param = new Param("statusesToHide", caseStatusesToHide);
				if (params != null && !params.contains(param))
					params.add(param);
				return " and " + columnName + ".case_status not in (:statusesToHide) ";
			}
		} else {
			Param param = new Param("statusesToShow", caseStatusesToShow);
			if (params != null && !params.contains(param))
				params.add(param);
			return " and " + columnName + ".case_status " + (notIn ? "not" : CoreConstants.EMPTY) + " in (:statusesToShow) ";
		}
		return CoreConstants.EMPTY;
	}
	private String getConditionForCaseStatuses(List<Param> params, List<String> caseStatusesToShow, List<String> caseStatusesToHide, boolean notIn) {
		return getConditionForCaseStatuses("proc_case", params, caseStatusesToShow, caseStatusesToHide, notIn);
	}

	private String getConditionForFromAndTo(List<Param> params, Timestamp from, Timestamp to) {
		if (from == null || to == null) {
			return CoreConstants.EMPTY;
		}

		params.add(new Param("from", from));
		params.add(new Param("to", to));
		return " proc_case.created >= :from and proc_case.created <= :to and ";
	}

	@Override
	public Map<Integer, Date> getOpenCasesIds(
			User user,
			List<String> caseCodes,
			List<String> caseStatusesToShow,
			List<String> caseStatusesToHide,
	        Collection<Integer> groups,
	        Collection<String> roles,
	        boolean onlySubscribedCases,
	        Integer caseId,
	        List<Long> procInstIds,
	        Collection<? extends Number> subscriberGroupIDs,
	        Timestamp from,
	        Timestamp to
	) {
		boolean showClosedCases = false;
		if (caseStatusesToShow.contains(CaseBMPBean.CASE_STATUS_DENIED_KEY) || caseStatusesToShow.contains(CaseBMPBean.CASE_STATUS_CLOSED) ||
				caseStatusesToShow.contains(CaseBMPBean.CASE_STATUS_FINISHED_KEY))
			showClosedCases = true;

		List<Param> params = new ArrayList<Param>();
		params.add(new Param(NativeIdentityBind.identityIdProperty, user.getPrimaryKey().toString()));
		params.add(new Param(NativeIdentityBind.identityTypeProperty, IdentityType.USER.toString()));
		if (!ListUtil.isEmpty(caseCodes))
			params.add(new Param("caseCodes", caseCodes));

		StringBuilder builder = new StringBuilder(1000);
		builder.append("(select distinct comm_case.comm_case_id as caseId, proc_case.created as Created from comm_case "
		                + "inner join proc_case on comm_case.comm_case_id = proc_case.proc_case_id "
		                + "inner join " + CaseProcInstBind.TABLE_NAME + " cp on cp.case_id = comm_case.comm_case_id "
		                + "inner join bpm_actors act on act.process_instance_id = cp.process_instance_id "
		                + "inner join jbpm_processinstance pi on pi.id_ = cp.process_instance_id "
		                + "left join bpm_native_identities ni on act.actor_id = ni.actor_fk ");

		if (onlySubscribedCases || !ListUtil.isEmpty(subscriberGroupIDs)) {
			builder.append("inner join proc_case_subscribers on proc_case.proc_case_id = proc_case_subscribers.proc_case_id ");

			if (!ListUtil.isEmpty(subscriberGroupIDs)) {
				builder.append("JOIN ic_user ON ic_user.IC_USER_ID = proc_case_subscribers.IC_USER_ID ");
				builder.append("AND ic_user.PRIMARY_GROUP IN (").append(toStringNumbers(subscriberGroupIDs)).append(") ");
			}
		}

		builder.append("where");

		builder.append(getConditionForCaseId(params, caseId, "comm_case.comm_case_id"));
		builder.append(getConditionForFromAndTo(params, from, to));
		builder.append(getConditionForProcInstIds(params, procInstIds, "cp." + CaseProcInstBind.procInstIdColumnName));

		builder.append(" (");
		if (!ListUtil.isEmpty(roles)) {
			builder.append("(act.role_name in (:roles) or (ni.identity_type = :identityTypeRole and ni.identity_id in(:roles))) or ");
			params.add(new Param("roles", roles));
			params.add(new Param("identityTypeRole", IdentityType.ROLE.toString()));
		}
		builder.append("ni.identity_id = :identityId  and  ni.identity_type = :identityType) ");
		builder.append("and act.process_instance_id is not null ");
		if (!showClosedCases) {
			builder.append("and pi.end_ is null ");
		}
		builder.append(getConditionForCaseStatuses(params, caseStatusesToShow, caseStatusesToHide));
		if (!ListUtil.isEmpty(caseCodes))
			builder.append(" and pi.processdefinition_ in (select id_ from jbpm_processdefinition where name_ in (:caseCodes)) ");
		if (onlySubscribedCases) {
			builder.append(" and (proc_case.user_id = :caseAuthor or proc_case_subscribers.ic_user_id = :subscriber) ");
			params.add(new Param("subscriber", user.getPrimaryKey()));
			params.add(new Param("caseAuthor", user.getPrimaryKey()));
		}

		//	The second part of a query
		builder.append(") union (select distinct comm_case.comm_case_id as caseId, proc_case.created as Created from comm_case ")
						.append("inner join proc_case on proc_case.proc_case_id = comm_case.comm_case_id ");
		if (!ListUtil.isEmpty(procInstIds))
			builder.append("inner join " + CaseProcInstBind.TABLE_NAME + " cp on cp.case_id = comm_case.comm_case_id ");

		if (!ListUtil.isEmpty(subscriberGroupIDs)) {
			builder.append("inner join proc_case_subscribers on proc_case.proc_case_id = proc_case_subscribers.proc_case_id ")
			.append("JOIN ic_user ON ic_user.IC_USER_ID = proc_case_subscribers.IC_USER_ID ")
			.append("AND ic_user.PRIMARY_GROUP IN (").append(toStringNumbers(subscriberGroupIDs)).append(") ");
		}

		builder.append(" where ");

		builder.append(getConditionForCaseId(params, caseId, "comm_case.comm_case_id"));
		builder.append(getConditionForFromAndTo(params, from, to));
		builder.append(getConditionForProcInstIds(params, procInstIds, "cp." + CaseProcInstBind.procInstIdColumnName));

		builder.append(" proc_case.case_manager_type is null");
		if (!ListUtil.isEmpty(groups)) {
			builder.append(" and proc_case.handler_group_id in (:groups)");
			params.add(new Param("groups", groups));
		}
		if (!ListUtil.isEmpty(caseCodes))
			builder.append(" and proc_case.case_code in (:caseCodes)");
		builder.append(getConditionForCaseStatuses(params, caseStatusesToShow, caseStatusesToHide));
		builder.append(") order by Created desc");

		Map<Integer, Date> results = getResults(builder.toString(), params);
		return results;
	}

	@Override
	public Map<Integer, Date> getOpenCasesIdsForAdmin(
			List<String> caseCodes,
			List<String> caseStatusesToShow,
			List<String> caseStatusesToHide,
			Integer caseId,
			List<Long> procInstIds,
			Collection<? extends Number> subscriberGroupIDs,
			Timestamp from,
			Timestamp to
	) {
		boolean showClosedCases = false;
		if (caseStatusesToShow.contains(CaseBMPBean.CASE_STATUS_DENIED_KEY) || caseStatusesToShow.contains(CaseBMPBean.CASE_STATUS_CLOSED) ||
				caseStatusesToShow.contains(CaseBMPBean.CASE_STATUS_FINISHED_KEY))
			showClosedCases = true;

		List<Param> params = new ArrayList<Param>();
		if (!ListUtil.isEmpty(caseCodes)) {
			params.add(new Param("caseCodes", caseCodes));
		}
		StringBuilder builder = new StringBuilder(1000);
		builder.append("(select distinct comm_case.comm_case_id as caseId, proc_case.created as Created from comm_case ")
				.append("inner join proc_case on comm_case.comm_case_id = proc_case.proc_case_id ")
		        .append("inner join " + CaseProcInstBind.TABLE_NAME + " cp on cp.case_id = comm_case.comm_case_id ")
		        .append("inner join bpm_actors act on act.process_instance_id = cp.process_instance_id ")
		        .append("inner join jbpm_processinstance pi on pi.id_ = cp.process_instance_id ")
		        .append("left join bpm_native_identities ni on act.actor_id = ni.actor_fk ");

		if (!ListUtil.isEmpty(subscriberGroupIDs)) {
			builder.append("inner join proc_case_subscribers on proc_case.proc_case_id = proc_case_subscribers.proc_case_id ")
			.append("JOIN ic_user ON ic_user.IC_USER_ID = proc_case_subscribers.IC_USER_ID ")
			.append("AND ic_user.PRIMARY_GROUP IN (").append(toStringNumbers(subscriberGroupIDs)).append(") ");
		}

		builder.append("where ");

		builder.append(getConditionForCaseId(params, caseId, "comm_case.comm_case_id"));
		builder.append(getConditionForFromAndTo(params, from, to));
		builder.append(getConditionForProcInstIds(params, procInstIds, "cp." + CaseProcInstBind.procInstIdColumnName));

		builder.append(" act.process_instance_id is not null ");
		if (!showClosedCases) {
			builder.append("and pi.end_ is null ");
		}
		builder.append(getConditionForCaseStatuses(params, caseStatusesToShow, caseStatusesToHide));
		if (!ListUtil.isEmpty(caseCodes))
			builder.append(" and pi.processdefinition_ in (select id_ from jbpm_processdefinition where name_ in (:caseCodes))");

		builder.append(") union (select distinct comm_case.comm_case_id as caseId, proc_case.created as Created from comm_case ")
						.append("inner join proc_case on proc_case.proc_case_id = comm_case.comm_case_id ");
		if (!ListUtil.isEmpty(procInstIds))
			builder.append("inner join " + CaseProcInstBind.TABLE_NAME + " cp on cp.case_id = comm_case.comm_case_id ");

		if (!ListUtil.isEmpty(subscriberGroupIDs)) {
			builder.append("inner join proc_case_subscribers on proc_case.proc_case_id = proc_case_subscribers.proc_case_id ")
			.append("JOIN ic_user ON ic_user.IC_USER_ID = proc_case_subscribers.IC_USER_ID ")
			.append("AND ic_user.PRIMARY_GROUP IN (").append(toStringNumbers(subscriberGroupIDs)).append(") ");
		}

		builder.append(" where ");

		builder.append(getConditionForCaseId(params, caseId, "comm_case.comm_case_id"));
		builder.append(getConditionForFromAndTo(params, from, to));
		builder.append(getConditionForProcInstIds(params, procInstIds, "cp." + CaseProcInstBind.procInstIdColumnName));

		builder.append(" proc_case.case_manager_type is null ");
		if (!ListUtil.isEmpty(caseCodes))
			builder.append(" and proc_case.case_code in (:caseCodes) ");

		builder.append(getConditionForCaseStatuses(params, caseStatusesToShow, caseStatusesToHide));

		builder.append(") order by Created desc");

		return getResults(builder.toString(), params);
	}

	private Map<Integer, Date> getResults(String hqlQuery, List<Param> params) {
		boolean sqlMeasurementOn = CoreUtil.isSQLMeasurementOn();
		long start = sqlMeasurementOn ? System.currentTimeMillis() : 0;
		try {
			javax.persistence.Query query = getEntityManager().createNativeQuery(hqlQuery);
			if (!ListUtil.isEmpty(params)) {
				for (Param param: params) {
					query.setParameter(param.getParamName(), param.getParamValue());
				}
			}
			@SuppressWarnings("unchecked")
			List<Object[]> data = query.getResultList();
			return getResults(data);
		} catch (Exception e) {
			String message = "Error executing query '" + hqlQuery + "', parameters: " + params;
			getLogger().log(Level.WARNING, message, e);
			CoreUtil.sendExceptionNotification(message, e);
		} finally {
			if (sqlMeasurementOn) {
				getLogger().info("Results for query '" + hqlQuery + "' and params " + params + " where loaded in " +
						(System.currentTimeMillis() - start) + " ms");
			}
		}
		return Collections.emptyMap();
	}

	private <T> Map<Integer, Date> getResults(List<T[]> data) {
		if (ListUtil.isEmpty(data)) {
			return Collections.emptyMap();
		}

		Map<Integer, Date> results = new LinkedHashMap<Integer, Date>();
		for (Object[] caseData: data) {
			if (ArrayUtil.isEmpty(caseData) || caseData.length != 2) {
				continue;
			}

			Object id = caseData[0];
			Object created = caseData[1];
			if (id instanceof Number && created instanceof Date) {
				results.put(((Number) id).intValue(), (Date) created);
			}
		}
		return results;
	}

	@Override
	public Map<Integer, Date> getClosedCasesIds(
			User user,
			List<String> caseStatusesToShow,
			List<String> caseStatusesToHide,
			Collection<Integer> groups,
			Collection<String> roles,
			boolean onlySubscribedCases,
			Integer caseId,
			List<Long> procInstIds,
			Collection<? extends Number> subscriberGroupIDs,
	        Timestamp from,
	        Timestamp to
	) {
		List<Param> params = new ArrayList<Param>();
		params.add(new Param("statusesToShow", caseStatusesToShow));
		params.add(new Param(NativeIdentityBind.identityIdProperty, user.getPrimaryKey().toString()));
		params.add(new Param(NativeIdentityBind.identityTypeProperty, NativeIdentityBind.IdentityType.USER.toString()));
		if (!ListUtil.isEmpty(caseStatusesToHide)) {
			params.add(new Param("statusesToHide", caseStatusesToHide));
		}
		StringBuilder builder = new StringBuilder(1000);
		builder.append("(select distinct comm_case.comm_case_id as caseId, proc_case.created as Created from comm_case "
		                + "inner join proc_case on comm_case.comm_case_id = proc_case.proc_case_id "
		                + "inner join " + CaseProcInstBind.TABLE_NAME + " cp on cp.case_id = comm_case.comm_case_id "
		                + "inner join bpm_actors act on act.process_instance_id = cp.process_instance_id "
		                + "inner join jbpm_processinstance pi on pi.id_ = cp.process_instance_id "
		                + "left join bpm_native_identities ni on act.actor_id = ni.actor_fk ");

		if (onlySubscribedCases || !ListUtil.isEmpty(subscriberGroupIDs)) {
			builder.append("inner join proc_case_subscribers on proc_case.proc_case_id = proc_case_subscribers.proc_case_id ");

			if (!ListUtil.isEmpty(subscriberGroupIDs)) {
				builder.append("JOIN ic_user ON ic_user.IC_USER_ID = proc_case_subscribers.IC_USER_ID ");
				builder.append("AND ic_user.PRIMARY_GROUP IN (").append(toStringNumbers(subscriberGroupIDs)).append(") ");
			}
		}

		builder.append("where");

		builder.append(getConditionForCaseId(params, caseId, "comm_case.comm_case_id"));
		builder.append(getConditionForFromAndTo(params, from, to));
		builder.append(getConditionForProcInstIds(params, procInstIds, "cp." + CaseProcInstBind.procInstIdColumnName));

		builder.append(" (");
		if (!ListUtil.isEmpty(roles)) {
			builder.append("(act.role_name in (:roles) or (ni.identity_type = :identityTypeRole and ni.identity_id in(:roles))) or ");
			params.add(new Param("roles", roles));
			params.add(new Param("identityTypeRole", IdentityType.ROLE.toString()));
		}

		builder.append("ni.identity_id = :identityId and ni.identity_type = :identityType) ");
		builder.append("and act.process_instance_id is not null and (pi.end_ is not null or proc_case.case_status in (:statusesToShow)) ");
		if (!ListUtil.isEmpty(caseStatusesToHide)) {
			builder.append("and proc_case.case_status not in (:statusesToHide)");
		}
		if (onlySubscribedCases) {
			builder.append(" and (proc_case.user_id = :caseAuthor or proc_case_subscribers.ic_user_id = :subscriber) ");
			params.add(new Param("subscriber", user.getPrimaryKey()));
			params.add(new Param("caseAuthor", user.getPrimaryKey().toString()));
		}
		builder.append(") union (select distinct comm_case.comm_case_id as caseId, proc_case.created as Created from comm_case ")
						.append("inner join proc_case on proc_case.proc_case_id = comm_case.comm_case_id ");
		if (!ListUtil.isEmpty(procInstIds))
			builder.append("inner join " + CaseProcInstBind.TABLE_NAME + " cp on cp.case_id = comm_case.comm_case_id ");

		if (!ListUtil.isEmpty(subscriberGroupIDs)) {
			builder.append("inner join proc_case_subscribers on proc_case.proc_case_id = proc_case_subscribers.proc_case_id ")
			.append("JOIN ic_user ON ic_user.IC_USER_ID = proc_case_subscribers.IC_USER_ID ")
			.append("AND ic_user.PRIMARY_GROUP IN (").append(toStringNumbers(subscriberGroupIDs)).append(") ");
		}

		builder.append(" where");

		builder.append(getConditionForCaseId(params, caseId, "comm_case.comm_case_id"));
		builder.append(getConditionForFromAndTo(params, from, to));
		builder.append(getConditionForProcInstIds(params, procInstIds, "cp." + CaseProcInstBind.procInstIdColumnName));

		builder.append(" proc_case.case_status in (:statusesToShow) ");
		if (!ListUtil.isEmpty(groups)) {
			builder.append("and proc_case.handler_group_id in (:groups) ");
			params.add(new Param("groups", groups));
		}
		if (!ListUtil.isEmpty(caseStatusesToHide)) {
			builder.append("and proc_case.case_status not in (:statusesToHide) ");
		}
		builder.append("and proc_case.case_manager_type is null) order by Created desc");

		return getResults(builder.toString(), params);
	}

	@Override
	public Map<Integer, Date> getClosedCasesIdsForAdmin(
			List<String> caseStatusesToShow,
			List<String> caseStatusesToHide,
			Integer caseId,
			List<Long> procInstIds,
			Collection<? extends Number> subscriberGroupIDs,
	        Timestamp from,
	        Timestamp to
	) {
		List<Param> params = new ArrayList<Param>();
		params.add(new Param("statusesToShow", caseStatusesToShow));
		if (!ListUtil.isEmpty(caseStatusesToHide))
			params.add(new Param("statusesToHide", caseStatusesToHide));

		StringBuilder builder = new StringBuilder(1000);
		builder.append("(select distinct comm_case.comm_case_id as caseId, proc_case.created as Created from comm_case "
		                + "inner join proc_case on comm_case.comm_case_id = proc_case.proc_case_id "
		                + "inner join " + CaseProcInstBind.TABLE_NAME + " cp on cp.case_id = comm_case.comm_case_id "
		                + "inner join bpm_actors act on act.process_instance_id = cp.process_instance_id "
		                + "inner join jbpm_processinstance pi on pi.id_ = cp.process_instance_id "
		                + "left join bpm_native_identities ni on act.actor_id = ni.actor_fk ");

		if (!ListUtil.isEmpty(subscriberGroupIDs)) {
			builder.append("inner join proc_case_subscribers on proc_case.proc_case_id = proc_case_subscribers.proc_case_id ")
			.append("JOIN ic_user ON ic_user.IC_USER_ID = proc_case_subscribers.IC_USER_ID ")
			.append("AND ic_user.PRIMARY_GROUP IN (").append(toStringNumbers(subscriberGroupIDs)).append(") ");
		}

		builder.append("where ");

		builder.append(getConditionForCaseId(params, caseId, "comm_case.comm_case_id"));
		builder.append(getConditionForFromAndTo(params, from, to));
		builder.append(getConditionForProcInstIds(params, procInstIds, "cp." + CaseProcInstBind.procInstIdColumnName));

		builder.append(" act.process_instance_id is not null and (pi.end_ is not null or proc_case.case_status in (:statusesToShow))");
		if (!ListUtil.isEmpty(caseStatusesToHide))
			builder.append("and proc_case.case_status not in (:statusesToHide) ");

		builder.append(") union (select distinct comm_case.comm_case_id as caseId, proc_case.created as Created from comm_case ")
						.append("inner join proc_case on proc_case.proc_case_id = comm_case.comm_case_id ");
		if (!ListUtil.isEmpty(procInstIds))
			builder.append(" inner join " + CaseProcInstBind.TABLE_NAME + " cp on cp.case_id = comm_case.comm_case_id ");

		if (!ListUtil.isEmpty(subscriberGroupIDs)) {
			builder.append("inner join proc_case_subscribers on proc_case.proc_case_id = proc_case_subscribers.proc_case_id ")
			.append("JOIN ic_user ON ic_user.IC_USER_ID = proc_case_subscribers.IC_USER_ID ")
			.append("AND ic_user.PRIMARY_GROUP IN (").append(toStringNumbers(subscriberGroupIDs)).append(") ");
		}

		builder.append(" where ");

		builder.append(getConditionForCaseId(params, caseId, "comm_case.comm_case_id"));
		builder.append(getConditionForFromAndTo(params, from, to));
		builder.append(getConditionForProcInstIds(params, procInstIds, "cp." + CaseProcInstBind.procInstIdColumnName));

		builder.append(" proc_case.case_status in (:statusesToShow) ");
		if (!ListUtil.isEmpty(caseStatusesToHide))
			builder.append("and proc_case.case_status not in (:statusesToHide) ");
		builder.append("and proc_case.case_manager_type is null) order by Created desc");

		return getResults(builder.toString(), params);
	}

	@Override
	public Map<Integer, Date> getHandlerCasesIds(
			User handler,
			Collection<String> caseStatusesToShow,
			Collection<String> caseStatusesToHide,
			Collection<String> processDefinitionNames,
			Collection<? extends Number> caseIDs,
			Collection<? extends Number> procInstIds,
			Set<String> roles,
			Collection<? extends Number> handlerCategoryIDs,
	        Date from,
	        Date to
	) {
		if (handler == null) {
			return null;
		}

		return getCasesPrimaryKeys(
				processDefinitionNames,
				procInstIds,
				caseStatusesToShow,
				caseStatusesToHide,
				null,				//	subscribersIDs
				handlerCategoryIDs,	//	subscribersGroupIDs
				Arrays.asList(Long.valueOf(handler.getPrimaryKey().toString())),
				null,	//	handlerGroupIDs
				null,	//	caseManagerTypes
				null,	//	hasCaseManagerType
				null,	//	caseCodes
				roles,
				null,	//	authorsIDs
				caseIDs != null ? caseIDs : null,
				null,	//	isAnonymous
				null,	//	generalCases
				null,	//	ended
				from,
				to
		);
	}

	@Override
	public Map<Integer, Date> getHandlerCasesIds(
			User handler,
			List<String> caseStatusesToShow,
			List<String> caseStatusesToHide,
			List<String> caseCodes,
			Collection<String> roles,
			boolean onlySubscribedCases,
			Integer caseId,
			List<Long> procInstIds,
	        Date from,
	        Date to
	) {
		if (handler == null) {
			return null;
		}

		return getCasesPrimaryKeys(
				null,
				procInstIds,
				caseStatusesToShow,
				caseStatusesToHide,
				onlySubscribedCases ? Arrays.asList(Integer.valueOf(handler.getPrimaryKey().toString())) : null,	//	subscribersIDs
				null,				//	subscribersGroupIDs
				Arrays.asList(Long.valueOf(handler.getPrimaryKey().toString())),
				null,				//	handlerGroupIDs
				null,				//	caseManagerTypes
				null,				//	hasCaseManagerType
				caseCodes,			//	caseCodes
				roles,
				null,				//	authorsIDs
				null,				//	casesIDs
				null,				//	isAnonymous
				null,				//	generalCases
				null,				//	ended
				from,
				to
		);
	}

	@Override
	public Map<Integer, Date> getUserCasesIds(
			User user,
			List<String> caseStatusesToShow,
			List<String> caseStatusesToHide,
			List<String> caseCodes,
			Collection<String> roles,
			boolean onlySubscribedCases,
			Integer caseId,
			List<Long> procInstIds,
			Collection<? extends Number> subscriberGroupIDs,
	        Timestamp from,
	        Timestamp to
	) {
		List<Param> params = new ArrayList<Param>();
		params.add(new Param("caseCodes", caseCodes));
		params.add(new Param(NativeIdentityBind.identityIdProperty, user.getPrimaryKey().toString()));
		params.add(new Param(NativeIdentityBind.identityTypeProperty, NativeIdentityBind.IdentityType.USER.toString()));
		StringBuilder builder = new StringBuilder(1000);
		builder.append("(select distinct proc_case.proc_case_id as caseId, proc_case.created as Created from proc_case ")
				.append("inner join " + CaseProcInstBind.TABLE_NAME + " cp on cp.case_id = proc_case.proc_case_id ")
				.append("inner join bpm_actors act on act.process_instance_id = cp.process_instance_id ");

		if (onlySubscribedCases || !ListUtil.isEmpty(subscriberGroupIDs)) {
			builder.append("inner join proc_case_subscribers on proc_case.proc_case_id = proc_case_subscribers.proc_case_id ");

			if (!ListUtil.isEmpty(subscriberGroupIDs)) {
				builder.append("JOIN ic_user ON ic_user.IC_USER_ID = proc_case_subscribers.IC_USER_ID ");
				builder.append("AND ic_user.PRIMARY_GROUP IN (").append(toStringNumbers(subscriberGroupIDs)).append(") ");
			}
		}

		builder.append("left join bpm_native_identities ni on act.actor_id = ni.actor_fk where ");

		builder.append(getConditionForCaseId(params, caseId, "proc_case.proc_case_id"));
		builder.append(getConditionForFromAndTo(params, from, to));
		builder.append(getConditionForProcInstIds(params, procInstIds, "cp." + CaseProcInstBind.procInstIdColumnName));

		builder.append(" (");
		if (!ListUtil.isEmpty(roles)) {
			builder.append("(act.role_name in (:roles) or (ni.identity_type = :identityTypeRole and ni.identity_id in(:roles))) or ");
			params.add(new Param("roles", roles));
			params.add(new Param("identityTypeRole", IdentityType.ROLE.toString()));
		}
		builder.append("ni.identity_id = :identityId and ni.identity_type = :identityType) ");
		builder.append("and act.process_instance_id is not null and proc_case.case_code not in (:caseCodes) ");

		builder.append(getConditionForCaseStatuses(params, caseStatusesToShow, caseStatusesToHide, true));

		if (onlySubscribedCases) {
			builder.append(" and (proc_case.user_id = :caseAuthor or proc_case_subscribers.ic_user_id = :subscriber) ");
			params.add(new Param("subscriber", user.getPrimaryKey()));
			params.add(new Param("caseAuthor", user.getPrimaryKey().toString()));
		}

		builder.append(") union (select distinct proc_case.proc_case_id as caseId, proc_case.created as Created from proc_case ");
		if (!ListUtil.isEmpty(procInstIds))
			builder.append(" inner join " + CaseProcInstBind.TABLE_NAME + " cp on cp.case_id = proc_case.proc_case_id ");

		if (!ListUtil.isEmpty(subscriberGroupIDs)) {
			builder.append("inner join proc_case_subscribers on proc_case.proc_case_id = proc_case_subscribers.proc_case_id ")
			.append("JOIN ic_user ON ic_user.IC_USER_ID = proc_case_subscribers.IC_USER_ID ")
			.append("AND ic_user.PRIMARY_GROUP IN (").append(toStringNumbers(subscriberGroupIDs)).append(") ");
		}

		builder.append(" where ");

		builder.append(getConditionForCaseId(params, caseId, "proc_case.proc_case_id"));
		builder.append(getConditionForFromAndTo(params, from, to));
		if (!ListUtil.isEmpty(procInstIds))
			builder.append(getConditionForProcInstIds(params, procInstIds, "cp." + CaseProcInstBind.procInstIdColumnName));

		builder.append(" proc_case.user_id=:identityId and proc_case.case_code not in (:caseCodes) ");

		builder.append(getConditionForCaseStatuses(params, caseStatusesToShow, caseStatusesToHide, true));

		builder.append(") order by Created desc");

		return getResults(builder.toString(), params);
	}

	@Override
	public Map<Integer, Date> getPublicCasesIds(
			Collection<String> caseStatusesToShow,
			Collection<String> caseStatusesToHide,
			Collection<String> caseCodes,
			Collection<? extends Number> caseIDs,
			Collection<? extends Number> procInstIds,
			Collection<? extends Number> handlerCategoryIDs,
	        Timestamp from,
	        Timestamp to
	) {
		boolean useCaseCodes = !ListUtil.isEmpty(caseCodes);
		boolean useProcDef = false;
		if (useCaseCodes) {
			try {
				CaseBusiness caseBusiness = IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), CaseBusiness.class);
				List<String> allStatuses = caseBusiness.getAllCasesStatuses();
				useProcDef = !allStatuses.contains(caseCodes.iterator().next());
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "", e);
			}
		}

		if (useCaseCodes) {
			if (useProcDef) {
				return getCasesPrimaryKeys(caseCodes, procInstIds,
						caseStatusesToShow,	caseStatusesToHide, null,
						handlerCategoryIDs, null, null, null, null, null, null,
						null, caseIDs, Boolean.TRUE, Boolean.TRUE, null, from, to);
			} else {
				return getCasesPrimaryKeys(null, procInstIds,
						caseStatusesToShow,	caseStatusesToHide, null,
						handlerCategoryIDs, null, null, null, null, caseCodes,
						null, null, caseIDs, Boolean.TRUE, Boolean.TRUE, null, from, to);
			}
		} else {
			return getCasesPrimaryKeys(null, procInstIds,
					caseStatusesToShow,	caseStatusesToHide, null,
					handlerCategoryIDs, null, null, null, null, null, null,
					null, caseIDs, Boolean.TRUE, Boolean.TRUE, null, from, to);
		}
	}

	@Override
	public List<Integer> getCasesIdsByStatusForAdmin(List<String> caseStatusesToShow, List<String> caseStatusesToHide) {
		StringBuilder builder = new StringBuilder(200);
		List<Param> params = new ArrayList<Param>();
		params.add(new Param("statusToShow", caseStatusesToShow));
		builder.append("select comm_case.comm_case_id as caseId from comm_case "
		                + "inner join proc_case on comm_case.comm_case_id = proc_case.proc_case_id "
		                + "where proc_case.case_status in(:statusToShow) ");
		if (!ListUtil.isEmpty(caseStatusesToHide)) {
			builder.append("and proc_case.case_status in(:statusesToHide) ");
			params.add(new Param("statusesToHide", caseStatusesToHide));
		}
		builder.append("order by proc_case.created desc");

		return getQueryNativeInline(builder.toString()).getResultList(Integer.class, "caseId", params.toArray(new Param[params.size()]));
	}

	@Override
	public List<Long> getProcessInstancesByCaseStatusesAndProcessDefinitionNames(List<String> caseStatuses, List<String> procDefNames) {
		if (ListUtil.isEmpty(caseStatuses) || ListUtil.isEmpty(procDefNames)) {
			return Collections.emptyList();
		}

		return getResultList(CaseProcInstBind.getProcInstIdsByCaseStatusesAndProcDefNames, Long.class,
				new Param(CaseProcInstBind.caseStatusParam, caseStatuses),
				new Param(CaseProcInstBind.processDefinitionNameProp, procDefNames)
		);
	}

	@Override
	public Map<Long, Integer> getProcessInstancesAndCasesIdsByCaseStatusesAndProcessDefinitionNames(List<String> caseStatuses,
			List<String> procDefNames) {
		if (ListUtil.isEmpty(caseStatuses) || ListUtil.isEmpty(procDefNames))
			return Collections.emptyMap();

		return getProcessInstancesAndCasesIdsByCaseStatusesAndProcess(caseStatuses, procDefNames, null, null, null, -1, -1, null);
	}

	@Override
	public Map<Long, Integer> getProcessInstancesAndCasesIdsByCaseStatusesAndProcessDefinitionNames(List<String> caseStatuses,
			List<String> procDefNames, Param metadata, int offset, int maxCount, String endDate) {
		if (ListUtil.isEmpty(caseStatuses) || ListUtil.isEmpty(procDefNames))
			return Collections.emptyMap();

		return getProcessInstancesAndCasesIdsByCaseStatusesAndProcess(caseStatuses, procDefNames, null, null, metadata, offset, maxCount, endDate);
	}
	@Override
	public Map<Long, Integer> getProcessInstancesAndCasesIdsByCaseStatusesAndProcessInstanceIds(List<String> caseStatuses,
			List<Long> procInstIds) {
		if (ListUtil.isEmpty(caseStatuses) || ListUtil.isEmpty(procInstIds))
			return Collections.emptyMap();

		return getProcessInstancesAndCasesIdsByCaseStatusesAndProcess(caseStatuses, null, procInstIds, null, null, -1, -1, null);
	}

	@Override
	public Long getProcessInstanceIdByCaseIdAndMetaData(String caseId, Param metadata) {
		String query =	"select bind." + CaseProcInstBind.procInstIdColumnName + " from " + CaseProcInstBind.TABLE_NAME + " bind, " +
				CaseBMPBean.TABLE_NAME + " pc, " + MetaDataBMPBean.TABLE_NAME + CoreConstants.UNDER + CaseBMPBean.TABLE_NAME + " mb, " +
				MetaDataBMPBean.TABLE_NAME + " m where bind." +	CaseProcInstBind.caseIdColumnName + " = " + caseId + " and bind." +
				CaseProcInstBind.caseIdColumnName + " = pc.proc_case_id and pc.proc_case_id = mb.proc_case_id and m.IC_METADATA_ID = mb.IC_METADATA_ID";
		query += " and m." + MetaDataBMPBean.COLUMN_META_KEY + " = '" + metadata.getParamName() + "' and m." + MetaDataBMPBean.COLUMN_META_VALUE +
				" = '" + metadata.getParamValue() + "'";

		List<Serializable[]> data = null;
		try {
			data = SimpleQuerier.executeQuery(query, 1);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: " + query, e);
		}
		if (ListUtil.isEmpty(data))
			return null;

		for (Serializable[] ids: data) {
			if (ArrayUtil.isEmpty(ids))
				continue;

			Serializable piId = ids[0];
			if (piId instanceof Number)
				return ((Number) piId).longValue();
		}

		return null;
	}

	private Map<Long, Integer> getProcessInstancesAndCasesIdsByCaseStatusesAndProcess(List<String> caseStatuses, List<String> procDefNames,
			List<Long> procInstIds, Map<Long, Integer> results, Param metadata, int offset, int maxCount, String endDate) {

		if (results == null)
			results = new LinkedHashMap<Long, Integer>();

		if (ListUtil.isEmpty(procDefNames) && ListUtil.isEmpty(procInstIds))
			return results;

		StringBuilder statusesProp = new StringBuilder();
		for (Iterator<String> statusesIter = caseStatuses.iterator(); statusesIter.hasNext();) {
			statusesProp.append(CoreConstants.QOUTE_SINGLE_MARK).append(statusesIter.next()).append(CoreConstants.QOUTE_SINGLE_MARK);
			if (statusesIter.hasNext())
				statusesProp.append(CoreConstants.COMMA).append(CoreConstants.SPACE);
		}

		boolean useProcDefs = !ListUtil.isEmpty(procDefNames);
		StringBuilder processesProp = new StringBuilder();
		if (useProcDefs) {
			for (Iterator<String> procDefNamesIter = procDefNames.iterator(); procDefNamesIter.hasNext();) {
				processesProp.append(CoreConstants.QOUTE_SINGLE_MARK).append(procDefNamesIter.next()).append(CoreConstants.QOUTE_SINGLE_MARK);
				if (procDefNamesIter.hasNext())
					processesProp.append(CoreConstants.COMMA).append(CoreConstants.SPACE);
			}
			procDefNames = null;
		} else {
			List<Long> usedIds = null;
			if (procInstIds.size() > 1000) {
				usedIds = new ArrayList<Long>(procInstIds.subList(0, 1000));
				procInstIds = new ArrayList<Long>(procInstIds.subList(1000,	procInstIds.size()));
			} else {
				usedIds = new ArrayList<Long>(procInstIds);
				procInstIds = null;
			}
			for (Iterator<Long> procInstIdsIter = usedIds.iterator(); procInstIdsIter.hasNext();) {
				processesProp.append(procInstIdsIter.next());
				if (procInstIdsIter.hasNext())
					processesProp.append(CoreConstants.COMMA).append(CoreConstants.SPACE);
			}
		}

		String query = "select bind." + CaseProcInstBind.procInstIdColumnName + ", bind." + CaseProcInstBind.caseIdColumnName + " from " +
						CaseProcInstBind.TABLE_NAME + " bind, " + CaseBMPBean.TABLE_NAME + " pc, JBPM_PROCESSINSTANCE pi";
		if (useProcDefs)
			query += ", JBPM_PROCESSDEFINITION pd";

		boolean useMetaData = metadata != null;
		if (useMetaData)
			query += ", " + MetaDataBMPBean.TABLE_NAME + CoreConstants.UNDER + CaseBMPBean.TABLE_NAME + " mb, " + MetaDataBMPBean.TABLE_NAME + " m";

		query += " where ";

		if (useProcDefs)
			query += " pd.name_ in (" + processesProp.toString() + ") ";
		else
			query += " pi.id_ in (" + processesProp.toString() + ")";

		query += " and pc.CASE_STATUS in (" + statusesProp.toString() + ") and bind." +	CaseProcInstBind.procInstIdColumnName +
				" = pi.id_ and bind." + CaseProcInstBind.caseIdColumnName + " = pc.proc_case_id";
		if (useProcDefs)
			query += " and pd.id_ = pi.processdefinition_";
		if (useMetaData) {
			query += " and pc.proc_case_id = mb.proc_case_id and m.IC_METADATA_ID = mb.IC_METADATA_ID";
			query += " and m." + MetaDataBMPBean.COLUMN_META_KEY + " = '" + metadata.getParamName() + "' and m." +
					MetaDataBMPBean.COLUMN_META_VALUE + " = '" + metadata.getParamValue() + "'";
		}
		if (!StringUtil.isEmpty(endDate)) {
			query += " and (pi.end_ is null or pi.end_ >= '" + endDate + "')";
		}

		query += " order by pc.created desc";
		if (maxCount >= 0)
			query += " limit " + maxCount;
		if (offset > 0)
			query += " offset " + (offset - 1);

		List<Serializable[]> data = null;
		try {
			data = SimpleQuerier.executeQuery(query, 2);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: " + query, e);
		}
		if (ListUtil.isEmpty(data))
			return Collections.emptyMap();

		for (Serializable[] ids: data) {
			if (ArrayUtil.isEmpty(ids) || ids.length != 2)
				continue;

			Serializable piId = ids[0];
			Serializable caseId = ids[1];
			if (piId instanceof Number && caseId instanceof Number)
				results.put(((Number) piId).longValue(), ((Number) caseId).intValue());
		}

		return getProcessInstancesAndCasesIdsByCaseStatusesAndProcess(
				caseStatuses,
				procDefNames,
				procInstIds,
				results,
				metadata,
				offset,
				maxCount,
				endDate
		);
	}

	@Override
	public Long getProcessInstanceIdByCaseSubject(String subject) {
		if (StringUtil.isEmpty(subject)) {
			LOGGER.warning("Case subject is not provided!");
			return null;
		}

		List<Serializable[]> data = null;
		String query = "select b.process_instance_id from " + CaseProcInstBind.TABLE_NAME + " b, proc_case c where c.CASE_SUBJECT = '" + subject +
				"' and b.case_id = c.PROC_CASE_ID";
		try {
			data = SimpleQuerier.executeQuery(query, 1);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: " + query, e);
		}
		if (ListUtil.isEmpty(data))
			return null;

		Serializable[] ids = data.get(0);
		if (ArrayUtil.isEmpty(ids))
			return null;

		Serializable id = ids[0];
		if (id instanceof Number)
			return ((Number) id).longValue();

		return null;
	}

	@Override
	public List<Integer> getCasesIdsByHandlersAndProcessDefinition(List<Integer> handlersIds, String procDefName) {
		if (ListUtil.isEmpty(handlersIds) || StringUtil.isEmpty(procDefName))
			return null;

		StringBuilder ids = new StringBuilder();
		for (Iterator<Integer> handlersIter = handlersIds.iterator(); handlersIter.hasNext();) {
			ids.append(handlersIter.next());
			if (handlersIter.hasNext())
				ids.append(CoreConstants.COMMA).append(CoreConstants.SPACE);
		}
		String query = "select distinct c.COMM_CASE_ID from comm_case c inner join " + CaseProcInstBind.TABLE_NAME +
			" b on b.case_id = c.COMM_CASE_ID inner join jbpm_processinstance p"
			.concat(" on p.id_ = b.process_instance_id inner join jbpm_processdefinition d on d.id_ = p.processdefinition_ where d.name_ = '")
			.concat(procDefName).concat("' and c.handler in (").concat(ids.toString()).concat(")");
		List<Serializable[]> cases = null;
		try {
			cases = SimpleQuerier.executeQuery(query, 1);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: " + query, e);
		}
		if (ListUtil.isEmpty(cases))
			return null;

		List<Integer> casesIds = new ArrayList<Integer>();
		for (Serializable[] caseId: cases) {
			if (ArrayUtil.isEmpty(caseId))
				continue;

			Serializable id = caseId[0];
			if (id instanceof Number)
				casesIds.add(((Number) id).intValue());
		}
		return casesIds;
	}

	@Override
	public List<Long> getProcessInstanceIdsForSubscribedCases(com.idega.user.data.User user) {
		return getProcessInstanceIdsForSubscribedCases(Integer.valueOf(user.getId()));
	}

	@Override
	public List<Long> getProcessInstanceIdsForSubscribedCases(Integer userId) {
		return getProcessInstanceIdsForSubscribedCases(userId, null);
	}

	@Override
	public List<Long> getProcessInstanceIdsForSubscribedCases(Integer userId, List<Long> procInstIds) {
		if (userId == null)
			return null;

		String query = "select distinct b." + CaseProcInstBind.procInstIdColumnName + " from " + CaseProcInstBind.TABLE_NAME + " b, " +
				CaseBMPBean.COLUMN_CASE_SUBSCRIBERS + " s where s." + com.idega.user.data.User.FIELD_USER_ID + " = " + userId + " and s." +
				CaseBMPBean.PK_COLUMN + " = b." + CaseProcInstBind.caseIdColumnName;

		//	Checking if concrete processes are provided
		if (!ListUtil.isEmpty(procInstIds)) {
			query = query.concat(" and b.").concat(CaseProcInstBind.procInstIdColumnName).concat(" in (");
			StringBuilder ids = new StringBuilder();
			for (Iterator<Long> idsIter = procInstIds.iterator(); idsIter.hasNext();) {
				ids.append(idsIter.next());
				if (idsIter.hasNext())
					ids.append(CoreConstants.COMMA).append(CoreConstants.SPACE);
			}
			query = query.concat(ids.toString()).concat(")");
		}

		List<Serializable[]> data = null;
		try {
			data = SimpleQuerier.executeQuery(query, 1);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: " + query, e);
		}
		if (ListUtil.isEmpty(data))
			return null;

		List<Long> subscribed = new ArrayList<Long>();
		for (Serializable[] id: data) {
			if (ArrayUtil.isEmpty(id))
				continue;

			Object procId = id[0];
			if (procId instanceof Number) {
				subscribed.add(((Number) procId).longValue());
			}
		}
		return subscribed;
	}

	@Override
	public Map<Long, Integer> getProcessInstancesAndCasesIdsByUserAndProcessDefinition(com.idega.user.data.User user, String processDefinitionName) {
		String query = "select distinct pi.id_, c.proc_case_id from jbpm_processinstance pi, jbpm_processdefinition pd, proc_case c, " +
				CaseProcInstBind.TABLE_NAME + " b where c." + CaseBMPBean.COLUMN_USER + " = " + user.getId() + " and c." + CaseBMPBean.TABLE_NAME +
				"_ID = b." + CaseProcInstBind.caseIdColumnName + " and b." + CaseProcInstBind.procInstIdColumnName + " = pi.id_ and pd.name_ = '" +
				processDefinitionName + "' and pi.PROCESSDEFINITION_ = pd.id_";
		List<Serializable[]> data = null;
		try {
			data = SimpleQuerier.executeQuery(query, 2);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query:\n" + query, e);
		}
		if (ListUtil.isEmpty(data))
			return null;

		Map<Long, Integer> results = new HashMap<Long, Integer>();
		for (Serializable[] ids: data) {
			if (ArrayUtil.isEmpty(ids) || ids.length != 2)
				continue;

			Serializable piId = ids[0];
			Serializable caseId = ids[1];
			if (piId instanceof Number && caseId instanceof Number)
				results.put(((Number) piId).longValue(), ((Number) caseId).intValue());
		}

		return results;
	}

	@Override
	public List<Long> getProcessInstanceIdsByUserAndProcessDefinition(com.idega.user.data.User user, String processDefinitionName) {
		Map<Long, Integer> results = getProcessInstancesAndCasesIdsByUserAndProcessDefinition(user, processDefinitionName);
		if (MapUtil.isEmpty(results))
			return Collections.emptyList();

		return new ArrayList<Long>(results.keySet());
	}

	private Collection<Case> getCasesByProcessDefinition(String processDefinition) {
		if (StringUtil.isEmpty(processDefinition))
			return null;

		List<Long> casesIds = getCaseIdsByProcessDefinition(processDefinition);
		if (ListUtil.isEmpty(casesIds))
			return null;

		List<Integer> ids = new ArrayList<Integer>();
		for (Long id: casesIds)
			ids.add(id.intValue());
		try {
			CaseBusiness caseBusiness = IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), CaseBusiness.class);
			return caseBusiness.getCasesByIds(ids);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean doSubscribeToCasesByProcessDefinition(com.idega.user.data.User user, String processDefinitionName) {
		return doSubscribeToCases(user, getCasesByProcessDefinition(processDefinitionName));
	}

	@Override
	public boolean doSubscribeToCasesByProcessInstanceIds(com.idega.user.data.User user, List<Long> procInstIds) {
		if (ListUtil.isEmpty(procInstIds))
			return false;

		List<Integer> casesIds = getCasesIdsByProcInstIds(procInstIds);
		if (ListUtil.isEmpty(casesIds))
			return false;

		try {
			CaseBusiness caseBusiness = IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), CaseBusiness.class);
			return doSubscribeToCases(user, caseBusiness.getCasesByIds(casesIds));
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error subscribing to cases " + casesIds, e);
		}
		return false;
	}

	private boolean doSubscribeToCases(com.idega.user.data.User user, Collection<Case> cases) {
		if (user == null || ListUtil.isEmpty(cases))
			return false;

		for (Case theCase: cases) {
			try {
				theCase.addSubscriber(user);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean doUnSubscribeFromCasesByProcessDefinition(com.idega.user.data.User user, String processDefinitionName) {
		if (user == null)
			return false;

		Collection<Case> cases = getCasesByProcessDefinition(processDefinitionName);
		if (ListUtil.isEmpty(cases))
			return false;

		for (Case theCase: cases) {
			try {
				theCase.removeSubscriber(user);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}

		return true;
	}

	@Override
	public List<Long> getProcessInstancesByCasesIds(List<Integer> casesIds) {
		if (ListUtil.isEmpty(casesIds))
			return Collections.emptyList();

		List<Long> ids = getResultList(
				CaseProcInstBind.getProcInstIds_BY_CASES_IDS_QUERY_NAME,
				Long.class,
				new Param(CaseProcInstBind.casesIdsParam, casesIds)
		);

		return ids;
	}

	@Override
	public Map<Long, Integer> getProcessInstancesAndCasesIdsByCasesIds(List<Integer> casesIds) {
		List<CaseProcInstBind> binds = getCasesProcInstBindsByCasesIds(casesIds);
		if (ListUtil.isEmpty(binds))
			return Collections.emptyMap();

		Map<Long, Integer> results = new HashMap<Long, Integer>();
		for (CaseProcInstBind bind: binds)
			results.put(bind.getProcInstId(), bind.getCaseId());
		return results;
	}

	private VariableInstanceQuerier getVariableInstanceQuerier() {
		if (querier == null)
			ELUtil.getInstance().autowire(this);
		return querier;
	}

	@Override
	public Map<Long, List<VariableInstanceInfo>> getBPMValuesByCasesIdsAndVariablesNames(List<String> casesIds, List<String> names) {
		if (ListUtil.isEmpty(casesIds) || ListUtil.isEmpty(names))
			return null;

		StringBuilder tmpCases = new StringBuilder();
		for (Iterator<String> casesIdsIter = casesIds.iterator(); casesIdsIter.hasNext();) {
			tmpCases.append(casesIdsIter.next());
			if (casesIdsIter.hasNext())
				tmpCases.append(CoreConstants.COMMA).append(CoreConstants.SPACE);
		}
		StringBuilder tmpNames = new StringBuilder();
		for (Iterator<String> namesIter = names.iterator(); namesIter.hasNext();) {
			tmpNames.append(CoreConstants.QOUTE_SINGLE_MARK).append(namesIter.next()).append(CoreConstants.QOUTE_SINGLE_MARK);
			if (namesIter.hasNext())
				tmpNames.append(CoreConstants.COMMA).append(CoreConstants.SPACE);
		}

		String query = "select v.id_, v.name_, v.class_, v.stringvalue_, v.LONGVALUE_, v.DOUBLEVALUE_, v.DATEVALUE_, v.BYTEARRAYVALUE_, " +
				"v.TASKINSTANCE_, v.processinstance_, b." + CaseProcInstBind.caseIdColumnName + " from jbpm_variableinstance v, " +
				CaseProcInstBind.TABLE_NAME + " b where b." +	CaseProcInstBind.caseIdColumnName + " in (" + tmpCases.toString() +
				") and v.processinstance_ = b." + CaseProcInstBind.procInstIdColumnName + " and v.name_ in (" + tmpNames.toString() +
				") and v.CLASS_ <> '" + VariableInstanceType.NULL.getTypeKeys().get(0) + "'";
		List<Serializable[]> data = null;
		try {
			data = SimpleQuerier.executeQuery(query, 11);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: " + query, e);
		}

		return getVariableInstanceQuerier().getGroupedVariables(getVariableInstanceQuerier().getConverted(data));
	}

	/**
	 *
	 * @param casesIDs in {@link String} for to convert, not <code>null</code>;
	 * @return converted {@link String}s or {@link Collections#emptyList()}
	 * on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	protected List<Integer> convertIDs(String[] casesIDs) {
		if (ArrayUtil.isEmpty(casesIDs)) {
			return Collections.emptyList();
		}

		Map<Integer, Boolean> ids = new HashMap<Integer, Boolean>();
		for (String id: casesIDs) {
			ids.put(Integer.valueOf(id), Boolean.TRUE);
		}

		return new ArrayList<Integer>(ids.keySet());
	}

	/**
	 * @param collection to convert, not <code>null</code>;
	 * @return comma separated string of values or <code>null</code> on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	protected String toString(Collection<String> collection) {
		if (ListUtil.isEmpty(collection)) {
			return null;
		}

		StringBuilder sb = new StringBuilder();

		for (Iterator<String> iterator = collection.iterator(); iterator.hasNext();) {
			sb.append(CoreConstants.QOUTE_SINGLE_MARK)
			.append(iterator.next())
			.append(CoreConstants.QOUTE_SINGLE_MARK);

			if (iterator.hasNext()) {
				sb.append(CoreConstants.COMMA).append(CoreConstants.SPACE);
			}
		}

		return sb.toString();
	}

	/**
	 * @param collection to convert, not <code>null</code>;
	 * @return comma separated string of values or <code>null</code> on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	protected String toStringNumbers(Collection<? extends Number> collection) {
		if (ListUtil.isEmpty(collection)) {
			return null;
		}

		StringBuilder sb = new StringBuilder();

		for (Iterator<? extends Number> iterator = collection.iterator(); iterator.hasNext();) {
			sb.append(iterator.next());

			if (iterator.hasNext()) {
				sb.append(CoreConstants.COMMA).append(CoreConstants.SPACE);
			}
		}

		return sb.toString();
	}

	/**
	 * @param collection to convert, not <code>null</code>;
	 * @return comma separated string of values or <code>null</code> on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	protected String toStringUsers(Collection<com.idega.user.data.User> collection) {
		if (ListUtil.isEmpty(collection)) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (Iterator<com.idega.user.data.User> iterator = collection.iterator(); iterator.hasNext();) {
			sb.append(CoreConstants.QOUTE_SINGLE_MARK)
			.append(iterator.next().getPrimaryKey().toString())
			.append(CoreConstants.QOUTE_SINGLE_MARK);

			if (iterator.hasNext()) {
				sb.append(CoreConstants.COMMA).append(CoreConstants.SPACE);
			}
		}

		return sb.toString();
	}

	private String getCasesPrimaryKeysQuery(
			Collection<String> processDefinitionNames,
			Collection<? extends Number> processInstanceIds,
			Collection<String> caseStatuses,
			Collection<String> caseStatusesToHide,
			Collection<? extends Number> subscribersIDs,
			Collection<? extends Number> subscriberGroupsIDs,
			Collection<? extends Number> handlersIDs,
			Collection<? extends Number> handlerGroupIds,
			Collection<String> caseManagerTypes,
			Boolean hasCaseManagerType,
			Collection<String> caseCodes,
			Collection<String> roles,
			Collection<? extends Number> authorsIDs,
			Collection<? extends Number> casesIds,
			Boolean isAnonymous,
			Boolean isGeneralCases,
			Boolean hasEnded,
			Date dateCreatedFrom,
			Date dateCreatedTo
	) {

		StringBuilder query = new StringBuilder();
		query.append("SELECT bcpi.case_id, case_created created FROM bpm_cases_processinstances bcpi ");

		if (!ListUtil.isEmpty(processDefinitionNames) || hasEnded != null) {
			query.append("JOIN jbpm_processinstance jpi ON bcpi.process_instance_id=jpi.ID_ ");

			/* Selected processes that are ended or not */
			if (hasEnded != null) {
				if (hasEnded) {
					query.append("AND jpi.END_ IS NOT NULL ");
				} else {
					query.append("AND jpi.END_ IS NULL ");
				}
			}

			/* When process definition names are given, adding them to search query */
			if (!ListUtil.isEmpty(processDefinitionNames)) {
				query.append("JOIN jbpm_processdefinition jpd ON jpd.ID_=jpi.PROCESSDEFINITION_ ")
				.append("AND jpd.NAME_ IN (").append(toString(processDefinitionNames)).append(") ");
			}
		}

		String caseCreatedColumn = null;
		/* Searching by case statuses or case manage types */
		if (
				!ListUtil.isEmpty(caseStatuses) ||
				!ListUtil.isEmpty(caseStatusesToHide) ||
				!ListUtil.isEmpty(caseManagerTypes) ||
				!ListUtil.isEmpty(caseCodes) ||
				!ListUtil.isEmpty(authorsIDs) ||
				!ListUtil.isEmpty(casesIds) ||
				!ListUtil.isEmpty(handlerGroupIds) ||
				dateCreatedFrom != null ||
				dateCreatedTo != null) {

			query.append("JOIN proc_case pc ON bcpi.case_id=pc.PROC_CASE_ID ");
			caseCreatedColumn = "pc." + CaseBMPBean.COLUMN_CREATED;

			/* Searching by case statuses */
			if (!ListUtil.isEmpty(caseStatuses)) {
				query.append("AND pc.CASE_STATUS IN (").append(toString(caseStatuses)).append(") ");
			}

			/* Searching by case statuses */
			if (!ListUtil.isEmpty(caseStatusesToHide)) {
				query.append("AND pc.CASE_STATUS NOT IN (").append(toString(caseStatusesToHide)).append(") ");
			}

			/* Searching by case manager types */
			if (!ListUtil.isEmpty(caseManagerTypes)) {
				query.append("AND pc.CASE_MANAGER_TYPE IN (").append(toString(caseManagerTypes)).append(") ");
			}

			/* When cases manager must be null or not null. Priority is to
			 * defined manager types
			 */
			if (hasCaseManagerType != null && ListUtil.isEmpty(caseManagerTypes)) {
				if (hasCaseManagerType) {
					query.append("AND pc.CASE_MANAGER_TYPE IS NOT NULL ");
				} else {
					query.append("AND pc.CASE_MANAGER_TYPE IS NULL ");
				}
			}

			/* Filter by case codes */
			if (!ListUtil.isEmpty(caseCodes)) {
				query.append("AND pc.CASE_CODE IN (").append(toString(caseCodes)).append(") ");
			}

			/* Filter by authors */
			if (!ListUtil.isEmpty(authorsIDs)) {
				query.append("AND pc.USER_ID IN (").append(toStringNumbers(authorsIDs)).append(") ");
			}

			/* Filtering subset, if required */
			if (!ListUtil.isEmpty(casesIds)) {
				query.append("AND pc.PROC_CASE_ID IN (").append(toStringNumbers(casesIds)).append(") ");
			}

			/* Filtering by handler group ids */
			if (!ListUtil.isEmpty(handlerGroupIds)) {
				query.append("AND pc.HANDLER_GROUP_ID IN (").append(toStringNumbers(handlerGroupIds)).append(") ");
			}
		} else {
			caseCreatedColumn = "bcpi.DATE_CREATED";
		}

		/* Filter by handlers */
		if (!ListUtil.isEmpty(handlersIDs)) {
			query.append("JOIN jbpm_variableinstance jvi ")
			.append("ON bcpi.process_instance_id = jvi.PROCESSINSTANCE_ ")
			.append("AND jvi.NAME_='handlerUserId' ")
			.append("AND jvi.LONGVALUE_ IN (").append(toStringNumbers(handlersIDs)).append(") ");
		}

		if (!ListUtil.isEmpty(subscribersIDs) || !ListUtil.isEmpty(subscriberGroupsIDs)) {
			query.append("JOIN proc_case_subscribers pcs ON pcs.PROC_CASE_ID=bcpi.case_id ");

			/* Searching by subscribers */
			if (!ListUtil.isEmpty(subscribersIDs)) {
				query.append("AND pcs.IC_USER_ID IN (").append(toStringNumbers(subscribersIDs)).append(") ");
			}

			/* Searching by groups of subscribers */
			if (!ListUtil.isEmpty(subscriberGroupsIDs)) {
				query.append("JOIN ic_user iu ON iu.IC_USER_ID = pcs.IC_USER_ID ")
				.append("AND iu.PRIMARY_GROUP IN (").append(toStringNumbers(subscriberGroupsIDs)).append(") ");
			}
		}

		/* Filter by roles */
		if (!ListUtil.isEmpty(roles)) {
			query.append("JOIN bpm_actors ba ")
			.append("ON bcpi.process_instance_id = ba.process_instance_id ")
			.append("AND ba.role_name IN (").append(toString(roles)).append(") ");
		}

		if (isAnonymous != null || (isGeneralCases != null && isGeneralCases)) {
			/* Selecting only general cases */
			query.append("JOIN comm_case cc ON bcpi.case_id = cc.COMM_CASE_ID ");

			/* Selecting by anonymous property of general case */
			if (isAnonymous != null) {
				if (isAnonymous) {
					query.append("AND cc.IS_ANONYMOUS = 'Y' ");
				} else {
					query.append("AND cc.IS_ANONYMOUS = 'N' ");
				}
			}
		}

		String pattern = "yyyy-MM-dd HH:mm:ss.S";
		/*
		 * Filtering by date floor
		 */
		if (dateCreatedFrom != null) {
			IWTimestamp iwFrom = new IWTimestamp(dateCreatedFrom);
			query.append("AND case_created >= '").append(iwFrom.getDateString(pattern)).append("' ");
		}

		/*
		 * Filtering by date ceiling
		 */
		if (dateCreatedTo != null) {
			IWTimestamp iwTo = new IWTimestamp(dateCreatedTo);
			query.append("AND case_created <= '").append(iwTo.getDateString(pattern + "SS")).append("' ");
		}

		String sql = query.toString();
		sql = StringHandler.replace(sql, "case_created", caseCreatedColumn);
		return sql;
	}

	/*
	 * (non-Javadoc)
	 * @see com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO#getCasesPrimaryKeys(java.util.Collection, java.util.Collection, java.util.Collection, java.util.Collection, java.util.Collection, java.util.Collection)
	 */
	@Override
	public Map<Integer, Date> getCasesPrimaryKeys(
			Collection<String> processDefinitionNames,
			Collection<? extends Number> processInstanceIds,
			Collection<String> caseStatuses,
			Collection<String> caseStatusesToHide,
			Collection<? extends Number> subscribersIDs,
			Collection<? extends Number> subscribersGroupIDs,
			Collection<? extends Number> handlersIDs,
			Collection<? extends Number> handlerGroupIDs,
			Collection<String> caseManagerTypes,
			Boolean hasCaseManagerType,
			Collection<String> caseCodes,
			Collection<String> roles,
			Collection<? extends Number> authorsIDs,
			Collection<? extends Number> casesIds,
			Boolean isAnonymous,
			Boolean generalCases,
			Boolean ended,
			Date dateCreatedFrom,
			Date dateCreatedTo
	) {

		String query = getCasesPrimaryKeysQuery(processDefinitionNames,
				processInstanceIds, caseStatuses, caseStatusesToHide,
				subscribersIDs, subscribersGroupIDs, handlersIDs, handlerGroupIDs,
				caseManagerTypes, hasCaseManagerType, caseCodes, roles, authorsIDs,
				casesIds, isAnonymous, generalCases, ended, dateCreatedFrom, dateCreatedTo);

		/* Ordering by date created */
		query = query + "ORDER BY created DESC";

		boolean sqlMeasurementOn = CoreUtil.isSQLMeasurementOn();
		Long startTimeInMillis = sqlMeasurementOn ? System.currentTimeMillis() : 0;
		try {
			List<Serializable[]> data = SimpleQuerier.executeQuery(query, 2);
			if (sqlMeasurementOn) {
				getLogger().info("Query: " + query.toString() + " executed in " + (System.currentTimeMillis() - startTimeInMillis) +
					" ms. " + (ListUtil.isEmpty(data) ? "Nothing found" : "Results are: " + data));
			}
			return getResults(data);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Unable to find ids for " + Case.class.getName() + " by query: '" + query.toString() + "'", e);
		}

		return null;
	}

	@Override
	public List<Integer> getCaseIdsByProcessDefinitionId(Long processDefinitionId) {
		if (processDefinitionId == null) {
			return null;
		}

		String procDefName = getSingleResultByInlineQuery(
				"select d.name from " + ProcessDefinition.class.getName() + " d where d.id = :id",
				String.class,
				new Param("id", processDefinitionId)
		);

		List<Long> ids = getCaseIdsByProcessDefinition(procDefName);
		if (ListUtil.isEmpty(ids)) {
			return null;
		}

		List<Integer> casesIds = new ArrayList<Integer>(ids.size());
		for (Long id: ids) {
			casesIds.add(id.intValue());
		}
		return casesIds;
	}

	@Override
	public List<Integer> getCaseIdsByProcessDefinitionIdAndStatusAndDateRange(
			Long processDefinitionId,
			String status,
			IWTimestamp from,
			IWTimestamp to
	) {
		if (processDefinitionId == null && StringUtil.isEmpty(status) && from == null && to == null) {
			getLogger().warning("Criterias are not provided");
			return null;
		}

		String procDefName = getSingleResultByInlineQuery(
				"select d.name from " + ProcessDefinition.class.getName() + " d where d.id = :id",
				String.class,
				new Param("id", processDefinitionId)
		);
		if (StringUtil.isEmpty(procDefName)) {
			return null;
		}

		String query = "select distinct b.case_id from BPM_CASES_PROCESSINSTANCES b, PROC_CASE c, JBPM_PROCESSINSTANCE pi," +
				" JBPM_PROCESSDEFINITION pd where pd.name_ = '" + procDefName + "' and pd.id_ = pi.processdefinition_ and" +
				" pi.id_ = b.process_instance_id and c.proc_case_id = b.case_id ";
		if (!StringUtil.isEmpty(status) && !String.valueOf(-1).equals(status)) {
			query += " and c.CASE_STATUS ";
			if (status.indexOf(CoreConstants.COMMA) == -1) {
				query += "= '" + status + "' ";
			} else {
				List<String> statuses = Arrays.asList(status.split(CoreConstants.COMMA));
				query += " in (";
				for (Iterator<String> statusesIter = statuses.iterator(); statusesIter.hasNext();) {
					query += "'" + statusesIter.next() + "'";
					if (statusesIter.hasNext()) {
						query += ", ";
					}
				}
				query += ") ";
			}
		}
		if (from != null) {
			query += " and c.CREATED >= '" + from.getDateString("yyyy-MM-dd") + "'";
		}
		if (to != null) {
			query += " and c.CREATED <= '" + to.getDateString("yyyy-MM-dd") + "'";
		}

		long start = System.currentTimeMillis();
		List<Serializable[]> results = null;
		try {
			results = SimpleQuerier.executeQuery(query, 1);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error executing query: " + query, e);
		} finally {
			if (CoreUtil.isSQLMeasurementOn()) {
				getLogger().info("Query '" + query + "' was executed in " + (System.currentTimeMillis() - start) + " ms");
			}
		}
		if (ListUtil.isEmpty(results)) {
			return null;
		}

		List<Integer> ids = new ArrayList<Integer>();
		for (Serializable[] result: results) {
			if (ArrayUtil.isEmpty(result)) {
				continue;
			}

			Serializable id = result[0];
			if (id instanceof Number) {
				ids.add(((Number) id).intValue());
			}
		}
		return ids;
	}

	@Override
	public int getNumberOfApplications(Long procDefId) {
		if (procDefId == null) {
			return 0;
		}

		String procDefName = bpmFactory.getBPMDAO().getProcessDefinitionNameByProcessDefinitionId(procDefId);
		if (StringUtil.isEmpty(procDefName)) {
			return 0;
		}

		Number count = getSingleResultByInlineQuery(
				"select count(distinct b.procInstId) from " + CaseProcInstBind.class.getName() + " b, " + ProcessInstance.class.getName() + " pi, " +
				ProcessDefinition.class.getName() +	" pd where pd.name = :procDefName and pi.processDefinition.name = pd.name and b.procInstId = pi.id",
				Number.class,
				new Param("procDefName", procDefName)
		);
		return count == null ? 0 : count.intValue();
	}

	@Override
	public <N extends Number> Map<Integer, Date> getHandlerCasesIds(
			User handler,
			Collection<String> caseStatusesToShow,
			Collection<String> caseStatusesToHide,
			Collection<? extends Number> subscribersIDs,
			Collection<? extends Number> subscribersGroupIDs,
			Collection<N> handlersIDs,
			Collection<? extends Number> handlerGroupIDs,
			Collection<String> caseManagerTypes,
			Boolean hasCaseManagerType,
			Collection<String> caseCodes,
			Collection<String> roles,
			Collection<? extends Number> authorsIDs,
			Collection<? extends Number> casesIds,
			Boolean isAnonymous,
			Boolean generalCases,
			Boolean hasEnded,
			Date from,
			Date to
	) {
		if (handler == null) {
			return null;
		}

		if (handlersIDs == null) {
			handlersIDs = new ArrayList<N>();
		} else {
			handlersIDs = new ArrayList<N>(handlersIDs);
		}
		Long handlerId = Long.valueOf(handler.getPrimaryKey().toString());
		handlersIDs.add((N) handlerId);

		return getCasesPrimaryKeys(
				null,				//	proc. def. names
				null,				//	proc. inst. IDs
				caseStatusesToShow,
				caseStatusesToHide,
				subscribersIDs,
				subscribersGroupIDs,
				handlersIDs,
				handlerGroupIDs,
				caseManagerTypes,
				hasCaseManagerType,
				caseCodes,
				roles,
				authorsIDs,
				casesIds == null ? null : casesIds,
				isAnonymous,
				generalCases,
				hasEnded,
				from,
				to
		);
	}

}