#!/bin/bash
#
#    Copyright 2011-2019 the original author or authors.
#
#    This program is free software; you can redistribute it and/or
#    modify it under the terms of the GNU General Public License
#    as published by the Free Software Foundation; either version 2
#    of the License, or (at your option) any later version.
#
#    You may obtain a copy of the License at
#
#       https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#


# Get Commit Message
commit_message=$(git log --format=%B -n 1)
echo "Current commit detected: ${commit_message}"

# We build for several JDKs on Travis.
# Some actions, like analyzing the code (Coveralls) and uploading
# artifacts on a Maven repository, should only be made for one version.
 
# If the version is 1.8, then perform the following actions.
# 1. Upload artifacts to Sonatype.
# 2. Use -q option to only display Maven errors and warnings.
# 3. Use --settings to force the usage of our "settings.xml" file.
# 4. Notify Coveralls.
# 5. Deploy site

if [ $TRAVIS_REPO_SLUG == "hazendaz/makeself-maven-plugin" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "master" ] && [[ "$commit_message" != *"[maven-release-plugin]"* ]]; then

  if [ $TRAVIS_JDK_VERSION == "oraclejdk8" ]; then

    # Deploy to sonatype
    ./mvnw clean deploy -q -Dinvoker.skip=true --settings ./travis/settings.xml
    echo -e "Successfully deployed SNAPSHOT artifacts to Sonatype under Travis job ${TRAVIS_JOB_NUMBER}"

	# Deploy to coveralls
    # ./mvnw clean test jacoco:report coveralls:report -q --settings ./travis/settings.xml
    # echo -e "Successfully ran coveralls under Travis job ${TRAVIS_JOB_NUMBER}"

    # Deploy to site
    # Cannot currently run site this way
	# ./mvnw site site:deploy -q --settings ./travis/settings.xml
	# echo -e "Successfully deploy site under Travis job ${TRAVIS_JOB_NUMBER}"
  else
    echo "Java Version does not support additonal activity for travis CI"
  fi
else
  echo "Travis Pull Request: $TRAVIS_PULL_REQUEST"
  echo "Travis Branch: $TRAVIS_BRANCH"
  echo "Travis build skipped"
fi