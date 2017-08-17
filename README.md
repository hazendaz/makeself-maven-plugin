Makeself Maven Plugin
=====================

[![Build Status](https://travis-ci.org/hazendaz/makeself-maven-plugin.svg?branch=master)](https://travis-ci.org/hazendaz/makeself-maven-plugin)
[![License](http://img.shields.io/:license-glp-blue.svg)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)

![makeself-maven-plugin](http://hazendaz.github.io/images/hazendaz-logo.png)

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

Executable Permissions on Shell Scripts

Ensure that when makeself is updated, that the following is performed after the update commit.  This will ensures executable only from this location.  While it might not provide any benefit, it doesn't hurt.

git update-index --chmod=+x makeself.sh
git update-index --chmod=+x makeself-header.sh
git commit -M "Force makeself to be executable"
