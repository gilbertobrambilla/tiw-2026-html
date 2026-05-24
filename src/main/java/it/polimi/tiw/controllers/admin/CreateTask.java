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

/**
 * Controller utilized for task creation and related business logic validation.
 */
@WebServlet("/admin/create-task")
public class CreateTask extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // request parameters are extracted
        String wpIdStr = request.getParameter("wpId");
        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String startMonthStr = request.getParameter("startMonth");
        String endMonthStr = request.getParameter("endMonth");

        boolean hasError = false;

        // title validation is performed
        if (title == null || title.trim().isEmpty()) {
            request.setAttribute("taskTitleError", "Task title cannot be empty");
            hasError = true;
        } else if (title.length() > 100) {
            request.setAttribute("taskTitleError", "Task title cannot exceed 100 characters");
            hasError = true;
        }

        // description validation is performed
        if (description == null || description.trim().isEmpty()) {
            request.setAttribute("taskDescriptionError", "Task description cannot be empty");
            hasError = true;
        }

        // wp identification validation is performed
        int wpId = 0;
        if (wpIdStr == null || wpIdStr.trim().isEmpty()) {
            request.setAttribute("taskWpError", "WP must be selected");
            hasError = true;
        } else {
            try {
                wpId = Integer.parseInt(wpIdStr);
            } catch (NumberFormatException e) {
                request.setAttribute("taskWpError", "Invalid WP identification");
                hasError = true;
            }
        }

        // duration validation is performed
        int startMonth = 0;
        int endMonth = 0;
        try {
            startMonth = Integer.parseInt(startMonthStr);
            endMonth = Integer.parseInt(endMonthStr);
            if (startMonth <= 0 || endMonth <= 0) {
                request.setAttribute("taskStartMonthError", "Months must be strictly positive");
                hasError = true;
            } else if (startMonth > endMonth) {
                request.setAttribute("taskEndMonthError", "End month cannot precede start month");
                hasError = true;
            }
        } catch (NumberFormatException | NullPointerException e) {
            request.setAttribute("taskStartMonthError", "Valid numeric months are required");
            hasError = true;
        }

        // previously inserted values are preserved for the view
        request.setAttribute("taskWpValue", wpIdStr);
        request.setAttribute("taskTitleValue", title);
        request.setAttribute("taskDescriptionValue", description);
        request.setAttribute("taskStartMonthValue", startMonthStr);
        request.setAttribute("taskEndMonthValue", endMonthStr);

        // request is forwarded on syntax error detection
        if (hasError) {
            request.getRequestDispatcher("/admin/home").forward(request, response);
            return;
        }

        // session user is extracted assuming filter validation occurred
        HttpSession session = request.getSession();
        UserLogin admin = (UserLogin) session.getAttribute("user");

        // database operations are executed within a transactional context
        try (Connection connection = DatabaseConnection.getInstance(getServletContext()).getConnection()) {

            // auto-commit is disabled to ensure atomicity
            connection.setAutoCommit(false);

            try {
                WPDAO wpDAO = new WPDAO(connection);
                ProjectDAO projectDAO = new ProjectDAO(connection);
                TaskDAO taskDAO = new TaskDAO(connection);

                // wp existence and authorization are verified
                WP wp = wpDAO.getWPByIdAndCreator(wpId, admin.getId());
                if (wp == null) {
                    request.setAttribute("taskWpError", "WP not found or authorization denied");
                    hasError = true;
                } else {
                    // parent project status is verified
                    Project project = projectDAO.getProjectByIdAndCreator(wp.getProjectId(), admin.getId());
                    if (project == null || project.getStatus() != ProjectStatus.created) {
                        request.setAttribute("taskWpError", "Tasks can only be added to WP belonging to 'created' projects");
                        hasError = true;
                    } else if (startMonth < wp.getStartMonth() || endMonth > wp.getEndMonth()) {
                        // interval limits are verified
                        request.setAttribute("taskEndMonthError", "Task duration (M" + startMonth + "-M" + endMonth +
                                ") must fall within WP duration (M" + wp.getStartMonth() + "-M" + wp.getEndMonth() + ")");
                        hasError = true;
                    }
                }

                // business logic errors trigger rollback and forward
                if (hasError) {
                    connection.rollback();
                    request.getRequestDispatcher("/admin/home").forward(request, response);
                    return;
                }

                // task insertion is performed
                taskDAO.createTask(wpId, title, description, startMonth, endMonth);

                // transaction is finalized
                connection.commit();

            } catch (SQLException e) {
                // transaction is rolled back on database failure
                connection.rollback();
                request.setAttribute("taskGenericError", "A database error occurred during creation");
                request.getRequestDispatcher("/admin/home").forward(request, response);
                return;
            } finally {
                // connection default state is restored
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failure");
            return;
        }

        // redirect is performed on successful creation
        response.sendRedirect(getServletContext().getContextPath() + "/admin/home");
    }
}
