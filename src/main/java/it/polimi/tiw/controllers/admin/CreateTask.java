package it.polimi.tiw.controllers.admin;

import it.polimi.tiw.beans.Project;
import it.polimi.tiw.beans.ProjectStatus;
import it.polimi.tiw.beans.UserLogin;
import it.polimi.tiw.beans.WP;
import it.polimi.tiw.dao.ProjectDAO;
import it.polimi.tiw.dao.TaskDAO;
import it.polimi.tiw.dao.WPDAO;
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

@WebServlet("/admin/create-task")
public class CreateTask extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String wpIdStr = request.getParameter("wpId");
        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String startMonthStr = request.getParameter("startMonth");
        String endMonthStr = request.getParameter("endMonth");

        List<String> taskFormErrors = new ArrayList<>();

        // title validation is performed
        if (title == null || title.trim().isEmpty()) {
            taskFormErrors.add("Task title cannot be empty");
        } else if (title.length() > 100) {
            taskFormErrors.add("Task title cannot exceed 100 characters");
        }

        // description validation is performed
        if (description == null || description.trim().isEmpty()) {
            taskFormErrors.add("Task description cannot be empty");
        }

        // wp validation is performed
        int wpId = 0;
        if (wpIdStr == null || wpIdStr.trim().isEmpty()) {
            taskFormErrors.add("WP must be selected");
        } else {
            try {
                wpId = Integer.parseInt(wpIdStr);
            } catch (NumberFormatException e) {
                taskFormErrors.add("Invalid WP identification");
            }
        }

        // duration validation is performed
        int startMonth = 0;
        int endMonth = 0;
        try {
            startMonth = Integer.parseInt(startMonthStr);
            endMonth = Integer.parseInt(endMonthStr);
            if (startMonth <= 0 || endMonth <= 0) {
                taskFormErrors.add("Months must be strictly positive");
            } else if (startMonth > endMonth) {
                taskFormErrors.add("End month cannot precede start month");
            }
        } catch (NumberFormatException e) {
            taskFormErrors.add("Valid numeric months are required");
        }

        // session parameters are extracted
        HttpSession session = request.getSession();
        UserLogin loggedInUser = (UserLogin) session.getAttribute("user");

        // transactional database operation is executed
        try (Connection connection = DatabaseConnection.getInstance(getServletContext()).getConnection()) {

            connection.setAutoCommit(false);

            try {
                WPDAO wpDAO = new WPDAO(connection);
                ProjectDAO projectDAO = new ProjectDAO(connection);
                TaskDAO taskDAO = new TaskDAO(connection);

                if (taskFormErrors.isEmpty()) {
                    WP wp = wpDAO.getWPByIdAndCreator(wpId, loggedInUser.getId());
                    if (wp == null) {
                        taskFormErrors.add("WP not found or authorization denied");
                    } else {
                        Project project = projectDAO.getProjectByIdAndCreator(wp.getProjectId(), loggedInUser.getId());
                        if (project == null || project.getStatus() != ProjectStatus.created) {
                            taskFormErrors.add("Tasks can only be added to WP belonging to 'created' projects");
                        } else if (startMonth < wp.getStartMonth() || endMonth > wp.getEndMonth()) {
                            taskFormErrors.add("Task duration (M" + startMonth + "-M" + endMonth + ") must fall within WP duration (M" + wp.getStartMonth() + "-M" + wp.getEndMonth() + ")");
                        }
                    }
                }

                // request is forwarded on error detection
                if (!taskFormErrors.isEmpty()) {
                    connection.rollback();
                    request.setAttribute("taskFormErrors", taskFormErrors);
                    request.setAttribute("taskWpValue", wpIdStr);
                    request.setAttribute("taskTitleValue", title);
                    request.setAttribute("taskDescriptionValue", description);
                    request.setAttribute("taskStartMonthValue", startMonthStr);
                    request.setAttribute("taskEndMonthValue", endMonthStr);

                    request.getRequestDispatcher("/admin/home").forward(request, response);
                    return;
                }

                taskDAO.createTask(wpId, title, description, startMonth, endMonth);
                connection.commit();

            } catch (SQLException e) {
                connection.rollback();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error occurred during creation");
                return;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failure");
            return;
        }

        // redirect is performed on success
        response.sendRedirect(getServletContext().getContextPath() + "/admin/home");
    }
}
