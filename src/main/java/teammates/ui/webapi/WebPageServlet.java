package teammates.ui.webapi;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that handles the single web page.
 */
@SuppressWarnings("serial")
public class WebPageServlet extends HttpServlet {

    private static final String CSP_POLICY = String.join("; ", Arrays.asList(
            "default-src 'none'",
            "script-src 'self' https://www.google.com/recaptcha/ https://www.gstatic.com/recaptcha/",
            "style-src 'self' 'unsafe-inline'",
            "frame-src 'self' docs.google.com https://www.google.com/recaptcha/",
            "img-src 'self' data: http: https:",
            "font-src 'self'",
            "connect-src 'self'",
            "manifest-src 'self'",
            "form-action 'none'",
            "frame-ancestors 'self'",
            "base-uri 'self'"
    ));

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-Security-Policy", CSP_POLICY);
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "SAMEORIGIN");
        resp.setHeader("X-XSS-Protection", "1; mode=block");
        resp.setHeader("Strict-Transport-Security", "max-age=31536000");
        req.getRequestDispatcher("/index.html").forward(req, resp);
    }

}
