jQuery(document).ready(function() {

    var jQGridInclude = new JQGridInclude();
    jQGridInclude.SUBGRID = true;
    jqGridInclude(jQGridInclude);

    jQuery("#example > ul").tabs({ selected: 0 });
    
    jQuery('.ui-tabs-nav').bind('select.ui-tabs', function(event, ui) {
    
	    if(CasesBPMAssets.tabIndexes.tasks == ui.panel.id) {
	    	CasesBPMAssets.initTaskTab(ui.panel);
	    	
	    } else if(CasesBPMAssets.tabIndexes.documents == ui.panel.id) {
	    
	    	CasesBPMAssets.initDocumentsTab("#documentsTable", BPMProcessAssets.getProcessDocumentsList, ['Document name', 'Date submitted']);
	    	CasesBPMAssets.initDocumentsTab("#emailsTable", BPMProcessAssets.getProcessEmailsList, ['Subject', 'Receive date']);
	    }
	});
	
	CasesBPMAssets.downloader = jQuery("#casesBPMAttachmentDownloader");
	CasesBPMAssets.downloader_link = jQuery(CasesBPMAssets.downloader).attr("href");
});

if(CasesBPMAssets == null) var CasesBPMAssets = {};

CasesBPMAssets.downloader = null;
CasesBPMAssets.downloader_link = null;

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
    
    //params.colNames = ['Nr','Task name', 'Date created', 'Taken by', 'Status']; 
    params.colNames = ['Task name', 'Date created', 'Taken by', 'Status'];
    params.colModel = [
                //{name:'id',index:'id', width:55},
                {name:'name',index:'name'}, 
                {name:'createdDate',index:'createdDate'},
                {name:'takenBy',index:'takenBy'},
                {name:'status',index:'status'}
    ];
    
    params.onSelectRow = function(rowId) {
  
      jQuery(CasesBPMAssets.exp_viewSelected)[0].value = rowId;
      jQuery(CasesBPMAssets.exp_gotoTask)[0].click();
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
	
	CasesBPMAssets.initTaskTab.inited = true;
};

CasesBPMAssets.initTaskTab.inited = false;

CasesBPMAssets.initDocumentsTab = function(tblId, retrievalFunction, colNames) {

	//if(CasesBPMAssets.initDocumentsTab.inited)
//		return;
		
    var params = new JQGridParams();
    
    params.populateFromFunction = function(params, callback) {
            
        params.piId = jQuery(CasesBPMAssets.exp_piId)[0].value;
                                
        retrievalFunction(params,
            {
                callback: function(result) {
                    callback(result);
                }
            }
        );
    };
    
    //params.colNames = ['Nr','Document name', 'Date submitted']; 
    params.colNames = colNames;
    params.colModel = [
                //{name:'id',index:'id', width:55},
                {name:'name',index:'name'},
                {name:'submittedDate',index:'submittedDate'}
                //{name:'submittedBy',index:'submittedBy'}
    ];
    
    params.onSelectRow = function(rowId) {
  
      jQuery(CasesBPMAssets.exp_viewSelected)[0].value = rowId;
      jQuery(CasesBPMAssets.exp_gotoDocuments)[0].click();
    };
    
    CasesBPMAssets.addFilesSubgrid(params);
    
    var grid = new JQGrid();
    grid.createGrid(jQuery(tblId), params);
		
		/*
	jQuery(jQuery(tabContainer).children('div')).each(
		function(i) {
			jQuery(this).css({width: "auto", height: "auto"});
		}
	);
	*/
	
	CasesBPMAssets.initDocumentsTab.inited = true; 	
}

CasesBPMAssets.addFilesSubgrid = function(params) {

    params.subGrid = true;
    params.subGridRowExpanded = function(subgridId, rowId) {
    
         var subgridTableId;
         subgridTableId = subgridId+"_t";
         jQuery("#"+subgridId).html("<table id='"+subgridTableId+"' class='scroll' cellpadding='0' cellspacing='0'></table>");
         
         var subGridParams = new JQGridParams();
   
        subGridParams.populateFromFunction = function(params, callback) {
                
                    params.taskId = rowId;
                    
                    BPMProcessAssets.getTaskAttachments(params,
                        {
                            callback: function(result) {
                                callback(result);
                            }
                        }
                    );
        };
        
        subGridParams.colNames = ['File name']; 
        subGridParams.colModel = [
                    {name:'name',index:'name'} 
        ];
        
        subGridParams.onSelectRow = function(fileRowId) {
   
              var newLink = CasesBPMAssets.downloader_link+"&taskInstanceId="+rowId+"&varHash="+fileRowId;
              window.location.href = newLink;
        };

        //TODO: set height automatically (?)
        subGridParams.height = 70;
        
        var subgrid = new JQGrid();
        subgrid.createGrid("#"+subgridTableId, subGridParams);
    };
}

CasesBPMAssets.initDocumentsTab.inited = false;

CasesBPMAssets.exp_gotoTask = '.assetsState .state_gotoTaskView';
CasesBPMAssets.exp_gotoDocuments = '.assetsState .state_gotoDocumentsView';
CasesBPMAssets.exp_viewSelected = '#state_viewSelected';
CasesBPMAssets.exp_piId = '#state_processInstanceId';