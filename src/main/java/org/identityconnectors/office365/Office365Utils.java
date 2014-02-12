/**
 * 
 */
package org.identityconnectors.office365;

import java.io.UnsupportedEncodingException;

import org.identityconnectors.common.Base64;
import org.identityconnectors.framework.common.exceptions.ConnectorException;

/**
 * @author Paul Heaney
 *
 */
public class Office365Utils {

    public static String encodeUUIDInMicrosoftFormat(String uuid) throws ConnectorException {
        String s = uuid.replace("-", "");
        String[] array = new String[16];
        int pos = 0;
        for (int i = 0; i < 16; i++) {
            array[i] = s.substring(pos, pos+2);
            pos+=2;
        }
        
        String[] newArray = {array[3], array[2], array[1], array[0], array[5], array[4], array[7], array[6], array[8], array[9], array[10], array[11], array[12], array[13], array[14], array[15]};
        
        String ss = "";
        for (int i =0 ; i < 16; i++) {
            int num = Integer.parseInt(newArray[i], 16);
            char c = (char) num;
            ss = ss + c;
        }
        
        try {
            return Base64.encode(ss.getBytes("ISO-8859-1"));
        }catch (UnsupportedEncodingException uee) {
            throw new ConnectorException("unable to convert uuid "+uuid+"to MS format", uee);
        }
    }

}
