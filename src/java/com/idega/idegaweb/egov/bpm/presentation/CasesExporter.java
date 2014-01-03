package com.idega.idegaweb.egov.bpm.presentation;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.cases.business.CasesEngine;
import is.idega.idegaweb.egov.cases.util.CasesConstants;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.faces.component.UIComponent;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.web2.business.JQuery;
import com.idega.block.web2.business.Web2Business;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.builder.business.AdvancedPropertyComparator;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.Block;
import com.idega.presentation.IWContext;
import com.idega.presentation.Layer;
import com.idega.presentation.text.Heading4;
import com.idega.presentation.ui.DropdownMenu;
import com.idega.presentation.ui.GenericButton;
import com.idega.presentation.ui.InterfaceObject;
import com.idega.presentation.ui.Label;
import com.idega.presentation.ui.SelectOption;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.PresentationUtil;
import com.idega.util.expression.ELUtil;

public class CasesExporter extends Block {

	private static final String PARAMETER_PROCESS_ID = "ce_prm_process_id";

	@Autowired
	private CasesEngine casesEngine;

	@Autowired
	private Web2Business web2;

	@Autowired
	private JQuery jQuery;

	@Override
	public void main(IWContext iwc) throws Exception {
		ELUtil.getInstance().autowire(this);

		IWBundle bundle = getBundle(iwc);
		PresentationUtil.addJavaScriptSourcesLinesToHeader(iwc, Arrays.asList(
				jQuery.getBundleURIToJQueryLib(),
				web2.getBundleUriToHumanizedMessagesScript(),
				"/dwr/interface/CasesEngine.js",
				getBundle(iwc).getVirtualPathWithFileNameString("javascript/CasesExporter.js")
		));

		IWBundle applicationBundle = iwc.getIWMainApplication().getBundle(is.idega.idegaweb.egov.application.IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		IWBundle casesBundle = iwc.getIWMainApplication().getBundle(CasesConstants.IW_BUNDLE_IDENTIFIER);
		PresentationUtil.addStyleSheetsToHeader(iwc, Arrays.asList(
				applicationBundle.getVirtualPathWithFileNameString("style/application.css"),
				bundle.getVirtualPathWithFileNameString("style/casesBPM.css"),
				web2.getBundleUriToHumanizedMessagesStyleSheet()
		));

		Layer container = new Layer();
		add(container);
		container.setStyleClass("cases-exporter-container");

		IWResourceBundle iwrb = casesBundle.getResourceBundle(iwc);
		Layer processesContainer = new Layer();
		container.add(processesContainer);
		DropdownMenu processes = getDropdownForProcess(iwc);
		addFormItem(processesContainer, "process", iwrb.getLocalizedString("cases_search_select_process", "Process"), processes);

		Layer resultsContainer = new Layer();
		container.add(resultsContainer);
		Heading4 result = new Heading4();
		result.setStyleAttribute("display: none;");
		resultsContainer.add(result);

		Layer buttonsLayer = new Layer();
		buttonsLayer.setStyleClass("buttonLayer");
		container.add(buttonsLayer);
		GenericButton export = new GenericButton(iwrb.getLocalizedString("export_search_results", "Export"));
		buttonsLayer.add(export);
		export.setOnClick("CasesExporter.doExportCases({loading: '" + iwrb.getLocalizedString("exporting", "Exporting...") +
				"', dropdownId: '" + processes.getId() + "', id: '" + UUID.randomUUID().toString() + "', resultsId: '" + result.getId() + "'});");
	}

	private Layer addFormItem(Layer layer, String styleClass, String localizedLabelText, InterfaceObject input, UIComponent... additionalComponents) {
		Layer element = new Layer(Layer.DIV);
		layer.add(element);
		element.setStyleClass("formItem shortFormItem");

		Label label = null;
		label = new Label(localizedLabelText == null ? CoreConstants.MINUS : localizedLabelText, input);
		element.add(label);
		element.add(input);

		if (!ArrayUtil.isEmpty(additionalComponents)) {
			for (UIComponent component: additionalComponents) {
				element.add(component);
			}
		}
		return element;
	}

	@Override
	public String getBundleIdentifier() {
		return IWBundleStarter.IW_BUNDLE_IDENTIFIER;
	}

	private DropdownMenu getDropdownForProcess(IWContext iwc) {
		DropdownMenu menu = new DropdownMenu(PARAMETER_PROCESS_ID);
		menu.setStyleClass("availableVariablesChooserForProcess");
		String selectedProcess = iwc.isParameterSet(PARAMETER_PROCESS_ID) ? iwc.getParameter(PARAMETER_PROCESS_ID) : null;

		List<AdvancedProperty> allProcesses = casesEngine.getAvailableProcesses(iwc);

		if (ListUtil.isEmpty(allProcesses)) {
			return menu;
		}

		IWResourceBundle iwrb = getResourceBundle(iwc);

		Collections.sort(allProcesses, new AdvancedPropertyComparator(iwc.getCurrentLocale()));

		fillDropdown(iwc.getCurrentLocale(), menu, allProcesses, new AdvancedProperty(String.valueOf(-1),
				iwrb.getLocalizedString("cases_search_select_process", "Select process")), selectedProcess);

		menu.setOnChange(new StringBuilder("CasesListHelper.getProcessDefinitionVariablesByIwID('").append(iwrb
				.getLocalizedString("loading", "Loading...")).append("', '").append(menu.getId()).append("');").toString());

		return menu;
	}

	private void fillDropdown(Locale locale, DropdownMenu menu, List<AdvancedProperty> options, AdvancedProperty firstElement,
			String selectedElement) {
		if (locale == null) {
			locale = Locale.ENGLISH;
		}
		Collections.sort(options, new AdvancedPropertyComparator(locale));

		for (AdvancedProperty option: options) {
			menu.addOption(new SelectOption(option.getValue(), option.getId()));
		}
		if (firstElement != null) {
			menu.addFirstOption(new SelectOption(firstElement.getValue(), firstElement.getId()));
		}

		if (selectedElement != null) {
			menu.setSelectedElement(selectedElement);
		}

		if (ListUtil.isEmpty(options)) {
			menu.setDisabled(true);
		}
	}

}