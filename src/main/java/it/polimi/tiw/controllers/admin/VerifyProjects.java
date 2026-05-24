package it.polimi.tiw.controllers.admin;

import it.polimi.tiw.beans.Project;
import it.polimi.tiw.beans.TaskWithHours;
import it.polimi.tiw.beans.UserLogin;
import it.polimi.tiw.beans.WP;
import it.polimi.tiw.dao.ProjectDAO;
import it.polimi.tiw.dao.ProjectReportDAO;
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
 * Controller used for the administrator project verification page rendering.
 */
@WebServlet("/admin/verify-projects")
public class VerifyProjects extends HttpServlet {
    private TemplateEngine templateEngine;
    private static final String TEMPLATE_PATH = "/WEB-INF/admin/verify-projects.html";

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
            ProjectDAO projectDAO = new ProjectDAO(connection);
            ProjectReportDAO projectReportDAO = new ProjectReportDAO(connection);

            // all projects created by the admin are fetched for the sidebar
            List<Project> projects = projectDAO.getProjectsByCreator(admin.getId());
            ctx.setVariable("projects", projects);

            // selected project identifier is extracted from the request
            String projectIdParam = request.getParameter("projectId");

            if (projectIdParam != null && !projectIdParam.isEmpty()) {
                int projectId = Integer.parseInt(projectIdParam);

                // project ownership is verified
                Project selectedProject = projectDAO.getProjectByIdAndCreator(projectId, admin.getId());

                if (selectedProject != null) {
                    // project details and aggregated report are injected into the context
                    ctx.setVariable("selectedProject", selectedProject);

                    Map<WP, List<TaskWithHours>> projectReport = projectReportDAO.getProjectReport(projectId);
                    ctx.setVariable("projectReport", projectReport);
                }
            }

        } catch (SQLException e) {
            // database error is handled
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failure");
            return;
        } catch (NumberFormatException e) {
            // invalid parameter format is handled
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid project parameter format");
            return;
        }

        // template processing and response generation
        response.setContentType("text/html;charset=UTF-8");
        templateEngine.process(TEMPLATE_PATH, ctx, response.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // requests are routed to the get handler to support forwarded requests
        doGet(request, response);
    }
}
