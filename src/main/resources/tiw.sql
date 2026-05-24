CREATE DATABASE IF NOT EXISTS `tiw`;
USE `tiw`;

CREATE TABLE `users` (
 `id` int NOT NULL AUTO_INCREMENT,
 `username` varchar(50) NOT NULL,
 `first_name` varchar(50) NOT NULL,
 `last_name` varchar(50) NOT NULL,
 `password` varchar(255) NOT NULL,
 `photo` blob,
 `role` enum('admin','technical') NOT NULL DEFAULT 'technical',
 PRIMARY KEY (`id`),
 UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `projects` (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NOT NULL,
  `duration_months` int NOT NULL,
  `status` enum('created','assigned','completed') NOT NULL DEFAULT 'created',
  `created_by` int NOT NULL,
  `managed_by` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `created_by` (`created_by`),
  KEY `projects_fk_2` (`managed_by`),
  CONSTRAINT `projects_fk_1` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `projects_fk_2` FOREIGN KEY (`managed_by`) REFERENCES `users` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `wp` (
  `id` int NOT NULL AUTO_INCREMENT,
  `project_id` int NOT NULL,
  `sequence_number` int NOT NULL,
  `title` varchar(100) NOT NULL,
  `start_month` int NOT NULL,
  `end_month` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `project_id` (`project_id`),
  CONSTRAINT `wp_fk_1` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tasks` (
 `id` int NOT NULL AUTO_INCREMENT,
 `wp_id` int NOT NULL,
 `sequence_number` int NOT NULL,
 `title` varchar(100) NOT NULL,
 `description` text NOT NULL,
 `start_month` int NOT NULL,
 `end_month` int NOT NULL,
 PRIMARY KEY (`id`),
 KEY `wp_id` (`wp_id`),
 CONSTRAINT `tasks_fk_1` FOREIGN KEY (`wp_id`) REFERENCES `wp` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `task_expected_hours` (
  `task_id` int NOT NULL,
  `month_index` int NOT NULL,
  `hours` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`task_id`,`month_index`),
  CONSTRAINT `task_expected_hours_fk_1` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `task_worked_hours` (
  `task_id` int NOT NULL,
  `user_id` int NOT NULL,
  `month_index` int NOT NULL,
  `hours` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`task_id`,`user_id`,`month_index`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `task_worked_hours_fk_1` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `task_worked_hours_fk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `task_workers` (
  `task_id` int NOT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`task_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `task_workers_fk_1` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `task_workers_fk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE OR REPLACE VIEW project_wp_tasks_summary_view AS
SELECT
    p.id AS project_id,
    w.id AS wp_id,
    w.sequence_number AS wp_sequence,
    w.title AS wp_title,
    w.start_month AS wp_start_month,
    w.end_month AS wp_end_month,
    t.id AS task_id,
    t.sequence_number AS task_sequence,
    t.title AS task_title,
    t.description AS task_description,
    t.start_month AS start_month,
    t.end_month AS end_month,
    COALESCE(SUM(ph.hours), 0) AS total_expected_hours,
    COALESCE(SUM(wh.hours), 0) AS total_worked_hours
FROM
    projects p
        JOIN
    wp w ON p.id = w.project_id
        JOIN
    tasks t ON w.id = t.wp_id
        LEFT JOIN
    task_expected_hours ph ON t.id = ph.task_id
        LEFT JOIN
    task_worked_hours wh ON t.id = wh.task_id
GROUP BY
    p.id,
    w.id,
    w.sequence_number,
    w.title,
    w.start_month,
    w.end_month,
    t.id,
    t.sequence_number,
    t.title,
    t.description,
    t.start_month,
    t.end_month;
