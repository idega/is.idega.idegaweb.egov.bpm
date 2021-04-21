package is.idega.idegaweb.egov.bpm.cases.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.bpm.model.VariableInstance;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.builder.business.AdvancedPropertyComparator;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.identity.BPMUserFactory;
import com.idega.jbpm.variables.MultipleSelectionVariablesResolver;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.actionhandlers.CaseHandlerAssignmentHandler;

@Scope("request")
@Service(MultipleSelectionVariablesResolver.BEAN_NAME_PREFIX + CaseHandlerAssignmentHandler.handlerUserIdVarName)
public class BPMCasesHandlersResolver extends MultipleSelectionVariablesResolver {

	@Autowired
	private BPMUserFactory userFactory;

	BPMUserFactory getUserFactory() {
		if (userFactory == null)
			ELUtil.getInstance().autowire(this);
		return userFactory;
	}

	@Override
	public Collection<AdvancedProperty> getValues(String procDefId, String variableName) {
		if (StringUtil.isEmpty(procDefId) || StringUtil.isEmpty(variableName)) {
			addEmptyLabel(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
			return values;
		}

		String procDefName = getBpmDAO().getProcessDefinitionNameByProcessDefinitionId(Long.valueOf(procDefId));
		if (StringUtil.isEmpty(procDefName)) {
			addEmptyLabel(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
			return values;
		}

		List<Integer> ids = getUserFactory().getAllHandlersForProcess(procDefName);
		if (ListUtil.isEmpty(ids)) {
			addEmptyLabel(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
			return values;
		}

		values = new ArrayList<>();
		for (Integer id: ids) {
			values.add(new AdvancedProperty(String.valueOf(id), getUserName(id)));
		}

		List<AdvancedProperty> sorted = new ArrayList<>(values);
		Collections.sort(sorted, new AdvancedPropertyComparator(getCurrentLocale()));
		values = new ArrayList<>(sorted);

		return values;
	}

	protected String getUserName(Integer id) {
		try {
			UserBusiness userBusiness = getServiceInstance(UserBusiness.class);
			User user = userBusiness.getUser(Integer.valueOf(id));
			return user == null ? CoreConstants.MINUS : user.getName();
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting user by ID: " + id, e);
		}
		return CoreConstants.MINUS;
	}

	@Override
	public String getIdKey() {
		return null;
	}

	@Override
	public String getValueKey() {
		return null;
	}

	@Override
	protected String getNoValuesLocalizationKey() {
		return "no_handler_found";
	}

	@Override
	protected String getNoValuesDefaultString() {
		return "No handler found";
	}

	@Override
	public String getPresentation(VariableInstance variable) {
		return (variable == null || variable.getVariableValue() == null) ? CoreConstants.EMPTY : getPresentation((String) variable.getVariableValue());
	}

	@Override
	public String getPresentation(String value) {
		if (StringUtil.isEmpty(value)) {
			return null;
		}

		String[] ids = value.split(CoreConstants.SEMICOLON);
		return ArrayUtil.isEmpty(ids) ? CoreConstants.MINUS : getUsers(Arrays.asList(ids));
	}

	@Override
	public String getPresentation(VariableInstanceInfo variable) {
		if (variable == null) {
			return CoreConstants.MINUS;
		}

		Object value = variable.getValue();
		if (value == null) {
			return CoreConstants.MINUS;
		}

		boolean numeric = true;
		Collection<String> usersIds = new ArrayList<>();
		if (value instanceof Number) {
			usersIds.add(((Number) value).toString());
		} else if (value instanceof Collection) {
			Collection<?> values = (Collection<?>) value;
			for (Object object: values) {
				if (object == null) {
					continue;
				}

				String id = object.toString();
				if (StringUtil.isEmpty(id)) {
					continue;
				}

				usersIds.add(id);
				if (!StringHandler.isNumeric(id)) {
					numeric = false;
				}
			}
		}

		return numeric ? getUsers(usersIds) : getPresentations(usersIds);
	}

	private String getPresentations(Collection<String> values) {
		String presentations = CoreConstants.EMPTY;
		for (Iterator<String> valuesIter = values.iterator(); valuesIter.hasNext();) {
			String value = valuesIter.next();
			if (!StringUtil.isEmpty(value)) {
				presentations = presentations.concat(value);
				if (valuesIter.hasNext()) {
					presentations = presentations.concat(CoreConstants.COMMA).concat(CoreConstants.SPACE);
				}
			}
		}
		return presentations;
	}

	private String getUsers(Collection<String> usersIds) {
		if (ListUtil.isEmpty(usersIds)) {
			return CoreConstants.MINUS;
		}

		try {
			List<String> ids = new ArrayList<>();
			boolean numeric = true;
			for (String id: usersIds) {
				if (StringUtil.isEmpty(id)) {
					continue;
				}

				ids.add(id);
				if (!StringHandler.isNumeric(id)) {
					numeric = false;
				}
			}

			Collection<?> results = null;
			if (numeric) {
				try {
					UserBusiness userBusiness = getServiceInstance(UserBusiness.class);
					results = userBusiness.getUsers(ArrayUtil.convertListToArray(ids));
				} catch (Exception e) {
					getLogger().log(Level.WARNING, "Error getting users by IDs: " + usersIds, e);
				}
			} else {
				results = ids;
			}

			if (ListUtil.isEmpty(results)) {
				return CoreConstants.MINUS;
			}

			StringBuilder presentation = new StringBuilder();
			for (Iterator<?> resultsIter = results.iterator(); resultsIter.hasNext();) {
				Object result = resultsIter.next();
				String name = null;
				if (result instanceof User) {
					name = ((User) result).getName();
				} else if (result != null) {
					name = result.toString();
				}
				if (StringUtil.isEmpty(name)) {
					continue;
				}

				presentation.append(name);
				if (resultsIter.hasNext()) {
					presentation.append(CoreConstants.COMMA).append(CoreConstants.SPACE);
				}
			}
			String result = presentation.toString();
			return StringUtil.isEmpty(result) ? CoreConstants.MINUS : result;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return CoreConstants.MINUS;
	}

}