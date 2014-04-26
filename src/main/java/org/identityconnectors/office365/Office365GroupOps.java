/**
 * 
 */
package org.identityconnectors.office365;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.DatatypeConverter;

import org.codehaus.groovy.runtime.ConversionHandler;
import org.identityconnectors.common.Base64;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Paul Heaney
 *
 */
public class Office365GroupOps {

    private Office365Connector connector;
    private static final Log log = Log.getLog(Office365UserOps.class);
    
    private static final String NAME_ATTRIBUTE = "mailNickname";

    public Office365GroupOps(Office365Connector connector) {
        this.connector = connector;
    }

    public Uid createGroup(Name name, final Set<Attribute> createAttributes) {
        log.info("Entered createSecurityGroup");

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

        try {
            // The Graph API only supports this combination (as of 014-04-26)
            jsonCreate.put("mailEnabled", false);
            jsonCreate.put("securityEnabled", true);
        } catch (JSONException je) {
            log.error(je, "Error adding mandatory JSON attributes on create");
        }
        
        for (Attribute attr : createAttributes) {
            String attrName = attr.getName();
            Object value = null;
            
            if (attr.getName().equals(Name.NAME)) {
                attrName = NAME_ATTRIBUTE;
                value = name.getNameValue();
            } else {
                value = AttributeUtil.getSingleValue(attr); // TODO handle multivalued
            }
            
            if (value != null) {
                log.info("Adding attribute {0} with value {1}", attrName, value);
                try {
                    if (value instanceof String) {
                        jsonCreate.put(attrName, value);
                    } else if (value instanceof Boolean) {
                        jsonCreate.put(attrName, value);
                    } else {
                        log.error("Attribute {0} of non recognised type {1}", attrName, value.getClass());
                    }
                } catch (JSONException je) {
                    log.error(je, "Error adding JSON attribute {0} with value {1} on create - exception {}", attrName, value);
                }
            }
        }

        log.info("About to create group using JSON {0}",  jsonCreate.toString());

        try {
            uid = connector.getConnection().postRequest("/groups?api-version="+Office365Connection.API_VERSION, jsonCreate);
        } catch (ConnectorException ce) {
            log.error(ce, "Error creating group {0}", name);
        }

        log.ok("Create group {0} successfully",  name);

        return uid;
    }
    
    public boolean addUserToGroup(String groupID, String userID)
    {
        log.info("Entered addUserToGroup");
        if (groupID != null && userID != null)
        {
            log.info("Got user and group IDs");
            JSONObject update = new JSONObject();

            String value = this.connector.getConfiguration().getProtocol()+ connector.getConfiguration().getApiEndPoint()+ "/" + connector.getConfiguration().getTenancy() + "/directoryObjects/"+userID;

            try {
                update.put("url", value); // Copied from getAPIEndPoint
            } catch (JSONException je) {
                log.error(je, "Error adding JSON attribute url with value {0} on create - exception {1}", value, je);
            }
            
            try {
                this.connector.getConnection().postRequest("/groups/"+groupID+"/$links/members?api-version="+Office365Connection.API_VERSION, update);
            }  catch (ConnectorException ce) {
                log.error(ce, "Error adding group member {0} to {1}", userID, groupID);
                return false;
            }
            
            log.info("Sucessfully added group member");
            
            return true;
        }
        else
        {
            log.error("Not supplied both a group and user ID");
            return false;
        }
    }
    
    /**
     * Gets the list of groups a user is member of
     * @param uid
     * @return
     */
    public List<String> getUserGroups(String userID) {
        log.info("Entered getUserGroups");
        
        if (userID != null) {
            JSONObject json = new JSONObject();
            try {
                json.put("securityEnabledOnly", false);
            } catch (JSONException je) {
                log.error("Error creating JSON filter,  error {0}", je);
            }
            
            JSONObject jo = null;
            
            try {
                jo = this.connector.getConnection().postRequestReturnJson("/users/"+userID+"/getMemberGroups?api-version="+Office365Connection.API_VERSION, json);
            } catch (ConnectorException ce) {
                log.error(ce, "Error getting group membership for {0} exception {1}", userID, ce);
                return null;
            }
            
            List<String> groups = new ArrayList<String>();
            
            try {
                JSONArray array = jo.getJSONArray("value");
                for (int i = 0; i < array.length(); i++) {
                    groups.add(array.getString(i));
                }
            } catch (JSONException je) {
                log.error(je, "Error getting parsing group membership");
                return null;
            }
            
            return groups;
            
        } else {
            log.error("No user ID supplied");
            return null;
        }
    }
    
    public boolean removeUserFromGroup(String groupID, String userID) {
        log.info("Enetered removeUserFromGroup");
        
        if (groupID != null && userID != null)
        {
            log.info("Got user and group ID");
            
            return this.connector.getConnection().deleteRequest("/groups/"+groupID+"/$links/members/"+userID+"?api-version="+Office365Connection.API_VERSION);
        }
        else
        {
            log.error("Not supplied both a group and user ID");
            return false;
        }
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
