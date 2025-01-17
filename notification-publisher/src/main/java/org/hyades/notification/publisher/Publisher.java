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
package org.hyades.notification.publisher;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import org.hyades.commonnotification.NotificationScope;
import org.hyades.exception.PublisherException;
import org.hyades.model.ConfigProperty;
import org.hyades.model.ConfigPropertyConstants;
import org.hyades.model.Notification;
import org.hyades.notification.vo.AnalysisDecisionChange;
import org.hyades.notification.vo.BomConsumedOrProcessed;
import org.hyades.notification.vo.NewVulnerabilityIdentified;
import org.hyades.notification.vo.NewVulnerableDependency;
import org.hyades.notification.vo.PolicyViolationIdentified;
import org.hyades.notification.vo.VexConsumedOrProcessed;
import org.hyades.persistence.ConfigPropertyRepository;
import org.hyades.util.NotificationUtil;
import org.slf4j.LoggerFactory;

import javax.json.JsonObject;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public interface Publisher {

    String CONFIG_TEMPLATE_KEY = "template";

    String CONFIG_TEMPLATE_MIME_TYPE_KEY = "mimeType";

    String CONFIG_DESTINATION = "destination";


    void inform(Notification notification, JsonObject config);

    PebbleEngine getTemplateEngine();

    default PebbleTemplate getTemplate(JsonObject config) {
        try {
            String literalTemplate = config.getString(CONFIG_TEMPLATE_KEY);
            return getTemplateEngine().getLiteralTemplate(literalTemplate);
        } catch (NullPointerException | ClassCastException templateException) {
            throw new PublisherException(templateException.getMessage(), templateException);
        }
    }

    default String getTemplateMimeType(JsonObject config) {
        try {
            return config.getString(CONFIG_TEMPLATE_MIME_TYPE_KEY);
        } catch (NullPointerException | ClassCastException templateException) {
            throw new PublisherException(templateException.getMessage(), templateException);
        }
    }

    default String prepareTemplate(final Notification notification, final PebbleTemplate template, final ConfigPropertyRepository configPropertyRepository) {

            final ConfigProperty baseUrlProperty = configPropertyRepository.findByGroupAndName(
                    ConfigPropertyConstants.GENERAL_BASE_URL.getGroupName(),
                    ConfigPropertyConstants.GENERAL_BASE_URL.getPropertyName()
            );

            final Map<String, Object> context = new HashMap<>();
            final long epochSecond = notification.getTimestamp().toEpochSecond(
                    ZoneId.systemDefault().getRules()
                            .getOffset(notification.getTimestamp())
            );
            context.put("timestampEpochSecond", epochSecond);
            context.put("timestamp", notification.getTimestamp().toString());
            context.put("notification", notification);
            if (baseUrlProperty != null) {
                context.put("baseUrl", baseUrlProperty.getPropertyValue().replaceAll("/$", ""));
            } else {
                context.put("baseUrl", "");
            }

            if (NotificationScope.PORTFOLIO.name().equals(notification.getScope())) {
                if (notification.getSubject() instanceof NewVulnerabilityIdentified) {
                    final NewVulnerabilityIdentified subject = (NewVulnerabilityIdentified) notification.getSubject();
                    context.put("subject", subject);
                    context.put("subjectJson", NotificationUtil.toJson(subject));
                } else if (notification.getSubject() instanceof NewVulnerableDependency) {
                    final NewVulnerableDependency subject = (NewVulnerableDependency) notification.getSubject();
                    context.put("subject", subject);
                    context.put("subjectJson", NotificationUtil.toJson(subject));
                } else if (notification.getSubject() instanceof AnalysisDecisionChange) {
                    final AnalysisDecisionChange subject = (AnalysisDecisionChange) notification.getSubject();
                    context.put("subject", subject);
                    context.put("subjectJson", NotificationUtil.toJson(subject));
                } else if (notification.getSubject() instanceof BomConsumedOrProcessed) {
                    final BomConsumedOrProcessed subject = (BomConsumedOrProcessed) notification.getSubject();
                    context.put("subject", subject);
                    context.put("subjectJson", NotificationUtil.toJson(subject));
                } else if (notification.getSubject() instanceof VexConsumedOrProcessed) {
                    final VexConsumedOrProcessed subject = (VexConsumedOrProcessed) notification.getSubject();
                    context.put("subject", subject);
                    context.put("subjectJson", NotificationUtil.toJson(subject));
                } else if (notification.getSubject() instanceof PolicyViolationIdentified) {
                    final PolicyViolationIdentified subject = (PolicyViolationIdentified) notification.getSubject();
                    context.put("subject", subject);
                    context.put("subjectJson", NotificationUtil.toJson(subject));
                }
            }

            try (Writer writer = new StringWriter()) {
                template.evaluate(writer, context);
                return writer.toString();
            } catch (IOException e) {
                LoggerFactory.getLogger(this.getClass()).error("An error was encountered evaluating template", e);
                return null;
            }
        }
    //}

}
