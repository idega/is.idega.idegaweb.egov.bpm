if(Localization == null) var Localization = {};

Localization.DOCUMENT_NAME 			    = 'Document name';
Localization.DATE_SUBMITTED 			= 'Date submitted';
Localization.DATE_CREATED				= 'Date created';
Localization.SUBJECT					= 'Subject';
Localization.FROM						= 'From';
Localization.RECIEVE_DATE				= 'Receive date';
Localization.FILE_NAME					= 'File name';
Localization.TASK_NAME					= 'Task name';
Localization.TAKEN_BY					= 'Taken by';
Localization.STATUS						= 'Status';

jQuery(document).ready(function() {

    var val = jQuery("#selectedTabState").val();
    CasesBPMAssets.selectedTab = parseInt(val);

    var jQGridInclude = new JQGridInclude();
    jQGridInclude.SUBGRID = true;
    jqGridInclude(jQGridInclude);

    jQuery("#example > ul").tabs({ selected: CasesBPMAssets.selectedTab});
    
    jQuery('.ui-tabs-nav').bind('select.ui-tabs', function(event, ui) {
    
        if(CasesBPMAssets.tabIndexes.tasks == ui.panel.id) {
	    
	       CasesBPMAssets.initTab(CasesBPMAssets.selectedTabIndexes.tasks);
	       jQuery("#selectedTabState").val(CasesBPMAssets.selectedTabIndexes.tasks);
	    	
	    } else if(CasesBPMAssets.tabIndexes.documents == ui.panel.id) {
	    
	    	CasesBPMAssets.initTab(CasesBPMAssets.selectedTabIndexes.documents);
	    	jQuery("#selectedTabState").val(CasesBPMAssets.selectedTabIndexes.documents);
	    }
	});
	
	CasesBPMAssets.downloader = jQuery("#casesBPMAttachmentDownloader");
	CasesBPMAssets.downloader_link = jQuery(CasesBPMAssets.downloader).attr("href");
});

jQuery(window).load(function () {

    CasesBPMAssets.initTab(CasesBPMAssets.selectedTab);
});

if(CasesBPMAssets == null) var CasesBPMAssets = {};

CasesBPMAssets.downloader = null;
CasesBPMAssets.downloader_link = null;

CasesBPMAssets.initTab = function(tabIndex) {

    if(tabIndex == CasesBPMAssets.selectedTabIndexes.tasks)
        CasesBPMAssets.initTaskTab(".tasksTable");
    else if(tabIndex == CasesBPMAssets.selectedTabIndexes.documents) {
    
        CasesBPMAssets.initDocumentsTab("#documentsTable", BPMProcessAssets.getProcessDocumentsList, [Localization.DOCUMENT_NAME, Localization.DATE_SUBMITTED], [{name:'name',index:'name'}, {name:'submittedDate',index:'submittedDate'}]);
        CasesBPMAssets.initDocumentsTab("#emailsTable", BPMProcessAssets.getProcessEmailsList, [Localization.SUBJECT, Localization.FROM, Localization.RECIEVE_DATE], [{name:'subject',index:'subject'}, {name:'from',index:'from'}, {name:'submittedDate',index:'submittedDate'}]);
    }
};

CasesBPMAssets.tabIndexes = {
 
	tasks: 'tasksTab',
	documents: 'documentsTab'
};

CasesBPMAssets.selectedTabIndexes = {
    tasks: 0,
    documents: 1
};

CasesBPMAssets.selectedTab = CasesBPMAssets.selectedTabIndexes.tasks;

CasesBPMAssets.initTaskTab = function(tblId) {

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
    
    params.subGridRowExpanded = null;
    
    //params.colNames = ['Nr','Task name', 'Date created', 'Taken by', 'Status']; 
    params.colNames = [Localization.TASK_NAME, Localization.DATE_CREATED, Localization.TAKEN_BY, Localization.STATUS];
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
    var xxa = jQuery(tblId);
    grid.createGrid(tblId, params);
		
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

CasesBPMAssets.initDocumentsTab = function(tblId, retrievalFunction, colNames, colModel) {

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
    params.colModel = colModel;
    
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
        
        subGridParams.colNames = [Localization.FILE_NAME]; 
        subGridParams.colModel = [
                    {name:'name',index:'name'} 
        ];
        
        subGridParams.onSelectRow = function(fileRowId) {
   
              var newLink = CasesBPMAssets.downloader_link+"&taskInstanceId="+rowId+"&varHash="+fileRowId;
              window.location.href = newLink;
        };
s 
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