package teammates.e2e.cases;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.openqa.selenium.JavascriptExecutor;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.attributes.AccountAttributes;
import teammates.common.datatransfer.attributes.CourseAttributes;
import teammates.common.datatransfer.attributes.FeedbackQuestionAttributes;
import teammates.common.datatransfer.attributes.FeedbackResponseAttributes;
import teammates.common.datatransfer.attributes.FeedbackResponseCommentAttributes;
import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.datatransfer.attributes.StudentProfileAttributes;
import teammates.common.exception.HttpRequestFailedException;
import teammates.common.util.AppUrl;
import teammates.common.util.Const;
import teammates.common.util.ThreadHelper;
import teammates.common.util.retry.RetryManager;
import teammates.e2e.pageobjects.AdminHomePage;
import teammates.e2e.pageobjects.AppPage;
import teammates.e2e.pageobjects.Browser;
import teammates.e2e.pageobjects.DevServerLoginPage;
import teammates.e2e.pageobjects.HomePage;
import teammates.e2e.util.BackDoor;
import teammates.e2e.util.EmailAccount;
import teammates.e2e.util.TestProperties;
import teammates.test.BaseTestCaseWithDatastoreAccess;
import teammates.test.FileHelper;

/**
 * Base class for all browser tests.
 *
 * <p>This type of test has no knowledge of the workings of the application,
 * and can only communicate via the UI or via {@link BackDoor} to obtain/transmit data.
 */
public abstract class BaseE2ETestCase extends BaseTestCaseWithDatastoreAccess {

    static final BackDoor BACKDOOR = BackDoor.getInstance();

    protected Browser browser;
    protected DataBundle testData;

    @BeforeClass
    public void baseClassSetup() throws Exception {
        prepareTestData();
        prepareBrowser();
    }

    protected void prepareBrowser() {
        browser = new Browser(getClass().getSimpleName());
    }

    protected abstract void prepareTestData() throws Exception;

    @Override
    protected String getTestDataFolder() {
        return TestProperties.TEST_DATA_FOLDER;
    }

    protected String getTestDownloadsFolder() {
        return TestProperties.TEST_DOWNLOADS_FOLDER;
    }

    @AfterClass
    public void baseClassTearDown(ITestContext context) {
        boolean isSuccess = context.getFailedTests().getAllMethods()
                .stream()
                .noneMatch(method -> method.getConstructorOrMethod().getMethod().getDeclaringClass() == this.getClass());
        releaseBrowser(isSuccess);
    }

    protected void releaseBrowser(boolean isSuccess) {
        if (browser == null) {
            return;
        }
        if (TestProperties.BROWSER_SAUCELABS.equals(TestProperties.BROWSER)) {
            ((JavascriptExecutor) browser.driver).executeScript("sauce:job-result=" + (isSuccess ? "passed" : "failed"));
            browser.driver.close();
        } else {
            if (isSuccess || TestProperties.CLOSE_BROWSER_ON_FAILURE) {
                browser.driver.close();
            }
        }
    }

    /**
     * Creates an {@link AppUrl} for the supplied {@code relativeUrl} parameter.
     * The base URL will be the value of test.app.url in test.properties.
     * {@code relativeUrl} must start with a "/".
     */
    protected static AppUrl createUrl(String relativeUrl) {
        return new AppUrl(TestProperties.TEAMMATES_URL + relativeUrl);
    }

    /**
     * Logs in a page using admin credentials (i.e. in masquerade mode).
     */
    protected <T extends AppPage> T loginAdminToPage(AppUrl url, Class<T> typeOfPage) {
        // When not using dev server, Google blocks log in by automation.
        // To log in, log in manually to teammates in your browser before running e2e tests.
        // Refer to teammates.e2e.pageobjects.Browser for more information.
        if (!TestProperties.isDevServer()) {
            // skip login and navigate to the desired page.
            return AppPage.getNewPageInstance(browser, url, typeOfPage);
        }

        if (browser.isAdminLoggedIn) {
            try {
                return AppPage.getNewPageInstance(browser, url, typeOfPage);
            } catch (Exception e) {
                //ignore and try to logout and login again if fail.
            }
        }

        // logout and attempt to load the requested URL. This will be
        // redirected to a dev-server login page
        logout();
        browser.driver.get(url.toAbsoluteString());

        // In dev server, any username is acceptable as admin
        String adminUsername = "devserver.admin.account";

        String userId = url.get(Const.ParamsNames.USER_ID);

        if (userId != null) {
            // This workaround is necessary because the front-end has not been optimized
            // to enable masquerade mode yet
            adminUsername = userId;
        }

        DevServerLoginPage loginPage = AppPage.getNewPageInstance(browser, DevServerLoginPage.class);
        loginPage.loginAsAdmin(adminUsername);

        return AppPage.getNewPageInstance(browser, url, typeOfPage);
    }

    /**
     * Equivalent to clicking the 'logout' link in the top menu of the page.
     */
    protected void logout() {
        browser.driver.get(createUrl(Const.ResourceURIs.LOGOUT).toAbsoluteString());
        AppPage.getNewPageInstance(browser, HomePage.class).waitForPageToLoad();
        browser.isAdminLoggedIn = false;
    }

    protected AdminHomePage loginAdmin() {
        return loginAdminToPage(createUrl(Const.WebPageURIs.ADMIN_HOME_PAGE), AdminHomePage.class);
    }

    /**
     * Deletes file with fileName from the downloads folder.
     */
    protected void deleteDownloadsFile(String fileName) {
        String filePath = getTestDownloadsFolder() + fileName;
        FileHelper.deleteFile(filePath);
    }

    /**
     * Verifies downloaded file has correct fileName and contains expected content.
     */
    protected void verifyDownloadedFile(String expectedFileName, List<String> expectedContent) {
        if (TestProperties.BROWSER_SAUCELABS.equals(TestProperties.BROWSER)) {
            return;
        }
        String filePath = getTestDownloadsFolder() + expectedFileName;
        int retryLimit = 5;
        boolean actual = Files.exists(Paths.get(filePath));
        while (!actual && retryLimit > 0) {
            retryLimit--;
            ThreadHelper.waitFor(1000);
            actual = Files.exists(Paths.get(filePath));
        }
        assertTrue(actual);

        try {
            String actualContent = FileHelper.readFile(filePath);
            for (String content : expectedContent) {
                assertTrue(actualContent.contains(content));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verifies that email with subject is found in inbox.
     * Email used must be an authentic gmail account.
     */
    protected void verifyEmailSent(String email, String subject) {
        if (TestProperties.isDevServer()) {
            return;
        }
        EmailAccount emailAccount = new EmailAccount(email);
        try {
            emailAccount.getUserAuthenticated();
            int retryLimit = 5;
            boolean actual = emailAccount.isEmailWithSubjectPresent(subject);
            while (!actual && retryLimit > 0) {
                retryLimit--;
                ThreadHelper.waitFor(1000);
                actual = emailAccount.isEmailWithSubjectPresent(subject);
            }
            emailAccount.markAllUnreadEmailAsRead();
            assertTrue(actual);
        } catch (Exception e) {
            fail("Failed to verify email sent:" + e);
        }
    }

    @Override
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    public void setupObjectify() {
        // Not necessary as BackDoor API is used instead
    }

    @Override
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    public void tearDownObjectify() {
        // Not necessary as BackDoor API is used instead
    }

    @Override
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    public void setUpGae() {
        // Not necessary as BackDoor API is used instead
    }

    @Override
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    public void tearDownGae() {
        // Not necessary as BackDoor API is used instead
    }

    @Override
    protected RetryManager getPersistenceRetryManager() {
        return new RetryManager(TestProperties.PERSISTENCE_RETRY_PERIOD_IN_S / 2);
    }

    protected AccountAttributes getAccount(String googleId) {
        return BACKDOOR.getAccount(googleId);
    }

    @Override
    protected AccountAttributes getAccount(AccountAttributes account) {
        return getAccount(account.googleId);
    }

    @Override
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    protected StudentProfileAttributes getStudentProfile(StudentProfileAttributes studentProfileAttributes) {
        return null; // BACKDOOR.getStudentProfile(studentProfileAttributes.googleId);
    }

    protected CourseAttributes getCourse(String courseId) {
        return BACKDOOR.getCourse(courseId);
    }

    @Override
    protected CourseAttributes getCourse(CourseAttributes course) {
        return getCourse(course.getId());
    }

    protected CourseAttributes getArchivedCourse(String instructorId, String courseId) {
        return BACKDOOR.getArchivedCourse(instructorId, courseId);
    }

    protected FeedbackQuestionAttributes getFeedbackQuestion(String courseId, String feedbackSessionName, int qnNumber) {
        return BACKDOOR.getFeedbackQuestion(courseId, feedbackSessionName, qnNumber);
    }

    @Override
    protected FeedbackQuestionAttributes getFeedbackQuestion(FeedbackQuestionAttributes fq) {
        return getFeedbackQuestion(fq.courseId, fq.feedbackSessionName, fq.questionNumber);
    }

    protected FeedbackResponseCommentAttributes getFeedbackResponseComment(String feedbackResponseId) {
        return BACKDOOR.getFeedbackResponseComment(feedbackResponseId);
    }

    @Override
    protected FeedbackResponseCommentAttributes getFeedbackResponseComment(FeedbackResponseCommentAttributes frc) {
        return getFeedbackResponseComment(frc.feedbackResponseId);
    }

    protected FeedbackResponseAttributes getFeedbackResponse(String feedbackQuestionId, String giver, String recipient) {
        return BACKDOOR.getFeedbackResponse(feedbackQuestionId, giver, recipient);
    }

    @Override
    protected FeedbackResponseAttributes getFeedbackResponse(FeedbackResponseAttributes fr) {
        return getFeedbackResponse(fr.feedbackQuestionId, fr.giver, fr.recipient);
    }

    protected FeedbackSessionAttributes getFeedbackSession(String courseId, String feedbackSessionName) {
        return BACKDOOR.getFeedbackSession(courseId, feedbackSessionName);
    }

    @Override
    protected FeedbackSessionAttributes getFeedbackSession(FeedbackSessionAttributes fs) {
        return getFeedbackSession(fs.getCourseId(), fs.getFeedbackSessionName());
    }

    protected FeedbackSessionAttributes getSoftDeletedSession(String feedbackSessionName, String instructorId) {
        return BACKDOOR.getSoftDeletedSession(feedbackSessionName, instructorId);
    }

    protected InstructorAttributes getInstructor(String courseId, String instructorEmail) {
        return BACKDOOR.getInstructor(courseId, instructorEmail);
    }

    @Override
    protected InstructorAttributes getInstructor(InstructorAttributes instructor) {
        return getInstructor(instructor.courseId, instructor.email);
    }

    protected String getKeyForInstructor(String courseId, String instructorEmail) {
        return getInstructor(courseId, instructorEmail).getKey();
    }

    @Override
    protected StudentAttributes getStudent(StudentAttributes student) {
        return BACKDOOR.getStudent(student.course, student.email);
    }

    protected String getKeyForStudent(StudentAttributes student) {
        return getStudent(student).getKey();
    }

    @Override
    protected String doRemoveAndRestoreDataBundle(DataBundle testData) throws HttpRequestFailedException {
        return BACKDOOR.removeAndRestoreDataBundle(testData);
    }

    @Override
    protected String doPutDocuments(DataBundle testData) {
        return BACKDOOR.putDocuments(testData);
    }

}
