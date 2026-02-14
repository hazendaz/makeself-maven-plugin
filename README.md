Makeself Maven Plugin
=====================

[![Java CI](https://github.com/hazendaz/makeself-maven-plugin/actions/workflows/ci.yaml/badge.svg)](https://github.com/hazendaz/makeself-maven-plugin/actions/workflows/ci.yaml)
[![Coverage Status](https://coveralls.io/repos/github/hazendaz/makeself-maven-plugin/badge.svg?branch=master)](https://coveralls.io/github/hazendaz/makeself-maven-plugin?branch=master)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.hazendaz.maven/makeself-maven-plugin.svg)](https://central.sonatype.com/artifact/com.github.hazendaz.maven/makeself-maven-plugin)
[![License](http://img.shields.io/:license-glp-blue.svg)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)

![hazendaz](src/site/resources/images/hazendaz-banner.jpg)

See [makeself](https://github.com/megastep/makeself) for details on using makeself

See plugin site page [here](https://hazendaz.github.io/makeself-maven-plugin/)

See plugin details [here](https://hazendaz.github.io/makeself-maven-plugin/plugin-info.html)

See changelog [here](https://hazendaz.github.io/makeself-maven-plugin/CHANGELOG.md)

Makeself Maven Plugin provides maven integration for megastep makeself script.

Makeself is a self-extracting archiving tool for Unix systems, in 100% shell script.

With help of Cygwin, git for windows or other tools supplying bash for windows, this tool is fully functional with windows.

## Windows ##

Usage in windows leverages Portable Git distribution automatically during the build.

Versions prior to this point as noted below used bash in a different way.  It is best to use the latest where possible.

[12/30/2020] Original windows logic to confirm bash is currently disabled as it detects incorrectly on recent windows 10.  Instead for windows, it will always use the portable git distribution.

To use in windows, configure Bash or Add git for windows '/usr/bin' to environment 'Path' variable to execute this plugin.

Alternatively in windows, if bash is not found on the path, the plugin will download the portable copy of git-for-windows and install it into maven home under 'PortableGit' and will use that version to run necessary bash steps.

Example Usage

```xml
    <plugin>
        <groupId>com.github.hazendaz.maven</groupId>
        <artifactId>makeself-maven-plugin</artifactId>
        <version>1.9.2</version>
        <configuration>
            <archiveDir>distro</archiveDir>
            <fileName>installDistro.sh</fileName>
            <label>Distro Self Extraction</label>
            <startupScript>./runDistroScript.sh</startupScript>
        </configuration>
        <executions>
            <execution>
                <id>makeself</id>
                <goals>
                    <goal>makeself</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
```

*** Special notes: ***

- See release notes for more details
- Version 1.9.2 now indicates what maven plugin version used within scripts to easily link back to the code used to produce it.
- Version 1.8.1 full maven 4 compatibility
- Version 1.8.0 requires java 11
- Version 1.7.0 requires at least maven 3.6.3
- Version 1.6.0 requires at least maven 3.5.0
- Use version 1.5.1 with 'inlineScript' set to true if using startupScript to call a program with scriptArgs.
- Use version 1.3.0 or better for full windows support without having to pre-install bash.
- For jdk 8 and on usage, use version 1.2.0
- For jdk 7 usage, use version 1.1.5
- Use version 1.1.4 to avoid dead lock when buffer is overrun.
- Use version 1.0.0.beta3 or better for *nix support.  Earlier versions only worked in windows.

## Contributor Consideration ##

Executable Permissions on Shell Scripts

When makeself is updated on this repository, perform the following commands after the update commit.  This will ensure shell files are executable within project.
While it might not provide any benefit upon build, it doesn't hurt.

```git
    git add --chmod=+x *.sh; \
    git commit -m "Force makeself to be executable"
```

To check the files are of right permissions

```stat
    stat -c "%a %n" *
```

*** Note: *** It seems no matter the change makself-header.sh wants to stay 644.  Regardless, with all other protections this is handled on *nix now and line endings are addressed.

## Integration Tests ##

mvn -Prun-it clean install

Current tests that exist:

- ```existing-git``` where 'gitPath' is checked instead of downloading a copy of portable git inside the plugin for cases where users have existing git on windows and cannot run 'EXE' installer.  In this case, its prepared first by downloading it via the pom for use but in real usage would expect existing copy to exist so it can find /usr/bin.
- ```inline``` where it runs an inline script.
- ```missing-archive``` to demonstrate a missing archive
- ```missing-script``` to demonstrate a missing script
- ```sample``` to demonstrate a basic use-case

## Makeself Update ##

Run 'mvn clean -Pupdate-makeself' and review results / commit / code as needed.

## Release Checklist ##

- Run makeself update
- Provision any new support
- Make sure portable 'git' is current
- In makeself.sh, set MS_PLUGIN_VERSION to the to be released version
