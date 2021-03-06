package play.api.test

import org.openqa.selenium.remote.{RemoteWebDriver, DesiredCapabilities}
import java.net.URL

object SeleniumHelpers {
  def myRunning[T](testServer: TestServer,
                    gridUrl: String,
                    desiredCapabilities: DesiredCapabilities,
                    requiredCapabilities: DesiredCapabilities = null)(block: TestBrowser => T): T = {
    var browser: TestBrowser = null
    synchronized {
      try {
        testServer.start()

        browser = TestBrowser(new RemoteWebDriver(new URL(gridUrl), desiredCapabilities, requiredCapabilities), None)
        block(browser)
      } finally {
        if (browser != null) {
          browser.quit()
        }
        testServer.stop()
      }
    }
  }
}

