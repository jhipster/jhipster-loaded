http://jhipster.github.io/

Deployment process
==================
See the online documentation (http://central.sonatype.org/pages/apache-maven.html)

1. Performing a Snapshot Deployment
    1. mvn clean deploy

1. Performing a Release Deployment
    1. mvn versions:set -DnewVersion=1.2.3
    1. mvn clean deploy -P release

1. Releasing the Deployment to the Central Repository
    1. mvn nexus-staging:release -Prelease
    1. mvn versions:set -DnewVersion=1.2.3-SNAPSHOT

