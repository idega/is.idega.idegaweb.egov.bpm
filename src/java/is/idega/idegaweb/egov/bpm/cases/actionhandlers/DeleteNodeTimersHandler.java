package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import java.util.List;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.scheduler.SchedulerService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * @author <a href="anton@idega.com">Anton Makarov</a>
 * @version Revision: 1.0 Last modified: Nov 28, 2008 by Author: Anton
 */

@Service("deleteNodeTimersHandler")
@Scope("prototype")
public class DeleteNodeTimersHandler implements ActionHandler {
	
	private static final long serialVersionUID = -8838368363278394618L;
	
	private String timersNodeName;
	
	@SuppressWarnings("unchecked")
	public void execute(ExecutionContext executionContext) throws Exception {
		
		SchedulerService schedulerService = executionContext.getJbpmContext()
		        .getServices().getSchedulerService();
		
		Node timersNode = null;
		
		if (getTimersNodeName() != null) {
			
			timersNode = executionContext.getProcessDefinition().getNode(
			    getTimersNodeName());
		} else {
			timersNode = executionContext.getNode();
		}
		
		String timersName = timersNode.getName();
		List<Token> tokens = executionContext.getProcessInstance()
		        .getRootToken().getChildrenAtNode(timersNode);
		
		for (Token tok : tokens) {
			
			if (!tok.equals(executionContext.getToken())) {
				
				schedulerService.deleteTimersByName(timersName, tok);
			}
		}
	}
	
	public String getTimersNodeName() {
		return timersNodeName;
	}
	
	public void setTimersNodeName(String timersNodeName) {
		this.timersNodeName = timersNodeName;
	}
}
