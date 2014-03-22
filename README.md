http://jhipster.github.io/

Deployment process - see the online documentation (http://central.sonatype.org/pages/apache-maven.html)

1) Performing a Snapshot Deployment
  1.1) mvn clean deploy

2) Performing a Release Deployment
  1.1) mvn release:clean release:prepare
  1.2) mvn release:perform

3) Releasing the Deployment to the Central Repository
  1.1) mvn nexus-staging:release
  1.2) mvn release:perform
  1.3) cd target/checkout
       mvn nexus-staging:release

