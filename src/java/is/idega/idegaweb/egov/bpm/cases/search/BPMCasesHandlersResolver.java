package is.idega.idegaweb.egov.bpm.cases.search;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.cases.actionhandlers.CaseHandlerAssignmentHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.builder.bean.AdvancedProperty;
import com.idega.jbpm.bean.BPMProcessVariable;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.variables.MultipleSelectionVariablesResolver;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

@Scope("request")
@Service(MultipleSelectionVariablesResolver.BEAN_NAME_PREFIX + CaseHandlerAssignmentHandler.handlerUserIdVarName)
public class BPMCasesHandlersResolver extends MultipleSelectionVariablesResolver {

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
		
		List<Long> procInstIds = getBpmDAO().getProcessInstanceIdsByProcessDefinitionNames(Arrays.asList(procDefName));
		if (ListUtil.isEmpty(procInstIds)) {
			addEmptyLabel(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
			return values;
		}
		
		Collection<VariableInstanceInfo> vars = getVariablesQuerier().getVariablesByProcessInstanceIdAndVariablesNames(Arrays.asList(variableName), procInstIds, false, false);
		if (ListUtil.isEmpty(vars)) {
			addEmptyLabel(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
			return values;
		}
		
		values = new ArrayList<AdvancedProperty>();
		List<String> ids = new ArrayList<String>();
		for (VariableInstanceInfo var: vars) {
			String id = var.getValue().toString();
			if (ids.contains(id))
				continue;	//	No need to duplicate users
			
			ids.add(id);
			values.add(new AdvancedProperty(id, getUserName(id)));
		}
		
		return values;
	}
	
	private String getUserName(String id) {
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
	public String getPresentation(BPMProcessVariable variable) {
		return (variable == null || variable.getValue() == null) ? CoreConstants.EMPTY : getPresentation(variable.getValue());
	}

	@Override
	public String getPresentation(String value) {
		if (StringUtil.isEmpty(value))
			return null;
		
		String[] ids = value.split(CoreConstants.SEMICOLON);
		return ArrayUtil.isEmpty(ids) ? CoreConstants.MINUS : getPresentation(Arrays.asList(ids));
	}

	@Override
	public String getPresentation(VariableInstanceInfo variable) {
		return (variable == null || variable.getValue() == null) ? CoreConstants.MINUS : getPresentation(Arrays.asList(variable.getValue().toString()));
	}
	
	private String getPresentation(Collection<String> usersIds) {
		if (ListUtil.isEmpty(usersIds))
			return CoreConstants.MINUS;
		
		try {
			UserBusiness userBusiness = getServiceInstance(UserBusiness.class);
			@SuppressWarnings("unchecked")
			Collection<User> users = userBusiness.getUsers(ArrayUtil.convertListToArray(usersIds));
			if (ListUtil.isEmpty(users))
				return CoreConstants.MINUS;
			
			StringBuilder presentation = new StringBuilder();
			for (Iterator<User> usersIter = users.iterator(); usersIter.hasNext();) {
				User user = usersIter.next();
				String name = user == null ? null : user.getName();
				if (StringUtil.isEmpty(name))
					continue;
				
				presentation.append(name);
				if (usersIter.hasNext())
					presentation.append(CoreConstants.COMMA).append(CoreConstants.SPACE);
			}
			String result = presentation.toString();
			return StringUtil.isEmpty(result) ? CoreConstants.MINUS : result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return CoreConstants.MINUS;
	}

}