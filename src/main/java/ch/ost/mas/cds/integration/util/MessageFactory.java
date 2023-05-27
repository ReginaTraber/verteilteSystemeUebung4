package ch.ost.mas.cds.integration.util;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;

import ch.ost.mas.cds.integration.base.MSGTYPE;

public final class MessageFactory {

	public final static String  STUDENT_EMAIL = "STUDENT_EMAIL";
	
	private static MessageFactory sInstance;
	
	private Session 	mSession;
	private Connection 	mConnection;
	private String		mStudentEMail; 
	
	private MessageFactory() {
		
	}
	
	public static MessageFactory getInstance() {
		if (sInstance == null) {
			synchronized (JMSUtil.class) {
				if (sInstance == null) {
					MessageFactory msgFact = new MessageFactory();
					msgFact.start();
					sInstance = msgFact;
				}
			}
		}
		return sInstance;
	}
	
	public void setStudentEMail(String pEmailAddress) {
		mStudentEMail = pEmailAddress;
	}
	
	public boolean start() {
		boolean ret = true;
		try {
			QueueConnectionFactory queueConnectionFactory = JMSUtil.getInstance().getQueueConnectionFactory();
			QueueConnection conn = queueConnectionFactory.createQueueConnection();
			mSession = conn.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
			mConnection = conn;
			mConnection.start();
		} catch (JMSException pEx) {
			System.err.printf("Error starting MessageFactory err=%s\n", pEx.getMessage());
			ret = false;
		}
		return ret;
	}
	
	public boolean stop() {
		boolean ret = true;
		try {
			mSession.close();
			mConnection.stop();
		} catch (JMSException pEx) {
			ret = false;
		}
		return ret;
	}
	
	/**
	 * Creates a message of designated type
	 * @param pMsgType MSGTYPE type of message to create
	 * @return <T extends Message> created message of null if not used
	 */
	@SuppressWarnings("unchecked")
	public <T extends Message> T create(MSGTYPE pMsgType) {
		T ret = null; 
		if (pMsgType != null) {
			try {
				switch(pMsgType) {
				case WALARM: 
				case WAVGMSG:
				case WRECNORM:  {
					ret = (T) mSession.createMapMessage();
					break; 
				}
				case WRECRAW:
				case TEXT:
				default: {
					ret = (T) mSession.createTextMessage();
					break;
				}
				}
				ret.setJMSType(pMsgType.name());
			} catch (Exception pEx) {
				System.err.printf("Error creating message for type %s err=%s\n", pMsgType.name(), pEx.getMessage());
			}
		}
		try {
			if (mStudentEMail != null) {
				ret.setStringProperty(STUDENT_EMAIL, mStudentEMail);
			}
		} catch (JMSException pEx) {
			// Ignore
		}
		return ret;
	}
	
}
