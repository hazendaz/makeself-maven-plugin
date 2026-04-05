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

import java.lang.reflect.Field;

import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The Class GitMojoTest.
 */
@ExtendWith(MockitoExtension.class)
class GitMojoTest {

    /** Mock Maven log. */
    @Mock
    private Log log;

    /**
     * Sets a private field on the given object via reflection, walking up the class hierarchy as needed.
     *
     * @param obj
     *            the object
     * @param fieldName
     *            the field name
     * @param value
     *            the value to set
     *
     * @throws Exception
     *             if the field cannot be found or set
     */
    private static void setField(final Object obj, final String fieldName, final Object value) throws Exception {
        final Field field = findField(obj.getClass(), fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    /**
     * Finds a declared field by name, walking the class hierarchy.
     *
     * @param clazz
     *            the class to start from
     * @param name
     *            the field name
     *
     * @return the field
     *
     * @throws NoSuchFieldException
     *             if no field with the given name is found
     */
    private static Field findField(final Class<?> clazz, final String name) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(name);
        } catch (final NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return findField(clazz.getSuperclass(), name);
            }
            throw e;
        }
    }

    /**
     * Test skip execution logs message and returns without error.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testSkipExecution() throws Exception {
        final GitMojo mojo = new GitMojo();
        mojo.setLog(log);
        setField(mojo, "skip", true);

        mojo.execute();

        Mockito.verify(log).info("Makeself git is skipped");
    }

    /**
     * Test execute on non-Windows platform logs the skipping message and returns without error.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testNonWindowsExecution() throws Exception {
        Assumptions.assumeFalse(AbstractGitMojo.WINDOWS, "Test only applicable on non-Windows platforms");

        final GitMojo mojo = new GitMojo();
        mojo.setLog(log);

        mojo.execute();

        Mockito.verify(log).info("Portable git is only applicable on Windows; skipping on this platform");
    }

}
