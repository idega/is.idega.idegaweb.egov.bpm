package is.idega.idegaweb.egov.bpm.cases;

import is.idega.idegaweb.egov.cases.data.GeneralCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.data.Case;
import com.idega.jbpm.data.CaseProcInstBind;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.util.ListUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $ Last modified: $Date: 2009/04/08 09:37:23 $ by $Author: valdas $
 */
@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class CaseProcessInstanceRelationImpl {

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
		List<CaseProcInstBind> binds = getCasesBPMDAO().getCasesProcInstBindsByCasesIds(new ArrayList<Integer>(casesIds));
		if (ListUtil.isEmpty(binds))
			return Collections.emptyMap();
		
		Map<Integer, Long> casesIdsMapping = new HashMap<Integer, Long>();
		for (CaseProcInstBind bind: binds) {
			Integer caseId = bind.getCaseId();
			Long processInstanceId = bind.getProcInstId();
			if (casesIdsMapping.get(caseId) == null) {
				casesIdsMapping.put(caseId, processInstanceId);
			}
		}
		
		return casesIdsMapping;
	}
	
	/**
	 * 
	 * @param cases to get {@link ProcessInstance}s for, not <code>null</code>;
	 * @return values from {@link CaseProcInstBind} table or 
	 * {@link Collections#emptyMap()} on failure;
	 */
	public Map<ProcessInstanceW, Case> getCasesAndProcessInstances(Collection<? extends Case> cases) {
		if (ListUtil.isEmpty(cases)) {
			return Collections.emptyMap();
		}

		/* Collecting ids of cases */
		Map<Integer, Case> caseIDs = new HashMap<Integer, Case>(cases.size());
		for (Case theCase: cases) {
			caseIDs.put(Integer.valueOf(theCase.getPrimaryKey().toString()), theCase);
		}

		/* Collecting relations between cases and process instances */
		Map<Integer, Long> ids = getCasesProcessInstancesIds(caseIDs.keySet());
		if (MapUtil.isEmpty(ids)) {
			return Collections.emptyMap();
		}

		/* Collecting process instances */
		List<ProcessInstanceW> processInstances = getBPMDAO().getProcessInstancesByIDs(ids.values());
		Map<Long, ProcessInstanceW> processInstancesMap = new HashMap<Long, ProcessInstanceW>(processInstances.size());
		for (ProcessInstanceW pi : processInstances) {
			processInstancesMap.put(pi.getId(), pi);
		}

		/* Creating map of relations */
		Map<ProcessInstanceW, Case> casesProcessInstances = new HashMap<ProcessInstanceW, Case>(ids.size());
		for (Integer caseId : ids.keySet()) {
			Long processInstanceId = ids.get(caseId);
			if (processInstanceId == null) {
				continue;
			}

			casesProcessInstances.put(
					processInstancesMap.get(processInstanceId),
					caseIDs.get(caseId));
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
	public GeneralCase getCase(
			Map<ProcessInstanceW, Case> relationMap, 
			Long processInstanceId) {
		if (MapUtil.isEmpty(relationMap) || processInstanceId == null) {
			return null;
		}

		ProcessInstanceW processInstance = getProcessInstance(relationMap, processInstanceId);
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
	 */
	public ProcessInstanceW getProcessInstance(
			Map<ProcessInstanceW, Case> relationMap, 
			Long processInstanceId) {
		if (MapUtil.isEmpty(relationMap) || processInstanceId == null) {
			return null;
		}

		for (ProcessInstanceW pi : relationMap.keySet()) {
			if (processInstanceId.longValue() == pi.getId()) {
				return pi;
			}
		}

		return null;
	}
}