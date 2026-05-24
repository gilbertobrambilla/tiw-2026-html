package it.polimi.tiw.controllers.admin;

import it.polimi.tiw.beans.UserRole;
import it.polimi.tiw.dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import it.polimi.tiw.beans.UserLogin;
import it.polimi.tiw.dao.ProjectDAO;
import it.polimi.tiw.utils.DatabaseConnection;

/**
 * Project creation controller handling project submission and validation.
 */
@WebServlet("/admin/create-project")
public class CreateProject extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String title = request.getParameter("title");
        String durationStr = request.getParameter("duration");
        String managedByStr = request.getParameter("managedBy");

        boolean hasError = false;

        // title validation
        if (title == null || title.trim().isEmpty()) {
            request.setAttribute("projectTitleError", "Project title cannot be empty");
            hasError = true;
        } else if (title.length() > 100) {
            request.setAttribute("projectTitleError", "Project title cannot exceed 100 characters");
            hasError = true;
        }

        // duration validation
        int duration = 0;
        if (durationStr == null || durationStr.trim().isEmpty()) {
            request.setAttribute("projectDurationError", "Project duration cannot be empty");
            hasError = true;
        } else {
            try {
                duration = Integer.parseInt(durationStr);
                if (duration <= 0) {
                    request.setAttribute("projectDurationError", "Project duration must be greater than zero");
                    hasError = true;
                }
            } catch (NumberFormatException e) {
                request.setAttribute("projectDurationError", "Project duration must be a valid integer");
                hasError = true;
            }
        }

        // manager format validation
        int managedById = 0;
        if (managedByStr == null || managedByStr.trim().isEmpty()) {
            request.setAttribute("projectManagedByError", "Technical user must be selected");
            hasError = true;
        } else {
            try {
                managedById = Integer.parseInt(managedByStr);
            } catch (NumberFormatException e) {
                request.setAttribute("projectManagedByError", "Invalid user identification");
                hasError = true;
            }
        }

        // database operations are executed
        try (Connection connection = DatabaseConnection.getInstance(getServletContext()).getConnection()) {

            // manager database validation is performed if format is valid
            if (!hasError && managedById > 0) {
                UserDAO userDAO = new UserDAO(connection);
                UserRole role = userDAO.getRoleByUser(managedById);

                if (role != UserRole.technical) {
                    request.setAttribute("projectManagedByError", "User is not a valid technical user");
                    hasError = true;
                }
            }

            // request attributes are preserved and forwarded on error
            if (hasError) {
                request.setAttribute("projectTitleValue", title);
                request.setAttribute("projectDurationValue", durationStr);
                request.setAttribute("projectManagedByValue", managedByStr);

                request.getRequestDispatcher("/admin/home").forward(request, response);
                return;
            }

            // session user is retrieved
            HttpSession session = request.getSession();
            UserLogin loggedInUser = (UserLogin) session.getAttribute("user");

            // project is created
            ProjectDAO projectDAO = new ProjectDAO(connection);
            projectDAO.createProject(title, duration, loggedInUser.getId(), managedById);

        } catch (SQLException e) {
            // internal server error is dispatched on database failure
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database failure occurred during project creation");
            return;
        }

        // redirect is performed on successful creation
        response.sendRedirect(getServletContext().getContextPath() + "/admin/home");
    }
}
