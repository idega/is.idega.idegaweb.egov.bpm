package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import java.util.List;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.scheduler.SchedulerService;

/**
 * 
 * 
 * @author <a href="anton@idega.com">Anton Makarov</a>
 * @version Revision: 1.0
 * 
 * Last modified: Nov 28, 2008 by Author: Anton
 * 
 */

public class EnsureSingleTimerOnNode implements ActionHandler {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	public void execute(ExecutionContext executionContext) throws Exception {
		SchedulerService schedulerService = executionContext.getJbpmContext()
				.getServices().getSchedulerService();

		List<Token> tokens = executionContext.getProcessInstance()
				.getRootToken().getChildrenAtNode(executionContext.getNode());
		for (Token tok : tokens) {
			if (!tok.equals(executionContext.getToken())) {
				schedulerService.deleteTimersByName("reminder", tok);
			}
		}
	}
}
