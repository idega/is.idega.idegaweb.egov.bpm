package is.idega.idegaweb.egov.bpm.bean;

import java.util.List;

import org.directwebremoting.annotations.DataTransferObject;
import org.directwebremoting.annotations.RemoteProperty;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.file.FileDownloadNotificationProperties;
import com.idega.dwr.business.DWRAnnotationPersistance;

@DataTransferObject
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class BPMAttachmentDownloadNotificationProperties extends FileDownloadNotificationProperties implements DWRAnnotationPersistance {

	private static final long serialVersionUID = 7394589589451806343L;

	@RemoteProperty
	private Long taskId;
	@RemoteProperty
	private Integer hash;
	
	public Long getTaskId() {
		return taskId;
	}
	public void setTaskId(Long taskId) {
		this.taskId = taskId;
	}
	public Integer getHash() {
		return hash;
	}
	public void setHash(Integer hash) {
		this.hash = hash;
	}
	
	@Override
	@RemoteProperty
	public String getFile() {
		return super.getFile();
	}
	
	@Override
	@RemoteProperty
	public List<String> getUsers() {
		return super.getUsers();
	}
	
	@Override
	@RemoteProperty
	public String getServer() {
		return super.getServer();
	}
	
	@Override
	@RemoteProperty
	public String getUrl() {
		return super.getUrl();
	}
	
}