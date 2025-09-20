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

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;

import static com.metaformsystems.fleet.Constants.GRADLE_TASK_GROUP;
import static com.metaformsystems.fleet.Constants.LAYER_DIGEST_PROPERTY;
import static com.metaformsystems.fleet.Constants.LAYER_SIZE_PROPERTY;
import static com.metaformsystems.fleet.Constants.SHA_PREFIX;
import static com.metaformsystems.fleet.Constants.XREGISTRY_LAYER_ARCHIVE_PATH;
import static com.metaformsystems.fleet.Constants.XREGISTRY_LAYER_SHA_PATH;
import static com.metaformsystems.fleet.ShaUtils.generateSha256;
import static java.nio.file.Files.write;

/**
 * Generates the SHA-256 digest for the xRegistry layer archive.
 */
public class GenerateLayerDigestAction implements Action<Task> {
    public static final String TASK_NAME = "generateLayerDigest";

    private static final String DESCRIPTION = "Generates SHA-256 digest for xRegistry layer";

    private Project project;

    public GenerateLayerDigestAction(Project project) {
        this.project = project;
    }

    @Override
    public void execute(Task task) {
        task.setDescription(DESCRIPTION);
        task.setGroup(GRADLE_TASK_GROUP);

        var layerFile = project.getLayout().getBuildDirectory().file(XREGISTRY_LAYER_ARCHIVE_PATH);
        var shaFile = project.getLayout().getBuildDirectory().file(XREGISTRY_LAYER_SHA_PATH);

        task.getInputs().file(layerFile);
        task.getOutputs().file(shaFile);

        task.doLast(t -> {
            try {
                var layerArchive = layerFile.get().getAsFile();
                if (!layerArchive.exists()) {
                    throw new GradleException("Layer archive not found: " + layerArchive.getAbsolutePath());
                }

                var digest = generateSha256(layerArchive);
                var digestWithPrefix = SHA_PREFIX + digest;

                // write the SHA file
                shaFile.get().getAsFile().getParentFile().mkdirs();
                write(shaFile.get().getAsFile().toPath(), digest.getBytes());

                // set project properties required by subsequent tasks
                project.getExtensions().getExtraProperties().set(LAYER_DIGEST_PROPERTY, digestWithPrefix);
                project.getExtensions().getExtraProperties().set(LAYER_SIZE_PROPERTY, layerArchive.length());

            } catch (Exception e) {
                throw new GradleException("Failed to generate layer digest", e);
            }
        });
    }
}