# scathon - Scala Marathon Client

This is a Scala Marathon Client. The aim is to provide a 100% coverage of the Marathon API (`v0.16.0+`) as a Scala SBT project.

## Build status

[![Build Status](https://travis-ci.org/AppMinistry/scathon.svg?branch=master)](https://travis-ci.org/AppMinistry/scathon)

## Usage

### Dependencies

    scalaVersion := "2.11.8"
    
    libraryDependencies ++= Seq(
      "com.appministry" %% "scathon-models" % "0.1.1",
      "com.appministry" %% "scathon-client" % "0.1.1"
    )
    
### Creating the client
    
    import com.appministry.scathon.apiClient._
    val client = new Client
    
## Examples

The client comes with a rich unit tests suite. A lot of examples can be found inside those. A few quickstart examples below.

### Loading applications:

    client.getApps().onComplete {
      case Success(apps) => apps.foreach { app => /* ... */ }
      case Failure(ex) => println(s"There was an error while loading applications ${ex.getMessage}")
    }
    
### Loading an application by ID

    client.getApp("/an/app/id").onComplete {
      case Success(app) => /* ... */
      case Failure(ex) => ex match {
        case _: NotFound => println(s"Application not found: ${ex.getMessage}")
        case _ => println(s"Error while loading an application: ${ex.getMessage}")
      }
    }
    
### Creating an application
    
    import com.appministry.scathon.models.mesos.UniqueConstraint
    import com.appministry.scathon.apiClient._
    import com.appministry.scathon.models._
    
    val app = Application( id = "/app/test",
                           cmd = Some("/bin/sleep"),
                           args = Some(List("10")),
                           ports = Some(List(10000,10001,10002)),
                           instances = 2,
                           container = Some(Container(
                            docker = Some(ContainerDocker(
                              image = testAppContainerDockerImage,
                              network = DockerNetworkTypes.BRIDGE,
                              portMappings = Some(List(
                                ContainerPortMapping(10000, 0, None, PortMappingTypes.TCP),
                                ContainerPortMapping(10001, 0, None, PortMappingTypes.TCP),
                                ContainerPortMapping(10002, 0, None, PortMappingTypes.TCP)
                              ))
                            ))
                           )),
                           env = Map("ENV_VAR" -> "some-value"),
                           labels = Map( "consul.watch" -> "true",
                                         "consul.id" -> "test-app-0",
                                         "consul.name" -> "test-app",
                                         "consul.tags" -> "tag1,tag2" ),
                           acceptedResourceRoles = Some(List("*")),
                           constraints = Some(List(UniqueConstraint("hostname"))),
                           fetch = Some(List(FetchUri("http://www.apache.org/dyn/closer.lua/spark/spark-1.6.1/spark-1.6.1.tgz"))),
                           healthChecks = Some(List(HttpHealthCheck())) )
    client.createApp(app).onComplete {
      case Success(createdApp) =>
        /* ... */
      case Failure(ex) => println(s"Error while creating an application: ${ex.getMessage}")
    }

### Subscribing to the event bus
    
    val callbackUri = "http://localhost:9000/marathon/callbacks"
    client.createEventSubscription(new URI(callbackUri)).onComplete {
      case Success(event) =>
        /* ... */
      case Failure(ex) => println(s"Failed to subscribe to the event bus: ${ex.getMessage}")
    }
    
## Code structure

This project comes in three modules:

- `scathon-client`: Marathon client
- `scathon-models`: models used by the client and...
- `scathon-test-server`: test server used by the client for unit tests

## Test server

This is a simplistic implementation of Marathon. It does not necessarily implement correct Marathon functionality but it simulates the responses Marathon would issue.  
The project is very helpful is one's goal is to write a Marathon based application without having to run a complex infrastructure like `mini-mesos`.  
To use the test server in one's unit tests:

    libraryDependencies ++= Seq(
      "org.appministry" %% "scathon-test-server" % "1.0.0" % "test"
    )

In the unit tests:

    import com.appministry.scathon.apiClient._
    import org.appministry.scathon.testServer._
    
    val server = new TestMarathon
    server.start()
    val client = new Client(port = server.port.get)
    /* ... */
    
### Configuring test server

- `testMarathon.bind-host`: host to bind test server on, default `localhost`
- `testMarathon.bind-port`: port to bind test server on, default `random free port`
- `testMarathon.executor-capacity`: thread executor capacity, default `10`
- `testMarathon.api-version`: Marathon API version, default `v2`
- `testMarathon.for-unit-tests`: used by the test server unit tests to verify correctness of the uris served, default `false`; usually, there is no need to play with this property

## License

Author: Rad Gruchalski (radek@gruchalski.com)

This work will be available under Apache License, Version 2.0.

Copyright 2016 Rad Gruchalski (radek@gruchalski.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License. You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.