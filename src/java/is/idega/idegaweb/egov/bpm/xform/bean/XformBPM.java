package is.idega.idegaweb.egov.bpm.xform.bean;
import java.util.List;

import com.idega.util.text.Item;
/**
 * @author <a href="mailto:arunas@idega.com">Arūnas Vasmanas</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/12/16 17:33:30 $ by $Author: arunas $
 */

public interface XformBPM {
	
	public abstract List<Item> getUsersConnectedToProcess(String pid);

}
