package ch.ost.mas.cds.integration.base;

import javax.jms.Message;

public interface IProcessor {
	
	/**
	 * starts any connections if needed
	 * @return boolean true if success, false otherwise
	 */
	boolean start(); 
	/**
	 * stops any connections if needed
	 * @return boolean true if success, false otherwise
	 */
	boolean stop();

	/**
	 * Checks if the processor can handle
	 * the passed message
	 * @param pMsg Message message to handle
	 * @return boolean true if the processor can handle the message
	 */
	boolean canHandle(Message pMsg);
	
	/**
	 * Checks if the processor can handle
	 * the passed message
	 * @param pMsg Message message to handle
	 * @return Message processed message to forward or the original message if not handled 
	 */
	Message process (Message pMsg);
	
}
