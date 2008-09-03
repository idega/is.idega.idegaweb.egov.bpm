if(AppTypeBPM == null) var AppTypeBPM = {};
 
AppTypeBPM.processRolesCheckbox = function(checkboxId, rolesSpanId, rolesMenuId) {
	
    if(document.getElementById(checkboxId).checked) {
    	
        jQuery('#'+rolesSpanId).show();
    	
    } else {
    	
    	jQuery('#'+rolesSpanId).hide();
    	
    	var opts = document.getElementById(rolesMenuId).options;
    	
        for(var i = 0; i < opts.length; i++) {
            opts[i].selected = false;
        }
    }
}

AppTypeBPM.processProcessesSelector = function(selectId, rolesMenuId, rolesSpanId, checkboxId, applicationId) {
	
	if(applicationId != null) {
		
		var processId = jQuery('#'+selectId).val();
	    
	    ApplicationTypeBPM.getRolesCanStartProcessDWR(processId, applicationId, 
               function(roles) {
                
                var opts = document.getElementById(rolesMenuId).options;
                
                if(roles == null || roles.length == 0) {
                    
                    jQuery('#'+rolesSpanId).hide();
                    document.getElementById(checkboxId).checked = false;
                    
                    for(var i = 0; i < opts.length; i++) {
                        opts[i].selected = false;
                    }
                    
                } else {
                    
                    jQuery('#'+rolesSpanId).show();
                    document.getElementById(checkboxId).checked = true;
                    
                    for(var i = 0; i < opts.length; i++) {
                        
                        opts[i].selected = false;
                        
                        for(var ii = 0; ii < roles.length; ii++) {
                            
                            if(roles[ii] == opts[i].value)
                                opts[i].selected = true;
                        }
                    }
                }
            });
	}
}