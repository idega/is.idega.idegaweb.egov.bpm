package com.idega.idegaweb.egov.bpm.presentation;

import is.idega.idegaweb.egov.bpm.IWBundleStarter;
import is.idega.idegaweb.egov.cases.util.CasesConstants;

import java.io.File;

import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.Block;
import com.idega.presentation.IWContext;
import com.idega.presentation.Layer;
import com.idega.presentation.Table2;
import com.idega.presentation.TableBodyRowGroup;
import com.idega.presentation.TableHeaderCell;
import com.idega.presentation.TableHeaderRowGroup;
import com.idega.presentation.TableRow;
import com.idega.presentation.text.Heading4;
import com.idega.presentation.text.Text;
import com.idega.presentation.ui.GenericButton;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.PresentationUtil;
import com.idega.util.StringUtil;

public class CasesExporterResults extends Block {

	private String exportId;

	public String getExportId() {
		return exportId;
	}

	public void setExportId(String exportId) {
		this.exportId = exportId;
	}

	@Override
	public void main(IWContext iwc) throws Exception {
		IWBundle bundle = getBundle(iwc);
		PresentationUtil.addStyleSheetToHeader(iwc, bundle.getVirtualPathWithFileNameString("style/casesBPM.css"));

		Layer container = new Layer();
		add(container);
		container.setStyleClass("exported-cases-results-table");

		String exportId = getExportId();
		String error = getResourceBundle(iwc).getLocalizedString("unable_to_find_exported_cases", "Unable to find exported cases, please try again");
		if (StringUtil.isEmpty(exportId)) {
			container.add(new Heading4(error));
			return;
		}
		File baseDir = CasesExporter.getDirectory(exportId);
		if (baseDir == null || !baseDir.exists() || !baseDir.canRead() || !baseDir.isDirectory()) {
			container.add(new Heading4(error));
			return;
		}
		File[] casesFolders = baseDir.listFiles();
		if (ArrayUtil.isEmpty(casesFolders)) {
			container.add(new Heading4(error));
			return;
		}

		container.add(new Heading4(getResourceBundle(iwc).getLocalizedString("exported_cases", "Exported cases") + CoreConstants.COLON));

		IWBundle casesBundle = iwc.getIWMainApplication().getBundle(CasesConstants.IW_BUNDLE_IDENTIFIER);
		IWResourceBundle iwrb = casesBundle.getResourceBundle(iwc);

		Table2 table = new Table2();
		container.add(table);
		TableHeaderRowGroup headerRow = table.createHeaderRowGroup();
		TableRow row = headerRow.createRow();
		TableHeaderCell headerCell = row.createHeaderCell();
		headerCell.add(new Text(iwrb.getLocalizedString("nr", "Nr")));

		headerCell = row.createHeaderCell();
		headerCell.add(new Text(iwrb.getLocalizedString("case_nr", "Case nr.")));

		headerCell = row.createHeaderCell();
		String download = iwrb.getLocalizedString("download", "Download");
		headerCell.add(new Text(CoreConstants.EMPTY));
		TableBodyRowGroup bodyRow = table.createBodyRowGroup();
		int index = 0;
		for (File caseFolder: casesFolders) {
			index++;
			if (caseFolder == null || !caseFolder.exists() || !caseFolder.canRead() || !caseFolder.isDirectory()) {
				continue;
			}

			row = bodyRow.createRow();

			row.createCell().add(new Text(String.valueOf(index)));

			String identifier = caseFolder.getName();
			row.createCell().add(new Text(identifier));

			GenericButton downloadButton = new GenericButton(download);
			downloadButton.setOnClick("CasesExporter.doDownload('" + exportId + "', '" + identifier + "');");
			row.createCell().add(downloadButton);
		}

		row = bodyRow.createRow();
		row.createCell();
		row.createCell();

		GenericButton downloadAllButton = new GenericButton(iwrb.getLocalizedString("download_all", "Download all"));
		downloadAllButton.setOnClick("CasesExporter.doDownload('" + exportId + "', null);");
		downloadAllButton.setStyleClass("exported-cases-results-download-all-button");
		row.createCell().add(downloadAllButton);
	}

	@Override
	public String getBundleIdentifier() {
		return IWBundleStarter.IW_BUNDLE_IDENTIFIER;
	}

}