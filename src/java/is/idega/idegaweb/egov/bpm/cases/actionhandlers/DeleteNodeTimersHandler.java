package is.idega.idegaweb.egov.bpm.cases.actionhandlers;

import java.util.List;
import java.util.logging.Level;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.scheduler.SchedulerService;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.business.DefaultSpringBean;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;

/**
 * @author <a href="anton@idega.com">Anton Makarov</a>
 * @version Revision: 1.0 Last modified: Nov 28, 2008 by Author: Anton
 */

@Service("deleteNodeTimersHandler")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteNodeTimersHandler extends DefaultSpringBean implements ActionHandler {

	private static final long serialVersionUID = -8838368363278394618L;

	private String timersNodeName;

	@Override
	@SuppressWarnings("unchecked")
	public void execute(ExecutionContext executionContext) throws Exception {
		ProcessInstance pi = null;
		try {
			pi = executionContext.getProcessInstance();
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting process instance from execution context " + executionContext, e);
		}
		if (pi == null) {
			return;
		}

		SchedulerService schedulerService = executionContext.getJbpmContext().getServices().getSchedulerService();

		Node timersNode = null;
		String timersNodeName = null;
		ProcessDefinition pd = null;
		try {
			timersNodeName = getTimersNodeName();
			if (timersNodeName == null) {
				timersNode = executionContext.getNode();
			} else {
				pd = executionContext.getProcessDefinition();
				timersNode = pd.getNode(timersNodeName);
			}
		} catch (Exception e) {
			String message = "Error getting node" + (timersNodeName == null ? CoreConstants.EMPTY : " (name: '" + timersNodeName + "')") +
					" for process definition" + (pd == null ? CoreConstants.EMPTY : " (name: " + pd.getName() + ", ID: " + pd.getId() + ")") +
					" and process instance " + pi + ", ID: " + pi.getId();
			CoreUtil.sendExceptionNotification(message, e);
			getLogger().log(Level.WARNING, message, e);
		}
		if (timersNode == null) {
			return;
		}

		String timersName = timersNode.getName();
		List<Token> tokens = pi.getRootToken().getChildrenAtNode(timersNode);
		if (ListUtil.isEmpty(tokens)) {
			return;
		}

		for (Token tok: tokens) {
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
