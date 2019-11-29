package is.idega.idegaweb.egov.bpm.cases.exe;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.business.DefaultSpringBean;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.util.CoreConstants;
import com.idega.util.IWTimestamp;

@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service
@Qualifier(ApplicationIdentifier.QUALIFIER)
public class ApplicationIdentifier extends DefaultSpringBean implements IdentifierGenerator {

	public static final String QUALIFIER = "applicationIdentifier";

	@Autowired
	private CasesBPMDAO casesBPMDAO;

	private Map<String, TimeCounter> counters = new HashMap<>();

	private TimeCounter getTimeCounter(int resetInterval, long lastReset, Integer latestCaseIdentifierForCurrentPrefix, String prefix) {
		TimeCounter counter = counters.get(prefix);
		if (counter != null) {
			return counter;
		}
		IWTimestamp now = new IWTimestamp();
		switch (resetInterval) {
		case 365:
			counter = resetInterval > 0 && lastReset > 0 && latestCaseIdentifierForCurrentPrefix != null ?
					new TimeCounter(resetInterval, lastReset, latestCaseIdentifierForCurrentPrefix, getMaxIdentifierValue()) :
					null;
			break;

		default:
			getLogger().warning("Reset interval " + resetInterval + " is not implemented");
			break;
		}
		if (counter == null) {
			CaseProcInstBind b = casesBPMDAO.getCaseProcInstBindLatestByDateQN(now.getDate());
			counter = new TimeCounter(
					resetInterval,
					now.getTime().getTime(),
					b == null ? 0 : b.getCaseIdentierID(),
					getMaxIdentifierValue()
			);
		}

		counters.put(prefix, counter);
		return counter;
	}

	public synchronized Object[] getCaseIdentifierWithPrefix(int resetInterval, long lastReset, Integer latestCaseIdentifierForCurrentPrefix, String prefix) {
		TimeCounter counter = getTimeCounter(resetInterval, lastReset, latestCaseIdentifierForCurrentPrefix, prefix);
		Integer identifierId = counter.getNextCounter();
		String nr = String.valueOf(identifierId);
		String zero = String.valueOf(0);
		while (nr.length() < 5) {
			nr = zero.concat(nr);
		}
		String id = prefix.concat(CoreConstants.MINUS).concat(counter.getDateString()).concat(CoreConstants.MINUS).concat(nr);
		return new Object[] {identifierId, id};
	}

	@Override
	public int getMaxIdentifierValue() {
		return 99999;
	}

}