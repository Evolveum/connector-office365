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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.identityconnectors.common.Base64;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.office365.domain.Office365Domain;
import org.identityconnectors.office365.domain.Office365FederatedDomain;
import org.identityconnectors.office365.domain.Office365ManagedDomain;
import org.identityconnectors.office365.jsontoken.JWTTokenHelper;
import org.identityconnectors.office365.jsontoken.JsonWebToken;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to represent a Office365 Connection
 *
 * @author Paul Heaney
 * @version $Revision$ $Date$
 */
public class Office365Connection {

    static Log log = Log.getLog(Office365Connection.class);
    private Office365Configuration configuration;
    private String token = null; // TODO Handle expiring tokens
    public static final String API_VERSION = "2013-11-08";
    public static final Uid SUCCESS_UID = new Uid("fffffff-ffff-ffff-ffff-ffffffffffff");
    private Pattern directoryObjectGUIDPattern = Pattern.compile(".*directoryObjects/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})/.*");
    private HashMap<String, String> servicePlanIDs = null; // Hashmap of servicePlanName, servicePlanId
    private HashMap<String, Office365License> licenses = null; // partNumber, O365License Can you have more than one of the same plan? 
    private HashMap<String, Office365Domain> verifiedDomains = null;

    public static Office365Connection createConnection(Office365Configuration configuration) {
        String token = createToken(configuration);
        return new Office365Connection(configuration, token);
    }

    private Office365Connection(Office365Configuration configuration, String token) {
        this.configuration = configuration;
        this.token = token;
        log.ok("New Office365Connection for tenancy {0}", configuration.getTenancy());
    }

    public static String createToken(Office365Configuration configuration) {
        log.info("createConnection");
        JsonWebToken webToken;

        try {
            webToken = new JsonWebToken(configuration.getPrincipalID(),
                    configuration.getTenancy(),
                    (new URI(configuration.getAuthURL())).getHost(),
                    configuration.getAcsPrincipalID(),
                    JWTTokenHelper.getCurrentDateTime(),
                    60 * 60);

            final String[] clearText = new String[1];
            GuardedString.Accessor accessor = new GuardedString.Accessor() {
                @Override
                public void access(char[] clearChars) {
                    clearText[0] = new String(clearChars);

                }
            };
            configuration.getSymetricKey().access(accessor);

            try {
                String assertion = JWTTokenHelper.generateAssertion(webToken, clearText[0]);
                String resource = String.format("%s/%s@%s", configuration.getResourceID(), configuration.getApiEndPoint(), configuration.getTenancy());
                String token = JWTTokenHelper.getOAuthAccessTokenFromACS(configuration.getAuthURL(), assertion, resource);
                return token;
            } catch (Exception e) {
                log.error("Error creating token, error {0}", e);
            }
        } catch (URISyntaxException use) {
            log.error("Error connecting to authetication server {0} error is {1}", configuration.getAuthURL(), use);
        }

        return null;
    }

    private String getToken() {
        log.info("getToken called");
        if (this.token == null) {
            log.info("No currently valid token attempting to retrieve");
            int count = 0;
            while (count < Office365Configuration.MAX_RECONNECT_ATTEMPTS) {
                this.token = createToken(this.configuration);
                if (this.token != null) {
                    break;
                } else {
                    log.info("Failed to get token, attempting againt, request {0} of {1}", count, Office365Configuration.MAX_RECONNECT_ATTEMPTS);
                }
                count++;
            }
        }

        return this.token;
    }

    private void invalidateToken() {
        log.info("Token invalidated");
        this.token = null;
    }

    public JSONObject getRequest(String path) {
        log.info("getRequest(" + path + ")");

        HttpGet get = new HttpGet(getAPIEndPoint(path));

        get.addHeader("Authorization", this.getToken());
        get.addHeader("Content-Type", "application/json;odata=verbose");
        get.addHeader("DataServiceVersion", "1.0;NetFx");
        get.addHeader("MaxDataServiceVersion", "3.0;NetFx");
        get.addHeader("Accept", "application/json");

        HttpClient httpClient = HttpClientBuilder.create().build();

        try {
            HttpResponse response = httpClient.execute(get);
            HttpEntity entity = response.getEntity();

            if (response.getStatusLine().getStatusCode() != 200) {
                log.error("An error occured running a get operation");
                this.invalidateToken();
                StringBuffer sb = new StringBuffer();
                if (entity != null && entity.getContent() != null) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()));
                    String s = null;

                    log.info("Response :{0}", response.getStatusLine().toString());

                    while ((s = in.readLine()) != null) {
                        sb.append(s);
                    }
                }
                throw new ConnectorException("Error on get to " + path + ". Error code: " + response.getStatusLine().getStatusCode() + " Received the following response " + sb.toString());
            } else {
                StringBuffer sb = new StringBuffer();
                if (entity != null && entity.getContent() != null) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()));
                    String s = null;

                    log.info("Response :{0}", response.getStatusLine().toString());

                    while ((s = in.readLine()) != null) {
                        sb.append(s);
                    }
                }

                log.info("Received in response to getRequest ({0}) : {1}", path, sb.toString().trim());

                return new JSONObject(sb.toString().trim());
            }
        } catch (ClientProtocolException cpe) {
            log.error(cpe, "Error doing getRequest to path {0}", path);
            throw new ConnectorException("Exception whilst doing GET to " + path);
        } catch (IOException ioe) {
            log.error(ioe, "IOE Error doing getRequest to path {0}", path);
            throw new ConnectorException("Exception whilst doing GET to " + path);
        } catch (JSONException je) {
            log.error(je, "Error parsing JSON from get request to path {0}", path);
            throw new ConnectorException("Exception which converting to JSON " + path);
        }
    }

    public Uid postRequest(String path, JSONObject body) {

        log.info("postRequest(" + path + ")");

        HttpPost post = new HttpPost(getAPIEndPoint(path));
        post.addHeader("Authorization", this.getToken());
        // patch.addHeader("Content-Type", "application/json;odata=verbose");
        post.addHeader("Content-Type", "application/json;charset=utf-8;odata=verbose");
        post.addHeader("DataServiceVersion", "3.0;NetFx");
        post.addHeader("MaxDataServiceVersion", "3.0;NetFx");
        post.addHeader("Accept", "application/atom+xml");

        StringEntity postEntity = null;
        try {
            postEntity = new StringEntity(body.toString(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            log.error("Unsupported Encoding when creating object in Office 365, path was {0}. Error: {1}", path, ex);
        }

        post.setEntity(postEntity);
        HttpClient httpClient = HttpClientBuilder.create().build();

        try {
            HttpResponse response = httpClient.execute(post);
            HttpEntity entity = response.getEntity();

            log.info("Status code from postRequest is {0}", response.getStatusLine().getStatusCode());

            // assignLicense returns 200

            if ((response.getStatusLine().getStatusCode() != 201 && !path.contains("/assignLicense?")) || response.getStatusLine().getStatusCode() == 400) {
                log.error("An error occured when creating object in Office 365, path was {0}", path);
                this.invalidateToken();
                StringBuffer sb = new StringBuffer();
                if (entity != null && entity.getContent() != null) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()));
                    String s = null;

                    log.info("Response :{0}", response.getStatusLine().toString());

                    while ((s = in.readLine()) != null) {
                        sb.append(s);
                        log.info(s);
                    }
                }
                log.error("Error on post to {0}  and body of {1}. Error code: {2} Received the following response: {3}", path, body.toString(), response.getStatusLine().getStatusCode(), sb.toString());
                throw new Office365Exception(response.getStatusLine().getStatusCode(), sb.toString());
                //throw new ConnectorException("Error on post to " + path + " and body of " + body.toString() + ". Error code: " + response.getStatusLine().getStatusCode() + " Received the following response " + sb.toString());
                
            } else if (path.contains("/assignLicense?") && response.getStatusLine().getStatusCode() == 200) {
                return SUCCESS_UID;
            } else {
                Header[] location = response.getHeaders("Location");
                // Location: https://directory.windows.net/contoso.onmicrosoft.com/directoryObjects/4e971521-101a-4311-94f4-0917d7218b4e/Microsoft.WindowsAzure.ActiveDirectory.User
                Matcher m = directoryObjectGUIDPattern.matcher(location[0].getValue());
                boolean b = m.matches();
                if (b) {
                    String guid = m.group(1);
                    log.info("Object has GUID of {0}", guid);
                    return new Uid(guid);
                } else {
                    log.error("No GUID found on path {0}", path);
                    throw new ConnectorException("No GUID found for " + path + " and body of " + body.toString());
                }
            }
        } catch (ClientProtocolException cpe) {
            log.error(cpe, "Error doing postRequest to path {0}", path);
            throw new ConnectorException("Exception whilst doing POST to " + path);
        } catch (IOException ioe) {
            log.error(ioe, "IOE Error doing postRequest to path {0}", path);
            throw new ConnectorException("Exception whilst doing POST to " + path);
        }
    }

    public boolean patchObject(String path, JSONObject body) {
        log.info("patchRequest(" + path + ")");

        // http://msdn.microsoft.com/en-us/library/windowsazure/dn151671.aspx
        HttpPatch httpPatch = new HttpPatch(getAPIEndPoint(path));
        httpPatch.addHeader("Authorization", this.getToken());
        // patch.addHeader("Content-Type", "application/json;odata=verbose");
        httpPatch.addHeader("Content-Type", "application/json;charset=utf-8;odata=verbose");
        httpPatch.addHeader("DataServiceVersion", "3.0;NetFx");
        httpPatch.addHeader("MaxDataServiceVersion", "3.0;NetFx");
        httpPatch.addHeader("Accept", "application/atom+xml");

        StringEntity postEntity = null;
        try {
            postEntity = new StringEntity(body.toString(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            log.error("Unsupported Encoding when updating object in Office 365, path was {0}. Error: {1}", path, ex);
        }

        httpPatch.setEntity(postEntity);
        HttpClient httpClient = HttpClientBuilder.create().build();

        try {
            HttpResponse response = httpClient.execute(httpPatch);
            HttpEntity entity = response.getEntity();

            if (response.getStatusLine().getStatusCode() != 204) {
                log.error("An error occured when modify an object in Office 365");
                this.invalidateToken();
                StringBuffer sb = new StringBuffer();
                if (entity != null && entity.getContent() != null) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()));
                    String s = null;

                    log.info("Response :{0}", response.getStatusLine().toString());

                    while ((s = in.readLine()) != null) {
                        sb.append(s);
                        log.info(s);
                    }
                }
                throw new ConnectorException("Modify Object failed to " + path + " and body of " + body.toString() + ". Error code was " + response.getStatusLine().getStatusCode() + ". Received the following response " + sb.toString());
            } else {
                return true;
            }
        } catch (ClientProtocolException cpe) {
            log.error(cpe, "Error doing patchRequest to path {0}", path);
            throw new ConnectorException("Exception whilst doing PATCH to " + path);
        } catch (IOException ioe) {
            log.error(ioe, "IOE Error doing patchRequest to path {0}", path);
            throw new ConnectorException("Exception whilst doing PATCH to " + path);
        }
    }

    public boolean deleteRequest(String path) {
        log.info("deleteRequest(" + path + ")");
        // http://msdn.microsoft.com/en-us/library/windowsazure/dn151676.aspx
        HttpDelete httpDelete = new HttpDelete(getAPIEndPoint(path));
        httpDelete.addHeader("Authorization", this.getToken());
        httpDelete.addHeader("Content-Type", "application/json");
        httpDelete.addHeader("DataServiceVersion", "3.0;NetFx");
        httpDelete.addHeader("MaxDataServiceVersion", "3.0;NetFx");

        HttpClient httpClient = HttpClientBuilder.create().build();

        try {
            HttpResponse response = httpClient.execute(httpDelete);
            HttpEntity entity = response.getEntity();

            if (response.getStatusLine().getStatusCode() != 204) {
                log.error("An error occured when deleting an object in Office 365");
                this.invalidateToken();
                StringBuffer sb = new StringBuffer();
                if (entity != null && entity.getContent() != null) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()));
                    String s = null;

                    log.info("Response :{0}", response.getStatusLine().toString());

                    while ((s = in.readLine()) != null) {
                        sb.append(s);
                        log.info(s);
                    }
                }
                throw new ConnectorException("Delete Object failed to " + path + ". Error code was " + response.getStatusLine().getStatusCode() + ". Received the following response " + sb.toString());
            } else {
                return true;
            }
        } catch (ClientProtocolException cpe) {
            log.error(cpe, "Error doing deleteRequest to path {0}", path);
            throw new ConnectorException("Exception whilst doing DELETE to " + path);
        } catch (IOException ioe) {
            log.error(ioe, "IOE Error doing deleteRequest to path {0}", path);
            throw new ConnectorException("Exception whilst doing DELETE to " + path);
        }
    }

    public String getServicePlanId(String planName) {
        if ((this.servicePlanIDs == null) || (this.servicePlanIDs.size() == 0)) {
            populateSKUs();
        }

        if (this.servicePlanIDs != null) {
            return this.servicePlanIDs.get(planName);
        } else {
            return null;
        }
    }

    public String getLicensePlanId(String licenseName) {
        Office365License lic = getLicensePlan(licenseName);

        if (lic != null) {
            return lic.getSkuID();
        } else {
            return null;
        }
    }

    public Office365License getLicensePlan(String licenseName) {
        if (this.licenses == null || this.licenses.size() == 0) {
            populateSKUs();
        }

        return this.licenses.get(licenseName);
    }

    private void populateSKUs() {
        log.info("populateSKUs");
        this.licenses = new HashMap<String, Office365License>();
        this.servicePlanIDs = new HashMap<String, String>();

        JSONObject obj = getRequest("/subscribedSkus?api-version=" + Office365Connection.API_VERSION);
        try {
            JSONArray skus = obj.getJSONArray("value");
            for (int i = 0; i < skus.length(); i++) {
                JSONObject sku = skus.getJSONObject(i);

                String skuID = sku.getString("skuId");
                String skuPartNumber = sku.getString("skuPartNumber");

                Office365License license = new Office365License(skuID);
                license.setSkuPartNumber(skuPartNumber);
                license.setObjectID(sku.getString("objectId"));
                license.setConsumedUnits(sku.getInt("consumedUnits"));
                JSONObject prepaidUnits = sku.getJSONObject("prepaidUnits");
                license.setPrepaidUnitsEnabled(prepaidUnits.getInt("enabled"));
                license.setPrepaidUnitsSuspended(prepaidUnits.getInt("suspended"));
                license.setPrepaidUnitsWarning(prepaidUnits.getInt("warning"));

                JSONArray servicePlans = sku.getJSONArray("servicePlans");

                for (int j = 0; j < servicePlans.length(); j++) {
                    JSONObject planObj = servicePlans.getJSONObject(j);
                    String planID = planObj.getString("servicePlanId");
                    String planName = planObj.getString("servicePlanName");
                    Office365ServicePlan plan = new Office365ServicePlan(planID, planName);
                    license.addServicePlan(plan);

                    this.servicePlanIDs.put(planName, planID);
                }

                this.licenses.put(skuPartNumber, license);
            }
        } catch (JSONException je) {
            log.error(je, "Error populating skus");
        }
    }

    public Office365Domain getDomain(String name) {
        if (this.verifiedDomains == null || this.verifiedDomains.size() == 0) {
            populateVerifiedDomains();
        }

        return this.verifiedDomains.get(name.toLowerCase());
    }

    private void populateVerifiedDomains() {
        log.info("populateVerifiedDomains");
        this.verifiedDomains = new HashMap<String, Office365Domain>();


        JSONObject obj = getRequest("/tenantDetails?api-version=" + Office365Connection.API_VERSION);
        try {
            JSONArray verifiedDomains = obj.getJSONArray("value").getJSONObject(0).getJSONArray("verifiedDomains");

            for (int i = 0; i < verifiedDomains.length(); i++) {
                JSONObject domainObj = verifiedDomains.getJSONObject(i);

                Office365Domain domain = null;
                String name = domainObj.getString("name").toLowerCase();
                String type = domainObj.getString("type");
                if (type.equals("Federated")) {
                    log.info("Got a Federated domain named {0}", name);
                    domain = new Office365FederatedDomain(name);
                    this.verifiedDomains.put(name, domain);
                } else if (type.equals("Managed")) {
                    log.info("Got a Managed domain named {0}", name);
                    domain = new Office365ManagedDomain(name);
                    this.verifiedDomains.put(name, domain);
                } else if (type.equals("None")) {
                    log.info("Received a None domain for {0},  skipping", name);
                } else {
                    log.error("Unrecognised type of {0} passed for domain {1}", type, domain);
                    throw new ConnectorException("Unrecognised domain type of " + type + " received for doman " + name);
                }

                if (domain != null) {
                    domain.initiliseCapability(domainObj.getString("capabilities").split(","));
                    domain.setDefaultDomain(domainObj.getBoolean("default"));
                    domain.setId(domainObj.getString("id"));
                    domain.setInitial(domainObj.getBoolean("initial"));
                }
            }

            log.info("Finished reading verified domains");
        } catch (JSONException je) {
            log.error(je, "Error populating verified domains");
        }
    }

    public boolean isUserInAFederatedDomain(String userPrinciaplName) {
        log.info("isUserInAFederatedDomain {0}", userPrinciaplName);

        String[] parts = userPrinciaplName.split("@");

        Office365Domain dom = getDomain(parts[1]);

        if (dom != null) {
            if (dom.getDomainType() == Office365Domain.DOMAIN_TYPE_FEDERATED) {
                return true;
            } else {
                return false;
            }
        } else {
            throw new ConnectorException("Domain for " + userPrinciaplName + " does not exist");
        }
    }

    private String getAPIEndPoint(String path) {
        log.info("API path is: {0}", path);
        return this.configuration.getProtocol() + this.configuration.getApiEndPoint() + "/" + this.configuration.getTenancy() + path;
    }

    String encodedUUID(String uuid) {
        log.info("Encoding uuid {0} with mechanism '{1}'", uuid, this.configuration.getImmutableIDEncodeMechanism());
        if (this.configuration.getImmutableIDEncodeMechanism().equals(Office365Configuration.ENCODE_MS_BASE64_STR)) {
            log.info("Encoding UUID with Microsoft format");
            return Office365Utils.encodeUUIDInMicrosoftFormat(uuid);
        } else if (this.configuration.getImmutableIDEncodeMechanism().equals(Office365Configuration.ENCODE_MS_BASE64_OPENICF_ADFS_STR)) {
            log.info("Doing base64 encode in ADFS compatible format");
            return Office365Utils.encodeUUIDInMicrosoftADFSFormat(uuid);
        } else if (this.configuration.getImmutableIDEncodeMechanism().equals(Office365Configuration.ENCODE_STRAIGHT_BASE64_STR)) {
            log.info("Encoding UUID with standard base64");
            try {
                return Base64.encode(uuid.getBytes("ISO-8859-1"));
            } catch (UnsupportedEncodingException uee) {
                throw new ConnectorException("unable to convert uuid " + uuid + "to MS format", uee);
            }
        } else {
            log.info("No encoding of UUID, returning unaltered");
            return uuid;
        }
    }

    /**
     * Release internal resources
     */
    public void dispose() {
        //implementation
    }

    /**
     * If internal connection is not usable, throw IllegalStateException
     */
    public void test() {
        JSONObject tenantDetails = getRequest("/tenantDetails?api-version=" + Office365Connection.API_VERSION);

        try {
            JSONArray ja = tenantDetails.getJSONArray("value").getJSONObject(0).getJSONArray("assignedPlans");
            if (ja == null || ja.length() == 0) {
                throw new IllegalStateException("No plans assigned to tenancy");
            }
        } catch (JSONException je) {
            log.error(je, "Error testing connection");
            throw new IllegalStateException("Error during test, JSONException thrown");
        }
    }
}
