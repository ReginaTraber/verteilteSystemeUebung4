package ch.ost.mas.cds.integration.control;


import java.util.ArrayList;
import java.util.List;

import javax.jms.Message;

import ch.ost.mas.cds.integration.base.AbstractProcessor;
import ch.ost.mas.cds.integration.base.IProcessor;
/**
 * Distributes an incomoing message to the various
 * registered processors
 *
 */
public class MultiDestinationRouter extends AbstractProcessor {
	
	List<IProcessor> 	mProcessors;
	
	public MultiDestinationRouter(IProcessor... pProcessors) {
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
	        if (!(success =  proc.start())) {
	            break;
	        }
	    }
	    return success;
	}
	
	@Override
	public boolean stop() {
        boolean success = super.stop();
        for (IProcessor proc : mProcessors) {
            if (!(success =  proc.stop())) {
                break;
            }
        }
        return success;
	}
	@Override
	public boolean canHandle(Message pMsg) {
		boolean canHandle = false; 
		for (IProcessor proc : mProcessors) {
			canHandle = canHandle || proc.canHandle(pMsg);
			if (canHandle) {
				break;
			}
		}
		return canHandle;
	}

	@Override
	public Message process(Message pMsg) {
		if ((pMsg != null) && (canHandle(pMsg))) {
			for (IProcessor proc : mProcessors) {
				if (proc.canHandle(pMsg)) {
					proc.process(pMsg);
				}
			}
		}
		return null;
	}
		
}
