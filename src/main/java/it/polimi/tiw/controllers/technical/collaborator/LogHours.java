package it.polimi.tiw.controllers.technical.collaborator;

import it.polimi.tiw.beans.Task;
import it.polimi.tiw.beans.UserLogin;
import it.polimi.tiw.dao.ProjectDAO;
import it.polimi.tiw.dao.TaskDAO;
import it.polimi.tiw.dao.TaskHoursDAO;
import it.polimi.tiw.utils.DatabaseConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/technical/collaborator/log-hours")
public class LogHours extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // session and user details are retrieved
        HttpSession session = request.getSession();
        UserLogin loggedInUser = (UserLogin) session.getAttribute("user");
        int userId = loggedInUser.getId();

        // request parameters are extracted
        String projectIdParam = request.getParameter("projectId");
        String taskIdParam = request.getParameter("taskId");
        String monthParam = request.getParameter("month");
        String workedHoursParam = request.getParameter("workedHours");

        // forward path is initialized
        String forwardPath = "/technical/collaborator/home";

        // syntax checks for identifiers are performed
        if (projectIdParam == null || projectIdParam.isEmpty() || taskIdParam == null || taskIdParam.isEmpty()) {
            request.setAttribute("homeCollaboratorGlobalError", "missing identifiers");
            request.getRequestDispatcher(forwardPath).forward(request, response);
            return;
        }

        int projectId;
        int taskId;

        try {
            projectId = Integer.parseInt(projectIdParam);
            taskId = Integer.parseInt(taskIdParam);
        } catch (NumberFormatException e) {
            request.setAttribute("homeCollaboratorGlobalError", "invalid format for identifiers");
            request.getRequestDispatcher(forwardPath).forward(request, response);
            return;
        }

        List<String> formErrors = new ArrayList<>();
        int month = 0;
        int workedHours = 0;

        // syntax checks for input values are performed
        try {
            month = Integer.parseInt(monthParam);
        } catch (NumberFormatException | NullPointerException e) {
            formErrors.add("month must be a valid number");
        }

        try {
            workedHours = Integer.parseInt(workedHoursParam);
            if (workedHours < 0) {
                formErrors.add("worked hours must be non-negative");
            }
        } catch (NumberFormatException | NullPointerException e) {
            formErrors.add("worked hours must be a valid number");
        }

        if (!formErrors.isEmpty()) {
            request.setAttribute("formErrors", formErrors);
            request.setAttribute("monthValue", monthParam);
            request.setAttribute("workedHoursValue", workedHoursParam);
            request.getRequestDispatcher(forwardPath).forward(request, response);
            return;
        }

        // database connection is established
        try (Connection connection = DatabaseConnection.getInstance(getServletContext()).getConnection()) {
            TaskDAO taskDAO = new TaskDAO(connection);
            TaskHoursDAO taskHoursDAO = new TaskHoursDAO(connection);
            ProjectDAO projectDAO = new ProjectDAO(connection);

            // project status and authorization are verified
            if (projectDAO.getProjectByIdAndWorker(projectId, userId) == null) {
                request.setAttribute("homeCollaboratorGlobalError", "project not assigned or unauthorized");
                request.getRequestDispatcher(forwardPath).forward(request, response);
                return;
            }

            // task existence, user authorization and project matching are verified
            Task task = taskDAO.getTaskByIdAndWorker(taskId, userId);
            if (task == null || taskDAO.getProjectIdByTask(taskId) != projectId) {
                request.setAttribute("homeCollaboratorGlobalError", "unauthorized access or task mismatch");
                request.getRequestDispatcher(forwardPath).forward(request, response);
                return;
            }

            // semantic interval boundaries are checked
            if (month < task.getStartMonth() || month > task.getEndMonth()) {
                formErrors.add("month must be within task boundaries");
                request.setAttribute("formErrors", formErrors);
                request.setAttribute("monthValue", monthParam);
                request.setAttribute("workedHoursValue", workedHoursParam);
                request.getRequestDispatcher(forwardPath).forward(request, response);
                return;
            }

            // data persistence is executed
            taskHoursDAO.saveWorkedHours(taskId, userId, month, workedHours);

        } catch (SQLException e) {
            // database exception is handled
            request.setAttribute("homeCollaboratorGlobalError", "database processing failure");
            request.getRequestDispatcher(forwardPath).forward(request, response);
            return;
        }

        // successful redirect is performed
        String redirectPath = getServletContext().getContextPath() + "/technical/collaborator/home?projectId=" + projectId + "&taskId=" + taskId;
        response.sendRedirect(redirectPath);
    }
}
