/**
 * 
 */
package org.identityconnectors.office365;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.DatatypeConverter;

import org.codehaus.groovy.runtime.ConversionHandler;
import org.identityconnectors.common.Base64;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;
import org.json.JSONObject;

/**
 * @author Paul Heaney
 *
 */
public class Office365GroupOps {

    private Office365Connector connector;
    private static final Log log = Log.getLog(Office365UserOps.class);

    public Office365GroupOps(Office365Connector connector) {
        this.connector = connector;
    }

    public Uid createGroup(Name name, final Set<Attribute> createAttributes) {
        log.info("Entered createGroup");

        Uid uid = null;

        if (createAttributes == null || createAttributes.size() == 0) {
            log.error("Attributes to create is empty");
            throw new IllegalArgumentException("Attributes to create are empty");
        }

        if (name == null) {
            log.error("Name attribute is empty");
            throw new IllegalArgumentException("Name is mandatory on create events");
        }

        log.ok("Name for create is {0}", name);

        JSONObject jsonCreate = new JSONObject();
        
        

        return uid;
    }
    
    public static void main(String[] args) throws Exception {
        String guid = "14c6c0c6-66fb-4c3c-a28e-a22a3e778dc4";
        System.out.println("GUID: "+guid);
        byte[] b = guid.getBytes();
        System.out.print("Byte array is: ");
        for (int i = 0; i < b.length; i++) {
            System.out.print((int) b[i] + " ");
        }
        System.out.println("");
        
        String s = guid.replace("-", "");
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
        System.out.println(Base64.encode(ss.getBytes("ISO-8859-1")));
    }
}
