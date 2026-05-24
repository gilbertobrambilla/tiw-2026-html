package it.polimi.tiw.filters;

import it.polimi.tiw.beans.UserLogin;
import it.polimi.tiw.beans.UserRole;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Filter used for technical macro-role authorization checks.
 */
public class TechnicalFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // current session is retrieved
        HttpSession session = req.getSession(false);

        // authentication status is verified
        if (session == null || session.getAttribute("user") == null) {
            res.sendRedirect(req.getServletContext().getContextPath() + "/");
            return;
        }

        // technical macro-role is verified
        UserLogin user = (UserLogin) session.getAttribute("user");
        if (user.getRole() != UserRole.technical) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied. Technical role required.");
            return;
        }

        // request is passed along the chain
        chain.doFilter(request, response);
    }
}
