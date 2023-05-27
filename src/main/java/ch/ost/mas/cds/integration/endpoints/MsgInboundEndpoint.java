package ch.ost.mas.cds.integration.endpoints;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

import ch.ost.mas.cds.integration.base.IProcessor;
import ch.ost.mas.cds.integration.base.MSGDEST;
import ch.ost.mas.cds.integration.base.MSGTYPE;
import ch.ost.mas.cds.integration.util.JMSUtil;

public class MsgInboundEndpoint implements IProcessor, MessageListener {
	
	private MessageConsumer 				mConsumer;
	private MSGDEST							mDestType;
	private MSGTYPE							mMsgTypeAccepted;
	private	String							mDestinationName;
	private Session							mSession;
	private Connection						mConnection;
	private Destination						mDestination;
	private long							mMsgReceived;
	private ConcurrentLinkedQueue<Message> 	mQueue;
	
	public  MsgInboundEndpoint(String pDestination, MSGDEST pDestType, MSGTYPE pMsgType) {
		mDestinationName = pDestination;
		mDestType = pDestType;
		mMsgTypeAccepted = pMsgType;
		mQueue = new ConcurrentLinkedQueue<>();
	}
	
	@Override
	public boolean canHandle(Message pMsg) {
		// Inbound Endpoint only returns messages
		return (mQueue.size() > 0);
	}

	@Override
	public Message process(Message pMsg) {
		Message retMsg = null; 
		if (mQueue.size() > 0) {
			try {
				retMsg = mQueue.poll();
			} catch (Exception pEx) {
				System.err.printf("Cannot receive a message from %s err=%s\n", mDestinationName, pEx.getMessage());
			}
		}
		return retMsg;
	}

	@Override
	public void onMessage(Message pMsg) {
		try {
			String msgType = pMsg.getJMSType();
			MSGTYPE type = null;
			if ((msgType != null) && (msgType.trim().length() > 0)) {
				try {
					type = MSGTYPE.valueOf(msgType.trim().toUpperCase());
				} catch (Exception pEx) {
					type = null; 
				}
			}
			if ((mMsgTypeAccepted == null) || (type == mMsgTypeAccepted)) {
				mQueue.add(pMsg);
				mMsgReceived++;
			} else {
				System.err.printf("Unexpected message %s arrived. Discard it\n", pMsg.getClass().getSimpleName());
			}
			pMsg.acknowledge();
		} catch (Exception pEx) {
			System.err.printf("Cannot handle message %s err=%s\n", pMsg != null ? pMsg.getClass().getSimpleName(): "Message == null", pEx.getMessage());
			try {
				if (pMsg != null) {
					pMsg.acknowledge();
				}
			} catch (JMSException pEx1) {
				System.err.printf("Cannot acknowlede the  message %s err=%s\n", pMsg.getClass().getSimpleName(), pEx1.getMessage());
			}
		}
	}
	
	@Override
	public boolean start() {
		boolean ret = true;
		if (mDestType == MSGDEST.QUEUE) {
			try {
				QueueConnectionFactory queueConnectionFactory = JMSUtil.getInstance().getQueueConnectionFactory();
				QueueConnection queueConnection = queueConnectionFactory.createQueueConnection();
				QueueSession queueSession = queueConnection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);

				Queue queue = queueSession.createQueue(mDestinationName);
				QueueReceiver receiver = queueSession.createReceiver(queue);
				receiver.setMessageListener(this); 
				queueConnection.start();
				mDestination = queue;
				mConsumer = receiver;
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
				Topic topic = topicSession.createTopic(mDestinationName);
				TopicSubscriber topicSubscriber = topicSession.createSubscriber(topic);
				topicSubscriber.setMessageListener(this);
				topicConnection.start();
				mDestination = topic;
				mConsumer = topicSubscriber;
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
		} catch (Exception pEx) {
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
		  .append(" #MsgRcvd=").append(mMsgReceived);

		return sb.toString();
	}
}
