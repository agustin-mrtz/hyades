/*
 * This file is part of Dependency-Track.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) Steve Springett. All Rights Reserved.
 */
package org.hyades.notification;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.hyades.commonnotification.NotificationGroup;
import org.hyades.commonnotification.NotificationScope;
import org.hyades.model.Notification;
import org.hyades.model.NotificationLevel;
import org.hyades.notification.publisher.DefaultNotificationPublishers;
import org.hyades.notification.publisher.MsTeamsPublisher;
import org.hyades.notification.publisher.Publisher;
import org.hyades.persistence.ConfigPropertyRepository;
import org.hyades.util.NotificationUtil;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.EntityManager;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@QuarkusTest
public class MsTeamsPublisherTest{

    @Inject
    MsTeamsPublisher publisher;

    private static ClientAndServer mockServer;

    @Inject
    ConfigPropertyRepository configPropertyRepository;

    @Inject
    EntityManager entityManager;

    @BeforeAll
    public static void beforeClass() {
        mockServer = startClientAndServer(1060);
    }

    @AfterAll
    public static void afterClass() {
        mockServer.stop();
    }

    @Test
    @TestTransaction
    public void testPublish() throws IOException {
        new MockServerClient("localhost", 1060)
                .when(
                        request()
                                .withMethod("POST")
                                .withPath("/mychannel")
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                );
        entityManager.createNativeQuery("""
                INSERT INTO "CONFIGPROPERTY" ("ID", "DESCRIPTION", "GROUPNAME", "PROPERTYTYPE", "PROPERTYNAME", "PROPERTYVALUE") VALUES
                                    (1, 'msteams', 'general', 'STRING', 'base.url', 'http://localhost:1060/mychannel');
                """).executeUpdate();

        JsonObject config = getConfig(DefaultNotificationPublishers.MS_TEAMS, "http://localhost:1060/mychannel");
        Notification notification = new Notification();
        notification.setScope(NotificationScope.PORTFOLIO.name());
        notification.setGroup(NotificationGroup.NEW_VULNERABILITY.name());
        notification.setLevel(NotificationLevel.INFORMATIONAL);
        notification.setTitle("Test Notification");
        notification.setContent("This is only a test");
        publisher.inform(notification, config);
    }

    JsonObject getConfig(DefaultNotificationPublishers publisher, String destination) throws IOException {
        File templateFile = new File(URLDecoder.decode(NotificationUtil.class.getResource(publisher.getPublisherTemplateFile()).getFile(), UTF_8.name()));
        String templateContent = FileUtils.readFileToString(templateFile, UTF_8);
        return Json.createObjectBuilder()
                .add(Publisher.CONFIG_TEMPLATE_MIME_TYPE_KEY, publisher.getTemplateMimeType())
                .add(Publisher.CONFIG_TEMPLATE_KEY, templateContent)
                .add(Publisher.CONFIG_DESTINATION, destination)
                .addAll(getExtraConfig())
                .build();
    }

    JsonObjectBuilder getExtraConfig() {
        return Json.createObjectBuilder();
    }
}
