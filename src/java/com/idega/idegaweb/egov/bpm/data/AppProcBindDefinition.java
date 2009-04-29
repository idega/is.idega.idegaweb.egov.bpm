package com.idega.idegaweb.egov.bpm.data;

import org.jbpm.module.def.ModuleDefinition;
import org.jbpm.module.exe.ModuleInstance;

/**
 * @deprecated this is held here only to support older processes, that contains this definition
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $ Last modified: $Date: 2009/04/29 13:38:11 $ by $Author: civilis $
 */
@Deprecated
public class AppProcBindDefinition extends ModuleDefinition {
	
	private static final long serialVersionUID = 6504030191381296426L;
	
	@Override
	public ModuleInstance createInstance() {
		return null;
	}
}