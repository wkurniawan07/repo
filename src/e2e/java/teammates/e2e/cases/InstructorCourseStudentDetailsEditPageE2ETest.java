package teammates.e2e.cases;

import org.testng.annotations.Test;

import teammates.common.datatransfer.attributes.CourseAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.util.AppUrl;
import teammates.common.util.Const;
import teammates.e2e.pageobjects.AppPage;
import teammates.e2e.pageobjects.InstructorCourseStudentDetailsEditPage;
import teammates.e2e.util.TestProperties;

/**
 * SUT: {@link Const.WebPageURIs#INSTRUCTOR_COURSE_STUDENT_DETAILS_EDIT_PAGE}.
 */
public class InstructorCourseStudentDetailsEditPageE2ETest extends BaseE2ETestCase {
    private StudentAttributes student;
    private StudentAttributes otherStudent;
    private CourseAttributes course;

    @Override
    protected void prepareTestData() {
        testData = loadDataBundle("/InstructorCourseStudentDetailsEditPageE2ETest.json");
        removeAndRestoreDataBundle(testData);

        student = testData.students.get("ICSDEditE2eT.jose.tmms");
        otherStudent = testData.students.get("ICSDEditE2eT.benny.c");
        course = testData.courses.get("ICSDEditE2eT.CS2104");
    }

    @Test
    public void testAll() {
        AppUrl editPageUrl = createUrl(Const.WebPageURIs.INSTRUCTOR_COURSE_STUDENT_DETAILS_EDIT_PAGE)
                .withUserId(testData.instructors.get("ICSDEditE2eT.instr").googleId)
                .withCourseId(course.getId())
                .withStudentEmail(student.email);
        InstructorCourseStudentDetailsEditPage editPage =
                loginAdminToPage(editPageUrl, InstructorCourseStudentDetailsEditPage.class);

        ______TS("verify loaded data");
        editPage.verifyStudentDetails(student);

        ______TS("edit student details");
        student.name = "edited name";
        student.section = "edited section";
        student.team = "edited team";
        student.comments = "edited comment";
        editPage.editStudentDetails(student);

        editPage.verifyStatusMessage("Student has been updated");
        verifyPresentInDatastore(student);

        ______TS("cannot edit to an existing email");
        editPage = AppPage.getNewPageInstance(browser, editPageUrl, InstructorCourseStudentDetailsEditPage.class);
        editPage.editStudentEmailAndResendLinks(otherStudent.email);

        editPage.verifyStatusMessage("Trying to update to an email that is already in use");

        ______TS("edit email and resend links");
        String newEmail = "editedEmail@gmail.tmt";
        if (!TestProperties.isDevServer()) {
            newEmail = TestProperties.TEST_EMAIL;
        }
        student.email = newEmail;
        student.googleId = null;
        editPage.editStudentEmailAndResendLinks(newEmail);

        editPage.verifyStatusMessage("Student has been updated and email sent");
        verifyPresentInDatastore(student);
        verifyEmailSent(newEmail, "TEAMMATES: Summary of course ["
                + course.getName() + "][Course ID: " + course.getId() + "]");
    }
}
