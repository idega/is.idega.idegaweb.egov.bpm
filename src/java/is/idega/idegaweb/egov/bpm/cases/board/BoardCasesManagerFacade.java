package is.idega.idegaweb.egov.bpm.cases.board;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.bpm.business.TaskViewerHelper;
import is.idega.idegaweb.egov.cases.presentation.CasesBoardViewer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.idega.builder.bean.AdvancedProperty;
import com.idega.core.business.DefaultSpringBean;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewSubmission;
import com.idega.presentation.IWContext;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

@Transactional
@Service("boardCasesManagerBean")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class BoardCasesManagerFacade extends DefaultSpringBean {

	@Autowired
	private CasesBPMDAO casesBPMDAO;

	@Autowired
	private BPMFactory bpmFactory;

	@Autowired
	private TaskViewerHelper taskViewer;

	@Transactional(propagation = Propagation.REQUIRED)
	public AdvancedProperty setCaseVariableValue(Integer caseId, String variableName, String value, String role, String backPage, Integer valueIndex,
			Integer totalValues) {
		if (caseId == null || StringUtil.isEmpty(variableName) || StringUtil.isEmpty(value))
			return null;

		boolean useNumberSign = valueIndex != null && totalValues != null;
		if (valueIndex == null)
			valueIndex = 0;
		if (totalValues == null)
			totalValues = 1;

		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null || !iwc.isLoggedOn())
			return null;
		if (!StringUtil.isEmpty(role) && !iwc.hasRole(role))
			return null;

		try {
			if (value.equals("no_value"))
				value = CoreConstants.EMPTY;

			Long processInstanceId = casesBPMDAO.getCaseProcInstBindByCaseId(caseId).getProcInstId();

			ProcessInstanceW piw = bpmFactory.getProcessInstanceW(processInstanceId);

			String taskName = "Grading";
			List<TaskInstanceW> allTasks = piw.getUnfinishedTaskInstancesForTask(taskName);

			if (ListUtil.isEmpty(allTasks)) {
				getLogger().warning("No tasks instances were found for task = " + taskName + " by process instance: " + processInstanceId);
				return null;
			}

			// should be only one task instance
			if (allTasks.size() > 1)
				getLogger().warning("More than one task instance found for task = " + taskName + " when only one expected");

			TaskInstanceW sharedTIW = allTasks.iterator().next();
			Long sharedTaskInstanceId = sharedTIW.getTaskInstanceId();
			View view = sharedTIW.loadView();

			// TODO: move getViewSubmission to view too
			// TODO: add addVariable and so to the viewSubmission
			ViewSubmission viewSubmission = bpmFactory.getViewSubmission();
			Map<String, Object> variables = view.resolveVariables();
			if (variables == null)
				variables = new HashMap<String, Object>();

			if (useNumberSign) {
				String[] currentValues = null;
				Object currentValue = variables.get(variableName);
				if (currentValue instanceof String)
					currentValues = currentValue.toString().split(CoreConstants.HASH);

				StringBuffer newValue = new StringBuffer();
				for (int i = 0; i < totalValues; i++) {
					if (i == valueIndex)
						newValue.append(value);
					else {
						if (ArrayUtil.isEmpty(currentValues))
							newValue.append(String.valueOf(0));
						else {
							if (i < currentValues.length)
								newValue.append(currentValues[i]);
							else
								newValue.append(String.valueOf(0));
						}
					}

					if (i < totalValues)
						newValue.append(CoreConstants.HASH);
				}

				value = newValue.toString();
			}

			variables.put(variableName, value);

			viewSubmission.populateParameters(view.resolveParameters());
			viewSubmission.populateVariables(variables);

			Long viewTaskInstanceId = view.getTaskInstanceId();

			TaskInstanceW viewTIW = bpmFactory.getProcessManagerByTaskInstanceId(viewTaskInstanceId).getTaskInstance(viewTaskInstanceId);

			viewTIW.submit(viewSubmission);

			return new AdvancedProperty(value, taskViewer.getLinkToTheTask(iwc, caseId.toString(), sharedTaskInstanceId.toString(), backPage));
		} catch (Exception e) {
			getLogger().log(Level.SEVERE, "Error saving variable '" + variableName + "' with value '" + value + "' for case: " + caseId, e);
		}

		return null;
	}

	public String saveCustomizedColumns(String uuid, List<String> columns) {
		String errorMessage = "Some error occurred. Reload a page and try again";
		IWResourceBundle iwrb = getResourceBundle(getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER));
		if (StringUtil.isEmpty(uuid) || CoreConstants.MINUS.equals(uuid)) {
			getLogger().warning("UUID is not provided of the " + CasesBoardViewer.class + " UI component");
			return iwrb.getLocalizedString("some_error_occurred", errorMessage);
		}
		if (ListUtil.isEmpty(columns)) {
			getLogger().warning("No columns selected");
			return iwrb.getLocalizedString("no_columns_selected", "Please select some columns to display");
		}

		IWContext iwc = CoreUtil.getIWContext();
		iwc.setSessionAttribute(CasesBoardViewer.PARAMETER_CUSTOM_COLUMNS + uuid, columns);
		return null;
	}

	public String resetCustomizedColumns(String uuid) {
		IWResourceBundle iwrb = getResourceBundle(getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER));
		if (StringUtil.isEmpty(uuid))
			return iwrb.getLocalizedString("error_resetting_custom_columns", "Some error occurred. Reload a page and try again");

		IWContext iwc = CoreUtil.getIWContext();
		iwc.removeSessionAttribute(CasesBoardViewer.PARAMETER_CUSTOM_COLUMNS + uuid);
		return null;
	}

}