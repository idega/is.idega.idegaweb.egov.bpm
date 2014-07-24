package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

public interface CaseStatusGuardian {

	public boolean isAllowedToChangeStatus(org.jbpm.graph.exe.ExecutionContext ectx, Integer caseId);

	public String getExtraHandlerBeanName(org.jbpm.graph.exe.ExecutionContext ectx, Integer caseId);

}