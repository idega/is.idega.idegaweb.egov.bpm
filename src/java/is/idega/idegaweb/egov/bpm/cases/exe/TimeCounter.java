package is.idega.idegaweb.egov.bpm.cases.exe;

import com.idega.util.CoreConstants;
import com.idega.util.IWTimestamp;

public class TimeCounter {

	private int resetInterval;
	private IWTimestamp time;
	private int next;
	private long timestamp;
	private int maxGeneratorValue;

	public TimeCounter(int resetInterval, long time, int lastNumber, int maxGeneratorValue) {
		this.resetInterval = resetInterval;
		this.timestamp = time;
		this.maxGeneratorValue = maxGeneratorValue;

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

		switch (resetInterval) {
		case 365:
			IWTimestamp yearAgo = new IWTimestamp();
			yearAgo.setYear(yearAgo.getYear() - 1);
			long timestampYearAgo = yearAgo.getTime().getTime();
			if (timestampYearAgo > timestamp || (next + 1 > maxGeneratorValue)) {
				//	Reseting counter
				next = 1;
				timestamp = new IWTimestamp().getTime().getTime();
			}

			//	Making sure current timestamp will be used for identifier
			IWTimestamp newTime = new IWTimestamp();
			setTime(newTime);

			return next++;

		default:
			if (now.equals(time)) {
				return next++;
			}

			break;
		}

		setTime(now);
		next = 2;
		return 1;
	}

	public String getDateString() {
		return time.getYear()
				+ CoreConstants.MINUS
				+ (time.getMonth() < 10 ? "0"+time.getMonth() : time.getMonth())
				+ CoreConstants.MINUS
				+ (time.getDay() < 10 ? "0"+time.getDay() : time.getDay());
	}

}