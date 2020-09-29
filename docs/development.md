# Development Guidelines

These are the common tasks involved when working on features, enhancements, bug fixes, etc. for TEAMMATES.

* [Managing the dev server: front-end](#managing-the-dev-server-front-end)
* [Managing the dev server: back-end](#managing-the-dev-server-back-end)
* [Building front-end files](#building-front-end-files)
* [Logging in to a TEAMMATES instance](#logging-in-to-a-teammates-instance)
* [Testing](#testing)
* [Deploying to a staging server](#deploying-to-a-staging-server)
* [Running client scripts](#running-client-scripts)
* [Config points](#config-points)

The instructions in all parts of this document work for Linux, OS X, and Windows, with the following pointers:
- Replace `./gradlew` to `gradlew.bat` if you are using Windows.
- All the commands are assumed to be run from the root project folder, unless otherwise specified.
- It is assumed that the development environment has been correctly set up. If this step has not been completed, refer to [this document](setting-up.md).

> If you encounter any problems during the any of the processes, please refer to our [troubleshooting guide](troubleshooting-guide.md) before posting a help request on our [issue tracker](https://github.com/TEAMMATES/teammates/issues).

## Managing the dev server: front-end

> `Dev server` is the server run in your local machine.

Front-end dev server is the Angular-based server handling the user interface.

First, you need to compile some type definitions from the back-end to be used in this dev server. Run the following command:
```sh
./gradlew generateTypes
```

To start the dev server, run the following command until you see something like `｢wdm｣: Compiled successfully.`:
```sh
npm run start
```

The dev server URL will be given at the console output, e.g. `http://localhost:4200`.

To stop the dev server, press `Ctrl + C`.

- The dev server is run in _watch mode_ by default, i.e. any saved change to the front-end code will be propagated to the server immediately.
- The dev server is also run in _live reload mode_ by default, i.e. any saved change to the front-end code will automatically load all dev server web pages currently being opened.
  To disable this behaviour, run the dev server as follows instead:

  ```sh
  npm run start -- --live-reload=false
  ```

## Managing the dev server: back-end

Back-end dev server is the Google App Engine-based server handling all the business logic, including data storage.

### Starting the dev server

To start the server in the background, run the following command
and wait until the task exits with a `BUILD SUCCESSFUL`:
```sh
./gradlew appengineStart
```

To start the server in the foreground (e.g. if you want the console output to be visible),
run the following command instead:
```sh
./gradlew appengineRun
```

The dev server URL will be `http://localhost:8080` as specified in `build.gradle`.

### Stopping the dev server

If you started the server in the background, run the following command to stop it:
```sh
./gradlew appengineStop
```

If the server is running in the foreground, press `Ctrl + C` (or equivalent in your OS) to stop it or run the above command in a new console.

## Building front-end files

In order for the dev server to be able to serve both the front-end and the back-end of the application, the front-end files need to be *bundled and transpiled* (afterwards `built`).

Run the following commands to build the front-end files for the application's use in production mode:
```sh
# Generate type definition file from back-end
./gradlew generateTypes

# Bundle, transpile, and minify front-end files
npm run build
```

After this, the back-end dev server will also be able to serve the front-end.

## Logging in to a TEAMMATES instance

This instruction set applies for both dev server and production server, with slight differences explained where applicable.
- The local dev server is assumed to be accessible at `http://localhost:8080`.
  - This instruction also works when the local front-end dev server and back-end dev server are separate. In that case, the dev server address will be the front-end's, e.g. `http://localhost:4200`. However, a back-end server needs to be running in order for the authentication logic to work.
- If a URL is given as relative, prepend the server URL to access the page, e.g `/web/page/somePage` is accessible in dev server at `http://localhost:8080/web/page/somePage`.

### As administrator

1. Go to any administrator page, e.g `/web/admin/home`.
1. On the dev server, log in using any username, but remember to check the `Log in as administrator` check box. You will have the required access.
1. On the production server, you will be granted the access only if your account has administrator permission to the application.
1. When logged in as administrator, ***masquerade mode*** can also be used to impersonate instructors and students by adding `user=username` to the URL
 e.g `http://localhost:8080/web/student/home?user=johnKent`.

### As instructor

You need an instructor account which can be created by administrators.

1. Log in to `/web/admin/home` as an administrator.
1. Enter credentials for an instructor, e.g<br>
   Name: `John Dorian`<br>
   Email: `teammates.instructor@university.edu`<br>
   Institution: `National University of Singapore`<br>
1. The system will send an email containing the join link to the added instructor.<br>
   On the dev server, this email will not be sent. Instead, you can use the join link given after adding an instructor to complete the joining process.

Alternatively, an instructor can create other instructors for a course if s/he has sufficient privileges. A course co-owner, for example, will have such a privilege.

1. Ensure that there is a course to add instructors to and an instructor in that course with the privilege to add instructors.
1. Log in as that instructor.
1. Add the instructors for the course (`Instructors` → `View/Edit`).
1. The system will send an email containing the join link to each added instructor. Again, this will not happen on the dev server, so additional steps are required.
1. Log out and log in to `http://localhost:8080/web/admin/search` as administrator.
1. Search for the instructor you added in. From the search results, click anywhere on the desired row to get the course join link for that instructor.
1. Log out and use that join link to log in as the new instructor.

### As student

You need a student account which can be created by instructors (with sufficient privileges).

The steps for adding a student is almost identical to the steps for adding instructors by another instructor:
- Where appropriate, change the reference to "instructor" to "student".
- `Students` → `Enroll` to add students for the course.

**Alternative**: Run the test cases, they create several student and instructor accounts in the datastore. Use one of them to log in.

### Logging in without UI

In dev server, it is also possible to "log in" without UI (e.g. when only testing API endpoints). In order to do that, you need to submit the following API call:

```sh
POST http://localhost:8080/_ah/login?action=Log+In&email=test@example.com&isAdmin=on
```

where `email=test@example.com` and `isAdmin=on` can be replaced as appropriate.

The back-end server will return cookies which will subsequently be used to authenticate your requests.

To "log out", submit the following API call:

```sh
POST http://localhost:8080/_ah/login?action=Log+Out
```

## Testing

There are two big categories of testing in TEAMMATES:
- **Component tests**: white-box unit and integration tests, i.e. they test the application components with full knowledge of the components' internal workings. This is configured in `src/test/resources/testng-component.xml` (back-end) and `src/web/jest.config.js` (front-end).
- **E2E (end-to-end) tests**: black-box tests, i.e. they test the application as a whole without knowing any internal working. This is configured in `src/e2e/resources/testng-e2e.xml`.

### Configuring browsers for E2E Testing

TEAMMATES E2E testing requires Firefox or Chrome.

Before running tests, modify `src/e2e/resources/test.properties` if necessary, e.g. to configure which browser and test accounts to use.

#### Using Firefox

* You need to use geckodriver for testing with Firefox.
  * Download the latest stable geckodriver from [here](https://github.com/mozilla/geckodriver/releases).
    The site will also inform the versions of Firefox that can be used with the driver.
  * Specify the path to the geckodriver executable in `test.geckodriver.path` value in `test.properties`.

* If you want to use a Firefox version other than your computer's default, specify the custom path in `test.firefox.path` value in `test.properties`.

* If you are planning to test changes to JavaScript code, disable JavaScript caching for Firefox:
  * Enter `about:config` into the Firefox address bar and set `network.http.use-cache` (or `browser.cache.disk.enable` in newer versions of Firefox) to `false`.

#### Using Chrome

* You need to use chromedriver for testing with Chrome.
  * Download the latest stable chromedriver from [here](https://sites.google.com/a/chromium.org/chromedriver/downloads).
    The site will also inform the versions of Chrome that can be used with the driver.
  * Specify the path to the chromedriver executable in `test.chromedriver.path` value in `test.properties`.

* If you are planning to test changes to JavaScript code, disable JavaScript caching for Chrome:
  * Press Ctrl+Shift+J to bring up the Web Console.
  * Click on the settings button at the bottom right corner.
  * Under the General tab, check "Disable Cache".

* The chromedriver process started by the test suite will not automatically get killed after the tests have finished executing.<br>
  You will need to manually kill these processes after the tests are done.
  * On Windows, use the Task Manager or `taskkill /f /im chromedriver.exe` command.
  * On OS X, use the Activity Monitor or `sudo killall chromedriver` command.

### Running the tests

- When running the test cases, a few cases may fail (this can happen due to timing issues). They can be re-run until they pass without affecting the accuracy of the tests.

#### Running the tests with command line

To run all front-end component tests in watch mode (i.e. any change to source code will automatically reload the tests), run the following command:
```sh
npm run test
```

To run all front-end component tests once and generate coverage data afterwards, run the following command:
```sh
npm run coverage
```

To run an individual test in a test file, change `it` in the `*.spec.ts` file to `fit`.

To run all tests in a test file (or all test files matching a pattern), you can use Jest's watch mode and filter by filename pattern.

Back-end component tests and E2E tests follow this configuration:

Test suite | Command | Results can be viewed in
---|---|---
`Component tests` | `./gradlew componentTests` | `{project folder}/build/reports/tests/componentTests/index.html`
`E2E tests` | `./gradlew e2eTests` | `{project folder}/build/reports/e2e-test-try-{n}/index.html`, where `{n}` is the sequence number of the test run
Any individual component test | `./gradlew componentTests --tests TestClassName` | `{project folder}/build/reports/tests/componentTests/index.html`
Any individual E2E test | `./gradlew e2eTestTry1 --tests TestClassName` | `{project folder}/build/reports/e2e-test-try-1/index.html`

- `E2E tests` will be run in their entirety once and the failed tests will be re-run a few times. All other test suites will be run once and only once.
- Before running `E2E tests`, it is important to have the both front-end and back-end dev servers running locally first if you are testing against them.

You can generate the coverage data with `jacocoReport` task after running tests, e.g.:
```sh
./gradlew componentTests jacocoReport
```
The report can be found in the `build/reports/jacoco/jacocoReport/` directory.

### Testing against production server

If you are testing against a production server (staging server or live server), some additional tasks need to be done.

1. You need to setup a `Gmail API`<sup>1</sup> as follows:
   * [Obtain a Gmail API credentials](https://github.com/TEAMMATES/teammates-ops/blob/master/platform-guide.md) and download it.
   * Copy the file to `src/e2e/resources/gmail-api` (create the `gmail-api` folder) of your project and rename it to `client_secret.json`.
   * It is also possible to use the Gmail API credentials from any other Google Cloud Platform project for this purpose.

1. Edit `src/e2e/resources/test.properties` as instructed is in its comments.
   * In particular, you will need legitimate Google accounts to be used for testing.

1. Run the full test suite or any subset of it as how you would have done it in dev server.
   * Do note that the GAE daily quota is usually not enough to run the full test suite, in particular for accounts with no billing enabled.

<sup>1</sup> This setup is necessary because our test suite uses the Gmail API to access Gmail accounts used for testing (these accounts are specified in `test.properties`) to confirm that those accounts receive the expected emails from TEAMMATES.
This is needed only when testing against a production server because no actual emails are sent by the dev server and therefore delivery of emails is not tested when testing against the dev server.

## Deploying to a staging server

> `Staging server` is the server instance you set up on Google App Engine for hosting the app for testing purposes.

For most cases, you do not need a staging server as the dev server has covered almost all of the application's functionality.
If you need to deploy your application to a staging server, refer to [this guide](https://github.com/TEAMMATES/teammates-ops/blob/master/platform-guide.md#deploying-to-a-staging-server).

## Running client scripts

> Client scripts are scripts that remotely manipulate data on GAE via its Remote API. They are run as standard Java applications.

Most of developers may not need to write and/or run client scripts but if you are to do so, take note of the following:

* If you are to run a script in a production environment, there are additional steps to follow. Refer to [this guide](https://github.com/TEAMMATES/teammates-ops/blob/master/platform-guide.md#running-client-scripts).
* It is not encouraged to compile and run any script via command line; use any of the supported IDEs to significantly ease this task.

## Config points

There are several files used to configure various aspects of the system.

**Main**: These vary from developer to developer and are subjected to frequent changes.
* `build.properties`: Contains the general purpose configuration values to be used by the web API.
* `config.ts`: Contains the general purpose configuration values to be used by the web application.
* `test.properties`: Contains the configuration values for the test driver.
  * There are two separate `test.properties`; one for component tests and one for E2E tests.
* `client.properties`: Contains some configuration values used in client scripts.
* `appengine-web.xml`: Contains the configuration for deploying the application on GAE.

**Tasks**: These do not concern the application directly, but rather the development process.
* `build.gradle`: Contains the back-end third-party dependencies specification, as well as configurations for automated tasks/routines to be run via Gradle.
* `gradle.properties`, `gradle-wrapper.properties`: Contains the Gradle and Gradle wrapper configuration.
* `package.json`: Contains the front-end third-party dependencies specification, as well as configurations for automated tasks/routines to be run via NPM.
* `angular.json`: Contains the Angular application configuration.
* `.travis.yml`: Contains the Travis CI job configuration.
* `appveyor.yml`: Contains the AppVeyor CI job configuration.

**Static Analysis**: These are used to maintain code quality and measure code coverage. See [Static Analysis](static-analysis.md).
* `static-analysis/*`: Contains most of the configuration files for all the different static analysis tools.

**Other**: These are rarely, if ever will be, subjected to changes.
* `logging.properties`: Contains the java.util.logging configuration.
* `web.xml`: Contains the web server configuration, e.g servlets to run, mapping from URLs to servlets, security constraints, etc.
* `cron.xml`: Contains the cron jobs specification.
* `queue.xml`: Contains the task queues configuration.
* `datastore-indexes.xml`: Contains the Datastore indexes configuration.
