@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM Apache Maven Wrapper startup batch script, version 3.2.0

@IF "%__MVNW_ARG0_NAME__%"=="" (SET "BASE_DIR=%~dp0")

@SET MAVEN_PROJECTBASEDIR=%BASE_DIR%
@IF NOT "%MAVEN_BASEDIR%"=="" SET "MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%"

@SET MAVEN_WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
@SET MAVEN_WRAPPER_PROPERTIES="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties"

@FOR /F "tokens=1,2 delims==" %%a IN (%MAVEN_WRAPPER_PROPERTIES%) DO (
  @IF "%%a"=="distributionUrl" SET "DISTRIBUTION_URL=%%b"
)

@SET MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists

@IF EXIST "%JAVA_HOME%\bin\java.exe" (
  SET "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) ELSE (
  SET "JAVA_EXE=java"
)

@"%JAVA_EXE%" -classpath %MAVEN_WRAPPER_JAR% org.apache.maven.wrapper.MavenWrapperMain %* 2>NUL
@IF %ERRORLEVEL% NEQ 0 (
  ECHO "Maven Wrapper failed. Ensure Java is installed and JAVA_HOME is set."
  EXIT /B %ERRORLEVEL%
)
