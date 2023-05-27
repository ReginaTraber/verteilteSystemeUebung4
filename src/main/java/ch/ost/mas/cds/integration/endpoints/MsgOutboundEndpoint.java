package ch.ost.mas.cds.integration.endpoints;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;

import ch.ost.mas.cds.integration.base.AbstractProcessor;
import ch.ost.mas.cds.integration.base.MSGDEST;
import ch.ost.mas.cds.integration.base.MSGTYPE;
import ch.ost.mas.cds.integration.util.JMSUtil;

public class MsgOutboundEndpoint extends AbstractProcessor {

	private MessageProducer 				mProducer;
	private MSGDEST							mDestType;
	private MSGTYPE							mMsgTypeAccepted;
	private	String							mDestinationName;
	private Session							mSession;
	private Connection						mConnection;
	private Destination						mDestination;
	private long							mMsgSent;
	
	public  MsgOutboundEndpoint(String pDestination, MSGDEST pDestType, MSGTYPE pMsgType) {
		mDestinationName = pDestination;
		mDestType = pDestType;
		mMsgTypeAccepted = pMsgType;
	}

	
	@Override
	public boolean canHandle(Message pMsg) {
		return testMsgtType(pMsg, mMsgTypeAccepted);
	}

	/**
	 * Send message to destination
	 */
	@Override
	public Message process(Message pMsg) {
		String destination = "not defined";
		try {
			destination = mDestinationName == null ? pMsg.getJMSDestination().toString() : mDestinationName;
			if (mDestination != null) {
				mProducer.send(pMsg);
			} else {
				mProducer.send(pMsg.getJMSDestination(), pMsg);
			}
			mMsgSent++;
		} catch (Exception pEx) {
			System.err.printf("Error sending message %s to %s err=%s\n", pMsg.toString(), destination, pEx.getMessage());
		}
		return null; 
	}

	@Override
	public boolean start() {
		boolean ret = true;
		if (mDestType == MSGDEST.QUEUE) {
			try {
				QueueConnectionFactory queueConnectionFactory = JMSUtil.getInstance().getQueueConnectionFactory();
				QueueConnection queueConnection = queueConnectionFactory.createQueueConnection();
				QueueSession queueSession = queueConnection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
				Queue queue = null; 
				QueueSender sender = null; 
				
				if (mDestinationName != null) {
					queue = queueSession.createQueue(mDestinationName);
				}
				sender = queueSession.createSender(queue);
				queueConnection.start();
				mDestination = queue;
				mProducer = sender;
				mSession = queueSession;
				mConnection = queueConnection;
			} catch (Exception pEx) {
				System.err.printf("Error starting InboundEndpoint(Queue) err=%s\n", pEx.getMessage());
				ret = false;
			}
		} else {
			try {
				TopicConnectionFactory topicConnectionFactory = JMSUtil.getInstance().getTopicConnectionFactory();
				TopicConnection topicConnection = topicConnectionFactory.createTopicConnection();
				TopicSession topicSession = topicConnection.createTopicSession(false, Session.CLIENT_ACKNOWLEDGE);
				Topic topic = null; 
				TopicPublisher topicSubscriber = null;
				
				if (mDestinationName != null) {
					topic = topicSession.createTopic(mDestinationName);
				} 
				topicSubscriber = topicSession.createPublisher(topic);
				
				topicConnection.start();
				mDestination = topic;
				mProducer = topicSubscriber;
				mSession = topicSession;
				mConnection = topicConnection;
			} catch (Exception pEx) {
				System.err.printf("Error starting InboundEndpoint (Topic) err=%s\n", pEx.getMessage());
				ret = false;
			}

		}
		return ret;
	}

	@Override
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


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[").append(getClass().getSimpleName()).append("] ")
		  .append("DestType=").append(mDestType)
		  .append(" MsgTypeAccepted=").append(mMsgTypeAccepted)
		  .append(" DestinationName=").append(mDestinationName)
		  .append(" #MsgSent=").append(mMsgSent);

		return sb.toString();
	}

	
}
