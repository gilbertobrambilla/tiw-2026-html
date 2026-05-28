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
 * Servlet used for rendering the collaborator monitoring page.
 */
@WebServlet("/technical/manager/monitor-collaborators")
public class MonitorCollaborators extends HttpServlet {
    private TemplateEngine templateEngine;
    private static final String TEMPLATE_PATH = "/WEB-INF/technical/manager/monitor-collaborators.html";

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
            UserDAO userDAO = new UserDAO(connection);
            ProjectReportDAO projectReportDAO = new ProjectReportDAO(connection);

            // collaborators assigned to manager projects are retrieved
            List<User> teamCollaborators = userDAO.getCollaboratorsByManager(loggedInUser.getId());
            ctx.setVariable("teamCollaborators", teamCollaborators);

            // request parameters are parsed
            String collabIdParam = request.getParameter("collaboratorId");
            if (collabIdParam != null && !collabIdParam.trim().isEmpty()) {
                int collaboratorId = Integer.parseInt(collabIdParam);

                // collaborator entity is fetched
                User selectedCollaborator = userDAO.getUserById(collaboratorId);

                if (selectedCollaborator != null) {
                    ctx.setVariable("selectedCollaborator", selectedCollaborator);

                    // hierarchical data list is generated for the collaborator
                    List<ProjectMonthlyReport> collaboratorReportList =
                            projectReportDAO.getCollaboratorMonthlyReportByManagerAndCollaborator(loggedInUser.getId(), collaboratorId);
                    ctx.setVariable("collaboratorReportList", collaboratorReportList);

                    // dynamic table scaling logic is calculated based on project durations
                    int maxMonths = 0;
                    for (ProjectMonthlyReport report : collaboratorReportList) {
                        if (report.getProject().getDurationMonths() > maxMonths) {
                            maxMonths = report.getProject().getDurationMonths();
                        }
                    }
                    ctx.setVariable("maxMonths", maxMonths);
                }
            }

        } catch (SQLException e) {
            // database error is handled
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
