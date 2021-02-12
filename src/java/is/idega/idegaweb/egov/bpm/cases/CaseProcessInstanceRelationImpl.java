package is.idega.idegaweb.egov.bpm.cases;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.data.Case;
import com.idega.core.business.DefaultSpringBean;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.expression.ELUtil;

import is.idega.idegaweb.egov.cases.data.GeneralCase;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $ Last modified: $Date: 2009/04/08 09:37:23 $ by $Author: valdas $
 */
@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class CaseProcessInstanceRelationImpl extends DefaultSpringBean {

	@Autowired
	private BPMDAO bpmDAO;

	protected BPMDAO getBPMDAO() {
		if (this.bpmDAO == null) {
			ELUtil.getInstance().autowire(this);
		}

		return this.bpmDAO;
	}

	@Autowired
	private CasesBPMDAO casesBPMDAO;

	protected CasesBPMDAO getCasesBPMDAO() {
		if (this.casesBPMDAO == null) {
			ELUtil.getInstance().autowire(this);
		}

		return casesBPMDAO;
	}

	public Long getCaseProcessInstanceId(Integer caseId) {
		CaseProcInstBind cpi = getCasesBPMDAO().getCaseProcInstBindByCaseId(caseId);
		Long processInstanceId = cpi.getProcInstId();

		return processInstanceId;
	}

	public Map<Integer, Long> getCasesProcessInstancesIds(Set<Integer> casesIds) {
		return getCasesProcessInstancesIds(casesIds, Long.class);
	}

	private <T extends Serializable> Map<Integer, T> getCasesProcessInstancesIds(Set<Integer> casesIds, Class<T> type) {
		List<CaseProcInstBind> binds = getCasesBPMDAO().getCasesProcInstBindsByCasesIds(new ArrayList<>(casesIds));
		if (ListUtil.isEmpty(binds)) {
			return Collections.emptyMap();
		}

		boolean id = type == null || type.getName().equals(Long.class.getName());
		Map<Integer, T> casesIdsMapping = new HashMap<>();
		for (CaseProcInstBind bind: binds) {
			Integer caseId = bind.getCaseId();
			if (id) {
				Long processInstanceId = bind.getProcInstId();
				if (casesIdsMapping.get(caseId) == null) {
					@SuppressWarnings("unchecked")
					T result = (T) processInstanceId;
					casesIdsMapping.put(caseId, result);
				}
			} else {
				String uuid = bind.getUuid();
				if (!StringUtil.isEmpty(uuid) && casesIdsMapping.get(caseId) == null) {
					@SuppressWarnings("unchecked")
					T result = (T) uuid;
					casesIdsMapping.put(caseId, result);
				}
			}
		}

		return casesIdsMapping;
	}

	/**
	 *
	 * @param cases to get {@link ProcessInstance}s for, not <code>null</code>;
	 * @return values from {@link CaseProcInstBind} table or
	 * {@link Collections#emptyMap()} on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	public Map<ProcessInstance, Case> getCasesAndProcessInstances(Collection<? extends Case> cases) {
		if (ListUtil.isEmpty(cases)) {
			return Collections.emptyMap();
		}

		/* Collecting ids of cases */
		Map<Integer, Case> caseIDs = new HashMap<>(cases.size());
		for (Case theCase: cases) {
			caseIDs.put(Integer.valueOf(theCase.getPrimaryKey().toString()), theCase);
		}

		/* Collecting relations between cases and process instances */
		Map<Integer, Long> ids = getCasesProcessInstancesIds(caseIDs.keySet());
		if (MapUtil.isEmpty(ids)) {
			return Collections.emptyMap();
		}

		/* Collecting process instances */
		List<ProcessInstance> processInstances = getBPMDAO().getProcessInstancesByIDs(ids.values());
		Map<Long, ProcessInstance> processInstancesMap = new HashMap<>(processInstances.size());
		for (ProcessInstance pi : processInstances) {
			processInstancesMap.put(pi.getId(), pi);
		}

		/* Creating map of relations */
		Map<ProcessInstance, Case> casesProcessInstances = new HashMap<>(ids.size());
		for (Integer caseId : ids.keySet()) {
			Long processInstanceId = ids.get(caseId);
			if (processInstanceId == null) {
				continue;
			}

			casesProcessInstances.put(
					processInstancesMap.get(processInstanceId),
					caseIDs.get(caseId)
			);
		}

		return casesProcessInstances;
	}

	public <K extends Serializable> Map<K, Case> getCasesAndProcessInstancesIds(Collection<? extends Case> cases, Class<K> keyType) {
		if (ListUtil.isEmpty(cases) || keyType == null) {
			return Collections.emptyMap();
		}

		if (keyType.getName().equals(ProcessInstance.class.getName())) {
			@SuppressWarnings("unchecked")
			Map<K, Case> results = (Map<K, Case>) getCasesAndProcessInstances(cases);
			return results;
		}

		/* Collecting ids of cases */
		Map<Integer, Case> caseIDs = new HashMap<>(cases.size());
		for (Case theCase: cases) {
			caseIDs.put(Integer.valueOf(theCase.getPrimaryKey().toString()), theCase);
		}

		/* Collecting relations between cases and process instances */
		Map<Integer, K> ids = getCasesProcessInstancesIds(caseIDs.keySet(), keyType);
		if (MapUtil.isEmpty(ids)) {
			return Collections.emptyMap();
		}

		/* Creating map of relations */
		Map<K, Case> casesProcessInstances = new HashMap<>(ids.size());
		for (Integer caseId : ids.keySet()) {
			K id = ids.get(caseId);
			if (id == null) {
				continue;
			}

			casesProcessInstances.put(
					id,
					caseIDs.get(caseId)
			);
		}

		return casesProcessInstances;
	}

	/**
	 *
	 * <p>Helping method, to get {@link GeneralCase} from created {@link Map}.
	 * No querying is done.</p>
	 * @param relationMap is {@link Map} from
	 * {@link CaseProcessInstanceRelationImpl#getCasesAndProcessInstances(Collection)},
	 * not <code>null</code>;
	 * @param processInstanceId is {@link ProcessInstance#getId()} to search by,
	 * not <code>null</code>;
	 * @return {@link Case} corresponding {@link ProcessInstance} or
	 * <code>null</code> on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	public <K extends Serializable> GeneralCase getCase(
			Map<K, Case> relationMap,
			K processInstanceId
	) {
		if (MapUtil.isEmpty(relationMap) || processInstanceId == null) {
			return null;
		}

		K processInstance = getProcessInstance(relationMap, processInstanceId);
		if (processInstance == null) {
			return null;
		}

		Case theCase = relationMap.get(processInstance);
		if (theCase instanceof GeneralCase) {
			return (GeneralCase) theCase;
		}

		return null;
	}

	/**
	 *
	 * <p>Helping method, to get {@link ProcessInstance} from created {@link Map}.
	 * No querying is done.</p>
	 * @param relationMap is {@link Map} from
	 * {@link CaseProcessInstanceRelationImpl#getCasesAndProcessInstances(Collection)},
	 * not <code>null</code>;
	 * @param processInstanceId is {@link ProcessInstance#getId()} to search by,
	 * not <code>null</code>;
	 * @return {@link ProcessInstance} exiting on {@link Map} or <code>null</code>
	 * on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	public <K extends Serializable> K getProcessInstance(
			Map<K, Case> relationMap,
			K processInstanceId
	) {
		if (MapUtil.isEmpty(relationMap) || processInstanceId == null) {
			return null;
		}

		long piId = -1;
		if (processInstanceId instanceof Number) {
			piId = ((Number) processInstanceId).longValue();
		}
		for (Serializable key: relationMap.keySet()) {
			if (key instanceof ProcessInstance) {
				ProcessInstance pi = (ProcessInstance) key;
				if (piId == pi.getId()) {
					@SuppressWarnings("unchecked")
					K result = (K) pi;
					return result;
				}

			} else if (key instanceof Number) {
				if (piId == ((Number) key).longValue()) {
					@SuppressWarnings("unchecked")
					K result = (K) key;
					return result;
				}

			} else if (key instanceof String) {
				if (processInstanceId.toString().equals(key.toString())) {
					@SuppressWarnings("unchecked")
					K result = (K) key;
					return result;
				}
			} else {
				getLogger().warning("Do not know how to handle key type " + key.getClass().getName() + ". Relations: " + relationMap);
			}
		}

		return null;
	}

}