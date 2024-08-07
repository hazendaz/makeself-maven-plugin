/*
 *    Copyright 2011-2024 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import lombok.Getter;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

/**
 * The Class PortableGit.
 */
public class PortableGit {

    /** The group id. */
    @Getter
    private String groupId;

    /** The artifact id. */
    @Getter
    private String artifactId;

    /** The version. */
    @Getter
    private String version;

    /** The extension. */
    @Getter
    private String extension;

    /** The classifier. */
    @Getter
    private String classifier;

    /** The name. */
    @Getter
    private String name;

    /**
     * Load portable git artifact from makeself.properties file.
     *
     * @param log
     *            the log
     *
     * @throws MojoFailureException
     *             the mojo failure exception
     */
    public PortableGit(final Log log) throws MojoFailureException {
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("META-INF/makeself.properties")) {
            final Properties properties = new Properties();
            properties.load(input);

            this.groupId = properties.getProperty("portable.git.groupId");
            this.artifactId = properties.getProperty("portable.git.artifactId");
            this.version = properties.getProperty("portable.git.version");
            this.extension = properties.getProperty("portable.git.extension");
            this.classifier = properties.getProperty("portable.git.classifier");
            this.name = properties.getProperty("portable.git.name");
        } catch (final IOException e) {
            log.error("Unable to read makeself.properties");
            throw new MojoFailureException(e);
        }
    }

}
