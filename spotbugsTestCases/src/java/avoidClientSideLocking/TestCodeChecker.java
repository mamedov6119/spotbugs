package avoidClientSideLocking;

public class TestCodeChecker {
    
    private String createPlacemarkDescription(String placeName) {
        StringBuilder buffer = new StringBuilder(100);
        
        buffer.append("NetworkFeatureFactory.STARTH2");
        buffer.append("LINK");
        buffer.append("NetworkFeatureFactory.ENDH2");
        buffer.append("NetworkFeatureFactory.STARTH3");
        buffer.append("H24OVERVIEW");
        buffer.append("NetworkFeatureFactory.ENDH3");
        buffer.append("NetworkFeatureFactory.STARTP");
        buffer.append("IMG");
        buffer.append("IMGEND");
        buffer.append("NetworkFeatureFactory.ENDP");
        buffer.append("NetworkFeatureFactory.STARTH3");
        buffer.append("DETAILSFROM");
        buffer.append("OCLOCKTO");
        buffer.append("OCLOCK");
        buffer.append("NetworkFeatureFactory.ENDH3");
        buffer.append("NetworkFeatureFactory.STARTP");
        buffer.append("COUNTVALUE");
        buffer.append("NetworkFeatureFactory.ENDP");
        buffer.append("NetworkFeatureFactory.STARTP");
        buffer.append("MATSIMVALUE");
        buffer.append("NetworkFeatureFactory.ENDP");
        buffer.append("NetworkFeatureFactory.STARTP");
        buffer.append("RELERROR");
        buffer.append("NetworkFeatureFactory.ENDP");
        buffer.append("NetworkFeatureFactory.STARTP");
        buffer.append("NORMRELERROR");
        buffer.append("nf.format(normalizedRelativeError * 100)");
        buffer.append("NetworkFeatureFactory.ENDP");
        buffer.append("NetworkFeatureFactory.STARTP");
        buffer.append("GEH");
        buffer.append("NetworkFeatureFactory.ENDP");
    //  buffer.append("NetworkFeatureFactory.ENDCDATA");
        return buffer.toString();
    }

    private Object createPlacemark() {
        
        return createPlacemarkDescription("HELLO");
         
    }
}
