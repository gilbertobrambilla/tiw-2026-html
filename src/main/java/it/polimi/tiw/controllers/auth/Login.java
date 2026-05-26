package it.polimi.tiw.controllers.auth;

import it.polimi.tiw.beans.UserLogin;
import it.polimi.tiw.beans.UserRole;
import it.polimi.tiw.dao.UserDAO;
import it.polimi.tiw.utils.DatabaseConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

        List<String> formErrors = new ArrayList<>();

        // validation of input parameters is performed
        if (username == null || username.trim().isEmpty()) {
            formErrors.add("Username cannot be empty");
        }
        if (password == null || password.trim().isEmpty()) {
            formErrors.add("Password cannot be empty");
        }

        // template is processed if validation errors are detected
        if (!formErrors.isEmpty()) {
            webContext.setVariable("formErrors", formErrors);
            webContext.setVariable("usernameValue", username);
            response.setContentType("text/html;charset=UTF-8");
            templateEngine.process(TEMPLATE_PATH, webContext, response.getWriter());
            return;
        }

        UserLogin user;

        // database connection and user lookup are executed
        try (Connection connection = DatabaseConnection.getInstance(context).getConnection()) {
            UserDAO userDAO = new UserDAO(connection);
            user = userDAO.getUserByCredentials(username, password);
        } catch (SQLException e) {
            formErrors.add("Database connection failure");
            webContext.setVariable("formErrors", formErrors);
            webContext.setVariable("usernameValue", username);
            response.setContentType("text/html;charset=UTF-8");
            templateEngine.process(TEMPLATE_PATH, webContext, response.getWriter());
            return;
        }

        // session establishment and macro role redirection are executed
        if (user == null) {
            formErrors.add("Invalid credentials");
            webContext.setVariable("formErrors", formErrors);
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
