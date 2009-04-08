package is.idega.idegaweb.egov.bpm.cases;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.util.ListUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $ Last modified: $Date: 2009/04/08 09:37:23 $ by $Author: valdas $
 */
@Scope("singleton")
@Service
public class CaseProcessInstanceRelationImpl {
	
	@Autowired
	private CasesBPMDAO casesBPMDAO;
	
	public Long getCaseProcessInstanceId(Integer caseId) {
		
		CaseProcInstBind cpi = getCasesBPMDAO().getCaseProcInstBindByCaseId(
		    caseId);
		Long processInstanceId = cpi.getProcInstId();
		
		return processInstanceId;
	}
	
	public Map<Integer, Long> getCasesProcessInstancesIds(Set<Integer> casesIds) {
		List<CaseProcInstBind> binds = getCasesBPMDAO().getCasesProcInstBindsByCasesIds(new ArrayList<Integer>(casesIds));
		if (ListUtil.isEmpty(binds)) {
			return null;
		}
		
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
	
	CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}
	
}