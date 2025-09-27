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
import org.gradle.api.Project;
import org.gradle.api.tasks.bundling.Compression;
import org.gradle.api.tasks.bundling.Tar;

import static com.metaformsystems.fleet.xregistry.oci.Constants.GRADLE_TASK_GROUP;
import static com.metaformsystems.fleet.xregistry.oci.Constants.OCI_LAYERS_DIR;
import static com.metaformsystems.fleet.xregistry.oci.Constants.XREGISTRY_LAYER_ARCHIVE;
import static com.metaformsystems.fleet.xregistry.oci.Constants.XREGISTRY_STAGING_DIR;

/**
 * Copies files into the xRegistry layer archive.
 */
public class CreateRegistryLayerAction implements Action<Tar> {
    public static final String TASK_NAME = "createXRegistryLayer";

    private static final String DESCRIPTION = "Creates xRegistry layer tar archive";
    private static final String XREGISTRY_DEST = "xregistry";

    private Project project;

    public CreateRegistryLayerAction(Project project) {
        this.project = project;
    }

    @Override
    public void execute(Tar task) {
        task.setDescription(DESCRIPTION);
        task.setGroup(GRADLE_TASK_GROUP);

        task.from(project.getLayout().getBuildDirectory().dir(XREGISTRY_STAGING_DIR));
        task.into(XREGISTRY_DEST);
        task.getArchiveFileName().set(XREGISTRY_LAYER_ARCHIVE);
        task.getDestinationDirectory().set(project.getLayout().getBuildDirectory().dir(OCI_LAYERS_DIR));
        task.setCompression(Compression.NONE);
    }
}
