package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import java.io.Serializable;
import java.util.Collection;

import com.idega.block.process.presentation.beans.CasePresentation;
import com.idega.io.MemoryFileBuffer;

public interface CasesSearchResultsHolder extends Serializable {

	public static final String SPRING_BEAN_IDENTIFIER = "casesSearchResultsHolder";
	
	public void setSearchResults(Collection<CasePresentation> cases);
	
	public boolean isSearchResultStored();
	
	public boolean doExport();
	
	public MemoryFileBuffer getExportedSearchResults();
	
}
