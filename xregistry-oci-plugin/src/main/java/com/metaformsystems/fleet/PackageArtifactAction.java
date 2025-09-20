
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
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.bundling.Tar;

import static com.metaformsystems.fleet.Constants.GRADLE_TASK_GROUP;
import static com.metaformsystems.fleet.Constants.OCI_LAYOUT_DIR;
import static org.gradle.api.tasks.bundling.Compression.GZIP;

/**
 * Packages the OCI layout as a tar archive for distribution.
 */
public class PackageArtifactAction implements Action<Tar> {
    public static final String TASK_NAME = "packageOciArtifact";

    private Project project;
    private Provider<String> artifactNameProvider;
    private Provider<String> versionProvider;

    public PackageArtifactAction(Project project,
                                 Provider<String> artifactNameProvider,
                                 Provider<String> versionProvider) {
        this.project = project;
        this.artifactNameProvider = artifactNameProvider;
        this.versionProvider = versionProvider;
    }

    @Override
    public void execute(Tar task) {
        task.setDescription("Packages OCI layout as tar archive for distribution");
        task.setGroup(GRADLE_TASK_GROUP);

        task.from(project.getLayout().getBuildDirectory().dir(OCI_LAYOUT_DIR));

        // use lazy configuration with providers
        task.getArchiveFileName().set(artifactNameProvider.zip(versionProvider,
                (name, version) -> name + "-" + version + ".tar"));
        task.getDestinationDirectory().set(project.getLayout().getBuildDirectory().dir("distributions"));
        task.setCompression(GZIP);
    }
}