/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Salford Software Ltd. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://opensource.org/licenses/cddl1.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://opensource.org/licenses/cddl1.txt
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.identityconnectors.office365;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Paul Heaney
 *
 */
public class Office365UserOps {

    private Office365Connector connector;
    private static final Log log = Log.getLog(Office365UserOps.class);
    private static final String NAME_ATTRIBUTE = "userPrincipalName";

    public Office365UserOps(Office365Connector connector) {
        this.connector = connector;
    }

    public Uid createUser(Name name, final Set<Attribute> createAttributes) {
        log.info("Entered createUser");

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

        if (this.connector.getConnection().isUserInAFederatedDomain(name.getNameValue())
                && (AttributeUtil.toMap(createAttributes).get(Office365Connector.IMMUTABLEID_ATTR) == null)) {
            log.error("User is in a federated domain, though no immutableID has been passed, this is required for a Federated User");
            throw new IllegalArgumentException("User (" + name.getNameValue() + ") is in a federated domain, though no immutableID has been passed, this is required for a Federated User");
        }


        JSONObject jsonCreate = new JSONObject();

        String password = null;
        Boolean forceChangePasswordNextLogin = new Boolean(false);

        String license = null;
        boolean usageLocationSet = false;

        for (Attribute attr : createAttributes) {
            String attrName = attr.getName();

            Object value = null;

            // All values in o365
            if (attr.getName().equals(OperationalAttributes.PASSWORD_NAME)) {
                log.info("Got password attribute on user creation");
                password = this.returnPassword(AttributeUtil.getGuardedStringValue(attr));
            } else if (attr.getName().equals("forceChangePasswordNextLogin")) {
                forceChangePasswordNextLogin = AttributeUtil.getBooleanValue(attr);
            } else if (attr.getName().equals("accountEnabled")) {
                value = new Boolean(AttributeUtil.getSingleValue(attr).toString());
            } else if (attr.getName().equals(Name.NAME)) {
                attrName = NAME_ATTRIBUTE;
                value = name.getNameValue().toString();
            } else if (attr.getName().equals(Office365Connector.LICENSE_ATTR)) {
                value = null;
                license = AttributeUtil.getSingleValue(attr).toString();
            } else if (attr.getName().equals(Office365Connector.USAGELOCATION_ATTR)) {
                value = AttributeUtil.getSingleValue(attr);
                usageLocationSet = true;
            } else if (attr.getName().equals(Office365Connector.IMMUTABLEID_ATTR)) {
                value = this.connector.getConnection().encodedUUID(AttributeUtil.getStringValue(attr));
            } else {
                if (this.connector.isAttributeMultiValues(ObjectClass.ACCOUNT_NAME, attrName)) {
                    value = attr.getValue();
                } else {
                    value = AttributeUtil.getSingleValue(attr);
                }
            }
            
            if (value != null) {
                log.info("Adding attribute {0} with value {1}", attrName, value);
                try {
                    if (value instanceof String) {
                        jsonCreate.put(attrName, value.toString());
                    } else if (value instanceof List) {
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

        if (password != null) {
            try {
                JSONObject pwd = new JSONObject();
                pwd.put("password", password);
                pwd.put("forceChangePasswordNextLogin", forceChangePasswordNextLogin);
                jsonCreate.put("passwordProfile", pwd);
            } catch (JSONException je) {
                log.error(je, "Error adding password to JSON attribute");
            }
        }

        log.info("About to create account using JSON {0}", jsonCreate.toString());

        try {
            uid = connector.getConnection().postRequest("/users?api-version=" + Office365Connection.API_VERSION, jsonCreate);            
        } 
        catch(Office365Exception oe)
        {
        	//Verify error if user Exist
        	if(oe.getErrorCode().equals(400) && oe.getErrorMessage().contains("Another object with the same value for property userPrincipalName already exists.")){
        		log.error("User Already exists {0}", name.getNameValue());
        		throw new AlreadyExistsException(oe.getMessage(),oe);
        	}
    	}catch (ConnectorException ce) {
            log.error(ce, "Error creating user {0}", name);
            log.error("Reason: {0}", ce.getMessage());
            log.error("Localized Message: {0}", ce.getLocalizedMessage());           
        }

        log.ok("Created account {0} successfully", name);

        if (uid != null && license != null) {
            log.info("Licenses to apply to newly created account");

            if (usageLocationSet) {
                log.info("Usage location was set so we can assign license");
                boolean b = assignLicense(uid, license);
                if (b) {
                    log.ok("License {0} set on {1}", license, uid.getUidValue());
                } else {
                    log.error("Failed to set license {0} set on {1}", license, uid.getUidValue());
                }
            } else {
                log.error("Usage Location not set on {0} unable to set license", uid.getUidValue());
            }
        }

        return uid;
    }

    public Uid updateUser(Uid uid, Set<Attribute> replaceAttributes, OperationOptions options) {

        log.info("Entered updateUser");

        if (replaceAttributes == null || replaceAttributes.size() == 0) {
            log.error("No attributes passed for update");
            throw new IllegalArgumentException("No attributes passed update");
        }

        log.info("Attribute set is ok");

        if (uid == null || (uid.getUidValue() == null)) {
            log.error("No UID specified for update");
            throw new IllegalArgumentException("No UID specified for update");
        }

        log.ok("UID of {0} is present", uid.getUidValue());

        JSONObject jsonModify = new JSONObject();

        String password = null;
        Boolean forceChangePasswordNextLogin = new Boolean(false);

        String license = null;
        Boolean licenseChanging = false;

        for (Attribute attr : replaceAttributes) {
            String attrName = attr.getName();
            
            Object value = null;

            if (attr.getName().equals(OperationalAttributes.PASSWORD_NAME)) {
                log.info("Changing password on user modification");
                password = this.returnPassword(AttributeUtil.getGuardedStringValue(attr));
            } else if (attr.getName().equals("forceChangePasswordNextLogin")) {
                forceChangePasswordNextLogin = AttributeUtil.getBooleanValue(attr);
            } else if (attr.getName().equals(Office365Connector.IMMUTABLEID_ATTR)) {
                // TODO is it possible to even change this?
                value = this.connector.getConnection().encodedUUID(AttributeUtil.getStringValue(attr));
            } else if (attr.getName().equals(Office365Connector.LICENSE_ATTR)) {
                value = null;
                licenseChanging = true;
                log.info("Attribute License is changing");
                
                if(!attr.getValue().isEmpty()){                	
                	license = AttributeUtil.getSingleValue(attr).toString();
                }
            } else {
                if (attr.getName().equals(Name.NAME)) {
                    attrName = NAME_ATTRIBUTE;
                }
                if (this.connector.isAttributeMultiValues(ObjectClass.ACCOUNT_NAME, attrName)) {
                    value = attr.getValue();
                } else {
                    value = AttributeUtil.getSingleValue(attr);
                }
            }

            log.info("Replacing attribute {0} with value {1}", attrName, value);
            try {
            	//Strip License from the JSON
            	if(!attrName.equals(Office365Connector.LICENSE_ATTR)){
                	if (value == null) {
                    	// Attribute being removed, excludes password
                    	if (!attr.getName().equals(OperationalAttributes.PASSWORD_NAME)) {
                    	    jsonModify.put(attrName, JSONObject.NULL);
                    	}
                	} else if (value instanceof String) {
                    		jsonModify.put(attrName, value.toString());
                	} else if (value instanceof List) {
                    		jsonModify.put(attrName, value);
                	} else {
                    		log.error("Attribute {0} of non recognised type {1}", attrName, value.getClass());
                	}
            	}
            } catch (JSONException je) {
                log.error(je, "Error adding JSON attribute {0} with value {1} on create - exception {}", attrName, value);
            }
        }

        if (password != null) {
            try {
                JSONObject pwd = new JSONObject();
                pwd.put("password", password);
                pwd.put("forceChangePasswordNextLogin", forceChangePasswordNextLogin);
                jsonModify.put("passwordProfile", pwd);
            } catch (JSONException je) {
                log.error(je, "Error adding password to JSON attribute");
            }
        }

        log.info("About to modify account using JSON {0}", jsonModify.toString());

        boolean b = false;
        try {
            b = this.connector.getConnection().patchObject("/users/" + uid.getUidValue() + "?api-version=" + Office365Connection.API_VERSION, jsonModify);
        } catch (ConnectorException ce) {
            log.error(ce, "Error modifying user {0}", uid.getUidValue());
        }

        if (b) {
            log.ok("Modified account {0} successfully", uid.getUidValue());
            
            if (licenseChanging == true) {
                log.info("Attempting to set the license");
                b = assignLicense(uid, license);
                if (b) {
                    log.ok("License updated sucessfully on {0}", uid.getUidValue());
                } else {
                    log.error("Failed to update license on {0}", uid.getUidValue());
                }
            }
        } else {
            log.ok("Failed to modify account {0}", uid.getUidValue());
        }

        return uid;
    }

    public void deleteUser(final Uid uid) {

        log.info("In deleteUser");

        if (uid == null || (uid.getUidValue() == null)) {
            log.error("No UID specified for update");
            throw new IllegalArgumentException("No UID specified for update");
        }

        log.ok("UID of {0} is present", uid.getUidValue());

        boolean b = this.connector.getConnection().deleteRequest("/users/" + uid.getUidValue() + "?api-version=" + Office365Connection.API_VERSION);

        if (b) {
            log.info("Sucessfully deleted account {0}", uid.getUidValue());
        } else {
            log.info("Failed to deleted account {0}", uid.getUidValue());
        }
    }

    public void queryUser(String query, ResultsHandler resultsHandler, OperationOptions options) {
        log.info("queryUser");

        if (query == null) {
            // retrieve all
            log.info("Fetching All Users from Office 365");
            JSONArray allObj;
            try {
                allObj = this.connector.getConnection().getRequest("/users?api-version=" + Office365Connection.API_VERSION).getJSONArray("value");
                for (int i = 0; i < allObj.length(); i++) {
                    ConnectorObject co = makeConnectorObject((JSONObject) allObj.get(i));
                    if (co != null) {
                        resultsHandler.handle(co);
                    }
                }
            } catch (JSONException ex) {
                Logger.getLogger(Office365UserOps.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else {
            log.info("Fetching Office 365 user {0}", query);
            JSONObject obj = this.connector.getConnection().getRequest("/users/" + query + "/?api-version=" + Office365Connection.API_VERSION);
            ConnectorObject co = makeConnectorObject(obj);

            if (co != null) {
                resultsHandler.handle(co);
            }
        }
    }

    public boolean assignLicense(Uid uid, String license) {
        log.info("assignLicense");

        if (uid == null) {
            log.error("No UID specified on assignLicense");
            throw new IllegalArgumentException("No UID specified for assignLicense");
        }

        log.ok("UID of {0} is present", uid.getUidValue());

        log.ok("License of {0} received for uid {1}", license, uid.getUidValue());
        
        /*
         * The Connector handles only single values only
         * Office 365 assignLicense Service does not support to remove and add the same license modifying the plans
         * so we need to remove the license first and then add the other license and plans 
         */
        log.ok("Query user for existing license(s) to be removed prior to set new license.");
        try
        {
        	JSONObject myUser = this.connector.getConnection().getRequest("/users/" + uid.getUidValue() + "?api-version=" + Office365Connection.API_VERSION);
        	log.info("User Information {0}", myUser);
        	JSONArray userAssignedLicenses = myUser.getJSONArray("assignedLicenses");
        	if(userAssignedLicenses.length() != 0){
	        	log.info("User Assigned Licenses {0}", userAssignedLicenses);        	
	        	for (int i = 0; i < userAssignedLicenses.length(); i++) { 
	        		if(userAssignedLicenses.getJSONObject(i).getString("skuId") != null){
		        		JSONObject license2remove = new JSONObject();
		        		license2remove.put("addLicenses", JSONObject.NULL);
		        		log.ok("User SkuID License {0}", userAssignedLicenses.getJSONObject(i).getString("skuId"));
		        		
		        		ArrayList<String> unwantedLicenses = new ArrayList<String>();
		        		unwantedLicenses.add(userAssignedLicenses.getJSONObject(i).getString("skuId"));
		        		license2remove.put("removeLicenses", unwantedLicenses);
		        		log.info("Remove License JSON {0}", license2remove);
		        		Uid returnedUid = this.connector.getConnection().postRequest("/users/" + uid.getUidValue() + "/assignLicense?api-version=" + Office365Connection.API_VERSION, license2remove);
		        		if (returnedUid != null && returnedUid.equals(Office365Connection.SUCCESS_UID)) {
		                    log.info("License removed successfully from user {0}", uid.getUidValue());
		                } else {
		                    log.error("Failed to remove license");
		                    
		                }
	        		}
	        	}	        	
        	}
         }
        catch (Exception e)
        {
        log.error(e, "Error removing existing license(s).");
                   throw new ConnectorException("Error removing existing license(s). ", e);
        }

        log.ok("Now add the new license, if it is not null.");
        try {
            if(license != null){ 
	        	JSONObject lic = convertLicenseToJson(license);
	
	            log.info("Attempting license assignment with {0}", lic.toString());
	
	            Uid returnedUid = this.connector.getConnection().postRequest("/users/" + uid.getUidValue() + "/assignLicense?api-version=" + Office365Connection.API_VERSION, lic);
	
	            if (returnedUid != null && returnedUid.equals(Office365Connection.SUCCESS_UID)) {
	                log.info("License assigned successfully to {0}", uid.getUidValue());
	                return true;
	            } else {
	                log.error("Failed to assign license.");
	                return false;
	            }
            }else{
            	log.info("Exit without assigning new license.");
                return true;
            }
        } catch (JSONException je) {
            log.error(je, "Error converting license {0} to JSON for {1}", license, uid.getUidValue());
            throw new ConnectorException("Error converting license " + license + " to JSON for " + uid.getUidValue(), je);
        }
    }

    private ConnectorObject makeConnectorObject(JSONObject jsonObject) {
        log.info("makeConnectorObject");

        if (jsonObject == null) {
            log.error("Passed empty jsonObject");
            return null;
        }

        try {
            String objectType = jsonObject.getString("objectType");
            if (!objectType.equals("User")) {
                log.error("Received object type {0} when doing a user query which is not supported", objectType);
                throw new IllegalArgumentException("Received " + objectType + " when searching for a user, this should be User");
            }

            ConnectorObjectBuilder cob = new ConnectorObjectBuilder();

            Uid uid = new Uid(jsonObject.getString("objectId"));
            String userPrincipalName = jsonObject.getString(NAME_ATTRIBUTE);
            cob.setUid(uid);
            cob.setName(userPrincipalName);

            String[] attrs = {"accountEnabled", "city", "country", "department", "displayName", "facsimileTelephoneNumber", "givenName",
                "jobTitle", "mail", "mailNickname", "mobile", "otherMails", "physicalDeliveryOfficeName", "postalCode",
                "preferredLanguage", "proxyAddresses", "state", "streetAddress", "surname", "telephoneNumber",
                "usageLocation"};

            for (String a : attrs) {
                if (jsonObject.has(a)) {
                    Object value = jsonObject.get(a);
                    // log.info("Retreieved attribute {0} with value {1}", a, value);
                    if (value != null && value != JSONObject.NULL) {
                        if (value instanceof JSONArray) {
                            JSONArray j = (JSONArray) value;
                            int length = j.length();
                            List<String> items = new ArrayList<String>();
                            for (int i = 0; i < length; i++) {
                                items.add(j.getString(i));
                            }
                            cob.addAttribute(AttributeBuilder.build(a, items));
                        } else {
                            cob.addAttribute(AttributeBuilder.build(a, value));
                        }
                    }
                } else {
                    log.info("No value returned for {0}", a);
                }
            }

            log.info("Object has the UID {0} and name {1}", uid, userPrincipalName);

            return cob.build();
        } catch (JSONException je) {
            log.error(je, "Exception thrown parisng returned JSON on user query");
            return null;
        }
    }

    public JSONObject convertLicenseToJson(String license) throws JSONException {
        // INPUT  licensename:planname:planname:...
        log.info("convertLicenseToJson");

        if (license != null && license.length() > 0) {
            log.info("License string passed");
            String[] components = license.split(":");

            /*
             String object = "{\"addLicenses\": [
             { \"disabledPlans\": [\"SHAREPOINTWAC_EDU\" , \"SHAREPOINTSTANDARD_EDU\" ], 
             \"skuId\": \"314c4481-f395-4525-be8b-2ec4bb1e9d91\" }
             ], 
             \"removeLicenses\": null }";
             */

            JSONObject obj = new JSONObject();
            JSONArray addObj = new JSONArray();

            String skuId = connector.getConnection().getLicensePlanId(components[0]);
            JSONObject licenseObj = new JSONObject();
            if (skuId != null) {
                log.info("valid license SKU of {0} passed", skuId);

                licenseObj.put("skuId", skuId);

                if (components.length == 1) {
                    log.info("Only a license sku passed, no plans - all assumed");
                    // we have just a single sku with no specific plans
                    licenseObj.put("disabledPlans", new ArrayList<String>());
                } else {
                    log.info("Plans passed with license");
                    // Need to do the inverse here and get the disables
                    Office365License lic = connector.getConnection().getLicensePlan(components[0]);

                    ArrayList<String> assignedPlans = new ArrayList<String>();
                    for (int i = 1; i < components.length; i++) {
                        assignedPlans.add(components[i]);
                    }

                    if (lic != null) {
                        log.info("Got valid license object for id {0}", components[0]);
                        Iterator<Office365ServicePlan> it = lic.getServicePlans().iterator();
                        ArrayList<String> unwantedPlans = new ArrayList<String>();

                        while (it.hasNext()) {
                            Office365ServicePlan sp = it.next();
                            log.info("Service plan on license {0}", sp.getServicePlanName());

                            if (!assignedPlans.contains(sp.getServicePlanName())) {
                                log.info("Adding {0} to list of plans we don't want", sp.getServicePlanName());
                                // We don't want this plan
                                String id = connector.getConnection().getServicePlanId(sp.getServicePlanName());
                                if (id != null) {
                                    unwantedPlans.add(id);
                                }
                            }
                        }

                        licenseObj.put("disabledPlans", unwantedPlans);
                    }

                    addObj.put(licenseObj);
                }

                obj.append("addLicenses", licenseObj);
                obj.put("removeLicenses", JSONObject.NULL); // TODO something smarter

                return obj;
            } else {
                log.error("Invalid SKU/License passed {0}", components[0]);
                return null;
            }
        } else {
            log.error("No license details passed");
            return null;
        }
    }

    /**
     *
     * @param password The password to format
     * @return String the plain text version of the password
     */
    private String returnPassword(GuardedString password) {
        final String[] clearText = new String[1];
        GuardedString.Accessor accessor = new GuardedString.Accessor() {
            @Override
            public void access(char[] clearChars) {
                clearText[0] = new String(clearChars);

            }
        };

        password.access(accessor);

        return clearText[0];
    }
}
