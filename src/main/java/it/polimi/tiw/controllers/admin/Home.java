package it.polimi.tiw.controllers.admin;

import it.polimi.tiw.beans.Project;
import it.polimi.tiw.beans.ProjectStatus;
import it.polimi.tiw.beans.User;
import it.polimi.tiw.beans.UserLogin;
import it.polimi.tiw.beans.WP;
import it.polimi.tiw.dao.ProjectDAO;
import it.polimi.tiw.dao.UserDAO;
import it.polimi.tiw.dao.WPDAO;
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
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Controller used for the administrator home page rendering.
 */
@WebServlet("/admin/home")
public class Home extends HttpServlet {
    private TemplateEngine templateEngine;
    private static final String TEMPLATE_PATH = "/WEB-INF/admin/home.html";

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

        // session and current user are retrieved
        HttpSession session = request.getSession();
        UserLogin admin = (UserLogin) session.getAttribute("user");

        // database connection is established
        try (Connection connection = DatabaseConnection.getInstance(getServletContext()).getConnection()) {
            UserDAO userDAO = new UserDAO(connection);
            ProjectDAO projectDAO = new ProjectDAO(connection);
            WPDAO wpDAO = new WPDAO(connection);

            // technical users are fetched
            List<User> technicalUsers = userDAO.getUsersByRole("technical");
            ctx.setVariable("technicalUsers", technicalUsers);

            // created projects belonging to the admin are fetched
            List<Project> createdProjects = projectDAO.getProjectsByCreatorAndStatus(admin.getId(), ProjectStatus.created);
            ctx.setVariable("createdProjects", createdProjects);

            // wps grouped by created projects are fetched
            Map<Project, List<WP>> projectWpMap = wpDAO.getWPGroupedByProject(admin.getId());
            ctx.setVariable("projectWpMap", projectWpMap);

        } catch (SQLException e) {
            // database error is handled
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failure");
            return;
        }

        // template processing and response generation
        response.setContentType("text/html;charset=UTF-8");
        templateEngine.process(TEMPLATE_PATH, ctx, response.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // requests are routed to the get handler to support forwarded requests from action controllers
        doGet(request, response);
    }
}
