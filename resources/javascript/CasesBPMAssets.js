if(CasesBPMAssets == null) var CasesBPMAssets = {};

/*
CasesBPMAssets.downloader = null;
CasesBPMAssets.downloader_link = null;
*/


jQuery(document).ready(function() {
	
	
	
});

CasesBPMAssets.initGrid = function(container, piId, caseId) {
	
	var jQGridInclude = new JQGridInclude();
    jQGridInclude.SUBGRID = true;
    jqGridInclude(jQGridInclude);
    
    if (piId != null) {
    	
        BPMProcessAssets.hasUserRolesEditorRights(piId, {
	        callback: function(hasRightChangeRights) {
	        	
	            CasesBPMAssets.initTasksGrid(caseId, piId, container, false);
	            CasesBPMAssets.initFormsGrid(caseId, piId, container, hasRightChangeRights);
	            CasesBPMAssets.initEmailsGrid(caseId, piId, container, hasRightChangeRights);
	            CasesBPMAssets.initContactsGrid(piId, container, hasRightChangeRights);
	        }
        });    
    }
};

CasesBPMAssets.initTasksGrid = function(caseId, piId, customerView, hasRightChangeRights) {
	
	var identifier = 'caseTasks';
    
    var populatingFunction = function(params, callback) {
        params.piId = piId;
        
        BPMProcessAssets.getProcessTasksList(params, {
            callback: function(result) {
                callback(result);
                
                CasesBPMAssets.setStyleClassesForGridColumns(jQuery('div.' + identifier + 'Part'));
            }
        });
    };
    
    var namesForColumns = new Array();
    namesForColumns.push(CASE_GRID_STRING_TASK_NAME);
    namesForColumns.push(CASE_GRID_STRING_DATE);
    namesForColumns.push(CASE_GRID_STRING_TAKEN_BY);
    if (hasRightChangeRights) {
        namesForColumns.push(CASE_GRID_STRING_CHANGE_ACCESS_RIGHTS);
    }
    var modelForColumns = new Array();
    modelForColumns.push({name:'name',index:'name'});
    modelForColumns.push({name:'createdDate',index:'createdDate'});
    modelForColumns.push({name:'takenBy',index:'takenBy'});
    if (hasRightChangeRights) {
        modelForColumns.push({name:'rightsForTaskResources',index:'rightsForTaskResources'});
    }
    
    var onSelectRowFunction = function(rowId) {
        setBPMProcessForPreview(caseId, rowId);
    };
    
    CasesBPMAssets.initCaseGrid(piId, customerView, identifier, populatingFunction, null, namesForColumns, modelForColumns, onSelectRowFunction, hasRightChangeRights);
};

CasesBPMAssets.initFormsGrid = function(caseId, piId, customerView, hasRightChangeRights) {
    var identifier = 'caseForms';
    
    var populatingFunction = function(params, callback) {
        params.piId = piId;
        params.rightsChanger = hasRightChangeRights;
        BPMProcessAssets.getProcessDocumentsList(params, {
            callback: function(result) {
                callback(result);
                
                CasesBPMAssets.openAllAttachmentsForCase(jQuery('#' + params.identifier + GRID_WITH_SUBGRID_ID_PREFIX + piId));
                
                CasesBPMAssets.setStyleClassesForGridColumns(jQuery('div.' + identifier + 'Part'));
            }
        });
    };

    var subGridFunction = function(subgridId, rowId) {
        CasesBPMAssets.initFilesSubGridForCasesListGrid(subgridId, rowId, hasRightChangeRights, identifier);
    };
    
    var namesForColumns = new Array();
    namesForColumns.push(CASE_GRID_STRING_FORM_NAME);
    namesForColumns.push(CASE_GRID_STRING_SUBMITTED_BY);
    namesForColumns.push(CASE_GRID_STRING_DATE);
    namesForColumns.push(CASE_GRID_STRING_DOWNLOAD_DOCUMENT_AS_PDF);    //  TODO: check if need to download document in PDF
    if (hasRightChangeRights) {
        namesForColumns.push(CASE_GRID_STRING_CHANGE_ACCESS_RIGHTS);
    }
    var modelForColumns = new Array();
    modelForColumns.push({name:'name',index:'name'});
    modelForColumns.push({name:'submittedByName',index:'submittedByName'});
    modelForColumns.push({name:'submittedDate',index:'submittedDate'});
    modelForColumns.push({name:'downloadAsPdf',index:'downloadAsPdf'});
    if (hasRightChangeRights) {
        modelForColumns.push({name:'rightsForDocumentResources',index:'rightsForDocumentResources'});
    }
    
    var onSelectRowFunction = function(rowId) {
        setBPMProcessForPreview(caseId, rowId);
    };
    
    CasesBPMAssets.initCaseGrid(piId, customerView, identifier, populatingFunction, subGridFunction, namesForColumns, modelForColumns, onSelectRowFunction, hasRightChangeRights);
};

CasesBPMAssets.initEmailsGrid = function(caseId, piId, customerView, hasRightChangeRights) {
    var identifier = 'caseEmails';
    
    var populatingFunction = function(params, callback) {
        params.piId = piId;

        BPMProcessAssets.getProcessEmailsList(params, {
            callback: function(result) {
                callback(result);

                CasesBPMAssets.openAllAttachmentsForCase(jQuery('#' + params.identifier + GRID_WITH_SUBGRID_ID_PREFIX + piId));
                
                CasesBPMAssets.setStyleClassesForGridColumns(jQuery('div.' + identifier + 'Part'));
            }
        });
    };
    
    var subGridFunction = function(subgridId, rowId) {
        CasesBPMAssets.initFilesSubGridForCasesListGrid(subgridId, rowId, hasRightChangeRights, identifier);
    };
    
    var namesForColumns = new Array();
    namesForColumns.push(CASE_GRID_STRING_SUBJECT);
    namesForColumns.push(CASE_GRID_STRING_SENDER);
    namesForColumns.push(CASE_GRID_STRING_DATE);
    if (hasRightChangeRights) {
        namesForColumns.push(CASE_GRID_STRING_CHANGE_ACCESS_RIGHTS);
    }
    var modelForColumns = new Array();
    modelForColumns.push({name:'subject',index:'subject'});
    modelForColumns.push({name:'from',index:'from'});
    modelForColumns.push({name:'submittedDate',index:'submittedDate'});
    if (hasRightChangeRights) {
        modelForColumns.push({name:'rightsForEmailResources',index:'rightsForEmailResources'});
    }
    
    var onSelectRowFunction = function(rowId) {
        setBPMProcessForPreview(caseId, rowId);
    };
    
    CasesBPMAssets.initCaseGrid(piId, customerView, identifier, populatingFunction, subGridFunction, namesForColumns, modelForColumns, onSelectRowFunction, hasRightChangeRights);
};

CasesBPMAssets.initContactsGrid = function(piId, customerView, hasRightChangeRights) {
    var identifier = 'caseContacts';
    
    var populatingFunction = function(params, callback) {
        params.piId = piId;
        
        BPMProcessAssets.getProcessContactsList(params, {
            callback: function(result) {
                callback(result);
                
                CasesBPMAssets.setStyleClassesForGridColumns(jQuery('div.' + identifier + 'Part'));
            }
        });
    };
    
    var namesForColumns = new Array();
    namesForColumns.push(CASE_GRID_STRING_CONTACT_NAME);
    namesForColumns.push(CASE_GRID_STRING_EMAIL_ADDRESS);
    namesForColumns.push(CASE_GRID_STRING_PHONE_NUMBER);
    namesForColumns.push(CASE_GRID_STRING_ADDRESS);
    var modelForColumns = new Array();
    modelForColumns.push({name:'name',index:'name'});
    modelForColumns.push({name:'emailAddress',index:'emailAddress'});
    modelForColumns.push({name:'phoneNumber',index:'phoneNumber'});
    modelForColumns.push({name:'address',index:'address'});
    
    var onSelectRowFunction = function(rowId) {
    }
    
    CasesBPMAssets.initCaseGrid(piId, customerView, identifier, populatingFunction, null, namesForColumns, modelForColumns, onSelectRowFunction, hasRightChangeRights);
};

//TODO: rename to initGridBase
CasesBPMAssets.initCaseGrid = function(piId, customerView, tableClassName, populatingFunction, subGridForThisGrid, namesForColumns, modelForColumns, onSelectRowFunction, rightsChanger) {
    var params = new JQGridParams();
    
    params.identifier = tableClassName;
    params.rightsChanger = rightsChanger;
    
    params.populateFromFunction = populatingFunction;
    
    params.colNames = namesForColumns;
    params.colModel = modelForColumns;
    
    if (onSelectRowFunction != null) {
        params.onSelectRow = onSelectRowFunction;
    }
    
    var tables = getElementsByClassName(customerView, 'table', tableClassName);
    if (tables == null || tables.length == 0) {
        return false;
    }
    var table = tables[0];
    
    if (subGridForThisGrid == null) {
        params.subGridRowExpanded = null;
    }
    else {
        jQuery(table).attr('id', params.identifier + GRID_WITH_SUBGRID_ID_PREFIX + piId);
        params.subGrid = true;
        params.subGridRowExpanded = subGridForThisGrid;
    }
    
    var grid = new JQGrid();
    grid.createGrid(table, params);
    
    jQuery(table).addClass('scroll');
    jQuery(table).attr('cellpadding', 0);
    jQuery(table).attr('cellspacing', 0);
}

//TODO: rename
function setBPMProcessForPreview(caseId, taskInstanceId) {
    changeWindowLocationHref('prm_case_pk=' + caseId + '&tiId=' + taskInstanceId + '&cp_prm_action=8');
}

CasesBPMAssets.initFilesSubGridForCasesListGrid = function(subgridId, rowId, hasRightChangeRights, identifier) {
    var subgridTableId = subgridId + '_t';
    jQuery('#' + subgridId).html('<table id=\''+subgridTableId+'\' class=\'scroll subGrid\' cellpadding=\'0\' cellspacing=\'0\'></table>');

    var subGridParams = new JQGridParams();
    subGridParams.rightsChanger = hasRightChangeRights;
    subGridParams.identifier = identifier +'Attachments';
    subGridParams.populateFromFunction = function(params, callback) {
        params.taskId = rowId;
        BPMProcessAssets.getTaskAttachments(params, {
            callback: function(result) {
                callback(result);
                
                var subGridTable = jQuery('#' + subgridTableId);
                if (subGridTable == null || subGridTable.length == 0) {
                    return false;
                }
                
                CasesBPMAssets.setStyleClassesForGridColumns(subGridTable.parent().parent());
                
                if (result != null) {
                    return false;
                }
                
                var tagName = 'TR';
                var className = 'subgrid';
                var fileGridRow = null;
                var foundRow = false;
                var parentElement = subGridTable;
                var tempParentElement = null;
                while (parentElement != null && !foundRow) {
                    tempParentElement = parentElement.get(0);
                    if (tempParentElement.tagName == tagName && jQuery(tempParentElement).hasClass(className)) {
                        fileGridRow = tempParentElement;
                        foundRow = true;
                    }
                    parentElement = parentElement.parent();
                }
                if (fileGridRow != null) {
                    jQuery(fileGridRow).css('display', 'none');
                    
                    var mainRow = jQuery('#' + rowId);
                    if (mainRow == null || mainRow.length == 0) {
                        return false;
                    }
                    
                    var subGridOpener = jQuery('td.subGridOpener', mainRow);
                    if (subGridOpener == null || subGridOpener.length == 0) {
                        return false;
                    }
                    subGridOpener.empty();
                    subGridOpener.unbind('click');
                }
            }
        });
    };

    var namesForColumns = new Array();
    namesForColumns.push(CASE_GRID_STRING_FILE_DESCRIPTION);
    namesForColumns.push(CASE_GRID_STRING_FILE_NAME);
    namesForColumns.push(CASE_GRID_STRING_FILE_SIZE);
    if (subGridParams.rightsChanger) {
        namesForColumns.push(CASE_GRID_STRING_CHANGE_ACCESS_RIGHTS);
    }
    subGridParams.colNames = namesForColumns;
    
    var modelForColumns = new Array();
    modelForColumns.push({name:'description',index:'description'});
    modelForColumns.push({name:'name',index:'name'});
    modelForColumns.push({name:'fileSize',index:'fileSize'});
    if (subGridParams.rightsChanger) {
        modelForColumns.push({name:'rightsForAttachment',index:'rightsForAttachment'});
    }
    subGridParams.colModel = modelForColumns;
    
    subGridParams.onSelectRow = function(fileRowId) {
        var uri = '&taskInstanceId=' + rowId + '&varHash=' + fileRowId;
        CasesBPMAssets.setCurrentWindowToDownloadCaseResource(uri, CASE_ATTACHEMENT_LINK_STYLE_CLASS);
    };

    var subgrid = new JQGrid();
    subgrid.createGrid("#"+subgridTableId, subGridParams);
};

CasesBPMAssets.setStyleClassesForGridColumns = function(elements) {
    if (elements == null || elements.length == 0) {
        return false;
    }
    
    var attributeName = 'col_with_classes';
    var attribute = null;
    var element = null;
    var grids = null;
    var grid = null;
    var rows = null;
    var row = null;
    for (var i = 0; i < elements.length; i++) {
        element = jQuery(elements[i]);
        attribute = element.attr(attributeName);
        if (attribute == null || attribute == '') {
            element.attr(attributeName, 'true');
            
            grids = jQuery('table', element);
            if (grids != null && grids.length > 0) {
                for (var j = 0; j < grids.length; j++) {
                    grid = jQuery(grids[j]);
                    
                    rows = jQuery('tr', grid);
                    if (rows != null && rows.length > 0) {
                        for (var k = 0; k < rows.length; k++) {
                            row = jQuery(rows[k]);
                            
                            var headerCells = jQuery('th', row);
                            if (headerCells != null && headerCells.length > 0) {
                                for (var l = 0; l < headerCells.length; l++) {
                                    jQuery(headerCells[l]).addClass('casesGridHeaderCell_' + l);
                                }
                            }
                            
                            var bodyCells = jQuery('td', row);
                            if (bodyCells != null && bodyCells.length > 0) {
                                for (var l = 0; l < bodyCells.length; l++) {
                                    jQuery(bodyCells[l]).addClass('casesGridBodyCell_' + l);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
};

CasesBPMAssets.openAllAttachmentsForCase = function(table) {
    if (table == null) {
        return false;
    }

    var subGridsOpeners = jQuery('td.subGridOpener', table);
    if (subGridsOpeners == null || subGridsOpeners.length == 0) {
        return false;
    }
    
    var subGridOpener = null;
    var parameterValue = null;
    var openOnLoadPar = 'opened_on_load';
    for (var j = 0; j < subGridsOpeners.length; j++) {
        subGridOpener = jQuery(subGridsOpeners[j]);
        
        parameterValue = subGridOpener.attr(openOnLoadPar);
        if (parameterValue == null || parameterValue == '') {
            subGridOpener.attr(openOnLoadPar, 'true');
            subGridOpener.click();
        }
    }
};

CasesBPMAssets.setCurrentWindowToDownloadCaseResource = function(uri, styleClass) {
    var links = jQuery('a.' + styleClass);
    if (links == null || links.length == 0) {
        return false;
    }
    
    if (uri == null || uri == '') {
        return false;
    }
    
    var linkHref = jQuery(links[0]).attr('href');
    if (linkHref == null) {
        return false;
    }
    
    linkHref += uri;
    window.location.href = linkHref;
    return true;
};

//old
/*
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




CasesBPMAssets.initTab = function(tabIndex) {

    if(tabIndex == CasesBPMAssets.selectedTabIndexes.tasks)
        CasesBPMAssets.initTaskTab(".tasksTable");
    else if(tabIndex == CasesBPMAssets.selectedTabIndexes.documents) {
    
        CasesBPMAssets.initDocumentsTab("#documentsTable", BPMProcessAssets.getProcessDocumentsList, [Localization.DOCUMENT_NAME, Localization.SUBMITTED_BY, Localization.DATE_SUBMITTED], [{name:'name',index:'name'}, {name:'submittedByName',index:'submittedByName'}, {name:'submittedDate',index:'submittedDate'}]);
        CasesBPMAssets.initDocumentsTab("#emailsTable", BPMProcessAssets.getProcessEmailsList, [Localization.SUBJECT, Localization.FROM, Localization.RECEIVE_DATE], [{name:'subject',index:'subject'}, {name:'from',index:'from'}, {name:'submittedDate',index:'submittedDate'}]);
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
		
		
	//jQuery(jQuery(tabContainer).children('div')).each(
		//function(i) {
			//jQuery(this).css({width: "auto", height: "auto"});
		//}
	//);
	
	CasesBPMAssets.initTaskTab.inited = true;
};

CasesBPMAssets.initTaskTab.inited = false;

CasesBPMAssets.initDocumentsTab = function(tblId, retrievalFunction, colNames, colModel) {

	//if(CasesBPMAssets.initDocumentsTab.inited)
//		return;
		
    var params = new JQGridParams();
    
    params.populateFromFunction = function(params, callback) {
            
        params.piId = jQuery(CasesBPMAssets.exp_piId)[0].value;
        params.downloadDocument = false;
                                
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
		
	///jQuery(jQuery(tabContainer).children('div')).each(
		//function(i) {
			//jQuery(this).css({width: "auto", height: "auto"});
		//}
	//);
	
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
        
        subGridParams.colNames = [Localization.FILE_DESCRIPTION, Localization.FILE_NAME, Localization.FILE_SIZE]; 
        subGridParams.colModel = [
                    {name:'description',index:'description'},
                    {name:'name',index:'name'},
                    {name:'fileSize',index:'fileSize'}
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
 */