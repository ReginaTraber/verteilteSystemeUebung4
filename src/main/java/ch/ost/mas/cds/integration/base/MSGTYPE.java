package ch.ost.mas.cds.integration.base;

import javax.jms.MapMessage;
import javax.jms.TextMessage;

/**
 * Message types used internaly
 * @author ako
 *
 */
public enum MSGTYPE {
	WRECRAW, 
	WRECNORM, 
	WALARM, 
	WAVGMSG,
	TEXT,
	; 
	

	

	
	public static MSGTYPE typeOf(String pType) {
		MSGTYPE ret = null; 
		
		for (MSGTYPE mt : MSGTYPE.values()) {
			if (mt.name().equalsIgnoreCase(pType)) {
				ret = mt; 
				break;
			}
		}
		return ret;
	}
}
