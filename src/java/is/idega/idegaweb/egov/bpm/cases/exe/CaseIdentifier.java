package is.idega.idegaweb.egov.bpm.cases.exe;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.util.CoreConstants;
import com.idega.util.IWTimestamp;
import com.idega.util.StringUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 * 
 *          Last modified: $Date: 2009/06/30 13:17:35 $ by $Author: valdas $
 */
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service
@Qualifier(CaseIdentifier.QUALIFIER)
public class CaseIdentifier extends DefaultIdentifierGenerator {
	
	static final String QUALIFIER = "defaultCaseIdentifier";
	
	public static final String IDENTIFIER_PREFIX = "P";
	
	private CaseIdentifierBean lastCaseIdentifierNumber;
	
	@Autowired
	private CasesBPMDAO casesBPMDAO;
	
	@Override
	public synchronized Object[] generateNewCaseIdentifier() {
		return generateNewCaseIdentifier(null);
	}
	
	@Override
	protected synchronized Object[] generateNewCaseIdentifier(String usedIdentifier) {
		IWTimestamp currentTime = new IWTimestamp();
		currentTime.setAsDate();
		
		CaseIdentifierBean scopedCI;
		
		if (lastCaseIdentifierNumber == null || !currentTime.equals(lastCaseIdentifierNumber.time)) {

			lastCaseIdentifierNumber = new CaseIdentifierBean();

			CaseProcInstBind b = getCasesBPMDAO().getCaseProcInstBindLatestByDateQN(new Date());

			if (b != null && b.getDateCreated() != null && b.getCaseIdentierID() != null) {

				lastCaseIdentifierNumber.time = new IWTimestamp(b.getDateCreated());
				lastCaseIdentifierNumber.time.setAsDate();
				lastCaseIdentifierNumber.number = b.getCaseIdentierID();
			} else {
				lastCaseIdentifierNumber.time = currentTime;
				lastCaseIdentifierNumber.time.setAsDate();
				lastCaseIdentifierNumber.number = 0;
			}
		}
		
		scopedCI = lastCaseIdentifierNumber;

		//	Will try to use used identifier's number (increased by 1)
		if (!StringUtil.isEmpty(usedIdentifier)) {
			String[] parts = usedIdentifier.split(CoreConstants.MINUS);
			String numberValue = parts[parts.length - 1];
			Integer number = Integer.valueOf(numberValue);
			if (number > scopedCI.number) {
				scopedCI.number = number;
			}
		}
		
		String generated = scopedCI.generate();
		while (!canUseIdentifier(generated)) {
			generated = scopedCI.generate();
		}

		return new Object[] { scopedCI.number, generated };
	}
	
	protected class CaseIdentifierBean {
		
		private IWTimestamp time;
		private Integer number;
		
		String generate() {
			
			String nr = String.valueOf(++number);
			
			while(nr.length() < 4)
				nr = "0"+nr;
			
			return new StringBuffer(IDENTIFIER_PREFIX)
			.append(CoreConstants.MINUS)
			.append(time.getYear())
			.append(CoreConstants.MINUS)
			.append(time.getMonth() < 10 ? "0"+time.getMonth() : time.getMonth())
			.append(CoreConstants.MINUS)
			.append(time.getDay() < 10 ? "0"+time.getDay() : time.getDay())
			.append(CoreConstants.MINUS)
			.append(nr)
			.toString();
		}

		public IWTimestamp getTime() {
			return time;
		}

		public Integer getNumber() {
			return number;
		}
	}
	
	protected CaseIdentifierBean getCaseIdentifierBean() {
		return lastCaseIdentifierNumber;
	}

	@Override
	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	@Override
	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}
}