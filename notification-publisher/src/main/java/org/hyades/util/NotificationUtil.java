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
package org.hyades.util;
import org.hyades.model.Analysis;
import org.hyades.model.Component;
import org.hyades.model.Cwe;
import org.hyades.model.Policy;
import org.hyades.model.PolicyCondition;
import org.hyades.model.PolicyViolation;
import org.hyades.model.Project;
import org.hyades.model.Tag;
import org.hyades.model.Vulnerability;
import org.hyades.notification.vo.AnalysisDecisionChange;
import org.hyades.notification.vo.BomConsumedOrProcessed;
import org.hyades.notification.vo.NewVulnerabilityIdentified;
import org.hyades.notification.vo.NewVulnerableDependency;
import org.hyades.notification.vo.PolicyViolationIdentified;
import org.hyades.notification.vo.VexConsumedOrProcessed;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Map;
import org.hyades.resolver.CweResolver;
import org.hyades.commonutil.*;
public final class NotificationUtil {

    /**
     * Private constructor.
     */
    private NotificationUtil() { }
    public static JsonObject toJson(final Project project) {
        final JsonObjectBuilder projectBuilder = Json.createObjectBuilder();
        projectBuilder.add("uuid", project.getUuid().toString());
        JsonUtil.add(projectBuilder, "name", project.getName());
        JsonUtil.add(projectBuilder, "version", project.getVersion());
        JsonUtil.add(projectBuilder, "description", project.getDescription());
        if (project.getPurl() != null) {
            projectBuilder.add("purl", project.getPurl().canonicalize());
        }
        if (project.getTags() != null && project.getTags().size() > 0) {
            final StringBuilder sb = new StringBuilder();
            for (final Tag tag: project.getTags()) {
                sb.append(tag.getName()).append(",");
            }
            String tags = sb.toString();
            if (tags.endsWith(",")) {
                tags = tags.substring(0, tags.length()-1);
            }
            JsonUtil.add(projectBuilder, "tags", tags);
        }
        return projectBuilder.build();
    }

    public static JsonObject toJson(final Component component) {
        final JsonObjectBuilder componentBuilder = Json.createObjectBuilder();
        componentBuilder.add("uuid", component.getUuid().toString());
        JsonUtil.add(componentBuilder, "group", component.getGroup());
        JsonUtil.add(componentBuilder, "name", component.getName());
        JsonUtil.add(componentBuilder, "version", component.getVersion());
        JsonUtil.add(componentBuilder, "md5", component.getMd5());
        JsonUtil.add(componentBuilder, "sha1", component.getSha1());
        JsonUtil.add(componentBuilder, "sha256", component.getSha256());
        JsonUtil.add(componentBuilder, "sha512", component.getSha512());
        if (component.getPurl() != null) {
            componentBuilder.add("purl", component.getPurl().canonicalize());
        }
        return componentBuilder.build();
    }

    public static JsonObject toJson(final Vulnerability vulnerability) {
        final JsonObjectBuilder vulnerabilityBuilder = Json.createObjectBuilder();
        vulnerabilityBuilder.add("uuid", vulnerability.getUuid().toString());
        JsonUtil.add(vulnerabilityBuilder, "vulnId", vulnerability.getVulnId());
        JsonUtil.add(vulnerabilityBuilder, "source", vulnerability.getSource());
        final JsonArrayBuilder aliasesBuilder = Json.createArrayBuilder();
        if (vulnerability.getAliases() != null) {
            for (final Map.Entry<Vulnerability.Source, String> vulnIdBySource : VulnerabilityUtil.getUniqueAliases(vulnerability)) {
                aliasesBuilder.add(Json.createObjectBuilder()
                        .add("source", vulnIdBySource.getKey().name())
                        .add("vulnId", vulnIdBySource.getValue())
                        .build());
            }
        }
        vulnerabilityBuilder.add("aliases", aliasesBuilder.build());
        JsonUtil.add(vulnerabilityBuilder, "title", vulnerability.getTitle());
        JsonUtil.add(vulnerabilityBuilder, "subtitle", vulnerability.getSubTitle());
        JsonUtil.add(vulnerabilityBuilder, "description", vulnerability.getDescription());
        JsonUtil.add(vulnerabilityBuilder, "recommendation", vulnerability.getRecommendation());
        JsonUtil.add(vulnerabilityBuilder, "cvssv2", vulnerability.getCvssV2BaseScore());
        JsonUtil.add(vulnerabilityBuilder, "cvssv3", vulnerability.getCvssV3BaseScore());
        JsonUtil.add(vulnerabilityBuilder, "severity",  vulnerability.getSeverity());
        final JsonArrayBuilder cwesBuilder = Json.createArrayBuilder();
        if (vulnerability.getCwes() != null) {

            for (final Integer cweId: vulnerability.getCwes()) {
                final Cwe cwe = CweResolver.getInstance().lookup(cweId);
                if (cwe != null) {
                    final JsonObject cweNode = Json.createObjectBuilder()
                            .add("cweId", cwe.getCweId())
                            .add("name", cwe.getName())
                            .build();
                    cwesBuilder.add(cweNode);
                }
            }
        }
        final JsonArray cwes = cwesBuilder.build();
        if (cwes != null && !cwes.isEmpty()) {
            // Ensure backwards-compatibility with DT < 4.5.0. Remove this in v5!
            vulnerabilityBuilder.add("cwe", cwes.getJsonObject(0));
        }
        vulnerabilityBuilder.add("cwes", cwes);
        return vulnerabilityBuilder.build();
    }

    public static JsonObject toJson(final Analysis analysis) {
        final JsonObjectBuilder analysisBuilder = Json.createObjectBuilder();
        analysisBuilder.add("suppressed", analysis.isSuppressed());
        JsonUtil.add(analysisBuilder, "state", analysis.getAnalysisState());
        if (analysis.getProject() != null) {
            JsonUtil.add(analysisBuilder, "project", analysis.getProject().getUuid().toString());
        }
        JsonUtil.add(analysisBuilder, "component", analysis.getComponent().getUuid().toString());
        JsonUtil.add(analysisBuilder, "vulnerability", analysis.getVulnerability().getUuid().toString());
        return analysisBuilder.build();
    }

    public static JsonObject toJson(final NewVulnerabilityIdentified vo) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        if (vo.getComponent() != null) {
            builder.add("component", toJson(vo.getComponent()));
        }
        if (vo.getVulnerabilityAnalysisLevel() != null) {
            builder.add("vulnerabilityAnalysisLevel", vo.getVulnerabilityAnalysisLevel().toString());
        }
        if (vo.getVulnerability() != null) {
            builder.add("vulnerability", toJson(vo.getVulnerability()));
        }
        if (vo.getAffectedProjects() != null && vo.getAffectedProjects().size() > 0) {
            final JsonArrayBuilder projectsBuilder = Json.createArrayBuilder();
            for (final Project project: vo.getAffectedProjects()) {
                projectsBuilder.add(toJson(project));
            }
            builder.add("affectedProjects", projectsBuilder.build());
        }
        return builder.build();
    }

    public static JsonObject toJson(final NewVulnerableDependency vo) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        if (vo.getComponent().getProject() != null) {
            builder.add("project", toJson(vo.getComponent().getProject()));
        }
        if (vo.getComponent() != null) {
            builder.add("component", toJson(vo.getComponent()));
        }
        if (vo.getVulnerabilities() != null && vo.getVulnerabilities().size() > 0) {
            final JsonArrayBuilder vulnsBuilder = Json.createArrayBuilder();
            for (final Vulnerability vulnerability : vo.getVulnerabilities()) {
                vulnsBuilder.add(toJson(vulnerability));
            }
            builder.add("vulnerabilities", vulnsBuilder.build());
        }
        return builder.build();
    }

    public static JsonObject toJson(final AnalysisDecisionChange vo) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        if (vo.getComponent() != null) {
            builder.add("component", toJson(vo.getComponent()));
        }
        if (vo.getVulnerability() != null) {
            builder.add("vulnerability", toJson(vo.getVulnerability()));
        }
        if (vo.getAnalysis() != null) {
            builder.add("analysis", toJson(vo.getAnalysis()));
        }
        if (vo.getProject() != null) {
            // Provide the affected project in the form of an array for backwards-compatibility
            builder.add("affectedProjects", Json.createArrayBuilder().add(toJson(vo.getProject())));
        }
        return builder.build();
    }

    public static JsonObject toJson(final BomConsumedOrProcessed vo) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        if (vo.getProject() != null) {
            builder.add("project", toJson(vo.getProject()));
        }
        if (vo.getBom() != null) {
            builder.add("bom", Json.createObjectBuilder()
                    .add("content", vo.getBom())
                    .add("format", vo.getFormat().getFormatShortName())
                    .add("specVersion", vo.getSpecVersion()).build()
            );
        }
        return builder.build();
    }

    public static JsonObject toJson(final VexConsumedOrProcessed vo) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        if (vo.getProject() != null) {
            builder.add("project", toJson(vo.getProject()));
        }
        if (vo.getVex() != null) {
            builder.add("vex", Json.createObjectBuilder()
                    .add("content", vo.getVex())
                    .add("format", vo.getFormat().getFormatShortName())
                    .add("specVersion", vo.getSpecVersion()).build()
            );
        }
        return builder.build();
    }

    public static JsonObject toJson(final PolicyViolationIdentified vo) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        if (vo.getComponent().getProject() != null) {
            builder.add("project", toJson(vo.getComponent().getProject()));
        }
        if (vo.getComponent() != null) {
            builder.add("component", toJson(vo.getComponent()));
        }
        if (vo.getPolicyViolation() != null) {
            builder.add("policyViolation", toJson(vo.getPolicyViolation()));
        }
        return builder.build();
    }

    public static JsonObject toJson(final PolicyViolation pv) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("uuid", pv.getUuid().toString());
        builder.add("type", pv.getType().name());
        builder.add("timestamp", DateUtil.toISO8601(pv.getTimestamp()));
        builder.add("policyCondition", toJson(pv.getPolicyCondition()));
        return builder.build();
    }

    public static JsonObject toJson(final PolicyCondition pc) {
        final JsonObjectBuilder componentBuilder = Json.createObjectBuilder();
        componentBuilder.add("uuid", pc.getUuid().toString());
        JsonUtil.add(componentBuilder, "subject", pc.getSubject().name());
        JsonUtil.add(componentBuilder, "operator", pc.getOperator().name());
        JsonUtil.add(componentBuilder, "value", pc.getValue());
        componentBuilder.add("policy", toJson(pc.getPolicy()));
        return componentBuilder.build();
    }

    public static JsonObject toJson(final Policy policy) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("uuid", policy.getUuid().toString());
        builder.add("name", policy.getName());
        builder.add("violationState", policy.getViolationState().name());
        return builder.build();
    }

}
