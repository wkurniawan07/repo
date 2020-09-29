package teammates.common.datatransfer.questions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import teammates.common.datatransfer.attributes.FeedbackQuestionAttributes;
import teammates.common.util.Const;

public class FeedbackRankOptionsQuestionDetails extends FeedbackRankQuestionDetails {
    public static final transient int MIN_NUM_OF_OPTIONS = 2;
    public static final transient String ERROR_INVALID_MAX_OPTIONS_ENABLED =
            "Max options enabled is invalid";
    public static final transient String ERROR_INVALID_MIN_OPTIONS_ENABLED =
            "Min options enabled is invalid";
    public static final transient String ERROR_MIN_OPTIONS_ENABLED_MORE_THAN_CHOICES =
            "Min options enabled is more than the total choices";
    public static final transient String ERROR_MAX_OPTIONS_ENABLED_MORE_THAN_CHOICES =
            "Max options enabled is more than the total choices";
    public static final transient String ERROR_NOT_ENOUGH_OPTIONS =
            "Too little options for " + Const.FeedbackQuestionTypeNames.RANK_OPTION
            + ". Minimum number of options is: ";
    public static final transient String ERROR_EMPTY_OPTIONS_ENTERED =
            "Empty Rank Options are not allowed";

    private List<String> options;

    public FeedbackRankOptionsQuestionDetails() {
        super(FeedbackQuestionType.RANK_OPTIONS);

        this.options = new ArrayList<>();
    }

    @Override
    public boolean shouldChangesRequireResponseDeletion(FeedbackQuestionDetails newDetails) {
        FeedbackRankOptionsQuestionDetails newRankQuestionDetails = (FeedbackRankOptionsQuestionDetails) newDetails;

        return this.options.size() != newRankQuestionDetails.options.size()
            || !this.options.containsAll(newRankQuestionDetails.options)
            || !newRankQuestionDetails.options.containsAll(this.options)
            || this.minOptionsToBeRanked != newRankQuestionDetails.minOptionsToBeRanked
            || this.maxOptionsToBeRanked != newRankQuestionDetails.maxOptionsToBeRanked;
    }

    @Override
    public List<String> validateQuestionDetails() {
        List<String> errors = new ArrayList<>();

        boolean isEmptyRankOptionEntered = options.stream().anyMatch(optionText -> optionText.trim().equals(""));
        if (isEmptyRankOptionEntered) {
            errors.add(ERROR_EMPTY_OPTIONS_ENTERED);
        }

        boolean isMaxOptionsToBeRankedEnabled = maxOptionsToBeRanked != NO_VALUE;
        boolean isMinOptionsToBeRankedEnabled = minOptionsToBeRanked != NO_VALUE;

        if (isMaxOptionsToBeRankedEnabled) {
            if (maxOptionsToBeRanked < 1) {
                errors.add(ERROR_INVALID_MAX_OPTIONS_ENABLED);
            }
            if (maxOptionsToBeRanked > options.size()) {
                errors.add(ERROR_MAX_OPTIONS_ENABLED_MORE_THAN_CHOICES);
            }
        }

        if (isMinOptionsToBeRankedEnabled) {
            if (minOptionsToBeRanked < 1) {
                errors.add(ERROR_INVALID_MIN_OPTIONS_ENABLED);
            }
            if (minOptionsToBeRanked > options.size()) {
                errors.add(ERROR_MIN_OPTIONS_ENABLED_MORE_THAN_CHOICES);
            }
        }

        if (isMaxOptionsToBeRankedEnabled && isMinOptionsToBeRankedEnabled
                && minOptionsToBeRanked > maxOptionsToBeRanked) {
            errors.add(ERROR_INVALID_MIN_OPTIONS_ENABLED);
        }

        if (options.size() < MIN_NUM_OF_OPTIONS) {
            errors.add(ERROR_NOT_ENOUGH_OPTIONS + MIN_NUM_OF_OPTIONS + ".");
        }

        return errors;
    }

    @Override
    public List<String> validateResponsesDetails(List<FeedbackResponseDetails> responses, int numRecipients) {
        List<String> errors = new ArrayList<>();

        boolean isMinOptionsEnabled = minOptionsToBeRanked != Integer.MIN_VALUE;
        boolean isMaxOptionsEnabled = maxOptionsToBeRanked != Integer.MIN_VALUE;

        for (FeedbackResponseDetails response : responses) {
            FeedbackRankOptionsResponseDetails details = (FeedbackRankOptionsResponseDetails) response;
            List<Integer> filteredAnswers = details.getFilteredSortedAnswerList();
            Set<Integer> set = new HashSet<>(filteredAnswers);
            boolean isAnswerContainsDuplicates = set.size() < filteredAnswers.size();

            // if duplicate ranks are not allowed but have been assigned trigger this error
            if (isAnswerContainsDuplicates && !areDuplicatesAllowed) {
                errors.add("Duplicate Ranks are not allowed.");
            }
            // if number of options ranked is less than the minimum required trigger this error
            if (isMinOptionsEnabled && filteredAnswers.size() < minOptionsToBeRanked) {
                errors.add("You must rank at least " + minOptionsToBeRanked + " options.");
            }
            // if number of options ranked is more than the maximum possible trigger this error
            if (isMaxOptionsEnabled && filteredAnswers.size() > maxOptionsToBeRanked) {
                errors.add("You can rank at most " + maxOptionsToBeRanked + " options.");
            }
            // if rank assigned is invalid trigger this error
            boolean isRankInvalid = filteredAnswers.stream().anyMatch(answer -> answer < 1 || answer > options.size());
            if (isRankInvalid) {
                errors.add("Invalid rank assigned.");
            }
        }

        return errors;
    }

    @Override
    public boolean isFeedbackParticipantCommentsOnResponsesAllowed() {
        return false;
    }

    @Override
    public String validateGiverRecipientVisibility(FeedbackQuestionAttributes feedbackQuestionAttributes) {
        return "";
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }
}
