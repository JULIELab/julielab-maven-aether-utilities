#!/usr/bin/env bash
if [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
	echo "Executing deploy"
    mvn deploy -P sonatype-nexus-deployment --settings travis-deployment/mvnsettings.xml -DskipTests=true
else
	echo "Deploy not executed"
fi
