package it.polimi.tiw.filters;

import it.polimi.tiw.beans.UserLogin;
import it.polimi.tiw.beans.UserRole;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Filter intercepting requests to administrative endpoints to ensure proper authorization.
 */
public class AdminFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // retrieve current session
        HttpSession session = req.getSession(false);

        // verify authentication status
        if (session == null || session.getAttribute("user") == null) {
            res.sendRedirect(req.getServletContext().getContextPath() + "/");
            return;
        }

        // verify administrative role
        UserLogin user = (UserLogin) session.getAttribute("user");
        UserRole role = user.getRole();
        if (!UserRole.admin.equals(role)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied. Administrator role required.");
            return;
        }

        // pass request along the chain
        chain.doFilter(request, response);
    }
}
