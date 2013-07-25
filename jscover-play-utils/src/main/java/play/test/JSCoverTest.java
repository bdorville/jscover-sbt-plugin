package play.test;

import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.remote.DesiredCapabilities;

import play.Logger;
import play.test.*;
import play.libs.F.*;

import java.io.File;
import java.io.FileWriter;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Set;

import static play.test.Helpers.*;
import static play.test.SeleniumHelpers.*;
import static org.fest.assertions.Assertions.*;

/**
 * This class should be extended to provide useful features for testing with JSCovergare
 */
public class JSCoverTest {

    private static final String JSCOVER_ASSETS_PATH = "/assets/jscover/javascripts/jscoverage.html";

    private static File jscoverReports;

    @BeforeClass
    public static void before() {
        jscoverReports = new File("target/test-reports/jscover");
        if (!jscoverReports.exists()) {
            jscoverReports.mkdir();
        }
        if (jscoverReports.isFile()) {
            throw new IllegalStateException("A file exists where JSCover reports should be saved");
        }
        System.out.println(jscoverReports.getAbsolutePath());
        for (String childPath: jscoverReports.list()) {
            System.out.println("\t" + childPath);
        }
    }

    public static void runningWithCoverage(final String defaultUrl,
                                           final Callback<TestBrowser> testCallback,
                                           String gridUrl,
                                           DesiredCapabilities desiredCapabilities) {
        runningWithCoverage(defaultUrl, testCallback, gridUrl, desiredCapabilities, null);
    }

    /**
     * Test the provided <code>defaultUrl</code> by going to it through a JSCover page.
     *
     * Once the page is reached, the browser window is changed.
     */
    public static void runningWithCoverage(final String defaultUrl,
                                           final Callback<TestBrowser> testCallback,
                                           String gridUrl,
                                           DesiredCapabilities desiredCapabilities,
                                           DesiredCapabilities requiredCapabilities) {
        running(testServer(3333, fakeApplication(inMemoryDatabase("test"))), gridUrl, DesiredCapabilities.chrome(), new Callback<TestBrowser>() {
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
                for (Iterator<String> it = windowHandles.iterator(); it.hasNext();) {
                    newTab = it.next();
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

                Logger.trace((String)((JavascriptExecutor)testBrowser.getDriver()).executeScript("return navigator.userAgent"));
                testBrowser.getDriver().switchTo().window(jscoverWindowHandle);
                String jsonReport = (String)((JavascriptExecutor)testBrowser.getDriver()).executeScript("return jscoverage_serializeCoverageToJSON()");
                if (StringUtils.isNotBlank(jsonReport)) {
                    File report = new File(jscoverReports, this.getClass().getName() + ".report");
                    if (!report.exists()) {
                        report.createNewFile();
                    }
                    FileWriter fw = new FileWriter(report, true);
                    fw.write(jsonReport);
                    fw.close();
                } else {
                    Logger.info("No report to generate for test: " + this.getClass().getName());
                }
            }
        });
    }
}
