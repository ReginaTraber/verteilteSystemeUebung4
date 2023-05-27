package ch.ost.mas.cds.integration.endpoints;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import ch.ost.mas.cds.integration.base.AbstractProcessor;
import ch.ost.mas.cds.integration.base.MSGTYPE;
import ch.ost.mas.cds.integration.util.MessageFactory;

public class FileRecordReader extends AbstractProcessor {

	private String mFileName;
	private BufferedReader mReader;
	
	public FileRecordReader(String pFileName) {
		mFileName = pFileName;
	}
	@Override
	public boolean canHandle(Message pMsg) {
		return pMsg == null;
	}

	@Override
	public Message process(Message pMsg) {
		Message rmsg = pMsg;
		try {
			if (rmsg == null) {
				String ln = mReader.readLine(); 
				if (ln == null) {
					rmsg = null; 
				} else {
					TextMessage tmsg = MessageFactory.getInstance().create(MSGTYPE.TEXT);
					tmsg.setText(ln);
					rmsg = tmsg;
				}
			}
		} catch (Exception pEx) {
			System.err.printf("Can't read or transfer input line from %s err=%s\n",  mFileName, pEx.getMessage());
			rmsg = null; 
		} 
		return rmsg;
	}

	@Override
	public boolean start() {
		boolean success = true;
		try {
			mReader = new BufferedReader(new FileReader(mFileName));
		} catch (FileNotFoundException pEx) {
			System.err.printf("Cannot open file %s err=%s\n", mFileName, pEx.getMessage());
			success = false;
		}
		return success;
	}
	@Override
	public boolean stop() {
		boolean success = true;
		if (mReader != null) {
			try {
				mReader.close();
				mReader = null;
			} catch (IOException pEx) {
				System.err.printf("Cannot close file %s err=%s\n", mFileName, pEx.getMessage());
				success = false;
			} 
		}
		return success;
	}
	
}
