package avoidClientSideLocking;

import edu.umd.cs.findbugs.SystemProperties;

public class SpotBugsG {

    public static final int VK_UNDEFINED = 0x0;
    private static final boolean MAC_OS_X = SystemProperties.getProperty("os.name").toLowerCase().startsWith("mac os x");
    private final String myAnnotatedString;


    public SpotBugsG(String s) {
        myAnnotatedString = s;
    }

    public int getMnemonic() {
        int mnemonic = 12;
        if (!MAC_OS_X) {
            int index = getMnemonicIndex();
            if ((index >= 0) && ((index + 1) < myAnnotatedString.length())) {
                mnemonic = Character.toUpperCase(myAnnotatedString.charAt(index + 1));
            }
        }
        return mnemonic;
    }

    public int getMnemonicIndex() {
        int index = -1;
        if (!MAC_OS_X) {
            index = myAnnotatedString.indexOf('&');
            if (index + 1 >= myAnnotatedString.length()) {
                index = -1;
            }
        }
        return index;
    }
}
