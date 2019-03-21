package teammates.e2e.pageobjects;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Stack;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import teammates.e2e.util.TestProperties;
import teammates.test.driver.FileHelper;

/**
 * A programmatic interface to the Browser used to test the app.
 */
public class Browser {

    private static final String PAGE_LOAD_SCRIPT;

    static {
        try {
            PAGE_LOAD_SCRIPT = FileHelper.readFile("src/e2e/resources/scripts/waitForPageLoad.js");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The {@link WebDriver} object that drives the Browser instance.
     */
    // TODO change this to private once all legacy UI tests are migrated
    public WebDriver driver;

    /**
     * Indicates whether the app is being used by an admin.
     */
    public boolean isAdminLoggedIn;

    /**
     * Indicates to the {@link BrowserPool} that this object is currently being used
     * and not ready to be reused by another test.
     */
    boolean isInUse;

    /**
     * Name of the browser session.
     *
     * <p>This is only used for identification in SauceLabs.
     */
    private final String name;

    /**
     * Keeps track of multiple windows opened by the {@link WebDriver}.
     */
    private final Stack<String> windowHandles = new Stack<>();

    public Browser(String name) {
        this.name = name;
        this.driver = createWebDriver();
        this.driver.manage().window().maximize();
        this.isInUse = false;
        this.isAdminLoggedIn = false;
    }

    /**
     * Switches to new browser window for browsing.
     */
    public void switchToNewWindow() {
        String curWin = driver.getWindowHandle();
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(curWin) && !windowHandles.contains(curWin)) {
                windowHandles.push(curWin);
                driver.switchTo().window(handle);
                break;
            }
        }
    }

    /**
     * Waits for the page to load. This includes all AJAX requests and Angular animations in the page.
     */
    public void waitForPageLoad() {
        WebDriverWait wait = new WebDriverWait(driver, TestProperties.TEST_TIMEOUT);
        wait.until(driver -> {
            return "complete".equals(((JavascriptExecutor) driver).executeAsyncScript(PAGE_LOAD_SCRIPT));
        });
    }

    /**
     * Waits for the page to load by only looking at the page's readyState.
     */
    public void waitForPageReadyState() {
        WebDriverWait wait = new WebDriverWait(driver, TestProperties.TEST_TIMEOUT);
        wait.until(driver -> {
            return "complete".equals(((JavascriptExecutor) driver).executeScript("return document.readyState"));
        });
    }

    /**
     * Closes the current browser window and switches back to the last window used previously.
     */
    public void closeCurrentWindowAndSwitchToParentWindow() {
        driver.close();
        driver.switchTo().window(windowHandles.pop());
    }

    private WebDriver createWebDriver() {
        System.out.print("Initializing Selenium: ");

        String browser = TestProperties.BROWSER;
        if (TestProperties.BROWSER_FIREFOX.equals(browser)) {
            System.out.println("Using Firefox with driver path: " + TestProperties.GECKODRIVER_PATH);
            String firefoxPath = TestProperties.FIREFOX_PATH;
            if (!firefoxPath.isEmpty()) {
                System.out.println("Custom path: " + firefoxPath);
                System.setProperty("webdriver.firefox.bin", firefoxPath);
            }
            System.setProperty("webdriver.gecko.driver", TestProperties.GECKODRIVER_PATH);

            // Allow CSV files to be download automatically, without a download popup.
            // This method is used because Selenium cannot directly interact with the download dialog.
            // Taken from http://stackoverflow.com/questions/24852709
            FirefoxProfile profile = new FirefoxProfile();
            profile.setPreference("browser.download.panel.shown", false);
            profile.setPreference("browser.helperApps.neverAsk.openFile", "text/csv,application/vnd.ms-excel");
            profile.setPreference("browser.helperApps.neverAsk.saveToDisk", "text/csv,application/vnd.ms-excel");
            profile.setPreference("browser.download.folderList", 2);
            profile.setPreference("browser.download.dir", System.getProperty("java.io.tmpdir"));

            FirefoxOptions options = new FirefoxOptions().setProfile(profile);
            return new FirefoxDriver(options);
        }

        if (TestProperties.BROWSER_CHROME.equals(browser)) {
            System.out.println("Using Chrome with driver path: " + TestProperties.CHROMEDRIVER_PATH);
            System.setProperty("webdriver.chrome.driver", TestProperties.CHROMEDRIVER_PATH);

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--allow-file-access-from-files");
            return new ChromeDriver(options);
        }

        if (TestProperties.BROWSER_SAUCELABS.equals(browser)) {
            String[] settings = System.getenv("SAUCELABS_SETTINGS").split("; ");
            String browserName = settings[0];

            DesiredCapabilities caps;
            switch (browserName) {
            case "firefox":
                caps = DesiredCapabilities.firefox();
                break;
            case "chrome":
                caps = DesiredCapabilities.chrome();
                break;
            case "edge":
                caps = DesiredCapabilities.edge();
                break;
            case "safari":
                caps = DesiredCapabilities.safari();
                break;
            case "ie":
                caps = DesiredCapabilities.internetExplorer();
                break;
            case "opera":
                caps = DesiredCapabilities.operaBlink();
                break;
            default:
                throw new RuntimeException("Using " + browserName + " is not supported!");
            }

            String browserVersion = settings[1];
            String os = settings[2];
            String tunnelId = System.getenv("TUNNEL_ID");
            String buildId = System.getenv("BUILD_ID");

            caps.setCapability("version", browserVersion);
            caps.setCapability("platform", os);
            caps.setCapability("tunnel-identifier", tunnelId);
            caps.setCapability("build", buildId);
            caps.setCapability("name", buildId + " - " + name);

            try {
                String username = System.getenv("SAUCE_USERNAME");
                String accessKey = System.getenv("SAUCE_ACCESS_KEY");
                String url = "https://" + username + ":" + accessKey + "@ondemand.saucelabs.com:443/wd/hub";

                RemoteWebDriver rwd = new RemoteWebDriver(new URL(url), caps);
                rwd.setFileDetector(new LocalFileDetector());
                return rwd;
            } catch (MalformedURLException e) {
                System.out.println("Error while instantiating SauceLabs driver: " + e.getMessage());
                return null;
            }
        }

        throw new RuntimeException("Using " + browser + " is not supported!");
    }

}
