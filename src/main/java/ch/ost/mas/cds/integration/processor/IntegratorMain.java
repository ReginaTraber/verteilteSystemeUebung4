package ch.ost.mas.cds.integration.processor;

import java.util.ArrayList;
import java.util.List;

import javax.jms.Message;

import ch.ost.mas.cds.integration.base.AVGPERIOD;
import ch.ost.mas.cds.integration.base.IProcessor;
import ch.ost.mas.cds.integration.base.MSGDEST;
import ch.ost.mas.cds.integration.base.MSGTYPE;
import ch.ost.mas.cds.integration.base.WPARAM;
import ch.ost.mas.cds.integration.control.MultiDestinationRouter;
import ch.ost.mas.cds.integration.control.ProcessSequence;
import ch.ost.mas.cds.integration.endpoints.ConsoleLogger;
import ch.ost.mas.cds.integration.endpoints.MsgInboundEndpoint;
import ch.ost.mas.cds.integration.endpoints.MsgOutboundEndpoint;
import ch.ost.mas.cds.integration.translator.AlarmFilter;
import ch.ost.mas.cds.integration.translator.AverageCollector;
import ch.ost.mas.cds.integration.translator.WeatherRecordTransformer;
import ch.ost.mas.cds.integration.util.JMSUtil;
import ch.ost.mas.cds.integration.util.MessageFactory;

public class IntegratorMain {
	private static final String ALARM_DEST = "Q.ALARM.Events";
	private static final String AVG_HOUR_DEST = "Q.AVG_HOUR.Events";
	private static final String AVG_DAY_DEST = "Q.AVG_DAY.Events";
	private static final String CONV_WRECS = "Q.WREC_MAP.Events";
	private static final String INP_WRECS = "T.CDS.WeatherDataEvents";

	private JMSUtil mJMSUtil;
	private MessageFactory mMessageFactory;

	private MsgInboundEndpoint mInp;
	private MsgInboundEndpoint mConvertedMsgRecordsIn;
	private MsgOutboundEndpoint mAvgHourOut;
	private MsgOutboundEndpoint mAvgDayOut;
	private MsgOutboundEndpoint mAlarmOut;
	private MsgOutboundEndpoint mConvertedMsgRecordsOut;

	private AlarmFilter mLowTempFilter;
	private AlarmFilter mHighWindFilter;
	private AverageCollector mHourTempAvgColl;
	private AverageCollector mDayHumidAvgColl;
	private WeatherRecordTransformer mWRecTransformer;
	private ConsoleLogger mConsoleLogger;

	private List<ProcessSequence> mFlows;

	// new objects for exercise
	private MultiDestinationRouter mMultiDestinationRouter;
	private MsgInboundEndpoint mAlarmIn;

	private void setup(String[] pArgs) {
		try {
			JMSUtil.init(pArgs);
			mJMSUtil = JMSUtil.getInstance();
			mFlows = new ArrayList<ProcessSequence>();
			setupEndpoints(pArgs);
			setupTranslators(pArgs);
			setupFilters(pArgs);
			setupFlows(pArgs);
		} catch (Exception pEx) {
			System.err.printf("Error when setting up the system err=%s\n", pEx.getMessage());
		}
	}

	/**
	 * Setup needed endpoints according to the passed arguments or the defaults otherwise
	 * 
	 * @param pArgs String[] command line arguments to parse
	 */
	protected void setupEndpoints(String[] pArgs) {
		String input = mJMSUtil.findArg("-inp", true);
		String avghourout = mJMSUtil.findArg("-avghourout", true);
		String avgdayout = mJMSUtil.findArg("-avgdayout", true);
		String alarm = mJMSUtil.findArg("-alarmout", true);
		String studmailaddress = mJMSUtil.findArg("-studemail", true);
		mMessageFactory = MessageFactory.getInstance();
		mMessageFactory.setStudentEMail(studmailaddress);

		mInp = new MsgInboundEndpoint(input != null ? input : INP_WRECS, MSGDEST.TOPIC, MSGTYPE.WRECRAW);

		mConvertedMsgRecordsOut = new MsgOutboundEndpoint(
				String.join("_", CONV_WRECS, System.getProperty("user.name")), MSGDEST.QUEUE, MSGTYPE.WRECNORM);
		mConvertedMsgRecordsIn = new MsgInboundEndpoint(
				String.join("_", CONV_WRECS, System.getProperty("user.name")), MSGDEST.QUEUE, MSGTYPE.WRECNORM);

		mAvgHourOut = new MsgOutboundEndpoint(
				avghourout != null ? avghourout
						: String.join("_", AVG_HOUR_DEST, System.getProperty("user.name")),
				MSGDEST.QUEUE, MSGTYPE.WAVGMSG);
		mAvgDayOut = new MsgOutboundEndpoint(
				avgdayout != null ? avgdayout : String.join("_", AVG_DAY_DEST, System.getProperty("user.name")),
				MSGDEST.QUEUE, MSGTYPE.WAVGMSG);
		mAlarmOut = new MsgOutboundEndpoint(
				alarm != null ? alarm : String.join("_", ALARM_DEST, System.getProperty("user.name")),
				MSGDEST.QUEUE, MSGTYPE.WALARM);
		mConsoleLogger = new ConsoleLogger();

		// New instanciation for Exercise Alarm input
		mAlarmIn = new MsgInboundEndpoint(
				alarm != null ? alarm : String.join("_", ALARM_DEST, System.getProperty("user.name")),
				MSGDEST.QUEUE, MSGTYPE.WALARM);
	}

	protected void setupTranslators(String[] pArgs) {
		mWRecTransformer = new WeatherRecordTransformer();
	}

	protected void setupFilters(String[] pArgs) {
		// create other transformer and filter as needed
		mHighWindFilter = new AlarmFilter(WPARAM.WINDGUST, 30.0, null);
		mLowTempFilter = new AlarmFilter(WPARAM.OUTTEMP, null, 0.0);
		mDayHumidAvgColl = new AverageCollector(WPARAM.OUTHUMIDITY, AVGPERIOD.DAY);
		mHourTempAvgColl = new AverageCollector(WPARAM.OUTTEMP, AVGPERIOD.HOUR);
	}

	/**
	 * Setup the process(es) to be run and add all processes to the process list
	 * 
	 * @param pArgs
	 */
	protected void setupFlows(String[] pArgs) {
		// flow definition
		mFlows.add(new ProcessSequence(mInp, mWRecTransformer, mConvertedMsgRecordsOut));
		// multi destinaton rerouter
		mMultiDestinationRouter = new MultiDestinationRouter(
				new ProcessSequence(mLowTempFilter, mAlarmOut), new ProcessSequence(mHighWindFilter, mAlarmOut),
				new ProcessSequence(mHourTempAvgColl, mAvgHourOut),
				new ProcessSequence(mDayHumidAvgColl, mAvgDayOut));
		mFlows.add(new ProcessSequence(mConvertedMsgRecordsIn, mMultiDestinationRouter));
		// alarm to console logger
		mFlows.add(new ProcessSequence(mAlarmIn, mConsoleLogger));
	}

	private boolean start() {
		boolean success = true;
		try {
			IProcessor activeFlow = null;
			for (IProcessor flow : mFlows) {
				try {
					activeFlow = flow;
					success = flow.start();
					if (!success) {
						break;
					}
				} catch (Exception pEx) {
					System.err.printf("Exception while starting process: flow=%s ex=%s",
							(activeFlow != null) ? activeFlow.getClass().getSimpleName() : "unknown", pEx.getMessage());
					pEx.printStackTrace(System.err);
				}
			}
		} catch (Exception pEx) {
			System.err.printf("Error when starting the producer err=%s\n", pEx.getMessage());
			success = false;
		}
		return success;
	}


	private void stop() {
		try {
			if (mMessageFactory != null) {
				mMessageFactory.stop();
			}
			mAvgDayOut.stop();
			mAvgHourOut.stop();
			mAlarmOut.stop();
			mConvertedMsgRecordsOut.stop();
			mConsoleLogger.stop();
			mInp.stop();
			mConvertedMsgRecordsIn.stop();
		} catch (Exception pEx) {
			System.err.printf("Error when shutting down err=%s\n", pEx.getMessage());
		}
	}


	private void run() {

		while (true) {
			IProcessor activeFlow = null;
			try {
				Message inMsg = null; // At begin there is no message
				for (IProcessor flow : mFlows) {
					activeFlow = flow;
					if (flow.canHandle(inMsg)) {
						flow.process(inMsg);
					}
				}
			} catch (Exception pEx) {
				System.err.printf("Exception while running process: flow=%s ex=%s",
						(activeFlow != null) ? activeFlow.getClass().getSimpleName() : "unknown", pEx.getMessage());
				pEx.printStackTrace(System.err);
			}
			try {
				Thread.sleep(100); // Avoid busy wait
			} catch (Exception pEx) {
				// ignore on purpose
			}
		}
	}


	public static void main(String[] pArgs) {
		IntegratorMain app = new IntegratorMain();
		app.setup(pArgs);
		if (app.start()) {
			app.run();
			app.stop();
			System.exit(0);
		} else {
			System.exit(1);
		}
	}


}
