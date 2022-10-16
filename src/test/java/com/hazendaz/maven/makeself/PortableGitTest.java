/*
 *    Copyright 2011-2022 the original author or authors.
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

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The Class PortableGitTest.
 */
public class PortableGitTest {

    /**
     * Process git test.
     *
     * @throws MojoFailureException
     *             the mojo failure exception
     */
    @Test
    void processGitTest() throws MojoFailureException {
        final PortableGit portableGit = new PortableGit(new SystemStreamLog());
        Assertions.assertEquals("git-for-windows", portableGit.getArtifactId());
        Assertions.assertEquals("portable", portableGit.getClassifier());
        Assertions.assertEquals("com.github.hazendaz.git", portableGit.getGroupId());
        Assertions.assertEquals("PortableGit", portableGit.getName());
        Assertions.assertEquals("tar.gz", portableGit.getType());
        Assertions.assertNotEquals("${git.version}", portableGit.getVersion());
    }

}
