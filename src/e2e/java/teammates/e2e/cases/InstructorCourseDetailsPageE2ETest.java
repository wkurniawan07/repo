package teammates.e2e.cases;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import teammates.common.datatransfer.attributes.CourseAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.util.AppUrl;
import teammates.common.util.Const;
import teammates.e2e.pageobjects.InstructorCourseDetailsPage;
import teammates.e2e.pageobjects.InstructorCourseStudentDetailsEditPage;
import teammates.e2e.pageobjects.InstructorCourseStudentDetailsViewPage;
import teammates.e2e.pageobjects.InstructorStudentRecordsPage;
import teammates.e2e.util.TestProperties;

/**
 * SUT: {@link Const.WebPageURIs#INSTRUCTOR_COURSE_DETAILS_PAGE}.
 */
public class InstructorCourseDetailsPageE2ETest extends BaseE2ETestCase {
    private StudentAttributes student;
    private CourseAttributes course;

    private String fileName;

    @Override
    protected void prepareTestData() {
        testData = loadDataBundle("/InstructorCourseDetailsPageE2ETest.json");
        student = testData.students.get("charlie.tmms@ICDetailsE2eT.CS2104");
        if (!TestProperties.isDevServer()) {
            student.email = TestProperties.TEST_EMAIL;
        }

        removeAndRestoreDataBundle(testData);
        course = testData.courses.get("ICDetailsE2eT.CS2104");
        fileName = "/" + course.getId() + "_studentList.csv";
    }

    @BeforeClass
    public void classSetup() {
        deleteDownloadsFile(fileName);
    }

    @Test
    public void allTests() {
        AppUrl detailsPageUrl = createUrl(Const.WebPageURIs.INSTRUCTOR_COURSE_DETAILS_PAGE)
                .withUserId(testData.instructors.get("ICDetailsE2eT.instr").googleId)
                .withCourseId(course.getId());
        InstructorCourseDetailsPage detailsPage = loginAdminToPage(detailsPageUrl, InstructorCourseDetailsPage.class);

        ______TS("verify loaded details");
        InstructorAttributes[] instructors = {
                testData.instructors.get("ICDetailsE2eT.instr"),
                testData.instructors.get("ICDetailsE2eT.instr2"),
        };
        StudentAttributes[] students = {
                testData.students.get("alice.tmms@ICDetailsE2eT.CS2104"),
                testData.students.get("benny.tmms@ICDetailsE2eT.CS2104"),
                testData.students.get("charlie.tmms@ICDetailsE2eT.CS2104"),
                testData.students.get("danny.tmms@ICDetailsE2eT.CS2104"),
        };

        verifyCourseDetails(detailsPage, course, instructors, students);
        detailsPage.verifyNumStudents(students.length);
        detailsPage.verifyStudentDetails(students);

        ______TS("link: view student details page");

        StudentAttributes studentToView = testData.students.get("benny.tmms@ICDetailsE2eT.CS2104");

        InstructorCourseStudentDetailsViewPage studentDetailsViewPage =
                detailsPage.clickViewStudent(studentToView);
        studentDetailsViewPage.verifyIsCorrectPage(course.getId(), studentToView.getEmail());
        studentDetailsViewPage.closeCurrentWindowAndSwitchToParentWindow();

        ______TS("link: edit student details page");

        InstructorCourseStudentDetailsEditPage studentDetailsEditPage =
                detailsPage.clickEditStudent(studentToView);
        studentDetailsEditPage.verifyIsCorrectPage(course.getId(), studentToView.getEmail());
        studentDetailsEditPage.closeCurrentWindowAndSwitchToParentWindow();

        ______TS("link: view all records page");

        InstructorStudentRecordsPage studentRecordsPage =
                detailsPage.clickViewAllRecords(studentToView);
        studentRecordsPage.verifyIsCorrectPage(course.getId(), studentToView.getName());
        studentRecordsPage.closeCurrentWindowAndSwitchToParentWindow();

        ______TS("send invite");
        detailsPage.sendInvite(student);

        detailsPage.verifyStatusMessage("An email has been sent to " + student.getEmail());
        String expectedEmailSubject = "TEAMMATES: Invitation to join course ["
                + course.getName() + "][" + course.getId() + "]";
        verifyEmailSent(student.getEmail(), expectedEmailSubject);

        ______TS("remind all students to join");
        detailsPage.remindAllToJoin();

        detailsPage.verifyStatusMessage("Emails have been sent to unregistered students.");
        verifyEmailSent(student.getEmail(), expectedEmailSubject);

        ______TS("download student list");
        detailsPage.downloadStudentList();
        String status = student.googleId.isEmpty() ? "Yet to Join" : "Joined";
        String lastName = student.getName().split(" ")[1];
        String[] studentInfo = { student.getTeam(), student.getName(), lastName, status, student.getEmail() };
        List<String> expectedContent = Arrays.asList("Course ID," + course.getId(),
                "Course Name," + course.getName(), String.join(",", studentInfo));
        verifyDownloadedFile(fileName, expectedContent);

        ______TS("delete student");
        detailsPage.sortByName();
        detailsPage.sortByStatus();
        StudentAttributes[] studentsAfterDelete = { students[3], students[0], students[1] };
        detailsPage.deleteStudent(student);

        detailsPage.verifyStatusMessage("Student is successfully deleted from course \""
                + course.getId() + "\"");
        detailsPage.verifyNumStudents(studentsAfterDelete.length);
        detailsPage.verifyStudentDetails(studentsAfterDelete);
        verifyAbsentInDatastore(student);

        ______TS("delete all students");
        detailsPage.deleteAllStudents();

        detailsPage.verifyStatusMessage("All the students have been removed from the course");
        detailsPage.verifyNumStudents(0);
        for (StudentAttributes student : studentsAfterDelete) {
            verifyAbsentInDatastore(student);
        }
    }

    private void verifyCourseDetails(InstructorCourseDetailsPage detailsPage, CourseAttributes course,
                                     InstructorAttributes[] instructors, StudentAttributes[] students) {
        Set<String> sections = new HashSet<>();
        Set<String> teams = new HashSet<>();

        for (StudentAttributes student : students) {
            sections.add(student.getSection());
            teams.add(student.getTeam());
        }

        detailsPage.verifyCourseDetails(course, instructors, sections.size(), teams.size(), students.length);
    }
}
