JSCover Utils for Play
======================

This projetc provides some Helper classes to
 - use Selenium Remote driver instances to connect to a global Grid
 - be able to simply reference instrumented files from scala view templates

Sample test:
    MyHelpers.myRunning(TestServer(3333), "http://localhost:4444/wd/hub", DesiredCapabilities.chrome()) { browser =>
      browser.goTo("http://localhost:3333/")
      browser.pageSource must contain("Your new application is ready.")
    }
