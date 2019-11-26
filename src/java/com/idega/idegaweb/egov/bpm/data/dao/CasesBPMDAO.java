package com.idega.idegaweb.egov.bpm.data.dao;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

import com.idega.block.process.data.Case;
import com.idega.core.persistence.GenericDao;
import com.idega.core.persistence.Param;
import com.idega.core.user.data.User;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.CaseState;
import com.idega.idegaweb.egov.bpm.data.CaseStateInstance;
import com.idega.idegaweb.egov.bpm.data.CaseTypesProcDefBind;
import com.idega.idegaweb.egov.bpm.data.ProcessUserBind;
import com.idega.jbpm.bean.BPMProcessVariable;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.data.Actor;
import com.idega.user.data.Group;
import com.idega.util.IWTimestamp;

import is.idega.idegaweb.egov.cases.data.GeneralCase;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.28 $ Last modified: $Date: 2009/07/06 16:55:26 $ by $Author: laddi $
 */
public interface CasesBPMDAO extends GenericDao {

	public static final String REPOSITORY_NAME = "casesBPMDAO";

	public abstract List<CaseTypesProcDefBind> getAllCaseTypes();

	public abstract CaseProcInstBind getCaseProcInstBindByCaseId(Integer caseId);

	public abstract CaseProcInstBind getCaseProcInstBindByProcessInstanceId(Long processInstanceId);

	public abstract List<CaseProcInstBind> getCasesProcInstBindsByCasesIds(Collection<Integer> casesIds);

	public abstract List<CaseProcInstBind> getCasesProcInstBindsByProcInstIds(List<Long> procInstIds);

	public abstract ProcessUserBind getProcessUserBind(long processInstanceId, int userId, boolean createIfNotFound);

	public abstract List<ProcessUserBind> getProcessUserBinds(int userId, Collection<Integer> casesIds);

	public abstract CaseTypesProcDefBind getCaseTypesProcDefBindByPDName(String processDefinitionName);

	public abstract void updateCaseTypesProcDefBind(CaseTypesProcDefBind bind);

	public abstract CaseProcInstBind getCaseProcInstBindLatestByDateQN(Date date);

	public abstract CaseProcInstBind getLastCreatedCaseProcInstBind();

	public abstract List<Object[]> getCaseProcInstBindProcessInstanceByDateCreatedAndCaseIdentifierId(Collection<Date> dates,
			Collection<Integer> identifierIDs);

	public abstract List<Token> getCaseProcInstBindSubprocessBySubprocessName(Long processInstanceId);

	public abstract List<Long> getCaseIdsByProcessDefinition(String processDefinitionName);
	public abstract List<Long> getCaseIdsByProcessDefinitionNameAndVariables(String processDefinitionName, List<BPMProcessVariable> variables);
	public abstract List<Integer> getCaseIdsByProcessDefinitionId(Long processDefinitionId);
	public abstract List<Integer> getCaseIdsByProcessDefinitionIdAndStatusAndDateRange(
			Long processDefinitionId,
			String status,
			IWTimestamp from,
			IWTimestamp to
	);

	public abstract List<Long> getCaseIdsByCaseNumber(String caseNumber);

	public abstract List<Long> getCaseIdsByProcessUserStatus(String status);

	public abstract List<Long> getCaseIdsByCaseStatus(String[] statuses);

	public abstract List<Long> getCaseIdsByUserIds(String userId);

	public abstract List<Long> getCaseIdsByDateRange(IWTimestamp dateFrom, IWTimestamp dateTo);

	public abstract List<Long> getCaseIdsByProcessInstanceIds(List<Long> processInstanceIds);
	public List<Integer> getCasesIdsByProcInstIds(List<Long> procInstIds);

	public List<Object[]> getCaseProcInstBindProcessInstanceByCaseIdentifier(Collection<String> identifiers);

	public abstract Map<Integer, Date> getOpenCasesIds(
			User user,
			List<String> caseCodes,
			List<String> caseStatuses,
			List<String> caseStatusesToHide,
	        Collection<Integer> groups,
	        Collection<String> roles,
	        boolean onlySubscribedCases,
	        Integer caseId,
	        List<Long> procInstIds,
	        Collection<? extends Number> subscriberGroupIDs,
			List<Integer> exceptOwnersIds,
	        Timestamp from,
	        Timestamp to,
	        Integer dataFrom,
	        Integer dataTo
	);

	public abstract Map<Integer, Date> getClosedCasesIds(
			User user,
			List<String> caseStatuses,
			List<String> caseStatusesToHide,
			Collection<Integer> groups,
			Collection<String> roles,
			boolean onlySubscribedCases,
			Integer caseId,
			List<Long> procInstIds,
			Collection<? extends Number> subscriberGroupIDs,
			List<Integer> exceptOwnersIds,
	        Timestamp from,
	        Timestamp to,
	        Integer dataFrom,
	        Integer dataTo
	);

	/**
	 * @param user
	 * @param caseStatuses
	 * @param caseStatusesToHide
	 * @return cases of not ended processes (end_ is null) whose user provided is handler of, or
	 *         what user is watching
	 */
	public abstract Map<Integer, Date> getMyCasesIds(
			User user,
			List<String> caseStatuses,
			List<String> caseStatusesToHide,
			boolean onlySubscribedCases,
			Integer caseId,
			List<Long> procInstIds,
			Collection<? extends Number> subscriberGroupIDs,
			List<Integer> exceptOwnersIds,
	        Timestamp from,
	        Timestamp to,
	        Integer dataFrom,
	        Integer dataTo
	);

	public abstract Map<Integer, Date> getUserCasesIds(
			User user,
			List<String> caseStatuses,
			List<String> caseStatusesToHide,
			List<String> caseCodes,
			Collection<String> roles,
			boolean onlySubscribedCases,
			Integer caseId,
			List<Long> procInstIds,
			Collection<? extends Number> subscriberGroupIDs,
			List<Integer> exceptOwnersIds,
	        Timestamp from,
	        Timestamp to,
	        Integer dataFrom,
	        Integer dataTo
	);

	/**
	 *
	 * @param caseStatuses is {@link Collection} of {@link Case#getCaseStatus()}
	 * of {@link Case}s that show be shown, skipped if <code>null</code>;
	 * @param caseStatusesToHide is {@link Collection} of {@link Case#getCaseStatus()}
	 * of {@link Case}s that show be hidden, skipped if <code>null</code>;
	 * @param caseCodes is {@link Collection} of {@link ProcessDefinition}s
	 * or {@link Collection} of {@link Case#getCaseCode()}s,
	 * skipped if <code>null</code>;
	 * @param caseIDs is {@link Case#getPrimaryKey()}s of {@link Case}s, that
	 * already are selected;
	 * @param procInstIds is {@link Collection} of {@link ProcessInstance}s,
	 * to filter cases by. Skipped if <code>null</code>;
	 * @param handlerCategoryIDs is {@link Collection} of
	 * {@link Group#getPrimaryKey()} where should be searched for
	 * {@link com.idega.user.data.User}s who are in {@link Case#getSubscribers()}
	 * list. Skipped if <code>null</code>;
	 * @return filtered {@link List} of {@link Case#getPrimaryKey()} or
	 * {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	public Map<Integer, Date> getPublicCasesIds(
			Collection<String> caseStatuses,
			Collection<String> caseStatusesToHide,
			Collection<String> caseCodes,
			Collection<? extends Number> caseIDs,
			Collection<? extends Number> procInstIds,
			Collection<? extends Number> handlerCategoryIDs,
			List<Integer> exceptOwnersIds,
	        Timestamp from,
	        Timestamp to,
	        Integer dataFrom,
	        Integer dataTo
	);

	public abstract List<Integer> getCasesIdsByStatusForAdmin(List<String> caseStatuses, List<String> caseStatusesToHide);

	public Map<Integer, Date> getOpenCasesIdsForAdmin(
			List<String> caseCodes,
			List<String> caseStatusesToShow,
			List<String> caseStatusesToHide,
			Integer caseId,
			List<Long> procInstIds,
			Collection<? extends Number> subscriberGroupIDs,
			Timestamp from,
			Timestamp to,
	        Integer dataFrom,
	        Integer dataTo
	);

	public Map<Integer, Date> getClosedCasesIdsForAdmin(
			List<String> caseStatusesToShow,
			List<String> caseStatusesToHide,
			Integer caseId,
			List<Long> procInstIds,
			Collection<? extends Number> subscriberGroupIDs,
			List<Integer> exceptOwnersIds,
	        Timestamp from,
	        Timestamp to,
	        Integer dataFrom,
	        Integer dataTo
	);

	public Long getCountedProcessInstancesByCaseStatusesAndProcessDefinitionNames(List<String> caseStatuses, List<String> procDefNames);
	public Long getCountedProcessInstancesByCaseStatusesAndProcessDefinitionNames(
			List<String> caseStatuses,
			List<String> procDefNames,
			com.idega.user.data.bean.User user,
			boolean onlySubscribed
	);

	public List<Long> getProcessInstancesByCaseStatusesAndProcessDefinitionNames(List<String> caseStatuses, List<String> procDefNames);
	public List<Long> getProcessInstancesByCaseStatusesAndProcessDefinitionNames(List<String> caseStatuses, List<String> procDefNames, Integer firstResult, Integer maxResults, boolean newestOnTop);
	public List<Long> getProcessInstancesByCaseStatusesAndProcessDefinitionNames(
			List<String> caseStatuses,
			List<String> procDefNames,
			com.idega.user.data.bean.User user,
			boolean onlySubscribed,
			Integer firstResult,
			Integer maxResults,
			boolean newestOnTop
	);
	public Map<Long, Integer> getProcessInstancesAndCasesIdsByCaseStatusesAndProcessDefinitionNames(List<String> caseStatuses,List<String> procDefNames);
	public Map<Long, Integer> getProcessInstancesAndCasesIdsByCaseStatusesAndProcessDefinitionNames(List<String> caseStatuses, List<String> procDefNames, Param metadata, int offset, int maxCount, String endDate);
	public Map<Long, Integer> getProcessInstancesAndCasesIdsByCaseStatusesAndProcessInstanceIds(List<String> caseStatuses, List<Long> procInstIds);

	public List<Long> getProcessInstancesByCasesIds(Collection<Integer> casesIds);
	public Map<Long, Integer> getProcessInstancesAndCasesIdsByCasesIds(List<Integer> casesIds);

	public Long getProcessInstanceIdByCaseSubject(String subject);

	public Long getProcessInstanceIdByCaseSubjectAndStatus(String subject, String caseStatus);

	public List<Integer> getCasesIdsByHandlersAndProcessDefinition(List<Integer> handlersIds, String procDefName);

	public List<Long> getProcessInstanceIdsForSubscribedCases(com.idega.user.data.User user);
	public List<Long> getProcessInstanceIdsForSubscribedCases(Integer userId);
	public List<Long> getProcessInstanceIdsForSubscribedCases(Integer userId, List<Long> procInstIds);

	public List<Long> getProcessInstanceIdsByUserAndProcessDefinition(com.idega.user.data.User user, String processDefinitionName);
	public Map<Long, Integer> getProcessInstancesAndCasesIdsByUserAndProcessDefinition(com.idega.user.data.User user, String processDefinitionName);

	public boolean doSubscribeToCasesByProcessDefinition(com.idega.user.data.User user, String processDefinitionName);
	public boolean doSubscribeToCasesByProcessInstanceIds(com.idega.user.data.User user, List<Long> procInstIds);
	public boolean doUnSubscribeFromCasesByProcessDefinition(com.idega.user.data.User user, String processDefinitionName);

	public Map<Long, List<VariableInstanceInfo>> getBPMValuesByCasesIdsAndVariablesNames(List<String> casesIds, List<String> names);

	/**
	 *
	 * <p>Searches cases in database, which is on administration
	 * by given handler.</p>
	 * @param user - handler, which manages cases;
	 * @param caseStatusesToShow
	 * @param caseStatusesToHide
	 * @param caseCodes
	 * @param roles
	 * @param onlySubscribedCases
	 * @param caseId
	 * @param procInstIds
	 * @return
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public Map<Integer, Date> getHandlerCasesIds(
			User user,
			List<String> caseStatusesToShow,
			List<String> caseStatusesToHide,
			List<String> caseCodes,
			Collection<String> roles,
			boolean onlySubscribedCases,
			Integer caseId,
			List<Long> procInstIds,
			List<Integer> exceptOwnersIds,
	        Date from,
	        Date to
	);

	public Long getProcessInstanceIdByCaseIdAndMetaData(String caseId, Param metadata);

	/**
	 *
	 * @param caseStatusesToShow is {@link Collection} of {@link Case#getCaseStatus()}
	 * of {@link Case}s that show be shown, skipped if <code>null</code>;
	 * @param caseStatusesToHide is {@link Collection} of {@link Case#getCaseStatus()}
	 * of {@link Case}s that show be hidden, skipped if <code>null</code>;
	 * @param processDefinitionNames is {@link Collection} of {@link ProcessDefinition}s,
	 * skipped if <code>null</code>;
	 * @param caseIDs is {@link Case#getPrimaryKey()}s of {@link Case}s, that
	 * already are selected;
	 * @param procInstIds is {@link Collection} of {@link ProcessInstance}s,
	 * to filter cases by. Skipped if <code>null</code>;
	 * @param handlerCategoryIDs is {@link Collection} of
	 * {@link Group#getPrimaryKey()} where should be searched for
	 * {@link com.idega.user.data.User}s who are in {@link Case#getSubscribers()}
	 * list. Skipped if <code>null</code>;
	 * @param handlersIDs is {@link com.idega.user.data.User}s, who has ability
	 * to manage {@link Case}s, skipped if <code>null</code>;
	 * @param handlerGroupIDs is {@link Collection} of
	 * {@link Group#getPrimaryKey()} which is connected to {@link Case#getHandler()}.
	 * Skipped if <code>null</code>;
	 * @param caseManagerTypes is {@link Collection} of
	 * {@link Case#getCaseManagerType()}, if <code>null</code> then option
	 * will be skipped;
	 * @param hasCaseManagerType means that {@link Case#getCaseManagerType()}
	 * must be <code>null</code> on <code>true</code>, must be not
	 * <code>null</code> on <code>false</code>. Skipped if <code>null</code> or
	 * overrided by "caseManagerTypes property";
	 * @param caseCodes is {@link Collection} of {@link Case#getCaseCode()}
	 * to filter {@link Case}s that are general ones, skipped if <code>null</code>;
	 * @param roles is {@link Collection} of {@link Actor#getProcessName()},
	 * skipped if <code>null</code>;
	 * @param authorsIDs is {@link User}s, who created the {@link Case}. Usually
	 * written in {@link Case#getOwner()}, skipped if <code>null</code>;
	 * @param casesIds is {@link Collection} of {@link Case#getId()}. It
	 * defines a subset of {@link Case}s, where should be searched, skipped if
	 * <code>null</code>;
	 * @param isAnonymous filters by {@link GeneralCase#isAnonymous()} property,
	 * skipped if <code>null</code>;
	 * @param generalCases tells if only {@link GeneralCase}s should be returned,
	 * skipped if <code>null</code> or <code>false</code>;
	 * @param hasEnded checks is {@link ProcessInstance} connected to
	 * the {@link Case} has {@link ProcessInstance#getEnd()}. If <code>false</code>
	 * is provided, then only not ended processes will be returned. Skipped
	 * if <code>null</code>;
	 * @param dateCreatedFrom is floor of {@link Case#getCreated()},
	 * skipped if <code>null</code>;
	 * @param dateCreatedTo is ceiling of {@link Case#getCreated()},
	 * skipped if <code>null</code>;
	 * @return array of {@link Case#getPrimaryKey()} by criteria or
	 * <code>null</code> on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
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
			List<Integer> exceptOwnersIds,
			Boolean isAnonymous,
			Boolean generalCases,
			Boolean hasEnded,
			Date dateCreatedFrom,
			Date dateCreatedTo
	);

	/**
	 * <p>"AND" relation for filtering BPM {@link Case}s.
	 * {@link Case}s are filtered by provided properties below. If there is
	 * no need to filter by some of these properties, just add <code>null</code>.
	 * If all properties will be <code>null</code>, then all BPM {@link Case}s
	 * will be returned.</p>
	 * @param processDefinitionNames is {@link Collection} of
	 * {@link ProcessDefinition#getName()} to filter {@link Case}s by. It is
	 * skipped, if <code>null</code>;
	 * @param processInstanceIds is {@link Collection} of {@link ProcessInstance#getId()},
	 * skipped if <code>null</code>;
	 * @param caseStatusesToShow is {@link Collection} of {@link Case#getStatus()}
	 * to filter {@link Case}s by. It is skipped, if <code>null</code>;
	 * @param caseStatusesToHide is {@link Collection} of {@link Case#getStatus()}
	 * to filter {@link Case}s by. It is skipped, if <code>null</code>;
	 * @param subscribersIDs is {@link Collection} of {@link User}, who
	 * is subscribed "{@link Case#addSubscriber(User)}". If <code>null</code>
	 * then this option will be skipped;
	 * @param subscriberGroupsIDs is {@link Collection} of
	 * {@link Group#getPrimaryKey()} where should be searched for
	 * {@link com.idega.user.data.User}s who are in {@link Case#getSubscribers()}
	 * list. Skipped if <code>null</code>;
	 * @param handlersIDs is {@link com.idega.user.data.User}s, who has ability
	 * to manage {@link Case}s, skipped if <code>null</code>;
	 * @param handlerGroupIds is {@link Collection} of
	 * {@link Group#getPrimaryKey()} which is connected to {@link Case#getHandler()}.
	 * Skipped if <code>null</code>;
	 * @param caseManagerTypes is {@link Collection} of
	 * {@link Case#getCaseManagerType()}, if <code>null</code> then option
	 * will be skipped;
	 * @param hasCaseManagerType means that {@link Case#getCaseManagerType()}
	 * must be <code>null</code> on <code>true</code>, must be not
	 * <code>null</code> on <code>false</code>. Skipped if <code>null</code> or
	 * overrided by "caseManagerTypes property";
	 * @param caseCodes is {@link Collection} of {@link Case#getCaseCode()}
	 * to filter {@link Case}s that are general ones, skipped if <code>null</code>;
	 * @param roles is {@link Collection} of {@link Actor#getProcessName()},
	 * skipped if <code>null</code>;
	 * @param authorsIDs is {@link User}s, who created the {@link Case}. Usually
	 * written in {@link Case#getOwner()}, skipped if <code>null</code>;
	 * @param casesIds is {@link Collection} of {@link Case#getId()}. It
	 * defines a subset of {@link Case}s, where should be searched, skipped if
	 * <code>null</code>;
	 * @param isAnonymous filters by {@link GeneralCase#isAnonymous()} property,
	 * skipped if <code>null</code>;
	 * @param generalCases tells if only {@link GeneralCase}s should be returned,
	 * skipped if <code>null</code> or <code>false</code>;
	 * @param hasEnded checks is {@link ProcessInstance} connected to
	 * the {@link Case} has {@link ProcessInstance#getEnd()}. If <code>false</code>
	 * is provided, then only not ended processes will be returned. Skipped
	 * if <code>null</code>;
	 * @param dateCreatedFrom is floor of {@link Case#getCreated()},
	 * skipped if <code>null</code>;
	 * @param dateCreatedTo is ceiling of {@link Case#getCreated()},
	 * skipped if <code>null</code>;
	 * @return array of {@link Case#getPrimaryKey()} by criteria or
	 * <code>null</code> on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	public Map<Integer, Date> getCasesPrimaryKeys(
			Collection<String> processDefinitionNames,
			Collection<? extends Number> processInstanceIds,
			Collection<String> caseStatusesToShow,
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
			Boolean hasEnded,
			Date from,
			Date to,
			List<Integer> exceptOwnersIds
	);

	/**
	 *
	 * @param caseStatusesToShow is {@link Collection} of {@link Case#getCaseStatus()}
	 * of {@link Case}s that show be shown, skipped if <code>null</code>;
	 * @param caseStatusesToHide is {@link Collection} of {@link Case#getCaseStatus()}
	 * of {@link Case}s that show be hidden, skipped if <code>null</code>;
	 * @param processDefinitionNames is {@link Collection} of {@link ProcessDefinition}s,
	 * skipped if <code>null</code>;
	 * @param caseIDs is {@link Case#getPrimaryKey()}s of {@link Case}s, that
	 * already are selected;
	 * @param procInstIds is {@link Collection} of {@link ProcessInstance}s,
	 * to filter cases by. Skipped if <code>null</code>;
	 * @param handlerCategoryIDs is {@link Collection} of
	 * {@link Group#getPrimaryKey()} where should be searched for
	 * {@link com.idega.user.data.User}s who are in {@link Case#getSubscribers()}
	 * list. Skipped if <code>null</code>;
	 * @param handler to get {@link Case}s for, not <code>null</code>;
	 * @return filtered {@link List} of {@link Case#getPrimaryKey()} or
	 * {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	public Map<Integer, Date> getHandlerCasesIds(
			User handler,
			Collection<String> caseStatusesToShow,
			Collection<String> caseStatusesToHide,
			Collection<String> processDefinitionNames,
			Collection<? extends Number> caseIDs,
			Collection<? extends Number> procInstIds,
			Set<String> roles,
			Collection<? extends Number> handlerCategoryIDs,
			List<Integer> exceptOwnersIds,
			Date from,
			Date to,
	        Integer dataFrom,
	        Integer dataTo
	);

	public int getNumberOfApplications(Long procDefId);

	/**
	 *
	 * @param processInstanceId is {@link ProcessInstance#getId()},
	 * not <code>null</code>;
	 * @return list of entities by criteria or {@link Collections#emptyList()}
	 * on failure;
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	List<ProcessUserBind> getProcessUserBind(long processInstanceId);

	/**
	 *
	 * @param processInstanceId is {@link ProcessInstance#getId()},
	 * not <code>null</code>;
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	void removeProcessUserBinds(long processInstanceId);

	public List<CaseState> getCaseStatesByProcessDefinitionName(String name);
	public CaseState getCaseStateByProcessDefinitionNameAndStateName(String processName, String stateName);

	void saveCasesStateInstance(CaseStateInstance state);

	void saveCasesState(CaseState state);

	public abstract List<CaseStateInstance> getStateInstancesForProcessByName(long id, List<String> stateList);

	List<CaseStateInstance> getStateInstancesForProcess(long id);

	List<CaseState> getCaseStates();

	Long getProcessIdByCaseId(Integer caseId);

	CaseStateInstance getStateInstanceById(Long id);

	Integer getCaseIdByProcessId(Long procInstId);

	List<Integer> getCaseIdsByProcessDef(String processDefinitionName);

	/**
	 *
	 * @param uuid is process instance id in Camunda BPM, not <code>null</code>
	 * @return entity or <code>null</code> on failure
	 */
	CaseProcInstBind findByUUID(String uuid);

	List<Integer> findCasesIdsByUUIDs(List<String> uuids);

	public CaseTypesProcDefBind getConfiguredCaseTypesProcDefBind(String procDefName);

	public Map<Integer, String> getUUIDsByCasesIds(Collection<Integer> casesIds);

	public List<String> getAllUUIDs();

	public CaseProcInstBind getFirstBindForPrefix(String prefix);
	public CaseProcInstBind getLatestBindForPrefix(String prefix);

}