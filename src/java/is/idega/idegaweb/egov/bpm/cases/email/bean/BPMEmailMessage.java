package is.idega.idegaweb.egov.bpm.cases.email.bean;

import java.io.InputStream;
import java.util.Map;

import com.idega.core.messaging.EmailMessage;

/**
 * Simple bean holding needed information
 *
 * @author <a href="mailto:valdas@idega.com">Valdas Žemaitis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/04/22 12:56:21 $ by $Author: valdas $
 */
public class BPMEmailMessage extends EmailMessage {

	private Long processInstanceId;
	private Long taskInstanceId;

	private Map<String, InputStream> attachments;

	public BPMEmailMessage() {
		super();
	}

	public BPMEmailMessage(Long processInstanceId) {
		this();

		this.processInstanceId = processInstanceId;
	}

	public BPMEmailMessage(EmailMessage message, Long processInstanceId) {
		super(message);

		setProcessInstanceId(processInstanceId);
	}

	protected BPMEmailMessage(BPMEmailMessage message) {
		super(message);

		setProcessInstanceId(message.getProcessInstanceId());
		setTaskInstanceId(message.getTaskInstanceId());
	}

	public BPMEmailMessage(Long processInstanceId, Long taskInstanceId) {
		this(processInstanceId);

		this.taskInstanceId = taskInstanceId;
	}

	public Long getProcessInstanceId() {
		return processInstanceId;
	}
	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}
	public Long getTaskInstanceId() {
		return taskInstanceId;
	}
	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new BPMEmailMessage(this);
	}

	public Map<String, InputStream> getAttachments() {
		return attachments;
	}

	@Override
	public void setAttachments(Map<String, InputStream> attachments) {
		this.attachments = attachments;
	}

	@Override
	public String toString() {
		return super.toString() + ". BPM process instance ID: " + getProcessInstanceId() + ", task instance ID: " + getTaskInstanceId() +
				", attachments: " + getAttachments();
	}
}