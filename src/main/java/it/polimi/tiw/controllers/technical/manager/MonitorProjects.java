package it.polimi.tiw.controllers.technical.manager;

import it.polimi.tiw.beans.*;
import it.polimi.tiw.dao.*;
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
 * Servlet used for rendering the project monitoring page.
 */
@WebServlet("/technical/manager/monitor-projects")
public class MonitorProjects extends HttpServlet {
    private TemplateEngine templateEngine;
    private static final String TEMPLATE_PATH = "/WEB-INF/technical/manager/monitor-projects.html";

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
        // thymeleaf web application context is initialized
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(getServletContext());
        WebContext ctx = new WebContext(webApplication.buildExchange(request, response), request.getLocale());

        // session parameters are extracted
        HttpSession session = request.getSession();
        UserLogin loggedInUser = (UserLogin) session.getAttribute("user");

        // database connection is established
        try (Connection connection = DatabaseConnection.getInstance(getServletContext()).getConnection()) {
            ProjectDAO projectDAO = new ProjectDAO(connection);
            ProjectReportDAO projectReportDAO = new ProjectReportDAO(connection);

            // projects managed by the current user are retrieved
            List<Project> managedProjects = projectDAO.getProjectsByManager(loggedInUser.getId());
            ctx.setVariable("managedProjects", managedProjects);

            String projectIdParam = request.getParameter("projectId");
            if (projectIdParam != null && !projectIdParam.trim().isEmpty()) {
                int selectedProjectId;

                // syntax check for identifier is performed
                try {
                    selectedProjectId = Integer.parseInt(projectIdParam);
                } catch (NumberFormatException e) {
                    ctx.setVariable("monitorProjectsGlobalError", "invalid project parameter");
                    response.setContentType("text/html;charset=UTF-8");
                    templateEngine.process(TEMPLATE_PATH, ctx, response.getWriter());
                    return;
                }

                // project ownership is semantically verified
                Project selectedProject = projectDAO.getProjectByIdAndManager(selectedProjectId, loggedInUser.getId());

                if (selectedProject != null) {
                    ctx.setVariable("selectedProject", selectedProject);

                    // max months variable is injected for view rendering
                    ctx.setVariable("maxMonths", selectedProject.getDurationMonths());

                    // closable status is verified dynamically
                    boolean isClosable = projectDAO.isProjectClosable(selectedProjectId);
                    ctx.setVariable("isClosable", isClosable);

                    // monthly report bean is loaded for the selected project
                    ProjectMonthlyReport projectReport = projectReportDAO.getProjectMonthlyReport(selectedProject, isClosable);
                    ctx.setVariable("projectReport", projectReport);
                } else {
                    // invalid project is flagged
                    ctx.setVariable("monitorProjectsGlobalError", "project not found or authorization denied");
                }
            }

            // forwarded errors are passed to the context
            if (request.getAttribute("monitorProjectsGlobalError") != null) {
                ctx.setVariable("monitorProjectsGlobalError", request.getAttribute("monitorProjectsGlobalError"));
            }
            if (request.getAttribute("formErrors") != null) {
                ctx.setVariable("formErrors", request.getAttribute("formErrors"));
            }

        } catch (SQLException e) {
            // database connection failure is handled
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failure");
            return;
        }

        // response content type is set and template is processed
        response.setContentType("text/html;charset=UTF-8");
        templateEngine.process(TEMPLATE_PATH, ctx, response.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // forwarded requests from action controllers are routed to the get handler
        doGet(request, response);
    }
}
