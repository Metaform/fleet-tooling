/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package com.metaformsystems.fleet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.metaformsystems.fleet.Constants.CONFIG_DIGEST_PROPERTY;
import static com.metaformsystems.fleet.Constants.CONFIG_SIZE_PROPERTY;
import static com.metaformsystems.fleet.Constants.GRADLE_TASK_GROUP;
import static com.metaformsystems.fleet.Constants.LAYER_DIGEST_PROPERTY;
import static com.metaformsystems.fleet.Constants.OCI_CONFIG_PATH;
import static com.metaformsystems.fleet.Constants.SHA_PREFIX;
import static com.metaformsystems.fleet.ShaUtils.generateSha256;
import static java.nio.file.Files.write;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

/**
 * Creates the OCI image configuration file.
 */
public class CreateConfigAction implements Action<Task> {
    public static final String TASK_NAME = "createOciConfig";

    private static final String DESCRIPTION = "Creates OCI image config";
    private Project project;
    private ObjectMapper mapper;

    public CreateConfigAction(Project project, ObjectMapper mapper) {
        this.project = project;
        this.mapper = mapper;
    }

    @Override
    public void execute(Task task) {
        task.setDescription(DESCRIPTION);
        task.setGroup(GRADLE_TASK_GROUP);

        var configFile = project.getLayout().getBuildDirectory().file(OCI_CONFIG_PATH);
        task.getOutputs().file(configFile);

        task.doLast(t -> {
            configFile.get().getAsFile().getParentFile().mkdirs();

            var layerDigest = (String) project.getExtensions().getExtraProperties().get(LAYER_DIGEST_PROPERTY);
            var config = createOciConfig(layerDigest);

            try {
                write(configFile.get().getAsFile().toPath(), mapper.writeValueAsBytes(config));
            } catch (Exception e) {
                throw new GradleException("Failed to write config file", e);
            }

            var digest = generateSha256(configFile.get().getAsFile());
            project.getExtensions().getExtraProperties().set(CONFIG_DIGEST_PROPERTY, SHA_PREFIX + digest);
            project.getExtensions().getExtraProperties().set(CONFIG_SIZE_PROPERTY, configFile.get().getAsFile().length());
        });
    }

    /**
     * Creates OCI config structure
     */
    public Map<String, Object> createOciConfig(String layerDigest) {
        var config = new LinkedHashMap<String, Object>();
        config.put("architecture", "amd64");
        config.put("os", "linux");
        config.put("config", emptyMap());

        var rootfs = new LinkedHashMap<>();
        rootfs.put("type", "layers");
        rootfs.put("diff_ids", singletonList(layerDigest));
        config.put("rootfs", rootfs);

        var history = new LinkedHashMap<>();
        history.put("created", Instant.now().toString());
        history.put("created_by", "gradle-xregistry-oci-packager");
        history.put("comment", "xRegistry policy layer");
        config.put("history", singletonList(history));

        return config;
    }

}
