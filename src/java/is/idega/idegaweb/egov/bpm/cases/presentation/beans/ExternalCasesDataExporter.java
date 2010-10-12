package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import java.util.List;

import com.idega.block.process.presentation.beans.CasePresentation;

public interface ExternalCasesDataExporter {
	
	public List<CasePresentation> getExternalData(String id);

}