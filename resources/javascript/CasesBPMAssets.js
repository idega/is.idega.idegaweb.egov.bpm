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
    CASE_GRID_STRING_SUBMITTED_BY: 'Submitted by',
    CASE_GRID_STRING_GENERATING_PDF: 'Downloading PDF',
    CASE_GRID_STRING_LOADING: 'Loading...',
    CASE_GRID_STRING_ARE_YOU_SURE: 'Are you sure?'
};

CasesBPMAssets.processParams = {};

CasesBPMAssets.GRID_WITH_SUBGRID_ID_PREFIX = '_tableForProcessInstanceGrid_';

CasesBPMAssets.CASE_ATTACHEMENT_LINK_STYLE_CLASS = 'casesBPMAttachmentDownloader';
CasesBPMAssets.CASE_PDF_DOWNLOADER_LINK_STYLE_CLASS = 'casesBPMPDFGeneratorAndDownloader';
CasesBPMAssets.DOWNLOAD_TASK_IN_PDF_LINK_STYLE_CLASS = 'casesBPMDownloadTaskInPDF';

CasesBPMAssets.rowInAction = null;

CasesBPMAssets.openedCase = {
	caseId: null,
	piId: null,
	container: null,
	
	hasRightChangeRights: false,
	usePdfDownloadColumn: false,
	allowPDFSigning: false,
	showAttachmentStatistics: false,
	showOnlyCreatorInContacts: false,
	showLogExportButton: false,
	specialBackPage: null,
	nameFromExternalEntity: false
}

CasesBPMAssets.setOpenedCase = function(caseId, piId, container, 
		hasRightChangeRights, usePdfDownloadColumn, allowPDFSigning, 
		hideEmptySection, showAttachmentStatistics, showOnlyCreatorInContacts, 
		showLogExportButton, showComments, showContacts, specialBackPage, 
		nameFromExternalEntity) {
	
	CasesBPMAssets.openedCase.caseId = caseId || null;
	CasesBPMAssets.openedCase.piId = piId || null;
	CasesBPMAssets.openedCase.container = container || null;
	
	CasesBPMAssets.openedCase.hasRightChangeRights = hasRightChangeRights || false;
	CasesBPMAssets.openedCase.usePdfDownloadColumn = usePdfDownloadColumn || false;
	CasesBPMAssets.openedCase.allowPDFSigning = allowPDFSigning || false;
	CasesBPMAssets.openedCase.hideEmptySection = hideEmptySection || false;
	CasesBPMAssets.openedCase.showAttachmentStatistics = showAttachmentStatistics || false;
	CasesBPMAssets.openedCase.showOnlyCreatorInContacts = showOnlyCreatorInContacts || false;
	CasesBPMAssets.openedCase.showLogExportButton = showLogExportButton || false;
	CasesBPMAssets.openedCase.showComments = showComments || false;
	CasesBPMAssets.openedCase.showContacts = showContacts || false;
	CasesBPMAssets.openedCase.nameFromExternalEntity = nameFromExternalEntity || false;
	CasesBPMAssets.specialBackPage = specialBackPage || null;
}

CasesBPMAssets.initGridsContainer = function(container, piId, caseId, 
		usePdfDownloadColumn, allowPDFSigning, hideEmptySection,
		showAttachmentStatistics, showOnlyCreatorInContacts, showLogExportButton, 
		showComments, showContacts, specialBackPage, nameFromExternalEntity) {
	
	CasesBPMAssets.openedCase.nameFromExternalEntity = nameFromExternalEntity;
	jQuery(container).mouseover(function() {
		if (CasesBPMAssets.openedCase.caseId == caseId) {
			return;
		}
		
		CasesBPMAssets.setOpenedCase(caseId, piId, container, null, 
				usePdfDownloadColumn, allowPDFSigning, hideEmptySection, 
				showAttachmentStatistics, showOnlyCreatorInContacts, 
				showLogExportButton, showComments, showContacts, 
				specialBackPage, nameFromExternalEntity);
	});
}

CasesBPMAssets.initGrid = function(container, piId, caseId, 
		usePdfDownloadColumn, allowPDFSigning, hideEmptySection, 
		showAttachmentStatistics, showOnlyCreatorInContacts, showLogExportButton, 
		showComments, showContacts, specialBackPage, nameFromExternalEntity) {
	
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
		CasesBPMAssets.initGridsContainer(container, piId, caseId, 
				usePdfDownloadColumn, allowPDFSigning, hideEmptySection, 
				showAttachmentStatistics, showOnlyCreatorInContacts,
				showLogExportButton, showComments, showContacts, specialBackPage, 
				nameFromExternalEntity);
		
		jQuery('img.reloadCaseStyleClass', container).each(function() {
			if (typeof CasesEngine == 'undefined' || typeof CasesListHelper == 'undefined') {
				jQuery(this).css('display', 'none');
			} else {
				jQuery(this).click(function() {
					CasesBPMAssets.reloadCaseView(this, container, piId);
				});
			}
		});
		
		jQuery('img.caseLogsAsPDFStyleClass', container).each(function() {
			jQuery(this).click(function(){
				CasesBPMAssets.showHumanizedMessage(CasesBPMAssets.Loc.CASE_GRID_STRING_GENERATING_PDF, {
					timeout: 3000
				});
				var uri = '&caseLogIdToDownload=' + caseId;
				CasesBPMAssets.setCurrentWindowToDownloadCaseResource(uri, 'caselogspdfdownload');
			});
		});
		
		BPMProcessAssets.hasUserRolesEditorRights(piId, {
			callback: function(hasRightChangeRights) {
				var onGridInitedFunction = function(identifier, needToShow) {
					CasesBPMAssets.setTableProperties(container);
					
					if (needToShow) {
						jQuery('div.' + identifier, container).show('slow');
						
						if (!jQuery('div.commentsViewerForTaskViewerInCasesList', container).hasClass('caseListTasksSectionVisibleStyleClass')) {
							jQuery('div.commentsViewerForTaskViewerInCasesList', container).addClass('caseListTasksSectionVisibleStyleClass').show('slow');
						}
						if (!jQuery('div.sendCaseEmailStyleInBPMCaseViewer', container).hasClass('caseListTasksSectionVisibleStyleClass')) {
							jQuery('div.sendCaseEmailStyleInBPMCaseViewer', container).addClass('caseListTasksSectionVisibleStyleClass').show('slow');
						}
					}
					
					CasesBPMAssets.manageLoadingMessage();
				}
				
				CasesBPMAssets.initTasksGrid(caseId, piId, container, false, hideEmptySection,
					function() {
						onGridInitedFunction('caseTasksPart', jQuery('div.caseTasksPart', container).hasClass('caseListTasksSectionVisibleStyleClass'));
					}
				);
				CasesBPMAssets.initFormsGrid(caseId, piId, container, hasRightChangeRights, usePdfDownloadColumn, allowPDFSigning, hideEmptySection,
					showAttachmentStatistics, specialBackPage,
					function() {
						onGridInitedFunction('caseFormsPart', jQuery('div.caseFormsPart', container).hasClass('caseListTasksSectionVisibleStyleClass'));
					}
				);
				CasesBPMAssets.initEmailsGrid(caseId, piId, container, hasRightChangeRights, allowPDFSigning, hideEmptySection, showAttachmentStatistics,
					function() {
						onGridInitedFunction('caseEmailsPart', jQuery('div.caseEmailsPart', container).hasClass('caseListTasksSectionVisibleStyleClass'));
					}
				);
				CasesBPMAssets.initContactsGrid(piId, container, 
						hasRightChangeRights, hideEmptySection, 
						showOnlyCreatorInContacts, showContacts,
						function() {
							onGridInitedFunction('caseContactsPart', jQuery('div.caseContactsPart', container).hasClass('caseListTasksSectionVisibleStyleClass'));
						}, nameFromExternalEntity
				);
				
				CasesBPMAssets.setOpenedCase(caseId, piId, container, hasRightChangeRights, usePdfDownloadColumn, allowPDFSigning, hideEmptySection,
											showAttachmentStatistics, showOnlyCreatorInContacts, showLogExportButton, showComments, showContacts,
											specialBackPage, nameFromExternalEntity);
			}
		});
	});
};

CasesBPMAssets.CASE_VIEW_PARTS_TO_INIT = 4;
CasesBPMAssets.manageLoadingMessage = function() {
	CasesBPMAssets.CASE_VIEW_PARTS_TO_INIT--;
	if (CasesBPMAssets.CASE_VIEW_PARTS_TO_INIT == 0) {
		CasesBPMAssets.CASE_VIEW_PARTS_TO_INIT = 4;
		closeAllLoadingMessages();
	}
}

CasesBPMAssets.reloadCaseView = function(controller, container, piId) {
	if (jQuery(controller).hasClass('caseViewReloadInProgress')) {
		return false;
	} else {
		jQuery(controller).addClass('caseViewReloadInProgress')
	}
	
	var viewContainer = jQuery(container).parent();
	var customerViewId = viewContainer.attr('id');
	if (customerViewId == null) {
		return false;
	}
	
	var caseOpeners = jQuery('div[customerviewid=\'' + customerViewId + '\']');
	if (caseOpeners == null || caseOpeners.length == 0) {
		return false;
	}
	
	viewContainer.hide('normal', function() {
		jQuery(container).remove();

		for (var i = 0; i < caseOpeners.length; i++) {
			jQuery(caseOpeners[i]).removeClass('expandedWithNoImage');
			jQuery(caseOpeners[i]).removeClass('expanded');
		}
		
		viewContainer.removeClass('caseWithInfo');
		jQuery(caseOpeners[0]).click();
	});
}

CasesBPMAssets.initTakeCaseSelector = function(container, piId) {
	var takeCaseSelect = jQuery('.takeCaseSelect', container);
	
	BPMProcessAssets.getAllHandlerUsers(piId, {
        callback: function(handlerUsers) {
        	if (handlerUsers == null || handlerUsers.length == 0) {
        		return;
        	}
        		
        	var selectId = takeCaseSelect.attr("id");
	        if (selectId == null || selectId.length == 0) {
	            var date = new Date();
	            selectId = 'takeCase_' + date.getTime();
                takeCaseSelect.attr("id", selectId);
	        }
	            
	        dwr.util.addOptions(selectId, handlerUsers, 'id', 'value');
	        
	        var selected = IWCORE.getSelectedFromAdvancedProperties(handlerUsers);
	        if (selected != null) {
	            takeCaseSelect.val(selected);
	        }
	        
	        takeCaseSelect.css("display", "inline");
	        jQuery.each(jQuery('.selectCaseHandlerLabelStyle', container), function() {
	        	jQuery(this).css("display", "inline");
	        });
	        
	        if (takeCaseSelect != null && takeCaseSelect.length > 0) {
	        	var attributeName = 'processinstanceid';
	            takeCaseSelect.attr(attributeName, piId);
	            takeCaseSelect.change(function() {
	            	var watcher = jQuery(this);
	                var processInstanceId = watcher.attr(attributeName);
	                var handlerId = watcher.val();
	                  
	                CasesBPMAssets.assignCase(handlerId, processInstanceId, container);
	            });
	        }
       	}
    });
}

CasesBPMAssets.initTasksGrid = function(caseId, piId, customerView, hasRightChangeRights, hideEmptySection, onTasksInited) {
	
	var identifier = 'caseTasks';
    
    var populatingFunction = function(params, callback) {
        params.piId = piId;
        
        BPMProcessAssets.getProcessTasksList(params, {
            callback: function(result) {
                callback(result);
                
                CasesBPMAssets.setStyleClassesForGridColumns(jQuery('div.' + identifier + 'Part'));
                
                CasesBPMAssets.hideHeaderTableIfNoContent(jQuery('div.' + identifier + 'Part', jQuery(customerView)), hideEmptySection);
                
                if (onTasksInited) {
                	onTasksInited();
                }
            }
        });
    };
    
    var namesForColumns = new Array();
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_TASK_NAME);
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_DATE);
    //			TODO commented for future use. 'Taken by' column label isn't shown now
    //namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_TAKEN_BY);
    if (hasRightChangeRights) {
        namesForColumns.push(''/*CasesBPMAssets.Loc.CASE_GRID_STRING_CHANGE_ACCESS_RIGHTS*/);
    }
    
    var modelForColumns = new Array();
    modelForColumns.push({name:'name',index:'name'});
    modelForColumns.push({name:'createdDate',index:'createdDate'});
    //			TODO commented for future use. 'Taken by' column label isn't shown now
    //modelForColumns.push({name:'takenBy',index:'takenBy'});
    if (hasRightChangeRights) {
        modelForColumns.push({name:'rightsForTaskResources',index:'rightsForTaskResources'});
    }
    
    var onSelectRowFunction = function(rowId) {
    	if (CasesBPMAssets.rowInAction == rowId) {
    		return false;
    	}
    	
    	CasesBPMAssets.rowInAction = rowId;
    	showLoadingMessage(CasesBPMAssets.Loc.CASE_GRID_STRING_LOADING);
        CasesBPMAssets.getProcessRersourceView(caseId, rowId);
    };
    
    CasesBPMAssets.initGridBase(piId, customerView, identifier, populatingFunction, null, namesForColumns, modelForColumns, onSelectRowFunction,
    							hasRightChangeRights, null);
};

CasesBPMAssets.pushRowsParams = function(gridEntriesBean) {
	if (gridEntriesBean == null) {
		return;
	}
	
	var params = {
		rowsParams: gridEntriesBean.rowsParams
	};
	CasesBPMAssets.processParams[gridEntriesBean.processInstanceId] = params;
};

CasesBPMAssets.getRowParam = function(processInstanceId, rowId, paramId) {

	var params = CasesBPMAssets.processParams[processInstanceId];
	
	var paramValue = null;
	
	if(params != null && params.rowsParams != null) {
		
		var rowParams = params.rowsParams[rowId];
		
		if(rowParams != null) {
		
            paramValue = rowParams[paramId];	
		}
	}
	
	return paramValue;
};

CasesBPMAssets.isRowHasViewUI = function(processInstanceId, rowId) {
	
	var hasViewUI = CasesBPMAssets.getRowParam(processInstanceId, rowId, "hasViewUI");
	
	if(hasViewUI == null)
	   hasViewUI = true;
	   
	return hasViewUI;	
};

CasesBPMAssets.initFormsGrid = function(caseId, piId, customerView, hasRightChangeRights, usePdfDownloadColumn, allowPDFSigning, hideEmptySection,
										showAttachmentStatistics, specialBackPage, onFormsInited) {
    var identifier = 'caseForms';
    
    var populatingFunction = function(params, callback) {
        params.piId = piId;
        params.rightsChanger = hasRightChangeRights;
        params.downloadDocument = usePdfDownloadColumn;
        params.allowPDFSigning = allowPDFSigning;
        params.nameFromExternalEntity = CasesBPMAssets.openedCase.nameFromExternalEntity;
        
        BPMProcessAssets.getProcessDocumentsList(params, {
            callback: function(gridEntriesBean) {
            	
            	CasesBPMAssets.pushRowsParams(gridEntriesBean);
            	
            	if (gridEntriesBean != null) {
            		var gridEntries = gridEntriesBean.gridEntries;
                	callback(gridEntries);
            	}
                
                CasesBPMAssets.openAllAttachmentsForCase(jQuery('#' + params.identifier + CasesBPMAssets.GRID_WITH_SUBGRID_ID_PREFIX + piId));
                
                CasesBPMAssets.setStyleClassesForGridColumns(jQuery('div.' + identifier + 'Part'));
                
                CasesBPMAssets.hideHeaderTableIfNoContent(jQuery('div.' + identifier + 'Part', jQuery(customerView)), hideEmptySection);
                
                if (onFormsInited) {
                	onFormsInited();
                }
            }
        });
    };

    var subGridFunction = function(subgridId, rowId) {
        CasesBPMAssets.initFilesSubGridForCasesListGrid(caseId, subgridId, rowId, hasRightChangeRights, identifier, allowPDFSigning, showAttachmentStatistics);
    };
    
    var namesForColumns = new Array();
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_FORM_NAME);
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_SUBMITTED_BY);
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_DATE);
    if (usePdfDownloadColumn) {
    	namesForColumns.push(''/*CasesBPMAssets.Loc.CASE_GRID_STRING_DOWNLOAD_DOCUMENT_AS_PDF*/);
    }
    if (allowPDFSigning) {
    	namesForColumns.push('');
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
    if (allowPDFSigning) {
    	modelForColumns.push({name:'allowPDFSigning',index:'allowPDFSigning'});
    }
    if (hasRightChangeRights) {
        modelForColumns.push({name:'rightsForDocumentResources',index:'rightsForDocumentResources'});
    }
    
    var onSelectRowFunction = function(rowId) {
		if (jQuery('#' + rowId).hasClass('pdfViewableItem')) {
			var uri = '&variableName=files_pdfTaskView&taskID=' + rowId;
        	CasesBPMAssets.setCurrentWindowToDownloadCaseResource(uri, CasesBPMAssets.DOWNLOAD_TASK_IN_PDF_LINK_STYLE_CLASS);
		} else if (CasesBPMAssets.isRowHasViewUI(piId, rowId)) {
    		CasesBPMAssets.getProcessRersourceView(caseId, rowId, specialBackPage);	
    	}
    };
    
    CasesBPMAssets.initGridBase(piId, customerView, identifier, populatingFunction, subGridFunction, namesForColumns, modelForColumns, onSelectRowFunction,
    							hasRightChangeRights, null);
};

CasesBPMAssets.reloadDocumentsGrid = function() {
	if (CasesBPMAssets.openedCase == null) {
		return false;
	}
	
	var caseId = CasesBPMAssets.openedCase.caseId;
	var container = CasesBPMAssets.openedCase.container;
	if (caseId == null || container == null) {
		return false;
	}
	
	var piId = CasesBPMAssets.openedCase.piId;
	if (piId == null) {
		var inputs = jQuery('input.processInstanceIdValueHolder', container);
		if (inputs != null && inputs.length > 0) {
			piId = jQuery(inputs[0]).attr('value');
		}
	}
	if (piId == null) {
		return false;
	}
	
	var documentsGrids = jQuery('div.caseFormsPart', jQuery(container));
	if (documentsGrids == null || documentsGrids.length == 0) {
		return false;
	}
	var documentsGrid = null;
	for (var i = 0; i < documentsGrids.length; i++) {
		documentsGrid = documentsGrids[i];
		
		var gridElements = new Array();
		for (var j = 0; j < documentsGrid.childNodes.length; j++) {
			gridElements.push(jQuery(documentsGrid.childNodes[j]));
		}
		
		var gridElement = null;
		for (var j = 0; j < gridElements.length; j++) {
			gridElement = gridElements[j];
			
			if (!gridElement.hasClass('header')) {
				gridElement.remove();
			}
		}
		
		jQuery(documentsGrid).removeAttr('col_with_classes');
		jQuery(documentsGrid).append('<table class=\'caseForms\' border=\'0\'/>');
	}
	
	var reOpenFormsGrid = function(hasRights) {
		var usePdfDownloadColumn = CasesBPMAssets.openedCase.usePdfDownloadColumn;
		var allowPDFSigning = CasesBPMAssets.openedCase.allowPDFSigning;
		var hideEmptySection = CasesBPMAssets.openedCase.hideEmptySection;
		var showAttachmentStatistics = CasesBPMAssets.openedCase.showAttachmentStatistics;
		var specialBackPage = CasesBPMAssets.openedCase.specialBackPage;
	
		CasesBPMAssets.initFormsGrid(caseId, piId, container, hasRights, 
				usePdfDownloadColumn, allowPDFSigning, hideEmptySection, 
				showAttachmentStatistics, specialBackPage,
		function() {
			CasesBPMAssets.setTableProperties(container);
		});
	}
	
	var hasRightChangeRights = CasesBPMAssets.openedCase.hasRightChangeRights == null ? false : CasesBPMAssets.openedCase.hasRightChangeRights;
	if (hasRightChangeRights) {
		reOpenFormsGrid(hasRightChangeRights);
	}
	else {
		BPMProcessAssets.hasUserRolesEditorRights(piId, {
			callback: function(hasRights) {
				reOpenFormsGrid(hasRights);
			}
		});
	}
}

CasesBPMAssets.initEmailsGrid = function(caseId, piId, customerView, hasRightChangeRights, allowPDFSigning, hideEmptySection, showAttachmentStatistics,
	onEmailsInited) {
	var identifier = 'caseEmails';
    
    var populatingFunction = function(params, callback) {
        params.piId = piId;

        BPMProcessAssets.getProcessEmailsList(params, {
            callback: function(result) {
                callback(result);

                CasesBPMAssets.openAllAttachmentsForCase(jQuery('#' + params.identifier + CasesBPMAssets.GRID_WITH_SUBGRID_ID_PREFIX + piId));
                
                CasesBPMAssets.setStyleClassesForGridColumns(jQuery('div.' + identifier + 'Part'));
                
                CasesBPMAssets.hideHeaderTableIfNoContent(jQuery('div.' + identifier + 'Part', jQuery(customerView)), hideEmptySection);
                
                jQuery.each(jQuery('a.emailSenderLightboxinBPMCasesStyle'), function() {
                	var link = jQuery(this);
                	
                	if (!link.hasClass('emailSenderLightboxinBPMCasesStyleInitialized')) {
                		link.addClass('emailSenderLightboxinBPMCasesStyleInitialized');
                		link.fancybox({
                			autoScale: false,
							autoDimensions: false,
                			width:	750,
							height:	450
                		});
                	}
                });
                
                if (onEmailsInited) {
                	onEmailsInited();
                }
            }
        });
    };
    
    var subGridFunction = function(subgridId, rowId) {
        CasesBPMAssets.initFilesSubGridForCasesListGrid(caseId, subgridId, rowId, hasRightChangeRights, identifier, allowPDFSigning, showAttachmentStatistics);
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
    
    CasesBPMAssets.initGridBase(piId, customerView, identifier, populatingFunction, subGridFunction, namesForColumns, modelForColumns, onSelectRowFunction,
    							hasRightChangeRights, null);
};

CasesBPMAssets.initContactsGrid = function(piId, customerView, 
		hasRightChangeRights, hideEmptySection, showOnlyCreatorInContacts, 
		showContacts, onContactsInited, nameFromExternalEntity) {
		
	if (!showContacts) {
		CasesBPMAssets.CASE_VIEW_PARTS_TO_INIT--;
		return;
	}
	
    var identifier = 'caseContacts';
    
    var populatingFunction = function(params, callback) {
        params.piId = piId;
        params.showOnlyCreatorInContacts = showOnlyCreatorInContacts;
        params.nameFromExternalEntity = nameFromExternalEntity;
        
        BPMProcessAssets.getProcessContactsList(params, {
            callback: function(result) {
                callback(result);
                
                CasesBPMAssets.setStyleClassesForGridColumns(jQuery('div.' + identifier + 'Part'));
                
                CasesBPMAssets.hideHeaderTableIfNoContent(jQuery('div.' + identifier + 'Part', jQuery(customerView)), hideEmptySection);
                
                var manageProfilePictures = function() {
                	jQuery.each(jQuery('img.userProfilePictureInCasesList', jQuery(customerView)), function() {
			    		var image = jQuery(this);
			    		var imageSource = image.attr('src');
			    		var tdCell = image.parent();
			    		var pictureBoxId = 'personProfilePictureInCasesListContactPart';
			    		tdCell.parent().mouseover(function(event) {
			    			jQuery(document.body).append('<div id=\''+pictureBoxId+'\' class=\''+pictureBoxId+'Style\' style=\'display: none;\'>'+
			    				'<img src=\''+imageSource+'\' /></div>');

			    			jQuery('#' + pictureBoxId).css({
								top: (jQuery(event.target).position().top + 2) + 'px',
								left: (event.clientX + 10) + 'px'
							});
							jQuery('#' + pictureBoxId).fadeIn('fast');
			    		});
			    		tdCell.parent().mouseout(function(event) {
			    			jQuery('#' + pictureBoxId).fadeOut('fast', function() {
			    				jQuery(this).remove();
			    			});
			    		});
					});
                }
                manageProfilePictures();
                
                if (onContactsInited) {
                	onContactsInited();
                }
            }
        });
    };
    
    var namesForColumns = new Array();
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_CONTACT_NAME);
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_EMAIL_ADDRESS);
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_PHONE_NUMBER);
    if (hasRightChangeRights) {
        namesForColumns.push(''/*CasesBPMAssets.Loc.CASE_GRID_STRING_CHANGE_ACCESS_RIGHTS*/);
    }
	
    var modelForColumns = new Array();
    modelForColumns.push({name:'name',index:'name'});
    modelForColumns.push({name:'emailAddress',index:'emailAddress'});
    modelForColumns.push({name:'phoneNumber',index:'phoneNumber'});
   	if (hasRightChangeRights) {
        modelForColumns.push({name:'rightsForContact',index:'rightsForContact'});
    }
    
    var onSelectRowFunction = function(rowId) {
    }
    
    CasesBPMAssets.initGridBase(piId, customerView, identifier, populatingFunction, null, namesForColumns, modelForColumns, onSelectRowFunction,
    							hasRightChangeRights, null);
};

CasesBPMAssets.initGridBase = function(piId, customerView, tableClassName, populatingFunction, subGridForThisGrid, namesForColumns, modelForColumns,
										onSelectRowFunction, rightsChanger, callbackAfterInserted) {
    var params = new JQGridParams();
    
    params.identifier = tableClassName;
    params.rightsChanger = rightsChanger;
    
    params.populateFromFunction = populatingFunction;
    
    params.colNames = namesForColumns;
    params.colModel = modelForColumns;
    
    if (onSelectRowFunction != null) {
        params.onSelectRow = onSelectRowFunction;
    }
    
    if (callbackAfterInserted != null) {
    	params.callbackAfterInserted = callbackAfterInserted;
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

CasesBPMAssets.hideHeaderTableIfNoContent = function(container, fullyHide) {
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
			if (fullyHide) {
				jQuery(headers[i]).parent().css('display', 'none').removeClass('caseListTasksSectionVisibleStyleClass')
																	.addClass('caseListTasksSectionNotVisibleStyleClass');
			}
			else {
				jQuery(headers[i]).css('display', 'none');
			}
		}
	}
	if (changeBody) {
		var bodies = jQuery('div.gridBodyTableContainer', container);
		var gridBody = null;
		for (var i = 0; i < bodies.length; i++) {
			gridBody = jQuery(bodies[i]);
			
			if (fullyHide) {
				gridBody.parent().css('display', 'none').removeClass('caseListTasksSectionVisibleStyleClass')
																	.addClass('caseListTasksSectionNotVisibleStyleClass');
			}
			else {
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
}

CasesBPMAssets.getProcessRersourceView = function(caseId, taskInstanceId, specialBackPage) {
	if (!caseId || !taskInstanceId)
		return false;
	if (caseId < 0 || taskInstanceId < 0)
		return false;
	
	var params = 'prm_case_pk=' + caseId + '&tiId=' + taskInstanceId + '&cp_prm_action=8';
	if (specialBackPage != null && specialBackPage != '')
		params += '&casesAssetsSpecialBackPage=' + specialBackPage;
    changeWindowLocationHref(params);
}

CasesBPMAssets.initFilesSubGridForCasesListGrid = function(caseId, subgridId, rowId, hasRightChangeRights, identifier, allowPDFSigning,
	showAttachmentStatistics) {
    var subgridTableId = subgridId + '_t';
    var subGridContainer = jQuery('#' + subgridId);
	subGridContainer.html('<table id=\''+subgridTableId+'\' class=\'scroll subGrid\' cellpadding=\'0\' cellspacing=\'0\' border=\'0\'></table>');

    var subGridParams = new JQGridParams();
    subGridParams.rightsChanger = hasRightChangeRights;
    subGridParams.identifier = identifier +'Attachments';
    subGridParams.allowPDFSigning = allowPDFSigning;
	subGridParams.showAttachmentStatistics = showAttachmentStatistics;
	subGridParams.caseId = caseId;
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
					subGridOpener.empty().html('&nbsp;');
                    subGridOpener.unbind('click');
                }
            }
        });
    };
    subGridParams.callbackAfterInserted = function() {
    	LinksLinker.linkLinks(false, subgridTableId);
    	
    	jQuery.each(jQuery('a.BPMCaseAttachmentStatisticsInfo', jQuery('#' + subgridTableId)), function() {
                	var link = jQuery(this);
                	
                	if (!link.hasClass('BPMCaseAttachmentStatisticsInfoInitialized')) {
                		link.addClass('BPMCaseAttachmentStatisticsInfoInitialized');
                		link.fancybox({
                			autoScale: false,
							autoDimensions: false,
                			width:	400,
							height:	300,
							hideOnContentClick: false
                		});
                	}
                });
    }

    var namesForColumns = new Array();
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_FILE_DESCRIPTION);
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_FILE_NAME);
    namesForColumns.push(CasesBPMAssets.Loc.CASE_GRID_STRING_FILE_SIZE);
    if (showAttachmentStatistics) {
    	namesForColumns.push('');
    }
    if (allowPDFSigning) {
    	namesForColumns.push('');
    }
    if (subGridParams.rightsChanger) {
        namesForColumns.push(''/*CasesBPMAssets.Loc.CASE_GRID_STRING_CHANGE_ACCESS_RIGHTS*/);
    }
    subGridParams.colNames = namesForColumns;
    
    var modelForColumns = new Array();
    modelForColumns.push({name:'description',index:'description'});
    modelForColumns.push({name:'name',index:'name'});
    modelForColumns.push({name:'fileSize',index:'fileSize'});
    if (showAttachmentStatistics) {
    	modelForColumns.push({name:'showAttachmentStatistics',index:'showAttachmentStatistics'});
    }
    if (subGridParams.allowPDFSigning) {
    	modelForColumns.push({name:'allowPDFSigning',index:'allowPDFSigning'});
    }
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

CasesBPMAssets.setTableProperties = function(component) {
	if (component == null) {
		return false;
	}
	
	var tables = jQuery('table', jQuery(component));
	if (tables == null || tables.length == 0) {
		return false;
	}
	
	var removeWidths = function(elements) {
		if (elements == null || elements.length == 0) {
			return false;
		}
		
		var element = null;
		var currentStyle = null;
		for (var i = 0; i < elements.length; i++) {
			element = jQuery(elements[i]);
			
			currentStyle = element.attr('style');
			if (currentStyle != null && currentStyle != '') {
				var widthStart = currentStyle.indexOf('width:');
				try {
					if (widthStart > 0) {
						var endIndex = widthStart;
						while ((endIndex + 1) < currentStyle.length && currentStyle.substring(endIndex, endIndex + 1) != ';') {
							endIndex++;
						}
						var widthValue = currentStyle.slice(widthStart, endIndex + 1);
						currentStyle = currentStyle.replace(widthValue, '');
						element.attr('style', currentStyle);
					}
				} catch(e) {
					element.removeAttr('style');
				}
			}
		}
	}
	
	var table = null;
	for (var i = 0; i < tables.length; i++) {
		table = jQuery(tables[i]);
		
		table.attr('border', '0');
		
		removeWidths(jQuery('th', table));
		removeWidths(jQuery('td', table));
	}
}

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
    closeAllLoadingMessages();
    
    return true;
};

CasesBPMAssets.downloadCaseDocument = function(event, taskId) {
	CasesBPMAssets.showHumanizedMessage(CasesBPMAssets.Loc.CASE_GRID_STRING_GENERATING_PDF, {
		timeout: 3000
	});
	
	var uri = '&taskInstanceId=' + taskId;
	CasesBPMAssets.setCurrentWindowToDownloadCaseResource(uri, CasesBPMAssets.CASE_PDF_DOWNLOADER_LINK_STYLE_CLASS);
	
	if (event) {
		if (event.stopPropagation) {
			event.stopPropagation();
		}
		event.cancelBubble = true;
	}
}

CasesBPMAssets.signCaseDocument = function(event, taskInstanceId, variableHash, message, lightBoxTitle, closeLightBoxTitle, errorMessage) {
	showLoadingMessage(message);
	try {
		BPMProcessAssets.getSigningAction(taskInstanceId, null, {
			callback: function(uri) {
				closeAllLoadingMessages();
				CasesBPMAssets.openDocumentSignerWindow(uri, lightBoxTitle, closeLightBoxTitle);
			}  
		});
	} catch(e) {
		closeAllLoadingMessages();
		CasesBPMAssets.showHumanizedMessage(errorMessage);
	}
	
	if (event) {
		if (event.stopPropagation) {
			event.stopPropagation();
		}
		event.cancelBubble = true;
	}
}

CasesBPMAssets.signCaseAttachment = function(event, taskInstanceId, variableHash, message, lightBoxTitle, closeLightBoxTitle, errorMessage) {
	showLoadingMessage(message);
	try {
		BPMProcessAssets.getSigningAction(taskInstanceId, variableHash, {
			callback: function(uri) {
				closeAllLoadingMessages();
				CasesBPMAssets.openDocumentSignerWindow(uri, lightBoxTitle, closeLightBoxTitle);
			} 
		});
	} catch(e) {
		closeAllLoadingMessages();
		CasesBPMAssets.showHumanizedMessage(errorMessage);
	}
	
	if (event) {
		if (event.stopPropagation) {
			event.stopPropagation();
		}
		event.cancelBubble = true;
	}
}

CasesBPMAssets.openDocumentSignerWindow = function(uri, lightBoxTitle, closeLightBoxTitle) {
	if (uri == null || uri == '') {
		return false;
	}
	
	var windowHeight = Math.round(windowinfo.getWindowHeight() * 0.8);
	var windowWidth = Math.round(windowinfo.getWindowWidth() * 0.8);
	var tmpFancyBoxLinkId = 'tmpFancyBoxLinkId_' + Math.ceil(1000 * Math.random());
	jQuery(document.body).append('<a class=\'iframe\' data-fancybox-type=\'iframe\' style=\'display: none;\' id=\'' + tmpFancyBoxLinkId + '\' href=\'' + uri + '\' title=\'' + lightBoxTitle + '\'>' +
		lightBoxTitle + '</a>');
	jQuery('#' + tmpFancyBoxLinkId).fancybox({
		autoScale: false,
		autoDimensions: false,
		width:	windowWidth,
		height:	windowHeight,
		hideOnOverlayClick: false,
		hideOnContentClick: false,
		onClosed: function() {
			CasesBPMAssets.reloadDocumentsGrid();
			jQuery('#' + tmpFancyBoxLinkId).remove();
		},
		onComplete: function() {
			closeAllLoadingMessages();
		}
	});
	jQuery('#' + tmpFancyBoxLinkId).trigger('click');
}

CasesBPMAssets.showHumanizedMessage = function(message, params) {
	humanMsg.displayMsg(message, params);
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

CasesBPMAssets.showAccessRightsForBpmRelatedResourceChangeMenu = function(event, processId, taskId, element, fileHashValue, setSameRightsForAttachments, userId) {
	
	if (element == null || event == null) {
		return false;
	}
	
	element = jQuery(element);
	
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
	
    var htmlForBox = "<div class='caseProcessResourceAccessRightsSetterStyle' />";
	var rightsBox = jQuery(htmlForBox);
		
	jQuery(document.body).append(rightsBox);
	   
	rightsBox.css('top', yCoord + 'px');
	rightsBox.css('left', xCoord + 'px');
	
	var clbck = {
	   callback: function(component) {
            if (component == null) {
                return false;
            }
            
            insertNodesToContainer(component, rightsBox[0]);
            
            rightsBox.show('fast');
        }
	};
	
	if(taskId != null) {
    
        BPMProcessAssets.getAccessRightsSetterBox(processId, taskId, fileHashValue, setSameRightsForAttachments, clbck);    
        
    } else {
        
        BPMProcessAssets.getContactsAccessRightsSetterBox(processId, userId, clbck);
    }
}

CasesBPMAssets.setAccessRightsForBpmRelatedResource = function(id, processId, taskInstanceId, userId, fileHashValue, sameRightsSetterId) {
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
	
	BPMProcessAssets.setAccessRightsForProcessResource(element.name, processId, taskInstanceId, fileHashValue, canAccess, setSameRightsForAttachments, userId, {
		callback: function(message) {
			if (message == null) {
				return false;
			} else {
				humanMsg.displayMsg(message);
			}
		}
	});
}

CasesBPMAssets.closeAccessRightsSetterBox = function(event, id) {
	var element = event == null ? null : event.target;
	if (element == null && id != null) {
		element = jQuery('#' + id);
	}
	if (element == null) {
		return false;
	}
	
	var rightsBoxCands = jQuery(element).parents('div.caseProcessResourceAccessRightsSetterStyle');
	if (rightsBoxCands == null || rightsBoxCands.length == 0) {
		return false;
	}
	
	jQuery.each(rightsBoxCands, function() {
		var rightsBox = jQuery(this);
	
		rightsBox.hide('fast', 
	        function () {
	            rightsBox.remove();
	        }
		);
	});
}

CasesBPMAssets.setRoleDefaultContactsForUser = function(event, processInstanceId, userId) {
	var element = event.target;
	
	showLoadingMessage(CasesBPMAssets.Loc.CASE_GRID_STRING_LOADING);
	
	var rightsBoxCands = jQuery(element).parents('div.caseProcessResourceAccessRightsSetterStyle');
    
    if (rightsBoxCands == null || rightsBoxCands.length == 0) {
    	closeLoadingMessage();
        return false;
    }
    
    var rightsBox = jQuery(rightsBoxCands[0]);
    
	var clbck = {
       callback: function(component) {
            if (component == null) {
            	closeLoadingMessage();
                return false;
            }
            
            rightsBox.empty();
            insertNodesToContainer(component, rightsBox[0]);
            closeLoadingMessage();
        }
    };
    
    BPMProcessAssets.setRoleDefaultContactsForUser(processInstanceId, userId, clbck);
}

CasesBPMAssets.setWatchOrUnwatchTask = function(element, processInstanceId) {
	showLoadingMessage(CasesBPMAssets.Loc.CASE_GRID_STRING_LOADING);
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

CasesBPMAssets.assignCase = function(handlerId, processInstanceId, container) {
	showLoadingMessage('');
    BPMProcessAssets.assignCase(handlerId, processInstanceId, {
        callback: function(result) {
	        if (result) {
	        	LazyLoader.load('/dwr/interface/CasesEngine.js', function() {
	            	CasesEngine.getCaseStatus(processInstanceId, {
		            	callback: function(caseStatus) {
		            		closeAllLoadingMessages();
		            		
		                	if (caseStatus == null) {
		                		return;
		                	}
		                					
		                	var statusContainer = jQuery('div.casesListBodyContainerItemStatus', jQuery(container).parent().parent());
		                	statusContainer.text(caseStatus);
		             	}
		            });
	          	}, null);
	       	}
        }
    });
}

CasesBPMAssets.showSendEmailWindow = function(event) {
	if (!event) {
		return false;
	}

	if (event.stopPropagation) {
		event.stopPropagation();
	}
	event.cancelBubble = true;
}

CasesBPMAssets.disableAttachmentForAllRoles = function(event, fileHash, processInstanceId, taskInstanceId) {
	if (!window.confirm(CasesBPMAssets.Loc.CASE_GRID_STRING_ARE_YOU_SURE)) {
		return false;
	}
	
	BPMProcessAssets.disableAttachmentForAllRoles(fileHash, processInstanceId, taskInstanceId, {
		callback: function(result) {
			if (result) {
				CasesBPMAssets.closeAccessRightsSetterBox(event, null);
			}
		}
	});
}

CasesBPMAssets.notifyToDownloadAttachment = function(properties) {
	if (properties == null) {
		return false;
	}
	
	var fullHref = window.location.href;
	properties.server = fullHref.substring(0, fullHref.indexOf(window.location.pathname));
	properties.url = properties.server + window.location.pathname + '?prm_case_pk=' + CasesBPMAssets.openedCase.caseId;
	ProcessAttachmentDownloadNotifier.sendDownloadNotifications(new BPMAttachmentDownloadNotificationProperties(properties), {
		callback: function(result) {
			if (result == null) {
				return false;
			}
			
			humanMsg.displayMsg(result.value, null);
		}
	});
}

function BPMAttachmentDownloadNotificationProperties(properties) {
	this.file = properties.file;
	this.url = properties.url;
	this.server = properties.server;
	
	this.users = properties.users;
	
	this.taskId = properties.taskId;
	this.hash = properties.hash;
}