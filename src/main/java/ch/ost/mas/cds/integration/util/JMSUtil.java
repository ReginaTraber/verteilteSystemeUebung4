/*
** (c) MAS/SE Distributed Computing 2002-2021
*/

package ch.ost.mas.cds.integration.util;

import javax.jms.QueueConnectionFactory;
import javax.jms.TopicConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;

public class JMSUtil {
	public final static String HOSTKEY 		= "-host";
	public final static String PORTKEY 		= "-port";
	public final static String JMSPROV 		= "-jmsprov";
	public final static String DESTINATION 	= "-dest";
	public final static String COUNT	 	= "-count";
	public final static String ACTIVEMQ 	= "activemq";

	private QueueConnectionFactory mQueConnFactory = null;
	private TopicConnectionFactory mTopicConnFactory = null;

	private static String[] mArgs;
	private static JMSUtil instance; 

	public static JMSUtil getInstance()  {
		if (instance == null) {
			throw new NullPointerException("JMSUtil is not yet initialized");
		}
		return instance;
	}
	
	public static void init(String[] pArgs) {
		if (instance == null) {
	            synchronized (JMSUtil.class) {
	                if (instance == null) {
	                	instance = new JMSUtil(pArgs);
	                }
	            }
		}
	}
	
	/**
	 * Method Implementation
	 */
	private JMSUtil(String[] pArgs) {
		mArgs = pArgs;
	}

	/**
	 * Initialize the Queue Connection Factory
	 */
	public QueueConnectionFactory getQueueConnectionFactory() {
		int portval;
		String host = null;
		String jmsprovider = null;

		portval = findArgVal(PORTKEY);
		host = findArg(HOSTKEY, true);
		jmsprovider = findArg(JMSPROV, true);

		if (mQueConnFactory != null)
			return (mQueConnFactory);

		if (host == null)
			host = "localhost";

		try {
			// Vendor specific initialization
			if ((jmsprovider == null) || (jmsprovider.equals(ACTIVEMQ))) {
				StringBuffer broker = new StringBuffer("tcp://");
				broker.append(host);
				broker.append(":");
				if (portval > 0) {
					broker.append(portval);
				} else {
					broker.append(61616); // Default ActiveMq
				}

				System.err.println("Connecting to ACTIVEMQ with " + broker);
				try {
					mQueConnFactory = new ActiveMQConnectionFactory(broker.toString());
				} catch (Exception pEx) {
					System.err.println("Got a naming exception when looking up QueueConnectionFactory: " + pEx);
				}
			}

		} catch (Exception pEx) {
			System.err.println("Got an exception when looking up QueueConnectionFactory: " + pEx);
		}
		return (mQueConnFactory);
	}

	/**
	 * Initialize the Topic Connection Factory
	 */
	public TopicConnectionFactory getTopicConnectionFactory() {
		int portval;
		String host = null;
		String jmsprovider = null;

		portval = findArgVal(PORTKEY);
		host = findArg(HOSTKEY, true);
		jmsprovider = findArg(JMSPROV, true);

		if (mTopicConnFactory != null) {
			return (mTopicConnFactory);
		}

		if (host == null) {
			host = "localhost";
		}

		try {
			// Vendor specific initialization
			if ((jmsprovider == null) || (jmsprovider.equals(ACTIVEMQ))) {

				StringBuffer broker = new StringBuffer("tcp://");
				broker.append(host);
				broker.append(":");
				if (portval > 0) {
					broker.append(portval);
				} else {
					broker.append(61616); // Default ActiveMq
				}

				System.err.println("Connecting to ACTIVEMQ with " + broker);
				try {
					mTopicConnFactory = new ActiveMQConnectionFactory(broker.toString());
				} catch (Exception pEx) {
					System.err.println("Got a naming exception when looking up TopicConnectionFactory: " + pEx);
				}
			}

		} catch (Exception pEx) {
			System.err.println("Error occcured: " + pEx);
		}
		return (mTopicConnFactory);
	}

	/**
	 * findArg() finds the argument with pKey and returns the following argument
	 * or just the key.
	 */
	public String findArg(String pKey, boolean pArgFollows) {
		String str = null;
		for (int i = 0; i < mArgs.length; i++) {
			if (mArgs[i].equals(pKey)) {
				if ((pArgFollows) && (++i < mArgs.length))
					str = mArgs[i];
				else
					str = pKey;
				break;
			}
		}
		return (str);
	}

	/**
	 * findArgVal() finds the argument with pKey and returns the following
	 * argument or just the key.
	 */
	public int findArgVal(String pKey) {
		String str = null;
		int val = -1;
		try {
			str = findArg(pKey, true);
			if (str != null) {
				val = Integer.parseInt(str);
			} else {
				// System.err.println("Argument " + pKey + " not found ");
			}
		} catch (Exception pEx) {
			System.err.println("Argument " + pKey + " should be a value:" + pEx);
			val = 0;
		}
		return (val);
	}
	
	/**
	 * Get current user name and normalize it
	 * @return String normalized (without blanks) user name
	 */
	public String getUserName() {
	    String userName = System.getProperty("user.name");
	    if ((userName == null) || (userName.trim().isEmpty())) {
	        userName = "nobody";
	    } else {
	        userName = userName.replace(' ', '_');
	    }
	    return userName;
	}
	
	public String getDestination() {
		return getDestination(null);
	}
	
	public String getDestination(String pPrefix) {
		String destination = findArg(DESTINATION, true);
		String prefix = (pPrefix != null) ? pPrefix : "";
		return (destination != null) ? destination : prefix.concat(getUserName().trim().toLowerCase()).concat(".Request");
	}
}