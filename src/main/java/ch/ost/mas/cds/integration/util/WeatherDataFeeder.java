/*
**  MAS/SE Comunication in Distributed Systems
**  Uebung: Integration concepts of a service bus
*/
package ch.ost.mas.cds.integration.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;

import ch.ost.mas.cds.integration.base.MSGTYPE;

/**
 * Weather data feeder
 * Publishes a weather record every 1 second which is 
 * 300 times faster than in real
 * IMPORTANT: Do not start this service in class
 * WICHTIG: Diesen Service nicht im Kursraum starten
 * use: -host masseamq.mywire.org -inp data/weatherdata_2016.csv  -feedinterval 5000
 * where: 
 * -feedinterval in ms (minimal 100 ms otherwise default of 1 sec) 
 * -inp data file to read records from 
 */
public class WeatherDataFeeder {
	
	private final static long	FEED_INTERVAL = 1000L; // Time in ms for which a 5-minute record is fed 
	
	private JMSUtil mJMSUtil;
	private String mTopicName;
	private TopicPublisher mPublisher;
	private TopicSession   mTopicSession;
	private TopicConnection mTopicConnection;

	public WeatherDataFeeder() {
	}

	private void setup(String[] pArgs) {
		try {
			JMSUtil.init(pArgs);
			mJMSUtil = JMSUtil.getInstance();
			String topicName = mJMSUtil.findArg("-dest", true);
			Topic topic = null; 
			TopicConnectionFactory topicConnectionFactory = mJMSUtil.getTopicConnectionFactory();

			mTopicConnection = topicConnectionFactory.createTopicConnection();
			mTopicSession = mTopicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
			mTopicName = topicName != null ? topicName : "T.CDS.WeatherDataEvents";

			topic = mTopicSession.createTopic(mTopicName);
			mPublisher = mTopicSession.createPublisher(topic);

			mTopicConnection.start();
		} catch (Exception pEx) {
			System.err.printf("Error when starting the producer err=%s\n",  pEx.getMessage());
		}
	}
	
	private void stop() {
		try {
			mTopicSession.close();
			mTopicConnection.stop();
		} catch (JMSException pEx) {
			System.err.printf("Error when shutting down err=%s\n",  pEx.getMessage());
		}
	}
	
	private void run() {
		String inpFile = mJMSUtil.findArg("-inp", true);
		long  feedinterval = mJMSUtil.findArgVal("-feedinterval");
		boolean forEver = mJMSUtil.findArg("-forever", false) != null;
		boolean atLeastOnce = true;
		if (feedinterval < 100) {
			feedinterval = FEED_INTERVAL;
		} 
		while ((forEver) || (atLeastOnce)) {
			try (BufferedReader input = new BufferedReader(new FileReader(inpFile))) {
				String line = null;
				while ((line = input.readLine()) != null) {
					if ((line.trim().length() > 0) && (Character.isDigit(line.charAt(0)))) {
						TextMessage msg = mTopicSession.createTextMessage(line.trim());
						msg.setJMSType(MSGTYPE.WRECRAW.name());
						mPublisher.publish(msg);
						try {
							Thread.sleep(feedinterval);
						} catch (InterruptedException pEx) {
							// ignore weather influence ;-) 
						}
					}
				}
				System.out.printf("Ending publishing weather data, no data left. %s\n", forEver ? "Rewind to begin!" : "Exit WeatherDataFeeder");
			} catch(IOException pEx) {
				System.err.println("Got exception (exit): " + pEx);
				forEver = false; 
			} catch (JMSException pEx) {
				System.err.println("Got a JMS exception: " + pEx);
			} finally {
				atLeastOnce = false;
			}
		} 
	}

	public static void main(String[] pArgs) {
		WeatherDataFeeder app = new WeatherDataFeeder();
		app.setup(pArgs);
		app.run();
		app.stop();
		System.exit(0);
	}
}
