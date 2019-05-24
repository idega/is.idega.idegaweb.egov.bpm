package is.idega.idegaweb.egov.bpm.cases.exe;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import javax.ejb.FinderException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.data.bean.Metadata;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.user.data.bean.Group;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

import is.idega.idegaweb.egov.application.business.ApplicationBusiness;
import is.idega.idegaweb.egov.application.data.Application;
import is.idega.idegaweb.egov.application.data.bean.ApplicationAccess;
import is.idega.idegaweb.egov.application.data.dao.ApplicationDAO;

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

	public static final String QUALIFIER = "defaultCaseIdentifier";

	public static final String IDENTIFIER_PREFIX = "P";
	
	public static final String METADATA_CASE_IDENTIFIER_PREFIX = "case_identifier_prefix";

	private CaseIdentifierBean lastCaseIdentifierNumber;

	@Autowired
	private CasesBPMDAO casesBPMDAO;
	
	@Autowired
	private ApplicationDAO applicationDAO;

	private Object[] getCustomIdentifier(String name) {
		if (StringUtil.isEmpty(name)) {
			return null;
		}

		try {
			ApplicationBusiness appBusiness = getServiceInstance(ApplicationBusiness.class);
			Collection<Application> apps = null;
			try {
				apps = appBusiness.getApplicationHome().findAllByApplicationUrl(name);
			} catch (FinderException e) {
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Error getting app by URL " + name, e);
			}
			if (ListUtil.isEmpty(apps)) {
				return null;
			}
			String prefix = getCustomIdentifierPrefix(apps);
			if(StringUtil.isEmpty(prefix)) {
				return null;
			}

			ApplicationIdentifier generator = null;
			try {
				generator = ELUtil.getInstance().getBean(ApplicationIdentifier.QUALIFIER);
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Error getting bean " + ApplicationIdentifier.QUALIFIER, e);
			}
			if (generator == null) {
				return null;
			}
			try {
				Object[] identifierData = generator.generatePrefixedCaseIdentifier(prefix);
				if (!ArrayUtil.isEmpty(identifierData)) {
					return identifierData;
				}
			} catch (Exception e) {
				getLogger().log(
						Level.WARNING, 
						"Error getting custom identifier for name " 
									+ name 
									+ " and prefix " 
									+ prefix, 
						e
				);
			}
		} catch (Exception e) {
			getLogger().log(
					Level.WARNING, 
					"Error getting custom identifier for name " 
								+ name, 
					e
			);
		}

		return null;
	}
	
	private String getCustomIdentifierPrefix(
			Collection<Application> apps
	) {
		for (Application app: apps) {
			String prefix = app.getIdentifierPrefix();
			if(!StringUtil.isEmpty(prefix)) {
				return prefix;
			}
			List<ApplicationAccess> accessList = applicationDAO.getApplicationAccessDescendingByLevel(
					(Integer)app.getPrimaryKey()
			);
			if(ListUtil.isEmpty(accessList)) {
				continue;
			}
			for(ApplicationAccess access : accessList) {
				prefix = getGroupCaseIdentifierPrefix(
						access.getGroup()
				);
				if(!StringUtil.isEmpty(prefix)) {
					return prefix;
				}
			}
		}
		return null;
	}
	
	private String getMetadataCaseIdentifierPrefix(Metadata metadata) {
		if(metadata == null) {
			return null;
		}
		return metadata.getValue();
	}
	private String getGroupCaseIdentifierPrefix(Group group) {
		return getMetadataCaseIdentifierPrefix(
				group.getMetadata(METADATA_CASE_IDENTIFIER_PREFIX)
		);
	}

	@Override
	public synchronized Object[] getNewCaseIdentifier(String name) {
		if (!StringUtil.isEmpty(name)) {
			Object[] identifierData = getCustomIdentifier(name);
			if (!ArrayUtil.isEmpty(identifierData)) {
				return identifierData;
			}
		}

		return getNewCaseIdentifier(null, null);
	}

	@Override
	protected synchronized Object[] getNewCaseIdentifier(String name, String usedIdentifier) {
		if (!StringUtil.isEmpty(name)) {
			Object[] identifierData = getCustomIdentifier(name);
			if (!ArrayUtil.isEmpty(identifierData)) {
				return identifierData;
			}
		}

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
			Integer number = getCaseIdentifierNumber(usedIdentifier);

			if (number > scopedCI.number) {
				scopedCI.number = number;
			}
		}

		String generated = scopedCI.generate();
		while (!canUseIdentifier(generated)) {
			generated = scopedCI.generate();
		}

		return new Object[] {scopedCI.number, generated};
	}

	protected class CaseIdentifierBean {

		private IWTimestamp time;
		private Integer number;

		String generate() {
			String nr = String.valueOf(++number);

			while (nr.length() < 4) {
				nr = "0" + nr;
			}

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

	public Integer getCaseIdentifierNumber(String caseIdentifier) {
		if (StringUtil.isEmpty(caseIdentifier)) {
			return null;
		}

		String[] parts = caseIdentifier.split(CoreConstants.MINUS);
		String numberValue = parts[parts.length - 1];
		Integer number = Integer.valueOf(numberValue);
		return number;
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