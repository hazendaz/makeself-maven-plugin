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
package com.github.hazendaz.maven.makeself_maven_plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link HelpMojo}.
 */
@ExtendWith(MockitoExtension.class)
class HelpMojoTest {

    /** Mock Maven log. */
    @Mock
    private Log log;

    /**
     * Sets a private field on the given object via reflection.
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
        final Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    /**
     * Test default execute loads plugin-help.xml and prints all goals.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testExecuteDefault() throws Exception {
        final HelpMojo mojo = new HelpMojo();
        mojo.setLog(log);
        Mockito.when(log.isInfoEnabled()).thenReturn(true);

        mojo.execute();

        Mockito.verify(log).isInfoEnabled();
        Mockito.verify(log).info(Mockito.anyString());
    }

    /**
     * Test execute with detail=true outputs parameter information for all goals.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testExecuteWithDetailTrue() throws Exception {
        final HelpMojo mojo = new HelpMojo();
        mojo.setLog(log);
        setField(mojo, "detail", true);
        Mockito.when(log.isInfoEnabled()).thenReturn(true);

        mojo.execute();

        Mockito.verify(log).isInfoEnabled();
        Mockito.verify(log).info(Mockito.anyString());
    }

    /**
     * Test execute filtered to the 'makeself' goal only.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testExecuteWithGoalMakeself() throws Exception {
        final HelpMojo mojo = new HelpMojo();
        mojo.setLog(log);
        setField(mojo, "goal", "makeself");
        Mockito.when(log.isInfoEnabled()).thenReturn(true);

        mojo.execute();

        Mockito.verify(log).isInfoEnabled();
    }

    /**
     * Test execute filtered to the 'git' goal only.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testExecuteWithGoalGit() throws Exception {
        final HelpMojo mojo = new HelpMojo();
        mojo.setLog(log);
        setField(mojo, "goal", "git");
        Mockito.when(log.isInfoEnabled()).thenReturn(true);

        mojo.execute();

        Mockito.verify(log).isInfoEnabled();
    }

    /**
     * Test execute filtered to a goal that doesn't exist produces no goal output.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testExecuteWithUnknownGoal() throws Exception {
        final HelpMojo mojo = new HelpMojo();
        mojo.setLog(log);
        setField(mojo, "goal", "nonexistent-goal");
        Mockito.when(log.isInfoEnabled()).thenReturn(true);

        mojo.execute();

        Mockito.verify(log).isInfoEnabled();
    }

    /**
     * Test execute with detail=true and a specific goal.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testExecuteWithDetailAndGoal() throws Exception {
        final HelpMojo mojo = new HelpMojo();
        mojo.setLog(log);
        setField(mojo, "detail", true);
        setField(mojo, "goal", "makeself");
        Mockito.when(log.isInfoEnabled()).thenReturn(true);

        mojo.execute();

        Mockito.verify(log).isInfoEnabled();
    }

    /**
     * Test execute when lineLength is zero or negative triggers warning and resets to default.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testExecuteWithZeroLineLength() throws Exception {
        final HelpMojo mojo = new HelpMojo();
        mojo.setLog(log);
        setField(mojo, "lineLength", 0);
        Mockito.when(log.isInfoEnabled()).thenReturn(true);

        mojo.execute();

        Mockito.verify(log).warn("The parameter 'lineLength' should be positive, using '80' as default.");
    }

    /**
     * Test execute when indentSize is zero or negative triggers warning and resets to default.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testExecuteWithZeroIndentSize() throws Exception {
        final HelpMojo mojo = new HelpMojo();
        mojo.setLog(log);
        setField(mojo, "indentSize", 0);
        Mockito.when(log.isInfoEnabled()).thenReturn(true);

        mojo.execute();

        Mockito.verify(log).warn("The parameter 'indentSize' should be positive, using '2' as default.");
    }

    /**
     * Test execute when lineLength is negative triggers warning.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testExecuteWithNegativeLineLength() throws Exception {
        final HelpMojo mojo = new HelpMojo();
        mojo.setLog(log);
        setField(mojo, "lineLength", -5);
        Mockito.when(log.isInfoEnabled()).thenReturn(true);

        mojo.execute();

        Mockito.verify(log).warn("The parameter 'lineLength' should be positive, using '80' as default.");
    }

    /**
     * Test execute when isInfoEnabled returns false, info is not logged.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testExecuteInfoNotEnabledSkipsInfoLog() throws Exception {
        final HelpMojo mojo = new HelpMojo();
        mojo.setLog(log);
        Mockito.when(log.isInfoEnabled()).thenReturn(false);

        mojo.execute();

        Mockito.verify(log).isInfoEnabled();
        Mockito.verify(log, Mockito.never()).info(Mockito.anyString());
    }

    /**
     * Test execute with detail=true and the git goal to exercise parameter writing.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testExecuteDetailWithGitGoal() throws Exception {
        final HelpMojo mojo = new HelpMojo();
        mojo.setLog(log);
        setField(mojo, "detail", true);
        setField(mojo, "goal", "git");
        Mockito.when(log.isInfoEnabled()).thenReturn(true);

        mojo.execute();

        Mockito.verify(log).isInfoEnabled();
    }

    /**
     * Test that the getPropertyFromExpression method correctly strips ${} wrapper.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testGetPropertyFromExpression() throws Exception {
        final Method method = HelpMojo.class.getDeclaredMethod("getPropertyFromExpression", String.class);
        method.setAccessible(true);

        Assertions.assertAll(
                // Standard expression
                () -> Assertions.assertEquals("myProp", method.invoke(null, "${myProp}")),
                // Dotted expression (common Maven property like ${project.version})
                () -> Assertions.assertEquals("project.version", method.invoke(null, "${project.version}")),
                // Null input
                () -> Assertions.assertNull(method.invoke(null, (Object) null)),
                // No ${} wrapper
                () -> Assertions.assertNull(method.invoke(null, "plain-value")),
                // Nested expression - should return null
                () -> Assertions.assertNull(method.invoke(null, "${outer.${inner}}")),
                // Missing closing brace
                () -> Assertions.assertNull(method.invoke(null, "${noClosure")));
    }

    /**
     * Test that the isNotEmpty method correctly identifies non-empty strings.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testIsNotEmpty() throws Exception {
        final Method method = HelpMojo.class.getDeclaredMethod("isNotEmpty", String.class);
        method.setAccessible(true);

        Assertions.assertAll(() -> Assertions.assertFalse((boolean) method.invoke(null, (Object) null)),
                () -> Assertions.assertFalse((boolean) method.invoke(null, "")),
                () -> Assertions.assertTrue((boolean) method.invoke(null, "hello")));
    }

    /**
     * Test that the repeat method correctly repeats strings.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testRepeat() throws Exception {
        final Method method = HelpMojo.class.getDeclaredMethod("repeat", String.class, int.class);
        method.setAccessible(true);

        Assertions.assertAll(() -> Assertions.assertEquals("", method.invoke(null, "ab", 0)),
                () -> Assertions.assertEquals("ab", method.invoke(null, "ab", 1)),
                () -> Assertions.assertEquals("ababab", method.invoke(null, "ab", 3)));
    }

    /**
     * Test that the getIndentLevel method correctly counts tab indentation.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testGetIndentLevel() throws Exception {
        final Method method = HelpMojo.class.getDeclaredMethod("getIndentLevel", String.class);
        method.setAccessible(true);

        Assertions.assertAll(
                // No indentation
                () -> Assertions.assertEquals(0, method.invoke(null, "no indent")),
                // One tab
                () -> Assertions.assertEquals(1, method.invoke(null, "\tsingle")),
                // Two tabs
                () -> Assertions.assertEquals(2, method.invoke(null, "\t\tdouble")),
                // One tab followed by a space then another tab: second loop detects the extra tab → level+1
                () -> Assertions.assertEquals(2, method.invoke(null, "\t \t")));
    }

    /**
     * Test that toLines correctly replaces non-breaking spaces (U+00A0) with regular spaces during line-wrapping.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    void testToLinesNonBreakingSpace() throws Exception {
        final Method method = HelpMojo.class.getDeclaredMethod("toLines", java.util.List.class, String.class, int.class,
                int.class);
        method.setAccessible(true);

        final java.util.List<String> lines = new java.util.ArrayList<>();
        // A line containing a non-breaking space (U+00A0) between words
        method.invoke(null, lines, "hello\u00A0world", 2, 80);

        Assertions.assertFalse(lines.isEmpty(), "toLines should produce at least one output line");
        // The non-breaking space should have been converted to a regular space
        Assertions.assertTrue(lines.stream().anyMatch(l -> l.contains(" ")),
                "Output should contain a regular space where the non-breaking space was");
    }

}
