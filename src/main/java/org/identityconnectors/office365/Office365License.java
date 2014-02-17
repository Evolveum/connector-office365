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

/**
 * 
 * @author Paul Heaney
 *
 */
public class Office365License {
    
    private String objectID = null;
    private String skuID = null;
    private String skuPartNumber = null;
    
    private ArrayList<Office365ServicePlan> servicePlans = null;
    
    private int consumedUnits;
    private boolean compatabilityStatus;

    private int prepaidUnitsEnabled, prepaidUnitsSuspended, prepaidUnitsWarning = 0;
    
    public Office365License(String skuID) {
        this.skuID = skuID;
    }

    public String getObjectID() {
        return objectID;
    }

    public void setObjectID(String objectID) {
        this.objectID = objectID;
    }

    public String getSkuID() {
        return skuID;
    }

    public String getSkuPartNumber() {
        return skuPartNumber;
    }

    public void setSkuPartNumber(String skuPartNumber) {
        this.skuPartNumber = skuPartNumber;
    }

    public ArrayList<Office365ServicePlan> getServicePlans() {
        return servicePlans;
    }

    public void addServicePlan(Office365ServicePlan servicePlan) {
        if (this.servicePlans == null) {
            this.servicePlans = new ArrayList<Office365ServicePlan>();
        }
        
        this.servicePlans.add(servicePlan);
    }

    public int getConsumedUnits() {
        return consumedUnits;
    }

    public void setConsumedUnits(int consumedUnits) {
        this.consumedUnits = consumedUnits;
    }

    public boolean isCompatabilityStatus() {
        return compatabilityStatus;
    }

    public void setCompatabilityStatus(boolean compatabilityStatus) {
        this.compatabilityStatus = compatabilityStatus;
    }

    public int getPrepaidUnitsEnabled() {
        return prepaidUnitsEnabled;
    }

    public void setPrepaidUnitsEnabled(int prepaidUnitsEnabled) {
        this.prepaidUnitsEnabled = prepaidUnitsEnabled;
    }

    public int getPrepaidUnitsSuspended() {
        return prepaidUnitsSuspended;
    }

    public void setPrepaidUnitsSuspended(int prepaidUnitsSuspended) {
        this.prepaidUnitsSuspended = prepaidUnitsSuspended;
    }

    public int getPrepaidUnitsWarning() {
        return prepaidUnitsWarning;
    }

    public void setPrepaidUnitsWarning(int prepaidUnitsWarning) {
        this.prepaidUnitsWarning = prepaidUnitsWarning;
    }
}
