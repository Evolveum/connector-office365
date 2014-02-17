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

import java.sql.Connection;
import java.util.ArrayList;

import org.identityconnectors.common.Base64;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.office365.Office365Configuration;
import org.identityconnectors.office365.Office365Connection;
import org.identityconnectors.office365.Office365Connector;
import org.identityconnectors.office365.Office365UserOps;
import org.identityconnectors.office365.domain.Office365Domain;
import org.identityconnectors.test.common.TestHelpers;
import org.identityconnectors.test.common.PropertyBag;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Attempts to test the {@link Office365Connector} with the framework.
 *
 * @author Paul Heaney
 * @version $Revision$ $Date$
 */
public class Office365ConnectorTests {

    /*
    * Example test properties.
    * See the Javadoc of the TestHelpers class for the location of the public and private configuration files.
    */
    private static final PropertyBag properties = TestHelpers.getProperties(Office365Connector.class);
    // Host is a public property read from public configuration file
    private static final String APIENDPOINT = properties.getStringProperty("configuration.apiEndPoint");
    // Login and password are private properties read from private configuration file 
    private static final String TENANCY = properties.getStringProperty("configuration.tenancy");
    private static final GuardedString SYMETRICKEY = properties.getProperty("configuration.symetricKey", GuardedString.class);
    private static final String AUTHURL = properties.getStringProperty("configuration.authUrl");
    private static final String PRINCIPALID = properties.getStringProperty("configuration.principalID");
    private static final String RESOURCEID = properties.getStringProperty("configuration.resourceID");
    private static final String ACSPRINCIPALID = properties.getStringProperty("configuration.acsPrincipalID");
    private static final String IMMUTABLEID_ENCODE = properties.getStringProperty("configuration.immutableIDEncodeMechanism");

    private static final String TEST_FEDERATED_DOMAIN = "feb14-fed.pheaney-dev.co.uk";
    private static final String TEST_MANAGED_DOMAIN = "ProofIDFeb14.onmicrosoft.com";

    private static final String TEST_FEDERATED_USER = "icf-test1@"+TEST_FEDERATED_DOMAIN;
    private static final String TEST_MANAGED_USER = "icf-test1@"+TEST_MANAGED_DOMAIN;

    private static final String LICENSE_PLAN = "ENTERPRISEPACK"; // For academia STANDARDWOFFPACK_STUDENT
    private static final String LICNESE_PLAN_ID= "6fd2c87f-b296-42f0-b197-1e91e994b900"; // For academia 314c4481-f395-4525-be8b-2ec4bb1e9d91
    private static final String EXCHANGE_SERVICE = "EXCHANGE_S_ENTERPRISE"; // For academia EXCHANGE_S_STANDARD
    private static final String EXCHANGE_SERVICE_ID = "efb87545-963c-4e0d-99df-69c6916d9eb0"; // For academia 9aaf7827-d63c-4b61-89c3-182f06f82e5c

    //set up logging
    private static final Log LOGGER = Log.getLog(Office365ConnectorTests.class);

    @BeforeClass
    public static void setUp() {
        Assert.assertNotNull(APIENDPOINT);
        Assert.assertNotNull(TENANCY);
        Assert.assertNotNull(SYMETRICKEY);
        Assert.assertNotNull(AUTHURL);
        Assert.assertNotNull(PRINCIPALID);
        Assert.assertNotNull(RESOURCEID);
        Assert.assertNotNull(ACSPRINCIPALID);
        Assert.assertNotNull(IMMUTABLEID_ENCODE);

        //
        //other setup work to do before running tests
        //

        //Configuration config = new Office365Configuration();
        //Map<String, ? extends Object> configData = (Map<String, ? extends Object>) properties.getProperty("configuration",Map.class)
        //TestHelpers.fillConfiguration(
    }

    @AfterClass
    public static void tearDown() {
        //
        //clean up resources
        //
    }

    @Test
    public void getToken() {
        Office365Configuration config = getConfiguration();
       
        String token = Office365Connection.createToken(config);
        Assert.assertNotNull(token);
    }
    
    @Test
    public void getSchema() {
    	Office365Connector o365 = new Office365Connector();
    	o365.init(getConfiguration());
    	Schema schema = o365.schema();

    	Assert.assertNotNull(schema);
    }

    /*
     * Non federated domain 
     */
    
    @Test
    public void testCreate() {
        Office365Configuration config = getConfiguration();

        Office365Connection o365Conn = Office365Connection.createConnection(config);

        String token = Office365Connection.createToken(config);
        Assert.assertNotNull(token);

        try {
            JSONObject obj = new JSONObject();
            obj.put("accountEnabled", true);
            obj.put("displayName", "OpenICF Test1");
            obj.put("mailNickname", "icf-test1");
            obj.put("userPrincipalName", TEST_MANAGED_USER);
            JSONObject pwd = new JSONObject();
            pwd.put("password", "Test1234!");
            pwd.put("forceChangePasswordNextLogin", true);
            obj.put("passwordProfile", pwd);
            obj.put("usageLocation", "GB");
            ArrayList<Object> otherEmails = new ArrayList<Object>();
            otherEmails.add("test11@"+TEST_MANAGED_DOMAIN);
            otherEmails.add("test12@"+TEST_MANAGED_DOMAIN);
            obj.put("otherMails", otherEmails);

            LOGGER.info("About to create using  {0}", obj.toString());
            Uid uid = o365Conn.postRequest("/users?api-version="+Office365Connection.API_VERSION, obj);
            LOGGER.info("Got a UID of {0}", uid);
            Assert.assertNotNull(uid);
        } catch(JSONException je) {
            LOGGER.error(je, "Error creating test create structure");
        }
    }

    @Test(dependsOnMethods={"testCreate"})
    public void testModify() {
        Office365Configuration config = getConfiguration();

        Office365Connection o365Conn = Office365Connection.createConnection(config);

        String token = Office365Connection.createToken(config);
        Assert.assertNotNull(token);

        try {
            JSONObject obj = new JSONObject();
            obj.put("department", "test");
            obj.put("usageLocation", "GB");

            boolean b = o365Conn.patchObject("/users/"+TEST_MANAGED_USER+"?api-version="+Office365Connection.API_VERSION, obj);

            Assert.assertTrue(b);
        } catch(JSONException je) {
            LOGGER.error(je, "Error creating test modify structure");
        }
    }

    @Test(dependsOnMethods={"testModify", "testLicenseAssignment"})
    public void testDelete() {
        Office365Configuration config = getConfiguration();

        Office365Connection o365Conn = Office365Connection.createConnection(config);

        String token = Office365Connection.createToken(config);
        Assert.assertNotNull(token);

        boolean b = o365Conn.deleteRequest("/users/"+TEST_MANAGED_USER+"?api-version="+Office365Connection.API_VERSION);
        Assert.assertTrue(b);
    }
    
    @Test(dependsOnMethods={"testCreate"})
    public void testLicenseAssignment() throws JSONException {
        Office365Configuration config = getConfiguration();

        Office365Connection o365Conn = Office365Connection.createConnection(config);

        Office365Connector conn = new Office365Connector();
        conn.init(config);

        Office365UserOps userOps = new Office365UserOps(conn);

        JSONObject obj = o365Conn.getRequest("/users/"+TEST_MANAGED_USER+"/?api-version="+Office365Connection.API_VERSION);

        Uid uid = new Uid(obj.getString("objectId"));
        // TODO actually assert assigned license is as passed

        boolean b = userOps.assignLicense(uid, LICENSE_PLAN+":EXCHANGE_S_STANDARD");
        Assert.assertTrue(b);
    }
    
    /*
     * END non federated
     */
    
    /*
     * Federated domain
     */

    @Test
    public void testCreateFederated() {
        Office365Configuration config = getConfiguration();

        Office365Connection o365Conn = Office365Connection.createConnection(config);

        String token = Office365Connection.createToken(config);
        Assert.assertNotNull(token);

        try {
            JSONObject obj = new JSONObject();
            obj.put("accountEnabled", true);
            obj.put("displayName", "OpenICF Test1");
            obj.put("mailNickname", "icf-test1");
            obj.put("userPrincipalName", TEST_FEDERATED_USER);
            JSONObject pwd = new JSONObject();
            pwd.put("password", "Test1234!");
            pwd.put("forceChangePasswordNextLogin", true);
            obj.put("passwordProfile", pwd);
            obj.put("usageLocation", "GB");

            String uuid = "7fcef92e-384f-4d44-b284-e64da703c0cf";
            
            obj.put("immutableId",  o365Conn.encodedUUID(uuid)); 

            LOGGER.info("About to create using  {0}", obj.toString());
            Uid uid = o365Conn.postRequest("/users?api-version="+Office365Connection.API_VERSION, obj);
            LOGGER.info("Got a UID of {0}", uid);
            Assert.assertNotNull(uid);
        } catch(JSONException je) {
            LOGGER.error(je, "Error creating test create structure");
        }
    }

    @Test(dependsOnMethods={"testCreateFederated"})
    public void testModifyFederated() {
        Office365Configuration config = getConfiguration();

        Office365Connection o365Conn = Office365Connection.createConnection(config);

        String token = Office365Connection.createToken(config);
        Assert.assertNotNull(token);

        try {
            JSONObject obj = new JSONObject();
            obj.put("department", "test");
            obj.put("usageLocation", "GB");

            boolean b = o365Conn.patchObject("/users/"+TEST_FEDERATED_USER+"?api-version="+Office365Connection.API_VERSION, obj);

            Assert.assertTrue(b);
        } catch(JSONException je) {
            LOGGER.error(je, "Error creating test modify structure");
        }
    }

    @Test(dependsOnMethods={"testModifyFederated", "testLicenseAssignmentFederated"}, enabled=false)
    public void testDeleteFederated() {
        Office365Configuration config = getConfiguration();

        Office365Connection o365Conn = Office365Connection.createConnection(config);

        String token = Office365Connection.createToken(config);
        Assert.assertNotNull(token);

        boolean b = o365Conn.deleteRequest("/users/"+TEST_FEDERATED_USER+"?api-version="+Office365Connection.API_VERSION);
        Assert.assertTrue(b);
    }
    
    @Test(dependsOnMethods={"testCreateFederated"})
    public void testLicenseAssignmentFederatedWholeSkuALlComponentsListed() throws JSONException {
        Office365Configuration config = getConfiguration();

        Office365Connection o365Conn = Office365Connection.createConnection(config);

        Office365Connector conn = new Office365Connector();
        conn.init(config);

        Office365UserOps userOps = new Office365UserOps(conn);

        JSONObject obj = o365Conn.getRequest("/users/"+TEST_FEDERATED_USER+"/?api-version="+Office365Connection.API_VERSION);

        Uid uid = new Uid(obj.getString("objectId"));
        // TODO actually assert assigned license is as passed

        boolean b = userOps.assignLicense(uid, LICENSE_PLAN+":EXCHANGE_S_ENTERPRISE:SHAREPOINTENTERPRISE:SHAREPOINTWAC:MCOSTANDARD:OFFICESUBSCRIPTION:RMS_S_ENTERPRISE:YAMMER_ENTERPRISE");
        Assert.assertTrue(b);
    }

    @Test(dependsOnMethods={"testCreateFederated"})
    public void testLicenseAssignmentFederatedWholeSku() throws JSONException {
        Office365Configuration config = getConfiguration();

        Office365Connection o365Conn = Office365Connection.createConnection(config);

        Office365Connector conn = new Office365Connector();
        conn.init(config);

        Office365UserOps userOps = new Office365UserOps(conn);

        JSONObject obj = o365Conn.getRequest("/users/"+TEST_FEDERATED_USER+"/?api-version="+Office365Connection.API_VERSION);

        Uid uid = new Uid(obj.getString("objectId"));
        // TODO actually assert assigned license is as passed

        boolean b = userOps.assignLicense(uid, LICENSE_PLAN);
        Assert.assertTrue(b);
    }
    
    @Test
    public void testLicenseAssignmentFederatedJustExchange() throws JSONException {
        Office365Configuration config = getConfiguration();

        Office365Connection o365Conn = Office365Connection.createConnection(config);

        Office365Connector conn = new Office365Connector();
        conn.init(config);

        Office365UserOps userOps = new Office365UserOps(conn);

        JSONObject obj = o365Conn.getRequest("/users/"+TEST_FEDERATED_USER+"/?api-version="+Office365Connection.API_VERSION);

        Uid uid = new Uid(obj.getString("objectId"));
        // TODO actually assert assigned license is as passed

        boolean b = userOps.assignLicense(uid, LICENSE_PLAN+":"+EXCHANGE_SERVICE);
        Assert.assertTrue(b);
    }
    
    /*
     * END federated domain
     */
    
    @Test
    public void testGetTenantDetails() {
        Office365Configuration config = getConfiguration();

        Office365Connection o365Conn = Office365Connection.createConnection(config);

        String token = Office365Connection.createToken(config);
        Assert.assertNotNull(token);
        
        JSONObject jo = o365Conn.getRequest("/tenantDetails?api-version="+Office365Connection.API_VERSION);
        Assert.assertNotNull(jo);
    }

    @Test
    public void testTest() {
        Office365Configuration config = getConfiguration();

        Office365Connection o365Conn = Office365Connection.createConnection(config);

        String token = Office365Connection.createToken(config);
        Assert.assertNotNull(token);
        
        o365Conn.test();
    }
    
    @Test
    public void testServicePlanIDRetrieval() {
        Office365Configuration config = getConfiguration();

        Office365Connection o365Conn = Office365Connection.createConnection(config);

        String planId = o365Conn.getServicePlanId(EXCHANGE_SERVICE);
        System.out.println("Service Plan ID: "+planId);
        Assert.assertNotNull(planId);
        Assert.assertEquals(planId, EXCHANGE_SERVICE_ID);
    }
    
    @Test
    public void testLicensePlanIDRetrieval() {
        Office365Configuration config = getConfiguration();

        Office365Connection o365Conn = Office365Connection.createConnection(config);

        String licenseId = o365Conn.getLicensePlanId(LICENSE_PLAN);
        System.out.println("Service license ID: "+licenseId);
        Assert.assertNotNull(licenseId);
        Assert.assertEquals(licenseId, LICNESE_PLAN_ID);
    }
    
    @Test
    public void testLicenseToJson() {
        Office365Configuration config = getConfiguration();

        Office365Connector conn = new Office365Connector();
        conn.init(config);

        Office365UserOps userOps = new Office365UserOps(conn);

        try {
            JSONObject obj = userOps.convertLicenseToJson(LICENSE_PLAN);
            System.out.println(obj);
            Assert.assertNotNull(obj);
        } catch (JSONException je) {
            System.err.println("Error with convertLicenseToJson");
            je.printStackTrace();
            Assert.assertTrue(false);
        }
    }
    
    @Test
    public void testLicenseToJsonPlanSpecified() {
        Office365Configuration config = getConfiguration();

        Office365Connector conn = new Office365Connector();
        conn.init(config);

        Office365UserOps userOps = new Office365UserOps(conn);

        try {
            JSONObject obj = userOps.convertLicenseToJson(LICENSE_PLAN+":"+EXCHANGE_SERVICE);
            System.out.println(obj);
            Assert.assertNotNull(obj);
        } catch (JSONException je) {
            System.err.println("Error with convertLicenseToJson");
            je.printStackTrace();
            Assert.assertTrue(false);
        }
    }
    
    @Test
    public void testGetFederatedDomainDetails() {
        Office365Configuration config = getConfiguration();

        Office365Connection o365Conn = Office365Connection.createConnection(config);

        Office365Domain dom = o365Conn.getDomain(TEST_FEDERATED_DOMAIN);
        Assert.assertNotNull(dom);
    }
    
    @Test
    public void testGetManagedDomainDetails() {
        Office365Configuration config = getConfiguration();

        Office365Connection o365Conn = Office365Connection.createConnection(config);

        Office365Domain dom = o365Conn.getDomain(TEST_MANAGED_DOMAIN);
        Assert.assertNotNull(dom);
    }
    
    @Test
    public void testIsUserInManagedDomain() {
        Office365Configuration config = getConfiguration();

        Office365Connection o365Conn = Office365Connection.createConnection(config);

        boolean b = o365Conn.isUserInAFederatedDomain(TEST_MANAGED_USER);

        Assert.assertFalse(b);
    }
    
    @Test
    public void testIsUserInFederatedDomain() {
        Office365Configuration config = getConfiguration();

        Office365Connection o365Conn = Office365Connection.createConnection(config);
        
        boolean b = o365Conn.isUserInAFederatedDomain(TEST_FEDERATED_USER);
        
        Assert.assertTrue(b);
    }
    
    @Test
    public void testUUIDEncodeMS() {
        Office365Configuration config = getConfiguration();

        config.setImmutableIDEncodeMechanism(Office365Configuration.ENCODE_MS_BASE64_STR);
        
        Office365Connection o365Conn = Office365Connection.createConnection(config);
        
        String guid = "14c6c0c6-66fb-4c3c-a28e-a22a3e778dc4";
        String expectedEncoding = "xsDGFPtmPEyijqIqPneNxA==";
        
        String enc = o365Conn.encodedUUID(guid);

        Assert.assertEquals(enc, expectedEncoding);
    }
    
    @Test
    public void testUUIDEncodeStandard() {
        Office365Configuration config = getConfiguration();

        config.setImmutableIDEncodeMechanism(Office365Configuration.ENCODE_STRAIGHT_BASE64_STR);
        
        Office365Connection o365Conn = Office365Connection.createConnection(config);
        
        String guid = "55c5435187826c4187606389254371bb";
        String expectedEncoding = "NTVjNTQzNTE4NzgyNmM0MTg3NjA2Mzg5MjU0MzcxYmI=";
        
        String enc = o365Conn.encodedUUID(guid);

        Assert.assertEquals(enc, expectedEncoding);
    }
    
    @Test
    public void testUUIDEncodeADFS() {
        Office365Configuration config = getConfiguration();

        config.setImmutableIDEncodeMechanism(Office365Configuration.ENCODE_MS_BASE64_OPENICF_ADFS_STR);
        
        Office365Connection o365Conn = Office365Connection.createConnection(config);
        
        String guid = "55c5435187826c4187606389254371bb";
        String expectedEncoding = "VcVDUYeCbEGHYGOJJUNxuw==";
        
        String enc = o365Conn.encodedUUID(guid);

        Assert.assertEquals(enc, expectedEncoding);
    }
    
    @Test
    public void testIsAttributeMultiValued() {
        Office365Configuration config = getConfiguration();

        Office365Connector conn = new Office365Connector();
        conn.init(config);
        
        boolean b = conn.isAttributeMultiValues(ObjectClass.ACCOUNT_NAME, "otherMails");
        
        Assert.assertTrue(b);
    }
    
    @Test
    public void testIsAttributeSingleValued() {
        Office365Configuration config = getConfiguration();

        Office365Connector conn = new Office365Connector();
        conn.init(config);
        
        boolean b = conn.isAttributeMultiValues(ObjectClass.ACCOUNT_NAME, "mailNickname");
        
        Assert.assertFalse(b);
    }
    
    @Test
    public void testIsDomainFederatedFed() {
        Office365Configuration config = getConfiguration();

        Office365Connection o365Conn = Office365Connection.createConnection(config);
        
        boolean b = o365Conn.isUserInAFederatedDomain(TEST_FEDERATED_USER);
        
        Assert.assertTrue(b);
    }
    
    @Test
    public void testIsDomainFederatedNonFed() {
        Office365Configuration config = getConfiguration();

        Office365Connection o365Conn = Office365Connection.createConnection(config);
        
        System.out.println("MANAGED: "+TEST_MANAGED_DOMAIN);
        
        boolean b = o365Conn.isUserInAFederatedDomain(TEST_MANAGED_DOMAIN);
        
        Assert.assertFalse(b);
    }
    
    /*
     * Contact
     */
    
    @Test(enabled=false)
    public void testCreateContact() {
        
        Office365Configuration config = getConfiguration();

        Office365Connector conn = new Office365Connector();
        conn.init(config);
        
        Office365ContactOps contactOps = new Office365ContactOps(conn);
        Uid uid = contactOps.createContact(null,  null);
        
        /*
        Office365Configuration config = getConfiguration();

        Office365Connection o365Conn = Office365Connection.createConnection(config);

        String token = Office365Connection.createToken(config);
        Assert.assertNotNull(token);

        try {
            JSONObject obj = new JSONObject();
            obj.put("displayName", "OpenICF Test1 Contact");
            obj.put("mailNickname", "icf-test1-contact");
            // obj.put("mail", "test@feb14-fed.pheaney-dev.co.uk");

            LOGGER.info("About to create using  {0}", obj.toString());
            Uid uid = o365Conn.postRequest("/contacts?api-version="+Office365Connection.API_VERSION, obj);
            LOGGER.info("Got a UID of {0}", uid);
            Assert.assertNotNull(uid);
        } catch(JSONException je) {
            LOGGER.error(je, "Error creating test contact structure");
        }
        */
    }
    
    /*
     * GROUPS
     */
    
    @Test(enabled=false)
    public void testCreateGroup() {
        Office365Configuration config = getConfiguration();

        Office365Connection o365Conn = Office365Connection.createConnection(config);

        String token = Office365Connection.createToken(config);
        Assert.assertNotNull(token);

        try {
            JSONObject obj = new JSONObject();
            obj.put("description", "Description");
            obj.put("displayName", "OpenICF Test1 Group");
            obj.put("mailNickname", "icf-test1-grp");
            // obj.put("mail", "icf-test1-grp@"+TEST_MANAGED_DOMAIN);
            obj.put("mailEnabled", false);
            obj.put("securityEnabled", true);


            LOGGER.info("About to create using  {0}", obj.toString());
            Uid uid = o365Conn.postRequest("/groups?api-version="+Office365Connection.API_VERSION, obj);
            LOGGER.info("Got a UID of {0}", uid);
            Assert.assertNotNull(uid);
        } catch(JSONException je) {
            LOGGER.error(je, "Error creating test create group");
        }
    }
    
    @Test(dependsOnMethods={"testCreateGroup"},enabled=false)
    public void testModifyGroup() {
        Office365Configuration config = getConfiguration();

        Office365Connection o365Conn = Office365Connection.createConnection(config);

        String token = Office365Connection.createToken(config);
        Assert.assertNotNull(token);

        try {
            JSONObject obj = new JSONObject();
            obj.put("department", "test");
            obj.put("usageLocation", "GB");

            boolean b = o365Conn.patchObject("/users/"+TEST_MANAGED_USER+"?api-version="+Office365Connection.API_VERSION, obj);

            Assert.assertTrue(b);
        } catch(JSONException je) {
            LOGGER.error(je, "Error creating test modify structure");
        }
    }
    
    /*
     * END GROUPS
     */
    
    private Office365Configuration getConfiguration() {
    	Office365Configuration o365 = new Office365Configuration();
    	
    	o365.setApiEndPoint(APIENDPOINT);
    	o365.setTenancy(TENANCY);
    	o365.setSymetricKey(SYMETRICKEY);
    	o365.setAuthURL(AUTHURL);
    	o365.setPrincipalID(PRINCIPALID);
    	o365.setResourceID(RESOURCEID);
    	o365.setAcsPrincipalID(ACSPRINCIPALID);
    	o365.setImmutableIDEncodeMechanism(IMMUTABLEID_ENCODE);
    	
    	return o365;
    }
    
    protected ConnectorFacade getFacade(Office365Configuration config) {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        // **test only**
        APIConfiguration impl = TestHelpers.createTestConfiguration(Office365Connector.class, config);
        return factory.newInstance(impl);
    }
}
