http://jhipster.github.io/

Deployment process
==================
See the online documentation (http://central.sonatype.org/pages/apache-maven.html)

1. Performing a Snapshot Deployment
    1. mvn clean deploy

1. Performing a Release Deployment
    1. mvn release:clean release:prepare
    1. mvn release:perform

1. Releasing the Deployment to the Central Repository
    1. mvn nexus-staging:release
    1. mvn release:perform
    1. cd target/checkout
    1. mvn nexus-staging:release

