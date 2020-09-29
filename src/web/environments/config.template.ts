/**
 * Contains some configuration values required to build and run the web application.
 */
export const config: any = {
  /**
   * The application version.
   */
  version: '7.0.0',

  /**
   * The URL of page to be loaded for the account request page.
   */
  accountRequestFormUrl: '',

  /**
   * The support email shown to the user in various pages of the web application.
   */
  supportEmail: 'teammates@comp.nus.edu.sg',

  /**
   * The public site key for the reCAPTCHA on the recover session links page.
   * You can get a pair of keys from the Google reCAPTCHA website.
   * e.g. "6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI" is a site key for test environments.
   */
  captchaSiteKey: '',

  /**
   * This URL address that will be used to redirect users to an older version of the application.
   */
  redirectUrl: '',
};
