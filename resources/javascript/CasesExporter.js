var CasesExporter = {};

CasesExporter.FINSHED_EXPORT = false;

CasesExporter.doExportCases = function(params) {
	var procDefId = jQuery('#' + params.dropdownId).val();
	CasesExporter.FINSHED_EXPORT = false;
	jQuery('#' + params.resultsId).html('');
	
	showLoadingMessage(params.exporting);
	CasesEngine.getExportedCasesToPDF(procDefId, params.id, {
		callback: function(result) {
			CasesExporter.FINSHED_EXPORT = true;
			closeAllLoadingMessages();
			if (result == null) {
				humanMsg.displayMsg('Error', {timeout: 3000});
				return;
			}
			
			humanMsg.displayMsg(result.value, {timeout: 3000});
			
			if (result.id == 'true') {
				showLoadingMessage(params.loading);
				IWCORE.getRenderedComponentByClassName({
					className: params.resultsUI,
					properties: [
						{id: 'setExportId', value: params.id}	
					],
					container: params.resultsId,
					rewrite: true,
					callback: function() {
						closeAllLoadingMessages();
					}
				});
			}
		}, errorHandler: function(o1, o2) {
			closeAllLoadingMessages();
			humanMsg.displayMsg('Error', {timeout: 3000});
			return;
		}
	});
	
	CasesExporter.doFetchStatus(params.id);
}

CasesExporter.doFetchStatus = function(id) {
	if (CasesExporter.FINSHED_EXPORT) {
		closeAllLoadingMessages();
		return;
	}
	
	CasesEngine.getStatusOfExport(id, {
		callback: function(result) {
			if (result == null) {
				window.setTimeout(function() {CasesExporter.doFetchStatus(id);}, 1000);
			} else {
				jQuery('#loadingtext').html(result.value);
				window.setTimeout(function() {CasesExporter.doFetchStatus(id);}, 1000);
			}
		}
	});
}

CasesExporter.doDownload = function(id, caseIdentifier) {
	var identifiers = null;
	if (caseIdentifier != null && caseIdentifier != '') {
		identifiers = [];
		identifiers.push(caseIdentifier);
	}
	CasesEngine.getLinkForZippedCases(id, identifiers, {
		callback: function(result) {
			if (result == null) {
				return false;
			} else if (result.id == 'false') {
				humanMsg.displayMsg(result.value, {timeout: 3000});
			} else if (result.id == 'true') {
				window.location.href = result.value;
				humanMsg.displayMsg(result.name, {timeout: 3000});
			}
		}
	});
}


