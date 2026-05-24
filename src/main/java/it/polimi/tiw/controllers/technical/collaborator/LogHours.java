package it.polimi.tiw.controllers.technical.collaborator;

import it.polimi.tiw.beans.Task;
import it.polimi.tiw.beans.UserLogin;
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

/**
 * Controller used for the submission and processing of logged hours by a collaborator.
 */
@WebServlet("/technical/collaborator/log-hours")
public class LogHours extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // session and user details are retrieved
        HttpSession session = request.getSession();
        UserLogin user = (UserLogin) session.getAttribute("user");
        int userId = user.getId();

        // request parameters are extracted
        String projectIdParam = request.getParameter("projectId");
        String taskIdParam = request.getParameter("taskId");
        String monthParam = request.getParameter("month");
        String workedHoursParam = request.getParameter("workedHours");

        // forward path is initialized
        String forwardPath = "/technical/collaborator/home";

        // syntax checks for identifiers are performed
        if (projectIdParam == null || projectIdParam.isEmpty() || taskIdParam == null || taskIdParam.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing identifiers");
            return;
        }

        int projectId;
        int taskId;

        try {
            projectId = Integer.parseInt(projectIdParam);
            taskId = Integer.parseInt(taskIdParam);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid format for identifiers");
            return;
        }

        int month;
        int workedHours;

        // syntax checks for input values are performed
        try {
            month = Integer.parseInt(monthParam);
            workedHours = Integer.parseInt(workedHoursParam);
            if (workedHours < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            // syntactic errors and previous values are saved in request
            request.setAttribute("logGenericError", "invalid format for month or hours");
            request.setAttribute("monthValue", monthParam);
            request.setAttribute("workedHoursValue", workedHoursParam);
            request.getRequestDispatcher(forwardPath).forward(request, response);
            return;
        }

        // database connection is established
        try (Connection connection = DatabaseConnection.getInstance(getServletContext()).getConnection()) {
            TaskDAO taskDAO = new TaskDAO(connection);
            TaskHoursDAO taskHoursDAO = new TaskHoursDAO(connection);

            // task existence and user authorization are verified
            Task task = taskDAO.getTaskByIdAndWorker(taskId, userId);
            if (task == null) {
                request.setAttribute("logGenericError", "unauthorized access or task not found");
                request.getRequestDispatcher(forwardPath).forward(request, response);
                return;
            }

            // semantic interval boundaries are checked
            if (month < task.getStartMonth() || month > task.getEndMonth()) {
                request.setAttribute("monthError", "month must be within task boundaries");
                request.setAttribute("monthValue", month);
                request.setAttribute("workedHoursValue", workedHours);
                request.getRequestDispatcher(forwardPath).forward(request, response);
                return;
            }

            // data persistence is executed
            taskHoursDAO.saveWorkedHours(taskId, userId, month, workedHours);

        } catch (SQLException e) {
            // database exception is handled
            request.setAttribute("logGenericError", "database processing failure");
            request.getRequestDispatcher(forwardPath).forward(request, response);
            return;
        }

        // successful redirect is performed
        String redirectPath = getServletContext().getContextPath() + "/technical/collaborator/home?projectId=" + projectId + "&taskId=" + taskId;
        response.sendRedirect(redirectPath);
    }
}
