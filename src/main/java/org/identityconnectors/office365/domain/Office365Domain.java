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
package org.identityconnectors.office365.domain;

import java.util.ArrayList;
import java.util.Collections;


/**
 * @author Paul Heaney
 *
 */
public abstract class Office365Domain {

    public static final int DOMAIN_TYPE_ABSTRACT = -1;
    public static final int DOMAIN_TYPE_FEDERATED = 1;
    public static final int DOMAIN_TYPE_MANAGED = 2;

    private int domainType = DOMAIN_TYPE_ABSTRACT;
    
    private ArrayList<String> capabilities = null;
    private boolean defaultDomain = false;
    private String id = null;
    private boolean initial = false;
    private String name = null;
    
    public Office365Domain(String name, int type) {
        this.name = name;
        this.domainType = type;
    }
    
    public boolean isDefaultDomain() {
        return defaultDomain;
    }

    public void setDefaultDomain(boolean defaultDomain) {
        this.defaultDomain = defaultDomain;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isInitial() {
        return initial;
    }

    public void setInitial(boolean initial) {
        this.initial = initial;
    }

    public ArrayList<String> getCapabilities() {
        return capabilities;
    }

    public String getName() {
        return name;
    }
    
    public void initiliseCapability(String[] capabilitys) {
        this.capabilities = new ArrayList<String>();
        Collections.addAll(this.capabilities, capabilitys);
    }
    
    public void addCapability(String capability) {
        if (this.capabilities == null) {
            this.capabilities = new ArrayList<String>();
        }

        this.capabilities.add(capability);
    }

    public int getDomainType() {
        return domainType;
    }
}
