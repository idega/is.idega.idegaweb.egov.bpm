package is.idega.idegaweb.egov.bpm.cases.exe;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.ejb.FinderException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.data.bean.Metadata;
import com.idega.idegaweb.IWMainApplicationSettings;
import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.user.data.bean.Group;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
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
public class CaseIdentifier extends DefaultIdentifierGenerator implements IdentifierGenerator {

	public static final String QUALIFIER = "defaultCaseIdentifier";

	public static final String IDENTIFIER_PREFIX = "P";

	public static final String METADATA_CASE_IDENTIFIER_PREFIX = "case_identifier_prefix";

	private static final String CASE_IDENTIFIER_LAST_RESET_SUFFIX = "case_identifier_last_reset",
								NR = "nr";

	private CaseIdentifierBean lastCaseIdentifierNumber;

	private Map<String, Object[]> dataForPrefixes = new HashMap<>();

	@Autowired
	private CasesBPMDAO casesBPMDAO;

	@Autowired
	private ApplicationDAO applicationDAO;

	private int getCaseIdentifierResetInterval() {
		return getApplication().getSettings().getInt("case_identifier_reset_interval", 1);
	}

	private long getCounterResetForPrefix(String prefix) {
		String key = prefix.concat(CoreConstants.DOT).concat(CASE_IDENTIFIER_LAST_RESET_SUFFIX);
		IWMainApplicationSettings settings = getApplication().getSettings();
		String lastReset = settings.getProperty(key);
		if (StringHandler.isNumeric(lastReset)) {
			return Long.valueOf(lastReset);
		}
		return -1;
	}

	private void setCounterResetForPrefix(String prefix, long time) {
		String key = prefix.concat(CoreConstants.DOT).concat(CASE_IDENTIFIER_LAST_RESET_SUFFIX);
		getApplication().getSettings().setProperty(key, String.valueOf(time));
	}

	private Object[] getCustomIdentifier(String name) {
		if (StringUtil.isEmpty(name)) {
			return null;
		}

		try {
			String prefix = getPrefixForName(name);
			if (StringUtil.isEmpty(prefix)) {
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
				Object[] data = getData(prefix, new IWTimestamp(), generator.getMaxIdentifierValue());
				Object[] identifierData = data == null || data.length < 3 ? null : generator.getCaseIdentifierWithPrefix((int) data[0], (long) data[1], (Integer) data[2], prefix);
				if (!ArrayUtil.isEmpty(identifierData)) {
					return identifierData;
				}
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Error getting custom identifier for name " + name + " and prefix " + prefix, e);
			}
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting custom identifier for name " + name, e);
		}

		return null;
	}

	@Override
	public String getCaseIdentifierPrefix(Application app) {
		String prefix = getCustomIdentifierPrefix(app);
		return StringUtil.isEmpty(prefix) ? IDENTIFIER_PREFIX : prefix;
	}

	private String getCustomIdentifierPrefix(Application app) {
		if (app == null) {
			return null;
		}

		String servicePrefix = app.getIdentifierPrefix();
		if (!StringUtil.isEmpty(servicePrefix)) {
			return servicePrefix;
		}
		return getAccessPrefix(app);
	}

	private String getAccessPrefix(Application app) {
		List<ApplicationAccess> accessList = null;
		if (getSettings().getBoolean("app_id_from_app_access", true)) {
			accessList = applicationDAO.getApplicationAccessDescendingByLevel((Integer)app.getPrimaryKey());
		}
		if (ListUtil.isEmpty(accessList)) {
			return null;
		}

		for (ApplicationAccess access : accessList) {
			String prefix = getGroupCaseIdentifierPrefix(access.getGroup());
			if (StringUtil.isEmpty(prefix)) {
				continue;
			}
			return prefix;
		}
		return null;
	}

	private String getMetadataCaseIdentifierPrefix(Metadata metadata) {
		if (metadata == null) {
			return null;
		}
		return metadata.getValue();
	}

	private String getGroupCaseIdentifierPrefix(Group group) {
		return getMetadataCaseIdentifierPrefix(group.getMetadata(METADATA_CASE_IDENTIFIER_PREFIX));
	}

	@Override
	public synchronized Object[] getNewCaseIdentifier(String name) {
		if (StringUtil.isEmpty(name)) {
			return getNewCaseIdentifier(null, null);
		}

		try {
			String prefix = getPrefixForName(name);
			if (StringUtil.isEmpty(prefix)) {
				return getNewCaseIdentifier(name, null);
			}

			return getNewCaseIdentifier(null, null, prefix);
		} catch(Exception e) {
			getLogger().log(Level.WARNING, "Error getting custom identifier for name " + name, e);
		}

		return getNewCaseIdentifier(name, null);
	}

	private String getPrefixForName(String name) {
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
			return getCustomIdentifierPrefix(apps.iterator().next());
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting prefix for name " + name, e);
		}
		return null;
	}

	@Override
	protected synchronized Object[] getNewCaseIdentifier(String name, String usedIdentifier) {
		return getNewCaseIdentifier(name, usedIdentifier, null);
	}

	protected synchronized Object[] getNewCaseIdentifier(String name, String usedIdentifier, String customIdentifierPrefix) {
		if (!StringUtil.isEmpty(name) && StringUtil.isEmpty(customIdentifierPrefix)) {
			Object[] identifierData = getCustomIdentifier(name);
			if (!ArrayUtil.isEmpty(identifierData)) {
				return identifierData;
			}
		}

		IWTimestamp currentTime = new IWTimestamp();
		currentTime.setAsDate();

		CaseIdentifierBean scopedCI;
		String prefix = StringUtil.isEmpty(customIdentifierPrefix) ? IDENTIFIER_PREFIX : customIdentifierPrefix;

		int resetInterval = getCaseIdentifierResetInterval();
		if (
				lastCaseIdentifierNumber == null ||
				(lastCaseIdentifierNumber.identifierPrefix == null || !lastCaseIdentifierNumber.identifierPrefix.equals(prefix)) ||
				!currentTime.equals(lastCaseIdentifierNumber.time)
		) {
			lastCaseIdentifierNumber = new CaseIdentifierBean();

			CaseProcInstBind b = null;

			switch (resetInterval) {
			case 365:
				Object[] data = getData(prefix, new IWTimestamp(), getMaxIdentifierValue());
				Integer latestCaseIdentifierForCurrentPrefix = data == null || data.length < 3 ? 0 : (Integer) data[2];
				latestCaseIdentifierForCurrentPrefix = latestCaseIdentifierForCurrentPrefix == getMaxIdentifierValue() ? 0 : latestCaseIdentifierForCurrentPrefix;

				if (lastCaseIdentifierNumber.number == null || lastCaseIdentifierNumber.number < 0) {
					lastCaseIdentifierNumber.time = currentTime;
					lastCaseIdentifierNumber.time.setAsDate();
					lastCaseIdentifierNumber.number = latestCaseIdentifierForCurrentPrefix;
				}

				break;

			default:
				b = getCasesBPMDAO().getCaseProcInstBindLatestByDateQN(new Date());

				if (b != null && b.getDateCreated() != null && b.getCaseIdentierID() != null) {
					lastCaseIdentifierNumber.time = new IWTimestamp(b.getDateCreated());
					lastCaseIdentifierNumber.time.setAsDate();
					lastCaseIdentifierNumber.number = b.getCaseIdentierID();
				} else {
					lastCaseIdentifierNumber.time = currentTime;
					lastCaseIdentifierNumber.time.setAsDate();
					lastCaseIdentifierNumber.number = 0;
				}

				break;
			}
		}
		if (resetInterval != 1) {
			lastCaseIdentifierNumber.time = currentTime;
			lastCaseIdentifierNumber.time.setAsDate();
		}

		scopedCI = lastCaseIdentifierNumber;

		//	Will try to use used identifier's number (increased by 1)
		if (!StringUtil.isEmpty(usedIdentifier)) {
			Integer number = getCaseIdentifierNumber(usedIdentifier);

			if (number > scopedCI.number) {
				scopedCI.number = number;
			}
		}

		if (!StringUtil.isEmpty(customIdentifierPrefix)) {
			scopedCI.identifierPrefix = customIdentifierPrefix;
		} else {
			scopedCI.identifierPrefix = IDENTIFIER_PREFIX;
		}

		String generated = scopedCI.generate();
		while (!canUseIdentifier(generated)) {
			generated = scopedCI.generate();
		}

		getLatestCaseIdentifier(resetInterval, scopedCI.identifierPrefix, IWTimestamp.RightNow());
		return new Object[] {scopedCI.number, generated};
	}

	protected class CaseIdentifierBean {

		private IWTimestamp time;
		private Integer number;
		private String identifierPrefix;

		String generate() {
			if (number + 1 > getMaxIdentifierValue()) {
				number = 0;
			}
			String nr = String.valueOf(++number);

			String zero = String.valueOf(0);
			if (getSettings().getBoolean("case_identifier.add_zeros_for_nr", true)) {
				while (nr.length() < 4) {
					nr = zero.concat(nr);
				}
			}

			String pattern = getPattern(identifierPrefix);
			String prefix = StringUtil.isEmpty(identifierPrefix) ? IDENTIFIER_PREFIX : identifierPrefix;
			String year = String.valueOf(time.getYear());
			String month = String.valueOf(time.getMonth() < 10 ? zero.concat(String.valueOf(time.getMonth())) : time.getMonth());
			String day = String.valueOf(time.getDay() < 10 ? zero.concat(String.valueOf(time.getDay())) : time.getDay());

			if (StringUtil.isEmpty(pattern)) {
				String id = new StringBuffer(prefix)
						.append(CoreConstants.MINUS).append(year)
						.append(CoreConstants.MINUS).append(month)
						.append(CoreConstants.MINUS).append(day)
						.append(CoreConstants.MINUS).append(nr)
						.toString();
				return id;
			}

			String id = StringHandler.replace(pattern, "prefix", prefix);
			id = StringHandler.replace(id, "year", year);
			id = StringHandler.replace(id, "month", month);
			id = StringHandler.replace(id, "day", day);
			id = StringHandler.replace(id, NR, nr);
			return id;
		}

		public IWTimestamp getTime() {
			return time;
		}

		public Integer getNumber() {
			return number;
		}

		public String getIdentifierPrefix() {
			return identifierPrefix;
		}
	}

	private String getPattern(String prefix) {
		return getSettings().getProperty("case_id_pattern." + (StringUtil.isEmpty(prefix) ? CoreConstants.EMPTY : prefix), "prefix-year-month-day-nr");
	}

	public Integer getCaseIdentifierNumber(String caseIdentifier) {
		if (StringUtil.isEmpty(caseIdentifier)) {
			return null;
		}

		String[] parts = caseIdentifier.split(CoreConstants.MINUS);
		String currentPrefix = parts[0];
		String pattern = getPattern(currentPrefix);
		int nrIndex = parts.length - 1;
		if (!StringUtil.isEmpty(pattern)) {
			String[] patternParts = pattern.split(CoreConstants.MINUS);
			for (int i = 0; i < patternParts.length; i++) {
				if (patternParts[i].equals(NR)) {
					nrIndex = i;
				}
			}
		}

		String numberValue = parts[nrIndex];
		Integer number = Integer.valueOf(numberValue);
		return number;
	}

	protected CaseIdentifierBean getCaseIdentifierBean() {
		return lastCaseIdentifierNumber;
	}

	private Object[] getData(String prefix, IWTimestamp now, int maxIdentifierValue) {
		if (StringUtil.isEmpty(prefix)) {
			return null;
		}

		if (dataForPrefixes.containsKey(prefix)) {
			return dataForPrefixes.get(prefix);
		}

		int interval = getCaseIdentifierResetInterval();
		switch (interval) {
		case 365:
			Object[] tempData = getLatestCaseIdentifier(interval, prefix, now);
			Integer latestCaseIdentifierForCurrentPrefix = (Integer) tempData[0];
			Long lastReset = (Long) tempData[1];
			latestCaseIdentifierForCurrentPrefix = latestCaseIdentifierForCurrentPrefix == maxIdentifierValue ? 0 : latestCaseIdentifierForCurrentPrefix;
			Object[] data = new Object[] {interval, lastReset, latestCaseIdentifierForCurrentPrefix};
			dataForPrefixes.put(prefix, data);
			return data;

		default:
			getLogger().warning("Reset interval " + interval + " is not implemented");

			break;
		}

		return null;
	}

	private Object[] getLatestCaseIdentifier(int resetInterval, String prefix, IWTimestamp now) {
		if (resetInterval != 365) {
			return null;
		}

		IWTimestamp yearAgo = new IWTimestamp();
		yearAgo.setYear(yearAgo.getYear() - 1);
		long lastReset = getCounterResetForPrefix(prefix);
		if (lastReset < 0) {
			CaseProcInstBind firstBind = casesBPMDAO.getFirstBindForPrefix(prefix);
			if (firstBind != null && firstBind.getDateCreated() != null) {
				lastReset = firstBind.getDateCreated().getTime();
			}
		}

		Integer latestCaseIdentifierForCurrentPrefix = 0;
		//	If first bind for prefix is not older than 1 year - counter will continue. Otherwise it will be reset
		if (lastReset > yearAgo.getTime().getTime()) {
			//	Continue with counter
			CaseProcInstBind latestBind = casesBPMDAO.getLatestBindForPrefix(prefix);
			if (latestBind != null && latestBind.getCaseIdentierID() != null) {
				latestCaseIdentifierForCurrentPrefix = latestBind.getCaseIdentierID();
			}
		} else {
			now = now == null ? new IWTimestamp() : now;
			lastReset = now.getTime().getTime();
			setCounterResetForPrefix(prefix, lastReset);
		}
		return new Object[] {latestCaseIdentifierForCurrentPrefix, lastReset};
	}

	@Override
	public CasesBPMDAO getCasesBPMDAO() {
		return casesBPMDAO;
	}

	@Override
	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}

	@Override
	public int getMaxIdentifierValue() {
		return 9999;
	}

}