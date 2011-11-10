package com.idega.idegaweb.egov.bpm.report.presentation;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.cases.presentation.CasesSearcher;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.idegaweb.IWResourceBundle;
import com.idega.jbpm.bean.VariableInstanceType;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.presentation.CSSSpacer;
import com.idega.presentation.IWContext;
import com.idega.presentation.Layer;
import com.idega.presentation.ui.GenericButton;
import com.idega.presentation.ui.InterfaceObject;
import com.idega.presentation.ui.TextInput;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

public abstract class CasesReportFilter extends CasesSearcher {

	@Autowired
	private VariableInstanceQuerier variablesQuerier;
	
	@Override
	protected abstract void addHeader();
	
	protected abstract void addStyleSheets(IWContext iwc);
	
	protected abstract void addJavaScript(IWContext iwc, String containerId);
	
	protected abstract String getPersonalIdFieldName();
	
	@Override
	public abstract void main(IWContext iwc) throws Exception;
	
	protected abstract List<String> getProcesses();
	
	protected abstract String getReportType();
	
	protected abstract String getFilterAction();
	
	@Override
	protected void present(IWContext iwc) throws Exception {
		addInputs(iwc);
		addButtons(iwc);
	}
	
	protected void addButtons(IWContext iwc) {
		inputsContainer.add(new CSSSpacer());

		Layer buttonsContainer = getContainer("buttonLayer");
		inputsContainer.add(buttonsContainer);
		
		IWResourceBundle iwrb = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc);
		
		GenericButton filter = new GenericButton(iwrb.getLocalizedString("filter", "Filter"));
		buttonsContainer.add(filter);
		filter.setOnClick("ParkingReportFilter.".concat(getFilterAction()).concat(";"));
		
		GenericButton export = new GenericButton(iwrb.getLocalizedString("export_search_results", "Export"));
		export.setOnClick("ParkingReportFilter.exportReport();");
		buttonsContainer.add(export);
	}
	
	protected abstract void addInputs(IWContext iwc);
	
	protected void addHiddenInput(String varName) {
		addHiddenInput(varName, null);
	}
	
	protected void addHiddenInput(String varName, String value) {
		TextInput input = getTextInput(varName, null);
		if (value != null)
			input.setValue(value);
		
		Layer hiddenElement = addFormItem(inputsContainer, null, input, VariableInstanceType.STRING.getTypeKeys().get(0), false);
		hiddenElement.setStyleAttribute("display", "none");
	}
	
	protected void addFormItem(Layer layer, String localizedLabelText, InterfaceObject input, String type) {
		addFormItem(layer, localizedLabelText, input, type, Boolean.FALSE);
	}
	
	protected Layer addFormItem(Layer layer, String localizedLabelText, InterfaceObject input, String type, Boolean flexibleSearh) {
		input.getId();
		if (!StringUtil.isEmpty(type)) {
			input.setName(input.getName().concat("@").concat(type));
		}
		if (flexibleSearh != null) {
			input.setName(input.getName().concat("@").concat(String.valueOf(flexibleSearh)));
		}
		input.setStyleClass("parkingReportFilterInput");
		
		return super.addFormItem(layer, localizedLabelText, input);
	}

	protected VariableInstanceQuerier getVariablesQuerier() {
		if (variablesQuerier == null)
			ELUtil.getInstance().autowire(this);
		
		return variablesQuerier;
	}
}