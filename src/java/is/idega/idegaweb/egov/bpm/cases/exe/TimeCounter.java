package is.idega.idegaweb.egov.bpm.cases.exe;

import com.idega.util.CoreConstants;
import com.idega.util.IWTimestamp;

public class TimeCounter {

	private IWTimestamp time;
	private int next;
	
	public TimeCounter(long time,int lastNumber) {
		next = lastNumber + 1;
		IWTimestamp newTime = new IWTimestamp(time);
		setTime(newTime);
	}
	private void setTime(IWTimestamp time) {
		time.setAsDate();
		this.time = time;
	}
	
	public int getNextCounter() {
		IWTimestamp now = IWTimestamp.RightNow();
		now.setAsDate();
		if(now.equals(time)) {
			return next++;
		}
		setTime(now);
		next = 1;
		return 0;
	}
	
	public String getDateString() {
		return time.getYear() 
				+ CoreConstants.MINUS 
				+ (time.getMonth() < 10 ? "0"+time.getMonth() : time.getMonth())
				+ CoreConstants.MINUS 
				+ (time.getDay() < 10 ? "0"+time.getDay() : time.getDay());
	}

}
