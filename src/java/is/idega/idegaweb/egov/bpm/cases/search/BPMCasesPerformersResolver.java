package is.idega.idegaweb.egov.bpm.cases.search;

import is.idega.idegaweb.egov.bpm.cases.actionhandlers.CaseHandlerAssignmentHandler;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.variables.MultipleSelectionVariablesResolver;

@Scope("request")
@Service(MultipleSelectionVariablesResolver.BEAN_NAME_PREFIX + CaseHandlerAssignmentHandler.performerUserIdVarName)
public class BPMCasesPerformersResolver extends BPMCasesHandlersResolver {
	//	Implementation is in a super class
}