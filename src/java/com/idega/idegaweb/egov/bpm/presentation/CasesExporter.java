package com.idega.idegaweb.egov.bpm.presentation;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.cases.business.CasesEngine;
import is.idega.idegaweb.egov.cases.presentation.CasesSearcher;
import is.idega.idegaweb.egov.cases.util.CasesConstants;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.web2.business.JQuery;
import com.idega.block.web2.business.Web2Business;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.builder.business.AdvancedPropertyComparator;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.presentation.IWContext;
import com.idega.presentation.Layer;
import com.idega.presentation.ui.DropdownMenu;
import com.idega.presentation.ui.GenericButton;
import com.idega.presentation.ui.IWDatePicker;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.PresentationUtil;
import com.idega.util.expression.ELUtil;

public class CasesExporter extends CasesSearcher {

	private static final String PARAMETER_PROCESS_ID = "ce_prm_process_id";

	@Autowired
	private CasesEngine casesEngine;

	@Autowired
	private CasesBPMDAO casesBPMDAO;

	@Autowired
	private Web2Business web2;

	@Autowired
	private JQuery jQuery;

	public static final File getDirectory(String id) {
		File baseDir = new File(System.getProperty("java.io.tmpdir") + File.separator + "exported_cases" + File.separator + id);
		if (!baseDir.exists()) {
			if (baseDir.mkdirs()) {
				return baseDir;
			}
			return null;
		}
		return baseDir;
	}

	@Override
	public void main(IWContext iwc) throws Exception {
		ELUtil.getInstance().autowire(this);

		IWBundle bundle = getBundle(iwc);
		PresentationUtil.addJavaScriptSourcesLinesToHeader(iwc, Arrays.asList(
				jQuery.getBundleURIToJQueryLib(),
				web2.getBundleUriToHumanizedMessagesScript(),
				CoreConstants.DWR_ENGINE_SCRIPT,
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
		Layer inputsContainer = new Layer();
		container.add(inputsContainer);
		DropdownMenu processes = getDropdownForProcess(iwc);
		addFormItem(inputsContainer, "process", iwrb.getLocalizedString("cases_search_select_process", "Process"), processes);

		//	Status
		DropdownMenu statuses = getDropdownForStatus(iwc);
		addFormItem(inputsContainer, "status", iwrb.getLocalizedString("status", "Status"), statuses);

		//	Dates from and to
		IWDatePicker dateRange = getDateRange(iwc, "dateRange", null, null);
		addFormItem(inputsContainer, "dateRange", iwrb.getLocalizedString("date_range", "Date range"), dateRange);

		Layer resultsContainer = new Layer();
		container.add(resultsContainer);

		GenericButton export = new GenericButton(iwrb.getLocalizedString("export_search_results", "Export"));
		export.setStyleClass("cases-exporter-action-button");
		inputsContainer.add(export);
		export.setOnClick(
				"CasesExporter.doExportCases({exporting: '" + iwrb.getLocalizedString("exporting", "Exporting...") +
				"', loading: '" + iwrb.getLocalizedString("loading", "Loading...") + "', dropdownId: '" + processes.getId() +
				"', id: '" + UUID.randomUUID().toString() + "', resultsId: '" + resultsContainer.getId() + "', resultsUI: '" +
				CasesExporterResults.class.getName() + "', selectCriterias: '" +
				iwrb.getLocalizedString("select_status_or_and_date_range", "You have to select status and/or date range") + "'});"
		);
	}

	@Override
	public String getBundleIdentifier() {
		return IWBundleStarter.IW_BUNDLE_IDENTIFIER;
	}

	private DropdownMenu getDropdownForProcess(IWContext iwc) {
		DropdownMenu menu = new DropdownMenu(PARAMETER_PROCESS_ID);
		menu.setStyleClass("availableVariablesChooserForProcess");
		String selectedProcess = iwc.isParameterSet(PARAMETER_PROCESS_ID)  ? iwc.getParameter(PARAMETER_PROCESS_ID) : null;

		List<AdvancedProperty> allProcesses = casesEngine.getAvailableProcesses(iwc);

		if (ListUtil.isEmpty(allProcesses)) {
			return menu;
		}

		IWResourceBundle iwrb = getResourceBundle(iwc);

		String total = iwrb.getLocalizedString("total", "total");
		for (AdvancedProperty process: allProcesses) {
			Integer numberOfApplications = casesBPMDAO.getNumberOfApplications(Long.valueOf(process.getId()));
			if (numberOfApplications == null) {
				numberOfApplications = 0;
			}
			process.setValue(process.getValue() + CoreConstants.SPACE + CoreConstants.BRACKET_LEFT + total + CoreConstants.COLON + CoreConstants.SPACE +
					numberOfApplications + CoreConstants.BRACKET_RIGHT);
		}

		Collections.sort(allProcesses, new AdvancedPropertyComparator(iwc.getCurrentLocale()));

		fillDropdown(iwc.getCurrentLocale(), menu, allProcesses, new AdvancedProperty(String.valueOf(-1),
				iwrb.getLocalizedString("cases_search_select_process", "Select process")), selectedProcess);

		return menu;
	}

}