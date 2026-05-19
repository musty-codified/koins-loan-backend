#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup batch script, version 3.2.0
# ----------------------------------------------------------------------------

# If JAVA_HOME is not set, try to detect it.
if [ -n "$JAVA_HOME" ] ; then
  if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
    JAVACMD="$JAVA_HOME/jre/sh/java"
  else
    JAVACMD="$JAVA_HOME/bin/java"
  fi
  if [ ! -x "$JAVACMD" ] ; then
    die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
  fi
else
  JAVACMD="$(
    'set' +e
    'unset' -f command 2>/dev/null
    'command' -v java
  )"
  if [ ! -x "$JAVACMD" ] ; then
    die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
  fi
fi

# Determine the Maven home directory and wrapper jar
APP_HOME="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)"
MAVEN_WRAPPER_JAR="$APP_HOME/.mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES="$APP_HOME/.mvn/wrapper/maven-wrapper.properties"

# Read properties
DISTRIBUTION_URL=$(grep "^distributionUrl" "$MAVEN_WRAPPER_PROPERTIES" | sed 's/.*=//;s/[[:space:]]//g')
WRAPPER_URL=$(grep "^wrapperUrl" "$MAVEN_WRAPPER_PROPERTIES" | sed 's/.*=//;s/[[:space:]]//g')

# Determine Maven home
MAVEN_HOME_BASENAME=$(basename "$DISTRIBUTION_URL" | sed 's/-bin.zip//')
MAVEN_HOME="$HOME/.m2/wrapper/dists/$MAVEN_HOME_BASENAME"
MAVEN_BIN="$MAVEN_HOME/bin/mvn"

# Download distribution if not present
if [ ! -f "$MAVEN_BIN" ]; then
  echo "Downloading Maven distribution from $DISTRIBUTION_URL"
  mkdir -p "$MAVEN_HOME"
  TMP_ZIP="$MAVEN_HOME/tmp-download.zip"
  if command -v curl > /dev/null 2>&1; then
    curl -fsSL -o "$TMP_ZIP" "$DISTRIBUTION_URL"
  elif command -v wget > /dev/null 2>&1; then
    wget -q -O "$TMP_ZIP" "$DISTRIBUTION_URL"
  else
    echo "ERROR: Neither curl nor wget is available. Cannot download Maven."
    exit 1
  fi
  unzip -q "$TMP_ZIP" -d "$HOME/.m2/wrapper/dists/"
  rm -f "$TMP_ZIP"
fi

# Download wrapper jar if not present
if [ ! -f "$MAVEN_WRAPPER_JAR" ]; then
  echo "Downloading Maven Wrapper jar from $WRAPPER_URL"
  if command -v curl > /dev/null 2>&1; then
    curl -fsSL -o "$MAVEN_WRAPPER_JAR" "$WRAPPER_URL"
  elif command -v wget > /dev/null 2>&1; then
    wget -q -O "$MAVEN_WRAPPER_JAR" "$WRAPPER_URL"
  fi
fi

exec "$MAVEN_BIN" "$@"
