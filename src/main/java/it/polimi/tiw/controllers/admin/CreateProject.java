package it.polimi.tiw.controllers.admin;

import it.polimi.tiw.beans.UserLogin;
import it.polimi.tiw.beans.UserRole;
import it.polimi.tiw.dao.ProjectDAO;
import it.polimi.tiw.dao.UserDAO;
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

@WebServlet("/admin/create-project")
public class CreateProject extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String title = request.getParameter("title");
        String durationStr = request.getParameter("duration");
        String managedByStr = request.getParameter("managedBy");

        List<String> formErrors = new ArrayList<>();

        // title validation is performed
        if (title == null || title.trim().isEmpty()) {
            formErrors.add("Project title cannot be empty");
        } else if (title.length() > 100) {
            formErrors.add("Project title cannot exceed 100 characters");
        }

        // duration validation is performed
        int duration = 0;
        if (durationStr == null || durationStr.trim().isEmpty()) {
            formErrors.add("Project duration cannot be empty");
        } else {
            try {
                duration = Integer.parseInt(durationStr);
                if (duration <= 0) {
                    formErrors.add("Project duration must be greater than zero");
                }
            } catch (NumberFormatException e) {
                formErrors.add("Project duration must be a valid integer");
            }
        }

        // manager validation is performed
        int managedById = 0;
        if (managedByStr == null || managedByStr.trim().isEmpty()) {
            formErrors.add("Technical user must be selected");
        } else {
            try {
                managedById = Integer.parseInt(managedByStr);
            } catch (NumberFormatException e) {
                formErrors.add("Invalid user identification");
            }
        }

        // session parameters are extracted
        HttpSession session = request.getSession();
        UserLogin loggedInUser = (UserLogin) session.getAttribute("user");

        // database validation is executed
        try (Connection connection = DatabaseConnection.getInstance(getServletContext()).getConnection()) {

            if (formErrors.isEmpty() && managedById > 0) {
                UserDAO userDAO = new UserDAO(connection);
                UserRole role = userDAO.getRoleByUser(managedById);

                if (role != UserRole.technical) {
                    formErrors.add("User is not a valid technical user");
                }
            }

            // request is forwarded on error detection
            if (!formErrors.isEmpty()) {
                request.setAttribute("formErrors", formErrors);
                request.setAttribute("projectTitleValue", title);
                request.setAttribute("projectDurationValue", durationStr);
                request.setAttribute("projectManagedByValue", managedByStr);

                request.getRequestDispatcher("/admin/home").forward(request, response);
                return;
            }

            ProjectDAO projectDAO = new ProjectDAO(connection);
            projectDAO.createProject(title, duration, loggedInUser.getId(), managedById);

        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database failure occurred during project creation");
            return;
        }

        // redirect is performed on success
        response.sendRedirect(getServletContext().getContextPath() + "/admin/home");
    }
}
