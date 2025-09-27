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

package com.metaformsystems.fleet.xregistry.oci;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Copy;

import java.io.File;

import static com.metaformsystems.fleet.xregistry.oci.Constants.ARTIFACT_EXTENSIONS;
import static com.metaformsystems.fleet.xregistry.oci.Constants.GRADLE_TASK_GROUP;
import static com.metaformsystems.fleet.xregistry.oci.Constants.XREGISTRY_STAGING_DIR;

/**
 * Prepares the xRegistry build layout.
 */
public class PrepareAction implements Action<Copy> {
    public static final String TASK_NAME = "prepareXRegistryFiles";

    private static final String DESCRIPTION = "Copies xRegistry files to a staging directory";

    private Project project;
    private Provider<String> sourceLocationProvider;

    public PrepareAction(Project project, Provider<String> sourceLocationProvider) {
        this.project = project;
        this.sourceLocationProvider = sourceLocationProvider;
    }

    @Override
    public void execute(Copy task) {
        task.setDescription(DESCRIPTION);
        task.setGroup(GRADLE_TASK_GROUP);

        task.from(sourceLocationProvider);
        task.into(project.getLayout().getBuildDirectory().dir(XREGISTRY_STAGING_DIR));
        task.include(ARTIFACT_EXTENSIONS);

        task.doFirst(t -> {
            var sourceDir = new File(sourceLocationProvider.get());
            if (!sourceDir.exists()) {
                throw new GradleException("xRegistry source directory does not exist: " + sourceDir.getAbsolutePath());
            }

            var registryFiles = project.fileTree(sourceDir, spec -> spec.include(ARTIFACT_EXTENSIONS));

            if (registryFiles.isEmpty()) {
                throw new GradleException("No xRegistry files found in directory: " + sourceDir.getAbsolutePath());
            }
        });
    }
}