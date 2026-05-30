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

            List<Project> managedProjects = projectDAO.getAssignedAndCompletedProjectsByManager(loggedInUser.getId());
            ctx.setVariable("managedProjects", managedProjects);

            String projectIdParam = request.getParameter("projectId");
            if (projectIdParam != null && !projectIdParam.trim().isEmpty()) {
                int selectedProjectId;

                try {
                    selectedProjectId = Integer.parseInt(projectIdParam);
                } catch (NumberFormatException e) {
                    ctx.setVariable("monitorProjectsGlobalError", "invalid project parameter");
                    response.setContentType("text/html;charset=UTF-8");
                    templateEngine.process(TEMPLATE_PATH, ctx, response.getWriter());
                    return;
                }

                Project selectedProject = projectDAO.getProjectByIdAndManager(selectedProjectId, loggedInUser.getId());
                boolean isMonitorable = selectedProject != null &&
                        (selectedProject.getStatus() == ProjectStatus.assigned ||
                                selectedProject.getStatus() == ProjectStatus.completed);

                if (isMonitorable) {
                    ctx.setVariable("selectedProject", selectedProject);

                    // max months variable is injected for view rendering
                    ctx.setVariable("maxMonths", selectedProject.getDurationMonths());

                    boolean selectedProjectCompleted = selectedProject.getStatus() == ProjectStatus.completed;
                    ctx.setVariable("selectedProjectCompleted", selectedProjectCompleted);

                    boolean isClosable = selectedProject.getStatus() == ProjectStatus.assigned &&
                            projectDAO.isProjectClosable(selectedProjectId);
                    ctx.setVariable("isClosable", isClosable);

                    ProjectMonthlyReport projectReport = projectReportDAO.getProjectMonthlyReport(selectedProject, isClosable);
                    ctx.setVariable("projectReport", projectReport);
                } else {
                    ctx.setVariable("monitorProjectsGlobalError", "project not found, not monitorable or authorization denied");
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
