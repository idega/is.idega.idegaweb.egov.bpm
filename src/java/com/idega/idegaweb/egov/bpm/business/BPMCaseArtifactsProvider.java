package com.idega.idegaweb.egov.bpm.business;

import is.idega.idegaweb.egov.cases.business.CaseArtifactsProvider;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.context.exe.VariableInstance;
import org.jbpm.context.exe.variableinstance.NullInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.idegaweb.egov.bpm.data.CaseProcInstBind;
import com.idega.idegaweb.egov.bpm.data.dao.CasesBPMDAO;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

@Service("bpmCaseArtifactsProvider")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class BPMCaseArtifactsProvider implements CaseArtifactsProvider {
	
	private static final Logger LOGGER = Logger.getLogger(BPMCaseArtifactsProvider.class.getName());
	
	@Autowired
	private CasesBPMDAO casesBPMDAO;
	
	@Transactional(readOnly = true)
	public Object getVariableValue(Object caseId, String variableName) {
		if (caseId == null || StringUtil.isEmpty(variableName)) {
			return null;
		}
		
		CaseProcInstBind bind = null;
		try {
			bind = casesBPMDAO.getCaseProcInstBindByCaseId(Integer.valueOf(caseId.toString()));
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting bind by case id: " + caseId);
		}
		if (bind == null) {
			return null;
		}
		
		Long procInstId = bind.getProcInstId();
		List<VariableInstance> variables = null;
		try {
			variables = casesBPMDAO.getVariablesByProcessInstanceIdAndVariablesNames(Arrays.asList(procInstId), Arrays.asList(variableName));
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting variable '"+variableName+"' from process instance: " + procInstId, e);
		}
		if (ListUtil.isEmpty(variables)) {
			return null;
		}
		
		for (VariableInstance variable: variables) {
			if ((variable instanceof NullInstance)) {
				continue;
			}
	
			String name = variable.getName();
			if (StringUtil.isEmpty(name)) {
				continue;
			}
			
			if (name.equals(variableName)) {
				return  variable.getValue();
			}
		}
		
		return null;
	}
	
}
