package play.test;

import static play.test.Helpers.*;
import play.libs.F.*;

import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.net.URL;

public class SeleniumHelpers {

    public static void runningRemote(TestServer server,
                                     String gridUrl,
                                     DesiredCapabilities desiredCapabilities,
                                     final Callback<TestBrowser> block) {
        runningRemote(server, gridUrl, desiredCapabilities, null, block);
    }

    public static synchronized void runningRemote(TestServer server,
                                                  String gridUrl,
                                                  DesiredCapabilities desiredCapabilities,
                                                  DesiredCapabilities requiredCapabilities,
                                                  final Callback<TestBrowser> block) {
        TestBrowser browser = null;
        TestServer startedServer = null;
        try {
            start(server);
            startedServer = server;
            browser = testBrowser(new RemoteWebDriver(new URL(gridUrl), desiredCapabilities, requiredCapabilities));
            block.invoke(browser);
        } catch(Throwable t) {
            throw new RuntimeException(t);
        } finally {
            if(browser != null) {
                browser.quit();
            }
            if(startedServer != null) {
                stop(startedServer);
            }
        }
    }
}