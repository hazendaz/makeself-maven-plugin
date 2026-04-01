/*
 *    Copyright 2011-2026 the original author or authors.
 *
 *    This program is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU General Public License
 *    as published by the Free Software Foundation; either version 2
 *    of the License, or (at your option) any later version.
 *
 *    You may obtain a copy of the License at
 *
 *       https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 */
package com.hazendaz.maven.makeself;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * The Class GitMojo.
 * <p>
 * Downloads and installs portable Git on Windows from the Maven repository. This goal can be used standalone in any
 * Maven project (or without a project) to obtain a secure, vetted copy of portable Git without needing to use a browser
 * or locate the artifact in the local Maven repository manually.
 * </p>
 * <p>
 * On non-Windows systems this goal is a no-op since makeself and bash are natively available.
 * </p>
 */
@Mojo(name = "git", defaultPhase = LifecyclePhase.NONE, requiresProject = false)
public class GitMojo extends AbstractGitMojo {

    /** Skip run of plugin. */
    @Parameter(defaultValue = "false", property = "makeself.skip")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // Ensure gitPath is never null
        if (this.gitPath == null) {
            this.gitPath = "";
        }

        // Check if plugin run should be skipped
        if (this.skip) {
            this.getLog().info("Makeself git is skipped");
            return;
        }

        if (!AbstractGitMojo.WINDOWS) {
            this.getLog().info("Portable git is only applicable on Windows; skipping on this platform");
            return;
        }

        // Use existing git if a valid path was provided
        if (!this.gitPath.isEmpty() && Files.exists(Path.of(this.gitPath))) {
            this.getLog().info("Using existing 'Git' found at " + this.gitPath);
            this.gitPath = this.gitPath + AbstractGitMojo.GIT_USER_BIN;
        } else {
            // Download and install portable git
            this.checkGitSetup();
        }

        this.getLog().info("Portable git is available at: " + this.gitPath);
    }

}
