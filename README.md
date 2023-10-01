Makeself Maven Plugin
=====================

[![Java CI](https://github.com/hazendaz/makeself-maven-plugin/workflows/Java%20CI/badge.svg)](https://github.com/hazendaz/makeself-maven-plugin/actions?query=workflow%3A%22Java+CI%22)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.github.hazendaz.maven/makeself-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.hazendaz.maven/makeself-maven-plugin)
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
                <version>1.6.0</version>
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

## Makeself Update ##

Run 'mvn clean -Pupdate-makeself' and review results / commit / code as needed.
