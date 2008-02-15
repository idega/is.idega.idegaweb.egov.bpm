package is.idega.idegaweb.egov.bpm.cases.presentation.beans;

import java.io.Serializable;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/02/15 10:19:34 $ by $Author: civilis $
 *
 */
public class CasesBPMAssetsState implements Serializable {

	private static final long serialVersionUID = -6474883869451606583L;
	
	private String viewSelected;
	private FacetRendered facetRendered = FacetRendered.ASSETS;
	
	public String getViewSelected() {
		return viewSelected;
	}

	public void setViewSelected(String viewSelected) {
		System.out.println("setviewselected: "+viewSelected);
		this.viewSelected = viewSelected;
	}
	
	public void selectTaskView() {
	
		System.out.println("selecting task view: "+getViewSelected());
		facetRendered = FacetRendered.ASSET_VIEW;
	}
	
	public void selectDocumentView() {
		
		System.out.println("selecting document view");
		facetRendered = FacetRendered.ASSET_VIEW;
	}
	
	public boolean isAssetsRendered() {
	
		System.out.println("is assets rendered: "+(facetRendered == FacetRendered.ASSETS));
		return facetRendered == FacetRendered.ASSETS;
	}
	
	public boolean isAssetViewRendered() {
		
		System.out.println("is assetView rendered: "+(facetRendered == FacetRendered.ASSET_VIEW));
		return facetRendered == FacetRendered.ASSET_VIEW;
	}
	
	private enum FacetRendered {
		
		ASSETS,
		ASSET_VIEW
	}
}