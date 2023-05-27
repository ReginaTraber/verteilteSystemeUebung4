package ch.ost.mas.cds.integration.base;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jms.JMSException;
import javax.jms.Message;

public abstract class AbstractProcessor implements IProcessor {
	private static final String WV_DATE_TIME_FORMAT = "yyyyMMdd HH:mm";

	protected boolean testMsgtType(Message pMsg, MSGTYPE pMsgType) {
		boolean ret = false;
		MSGTYPE mt = null; 
		if (pMsg != null) {
			try {
				mt = MSGTYPE.typeOf(pMsg.getJMSType());
				ret = ((pMsgType != null) && (mt != null) && (mt == pMsgType));
			} catch (JMSException pEx) {
				ret = false;
			}
		}
		return ret;
	}
	
	protected Date parseDate(String pDateTime) {
		Date ts;
		try {
			ts = new SimpleDateFormat(WV_DATE_TIME_FORMAT).parse(pDateTime);
		} catch (ParseException pEx) {
			ts = null; 
		}
		return ts;
	}
	
	protected String formatDate(Date pDateTime) {
		return new SimpleDateFormat(WV_DATE_TIME_FORMAT).format(pDateTime);
	}
	
	@Override
	public boolean start() {
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}

	@Override
	public String toString() {
		return "["  + getClass().getSimpleName() +  "]";
	}
	
	
}
