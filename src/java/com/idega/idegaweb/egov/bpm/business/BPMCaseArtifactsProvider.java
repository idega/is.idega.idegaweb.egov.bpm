package com.idega.idegaweb.egov.bpm.business;

import is.idega.idegaweb.egov.cases.business.CaseArtifactsProvider;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.data.CaseProcInstBind;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.data.dao.CasesBPMDAO;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

@Service("bpmCaseArtifactsProvider")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class BPMCaseArtifactsProvider implements CaseArtifactsProvider {
	
	private static final Logger LOGGER = Logger.getLogger(BPMCaseArtifactsProvider.class.getName());
	
	@Autowired
	private CasesBPMDAO casesBPMDAO;
	
	@Autowired
	private VariableInstanceQuerier variablesQuerier;
	
	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true)
	public <T extends Serializable> T getVariableValue(Object caseId, String variableName) {
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
		Collection<VariableInstanceInfo> variables = null;
		try {
			variables = variablesQuerier.getVariablesByProcessInstanceIdAndVariablesNames(Arrays.asList(variableName), Arrays.asList(procInstId), false, false);
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting variable '"+variableName+"' from process instance: " + procInstId, e);
		}
		if (ListUtil.isEmpty(variables)) {
			return null;
		}
		
		for (VariableInstanceInfo variable: variables) {	
			String name = variable.getName();
			if (StringUtil.isEmpty(name)) {
				continue;
			}
			
			if (name.equals(variableName)) {
				Serializable value = variable.getValue();
				return value == null ? null : (T) value;
			}
		}
		
		return null;
	}	
}