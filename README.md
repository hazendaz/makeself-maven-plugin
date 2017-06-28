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
                    <archiveDir>${project.build.directory}/distro</archiveDir>
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