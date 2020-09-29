package teammates.ui.webapi;

import org.apache.http.HttpStatus;

import teammates.common.util.Const;

/**
 * Action: deletes an existing account (either student or instructor).
 */
class DeleteAccountAction extends AdminOnlyAction {

    @Override
    JsonResult execute() {
        String instructorId = getNonNullRequestParamValue(Const.ParamsNames.INSTRUCTOR_ID);
        logic.deleteAccountCascade(instructorId);
        return new JsonResult("Account is successfully deleted.", HttpStatus.SC_OK);
    }

}
