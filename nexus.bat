@echo off
if "%1"=="" (
    java -jar "%~dp0target\nexus-autopilot-1.0-SNAPSHOT-jar-with-dependencies.jar"
    goto :eof
)
if /i "%1"=="start" (
    if not exist "%~dp0target\nexus-autopilot-1.0-SNAPSHOT-jar-with-dependencies.jar" (
        echo [nexus] Building project...
        call mvn package -q -f "%~dp0pom.xml"
    )
    java -jar "%~dp0target\nexus-autopilot-1.0-SNAPSHOT-jar-with-dependencies.jar" start
    goto :eof
)
java -jar "%~dp0target\nexus-autopilot-1.0-SNAPSHOT-jar-with-dependencies.jar" %*
