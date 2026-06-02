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

@WebServlet("/technical/manager/home")
public class HomeManager extends HttpServlet {
    private TemplateEngine templateEngine;
    private static final String TEMPLATE_PATH = "/WEB-INF/technical/manager/home-manager.html";

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

        // global errors from forwarded requests are transferred to context
        boolean hasForwardedGlobalError = request.getAttribute("managerHomeGlobalError") != null;
        if (hasForwardedGlobalError) {
            ctx.setVariable("managerHomeGlobalError", request.getAttribute("managerHomeGlobalError"));
        }

        // request parameters are parsed
        String taskIdParam = request.getParameter("taskId");

        // database connection is established
        try (Connection connection = DatabaseConnection.getInstance(getServletContext()).getConnection()) {
            ProjectDAO projectDAO = new ProjectDAO(connection);
            WPDAO wpDAO = new WPDAO(connection);
            TaskDAO taskDAO = new TaskDAO(connection);
            UserDAO userDAO = new UserDAO(connection);
            TaskHoursDAO taskHoursDAO = new TaskHoursDAO(connection);

            // project structure list is loaded for the manager
            List<ProjectStructure> projectStructuresList = wpDAO.getProjectStructureByManager(loggedInUser.getId());
            ctx.setVariable("projectStructuresList", projectStructuresList);

            if (taskIdParam != null && !taskIdParam.trim().isEmpty()) {
                int selectedTaskId;

                try {
                    selectedTaskId = Integer.parseInt(taskIdParam);
                } catch (NumberFormatException e) {
                    if (!hasForwardedGlobalError) {
                        ctx.setVariable("managerHomeGlobalError", "invalid task parameter");
                    }
                    response.setContentType("text/html;charset=UTF-8");
                    templateEngine.process(TEMPLATE_PATH, ctx, response.getWriter());
                    return;
                }

                // selected task is loaded only for created projects
                Task selectedTask = taskDAO.getTaskByIdAndManagerAndProjectStatus(
                        selectedTaskId,
                        loggedInUser.getId(),
                        ProjectStatus.created
                );

                // project authorization and correct status are validated
                if (selectedTask != null) {
                    Project selectedProject = projectDAO.getProjectByTaskIdAndManagerAndStatus(
                            selectedTaskId,
                            loggedInUser.getId(),
                            ProjectStatus.created
                    );

                    ctx.setVariable("selectedProject", selectedProject);
                    ctx.setVariable("selectedTask", selectedTask);
                    ctx.setVariable("selectedTaskId", selectedTaskId);

                    // available technical collaborators are fetched
                    List<User> availableCollaborators = userDAO.getAvailableCollaboratorsByManager(loggedInUser.getId());
                    ctx.setVariable("availableCollaborators", availableCollaborators);

                    // existing hours are loaded for the specific task
                    Map<Integer, Integer> existingHours = taskHoursDAO.getExpectedHoursByTask(
                            selectedTaskId,
                            selectedTask.getStartMonth(),
                            selectedTask.getEndMonth()
                    );
                    ctx.setVariable("existingHours", existingHours);

                    // existing collaborator assignments are fetched
                    List<Integer> existingCollaboratorIds = userDAO.getCollaboratorIdsByTask(selectedTaskId);
                    ctx.setVariable("existingCollaboratorIds", existingCollaboratorIds);
                } else {
                    if (!hasForwardedGlobalError) {
                        ctx.setVariable("managerHomeGlobalError", "task not found, not editable or authorization denied");
                    }
                }
            }

            // request attributes for form errors are transferred to the context if present
            String[] errorAttributes = {"formErrors", "hoursValues", "selectedCollaboratorIdsValue"};
            for (String attr : errorAttributes) {
                if (request.getAttribute(attr) != null) {
                    ctx.setVariable(attr, request.getAttribute(attr));
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
