package ch.ost.mas.cds.integration.base;

public enum WPARAM {
	    DATETIME("DateTime", "Date/Time", 0),
	    OUTTEMP("OutTemp", "Out Temperatur",1), 
	    WINDCHILL("Windchill", "Windchill", 2),
	    HEATINDEX("Heatindex", "Heat Index", 3),
	    OUTHUMIDITY("OutHumidity", "Out Humidity", 4),
	    DEWPOINT("Dewpoint", "Dew Point", 5),
	    WINDSPEED("WindSpeed", "Wind Speed", 6),
	    WINDDIR("WindDir", "Wind Direction", 7),
	    WINDGUST("WindGust", "Wind Gust", 8),
	    WINDGUSTDIR("WindGustDir", "Wind Gust Direction", 9),
	    RAIN("Barometer", "Rain", 10),
	    BAROMETER("barometer", "Barometer", 11),
	    WATERTEMP("ExtraTemp1", "Water Temperatur", 12),
	    ;

	private static final WPARAM[]	   sInputMap = new WPARAM[13];

	    private String                     mKey;
	    private String                     mDisplayName;
	    private int                        mInputSeq;

	    private WPARAM(String pKey, String pDisplayField, int pInputSeq) {
	        mKey = pKey;
	        mDisplayName = pDisplayField;
	        mInputSeq = pInputSeq;
	    }


	    public String key() {
	        return mKey;
	    }

	    public String getName() {
	        return mDisplayName;
	    }


	    public int getInputSeq() {
	        return mInputSeq;
	    }

	    public static WPARAM tagForSequence(int pInpSeq) {
	    	WPARAM ret = null; 
	    	if (sInputMap[0] != WPARAM.DATETIME) {
	    		for (WPARAM wp: WPARAM.values()) {
	    			sInputMap[wp.getInputSeq()] = wp;
	    		}
	    	}
	    	if ((pInpSeq >= 0) && (pInpSeq < sInputMap.length)) {
	    		ret = sInputMap[pInpSeq];
	    	}
	    	return ret; 
	    }
}
