jQuery(document).ready(function() {

    var jQGridInclude = new JQGridInclude();
    jQGridInclude.SUBGRID = true;
    jqGridInclude(jQGridInclude);

    jQuery("#example > ul").tabs({ selected: 0 });
    
    jQuery('.ui-tabs-nav').bind('select.ui-tabs', function(event, ui) {
    
	    if(CasesBPMAssets.tabIndexes.tasks == ui.panel.id) {
	    	CasesBPMAssets.initTaskTab(ui.panel);
	    	
	    } else if(CasesBPMAssets.tabIndexes.documents == ui.panel.id) {
	    	CasesBPMAssets.initDocumentsTab(ui.panel);
	    }
	});
});

if(CasesBPMAssets == null) var CasesBPMAssets = {};

CasesBPMAssets.tabIndexes = {

	tasks: 'tasksTab',
	documents: 'documentsTab'
};

CasesBPMAssets.initTaskTab = function(tabContainer) {

	if(CasesBPMAssets.initTaskTab.inited)
		return;
		
    var params = new JQGridParams();
    
    params.populateFromFunction = function(params, callback) {
            
                params.piId = jQuery(CasesBPMAssets.exp_piId)[0].value;
                
                BPMProcessAssets.getProcessTasksList(params,
                    {
                        callback: function(result) {
                            callback(result);
                        }
                    }
                );
    };
    
    params.colNames = ['Nr','Task name', 'Date created', 'Taken by', 'Status']; 
    params.colModel = [
                {name:'id',index:'id', width:55},
                {name:'name',index:'name'}, 
                {name:'createdDate',index:'createdDate'},
                {name:'takenBy',index:'takenBy'},
                {name:'status',index:'status'}
    ];
    
    params.onSelectRow = function(rowId) {
  
      jQuery(CasesBPMAssets.exp_viewSelected)[0].value = rowId;
      jQuery(CasesBPMAssets.exp_gotoTask)[0].click();
    };
    
    /*
    params.subGrid = true;
    params.subGridRowExpanded = function(subgridId, rowId) {
    
         var subgridTableId;
         subgridTableId = subgridId+"_t";
         jQuery("#"+subgridId).html("<table id='"+subgridTableId+"' class='scroll' cellpadding='0' cellspacing='0'></table>");
         
         var gridParams = new JQGridParams();
   
	    gridParams.populateFromFunction = function(params, callback) {
	            
	                params.piId = jQuery(CasesBPMAssets.exp_piId)[0].value;
	                
	                BPMProcessAssets.getProcessTasksList(params,
	                    {
	                        callback: function(result) {
	                            callback(result);
	                        }
	                    }
	                );
	    };
	    
	    gridParams.colNames = ['Nr','Task name', 'Date created', 'Taken by', 'Status']; 
	    gridParams.colModel = [
	                {name:'id',index:'id', width:55},
	                {name:'name',index:'name'}, 
	                {name:'createdDate',index:'createdDate'},
	                {name:'takenBy',index:'takenBy'},
	                {name:'status',index:'status'}
	    ];
	    
	    gridParams.onSelectRow = function(rowId) {
  
		      jQuery(CasesBPMAssets.exp_viewSelected)[0].value = rowId;
		      jQuery(CasesBPMAssets.exp_gotoTask)[0].click();
	    };
	    
        var subgrid = new JQGrid();
        subgrid.createGrid("#"+subgridTableId, gridParams);
    };
    */

    var grid = new JQGrid();
    grid.createGrid(jQuery(tabContainer).children('table')[0], params);
		
		/*
	jQuery(jQuery(tabContainer).children('div')).each(
		function(i) {
			jQuery(this).css({width: "auto", height: "auto"});
		}
	);
	*/
	
	CasesBPMAssets.initTaskTab.inited = true;
};

CasesBPMAssets.initTaskTab.inited = false;

CasesBPMAssets.initDocumentsTab = function(tabContainer) {

	if(CasesBPMAssets.initDocumentsTab.inited)
		return;
		
    var params = new JQGridParams();
    
    params.populateFromFunction = function(params, callback) {
            
        params.piId = jQuery(CasesBPMAssets.exp_piId)[0].value;
                                
        BPMProcessAssets.getProcessDocumentsList(params,
            {
                callback: function(result) {
                    callback(result);
                }
            }
        );
    };
    
    params.colNames = ['Nr','Document name', 'Date submitted']; 
    params.colModel = [
                {name:'id',index:'id', width:55},
                {name:'name',index:'name'}, 
                {name:'submittedDate',index:'submittedDate'}
                //{name:'submittedBy',index:'submittedBy'}
    ];
    
    params.onSelectRow = function(rowId) {
  
      jQuery(CasesBPMAssets.exp_viewSelected)[0].value = rowId;
      jQuery(CasesBPMAssets.exp_gotoDocuments)[0].click();
    };

    var grid = new JQGrid();
    grid.createGrid(jQuery(tabContainer).children('table')[0], params);
		
		/*
	jQuery(jQuery(tabContainer).children('div')).each(
		function(i) {
			jQuery(this).css({width: "auto", height: "auto"});
		}
	);
	*/
	
	CasesBPMAssets.initDocumentsTab.inited = true; 	
}

CasesBPMAssets.initDocumentsTab.inited = false;

CasesBPMAssets.exp_gotoTask = '.assetsState .state_gotoTaskView';
CasesBPMAssets.exp_gotoDocuments = '.assetsState .state_gotoDocumentsView';
CasesBPMAssets.exp_viewSelected = '#state_viewSelected';
CasesBPMAssets.exp_piId = '#state_processInstanceId';