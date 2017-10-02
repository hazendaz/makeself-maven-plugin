Makeself Maven Plugin
=====================

[![Build Status](https://travis-ci.org/hazendaz/makeself-maven-plugin.svg?branch=master)](https://travis-ci.org/hazendaz/makeself-maven-plugin)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.github.hazendaz.maven/makeself-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.hazendaz.maven/makeself-maven-plugin)
[![License](http://img.shields.io/:license-glp-blue.svg)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)

![hazendaz](src/site/resources/images/hazendaz-banner.jpg)

Makeself Maven Plugin provides maven integration for megastep makeself script.

Makeself is a self-extracting archiving tool for Unix systems, in 100% shell script.

See [makeself](https://github.com/megastep/makeself)

Example Usage

```xml
            <plugin>
                <groupId>com.github.hazendaz.maven</groupId>
                <artifactId>makeself-maven-plugin</artifactId>
                <version>1.0.0-SNAPSHOT</version>
                <configuration>
                    <archiveDir>distro</archiveDir>
                    <fileName>installDistro.sh</fileName>
                    <label>Distro Self Extraction</label>
                    <startupScript>runDistroScript.sh</startupScript>
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

*** Special note: *** Use version 1.0.0.beta3 or better for *nix support.  Earlier versions only worked in windows.

Executable Permissions on Shell Scripts

When makeself is updated, performed the following commands after the update commit.  This will ensure executable only from this location.
While it might not provide any benefit, it doesn't hurt.

```git
    git update-index --chmod=+x makeself.sh
    git update-index --chmod=+x makeself-header.sh
    git commit -m "Force makeself to be executable"
```

To check the files are of right permissions

```stat
    stat -c "%a %n" *
```

*** Note: *** It seems no matter the change makself-header.sh wants to stay 644.  Regardless, with all other protections this is handled on *nix now and line endings are addressed.
