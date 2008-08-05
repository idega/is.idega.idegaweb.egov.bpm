if(CasesBPMAssets == null) var CasesBPMAssets = {};
if(CasesBPMAssets.Loc == null) CasesBPMAssets.Loc = {
	
	inited: false,
	
    CASE_GRID_STRING_CONTACT_NAME: 'Name',
    CASE_GRID_STRING_TASK_NAME: 'Task name',
    CASE_GRID_STRING_FORM_NAME: 'Document name',
    CASE_GRID_STRING_SENDER: 'Sender',
    CASE_GRID_STRING_DATE: 'Date',
    CASE_GRID_STRING_TAKEN_BY: 'Taken by',
    CASE_GRID_STRING_EMAIL_ADDRESS: 'E-mail address',
    CASE_GRID_STRING_PHONE_NUMBER: 'Phone number',
    CASE_GRID_STRING_ADDRESS: 'Address',
    CASE_GRID_STRING_SUBJECT: 'Subject',
    CASE_GRID_STRING_FILE_DESCRIPTION: 'Descriptive name',
    CASE_GRID_STRING_FILE_NAME: 'File name',
    CASE_GRID_STRING_CHANGE_ACCESS_RIGHTS: 'Change access rights',
    CASE_GRID_STRING_DOWNLOAD_DOCUMENT_AS_PDF: 'Download document',
    CASE_GRID_STRING_FILE_SIZE: 'File size',
    CASE_GRID_STRING_SUBMITTED_BY: 'Submitted by'
};

CasesBPMAssets.GRID_WITH_SUBGRID_ID_PREFIX = '_tableForProcessInstanceGrid_';

CasesBPMAssets.CASE_ATTACHEMENT_LINK_STYLE_CLASS = 'casesBPMAttachmentDownloader';
CasesBPMAssets.CASE_PDF_DOWNLOADER_LINK_STYLE_CLASS = 'casesBPMPDFGeneratorAndDownloader';

CasesBPMAssets.initGrid = function(container, piId, caseId, usePdfDownloadColumn) {
	if (container == null) {
		return false;
	}
	
	if ('true' == jQuery(container).attr('inited')) {
		return false;
	}
	
	jQuery(container).attr('inited', 'true');
	
	var jQGridInclude = new JQGridInclude();
    jQGridInclude.SUBGRID = true;
    jqGridInclude(jQGridInclude, function() {
    	if (piId == null || piId == '') {
    		return false;
    	}

	    var caseWatcherSpan = jQuery('span.watchUnwatchCase', container);
	    if (caseWatcherSpan != null && caseWatcherSpan.length > 0) {
	    	var attributeName = 'processinstanceid';
	    	caseWatcherSpan.attr(attributeName, piId);
	    	caseWatcherSpan.click(function() {
	    		var watcher = jQuery(this);
	    		var processInstanceId = watcher.attr(attributeName);
	    		
	    		CasesBPMAssets.setWatchOrUnwatchTask(watcher, processInstanceId);
	    	});
	   	}
	   	
	   	CasesBPMAssets.initTakeCaseSelector(container, piId);
		
		BPMProcessAssets.hasUserRolesEditorRights(piId, {
			callback: function(hasRightChangeRights) {
				CasesBPMAssets.initTasksGrid(caseId, piId, container, false);
				CasesBPMAssets.initFormsGrid(caseId, piId, container, hasRightChangeRights, usePdfDownloadColumn);
				CasesBPMAssets.initEmailsGrid(caseId, piId, container, hasRightChangeRights);
				CasesBPMAssets.initContactsGrid(piId, container, hasRightChangeRights);
			}
		});
	});
};

CasesBPMAssets.initTakeCaseSelector = function(container, piId) {
	
	var takeCaseSelect = jQuery('.takeCaseSelect', container);
	
	BPMProcessAssets.getAllHandlerUsers(piId, {
        callback: function(handlerUsers) {
        	
        	if(handlerUsers != null && handlerUsers.length != 0) {
        		
        		var selectId = takeCaseSelect.attr("id");
            
	            if(selectId == null) {
	                
	                var date = new Date();
	                selectId = 'takeCase_' + date.getTime();
	                takeCaseSelect.attr("id", selectId);
	            }
	            
	            DWRUtil.addOptions(selectId, handlerUsers, 'id', 'value');
	            
	            var selected = IWCORE.getSelectedFromAdvancedProperties(handlerUsers);
	            
	            if(selected != null) {
	                
	                takeCaseSelect.val(selected);
	            }
	            
	            takeCaseSelect.css("display", "inline");
	            
	            if (takeCaseSelect != null && takeCaseSelect.length > 0) {
	                
	                var attributeName = 'processinstanceid';
	                takeCaseSelect.attr(attributeName, piId);
	                takeCaseSelect.change(function() {
	                    var watcher = jQuery(this);
	                    var processInstanceId = watcher.attr(attributeName);
	                    var handlerId = watcher.val();
	                    
	                    CasesBPMAssets.assignCase(handlerId, processInstanceId);
	                });
	            }
        	}
        }
    });
}

CasesBPMAssets.initTasksGrid = function(caseId, piId, customerView, hasRightChangeRights) {
	
	var identifier = 'caseTasks';
    
    var populatingFunction = function(params, callback) {
        params.piId = piId;
        
        BPMProcessAssets.getProcessTasksList(params, {
            callback: function(result) {
                callback(result);
                
                CasesBPMAssets.setStyleClassesForGridColumns(jQuery('div.' + identifier + 'Part'));
                
                CasesBPMAssets.hideHeaderTableIfNoContent(jQuery('div.' + identifier + 'Part', jQuery(customerView)));
            }
        });
    };
    
    var namesForColumns = new Array();
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_TASK_NAME);
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_DATE);
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_TAKEN_BY);
    if (hasRightChangeRights) {
        namesForColumns.push(''/*CasesBPMAssets.Loc.CASE_GRID_STRING_CHANGE_ACCESS_RIGHTS*/);
    }
    
    var modelForColumns = new Array();
    modelForColumns.push({name:'name',index:'name'});
    modelForColumns.push({name:'createdDate',index:'createdDate'});
    modelForColumns.push({name:'takenBy',index:'takenBy'});
    if (hasRightChangeRights) {
        modelForColumns.push({name:'rightsForTaskResources',index:'rightsForTaskResources'});
    }
    
    var onSelectRowFunction = function(rowId) {
        CasesBPMAssets.getProcessRersourceView(caseId, rowId);
    };
    
    CasesBPMAssets.initGridBase(piId, customerView, identifier, populatingFunction, null, namesForColumns, modelForColumns, onSelectRowFunction, hasRightChangeRights);
};

CasesBPMAssets.initFormsGrid = function(caseId, piId, customerView, hasRightChangeRights, usePdfDownloadColumn) {
    var identifier = 'caseForms';
    
    var populatingFunction = function(params, callback) {
        params.piId = piId;
        params.rightsChanger = hasRightChangeRights;
        params.downloadDocument = usePdfDownloadColumn;
        BPMProcessAssets.getProcessDocumentsList(params, {
            callback: function(result) {
                callback(result);
                
                CasesBPMAssets.openAllAttachmentsForCase(jQuery('#' + params.identifier + CasesBPMAssets.GRID_WITH_SUBGRID_ID_PREFIX + piId));
                
                CasesBPMAssets.setStyleClassesForGridColumns(jQuery('div.' + identifier + 'Part'));
                
                CasesBPMAssets.hideHeaderTableIfNoContent(jQuery('div.' + identifier + 'Part', jQuery(customerView)));
            }
        });
    };

    var subGridFunction = function(subgridId, rowId) {
        CasesBPMAssets.initFilesSubGridForCasesListGrid(subgridId, rowId, hasRightChangeRights, identifier);
    };
    
    var namesForColumns = new Array();
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_FORM_NAME);
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_SUBMITTED_BY);
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_DATE);
    if (usePdfDownloadColumn) {
    	namesForColumns.push(''/*CasesBPMAssets.Loc.CASE_GRID_STRING_DOWNLOAD_DOCUMENT_AS_PDF*/);
    }
    if (hasRightChangeRights) {
        namesForColumns.push(''/*CasesBPMAssets.Loc.CASE_GRID_STRING_CHANGE_ACCESS_RIGHTS*/);
    }
    var modelForColumns = new Array();
    modelForColumns.push({name:'name',index:'name'});
    modelForColumns.push({name:'submittedByName',index:'submittedByName'});
    modelForColumns.push({name:'submittedDate',index:'submittedDate'});
    if (usePdfDownloadColumn) {
    	modelForColumns.push({name:'downloadAsPdf',index:'downloadAsPdf'});
    }
    if (hasRightChangeRights) {
        modelForColumns.push({name:'rightsForDocumentResources',index:'rightsForDocumentResources'});
    }
    
    var onSelectRowFunction = function(rowId) {
        CasesBPMAssets.getProcessRersourceView(caseId, rowId);
    };
    
    CasesBPMAssets.initGridBase(piId, customerView, identifier, populatingFunction, subGridFunction, namesForColumns, modelForColumns, onSelectRowFunction, hasRightChangeRights);
};

CasesBPMAssets.initEmailsGrid = function(caseId, piId, customerView, hasRightChangeRights) {
    var identifier = 'caseEmails';
    
    var populatingFunction = function(params, callback) {
        params.piId = piId;

        BPMProcessAssets.getProcessEmailsList(params, {
            callback: function(result) {
                callback(result);

                CasesBPMAssets.openAllAttachmentsForCase(jQuery('#' + params.identifier + CasesBPMAssets.GRID_WITH_SUBGRID_ID_PREFIX + piId));
                
                CasesBPMAssets.setStyleClassesForGridColumns(jQuery('div.' + identifier + 'Part'));
                
                CasesBPMAssets.hideHeaderTableIfNoContent(jQuery('div.' + identifier + 'Part', jQuery(customerView)));
            }
        });
    };
    
    var subGridFunction = function(subgridId, rowId) {
        CasesBPMAssets.initFilesSubGridForCasesListGrid(subgridId, rowId, hasRightChangeRights, identifier);
    };
    
    var namesForColumns = new Array();
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_SUBJECT);
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_SENDER);
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_DATE);
    if (hasRightChangeRights) {
        namesForColumns.push(''/*CasesBPMAssets.Loc.CASE_GRID_STRING_CHANGE_ACCESS_RIGHTS*/);
    }
    var modelForColumns = new Array();
    modelForColumns.push({name:'subject',index:'subject'});
    modelForColumns.push({name:'from',index:'from'});
    modelForColumns.push({name:'submittedDate',index:'submittedDate'});
    if (hasRightChangeRights) {
        modelForColumns.push({name:'rightsForEmailResources',index:'rightsForEmailResources'});
    }
    
    var onSelectRowFunction = function(rowId) {
        CasesBPMAssets.getProcessRersourceView(caseId, rowId);
    };
    
    CasesBPMAssets.initGridBase(piId, customerView, identifier, populatingFunction, subGridFunction, namesForColumns, modelForColumns, onSelectRowFunction, hasRightChangeRights);
};

CasesBPMAssets.initContactsGrid = function(piId, customerView, hasRightChangeRights) {
    var identifier = 'caseContacts';
    
    var populatingFunction = function(params, callback) {
        params.piId = piId;
        
        BPMProcessAssets.getProcessContactsList(params, {
            callback: function(result) {
                callback(result);
                
                CasesBPMAssets.setStyleClassesForGridColumns(jQuery('div.' + identifier + 'Part'));
                
                CasesBPMAssets.hideHeaderTableIfNoContent(jQuery('div.' + identifier + 'Part', jQuery(customerView)));
            }
        });
    };
    
    var namesForColumns = new Array();
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_CONTACT_NAME);
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_EMAIL_ADDRESS);
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_PHONE_NUMBER);
    //namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_ADDRESS);
    var modelForColumns = new Array();
    modelForColumns.push({name:'name',index:'name'});
    modelForColumns.push({name:'emailAddress',index:'emailAddress'});
    modelForColumns.push({name:'phoneNumber',index:'phoneNumber'});
   	//modelForColumns.push({name:'address',index:'address'});
    
    var onSelectRowFunction = function(rowId) {
    }
    
    CasesBPMAssets.initGridBase(piId, customerView, identifier, populatingFunction, null, namesForColumns, modelForColumns, onSelectRowFunction, hasRightChangeRights);
};

CasesBPMAssets.initGridBase = function(piId, customerView, tableClassName, populatingFunction, subGridForThisGrid, namesForColumns, modelForColumns, onSelectRowFunction, rightsChanger) {
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
        jQuery(table).attr('id', params.identifier + CasesBPMAssets.GRID_WITH_SUBGRID_ID_PREFIX + piId);
        params.subGrid = true;
        params.subGridRowExpanded = subGridForThisGrid;
    }
    
    var grid = new JQGrid();
    grid.createGrid(table, params);
    
    jQuery(table).addClass('scroll');
    jQuery(table).attr('cellpadding', 0);
    jQuery(table).attr('cellspacing', 0);
}

CasesBPMAssets.hideHeaderTableIfNoContent = function(container) {
	container = jQuery(container);
	
	var hideHeader = false;
	var changeBody = false;
	var textFromRows = null;
	var rowsWithIllegalIds = jQuery('#-1', container);
	if (rowsWithIllegalIds != null && rowsWithIllegalIds.length > 0) {
		hideHeader = true;
		changeBody = true;
		textFromRows = new Array();
		
		var row = null;
		var text = null;
		var cells = null;
		for (var i = 0; i < rowsWithIllegalIds.length; i++) {
			row = jQuery(rowsWithIllegalIds[i]);
			cells = jQuery('td', row);
			
			for (var j = 0; j < cells.length; j++) {
				text = jQuery(cells[j]).text();
				if (text != null && text != '' && text != ' ') {
					textFromRows.push(text);
				}
			}
		}
	}
	
	var divsWithNoContent = jQuery("div[records='0']", container);
	if (divsWithNoContent != null && divsWithNoContent.length > 0) {
		hideHeader = true;
		changeBody = true;
		textFromRows = ['-'];
	}
	
	if (hideHeader) {
		var headers = jQuery('div.gridHeadersTableContainer', container);
		for (var i = 0; i < headers.length; i++) {
			jQuery(headers[i]).css('display', 'none');
		}
	}
	if (changeBody) {
		var bodies = jQuery('div.gridBodyTableContainer', container);
		var gridBody = null;
		for (var i = 0; i < bodies.length; i++) {
			gridBody =  jQuery(bodies[i]);
			
			gridBody.empty();
			gridBody.addClass('noContentInCasesListGridBody');
			if (textFromRows != null) {
				var allText = '';
				for (var j = 0; j < textFromRows.length; j++) {
					allText += textFromRows[j];
					if ((j + 1) < textFromRows.length) {
						allText += ' ';
					}
				}
				if (allText == '') {
					allText = '-';
				}
				gridBody.text(allText);
			}
		}
	}
}

CasesBPMAssets.getProcessRersourceView = function(caseId, taskInstanceId) {
	if (!caseId || !taskInstanceId) {
		return false;
	}
	if (caseId < 0 || taskInstanceId < 0) {
		return false;
	}
	
    changeWindowLocationHref('prm_case_pk=' + caseId + '&tiId=' + taskInstanceId + '&cp_prm_action=8');
}

CasesBPMAssets.initFilesSubGridForCasesListGrid = function(subgridId, rowId, hasRightChangeRights, identifier) {
    var subgridTableId = subgridId + '_t';
    var subGridContainer = jQuery('#' + subgridId);
	subGridContainer.html('<table id=\''+subgridTableId+'\' class=\'scroll subGrid\' cellpadding=\'0\' cellspacing=\'0\'></table>');

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
                
                var tableHeadersContainers = jQuery('div.gridHeadersTableContainer', subGridContainer);
                if (tableHeadersContainers != null && tableHeadersContainers.length > 0) {
                	for (var i = 0; i < tableHeadersContainers.length; i++) {
                		jQuery(tableHeadersContainers[i]).css('display', 'none');
                	}
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
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_FILE_DESCRIPTION);
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_FILE_NAME);
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_FILE_SIZE);
    if (subGridParams.rightsChanger) {
        namesForColumns.push(''/*CasesBPMAssets.Loc.CASE_GRID_STRING_CHANGE_ACCESS_RIGHTS*/);
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
        CasesBPMAssets.setCurrentWindowToDownloadCaseResource(uri, CasesBPMAssets.CASE_ATTACHEMENT_LINK_STYLE_CLASS);
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

CasesBPMAssets.downloadCaseDocument = function(event, taskId) {
	var uri = '&taskInstanceId=' + taskId;
	CasesBPMAssets.setCurrentWindowToDownloadCaseResource(uri, CasesBPMAssets.CASE_PDF_DOWNLOADER_LINK_STYLE_CLASS);
	
	if (event) {
		if (event.stopPropagation) {
			event.stopPropagation();
		}
		event.cancelBubble = true;
	}
}

CasesBPMAssets.takeCurrentProcessTask = function(event, taskInstanceId, id, allowReAssign) {
	if (event) {
		if (event.stopPropagation) {
			event.stopPropagation();
		}
		event.cancelBubble = true;
	}
	
	BPMProcessAssets.takeBPMProcessTask(taskInstanceId, allowReAssign, {
		callback: function(takenByValue) {
			if (takenByValue == null) {
				return false;
			}
		
			jQuery('#' + id).parent().empty().text(takenByValue);
		}
	});
}

CasesBPMAssets.changeAccessRightsForBpmRelatedResource = function(event, processId, taskId, id, fileHashValue, setSameRightsForAttachments) {	
	var element = jQuery('#' + id);
	if (element == null || event == null) {
		return false;
	}
	
	var offsets = element.offset();
	if (offsets == null) {
		return false;
	}
	
	var xCoord = offsets.left;
	var yCoord = offsets.top;
	
	if (event) {
		if (event.stopPropagation) {
			event.stopPropagation();
		}
		event.cancelBubble = true;
	}
	
	var rightsBoxId = 'caseProcessResourceAccessRightsSetterBox';
	var rightsBox = jQuery('#' + rightsBoxId);
	if (rightsBox == null || rightsBox.length == 0) {
		var htmlForBox = '<div id=\''+rightsBoxId+'\' class=\'caseProcessResourceAccessRightsSetterStyle\' />';
		jQuery(document.body).append(htmlForBox);
		rightsBox = jQuery('#' + rightsBoxId);
	}
	else {
		rightsBox.empty();
	}
	rightsBox.css('top', yCoord + 'px');
	rightsBox.css('left', xCoord + 'px');
	
	BPMProcessAssets.getAccessRightsSetterBox(processId, taskId, fileHashValue, setSameRightsForAttachments, {
		callback: function(component) {
			if (component == null) {
				return false;
			}
			
			insertNodesToContainer(component, rightsBox[0]);
			jQuery(rightsBox).show('fast');
		}
	});
}

CasesBPMAssets.setAccessRightsForBpmRelatedResource = function(id, processId, taskInstanceId, fileHashValue, sameRightsSetterId) {
	var element = document.getElementById(id);
	if (element == null) {
		return false;
	}
	
	var canAccess = element.checked;
	var setSameRightsForAttachments = false;
	if (sameRightsSetterId != null) {
		var sameRightsSetter = document.getElementById(sameRightsSetterId);
		if (sameRightsSetter != null) {
			setSameRightsForAttachments = true;
		}
	}
	
	BPMProcessAssets.setAccessRightsForProcessResource(element.name, taskInstanceId, fileHashValue, canAccess, setSameRightsForAttachments, {
		callback: function(message) {
			if (message == null){
				return false;
			}else {
				if (setSameRightsForAttachments) 
					humanMsg.displayMsg(message);	
			}
		}
	});
}

CasesBPMAssets.closeAccessRightsSetterBox = function() {
	var rightsBoxId = 'caseProcessResourceAccessRightsSetterBox';
	var rightsBox = jQuery('#' + rightsBoxId);
	if (rightsBox == null || rightsBox.length == 0) {
		return false;
	}
	
	rightsBox.hide('fast');
}

CasesBPMAssets.setWatchOrUnwatchTask = function(element, processInstanceId) {
	showLoadingMessage('');
	BPMProcessAssets.watchOrUnwatchBPMProcessTask(processInstanceId, {
		callback: function(message) {
			closeAllLoadingMessages();
			
			if (message == null) {
				return false;
			}
			
			jQuery(element).text(message);
		}
	});
}

CasesBPMAssets.assignCase = function(handlerId, processInstanceId) {
	
	showLoadingMessage('');
    BPMProcessAssets.assignCase(handlerId, processInstanceId, {
        callback: function(message) {
            closeAllLoadingMessages();
            
            if (message == null) {
                return false;
            }
        }
    });
}