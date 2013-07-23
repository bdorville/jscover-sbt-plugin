package play.test;

import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.openqa.selenium.JavascriptExecutor;

import play.Logger;
import play.test.*;
import play.libs.F.*;

import java.io.File;
import java.io.FileWriter;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Set;

import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;

public class JSCoverTest {

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

  public static void runningWithCoverage(final String defaultUrl, final Callback<TestBrowser> testCallback) {
    running(testServer(3333, fakeApplication(inMemoryDatabase())), HTMLUNIT, new Callback<TestBrowser>() {
      @Override
      public void invoke(TestBrowser testBrowser) throws Throwable {
        testBrowser.goTo("http://localhost:3333/assets/jscover/javascripts/jscoverage.html");
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

        System.out.println(((JavascriptExecutor)testBrowser.getDriver()).executeScript("navigator.userAgent"));
        testBrowser.getDriver().switchTo().window(jscoverWindowHandle);
        String jsonReport = (String)((JavascriptExecutor)testBrowser.getDriver()).executeScript("jscoverage_serializeCoverageToJSON()");
        if (StringUtils.isNotBlank(jsonReport)) {
          File report = new File(jscoverReports, this.getClass().getName() + ".report");
          if (!report.exists()) {
            report.createNewFile();
          }
          FileWriter fw = new FileWriter(report, true);
          fw.write(jsonReport);
        } else {
          Logger.info("No report to generate for test: " + this.getClass().getName());
        }
      }
    });
  }
}

