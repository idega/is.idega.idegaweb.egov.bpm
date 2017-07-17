package is.idega.idegaweb.egov.bpm.cases.search.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AddressFilter extends DefaultCasesListSearchFilter {
	public static final String VARIABLE_NAME_STREET = "string_street";
	public static final String VARIABLE_NAME_HOUSE_NUMBER = "string_houseNumber";
	public static final String VARIABLE_NAME_APARTMENT_NUMBER = "string_apartmentNumber";

	@Autowired
	private VariableInstanceQuerier querier;

	@Autowired
	private CasesBPMDAO casesBPMDAO;

	@Override
	public List<Integer> getSearchResults(List<Integer> casesIds) {
		String addressToSearch = getAddress();
		String addressToSearchWithoutSymbols = addressToSearch.trim().replaceAll("[.,-]", "").replaceAll(" ", "");
		List<Integer> casesByAddressIds = new ArrayList<Integer>();
		List<CaseProcInstBind> bindList = null;

		//Searching for the cases
		try {
			bindList = getCasesDAO().getCasesProcInstBindsByCasesIds(casesIds);
			if (bindList != null && bindList.size() > 0) {
				for (CaseProcInstBind bind : bindList) {
					if (bind.getProcInstId() != null) {
						Collection<VariableInstanceInfo> streetCol = getVariableInstanceQuerier().getVariableByProcessInstanceIdAndVariableName(bind.getProcInstId(), VARIABLE_NAME_STREET);
						Collection<VariableInstanceInfo> houseNumberCol = getVariableInstanceQuerier().getVariableByProcessInstanceIdAndVariableName(bind.getProcInstId(), VARIABLE_NAME_HOUSE_NUMBER);
						Collection<VariableInstanceInfo> appartmentNumberCol = getVariableInstanceQuerier().getVariableByProcessInstanceIdAndVariableName(bind.getProcInstId(), VARIABLE_NAME_APARTMENT_NUMBER);

						String street = "";
						String houseNumber = "";
						String appartmentNumber = "";

						if (streetCol != null && !streetCol.isEmpty() && streetCol.iterator().hasNext()) {
							VariableInstanceInfo variableInstanceInfoStreet = streetCol.iterator().next();
							if (variableInstanceInfoStreet != null && variableInstanceInfoStreet.getName().equalsIgnoreCase(VARIABLE_NAME_STREET)) {
								street = (String) variableInstanceInfoStreet.getValue();
							}
						}
						if (houseNumberCol != null && !houseNumberCol.isEmpty() && houseNumberCol.iterator().hasNext()) {
							VariableInstanceInfo variableInstanceInfoHouseNumber = houseNumberCol.iterator().next();
							if (variableInstanceInfoHouseNumber != null && variableInstanceInfoHouseNumber.getName().equalsIgnoreCase(VARIABLE_NAME_HOUSE_NUMBER)) {
								houseNumber = (String) variableInstanceInfoHouseNumber.getValue();
							}
						}
						if (appartmentNumberCol != null && !appartmentNumberCol.isEmpty() && appartmentNumberCol.iterator().hasNext()) {
							VariableInstanceInfo variableInstanceInfoAppartmentNumber = appartmentNumberCol.iterator().next();
							if (variableInstanceInfoAppartmentNumber != null && variableInstanceInfoAppartmentNumber.getName().equalsIgnoreCase(VARIABLE_NAME_APARTMENT_NUMBER)) {
								appartmentNumber = (String) variableInstanceInfoAppartmentNumber.getValue();
							}
						}

						//Checking if address is the same
						String caseAddress = StringUtils.isNotBlank(street) ? street : "";
						caseAddress += StringUtils.isNotBlank(houseNumber) ? houseNumber : "";
						caseAddress += StringUtils.isNotBlank(appartmentNumber) ? appartmentNumber : "";
						if ( addressToSearchWithoutSymbols.equalsIgnoreCase(caseAddress) || (!StringUtils.contains(addressToSearch, " ") && addressToSearch.equalsIgnoreCase(street)) ) {
							casesByAddressIds.add(bind.getCaseId());
						}

						if (streetCol != null) streetCol.clear();
						if (houseNumberCol != null) houseNumberCol.clear();
						if (appartmentNumberCol != null) appartmentNumberCol.clear();
					}
				}
			}
		} catch(Exception e) {
			getLogger().log(Level.WARNING, "Exception while resolving case ids by address = " + addressToSearch, e);
		}

		if (ListUtil.isEmpty(casesByAddressIds)) {
			getLogger().log(Level.INFO, "No cases found by address: " + addressToSearch);
		} else {
			getLogger().log(Level.INFO, "Cases by address (" + addressToSearch + "): " + casesByAddressIds);
		}

		return casesByAddressIds;
	}


	private VariableInstanceQuerier getVariableInstanceQuerier() {
		if (querier == null) {
			ELUtil.getInstance().autowire(this);
		}
		return querier;
	}

	protected CasesBPMDAO getCasesDAO() {
		if (this.casesBPMDAO == null) {
			ELUtil.getInstance().autowire(this);
		}
		return casesBPMDAO;
	}


	@Override
	protected String getInfo() {
		return "Looking for cases by address: " + getAddress();
	}

	@Override
	protected String getFilterKey() {
		return getAddress();
	}

	@Override
	protected boolean isFilterKeyDefined() {
		String address = getAddress();
		if (StringUtil.isEmpty(address)) {
			getLogger().log(Level.INFO, "Address is undefined, not filtering by it!");
			return false;
		}
		return true;
	}
}