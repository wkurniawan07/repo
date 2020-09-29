package teammates.e2e.pageobjects;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.ScriptTimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.ProfilesIni;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import teammates.e2e.util.TestProperties;
import teammates.test.FileHelper;

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
        this.driver.manage().timeouts().pageLoadTimeout(TestProperties.TEST_TIMEOUT * 2, TimeUnit.SECONDS);
        this.driver.manage().timeouts().setScriptTimeout(TestProperties.TEST_TIMEOUT, TimeUnit.SECONDS);
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
        waitForPageLoad(false);
    }

    /**
     * Waits for the page to load. This includes all AJAX requests and Angular animations in the page.
     *
     * @param excludeToast Set this to true if toast message's disappearance should not be counted
     *         as criteria for page load's completion.
     */
    public void waitForPageLoad(boolean excludeToast) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, TestProperties.TEST_TIMEOUT);
            wait.until(driver -> {
                return "complete".equals(
                        ((JavascriptExecutor) driver).executeAsyncScript(PAGE_LOAD_SCRIPT, excludeToast ? 1 : 0)
                );
            });
        } catch (ScriptTimeoutException e) {
            System.out.println("Page could not load completely. Trying to continue test.");
        }
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

        String downloadPath;
        try {
            downloadPath = new File(TestProperties.TEST_DOWNLOADS_FOLDER).getCanonicalPath();
            System.out.println("Download path: " + downloadPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String browser = TestProperties.BROWSER;
        if (TestProperties.BROWSER_FIREFOX.equals(browser)) {
            System.out.println("Using Firefox with driver path: " + TestProperties.GECKODRIVER_PATH);
            String firefoxPath = TestProperties.FIREFOX_PATH;
            if (!firefoxPath.isEmpty()) {
                System.out.println("Custom path: " + firefoxPath);
                System.setProperty("webdriver.firefox.bin", firefoxPath);
            }
            System.setProperty("webdriver.gecko.driver", TestProperties.GECKODRIVER_PATH);

            FirefoxProfile profile;
            if (TestProperties.isDevServer()) {
                profile = new FirefoxProfile();
                profile.setPreference("browser.private.browsing.autostart", true);
            } else {
                // Get user data from browser to bypass google blocking automated log in.
                // Log in manually to teammates to use that log in data for e2e tests.
                ProfilesIni profileIni = new ProfilesIni();
                profile = profileIni.getProfile(TestProperties.FIREFOX_PROFILE_NAME);
                if (profile == null) {
                    throw new RuntimeException("Firefox profile not found. Failed to create webdriver.");
                }
            }

            // Allow CSV files to be download automatically, without a download popup.
            // This method is used because Selenium cannot directly interact with the download dialog.
            // Taken from http://stackoverflow.com/questions/24852709
            profile.setPreference("browser.download.panel.shown", false);
            profile.setPreference("browser.helperApps.neverAsk.openFile", "text/csv,application/vnd.ms-excel");
            profile.setPreference("browser.helperApps.neverAsk.saveToDisk", "text/csv,application/vnd.ms-excel");
            profile.setPreference("browser.download.folderList", 2);
            profile.setPreference("browser.download.dir", downloadPath);

            FirefoxOptions options = new FirefoxOptions().setProfile(profile);
            return new FirefoxDriver(options);
        }

        if (TestProperties.BROWSER_CHROME.equals(browser)) {
            System.out.println("Using Chrome with driver path: " + TestProperties.CHROMEDRIVER_PATH);
            System.setProperty("webdriver.chrome.driver", TestProperties.CHROMEDRIVER_PATH);

            Map<String, Object> chromePrefs = new HashMap<>();
            chromePrefs.put("download.default_directory", downloadPath);
            chromePrefs.put("profile.default_content_settings.popups", 0);
            ChromeOptions options = new ChromeOptions();
            options.setExperimentalOption("prefs", chromePrefs);
            options.addArguments("--allow-file-access-from-files");
            if (TestProperties.isDevServer()) {
                options.addArguments("incognito");
            } else {
                // Get user data from browser to bypass google blocking automated log in.
                // Log in manually to teammates to use that log in data for e2e tests.
                if (TestProperties.CHROME_USER_DATA_PATH.isEmpty()
                        || !Files.exists(Paths.get(TestProperties.CHROME_USER_DATA_PATH))) {
                    throw new RuntimeException("Chrome user data path not found. Failed to create webdriver.");
                }
                options.addArguments("user-data-dir=" + TestProperties.CHROME_USER_DATA_PATH);
            }

            return new ChromeDriver(options);
        }

        if (TestProperties.BROWSER_SAUCELABS.equals(browser)) {
            String[] settings = System.getenv("SAUCELABS_SETTINGS").split("; ");
            String browserName = settings[0];

            MutableCapabilities browserOpts;
            switch (browserName) {
            case "firefox":
                browserOpts = new FirefoxOptions();
                break;
            case "chrome":
                browserOpts = new ChromeOptions();
                ((ChromeOptions) browserOpts).setExperimentalOption("w3c", true);
                break;
            case "edge":
                browserOpts = new EdgeOptions();
                break;
            case "safari":
                browserOpts = new SafariOptions();
                break;
            case "ie":
                browserOpts = new InternetExplorerOptions();
                break;
            default:
                throw new RuntimeException("Using " + browserName + " is not supported!");
            }

            String browserVersion = settings[1];
            String os = settings[2];
            String tunnelId = System.getenv("TUNNEL_ID");
            String buildId = System.getenv("BUILD_ID");
            String username = System.getenv("SAUCE_USERNAME");
            String accessKey = System.getenv("SAUCE_ACCESS_KEY");

            MutableCapabilities sauceOpts = new MutableCapabilities();
            sauceOpts.setCapability("username", username);
            sauceOpts.setCapability("accessKey", accessKey);
            sauceOpts.setCapability("tunnelIdentifier", tunnelId);
            sauceOpts.setCapability("build", buildId);
            sauceOpts.setCapability("name", buildId + " - " + name);
            sauceOpts.setCapability("seleniumVersion", "3.141.59");

            browserOpts.setCapability("browserVersion", browserVersion);
            browserOpts.setCapability("platformName", os);
            browserOpts.setCapability("sauce:options", sauceOpts);

            try {
                URL url = new URL("https://ondemand.saucelabs.com:443/wd/hub");
                RemoteWebDriver rwd = new RemoteWebDriver(url, browserOpts);
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
