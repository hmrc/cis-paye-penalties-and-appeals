# cis-paye-penalties-and-appeals

This is the backend service responsible for coordinating the business processing and submission of appeals for CIS & PAYE.

For more information please refer to the [documentation](https://confluence.tools.tax.service.gov.uk/spaces/RBD/pages/1389331265/14.+Penalities+and+Appeals+PAS).

## Running the service
Before starting, you will need to have  [service-manager](https://github.com/hmrc/service-manager) installed/configured

### Dependencies
All dependencies can be found in [AppDependencies.scala](https://github.com/hmrc/cis-paye-penalties-and-appeals-frontend/blob/main/project/AppDependencies.scala)

### Running locally:
Service Manager:
- Start dependent services `sm2 --start PAS_ALL`
- Stop this service `sm2 --stop CIS_PAYE_PENALTIES_AND_APPEALS`
- Start the server locally on port 10913 `sbt 'run 61001'`

### Testing:
- Run unit tests: `sbt test`
- Run integration tests: `sbt it/test`
- To run all tests and coverage: `sbt clean compile coverage test it/test coverageOff coverageReport`

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").