package it.polimi.tiw.controllers.technical.collaborator;

import it.polimi.tiw.beans.Project;
import it.polimi.tiw.beans.Task;
import it.polimi.tiw.beans.UserLogin;
import it.polimi.tiw.beans.WP;
import it.polimi.tiw.dao.ProjectDAO;
import it.polimi.tiw.dao.TaskDAO;
import it.polimi.tiw.dao.TaskHoursDAO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller used for the collaborator home page rendering.
 */
@WebServlet("/technical/collaborator/home")
public class Home extends HttpServlet {
    private TemplateEngine templateEngine;
    private static final String TEMPLATE_PATH = "/WEB-INF/technical/collaborator/home.html";

    @Override
    public void init() throws ServletException {
        ServletContext servletContext = getServletContext();

        // thymeleaf web application and template resolver are initialized
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        WebApplicationTemplateResolver templateResolver = new WebApplicationTemplateResolver(webApplication);

        // template engine is configured for html processing
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

        // session and user details are retrieved
        HttpSession session = request.getSession();
        UserLogin user = (UserLogin) session.getAttribute("user");
        int userId = user.getId();

        // request parameters are parsed
        String projectIdParam = request.getParameter("projectId");
        String taskIdParam = request.getParameter("taskId");

        // database connection is established
        try (Connection connection = DatabaseConnection.getInstance(getServletContext()).getConnection()) {
            ProjectDAO projectDAO = new ProjectDAO(connection);
            WPDAO wpDAO = new WPDAO(connection);
            TaskDAO taskDAO = new TaskDAO(connection);
            TaskHoursDAO taskHoursDAO = new TaskHoursDAO(connection);

            // assigned projects are fetched
            List<Project> assignedProjects = projectDAO.getProjectsByWorker(userId);
            ctx.setVariable("assignedProjects", assignedProjects);

            if (projectIdParam != null && !projectIdParam.isEmpty()) {
                int projectId = Integer.parseInt(projectIdParam);
                Project selectedProject = projectDAO.getProjectByIdAndWorker(projectId, userId);
                ctx.setVariable("selectedProject", selectedProject);

                // assigned wps are fetched
                List<WP> assignedWps = wpDAO.getWPsByProjectAndWorker(projectId, userId);
                ctx.setVariable("assignedWps", assignedWps);

                // assigned tasks map is populated
                Map<Integer, List<Task>> assignedTasksMap = new HashMap<>();
                for (WP wp : assignedWps) {
                    List<Task> tasks = taskDAO.getTasksByWpAndWorker(wp.getId(), userId);
                    assignedTasksMap.put(wp.getId(), tasks);
                }
                ctx.setVariable("assignedTasksMap", assignedTasksMap);

                if (taskIdParam != null && !taskIdParam.isEmpty()) {
                    int taskId = Integer.parseInt(taskIdParam);
                    Task selectedTask = taskDAO.getTaskByIdAndWorker(taskId, userId);
                    ctx.setVariable("selectedTask", selectedTask);

                    // hours details are fetched
                    int totalExpectedHours = taskHoursDAO.getTotalExpectedHoursByTask(taskId);
                    int totalWorkedHours = taskHoursDAO.getTotalWorkedHoursByTaskAndUser(taskId, userId);

                    ctx.setVariable("totalExpectedHours", totalExpectedHours);
                    ctx.setVariable("totalWorkedHours", totalWorkedHours);

                    // request attributes for errors are transferred to the context if present
                    String[] errorAttributes = {"logGenericError", "monthError", "workedHoursError", "monthValue", "workedHoursValue"};
                    for (String attr : errorAttributes) {
                        if (request.getAttribute(attr) != null) {
                            ctx.setVariable(attr, request.getAttribute(attr));
                        }
                    }
                }
            }
        } catch (SQLException | NumberFormatException e) {
            // database or parsing errors are handled
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Data processing failure");
            return;
        }

        // response content type is set and template is processed
        response.setContentType("text/html;charset=UTF-8");
        templateEngine.process(TEMPLATE_PATH, ctx, response.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // requests are routed to the get handler to support forwarded requests from action controllers
        doGet(request, response);
    }
}
