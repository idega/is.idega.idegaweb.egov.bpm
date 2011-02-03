package is.idega.idegaweb.egov.bpm.cases.search.impl;

import is.idega.idegaweb.egov.bpm.cases.search.CasesListSearchCriteriaBean;
import is.idega.idegaweb.egov.bpm.cases.search.CasesListSearchFilter;
import is.idega.idegaweb.egov.cases.business.CasesBusiness;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.process.data.Case;
import com.idega.block.process.presentation.beans.CasesSearchCriteriaBean;
import com.idega.core.business.DefaultSpringBean;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.bean.BPMProcessVariable;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.identity.RolesManager;
import com.idega.user.business.UserBusiness;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.expression.ELUtil;

public abstract class DefaultCasesListSearchFilter extends DefaultSpringBean implements CasesListSearchFilter {

	public static final String SEARCH_FILTER_CACHE_NAME = "casesSearchFilterCacheName";
	public static final Long SEARCH_FILTER_CACHE_TTL = Long.valueOf(1800);
	
	@Autowired
	private CasesBPMDAO casesBPMDAO;
	@Autowired
	private RolesManager rolesManager;
	@Autowired
	private BPMFactory bpmFactory;
	
	private CasesSearchCriteriaBean criterias;
	
	private long start;
	private long end;
	private boolean measure;
	
	public DefaultCasesListSearchFilter() {
		super();
	}
	
	public DefaultCasesListSearchFilter(CasesSearchCriteriaBean criterias) {
		this();
		
		this.criterias = criterias;
	}
	
	protected abstract String getFilterKey();
	
	protected abstract List<Integer> getSearchResults(List<Integer> casesIds);
	
	protected abstract String getInfo();
	
	protected abstract boolean isFilterKeyDefined();
	
	private Map<String, List<Integer>> getCache() {
		return getCache(SEARCH_FILTER_CACHE_NAME, SEARCH_FILTER_CACHE_TTL);
	}
	
	private String getSearchKey() {
		if (criterias == null) {
			return null;
		}
		
		String filterKey = getFilterKey();
		filterKey = filterKey == null ? CoreConstants.MINUS : filterKey;
		return filterKey.concat(CoreConstants.UNDER).concat(this.toString());
	}
	
	private List<Integer> beforeFiltering() {
		String searchKey = getSearchKey();
		if (searchKey == null) {
			return null;
		}
		
		Map<String, List<Integer>> cache = getCache();
		if (cache == null) {
			return null;
		}
		
		List<Integer> cachedIds = cache.get(searchKey);
		return cachedIds;
	}
	
	protected void afterFiltering(String info, List<Integer> ids) {
		try {
			if (ids == null) {
				return;
			}
			
			String searchKey = getSearchKey();
			if (searchKey == null) {
				return;
			}
			
			Map<String, List<Integer>> cache = getCache();
			if (cache == null) {
				return;
			}
			
			cache.put(searchKey, ids);
		} finally {
			endFiltering(info);
		}
	}
	
	public List<Integer> doFilter(List<Integer> casesIds) {
		List<Integer> cachedIds = beforeFiltering();
		if (!ListUtil.isEmpty(cachedIds)) {
			return getNarrowedResults(casesIds, cachedIds);
		}
		
		if (ListUtil.isEmpty(casesIds)) {
			return casesIds;
		}
		
		if (!isFilterKeyDefined()) {
			return casesIds;
		}
		
		startFiltering();
		List<Integer> filtered = getSearchResults(casesIds);
		if (ListUtil.isEmpty(filtered)) {
			return getNarrowedResults(casesIds, filtered);
		} else {
			afterFiltering(getInfo(), filtered);
		}
		
		filtered = getNarrowedResults(casesIds, filtered);
		return filtered;
	}
	
	private void endFiltering(String info) {
		if (!measure) {
			return;
		}
		
		end = System.currentTimeMillis();
		getLogger().info("Query (" + info + ") executed in: " + (end - start) + " ms");
	}

	private void startFiltering() {
		measure = CoreUtil.isSQLMeasurementOn();
		if (measure) {
			start = System.currentTimeMillis();
		}
	}

	protected CasesBPMDAO getCasesBPMDAO() {
		if (casesBPMDAO == null)
			ELUtil.getInstance().autowire(this);
		
		return casesBPMDAO;
	}
	public void setCasesBPMDAO(CasesBPMDAO casesBPMDAO) {
		this.casesBPMDAO = casesBPMDAO;
	}
	
	protected RolesManager getRolesManager() {
		if (rolesManager == null)
			ELUtil.getInstance().autowire(this);
		
		return rolesManager;
	}
	public void setRolesManager(RolesManager rolesManager) {
		this.rolesManager = rolesManager;
	}
	
	protected BPMFactory getBpmFactory() {
		if (bpmFactory == null)
			ELUtil.getInstance().autowire(this);
		
		return bpmFactory;
	}
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
	
	protected List<Integer> getConvertedFromNumbers(List<? extends Number> values) {
		if (ListUtil.isEmpty(values)) {
			return null;
		}
		
		List<Integer> convertedValues = new ArrayList<Integer>();
		for (Object o: values) {
			if (o instanceof Number) {
				convertedValues.add(((Number) o).intValue());
			} else {
				getLogger().log(Level.WARNING, "Object is not type of Number: " + o);
			}
		}
		
		return convertedValues;
	}
	
	protected List<Integer> getNarrowedResults(List<? extends Number> casesIds, List<? extends Number> filterResults) {
		if (ListUtil.isEmpty(casesIds)) {
			getLogger().info("There are no start data, emptying IDs");
			return null;
		}
		if (ListUtil.isEmpty(filterResults)) {
			getLogger().info("No results found, emptying IDs");
			return null;
		}
		
		List<Integer> tmp = getConvertedFromNumbers(casesIds);
		
		Integer id = null;
		List<Integer> filtered = new ArrayList<Integer>();
		for (Object o: filterResults) {
			if (o instanceof Number) {
				id = ((Number) o).intValue();
				if (tmp.contains(id)) {
					filtered.add(id);
				}
			} else {
				getLogger().log(Level.WARNING, "ID is not type of Number: " + o);
			}
		}
		
		return filtered;
	}
	
	protected List<Integer> getUniqueIds(Collection<Integer> casesIDs) {
		if (ListUtil.isEmpty(casesIDs)) {
			return null;
		}
		
		List<Integer> ids = new ArrayList<Integer>(casesIDs.size());
		for (Object id: casesIDs) {
			Integer realId = null;
			
			if (id instanceof Number) {
				realId = ((Number) id).intValue();
			} else if (id != null) {
				try {
					realId = Integer.valueOf(id.toString());
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			
			if (realId != null && !ids.contains(realId)) {
				ids.add(realId);
			}
		}
		
		return ids;
	}
	
	protected List<Integer> getCasesIds(Collection<Case> cases) {
		if (ListUtil.isEmpty(cases)) {
			return null;
		}
		
		Integer id = null;
		List<Integer> ids = new ArrayList<Integer>(cases.size());
		for (Case theCase: cases) {
			try {
				id = Integer.valueOf(theCase.getId());
			} catch(NumberFormatException e) {
				e.printStackTrace();
			}
			
			if (id != null && !ids.contains(id)) {
				ids.add(id);
			}
		}
		
		return ids;
	}
	
	protected CasesBusiness getCasesBusiness() {
		return getServiceInstance(CasesBusiness.class);
	}
	
	protected UserBusiness getUserBusiness() {
		return getServiceInstance(UserBusiness.class);
	}

	public void setCriterias(CasesSearchCriteriaBean criterias) {
		this.criterias = criterias;
	}
	
	@Override
	public String toString() {
		return this.getClass().getCanonicalName();
	}

	/** Getters from  {@link CasesSearchCriteriaBean} **/
	protected String getCaseNumber() {
		return criterias == null ? null : criterias.getCaseNumber();
	}
	
	protected String getDescription() {
		return criterias == null ? null : criterias.getDescription();
	}
	
	protected String getName() {
		return criterias == null ? null : criterias.getName();
	}
	
	protected String getPersonalId() {
		return criterias == null ? null : criterias.getPersonalId();
	}
	
	protected String[] getStatuses() {
		return criterias == null ? null : criterias.getStatuses();
	}
	
	protected IWTimestamp getDateFrom() {
		return criterias == null ? null : criterias.getDateFrom();
	}
	
	protected IWTimestamp getDateTo() {
		return criterias == null ? null : criterias.getDateTo();
	}
	
	protected String getDateRange() {
		return criterias == null ? null : criterias.getDateRange();
	}
	
	protected String getContact() {
		return criterias == null ? null : criterias.getContact();
	}
	
	protected String getCaseListType() {
		return criterias instanceof CasesListSearchCriteriaBean ? ((CasesListSearchCriteriaBean) criterias).getCaseListType() : null;
	}
	
	protected String getProcessId() {
		return criterias instanceof CasesListSearchCriteriaBean ? ((CasesListSearchCriteriaBean) criterias).getProcessId() : null;
	}
	
	protected List<BPMProcessVariable> getProcessVariables() {
		return criterias instanceof CasesListSearchCriteriaBean ? ((CasesListSearchCriteriaBean) criterias).getProcessVariables() : null;
	}
}