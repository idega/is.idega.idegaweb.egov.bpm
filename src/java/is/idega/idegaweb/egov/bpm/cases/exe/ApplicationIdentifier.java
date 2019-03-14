package is.idega.idegaweb.egov.bpm.cases.exe;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.util.CoreConstants;


@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service
@Qualifier(ApplicationIdentifier.QUALIFIER)
public class ApplicationIdentifier{
	
	public static final String QUALIFIER = "applicationIdentifier";
	
	@Autowired
	private CasesBPMDAO casesBPMDAO;
	
	private Map<String, TimeCounter> counters = new HashMap<>();

	private TimeCounter getTimeCounter(String prefix) {
		TimeCounter counter = counters.get(prefix);
		if(counter != null) {
			return counter;
		}
		Date now = new Date();
		CaseProcInstBind b = casesBPMDAO
				.getCaseProcInstBindLatestByDateQN(
						now
				);
		counter = new TimeCounter(now.getTime(),b.getCaseIdentierID());
		counters.put(prefix, counter);
		return counter;
	}
	public synchronized Object[] generatePrefixedCaseIdentifier(String prefix) {
		TimeCounter counter = getTimeCounter(prefix);
		Integer IdentifierId = counter.getNextCounter();
		String nr = String.valueOf(IdentifierId);
		while (nr.length() < 4)
			nr = "0" + nr;
		String id = prefix
				+ CoreConstants.MINUS
				+ counter.getDateString()
				+ CoreConstants.MINUS 
				+ nr;
		
		return new Object[] {IdentifierId, id};
	}
}