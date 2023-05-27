package ch.ost.mas.cds.integration.endpoints;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Iterator;

import javax.jms.MapMessage;
import javax.jms.Message;

import ch.ost.mas.cds.integration.base.AbstractProcessor;
import ch.ost.mas.cds.integration.base.WPARAM;

/**
 * Logs Weather Record Messages to Console
 * Type Description!!
 */
public class ConsoleLogger extends AbstractProcessor {

	private PrintStream mWriter;
	
	public ConsoleLogger() {
	}
	
	@Override
	public boolean canHandle(Message pMsg) {
		return pMsg instanceof MapMessage;
	}

	@Override
	public Message process(Message pMsg) {
		MapMessage tmsg = (MapMessage) pMsg;
		StringBuilder out = new StringBuilder();
		try {
	        out.append('[').append(formatDATETIME(tmsg.getString(WPARAM.DATETIME.name()))).append("] ");
		    @SuppressWarnings("unchecked")
            Iterator<String> itr = tmsg.getMapNames().asIterator();
		    while(itr.hasNext()) {
		        String tag = itr.next();
		        if(!WPARAM.DATETIME.getName().equals(tag)) {
		            Object val = tmsg.getObject(tag);
		            if (val != null) {
		                out.append(tag).append('=').append(val.toString()).append(' ');
		            }
		        }
		    }
		    mWriter.println(out.toString()); 
		} catch (Exception pEx) {
		    mWriter.printf("Error writing msg (%s) to console ex=%s\n", pMsg.toString(), pEx.getMessage());
		} 
		return pMsg;
	}
	
	private String formatDATETIME(String pUnformattedDateTime) {
	    String retS = null;
	    if (pUnformattedDateTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            retS = sdf.format(parseDate(pUnformattedDateTime));
	    } else {
	        retS = "--.--.---- --:--";
	    }
	    return retS;
	    
	}
	

	@Override
	public boolean start() {
	    mWriter = System.err;
	    return true;
	}
	
	@Override
	public boolean stop() {
	    mWriter.flush();
		return true;
	}
	
}
