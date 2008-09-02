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