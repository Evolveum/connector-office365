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

import java.util.EnumSet;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

/**
 * Main implementation of the Office365 Connector
 * 
 * @author Paul Heaney
 * @version $Revision$ $Date$
 */
@ConnectorClass(displayNameKey = "Office365.connector.display", configurationClass = Office365Configuration.class)
public class Office365Connector implements 
        Connector,
        CreateOp,
        DeleteOp,
        SearchOp<String>,
        TestOp,
        UpdateOp,
        SchemaOp
    {

    public static final String LICENSE_ATTR = "licenses";
    public static final String USAGELOCATION_ATTR = "usageLocation";
    public static final String IMMUTABLEID_ATTR = "immutableId";
    
    /**
     * Setup logging for the {@link Office365Connector}.
     */
    private static final Log log = Log.getLog(Office365Connector.class);

    /**
     * Place holder for the Connection created in the init method
     */
    private Office365Connection connection;

    /**
     * Place holder for the {@link Configuration} passed into the init() method
     * {@link Office365Connector#init(org.identityconnectors.framework.spi.Configuration)}
     * .
     */
    private Office365Configuration configuration;

    private Schema schema = null;

    private Office365UserOps userOps;
    
    /**
     * Gets the Configuration context for this connector.
     */
    public Configuration getConfiguration() {
        return this.configuration;
    }

    /**
     * Callback method to receive the {@link Configuration}.
     * 
     * @see org.identityconnectors.framework.spi.Connector#init(org.identityconnectors.framework.spi.Configuration)
     */
    public void init(Configuration configuration) {
        this.configuration = (Office365Configuration) configuration;
        this.userOps = new Office365UserOps(this);
    }

    /**
     * Disposes of the {@link Office365Connector}'s resources.
     * 
     * @see org.identityconnectors.framework.spi.Connector#dispose()
     */
    public void dispose() {
        configuration = null;
        if (connection != null) {
            connection.dispose();
            connection = null;
        }
    }

    /******************
     * SPI Operations
     * 
     * Implement the following operations using the contract and description
     * found in the Javadoc for these methods.
     ******************/

    /**
     * {@inheritDoc}
     */
    public Uid create(final ObjectClass objectClass, final Set<Attribute> createAttributes, final OperationOptions options) {

        log.info("Entered create for objectClass {0}", objectClass);

        Name name = AttributeUtil.getNameFromAttributes(createAttributes);
        if (name == null) {
            log.error("Name attribute is empty");
            throw new IllegalArgumentException("Name is mandatory on create events");
        }
        
        log.info("Name of {0} passed", name.getNameValue());

        if (objectClass.equals(ObjectClass.ACCOUNT)) {
        	return userOps.createUser(name, createAttributes);
        } else {
        	log.error("Invalid objectClass {0} specified", objectClass.getObjectClassValue());
        	throw new IllegalArgumentException("Unsupported objectClass of "+objectClass.getObjectClassValue()+" specified");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void delete(final ObjectClass objectClass, final Uid uid, final OperationOptions options) {

        log.info ("In delete");

        if (uid == null || (uid.getUidValue() == null)) {
            log.error("No UID specified for update");
            throw new IllegalArgumentException("No UID specified for update");
        }

        log.ok("UID of {0} is present", uid.getUidValue());
        
        if (objectClass.equals(ObjectClass.ACCOUNT)) {
           this.userOps.deleteUser(uid);
        } else {
            log.info("Unsupported objectClass {0} passed to delete", objectClass.getObjectClassValue());
            throw new IllegalArgumentException("Class of type "+objectClass.getObjectClassValue()+" not supported");
        }

    }

    /**
     * {@inheritDoc}
     */
    public FilterTranslator<String> createFilterTranslator(ObjectClass objectClass, OperationOptions options) {
        return new Office365FilterTranslator();
    }

    /**
     * {@inheritDoc}
     */
    public void executeQuery(ObjectClass objectClass, String query, ResultsHandler handler, OperationOptions options) {
        // http://msdn.microsoft.com/en-us/library/windowsazure/jj126255.aspx
        log.info("Execute query for {0}", objectClass);

        if (handler == null) {
            throw new IllegalArgumentException("Null Results Handler");
        }
        
        log.ok("Handler OK");
        
        if (objectClass.equals(ObjectClass.ACCOUNT)) {
            this.userOps.queryUser(query, handler, options);
        } else {
            log.info("Unsupported objectClass {0} passed to query", objectClass.getDisplayNameKey());
            throw new IllegalArgumentException("Unsupported object class "+objectClass.getDisplayNameKey());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void test() {
        log.info("test");

        this.getConnection().test();

        log.info("test ok");

        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Uid update(ObjectClass objectClass, Uid uid, Set<Attribute> replaceAttributes, OperationOptions options) {

        log.info("Update for objectClass {0}", objectClass);

        if (objectClass.equals(ObjectClass.ACCOUNT)) {
            return userOps.updateUser(uid, replaceAttributes, options);
        } else {
            log.error("Invalid objectClass {0} specified", objectClass.getObjectClassValue());
            throw new IllegalArgumentException("Unsupported objectClass of "+objectClass.getObjectClassValue()+" specified");
        }
    }

    private void buildSchema() {
        log.info("Build Schema");

        // Supports just user Class at the moment
        // Based on
        // https://directory.windows.net/live.salfordsoftware.co.uk/$metadata
        // NOTE: not retreiving schema from above as some fields are read-only
        // though not indicated, others are complex objects

        SchemaBuilder schemaBuilder = new SchemaBuilder(
                Office365Connector.class);

        // We only support Users at the moment
        ObjectClassInfoBuilder objectClassInfoBuilderUser = new ObjectClassInfoBuilder();
        objectClassInfoBuilderUser.setType(ObjectClass.ACCOUNT_NAME);

        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build("accountEnabled", Boolean.class, EnumSet.of(Flags.REQUIRED)));
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build("city", String.class));
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build("country", String.class));
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build("department", String.class));
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build("displayName", String.class));
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build("facsimileTelephoneNumber", String.class));
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build("givenName", String.class));
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build("jobTitle", String.class));
        // license format   licensename:planname:planname:...
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build(LICENSE_ATTR, String.class));
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build("mail", String.class));
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build("mailNickname", String.class, EnumSet.of(Flags.REQUIRED)));
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build("mobile", String.class));
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build("otherMails", String.class, EnumSet.of(Flags.MULTIVALUED)));

        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build("forceChangePasswordNextLogin", Boolean.class));
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build("physicalDeliveryOfficeName", String.class));
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build("postalCode", String.class));
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build("preferredLanguage", String.class));
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build("proxyAddresses", String.class, EnumSet.of(Flags.MULTIVALUED)));
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build("state", String.class));
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build("streetAddress", String.class));
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build("surname", String.class));
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build("telephoneNumber", String.class));
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build("thumbnailPhoto", byte[].class));  

        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build(IMMUTABLEID_ATTR, String.class)); // Mandatory if its a federated domain
        
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build(USAGELOCATION_ATTR, String.class));
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, EnumSet.of(Flags.REQUIRED)));  // userPrinciaplName 
        // Operation Attributes
        objectClassInfoBuilderUser.addAttributeInfo(AttributeInfoBuilder.build(OperationalAttributes.PASSWORD_NAME, GuardedString.class, EnumSet.of(Flags.NOT_READABLE, Flags.NOT_RETURNED_BY_DEFAULT)));

        ObjectClassInfo oci = objectClassInfoBuilderUser.build();
        schemaBuilder.defineObjectClass(oci);

        ObjectClassInfoBuilder objectClassInfoBuilderGroup = new ObjectClassInfoBuilder();
        objectClassInfoBuilderGroup.setType(ObjectClass.GROUP_NAME);
        objectClassInfoBuilderGroup.addAttributeInfo(AttributeInfoBuilder.build("description", String.class));
        objectClassInfoBuilderGroup.addAttributeInfo(AttributeInfoBuilder.build("displayName", String.class, EnumSet.of(Flags.REQUIRED)));
        objectClassInfoBuilderGroup.addAttributeInfo(AttributeInfoBuilder.build("mail", String.class, EnumSet.of(Flags.NOT_CREATABLE, Flags.NOT_UPDATEABLE)));  // THIS IS read only
        objectClassInfoBuilderGroup.addAttributeInfo(AttributeInfoBuilder.build("mailEnabled", Boolean.class, EnumSet.of(Flags.REQUIRED)));
        objectClassInfoBuilderGroup.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, EnumSet.of(Flags.REQUIRED))); // mailNickname
        objectClassInfoBuilderGroup.addAttributeInfo(AttributeInfoBuilder.build("proxyAddresses", String.class, EnumSet.of(Flags.MULTIVALUED)));
        objectClassInfoBuilderGroup.addAttributeInfo(AttributeInfoBuilder.build("securityEnabled", Boolean.class, EnumSet.of(Flags.REQUIRED)));
        objectClassInfoBuilderGroup.addAttributeInfo(AttributeInfoBuilder.build("members", String.class, EnumSet.of(Flags.MULTIVALUED)));

        this.schema = schemaBuilder.build();
    }

    @Override
    public Schema schema() {

        log.info("Schema");

        if (this.schema == null) {
            // Need to retrieve
            log.info("Need to retrieve schema");
            buildSchema();
            log.info("Built Schema");
        }

        log.info("Got schema");

        return this.schema;

    }

    public boolean isAttributeMultiValues(String objectClass, String attrName) {
        Schema schema = this.schema();
        ObjectClassInfo oci = schema.findObjectClassInfo(objectClass);
        for (AttributeInfo ai : oci.getAttributeInfo()) {
            if(ai.getName().equals(attrName)) {
                if (ai.getFlags().contains(Flags.MULTIVALUED)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public Office365Connection getConnection() {
    	if (this.connection == null) {
    		this.configuration.validate();
    		this.connection = Office365Connection.createConnection(configuration);
    	} 
    	
    	return this.connection;
    }
}
