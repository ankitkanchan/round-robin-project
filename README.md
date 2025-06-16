# round-robin-project
Round Robin api router

The round-robin-router-project is an attempt to implement an api router, which can route requests to simple backend servers in a round robin fashion. It has primarily two components, round-robin-simple-api
And round-robin-router-api

Please go through the instructions/guidelines below carefully to successfully setup and run the project.

# Round Robin Simple API
===============================
This project is a simple Spring Boot application that provides two basic API endpoints:

1.  A POST endpoint that echoes back the JSON payload it receives.
2.  A GET endpoint for a health check.

This API can be used as a backend service for testing round-robin-router-api’s  or other systems that need a simple HTTP target.

## Features

*   **Echo Endpoint**: `/reply` (POST) - Returns the exact JSON payload sent in the request body.
*   **Health Check Endpoint**: `/reply/health` (GET) - Returns a JSON response indicating the service status (e.g., `{"status": "OK"}`).
*   Built with **Spring Boot 3.2.5**.
*   Uses **Java 21**.
*   Managed with **Maven**.

## Prerequisites

*   Java Development Kit (JDK) 21 or higher.
*   Apache Maven 3.6.0 or higher.

## execution steps (For MacOS/linux)
Extract the project Zip File and cd to the root directory using a terminal

For eg: cd/<~yourdir~>/round-robin-project

You should be able to see two folders
1.) round-robin-router-api
2.) round-robin-simple-api

First, we will bring up 3 instances of round-robin-simple-api 

###Do the following:
==============
### 1.)  Navigate to the simple api project using a bash terminal

cd round-robin-simple-api

### 2. Build the Project, You can build the project, run tests and move it to a runnable state using the below command, make sure you have MAVEN_HOME and JAVA_HOME env variables defined: 

 mvn clean install

#### This command will compile the code, run tests, and create an executable JAR file in the `target/` directory (e.g., `round-robin-simple-api-1.0-SNAPSHOT.jar`).

### 3. Run the Application

Once the project is built, you can run the application using the following command, make sure you can run 3 instances on port 8081, 8082, 8083 respectively, using the SERVER_PORT env variable:

SERVER_PORT=8081 mvn spring-boot:run
SERVER_PORT=8082 mvn spring-boot:run
SERVER_PORT=8083 mvn spring-boot:run

The application will start, and by default, it will be accessible at `http://localhost:8081`,`http://localhost:8082`,`http://localhost:8083`. You should see Spring Boot startup logs in your console.

## API Endpoints

### Echo Payload

*   **URL**: `/reply`
*   **Method**: `POST`
*   **Request Body**: Any valid JSON payload.

json:
{ "message": "Hello, world!", "data": { "id": 123, "active": true } }

OR 

json: 
{"game":"Mobile Legends", "gamerID":"GYUTDTE", "points":20}

*   **Success Response**:
    *   **Code**: `200 OK`
    *   **Content**: The same JSON payload that was sent in the request.

json:  
{ "message": "Hello, world!", "data": { "id": 123, "active": true } }

OR 

json: 
{"game":"Mobile Legends", "gamerID":"GYUTDTE", "points":20}


### Health Check

*   **URL**: `/reply/health`
*   **Method**: `GET`
*   **Success Response**:
    *   **Code**: `200 OK`
    *   **Content**:

json { "status": "OK" }

## Running Tests

The project includes unit tests for the controller. You can run these tests using Maven:
mvn test
The test results will be displayed in the console.


## Project Structure

round-robin-simple-api/  
	├── pom.xml # Maven Project Object Model 
	└── src/ 
			├── main/ │ 
					 ├── java/ │ │ 
							└── com/coda/roundrobin/ simpleapi/  │ 
																│ ├── SimpleAPIPostApplication. java # Main Spring Boot application class │ 
																│ └── controller/ │ 
																				│ └── SimpleAPIController. java # API controller 
					│ └── resources/ │ 
									└── application.properties # Spring Boot configuration (if any) 
			└── test/ 
					└── java/ 
							└── com/coda/roundrobin/ simpleapi/  |
																└── controller/ 
																			└── SimpleAPIControllerTest. java # Unit tests for the controller


## Technologies Used
*   Java 21
*   Spring Boot 3.2.5
    *   Spring Web
    *   Spring Boot Starter Test (for JUnit 5 and Mockito)
*   Maven

======================================================================xxxxxxxx===========================================================

# Round Robin Router API
=====================================================

This project implements a simple round-robin load balancer that distributes incoming HTTP requests to a set of backend servers. It uses Java's built-in `HttpServer` for handling requests and a configurable list of backend URLs.  The core logic for handling requests and distributing them is within the `RoundRobinHandler` class.

## Features

*   **Round Robin Load Balancing**: Distributes incoming requests evenly across configured backend servers.
*   **Configurable Backends**: Backend server URLs are loaded from a configuration file (`config.properties`).
*   **Basic Health Checking**: Implements a simple circuit breaker pattern to avoid sending requests to unhealthy backends.  If a backend fails to respond after a configurable number of retries, it's temporarily removed from the rotation.
*   **Asynchronous Handling**: Utilizes a cached thread pool for handling requests concurrently.
*   **Testability**: Designed with testability in mind, includes unit tests and a load test to verify functionality and performance.

## Getting Started

### Prerequisites

*   Java 21 or higher (ensure `java` and `javac` are in your PATH).

### Building and Running

###1. For eg: cd/<~yourdir~>/round-robin-project

##You should be able to see two folders
##1.) round-robin-router-api
##2.) round-robin-simple-api

Do the following:
==============
### 1.)  Navigate to the router api project using the terminal
cd round-robin-router-api
  

2.  **Compile:**  Since this project doesn't use a build tool like Maven or Gradle, you'll need to compile manually.
  
    $ javac -cp "lib/*" -Xlint:deprecation src/com/coda/roundrobin/**/**/*.java src/com/coda/roundrobin/**/*.java
    
    *Note:* If you do not have the `lib` directory with necessary jars, create one and download the jars into the directory.

3.  **Configuration:** The backend URLs are specified in `src/resources/config.properties`.  You can modify this file to point to your desired backend servers. The default configuration is:
    ```properties
    backends=http://localhost:8081/reply,http://localhost:8082/reply,http://localhost:8083/reply
    ```

4.  **Run:**

   $ java -cp "lib/*:src" com.coda.roundrobin.router.RoundRobinRouter
   
This starts the router on port 8090.  You should see the message:  `RoundRobinRouter listening on http://localhost:8090/route`

### Testing

#### Unit Tests

*Note:* You'll need to download `junit-platform-console-standalone-1.10.2.jar` and other JUnit/Mockito jars (for running unit tests) and place them in your `lib` directory. Make sure the paths match your directory structure.
Download URLs: 
https://mvnrepository.com/artifact/org.apiguardian/apiguardian-api/1.1.2
https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api/5.8.1

#### Load Test

The project also includes a load test to evaluate the router's performance under stress,  It has a variable to simulate concurrent loads as well.  To run the load test:

java -cp "lib/*:src" com.coda.roundrobin.tests.loadtests.LoadTester

This will send a series of requests to the router and print statistics about success rates, failures, latency, and throughput.

## Project Structure
round-robin-router-api/  
├── lib/ # External dependencies (if any) - e.g., commons-net, JUnit, Mockito 
├── src/ │ 
		└── com/ │ 
				└── coda/ │ 
						└── roundrobin/ │ 
								├── router/ │ 
									 │ └── RoundRobinRouter.java # Main class - starts the router │ 
								├── httphandler/ │ 
									│ ├── RoundRobinHandler.java # Request handling and load balancing logic │ 
									│ └── circuitbreaker/ │ 
											│ └── CircuitBreaker.java # Simple health check implementation 
								│ └── utils/ │
										 └── ConfigFileReader.java # Loads backend URLs from the config file 
	
								|└── tests/ │ 
										├── unittests/ │ 
												│ └── RoundRobinHandlerTest. java # Unit tests for RoundRobinHandler │ 
										└── loadtests/ │ 
												└── LoadTester.java # Load testing tool 
		└── resources/ 
				└── config.properties # Configuration file for backend URLs

## Configuration
The backend servers are specified in `src/resources/config.properties`. Edit this file to configure the target URLs. The format is a comma-separated list of URLs.



