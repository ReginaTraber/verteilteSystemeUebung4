package ch.ost.mas.cds.integration.translator;

import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.TextMessage;

import ch.ost.mas.cds.integration.base.AbstractProcessor;
import ch.ost.mas.cds.integration.base.MSGTYPE;
import ch.ost.mas.cds.integration.base.WPARAM;
import ch.ost.mas.cds.integration.util.MessageFactory;

public class WeatherRecordTransformer 
extends AbstractProcessor {

	@Override
	public boolean canHandle(Message pMsg) {
		return testMsgtType(pMsg, MSGTYPE.WRECRAW);
	}

	@Override
	public Message process(Message pMsg) {
		Message retMsg = pMsg;
		if ((pMsg != null) && (canHandle(pMsg))) {
			try {
				TextMessage tmsg = (TextMessage) pMsg;
				String flds[] = tmsg.getText().split(";");
				// DateTime;      OutTemp;Windchill;Heatindex;OutHumidity;Dewpoint;WindSpeed;WindDir;WindGust;WindGustDir;Rain;Barometer;ExtraTemp1;
				// 20160101 01:00;3.33;3.33;3.33;97.0;2.90;1.609;0;3.219;45;0.000;1026.278;-0.00;
				if (flds.length >= 13) {
					MapMessage xmsg = MessageFactory.getInstance().create(MSGTYPE.WRECNORM);
					for (int ix = 0; ix < 13; ix++) {
						WPARAM parm = WPARAM.tagForSequence(ix);
						if (parm != null) {
							switch (parm) {
							case DATETIME: {
								try {
									xmsg.setString(parm.name(), flds[ix]);
								} catch (Exception pEx) {
									System.err.printf("%s: parameter error for %s = %s\n",  getClass().getSimpleName(), parm.name(), flds[ix]);
								}
								break;
							}
							case WINDDIR: 
							case WINDGUSTDIR: {
								try {
									Integer value = Integer.parseInt(flds[ix].trim());
									xmsg.setInt(parm.name(), value);
								} catch (Exception pEx) {
									System.err.printf("%s: parameter error for %s = %s\n",  getClass().getSimpleName(), parm.name(), flds[ix]);
								}
								break;
							}
							case BAROMETER: 
							case OUTHUMIDITY: 
							case DEWPOINT:
							case HEATINDEX:
							case OUTTEMP: 
							case RAIN:
							case WATERTEMP:
							case WINDCHILL: 
							case WINDGUST:   
							case WINDSPEED: {
								try {
									Double value = Double.parseDouble(flds[ix].trim());
									xmsg.setDouble(parm.name(), value);
								} catch (Exception pEx) {
									System.err.printf("%s: parameter error for %s = %s\n",  getClass().getSimpleName(), parm.name(), flds[ix]);
								}
								break;
							}
							default: {
								System.err.printf("%s: parameter error for %s = %s\n",  getClass().getSimpleName(), parm.name(), flds[ix]);
							}
							}
						}
					}
					retMsg = xmsg;
				} else {
					System.err.printf("%s: Incompatible record rec=%s\n",  getClass().getSimpleName(), tmsg.getText());
				}
			} catch (Exception pEx) {
				System.err.printf("%s: Error when converting err=%s\n",  getClass().getSimpleName(), pEx.getMessage());
				retMsg = null;
			}
		}
		return retMsg;
	}

}
