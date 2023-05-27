package ch.ost.mas.cds.integration.translator;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;

import ch.ost.mas.cds.integration.base.AVGPERIOD;
import ch.ost.mas.cds.integration.base.AbstractProcessor;
import ch.ost.mas.cds.integration.base.MSGTYPE;
import ch.ost.mas.cds.integration.base.WPARAM;
import ch.ost.mas.cds.integration.util.MessageFactory;

public class AverageCollector 
	  extends AbstractProcessor {

	private WPARAM		mParam;
	private AVGPERIOD 	mCompressPeriod;
	private int	     	mValueCount;
	private Double		mSum;
	private boolean 	mInProgress;
	private Calendar    mStartPeriod;
	
	public AverageCollector(WPARAM pParm, AVGPERIOD pCompressPeriod) {
		mParam = pParm;
		mCompressPeriod = pCompressPeriod;
	}

	
	@Override
	public boolean canHandle(Message pMsg) {
		return testMsgtType(pMsg, MSGTYPE.WRECNORM);
	}

	@Override
	public Message process(Message pMsg) {
		MapMessage imsg = (MapMessage) pMsg;
		Message rmsg = null; 
		if ((pMsg != null) && (canHandle(pMsg))) {
			try {
				Date ts = parseDate(imsg.getString(WPARAM.DATETIME.name()));
				Calendar cal = GregorianCalendar.getInstance(); 
				cal.setTime(ts);
				Double value = imsg.getDouble(mParam.name());
				int seconds = cal.get(Calendar.SECOND) + 
						(cal.get(Calendar.MINUTE) * 60) +  
						(mCompressPeriod == AVGPERIOD.DAY ?  (cal.get(Calendar.HOUR_OF_DAY)* 3600):0);
				if (mInProgress) {
					if (seconds == 0) {
						if (mValueCount > 0) {
							MapMessage xmsg = MessageFactory.getInstance().create(MSGTYPE.WAVGMSG);
							xmsg.setDouble(mParam.name(), mSum / mValueCount);
							xmsg.setString(WPARAM.DATETIME.name(),  formatDate(mStartPeriod.getTime()));
							rmsg = xmsg;
						}
						mValueCount = 1; 
						mSum = value; 
						mStartPeriod = cal; 
					} else {
						mSum += value;
						mValueCount++;
					} 
				} else if (seconds == 0) {
					mInProgress = true;
					mValueCount = 1; 
					mSum = value; 
					mStartPeriod = cal; 
				}
			} catch (JMSException pEx) {
				System.err.printf("%s: parameter error for %s \n",  getClass().getSimpleName(), mParam.name());
			}
		}
		return rmsg;
	}
}
