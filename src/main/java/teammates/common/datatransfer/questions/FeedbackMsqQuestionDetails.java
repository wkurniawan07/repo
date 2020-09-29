package teammates.common.datatransfer.questions;

import java.util.ArrayList;
import java.util.List;

import teammates.common.datatransfer.FeedbackParticipantType;
import teammates.common.datatransfer.attributes.FeedbackQuestionAttributes;
import teammates.common.util.Const;

public class FeedbackMsqQuestionDetails extends FeedbackQuestionDetails {
    private List<String> msqChoices;
    private boolean otherEnabled;
    private boolean hasAssignedWeights;
    private List<Double> msqWeights;
    private double msqOtherWeight;
    private FeedbackParticipantType generateOptionsFor;
    private int maxSelectableChoices;
    private int minSelectableChoices;
    private transient int numOfGeneratedMsqChoices;

    public FeedbackMsqQuestionDetails() {
        super(FeedbackQuestionType.MSQ);

        this.msqChoices = new ArrayList<>();
        this.otherEnabled = false;
        this.generateOptionsFor = FeedbackParticipantType.NONE;
        this.maxSelectableChoices = Integer.MIN_VALUE;
        this.minSelectableChoices = Integer.MIN_VALUE;
        this.hasAssignedWeights = false;
        this.msqWeights = new ArrayList<>();
        this.msqOtherWeight = 0;
    }

    @Override
    public boolean shouldChangesRequireResponseDeletion(FeedbackQuestionDetails newDetails) {
        FeedbackMsqQuestionDetails newMsqDetails = (FeedbackMsqQuestionDetails) newDetails;

        if (this.msqChoices.size() != newMsqDetails.msqChoices.size()
                || !this.msqChoices.containsAll(newMsqDetails.msqChoices)
                || !newMsqDetails.msqChoices.containsAll(this.msqChoices)) {
            return true;
        }

        if (this.generateOptionsFor != newMsqDetails.generateOptionsFor) {
            return true;
        }

        if (this.maxSelectableChoices == Integer.MIN_VALUE && newMsqDetails.maxSelectableChoices != Integer.MIN_VALUE) {
            // Delete responses if max selectable restriction is newly added
            return true;
        }

        if (this.minSelectableChoices == Integer.MIN_VALUE && newMsqDetails.minSelectableChoices != Integer.MIN_VALUE) {
            // Delete responses if min selectable restriction is newly added
            return true;
        }

        if (this.minSelectableChoices != Integer.MIN_VALUE && newMsqDetails.minSelectableChoices != Integer.MIN_VALUE
                && this.minSelectableChoices < newMsqDetails.minSelectableChoices) {
            // A more strict min selectable choices restriction is placed
            return true;
        }

        if (this.maxSelectableChoices != Integer.MIN_VALUE && newMsqDetails.maxSelectableChoices != Integer.MIN_VALUE
                && this.maxSelectableChoices > newMsqDetails.maxSelectableChoices) {
            // A more strict max selectable choices restriction is placed
            return true;
        }

        return this.otherEnabled != newMsqDetails.otherEnabled;
    }

    @Override
    public List<String> validateQuestionDetails() {
        List<String> errors = new ArrayList<>();
        if (generateOptionsFor == FeedbackParticipantType.NONE) {

            if (msqChoices.size() < Const.FeedbackQuestion.MSQ_MIN_NUM_OF_CHOICES) {
                errors.add(Const.FeedbackQuestion.MSQ_ERROR_NOT_ENOUGH_CHOICES
                           + Const.FeedbackQuestion.MSQ_MIN_NUM_OF_CHOICES + ".");
            }

            // If there are Empty Msq options entered trigger this error
            boolean isEmptyMsqOptionEntered = msqChoices.stream().anyMatch(msqText -> msqText.trim().equals(""));
            if (isEmptyMsqOptionEntered) {
                errors.add(Const.FeedbackQuestion.MSQ_ERROR_EMPTY_MSQ_OPTION);
            }

            // If weights are enabled, number of choices and weights should be same.
            // If a user enters an invalid weight for a valid choice,
            // the msqChoices.size() will be greater than msqWeights.size(), in that case
            // trigger this error.
            if (hasAssignedWeights && msqChoices.size() != msqWeights.size()) {
                errors.add(Const.FeedbackQuestion.MSQ_ERROR_INVALID_WEIGHT);
            }

            // If weights are not enabled, but weight list is not empty or otherWeight is not 0
            // In that case, trigger this error.
            if (!hasAssignedWeights && (!msqWeights.isEmpty() || msqOtherWeight != 0)) {
                errors.add(Const.FeedbackQuestion.MSQ_ERROR_INVALID_WEIGHT);
            }

            // If weight is enabled, but other option is disabled, and msqOtherWeight is not 0
            // In that case, trigger this error.
            if (hasAssignedWeights && !otherEnabled && msqOtherWeight != 0) {
                errors.add(Const.FeedbackQuestion.MSQ_ERROR_INVALID_WEIGHT);
            }

            // If weights are negative, trigger this error.
            if (hasAssignedWeights && !msqWeights.isEmpty()) {
                msqWeights.stream()
                        .filter(weight -> weight < 0)
                        .forEach(weight -> errors.add(Const.FeedbackQuestion.MSQ_ERROR_INVALID_WEIGHT));
            }

            // If 'Other' option is enabled, and other weight has negative value,
            // trigger this error.
            if (hasAssignedWeights && otherEnabled && msqOtherWeight < 0) {
                errors.add(Const.FeedbackQuestion.MSQ_ERROR_INVALID_WEIGHT);
            }

            //If there are duplicate mcq options trigger this error
            boolean isDuplicateOptionsEntered = msqChoices.stream().map(String::trim).distinct().count()
                                                != msqChoices.size();
            if (isDuplicateOptionsEntered) {
                errors.add(Const.FeedbackQuestion.MSQ_ERROR_DUPLICATE_MSQ_OPTION);
            }
        }

        boolean isMaxSelectableChoicesEnabled = maxSelectableChoices != Integer.MIN_VALUE;
        boolean isMinSelectableChoicesEnabled = minSelectableChoices != Integer.MIN_VALUE;

        int numOfMsqChoices = numOfGeneratedMsqChoices;
        if (generateOptionsFor == FeedbackParticipantType.NONE) {
            numOfMsqChoices = msqChoices.size() + (otherEnabled ? 1 : 0);
        }
        if (isMaxSelectableChoicesEnabled) {
            if (numOfMsqChoices < maxSelectableChoices) {
                errors.add(Const.FeedbackQuestion.MSQ_ERROR_MAX_SELECTABLE_EXCEEDED_TOTAL);
            } else if (maxSelectableChoices < 2) {
                errors.add(Const.FeedbackQuestion.MSQ_ERROR_MIN_FOR_MAX_SELECTABLE_CHOICES);
            }
        }

        if (isMinSelectableChoicesEnabled) {
            if (minSelectableChoices < 1) {
                errors.add(Const.FeedbackQuestion.MSQ_ERROR_MIN_FOR_MIN_SELECTABLE_CHOICES);
            }
            if (minSelectableChoices > numOfMsqChoices) {
                errors.add(Const.FeedbackQuestion.MSQ_ERROR_MIN_SELECTABLE_MORE_THAN_NUM_CHOICES);
            }
        }

        if (isMaxSelectableChoicesEnabled && isMinSelectableChoicesEnabled
                && minSelectableChoices > maxSelectableChoices) {
            errors.add(Const.FeedbackQuestion.MSQ_ERROR_MIN_SELECTABLE_EXCEEDED_MAX_SELECTABLE);
        }

        return errors;
    }

    @Override
    public List<String> validateResponsesDetails(List<FeedbackResponseDetails> responses, int numRecipients) {
        List<String> errors = new ArrayList<>();

        for (FeedbackResponseDetails response : responses) {
            FeedbackMsqResponseDetails details = (FeedbackMsqResponseDetails) response;

            // number of Msq options selected including other option
            int totalChoicesSelected = details.getAnswers().size();
            boolean isMaxSelectableEnabled = maxSelectableChoices != Integer.MIN_VALUE;
            boolean isMinSelectableEnabled = minSelectableChoices != Integer.MIN_VALUE;
            boolean isNoneOfTheAboveOptionEnabled =
                    details.getAnswers().contains(Const.FeedbackQuestion.MSQ_ANSWER_NONE_OF_THE_ABOVE);

            // if other is not enabled and other is selected as an answer trigger this error
            if (details.isOther() && !otherEnabled) {
                errors.add(Const.FeedbackQuestion.MSQ_ERROR_INVALID_OPTION);
            }

            // if other is not chosen while other field is not empty trigger this error
            if (otherEnabled && !details.isOther() && !details.getOtherFieldContent().isEmpty()) {
                errors.add(Const.FeedbackQuestion.MSQ_ERROR_INVALID_OPTION);
            }

            List<String> validChoices = new ArrayList<>(msqChoices);
            if (otherEnabled && details.isOther()) {
                // other field content becomes a valid choice if other is enabled
                validChoices.add(details.getOtherFieldContent());
            }
            // if selected answers are not a part of the Msq option list trigger this error
            boolean isAnswersPartOfChoices = validChoices.containsAll(details.getAnswers());
            if (!isAnswersPartOfChoices && !isNoneOfTheAboveOptionEnabled) {
                errors.add(details.getAnswerString() + " " + Const.FeedbackQuestion.MSQ_ERROR_INVALID_OPTION);
            }

            // if other option is selected but no text is provided trigger this error
            if (details.isOther() && "".equals(details.getOtherFieldContent().trim())) {
                errors.add(Const.FeedbackQuestion.MSQ_ERROR_OTHER_CONTENT_NOT_PROVIDED);
            }

            // if other option is selected but not in the answer list trigger this error
            if (details.isOther() && !details.getAnswers().contains(details.getOtherFieldContent())) {
                errors.add(Const.FeedbackQuestion.MSQ_ERROR_OTHER_CONTENT_NOT_PROVIDED);
            }

            // if total choices selected exceed maximum choices allowed trigger this error
            if (isMaxSelectableEnabled && totalChoicesSelected > maxSelectableChoices) {
                errors.add(Const.FeedbackQuestion.MSQ_ERROR_NUM_SELECTED_MORE_THAN_MAXIMUM + maxSelectableChoices);
            }

            if (isMinSelectableEnabled) {
                // if total choices selected is less than the minimum required choices
                if (totalChoicesSelected < minSelectableChoices) {
                    errors.add(Const.FeedbackQuestion.MSQ_ERROR_NUM_SELECTED_LESS_THAN_MINIMUM + minSelectableChoices);
                }
                // if minimumSelectableChoices is enabled and None of the Above is selected as an answer trigger this error
                if (isNoneOfTheAboveOptionEnabled) {
                    errors.add(Const.FeedbackQuestion.MSQ_ERROR_INVALID_OPTION);
                }
            } else {
                // if none of the above is selected AND other options are selected trigger this error
                if ((details.getAnswers().size() > 1 || details.isOther()) && isNoneOfTheAboveOptionEnabled) {
                    errors.add(Const.FeedbackQuestion.MSQ_ERROR_NONE_OF_THE_ABOVE_ANSWER);
                }
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

    public List<String> getMsqChoices() {
        return msqChoices;
    }

    public void setMsqChoices(List<String> msqChoices) {
        this.msqChoices = msqChoices;
    }

    public boolean isOtherEnabled() {
        return otherEnabled;
    }

    public void setOtherEnabled(boolean otherEnabled) {
        this.otherEnabled = otherEnabled;
    }

    public boolean hasAssignedWeights() {
        return hasAssignedWeights;
    }

    public void setHasAssignedWeights(boolean hasAssignedWeights) {
        this.hasAssignedWeights = hasAssignedWeights;
    }

    public List<Double> getMsqWeights() {
        return msqWeights;
    }

    public void setMsqWeights(List<Double> msqWeights) {
        this.msqWeights = msqWeights;
    }

    public double getMsqOtherWeight() {
        return msqOtherWeight;
    }

    public void setMsqOtherWeight(double msqOtherWeight) {
        this.msqOtherWeight = msqOtherWeight;
    }

    public FeedbackParticipantType getGenerateOptionsFor() {
        return generateOptionsFor;
    }

    public void setGenerateOptionsFor(FeedbackParticipantType generateOptionsFor) {
        this.generateOptionsFor = generateOptionsFor;
    }

    public int getMaxSelectableChoices() {
        return maxSelectableChoices;
    }

    public void setMaxSelectableChoices(int maxSelectableChoices) {
        this.maxSelectableChoices = maxSelectableChoices;
    }

    public int getMinSelectableChoices() {
        return minSelectableChoices;
    }

    public void setMinSelectableChoices(int minSelectableChoices) {
        this.minSelectableChoices = minSelectableChoices;
    }

    public int getNumOfGeneratedMsqChoices() {
        return numOfGeneratedMsqChoices;
    }

    public void setNumOfGeneratedMsqChoices(int numOfGeneratedMsqChoices) {
        this.numOfGeneratedMsqChoices = numOfGeneratedMsqChoices;
    }
}
