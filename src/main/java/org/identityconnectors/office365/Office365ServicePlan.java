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

/**
 * 
 * @author Paul Heaney
 *
 */
public class Office365ServicePlan {

    private String servicePlanID = null;
    private String servicePlanName = null;
    
    public Office365ServicePlan(String servicePlanID, String servicePlanName) {
        this.servicePlanID = servicePlanID;
        this.servicePlanName = servicePlanName;
    }
    
    public String getServicePlanID() {
        return servicePlanID;
    }

    public String getServicePlanName() {
        return servicePlanName;
    }
}
