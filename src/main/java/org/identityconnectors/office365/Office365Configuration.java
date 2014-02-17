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

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;


/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the Office365 Connector.
 *
 * @author Paul Heaney 
 * @version $Revision$ $Date$
 */
public class Office365Configuration extends AbstractConfiguration {


    private final String protocol = "https://"; // Not configurable
    private String apiEndPoint = "graph.windows.net";
    private String tenancy = null; // e.g. live.salfordsoftware.co.uk
    private GuardedString symetricKey = null;
    private String authURL = "https://accounts.accesscontrol.windows.net/tokens/OAuth/2";
    private String principalID = null; // aa8de79f-4c6a-4f81-bcc7-61262226256a
    private String resourceID = "00000002-0000-0000-c000-000000000000";
    private String acsPrincipalID = "00000001-0000-0000-c000-000000000000";
    

    /**
     * Constructor
     */
    public Office365Configuration() {

    }

    public String getProtocol() {
    	return protocol;
    }
    
    @ConfigurationProperty(order = 1, displayMessageKey = "apiEndPoint.display",
            groupMessageKey ="basic.group", helpMessageKey = "apiEndPoint.help",
            required = true, confidential = false)
    public String getApiEndPoint() {
        return apiEndPoint;
    }

    public void setApiEndPoint(String apiEndPoint) {
        this.apiEndPoint = apiEndPoint;
    }


    @ConfigurationProperty(order = 2, displayMessageKey = "tenancy.display",
            groupMessageKey ="basic.group", helpMessageKey = "tenancy.help",
            required = true, confidential = false)
    public String getTenancy() {
        return tenancy;
    }

    public void setTenancy(String tenancy) {
        this.tenancy = tenancy;
    }

    @ConfigurationProperty(order = 3, displayMessageKey = "symetricKey.display",
            groupMessageKey ="basic.group", helpMessageKey = "symetricKey.help",
            confidential = true)
    public GuardedString getSymetricKey() {
        return symetricKey;
    }

    public void setSymetricKey(GuardedString symetricKey) {
        this.symetricKey = symetricKey;
    }
    
    @ConfigurationProperty(order = 4, displayMessageKey = "authURL.display",
            groupMessageKey ="basic.group", helpMessageKey = "authURL.help",
            confidential = false)
    public String getAuthURL() {
        return authURL;
    }

    public void setAuthURL(String authURL) {
        this.authURL = authURL;
    }

    @ConfigurationProperty(order = 5, displayMessageKey = "principalID.display",
            groupMessageKey ="basic.group", helpMessageKey = "principalID.help",
            confidential = false)
    public String getPrincipalID() {
        return principalID;
    }

    public void setPrincipalID(String principalID) {
        this.principalID = principalID;
    }
    
    @ConfigurationProperty(order = 6, displayMessageKey = "resourceID.display",
            groupMessageKey ="basic.group", helpMessageKey = "resourceID.help",
            confidential = false)
    public String getResourceID() {
        return resourceID;
    }

    public void setResourceID(String resourceID) {
        this.resourceID = resourceID;
    }
    
    @ConfigurationProperty(order = 7, displayMessageKey = "acsPrincipalID.display",
            groupMessageKey ="basic.group", helpMessageKey = "acsPrincipalID.help",
            confidential = false)
    public String getAcsPrincipalID() {
        return acsPrincipalID;
    }

    public void setAcsPrincipalID(String acsPrincipalID) {
        this.acsPrincipalID = acsPrincipalID;
    }
    
    /**
     * {@inheritDoc}
     */
    public void validate() {
        if (StringUtil.isBlank(apiEndPoint)) {
            throw new IllegalArgumentException("API Endpoint cannot be null or empty.");
        }

        if (StringUtil.isBlank(tenancy)) {
            throw new IllegalArgumentException("Tenancy cannot be null or empty.");
        }

        if (symetricKey == null) {
        	throw new IllegalArgumentException("Symetric Key cannot be null or empty.");
        }
        
        if (StringUtil.isBlank(authURL)) {
            throw new IllegalArgumentException("Authentication URL cannot be null or empty.");
        }
        
        if (StringUtil.isBlank(principalID)) {
            throw new IllegalArgumentException("Principal ID cannot be null or empty.");
        }
        
        if (StringUtil.isBlank(resourceID)) {
            throw new IllegalArgumentException("Resource ID cannot be null or empty.");
        }
        
        if (StringUtil.isBlank(acsPrincipalID)) {
            throw new IllegalArgumentException("ACS Principal ID cannot be null or empty.");
        }
    }

}
