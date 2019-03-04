#Local Development
###Prerequisites
Note: This guide uses Homebrew (package manage for macOS). It is not necessary to use Homebrew, but it does
simplify the installation process.
* Install Homebrew: `/usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"`
    * If you already have Homebrew installed, make sure it is updated: `brew update`
* Add the cask-versions tap for Homebrew: `brew tap homebrew/cask-versions`
* Install Docker: `brew cask install docker`
* Install docker-machine: `brew install docker-machine`
* Set up Docker connection to nexus: https://confluence.nextiva.xyz/display/DP/Docker+connection+to+nexus
* Find OpenJDK 11 cask `brew search java`
    * Currently the cask for java 11 is just named 'java', but this may change in the future. You can verify by
    running `brew cask info <caskName>` to verify the version.
* Install OpenJDK 11 cask: `brew cask install java`
* Set JAVA_HOME to java 11 installation directory (/Library/Java/JavaVirtualMachines/jdk-11.0.1.jdk/Contents/Home)
    * If you need to use and switch between multiple Java versions, consider using jEnv to simplify this process
        http://www.jenv.be

###Starting the Application
* Start local postgres image `./docker/up.sh`
* Compile and start local application image `./docker/restartApp.sh`
    * Alternatively: Run FulfillmentApplication.java as a Spring Boot application using java -jar target/fulfillment-service.jar.
* Local swagger URL is available at http://localhost:8080/swagger-ui.html

# Java client
* Using the fulfillment-service-client is recommended when integrating Java applications with this service.
###Using the client
* Add the latest version of fulfillment-service-client as a dependency to your application.
* Import the OrderSubmissionServiceClient class into your application configuration.
* Specify fulfillment-service.url as a property

# Database setup
Docker will create a container for postgres - a local version of Postgresql does not need to be installed.
Before starting the application the tables in Postgres must be created. Until this is automatced login to pgAdmin
using fulfillment_app/fulfillment_app as the credentialsand run the script in 
fulfillment-service-web/resources/postgres/schema.sql.

# Environment properties
* This applications uses Kubernetes Config Maps to configure properties for an environment. The properties
configured in application.yml will be used unless overridden at https://git.nextiva.xyz/projects/REL/repos/k8s-platform/browse

# Swagger
* Dev: https://fulfillment-service.dev.nextiva.io/swagger-ui.ml
* Rc: https://fulfillment-service.qa.nextiva.io/swagger-ui.html
* Prod: https://fulfillment-service.prod.nextiva.io/swagger-ui.html

#Automated Testing
<!---
* Unit tests can be run using `mvn test -Dgroups=UnitTest`
* Integration tests (do not require Fulfillment Service but do require connection to third parties) 
can be run using `mvn test -Dgroups=IntegrationTest`
-->
* Functional tests (those that require Fulfillment Service running) can be run using `mvn -P integration-tests verify`