package is.idega.idegaweb.egov.bpm.xform.bean;
import java.util.List;

import com.idega.user.data.User;
import com.idega.util.text.Item;
/**
 * @author <a href="mailto:arunas@idega.com">Arūnas Vasmanas</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/12/16 07:02:10 $ by $Author: arunas $
 */

public interface XformBPM {
	
	public abstract List<Item> getUsersConnectedToProcess(String pid);
	
	public abstract List<User> getUsersConnecetedList(Long pid);


}
