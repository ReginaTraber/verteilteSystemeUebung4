package ch.ost.mas.cds.integration.translator;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;

import ch.ost.mas.cds.integration.base.AbstractProcessor;
import ch.ost.mas.cds.integration.base.MSGTYPE;
import ch.ost.mas.cds.integration.base.WPARAM;
import ch.ost.mas.cds.integration.util.MessageFactory;

/**
 * Alarm filter observes the value
 * defined and sends alarm if it goes over/under threshold
 * Example: If mParam = WPARAM.OUTTEMP, UpperLimit = 30.0C, LowerLimit 0.0C
 *          and value = 30.1 C or  -0.5C alarm is set to true
 *          and a message is generated
 *
 */
public class AlarmFilter 
	   extends AbstractProcessor {
	
	private WPARAM	mParam;
	private Double  mUpperLimit;
	private Double  mLowerLimit;
	private boolean mAlarmOn;
	
	public AlarmFilter(WPARAM pParm, Double pUpperLimit, Double pLowerLimit) {
		mParam = pParm;
		mUpperLimit = pUpperLimit;
		mLowerLimit = pLowerLimit;
	}
	
	@Override
	public boolean canHandle(Message pMsg) {
		return testMsgtType(pMsg, MSGTYPE.WRECNORM);
	}

	@Override
	public Message process(Message pMsg)  {
		Message rmsg = null; 
		if ((pMsg != null) && (canHandle(pMsg))) {
			try {
				MapMessage imsg = (MapMessage) pMsg;
				Double value = imsg.getDouble(mParam.name());
				if (((mUpperLimit != null) && (mUpperLimit <= value)) || 
						((mLowerLimit != null) && (mLowerLimit > value))) {
					if (!mAlarmOn) {
						MapMessage xmsg = MessageFactory.getInstance().create(MSGTYPE.WALARM);
						xmsg.setString(WPARAM.DATETIME.name(), imsg.getString(WPARAM.DATETIME.name()));
						xmsg.setDouble(mParam.name(), value);
						mAlarmOn = true; // Avoid more than one alarm per bypassing threshold
						rmsg = xmsg;
					}
				} else {
					mAlarmOn = false;
				}
			} catch (JMSException pEx) {
				System.err.printf("%s: parameter error for %s \n",  getClass().getSimpleName(), mParam.name());
			}
		} 
		return rmsg;
	}
	
	@Override
	public String toString() {
	    return new StringBuilder(getClass().getSimpleName())
	            .append(" for ").append(mParam.name()).append(" [")
	            .append((mLowerLimit != null)? mLowerLimit.toString() : "--").append(',')
	            .append((mUpperLimit != null)? mUpperLimit.toString() : "--").append(']')
	            .toString();
	}
}
