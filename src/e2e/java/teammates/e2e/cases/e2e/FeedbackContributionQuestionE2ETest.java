package teammates.e2e.cases.e2e;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import teammates.common.datatransfer.attributes.CourseAttributes;
import teammates.common.datatransfer.attributes.FeedbackQuestionAttributes;
import teammates.common.datatransfer.attributes.FeedbackResponseAttributes;
import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.datatransfer.questions.FeedbackContributionQuestionDetails;
import teammates.common.datatransfer.questions.FeedbackContributionResponseDetails;
import teammates.common.util.AppUrl;
import teammates.common.util.Const;
import teammates.e2e.pageobjects.AppPage;
import teammates.e2e.pageobjects.FeedbackSubmitPage;
import teammates.e2e.pageobjects.InstructorFeedbackEditPage;

/**
 * SUT: {@link Const.WebPageURIs#INSTRUCTOR_SESSION_EDIT_PAGE}, {@link Const.WebPageURIs#SESSION_SUBMISSION_PAGE}
 *      specifically for Contribution questions.
 */
public class FeedbackContributionQuestionE2ETest extends BaseE2ETestCase {
    InstructorAttributes instructor;
    CourseAttributes course;
    FeedbackSessionAttributes feedbackSession;
    StudentAttributes student;

    @Override
    protected void prepareTestData() {
        testData = loadDataBundle("/FeedbackContributionQuestionE2ETest.json");
        removeAndRestoreDataBundle(testData);

        instructor = testData.instructors.get("instructor");
        course = testData.courses.get("course");
        feedbackSession = testData.feedbackSessions.get("openSession");
        student = testData.students.get("alice.tmms@FContributionQuestionE2eT.CS2104");
    }

    @Test
    public void testAll() {
        testEditPage();
        testSubmitPage();
    }

    private void testEditPage() {
        AppUrl url = createUrl(Const.WebPageURIs.INSTRUCTOR_SESSION_EDIT_PAGE)
                .withUserId(instructor.googleId)
                .withCourseId(course.getId())
                .withSessionName(feedbackSession.getFeedbackSessionName());
        InstructorFeedbackEditPage feedbackEditPage = loginAdminToPage(url, InstructorFeedbackEditPage.class);
        feedbackEditPage.waitForPageToLoad();

        ______TS("verify loaded question");
        FeedbackQuestionAttributes loadedQuestion = testData.feedbackQuestions.get("qn1ForFirstSession").getCopy();
        FeedbackContributionQuestionDetails questionDetails =
                (FeedbackContributionQuestionDetails) loadedQuestion.getQuestionDetails();
        feedbackEditPage.verifyContributionQuestionDetails(1, questionDetails);

        ______TS("add new question");
        // add new question exactly like loaded question
        loadedQuestion.setQuestionNumber(2);
        feedbackEditPage.addContributionQuestion(loadedQuestion);

        feedbackEditPage.verifyContributionQuestionDetails(2, questionDetails);
        verifyPresentInDatastore(loadedQuestion);

        ______TS("copy question");
        FeedbackQuestionAttributes copiedQuestion = testData.feedbackQuestions.get("qn1ForSecondSession");
        questionDetails = (FeedbackContributionQuestionDetails) copiedQuestion.getQuestionDetails();
        feedbackEditPage.copyQuestion(copiedQuestion.getCourseId(),
                copiedQuestion.getQuestionDetails().getQuestionText());
        copiedQuestion.courseId = course.getId();
        copiedQuestion.feedbackSessionName = feedbackSession.getFeedbackSessionName();
        copiedQuestion.setQuestionNumber(3);

        feedbackEditPage.verifyContributionQuestionDetails(3, questionDetails);
        verifyPresentInDatastore(copiedQuestion);

        ______TS("edit question");
        questionDetails = (FeedbackContributionQuestionDetails) loadedQuestion.getQuestionDetails();
        questionDetails.setNotSureAllowed(false);
        loadedQuestion.questionDetails = questionDetails;
        feedbackEditPage.editContributionQuestion(2, questionDetails);

        feedbackEditPage.verifyContributionQuestionDetails(2, questionDetails);
        verifyPresentInDatastore(loadedQuestion);
    }

    private void testSubmitPage() {
        AppUrl url = createUrl(Const.WebPageURIs.SESSION_SUBMISSION_PAGE)
                .withUserId(student.googleId)
                .withCourseId(student.course)
                .withSessionName(feedbackSession.getFeedbackSessionName())
                .withRegistrationKey(getKeyForStudent(student));
        FeedbackSubmitPage feedbackSubmitPage = loginAdminToPage(url, FeedbackSubmitPage.class);
        feedbackSubmitPage.waitForPageToLoad();

        ______TS("verify loaded question");
        FeedbackQuestionAttributes question = testData.feedbackQuestions.get("qn1ForFirstSession");
        StudentAttributes receiver = testData.students.get("benny.tmms@FContributionQuestionE2eT.CS2104");
        StudentAttributes receiver2 = testData.students.get("charlie.tmms@FContributionQuestionE2eT.CS2104");
        feedbackSubmitPage.verifyContributionQuestion(1,
                (FeedbackContributionQuestionDetails) question.getQuestionDetails());

        ______TS("submit response");
        String questionId = getFeedbackQuestion(question).getId();
        FeedbackResponseAttributes response = getResponse(questionId, student, 170);
        FeedbackResponseAttributes response2 = getResponse(questionId, receiver, 180);
        FeedbackResponseAttributes response3 = getResponse(questionId, receiver2, 60);
        List responses = Arrays.asList(response, response2, response3);
        feedbackSubmitPage.submitContributionResponse(1, responses);

        verifyPresentInDatastore(response);
        verifyPresentInDatastore(response2);
        verifyPresentInDatastore(response3);

        ______TS("check previous response");
        feedbackSubmitPage = AppPage.getNewPageInstance(browser, url, FeedbackSubmitPage.class);
        feedbackSubmitPage.waitForPageToLoad();
        feedbackSubmitPage.verifyContributionResponse(1, responses);

        ______TS("edit response");
        response = getResponse(questionId, student, 50);
        response2 = getResponse(questionId, receiver, Const.POINTS_EQUAL_SHARE);
        response3 = getResponse(questionId, receiver2, Const.POINTS_NOT_SURE);
        responses = Arrays.asList(response, response2, response3);
        feedbackSubmitPage.submitContributionResponse(1, responses);

        feedbackSubmitPage = AppPage.getNewPageInstance(browser, url, FeedbackSubmitPage.class);
        feedbackSubmitPage.waitForPageToLoad();
        feedbackSubmitPage.verifyContributionResponse(1, responses);
        verifyPresentInDatastore(response);
        verifyPresentInDatastore(response2);
        verifyPresentInDatastore(response3);
    }

    private FeedbackResponseAttributes getResponse(String questionId, StudentAttributes receiver, int answer) {
        FeedbackContributionResponseDetails details = new FeedbackContributionResponseDetails();
        details.setAnswer(answer);
        return FeedbackResponseAttributes.builder(questionId, student.getEmail(), receiver.getEmail())
                .withResponseDetails(details)
                .build();
    }
}
