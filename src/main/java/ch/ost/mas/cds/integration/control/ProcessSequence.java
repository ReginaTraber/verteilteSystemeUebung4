package ch.ost.mas.cds.integration.control;

import java.util.ArrayList;
import java.util.List;

import javax.jms.Message;

import ch.ost.mas.cds.integration.base.AbstractProcessor;
import ch.ost.mas.cds.integration.base.IProcessor;

/**
 * Passes the message to the sequence of processors
 *
 */
public class ProcessSequence extends AbstractProcessor {

	List<IProcessor> 	mProcessors;
	
	public ProcessSequence(IProcessor... pProcessors) {
		mProcessors = new ArrayList<IProcessor>();
		if ((pProcessors != null) && (pProcessors.length > 0)) {
			for (IProcessor proc : pProcessors) {
				mProcessors.add(proc);
			}
		}
	}
	@Override
	public boolean start() {
	    boolean success = super.start();
	    for (IProcessor proc : mProcessors) {
	        if (!(success = proc.start())) {
	            break;
	        }
	    }
	    return success;
	}

	@Override
	public boolean stop() {
	    boolean success = super.stop();
	    for (IProcessor proc : mProcessors) {
	        if (!(success = proc.stop())) {
	            break;
	        }
	    }
	    return success;
	}
	
	@Override
	public boolean canHandle(Message pMsg) {
		// only the first processor in the list has to be able to handle the message
		return ((mProcessors.size() > 0) && (mProcessors.get(0).canHandle(pMsg)));
	}

	@Override
	public Message process(Message pMsg) {
	    Message rmsg = pMsg; 
	    boolean first = true;
	    for (IProcessor proc : mProcessors) {
	        if (((first) || (rmsg != null)) && (proc.canHandle(rmsg))) {
	            rmsg = proc.process(rmsg); first = false;
	        } else {
	            break;
	        }
	    }
	    return rmsg;
	}

}
