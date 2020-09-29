package teammates.logic.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import teammates.common.datatransfer.AttributesDeletionQuery;
import teammates.common.datatransfer.CourseRoster;
import teammates.common.datatransfer.FeedbackParticipantType;
import teammates.common.datatransfer.UserRole;
import teammates.common.datatransfer.attributes.FeedbackQuestionAttributes;
import teammates.common.datatransfer.attributes.FeedbackResponseAttributes;
import teammates.common.datatransfer.attributes.FeedbackResponseCommentAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.exception.TeammatesException;
import teammates.common.util.Assumption;
import teammates.common.util.Logger;
import teammates.storage.api.FeedbackResponsesDb;

/**
 * Handles operations related to feedback responses.
 *
 * @see FeedbackResponseAttributes
 * @see FeedbackResponsesDb
 */
public final class FeedbackResponsesLogic {

    private static final Logger log = Logger.getLogger();

    private static FeedbackResponsesLogic instance = new FeedbackResponsesLogic();

    private static final FeedbackResponsesDb frDb = new FeedbackResponsesDb();

    private static final FeedbackQuestionsLogic fqLogic = FeedbackQuestionsLogic.inst();
    private static final FeedbackResponseCommentsLogic frcLogic = FeedbackResponseCommentsLogic.inst();
    private static final FeedbackSessionsLogic fsLogic = FeedbackSessionsLogic.inst();
    private static final StudentsLogic studentsLogic = StudentsLogic.inst();

    private FeedbackResponsesLogic() {
        // prevent initialization
    }

    public static FeedbackResponsesLogic inst() {
        return instance;
    }

    /**
     * Gets a set of giver identifiers that has at least one response under a feedback session.
     */
    public Set<String> getGiverSetThatAnswerFeedbackSession(String courseId, String feedbackSessionName) {
        return frDb.getGiverSetThatAnswerFeedbackSession(courseId, feedbackSessionName);
    }

    /**
     * Creates a feedback response.
     *
     * @return created feedback response
     * @throws InvalidParametersException if the response is not valid
     * @throws EntityAlreadyExistsException if the response already exist
     */
    public FeedbackResponseAttributes createFeedbackResponse(FeedbackResponseAttributes fra)
            throws InvalidParametersException, EntityAlreadyExistsException {
        return frDb.createEntity(fra);
    }

    /**
     * Gets a feedback response by its ID.
     */
    public FeedbackResponseAttributes getFeedbackResponse(
            String feedbackResponseId) {
        return frDb.getFeedbackResponse(feedbackResponseId);
    }

    /**
     * Gets a feedback response by its unique key.
     */
    public FeedbackResponseAttributes getFeedbackResponse(
            String feedbackQuestionId, String giverEmail, String recipient) {
        return frDb.getFeedbackResponse(feedbackQuestionId, giverEmail, recipient);
    }

    /**
     * Gets all responses for a session.
     */
    List<FeedbackResponseAttributes> getFeedbackResponsesForSession(
            String feedbackSessionName, String courseId) {
        return frDb.getFeedbackResponsesForSession(feedbackSessionName, courseId);
    }

    /**
     * Gets all responses given to/from a section in a feedback session in a course.
     *
     * @param feedbackSessionName the name if the session
     * @param courseId the course ID of the session
     * @param section if null, will retrieve all responses in the session
     * @return a list of responses
     */
    public List<FeedbackResponseAttributes> getFeedbackResponsesForSessionInSection(
            String feedbackSessionName, String courseId, @Nullable String section) {
        if (section == null) {
            return getFeedbackResponsesForSession(feedbackSessionName, courseId);
        }
        return frDb.getFeedbackResponsesForSessionInSection(feedbackSessionName, courseId, section);
    }

    /**
     * Gets all responses for a question.
     */
    public List<FeedbackResponseAttributes> getFeedbackResponsesForQuestion(String feedbackQuestionId) {
        return frDb.getFeedbackResponsesForQuestion(feedbackQuestionId);
    }

    /**
     * Checks whether there are responses for a question.
     */
    public boolean areThereResponsesForQuestion(String feedbackQuestionId) {
        return frDb.areThereResponsesForQuestion(feedbackQuestionId);
    }

    /**
     * Gets all responses given to/from a section for a question.
     *
     * @param feedbackQuestionId the ID of the question
     * @param section if null, will retrieve all responses for the question
     * @return a list of responses
     */
    public List<FeedbackResponseAttributes> getFeedbackResponsesForQuestionInSection(
            String feedbackQuestionId, @Nullable String section) {
        if (section == null) {
            return getFeedbackResponsesForQuestion(feedbackQuestionId);
        }
        return frDb.getFeedbackResponsesForQuestionInSection(feedbackQuestionId, section);
    }

    /**
     * Gets all responses given by an user for a question.
     */
    public List<FeedbackResponseAttributes> getFeedbackResponsesFromGiverForQuestion(
            String feedbackQuestionId, String userEmail) {
        return frDb.getFeedbackResponsesFromGiverForQuestion(feedbackQuestionId, userEmail);
    }

    /**
     * Gets all responses received by an user for a question.
     */
    private List<FeedbackResponseAttributes> getFeedbackResponsesForReceiverForQuestion(
            String feedbackQuestionId, String userEmail) {
        return frDb.getFeedbackResponsesForReceiverForQuestion(feedbackQuestionId, userEmail);
    }

    /**
     * Checks whether a giver has responded a session.
     */
    public boolean hasGiverRespondedForSession(String userEmail, String feedbackSessionName, String courseId) {

        return frDb.hasResponsesFromGiverInSession(userEmail, feedbackSessionName, courseId);
    }

    /**
     * Gets all responses received by an user for a course.
     */
    public List<FeedbackResponseAttributes> getFeedbackResponsesForReceiverForCourse(
            String courseId, String userEmail) {
        return frDb.getFeedbackResponsesForReceiverForCourse(courseId, userEmail);
    }

    /**
     * Gets all responses given by an user for a course.
     */
    public List<FeedbackResponseAttributes> getFeedbackResponsesFromGiverForCourse(
            String courseId, String userEmail) {
        return frDb.getFeedbackResponsesFromGiverForCourse(courseId, userEmail);
    }

    /**
     * Get existing feedback responses from student or his team for the given
     * question.
     */
    public List<FeedbackResponseAttributes> getFeedbackResponsesFromStudentOrTeamForQuestion(
            FeedbackQuestionAttributes question, StudentAttributes student) {
        if (question.giverType == FeedbackParticipantType.TEAMS) {
            return getFeedbackResponsesFromTeamForQuestion(
                    question.getId(), question.courseId, student.team, null);
        }
        return frDb.getFeedbackResponsesFromGiverForQuestion(question.getId(), student.email);
    }

    /**
     * Checks whether the giver name of a response is visible to an user.
     */
    public boolean isNameVisibleToUser(
            FeedbackQuestionAttributes question,
            FeedbackResponseAttributes response,
            String userEmail,
            UserRole role, boolean isGiverName, CourseRoster roster) {

        if (question == null) {
            return false;
        }

        // Early return if user is giver
        if (question.giverType == FeedbackParticipantType.TEAMS) {
            // if response is given by team, then anyone in the team can see the response
            // The second check is used to accommodate legacy data where team giver is a student email
            if (roster.isStudentInTeam(userEmail, response.giver)
                    || roster.isStudentsInSameTeam(userEmail, response.giver)) {
                return true;
            }
        } else {
            if (response.giver.equals(userEmail)) {
                return true;
            }
        }

        return isFeedbackParticipantNameVisibleToUser(question, response,
                userEmail, role, isGiverName, roster);
    }

    private boolean isFeedbackParticipantNameVisibleToUser(
            FeedbackQuestionAttributes question, FeedbackResponseAttributes response,
            String userEmail, UserRole role, boolean isGiverName, CourseRoster roster) {
        List<FeedbackParticipantType> showNameTo = isGiverName
                                                 ? question.showGiverNameTo
                                                 : question.showRecipientNameTo;
        for (FeedbackParticipantType type : showNameTo) {
            switch (type) {
            case INSTRUCTORS:
                if (roster.getInstructorForEmail(userEmail) != null && role == UserRole.INSTRUCTOR) {
                    return true;
                }
                break;
            case OWN_TEAM_MEMBERS:
            case OWN_TEAM_MEMBERS_INCLUDING_SELF:
                // Refers to Giver's Team Members
                if (roster.isStudentsInSameTeam(response.giver, userEmail)) {
                    return true;
                }
                break;
            case RECEIVER:
                // Response to team
                if (question.recipientType.isTeam()) {
                    if (roster.isStudentInTeam(userEmail, response.recipient)) {
                        // this is a team name
                        return true;
                    }
                    break;
                    // Response to individual
                } else if (response.recipient.equals(userEmail)) {
                    return true;
                } else {
                    break;
                }
            case RECEIVER_TEAM_MEMBERS:
                // Response to team; recipient = teamName
                if (question.recipientType.isTeam()) {
                    if (roster.isStudentInTeam(userEmail, response.recipient)) {
                        // this is a team name
                        return true;
                    }
                    break;
                } else if (roster.isStudentsInSameTeam(response.recipient, userEmail)) {
                    // Response to individual
                    return true;
                }
                break;
            case STUDENTS:
                if (roster.isStudentInCourse(userEmail)) {
                    return true;
                }
                break;
            default:
                Assumption.fail("Invalid FeedbackParticipantType for showNameTo in "
                                + "FeedbackResponseLogic.isFeedbackParticipantNameVisibleToUser()");
                break;
            }
        }
        return false;
    }

    /**
     * Returns true if the responses of the question are visible to students.
     */
    public boolean isResponseOfFeedbackQuestionVisibleToStudent(FeedbackQuestionAttributes question) {
        if (question.isResponseVisibleTo(FeedbackParticipantType.STUDENTS)) {
            return true;
        }
        boolean isStudentRecipientType =
                   question.recipientType.equals(FeedbackParticipantType.STUDENTS)
                || question.recipientType.equals(FeedbackParticipantType.OWN_TEAM_MEMBERS)
                || question.recipientType.equals(FeedbackParticipantType.OWN_TEAM_MEMBERS_INCLUDING_SELF)
                || question.recipientType.equals(FeedbackParticipantType.GIVER)
                   && question.giverType.equals(FeedbackParticipantType.STUDENTS);

        if ((isStudentRecipientType || question.recipientType.isTeam())
                && question.isResponseVisibleTo(FeedbackParticipantType.RECEIVER)) {
            return true;
        }
        if (question.giverType == FeedbackParticipantType.TEAMS
                || question.isResponseVisibleTo(FeedbackParticipantType.OWN_TEAM_MEMBERS)) {
            return true;
        }
        return question.isResponseVisibleTo(FeedbackParticipantType.RECEIVER_TEAM_MEMBERS);
    }

    /**
     * Checks whether there are responses for a course.
     */
    public boolean hasResponsesForCourse(String courseId) {
        return frDb.hasFeedbackResponseEntitiesForCourse(courseId);
    }

    /**
     * Updates a feedback response by {@link FeedbackResponseAttributes.UpdateOptions}.
     *
     * <p>Cascade updates its associated feedback response comment
     * (e.g. associated response ID, giverSection and recipientSection).
     *
     * <p>If the giver/recipient field is changed, the response is updated by recreating the response
     * as question-giver-recipient is the primary key.
     *
     * @return updated feedback response
     * @throws InvalidParametersException if attributes to update are not valid
     * @throws EntityDoesNotExistException if the comment cannot be found
     * @throws EntityAlreadyExistsException if the response cannot be updated
     *         by recreation because of an existent response
     */
    public FeedbackResponseAttributes updateFeedbackResponseCascade(FeedbackResponseAttributes.UpdateOptions updateOptions)
            throws InvalidParametersException, EntityDoesNotExistException, EntityAlreadyExistsException {

        FeedbackResponseAttributes oldResponse = frDb.getFeedbackResponse(updateOptions.getFeedbackResponseId());
        FeedbackResponseAttributes newResponse = frDb.updateFeedbackResponse(updateOptions);

        boolean isResponseIdChanged = !oldResponse.getId().equals(newResponse.getId());
        boolean isGiverSectionChanged = !oldResponse.giverSection.equals(newResponse.giverSection);
        boolean isRecipientSectionChanged = !oldResponse.recipientSection.equals(newResponse.recipientSection);

        if (isResponseIdChanged || isGiverSectionChanged || isRecipientSectionChanged) {
            List<FeedbackResponseCommentAttributes> responseComments =
                    frcLogic.getFeedbackResponseCommentForResponse(oldResponse.getId());
            for (FeedbackResponseCommentAttributes responseComment : responseComments) {
                FeedbackResponseCommentAttributes.UpdateOptions.Builder updateOptionsBuilder =
                        FeedbackResponseCommentAttributes.updateOptionsBuilder(responseComment.getId());

                if (isResponseIdChanged) {
                    updateOptionsBuilder.withFeedbackResponseId(newResponse.getId());
                }

                if (isGiverSectionChanged) {
                    updateOptionsBuilder.withGiverSection(newResponse.giverSection);
                }

                if (isRecipientSectionChanged) {
                    updateOptionsBuilder.withReceiverSection(newResponse.recipientSection);
                }

                frcLogic.updateFeedbackResponseComment(updateOptionsBuilder.build());
            }
        }

        return newResponse;
    }

    /**
     * Updates responses for a student when his team changes.
     *
     * <p>This is done by deleting responses that are no longer relevant to him in his new team.
     */
    public void updateFeedbackResponsesForChangingTeam(
            String courseId, String userEmail, String oldTeam, String newTeam) {
        FeedbackQuestionAttributes question;
        // key is feedback session name, value is a set of student emails that need respondents update
        Map<String, Set<String>> studentEmailsNeedRespondentsUpdate = new HashMap<>();
        // key is feedback session name, value is a set of student emails that need respondents update
        Map<String, Set<String>> instructorEmailsNeedRespondentsUpdate = new HashMap<>();

        // deletes all responses given by the user to team members or given by the user as a representative of a team.
        List<FeedbackResponseAttributes> responsesFromUser =
                getFeedbackResponsesFromGiverForCourse(courseId, userEmail);
        for (FeedbackResponseAttributes response : responsesFromUser) {
            question = fqLogic.getFeedbackQuestion(response.feedbackQuestionId);
            if (question.giverType == FeedbackParticipantType.TEAMS
                    || isRecipientTypeTeamMembers(question)) {
                deleteFeedbackResponseCascade(response.getId());

                studentEmailsNeedRespondentsUpdate
                        .computeIfAbsent(response.feedbackSessionName, key -> new HashSet<>())
                        .add(response.giver);
            }
        }

        // Deletes all responses given by other team members to the user.
        List<FeedbackResponseAttributes> responsesToUser =
                getFeedbackResponsesForReceiverForCourse(courseId, userEmail);
        for (FeedbackResponseAttributes response : responsesToUser) {
            question = fqLogic.getFeedbackQuestion(response.feedbackQuestionId);
            if (isRecipientTypeTeamMembers(question)) {
                deleteFeedbackResponseCascade(response.getId());

                if (question.getGiverType() == FeedbackParticipantType.STUDENTS) {
                    studentEmailsNeedRespondentsUpdate
                            .computeIfAbsent(response.feedbackSessionName, key -> new HashSet<>())
                            .add(response.giver);
                }
            }
        }

        boolean isOldTeamEmpty = studentsLogic.getStudentsForTeam(oldTeam, courseId).isEmpty();
        if (isOldTeamEmpty) {
            deleteResponsesInvolvedTeam(courseId, oldTeam,
                    studentEmailsNeedRespondentsUpdate, instructorEmailsNeedRespondentsUpdate);
        }

        // update respondents
        studentEmailsNeedRespondentsUpdate.forEach((sessionName, emails) -> {
            deleteStudentFromRespondentsIfNecessary(courseId, sessionName, emails.toArray(new String[0]));
        });
        instructorEmailsNeedRespondentsUpdate.forEach((sessionName, emails) -> {
            deleteInstructorFromRespondentsIfNecessary(courseId, sessionName, emails.toArray(new String[0]));
        });
    }

    /**
     * Deletes all feedback response involved a team.
     *
     * @param courseId the course id
     * @param teamName the team name
     * @param studentEmailsNeedRespondentsUpdate map to keep track of deleted response for student respondents update
     * @param instructorEmailsNeedRespondentsUpdate map to keep track of deleted response for instructor respondents update
     */
    private void deleteResponsesInvolvedTeam(String courseId, String teamName,
                                             Map<String, Set<String>> studentEmailsNeedRespondentsUpdate,
                                             Map<String, Set<String>> instructorEmailsNeedRespondentsUpdate) {
        // Deletes all responses given by the team.
        List<FeedbackResponseAttributes> responsesFromOldTeam =
                getFeedbackResponsesFromGiverForCourse(courseId, teamName);
        for (FeedbackResponseAttributes response : responsesFromOldTeam) {
            deleteFeedbackResponseCascade(response.getId());
        }

        // Deletes all responses received by the team.
        List<FeedbackResponseAttributes> responsesToOldTeam =
                getFeedbackResponsesForReceiverForCourse(courseId, teamName);
        for (FeedbackResponseAttributes response : responsesToOldTeam) {
            deleteFeedbackResponseCascade(response.getId());

            FeedbackQuestionAttributes question = fqLogic.getFeedbackQuestion(response.feedbackQuestionId);
            if (question.getGiverType() == FeedbackParticipantType.INSTRUCTORS
                    || question.getGiverType() == FeedbackParticipantType.SELF) {
                instructorEmailsNeedRespondentsUpdate
                        .computeIfAbsent(response.feedbackSessionName, key -> new HashSet<>())
                        .add(response.giver);
            }
            if (question.getGiverType() == FeedbackParticipantType.STUDENTS) {
                studentEmailsNeedRespondentsUpdate
                        .computeIfAbsent(response.feedbackSessionName, key -> new HashSet<>())
                        .add(response.giver);
            }
        }
    }

    /**
     * Updates responses for a student when his section changes.
     */
    public void updateFeedbackResponsesForChangingSection(
            String courseId, String userEmail, String oldSection, String newSection)
            throws EntityDoesNotExistException, InvalidParametersException {
        updateSectionOfResponsesFromUser(courseId, userEmail, newSection);
        updateSectionOfResponsesToUser(courseId, userEmail, newSection);
    }

    private void updateSectionOfResponsesToUser(String courseId, String userEmail, String newSection)
            throws InvalidParametersException, EntityDoesNotExistException {
        List<FeedbackResponseAttributes> responsesToUser =
                getFeedbackResponsesForReceiverForCourse(courseId, userEmail);

        for (FeedbackResponseAttributes response : responsesToUser) {
            try {
                frDb.updateFeedbackResponse(
                        FeedbackResponseAttributes.updateOptionsBuilder(response.getId())
                                .withRecipientSection(newSection)
                                .build());
            } catch (EntityAlreadyExistsException e) {
                Assumption.fail("Not possible to trigger recreating of response");
            }
            frcLogic.updateFeedbackResponseCommentsForResponse(response.getId());
        }
    }

    private void updateSectionOfResponsesFromUser(String courseId, String userEmail, String newSection)
            throws InvalidParametersException, EntityDoesNotExistException {
        List<FeedbackResponseAttributes> responsesFromUser =
                getFeedbackResponsesFromGiverForCourse(courseId, userEmail);

        for (FeedbackResponseAttributes response : responsesFromUser) {
            try {
                frDb.updateFeedbackResponse(
                        FeedbackResponseAttributes.updateOptionsBuilder(response.getId())
                                .withGiverSection(newSection)
                                .build());
            } catch (EntityAlreadyExistsException e) {
                Assumption.fail("Not possible to trigger recreating of response");
            }
            frcLogic.updateFeedbackResponseCommentsForResponse(response.getId());
        }
    }

    /**
     * Delete student from session respondents if he does not have any responses for the session.
     */
    private void deleteStudentFromRespondentsIfNecessary(String courseId, String sessionName, String... studentEmails) {
        for (String studentEmail : studentEmails) {
            try {
                if (!hasGiverRespondedForSession(studentEmail, sessionName, courseId)) {
                    fsLogic.deleteStudentFromRespondentList(studentEmail, sessionName, courseId);
                }
            } catch (EntityDoesNotExistException | InvalidParametersException e) {
                log.warning(String.format(
                        "Cannot adjust response rate for student %s course %s feedbackSession %s because of %s",
                        studentEmail, courseId, sessionName, TeammatesException.toStringWithStackTrace(e)));
            }
        }
    }

    /**
     * Delete instructor from session respondents if he does not have any responses for the session.
     */
    private void deleteInstructorFromRespondentsIfNecessary(
            String courseId, String sessionName, String... instructorEmails) {
        for (String instructorEmail : instructorEmails) {
            try {
                if (!hasGiverRespondedForSession(instructorEmail, sessionName, courseId)) {
                    fsLogic.deleteInstructorRespondent(instructorEmail, sessionName, courseId);
                }
            } catch (EntityDoesNotExistException | InvalidParametersException e) {
                log.warning(String.format(
                        "Cannot adjust response rate for instructor %s course %s feedbackSession %s because of %s",
                        instructorEmail, courseId, sessionName, TeammatesException.toStringWithStackTrace(e)));
            }
        }
    }

    private boolean isRecipientTypeTeamMembers(FeedbackQuestionAttributes question) {
        return question.recipientType == FeedbackParticipantType.OWN_TEAM_MEMBERS
               || question.recipientType == FeedbackParticipantType.OWN_TEAM_MEMBERS_INCLUDING_SELF;
    }

    /**
     * Updates responses for a student when his email changes.
     */
    public void updateFeedbackResponsesForChangingEmail(
            String courseId, String oldEmail, String newEmail)
            throws InvalidParametersException, EntityDoesNotExistException {

        List<FeedbackResponseAttributes> responsesFromUser =
                getFeedbackResponsesFromGiverForCourse(courseId, oldEmail);

        for (FeedbackResponseAttributes response : responsesFromUser) {
            try {
                updateFeedbackResponseCascade(
                        FeedbackResponseAttributes.updateOptionsBuilder(response.getId())
                                .withGiver(newEmail)
                                .build());
                frcLogic.updateFeedbackResponseCommentsEmails(courseId, oldEmail, newEmail);
            } catch (EntityAlreadyExistsException e) {
                Assumption
                        .fail("Feedback response failed to update successfully "
                            + "as email was already in use.");
            }
        }

        List<FeedbackResponseAttributes> responsesToUser =
                getFeedbackResponsesForReceiverForCourse(courseId, oldEmail);

        for (FeedbackResponseAttributes response : responsesToUser) {
            try {
                updateFeedbackResponseCascade(
                        FeedbackResponseAttributes.updateOptionsBuilder(response.getId())
                                .withRecipient(newEmail)
                                .build());
            } catch (EntityAlreadyExistsException e) {
                Assumption
                        .fail("Feedback response failed to update successfully "
                            + "as email was already in use.");
            }
        }
    }

    /**
     * Deletes responses using {@link AttributesDeletionQuery}.
     */
    public void deleteFeedbackResponses(AttributesDeletionQuery query) {
        frDb.deleteFeedbackResponses(query);
    }

    /**
     * Deletes a feedback response cascade its associated comments.
     *
     * <p>The respondent lists will NOT be updated.
     */
    public void deleteFeedbackResponseCascade(String responseId) {
        frcLogic.deleteFeedbackResponseComments(
                AttributesDeletionQuery.builder()
                        .withResponseId(responseId)
                        .build());
        frDb.deleteFeedbackResponse(responseId);
    }

    /**
     * Deletes all feedback responses of a question cascade its associated comments.
     *
     * <p>The respondent lists will also be updated.
     */
    public void deleteFeedbackResponsesForQuestionCascade(String feedbackQuestionId) {
        List<FeedbackResponseAttributes> responsesForQuestion =
                getFeedbackResponsesForQuestion(feedbackQuestionId);

        Set<String> emails = new HashSet<>();
        // record all giver and prepare respondents update
        for (FeedbackResponseAttributes response : responsesForQuestion) {
            emails.add(response.giver);
        }

        // delete all responses, comments of the question
        AttributesDeletionQuery query = AttributesDeletionQuery.builder()
                .withQuestionId(feedbackQuestionId)
                .build();
        deleteFeedbackResponses(query);
        frcLogic.deleteFeedbackResponseComments(query);

        FeedbackQuestionAttributes question = fqLogic.getFeedbackQuestion(feedbackQuestionId);
        if (question.getGiverType() == FeedbackParticipantType.SELF
                || question.getGiverType() == FeedbackParticipantType.INSTRUCTORS) {
            deleteInstructorFromRespondentsIfNecessary(
                    question.getCourseId(), question.getFeedbackSessionName(), emails.toArray(new String[0]));
        }
        if (question.getGiverType() == FeedbackParticipantType.STUDENTS) {
            deleteStudentFromRespondentsIfNecessary(
                    question.getCourseId(), question.getFeedbackSessionName(), emails.toArray(new String[0]));
        }
    }

    /**
     * Deletes all feedback responses involved a student cascade its associated comments.
     *
     * <p>The respondent lists will also be updated.
     */
    public void deleteFeedbackResponsesInvolvedStudentOfCourseCascade(String courseId, String studentEmail) {
        // key is feedback session name, value is a set of student emails that need respondents update
        Map<String, Set<String>> studentEmailsNeedRespondentsUpdate = new HashMap<>();
        // key is feedback session name, value is a set of student emails that need respondents update
        Map<String, Set<String>> instructorEmailsNeedRespondentsUpdate = new HashMap<>();

        deleteFeedbackResponsesInvolvedEntityOfCourseCascade(courseId, studentEmail,
                studentEmailsNeedRespondentsUpdate, instructorEmailsNeedRespondentsUpdate);

        // update respondents
        studentEmailsNeedRespondentsUpdate.forEach((sessionName, emails) -> {
            deleteStudentFromRespondentsIfNecessary(courseId, sessionName, emails.toArray(new String[0]));
        });
        instructorEmailsNeedRespondentsUpdate.forEach((sessionName, emails) -> {
            deleteInstructorFromRespondentsIfNecessary(courseId, sessionName, emails.toArray(new String[0]));
        });

        fsLogic.deleteStudentFromRespondentsList(courseId, studentEmail);
    }

    /**
     * Deletes all feedback responses involved a team cascade its associated comments.
     */
    public void deleteFeedbackResponsesInvolvedTeamOfCourseCascade(String courseId, String teamName) {
        // key is feedback session name, value is a set of student emails that need respondents update
        Map<String, Set<String>> studentEmailsNeedRespondentsUpdate = new HashMap<>();
        // key is feedback session name, value is a set of student emails that need respondents update
        Map<String, Set<String>> instructorEmailsNeedRespondentsUpdate = new HashMap<>();

        deleteResponsesInvolvedTeam(courseId, teamName,
                studentEmailsNeedRespondentsUpdate, instructorEmailsNeedRespondentsUpdate);

        // update respondents
        studentEmailsNeedRespondentsUpdate.forEach((sessionName, emails) -> {
            deleteStudentFromRespondentsIfNecessary(courseId, sessionName, emails.toArray(new String[0]));
        });
        instructorEmailsNeedRespondentsUpdate.forEach((sessionName, emails) -> {
            deleteInstructorFromRespondentsIfNecessary(courseId, sessionName, emails.toArray(new String[0]));
        });
    }

    /**
     * Deletes all feedback responses involved an instructor cascade its associated comments.
     *
     * <p>The respondent lists will also be updated.
     */
    public void deleteFeedbackResponsesInvolvedInstructorOfCourseCascade(String courseId, String instructorEmail) {
        // key is feedback session name, value is a set of student emails that need respondents update
        Map<String, Set<String>> studentEmailsNeedRespondentsUpdate = new HashMap<>();
        // key is feedback session name, value is a set of student emails that need respondents update
        Map<String, Set<String>> instructorEmailsNeedRespondentsUpdate = new HashMap<>();

        deleteFeedbackResponsesInvolvedEntityOfCourseCascade(courseId, instructorEmail,
                studentEmailsNeedRespondentsUpdate, instructorEmailsNeedRespondentsUpdate);

        // update respondents
        studentEmailsNeedRespondentsUpdate.forEach((sessionName, emails) -> {
            deleteStudentFromRespondentsIfNecessary(courseId, sessionName, emails.toArray(new String[0]));
        });
        instructorEmailsNeedRespondentsUpdate.forEach((sessionName, emails) -> {
            deleteInstructorFromRespondentsIfNecessary(courseId, sessionName, emails.toArray(new String[0]));
        });

        fsLogic.deleteInstructorFromRespondentsList(courseId, instructorEmail);
    }

    /**
     * Deletes all feedback responses involved an entity cascade its associated comments.
     *
     * @param courseId the course id
     * @param entityEmail the entity email
     * @param studentEmailsNeedRespondentsUpdate map to keep track of deleted responses for student respondents update
     * @param instructorEmailsNeedRespondentsUpdate map to keep track of deleted responses for instructor respondents update
     */
    private void deleteFeedbackResponsesInvolvedEntityOfCourseCascade(
            String courseId, String entityEmail,
            Map<String, Set<String>> studentEmailsNeedRespondentsUpdate,
            Map<String, Set<String>> instructorEmailsNeedRespondentsUpdate) {
        // delete responses from the entity
        List<FeedbackResponseAttributes> responsesFromStudent =
                getFeedbackResponsesFromGiverForCourse(courseId, entityEmail);
        for (FeedbackResponseAttributes response : responsesFromStudent) {
            deleteFeedbackResponseCascade(response.getId());
        }

        // delete responses to the entity
        List<FeedbackResponseAttributes> responsesToStudent =
                getFeedbackResponsesForReceiverForCourse(courseId, entityEmail);
        FeedbackQuestionAttributes question;
        for (FeedbackResponseAttributes response : responsesToStudent) {
            question = fqLogic.getFeedbackQuestion(response.feedbackQuestionId);
            deleteFeedbackResponseCascade(response.getId());

            if (question.getGiverType() == FeedbackParticipantType.STUDENTS) {
                studentEmailsNeedRespondentsUpdate
                        .computeIfAbsent(response.feedbackSessionName, key -> new HashSet<>())
                        .add(response.giver);
            }
            if (question.getGiverType() == FeedbackParticipantType.INSTRUCTORS
                    || question.getGiverType() == FeedbackParticipantType.SELF) {
                instructorEmailsNeedRespondentsUpdate
                        .computeIfAbsent(response.feedbackSessionName, key -> new HashSet<>())
                        .add(response.giver);
            }
        }
    }

    private List<FeedbackResponseAttributes> getFeedbackResponsesFromTeamForQuestion(
            String feedbackQuestionId, String courseId, String teamName, @Nullable CourseRoster courseRoster) {

        List<FeedbackResponseAttributes> responses = new ArrayList<>();
        List<StudentAttributes> studentsInTeam = courseRoster == null
                ? studentsLogic.getStudentsForTeam(teamName, courseId) : courseRoster.getTeamToMembersTable().get(teamName);

        for (StudentAttributes student : studentsInTeam) {
            responses.addAll(frDb.getFeedbackResponsesFromGiverForQuestion(
                    feedbackQuestionId, student.email));
        }

        responses.addAll(frDb.getFeedbackResponsesFromGiverForQuestion(
                                        feedbackQuestionId, teamName));

        return responses;
    }

    /**
     * Returns viewable feedback responses for a student.
     */
    public List<FeedbackResponseAttributes> getViewableFeedbackResponsesForStudentForQuestion(
            FeedbackQuestionAttributes question, StudentAttributes student, CourseRoster courseRoster) {
        UniqueResponsesSet viewableResponses = new UniqueResponsesSet();

        // Add responses that the student submitted himself
        viewableResponses.addNewResponses(
                getFeedbackResponsesFromGiverForQuestion(question.getFeedbackQuestionId(), student.getEmail())
        );

        // Add responses that user is a receiver of when question is visible to
        // receiver.
        if (question.isResponseVisibleTo(FeedbackParticipantType.RECEIVER)) {
            viewableResponses.addNewResponses(
                    getFeedbackResponsesForReceiverForQuestion(question.getFeedbackQuestionId(), student.getEmail())
            );
        }

        if (question.isResponseVisibleTo(FeedbackParticipantType.STUDENTS)) {
            viewableResponses.addNewResponses(getFeedbackResponsesForQuestion(question.getId()));

            // Early return as STUDENTS covers all cases below.
            return viewableResponses.getResponses();
        }

        if (question.getRecipientType().isTeam()
                && question.isResponseVisibleTo(FeedbackParticipantType.RECEIVER)) {
            viewableResponses.addNewResponses(
                    getFeedbackResponsesForReceiverForQuestion(question.getId(), student.getTeam())
            );
        }

        if (question.getGiverType() == FeedbackParticipantType.TEAMS
                || question.isResponseVisibleTo(FeedbackParticipantType.OWN_TEAM_MEMBERS)) {
            viewableResponses.addNewResponses(
                    getFeedbackResponsesFromTeamForQuestion(
                            question.getId(), question.getCourseId(), student.getTeam(), courseRoster));
        }

        if (question.isResponseVisibleTo(FeedbackParticipantType.RECEIVER_TEAM_MEMBERS)) {
            for (StudentAttributes studentInTeam : courseRoster.getTeamToMembersTable().get(student.getTeam())) {
                if (studentInTeam.getEmail().equals(student.getEmail())) {
                    continue;
                }
                List<FeedbackResponseAttributes> responses =
                        frDb.getFeedbackResponsesForReceiverForQuestion(question.getId(), studentInTeam.getEmail());
                viewableResponses.addNewResponses(responses);
            }
        }

        return viewableResponses.getResponses();
    }

    /**
     * Set contains only unique response.
     */
    private static class UniqueResponsesSet {

        private final Set<String> responseIds;
        private final List<FeedbackResponseAttributes> responses;

        private UniqueResponsesSet() {
            responseIds = new HashSet<>();
            responses = new ArrayList<>();
        }

        private void addNewResponses(Collection<FeedbackResponseAttributes> newResponses) {
            newResponses.forEach(this::addNewResponse);
        }

        private void addNewResponse(FeedbackResponseAttributes newResponse) {
            if (responseIds.contains(newResponse.getId())) {
                return;
            }
            responseIds.add(newResponse.getId());
            responses.add(newResponse);
        }

        private List<FeedbackResponseAttributes> getResponses() {
            return responses;
        }
    }
}
