/*
*Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.appmanager.integration.test.cases.publisher.webapp.lifecycle.management;

import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.appmanager.integration.utils.APPMPublisherRestClient;
import org.wso2.appmanager.integration.utils.AppmTestConstants;
import org.wso2.appmanager.integration.utils.WebAppUtil;
import org.wso2.appmanager.integration.utils.bean.PolicyGroup;
import org.wso2.appmanager.integration.utils.bean.WebApp;
import org.wso2.appmanager.integration.utils.bean.WebAppResource;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * This Test class verifies the ability of appCreator, appPublisher and admin users of changing web app life cycle
 * state from 'In review' to 'Approve'
 */
public class ChangeStateFromInReviewToApproveTestCase {
    private static final String TEST_DESCRIPTION = "Verify approving a web app in-review";
    private static AutomationContext appMServer;
    private APPMPublisherRestClient appmPublisherRestClient;
    private String appName = "ChangeStateFromInReviewToApproveTestCase";
    private String appVersion = "1.0.0";
    private String context = "/" + appName;
    private String backEndUrl;
    private String adminUserName;
    private String adminPassword;
    private String app1Uuid;
    private String app2Uuid;
    private String app3Uuid;


    @BeforeClass(alwaysRun = true)
    public void startUp() throws Exception {
        appMServer = new AutomationContext(AppmTestConstants.APP_MANAGER, TestUserMode.SUPER_TENANT_ADMIN);
        backEndUrl = appMServer.getContextUrls().getWebAppURLHttps();
        appmPublisherRestClient = new APPMPublisherRestClient(backEndUrl);

        User adminUser = appMServer.getSuperTenant().getTenantAdmin();
        adminUserName = adminUser.getUserName();
        adminPassword = adminUser.getPassword();
        // Login to publisher by admin.
        appmPublisherRestClient.login(adminUserName, adminPassword);

        // Multiple web apps are created for multiple users.
        app1Uuid = createWebAppAndSubmitForReview("1");
        app2Uuid = createWebAppAndSubmitForReview("2");
        app3Uuid = createWebAppAndSubmitForReview("3");
    }

    @Test(dataProvider = "validUserModeDataProvider", description = TEST_DESCRIPTION)
    public void testChangeStateFromInReviewToApproveWithValidUsers(String userName, String password, String uuid)
            throws Exception {
        APPMPublisherRestClient publisherRestClient = new APPMPublisherRestClient(backEndUrl);
        // Login to publisher by valid user.
        publisherRestClient.login(userName, password);
        HttpResponse httpResponse = publisherRestClient.changeState(uuid, AppmTestConstants.LifeCycleStatus.APPROVE);
        JSONObject responseData = new JSONObject(httpResponse.getData());
        // Logout from publisher by valid user.
        publisherRestClient.logout();
        int responseCode = httpResponse.getResponseCode();
        assertTrue(responseCode == 200, "Excepted status code is 200 for user :" + userName + ". But received status " +
                "code is " + responseCode);
        assertEquals(responseData.getString(AppmTestConstants.STATUS), "Success", "Changing web app life cycle state " +
                "from in review to approve failed for user : " + userName + " who has sufficient privileges to change" +
                " life cycle status.");
    }

    @Test(dataProvider = "inValidUserModeDataProvider", description = TEST_DESCRIPTION)
    public void testChangeStateFromInReviewToApproveWithInValidUsers(String userName, String password, String uuid)
            throws Exception {
        APPMPublisherRestClient publisherRestClient = new APPMPublisherRestClient(backEndUrl);
        // Login to publisher by invalid user.
        publisherRestClient.login(userName, password);
        HttpResponse httpResponse = publisherRestClient.changeState(uuid, AppmTestConstants.LifeCycleStatus.APPROVE);
        JSONObject responseData = new JSONObject(httpResponse.getData());
        // Logout from publisher by invalid user.
        publisherRestClient.logout();
        int responseCode = httpResponse.getResponseCode();
        assertTrue(responseCode == 401, "Excepted status code is 401 for user :" + userName + ". But received " +
                "status code is " + responseCode);
        assertEquals(responseData.getString(AppmTestConstants.STATUS), "Access Denied", "Changing web app life cycle " +
                "state from in review to approve allowed for user : " + userName + " who has insufficient privileges " +
                "to change life cycle status.");
    }

    @AfterClass(alwaysRun = true)
    public void closeDown() throws Exception {
        // Deleted created web app by admin.
        appmPublisherRestClient.deleteApp(app1Uuid);
        appmPublisherRestClient.deleteApp(app2Uuid);
        appmPublisherRestClient.deleteApp(app3Uuid);
        // Logout from publisher by admin.
        appmPublisherRestClient.logout();
    }

    private String createWebAppAndSubmitForReview (String appPrefix) throws Exception {
        PolicyGroup defaultPolicyGroup = WebAppUtil.createDefaultPolicy();
        HttpResponse response = appmPublisherRestClient.addPolicyGroup(defaultPolicyGroup);
        String policyId = WebAppUtil.getPolicyId(response);
        List<WebAppResource> webAppResources = WebAppUtil.createDefaultResources(policyId);
        WebApp webApp = WebAppUtil.createBasicWebApp(adminUserName, appName + appPrefix, context + appPrefix,
                                                     appVersion, "http://wso2.com/", webAppResources);
        appmPublisherRestClient.createWebApp(webApp);
        String appId = webApp.getAppId();
        appmPublisherRestClient.changeState(appId, AppmTestConstants.LifeCycleStatus.SUBMIT_FOR_REVIEW);
        return appId;
    }

    @DataProvider
    public Object[][] validUserModeDataProvider() throws Exception {
        User adminUser = appMServer.getSuperTenant().getTenantAdmin();
        User appPublisher = appMServer.getSuperTenant().getTenantUser("AppPublisher");
        return new Object[][]{
                new Object[]{adminUser.getUserName(), adminUser.getPassword(), app1Uuid},
                new Object[]{appPublisher.getUserName(), appPublisher.getPassword(), app2Uuid}
        };
    }

    @DataProvider
    public Object[][] inValidUserModeDataProvider() throws Exception {
        User appCreator = appMServer.getSuperTenant().getTenantUser("AppCreator");
        return new Object[][]{
                new Object[]{appCreator.getUserName(), appCreator.getPassword(), app3Uuid}
        };
    }
}
