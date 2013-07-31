package play.test;

import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.remote.DesiredCapabilities;
import play.Logger;
import play.libs.F.Callback;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.*;
import static play.test.SeleniumHelpers.runningRemote;

/**
 * This class should be extended to provide useful features for testing with JSCovergare
 */
public class JSCoverTest {

    private static final String JSCOVER_ASSETS_PATH = "/assets/jscover/javascripts/jscoverage.html";

    private static String jscoverReportTmp;

    @Rule
    public TestRule coverageReportRule = new Watcher();

    public static void runningRemoteDriverWithCoverage(final String defaultUrl,
                                                       final Callback<TestBrowser> testCallback,
                                                       String gridUrl,
                                                       DesiredCapabilities desiredCapabilities) {
        runningRemoteDriverWithCoverage(defaultUrl, testCallback, gridUrl, desiredCapabilities, null);
    }

    /**
     * Test the provided <code>defaultUrl</code> by going to it through a JSCover page.
     *
     * Once the page is reached, the browser window is changed.
     */
    public static void runningRemoteDriverWithCoverage(final String defaultUrl,
                                                       final Callback<TestBrowser> testCallback,
                                                       String gridUrl,
                                                       DesiredCapabilities desiredCapabilities,
                                                       DesiredCapabilities requiredCapabilities) {
        runningRemote(testServer(3333, fakeApplication(inMemoryDatabase("test"))), gridUrl, desiredCapabilities, new Callback<TestBrowser>() {
            @Override
            public void invoke(TestBrowser testBrowser) throws Throwable {
                // FIXME make relative to default Url
                testBrowser.goTo("http://localhost:3333" + JSCOVER_ASSETS_PATH);
                String jscoverWindowHandle = testBrowser.getDriver().getWindowHandle();
                Logger.trace(MessageFormat.format("JSCover Window Handle: %s", jscoverWindowHandle));
//        testBrowser.$("input[id=location]").text(defaultUrl);
                testBrowser.executeScript("document.getElementById('location').value = '" + defaultUrl + "'");
                testBrowser.$("button[id=openInWindowButton]").click();
                assertThat(testBrowser.getDriver().getWindowHandles().size()).isEqualTo(2);
                Set<String> windowHandles = testBrowser.getDriver().getWindowHandles();
                String newTab = null;
                for (String windowHandle : windowHandles) {
                    newTab = windowHandle;
                    if (!jscoverWindowHandle.equals(newTab)) {
                        break;
                    }
                }
                if (newTab == null) {
                    throw new IllegalStateException("The driver was unable to open a new window/tab");
                }
                Logger.trace(MessageFormat.format("New Tab for tests: %s", newTab));
                testBrowser.getDriver().switchTo().window(newTab);

                testCallback.invoke(testBrowser);

                Logger.trace((String) ((JavascriptExecutor) testBrowser.getDriver()).executeScript("return navigator.userAgent"));
                testBrowser.getDriver().switchTo().window(jscoverWindowHandle);
                String jsonReport = (String) ((JavascriptExecutor) testBrowser.getDriver()).executeScript("return jscoverage_serializeCoverageToJSON()");
                if (StringUtils.isNotBlank(jsonReport)) {
                    jscoverReportTmp = jsonReport;
                } else {
                    Logger.info("No report to generate for test: " + this.getClass().getName());
                }
            }
        });
    }

    private class Watcher extends TestWatcher {

        private File jscoverReports;

        private String jsonReport;

        @Override
        protected void starting(Description description) {
            jscoverReports = new File("target/test-reports/jscover");
            if (!jscoverReports.exists()) {
                jscoverReports.mkdir();
            }
            if (jscoverReports.isFile()) {
                throw new IllegalStateException("A file exists where JSCover reports should be saved");
            }
            Logger.debug(jscoverReports.getAbsolutePath());
            for (String childPath: jscoverReports.list()) {
                Logger.debug("\t" + childPath);
            }
            String reportFileName = description.getClassName() + "-" + description.getMethodName() + ".json";
            jscoverReports = new File(jscoverReports, reportFileName);
            if (!jscoverReports.exists()) {
                try {
                    jscoverReports.createNewFile();
                } catch (IOException e) {
                    Logger.error("Failed to create report file", e);
                }
            }
        }

        @Override
        protected void succeeded(Description description) {
            if (StringUtils.isNotEmpty(JSCoverTest.jscoverReportTmp)) {
                jsonReport = jscoverReportTmp;
            }
        }

        @Override
        protected void finished(Description description) {
            if (StringUtils.isNotBlank(jsonReport)) {
                try {
                    FileWriter fw = new FileWriter(jscoverReports);
                    fw.write(jsonReport);
                } catch (IOException e) {
                    Logger.error("Failed to write JSON coverage report", e);
                }
            }
        }
    }
}
