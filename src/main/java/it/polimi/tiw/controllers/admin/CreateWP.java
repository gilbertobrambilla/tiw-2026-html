package it.polimi.tiw.controllers.admin;

import it.polimi.tiw.beans.Project;
import it.polimi.tiw.beans.ProjectStatus;
import it.polimi.tiw.beans.UserLogin;
import it.polimi.tiw.dao.ProjectDAO;
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
 * Controller used for wp creation and related business logic validation.
 */
@WebServlet("/admin/create-wp")
public class CreateWP extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // request parameters are extracted
        String projectIdStr = request.getParameter("projectId");
        String title = request.getParameter("title");
        String startMonthStr = request.getParameter("startMonth");
        String endMonthStr = request.getParameter("endMonth");

        boolean hasError = false;

        // title validation is performed
        if (title == null || title.trim().isEmpty()) {
            request.setAttribute("wpTitleError", "Title cannot be empty");
            hasError = true;
        } else if (title.length() > 100) {
            request.setAttribute("wpTitleError", "Title cannot exceed 100 characters");
            hasError = true;
        }

        // project identification validation is performed
        int projectId = 0;
        if (projectIdStr == null || projectIdStr.trim().isEmpty()) {
            request.setAttribute("wpProjectError", "Project must be selected");
            hasError = true;
        } else {
            try {
                projectId = Integer.parseInt(projectIdStr);
            } catch (NumberFormatException e) {
                request.setAttribute("wpProjectError", "Invalid project identification");
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
                request.setAttribute("wpStartMonthError", "Months must be positive");
                hasError = true;
            } else if (startMonth > endMonth) {
                request.setAttribute("wpEndMonthError", "End month cannot precede start month");
                hasError = true;
            }
        } catch (NumberFormatException | NullPointerException e) {
            request.setAttribute("wpStartMonthError", "Valid numeric months are required");
            hasError = true;
        }

        // previously inserted values are preserved for the view
        request.setAttribute("wpProjectIdValue", projectIdStr);
        request.setAttribute("wpTitleValue", title);
        request.setAttribute("wpStartMonthValue", startMonthStr);
        request.setAttribute("wpEndMonthValue", endMonthStr);

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
                ProjectDAO projectDAO = new ProjectDAO(connection);
                WPDAO wpDAO = new WPDAO(connection);

                // project existence and authorization are verified
                Project project = projectDAO.getProjectByIdAndCreator(projectId, admin.getId());
                if (project == null) {
                    request.setAttribute("wpProjectError", "Project not found or authorization denied");
                    hasError = true;
                } else if (project.getStatus() != ProjectStatus.created) {
                    request.setAttribute("wpProjectError", "WP can only be added to projects in 'created' status");
                    hasError = true;
                } else if (endMonth > project.getDurationMonths()) {
                    request.setAttribute("wpEndMonthError", "Duration exceeds project length (Max: " + project.getDurationMonths() + ")");
                    hasError = true;
                }

                // business logic errors trigger rollback and forward
                if (hasError) {
                    connection.rollback();
                    request.getRequestDispatcher("/admin/home").forward(request, response);
                    return;
                }

                // wp insertion is performed
                wpDAO.createWP(projectId, title, startMonth, endMonth);

                // transaction is finalized
                connection.commit();

            } catch (SQLException e) {
                // transaction is rolled back on database failure
                connection.rollback();
                request.setAttribute("wpGenericError", "A database error occurred during creation");
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
