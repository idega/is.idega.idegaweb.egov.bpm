package is.idega.idegaweb.egov.bpm.cases.board;

import is.idega.idegaweb.egov.cases.business.BoardCasesManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.builder.bean.AdvancedProperty;

@Scope("singleton")
@Service("boardCasesManagerBean")
public class BoardCasesManagerFacade {
	
	@Autowired
	private BoardCasesManager boardCasesManager;
	
	public AdvancedProperty setCaseVariableValue(Integer caseId,
	        String variableName, String value, String role) {
		
		return getBoardCasesManager().setCaseVariableValue(caseId,
		    variableName, value, role);
	}
	
	BoardCasesManager getBoardCasesManager() {
		return boardCasesManager;
	}
}