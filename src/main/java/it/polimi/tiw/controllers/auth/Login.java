package it.polimi.tiw.controllers.auth;

import it.polimi.tiw.beans.UserRole;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import it.polimi.tiw.beans.UserLogin;
import it.polimi.tiw.dao.UserDAO;
import it.polimi.tiw.utils.DatabaseConnection;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

/**
 * Authentication controller handling login page rendering and macro role verification.
 */
@WebServlet("/login")
public class Login extends HttpServlet {
    private TemplateEngine templateEngine;
    private static final String TEMPLATE_PATH = "/WEB-INF/auth/login.html";

    @Override
    public void init() throws ServletException {
        ServletContext servletContext = getServletContext();

        // thymeleaf web application and template resolver initialization
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        WebApplicationTemplateResolver templateResolver = new WebApplicationTemplateResolver(webApplication);

        // template engine configuration for html processing
        templateResolver.setTemplateMode(org.thymeleaf.templatemode.TemplateMode.HTML);
        templateResolver.setSuffix(".html");

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // thymeleaf web application context initialization
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(getServletContext());
        WebContext ctx = new WebContext(webApplication.buildExchange(request, response), request.getLocale());

        // template processing and response generation
        response.setContentType("text/html;charset=UTF-8");
        templateEngine.process(TEMPLATE_PATH, ctx, response.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        ServletContext context = getServletContext();
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(context);
        IWebExchange webExchange = application.buildExchange(request, response);
        WebContext webContext = new WebContext(webExchange, webExchange.getLocale());

        // validation of input parameters
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            webContext.setVariable("error", "Missing credentials");
            response.setContentType("text/html;charset=UTF-8");
            templateEngine.process(TEMPLATE_PATH, webContext, response.getWriter());
            return;
        }

        UserLogin user = null;

        // processing login logic through safe database connection lookup
        try (Connection connection = DatabaseConnection.getInstance(context).getConnection()) {
            UserDAO userDAO = new UserDAO(connection);
            user = userDAO.getUserByCredentials(username, password);
        } catch (SQLException e) {
            webContext.setVariable("error", "Database connection failure");
            webContext.setVariable("usernameValue", username);
            response.setContentType("text/html;charset=UTF-8");
            templateEngine.process(TEMPLATE_PATH, webContext, response.getWriter());
            return;
        }

        // session establishment and conditional macro role redirection
        if (user == null) {
            webContext.setVariable("error", "Invalid credentials");
            webContext.setVariable("usernameValue", username);
            response.setContentType("text/html;charset=UTF-8");
            templateEngine.process(TEMPLATE_PATH, webContext, response.getWriter());
        } else {
            HttpSession session = request.getSession();
            session.setAttribute("user", user);

            String target = "/technical/home";
            if (user.getRole().equals(UserRole.admin)) {
                target = "/admin/home";
            }
            response.sendRedirect(context.getContextPath() + target);
        }
    }
}
