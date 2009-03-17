package is.idega.idegaweb.egov.bpm.cases;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/03/17 17:54:17 $ by $Author: civilis $
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
	
	CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}
	
}