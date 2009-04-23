package is.idega.idegaweb.egov.bpm.xform.bean;
import java.util.List;

import com.idega.util.text.Item;
/**
 * @author <a href="mailto:arunas@idega.com">Arūnas Vasmanas</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2009/04/23 08:11:01 $ by $Author: arunas $
 */

public interface XformBPM {
	
	public abstract List<Item> getUsersConnectedToProcess(String pid);
	
	public abstract List<Item> getUsersConnectedToProcessEmails(String pid);
	
	public abstract List<Item> getProcessAttachments(String pid);
	
	public abstract boolean hasProcessAttachments(String pid);

}
