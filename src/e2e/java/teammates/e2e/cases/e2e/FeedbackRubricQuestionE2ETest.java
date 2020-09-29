package teammates.e2e.cases.e2e;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import teammates.common.datatransfer.attributes.CourseAttributes;
import teammates.common.datatransfer.attributes.FeedbackQuestionAttributes;
import teammates.common.datatransfer.attributes.FeedbackResponseAttributes;
import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.datatransfer.questions.FeedbackRubricQuestionDetails;
import teammates.common.datatransfer.questions.FeedbackRubricResponseDetails;
import teammates.common.util.AppUrl;
import teammates.common.util.Const;
import teammates.e2e.pageobjects.AppPage;
import teammates.e2e.pageobjects.FeedbackSubmitPage;
import teammates.e2e.pageobjects.InstructorFeedbackEditPage;

/**
 * SUT: {@link Const.WebPageURIs#INSTRUCTOR_SESSION_EDIT_PAGE}, {@link Const.WebPageURIs#SESSION_SUBMISSION_PAGE}
 *      specifically for Rubric questions.
 */
public class FeedbackRubricQuestionE2ETest extends BaseE2ETestCase {
    InstructorAttributes instructor;
    CourseAttributes course;
    FeedbackSessionAttributes feedbackSession;
    StudentAttributes student;

    @Override
    protected void prepareTestData() {
        testData = loadDataBundle("/FeedbackRubricQuestionE2ETest.json");
        removeAndRestoreDataBundle(testData);

        instructor = testData.instructors.get("instructor");
        course = testData.courses.get("course");
        feedbackSession = testData.feedbackSessions.get("openSession");
        student = testData.students.get("alice.tmms@FRubricQuestionE2eT.CS2104");
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
        FeedbackRubricQuestionDetails questionDetails =
                (FeedbackRubricQuestionDetails) loadedQuestion.getQuestionDetails();
        feedbackEditPage.verifyRubricQuestionDetails(1, questionDetails);

        ______TS("add new question");
        // add new question exactly like loaded question
        loadedQuestion.setQuestionNumber(2);
        feedbackEditPage.addRubricQuestion(loadedQuestion);

        feedbackEditPage.verifyRubricQuestionDetails(2, questionDetails);
        verifyPresentInDatastore(loadedQuestion);

        ______TS("copy question");
        FeedbackQuestionAttributes copiedQuestion = testData.feedbackQuestions.get("qn1ForSecondSession");
        questionDetails = (FeedbackRubricQuestionDetails) copiedQuestion.getQuestionDetails();
        feedbackEditPage.copyQuestion(copiedQuestion.getCourseId(),
                copiedQuestion.getQuestionDetails().getQuestionText());
        copiedQuestion.courseId = course.getId();
        copiedQuestion.feedbackSessionName = feedbackSession.getFeedbackSessionName();
        copiedQuestion.setQuestionNumber(3);

        feedbackEditPage.verifyRubricQuestionDetails(3, questionDetails);
        verifyPresentInDatastore(copiedQuestion);

        ______TS("edit question");
        // add a new choice
        questionDetails = (FeedbackRubricQuestionDetails) loadedQuestion.getQuestionDetails();
        List<String> choices = questionDetails.getRubricChoices();
        choices.add("Edited choice.");
        List<List<String>> descriptions = questionDetails.getRubricDescriptions();
        descriptions.get(0).add("Edit description.");
        descriptions.get(1).add("Edit description 1.");
        questionDetails.setNumOfRubricChoices(3);
        // edit existing descriptions
        descriptions.get(0).set(1, "Edit description 2");
        descriptions.get(1).set(0, "");
        // edit existing subquestion
        List<String> subQns = questionDetails.getRubricSubQuestions();
        subQns.set(0, "Edited subquestion.");
        // add a new subquestion
        subQns.add("Added subquestion.");
        questionDetails.setNumOfRubricSubQuestions(3);
        descriptions.add(Arrays.asList("", "test", ""));
        // remove assigned weights
        questionDetails.setHasAssignedWeights(false);
        questionDetails.setRubricWeightsForEachCell(new ArrayList<>());
        loadedQuestion.questionDetails = questionDetails;
        feedbackEditPage.editRubricQuestion(2, questionDetails);

        feedbackEditPage.verifyRubricQuestionDetails(2, questionDetails);
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
        StudentAttributes receiver = testData.students.get("benny.tmms@FRubricQuestionE2eT.CS2104");
        feedbackSubmitPage.verifyRubricQuestion(1, receiver.getName(),
                (FeedbackRubricQuestionDetails) question.getQuestionDetails());

        ______TS("submit response");
        String questionId = getFeedbackQuestion(question).getId();
        FeedbackResponseAttributes response = getResponse(questionId, receiver, Arrays.asList(1, 1));
        feedbackSubmitPage.submitRubricResponse(1, receiver.getName(), response);

        verifyPresentInDatastore(response);

        ______TS("check previous response");
        feedbackSubmitPage = AppPage.getNewPageInstance(browser, url, FeedbackSubmitPage.class);
        feedbackSubmitPage.waitForPageToLoad();
        feedbackSubmitPage.verifyRubricResponse(1, receiver.getName(), response);

        ______TS("edit response");
        response = getResponse(questionId, receiver, Arrays.asList(0, 0));
        feedbackSubmitPage.submitRubricResponse(1, receiver.getName(), response);

        feedbackSubmitPage = AppPage.getNewPageInstance(browser, url, FeedbackSubmitPage.class);
        feedbackSubmitPage.waitForPageToLoad();
        feedbackSubmitPage.verifyRubricResponse(1, receiver.getName(), response);
        verifyPresentInDatastore(response);
    }

    private FeedbackResponseAttributes getResponse(String questionId, StudentAttributes receiver, List<Integer> answers) {
        FeedbackRubricResponseDetails details = new FeedbackRubricResponseDetails();
        details.setAnswer(answers);
        return FeedbackResponseAttributes.builder(questionId, student.getEmail(), receiver.getEmail())
                .withResponseDetails(details)
                .build();
    }
}
