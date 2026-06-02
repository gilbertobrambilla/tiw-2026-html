CREATE DATABASE IF NOT EXISTS `tiw-html`;
USE `tiw-html`;

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
  KEY `managed_by` (`managed_by`),
  CONSTRAINT `projects_fk_1` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `projects_fk_2` FOREIGN KEY (`managed_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `projects_chk_1` CHECK (`duration_months` > 0)
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
  CONSTRAINT `wp_fk_1` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CHECK (`start_month` >= 1 AND `end_month` >= `start_month`)
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
 CONSTRAINT `tasks_fk_1` FOREIGN KEY (`wp_id`) REFERENCES `wp` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
 CONSTRAINT `tasks_chk_1` CHECK (`start_month` >= 1 AND `end_month` >= `start_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `task_expected_hours` (
  `task_id` int NOT NULL,
  `month_index` int NOT NULL,
  `hours` int NOT NULL,
  PRIMARY KEY (`task_id`,`month_index`),
  CONSTRAINT `task_expected_hours_fk_1` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `task_expected_hours_chk_1` CHECK (`month_index` >= 1),
  CONSTRAINT `task_expected_hours_chk_2` CHECK (`hours` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `task_workers` (
  `task_id` int NOT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`task_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `task_workers_fk_1` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `task_workers_fk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `task_worked_hours` (
  `task_id` int NOT NULL,
  `user_id` int NOT NULL,
  `month_index` int NOT NULL,
  `hours` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`task_id`,`user_id`,`month_index`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `task_worked_hours_fk_1` FOREIGN KEY (`task_id`,`user_id`) REFERENCES `task_workers` (`task_id`, `user_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `task_worked_hours_chk_1` CHECK (`month_index` >= 1),
  CONSTRAINT `task_worked_hours_chk_2` CHECK (`hours` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



-- ============================================================================
-- ADMIN DISPLAY VIEW
-- ============================================================================
-- Used mainly by the administrator to display each project structure with
-- total expected hours and total worked hours per task.
-- COALESCE is intentionally used here because this view is for aggregate display:
-- missing expected/worked rows are shown as 0 in the admin summary.
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
    COALESCE(ph.total_expected, 0) AS total_expected_hours,
    COALESCE(wh.total_worked, 0) AS total_worked_hours
FROM projects p
         JOIN wp w ON p.id = w.project_id
         LEFT JOIN tasks t ON w.id = t.wp_id
         LEFT JOIN (
    SELECT task_id, SUM(hours) AS total_expected
    FROM task_expected_hours
    GROUP BY task_id
) ph ON t.id = ph.task_id
         LEFT JOIN (
    SELECT task_id, SUM(hours) AS total_worked
    FROM task_worked_hours
    GROUP BY task_id
) wh ON t.id = wh.task_id;



-- ============================================================================
-- MANAGER RAW MONTHLY VIEW
-- ============================================================================
-- Used by the manager project-monitoring flow to read monthly expected/worked
-- hours without hiding the semantic difference between missing expected hours
-- and explicitly saved 0 expected hours.
-- COALESCE is NOT used on expected_hours: NULL/missing expected values must
-- remain distinguishable from explicit 0 values. COALESCE is still used on
-- worked_hours because missing worked-hours rows mean 0 hours worked.
CREATE OR REPLACE VIEW project_monthly_report_view AS
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
    t.start_month AS task_start_month,
    t.end_month AS task_end_month,
    teh.month_index,
    teh.hours AS expected_hours,
    COALESCE(twh.total_worked, 0) AS worked_hours
FROM projects p
         JOIN wp w ON p.id = w.project_id
         JOIN tasks t ON w.id = t.wp_id
         LEFT JOIN task_expected_hours teh ON t.id = teh.task_id
         LEFT JOIN (
    SELECT task_id, month_index, SUM(hours) AS total_worked
    FROM task_worked_hours
    GROUP BY task_id, month_index
) twh ON teh.task_id = twh.task_id
    AND teh.month_index = twh.month_index;



-- ============================================================================
-- COLLABORATOR RAW MONTHLY VIEW
-- ============================================================================
-- Used by collaborator-related flows to read worked hours per assigned task,
-- collaborator, and month.
-- This view does not expose expected-hours semantics. COALESCE is used on
-- worked_hours because missing worked-hours rows mean 0 hours worked by the
-- collaborator for that month.
CREATE OR REPLACE VIEW collaborator_monthly_report_view AS
SELECT
    p.id AS project_id,
    p.title AS project_title,
    p.duration_months AS project_duration,
    p.status AS project_status,
    p.created_by AS project_created_by,
    p.managed_by AS manager_id,
    w.id AS wp_id,
    w.sequence_number AS wp_sequence,
    w.title AS wp_title,
    t.id AS task_id,
    t.sequence_number AS task_sequence,
    t.title AS task_title,
    t.description AS task_description,
    t.start_month AS task_start_month,
    t.end_month AS task_end_month,
    tw.user_id AS collaborator_id,
    twh.month_index,
    COALESCE(twh.hours, 0) AS worked_hours
FROM projects p
         JOIN wp w ON p.id = w.project_id
         JOIN tasks t ON w.id = t.wp_id
         JOIN task_workers tw ON t.id = tw.task_id
         LEFT JOIN task_worked_hours twh ON t.id = twh.task_id
    AND tw.user_id = twh.user_id;




-- ============================================================================
-- 1. USERS
-- ============================================================================

INSERT INTO `users` (`id`, `username`, `first_name`, `last_name`, `password`, `role`) VALUES
(1, 'admin1', 'Anna', 'Rossi', '123', 'admin'),
(2, 'admin2', 'Bruno', 'Bianchi', '123', 'admin'),
(6, 'admin3', 'Chiara', 'Conti', '123', 'admin'),
(3, 'tech1', 'Marco', 'Verdi', '123', 'technical'),
(4, 'tech2', 'Laura', 'Neri', '123', 'technical'),
(5, 'tech3', 'Giulia', 'Gallo', '123', 'technical');

-- ============================================================================
-- 2. PROJECTS
-- ============================================================================

INSERT INTO `projects` (`id`, `title`, `duration_months`, `status`, `created_by`, `managed_by`) VALUES
(1, 'Sistema ERP Operativo', 6, 'assigned', 1, 3),
(2, 'Portale Fornitori', 4, 'created', 2, 4),
(3, 'App Mobile Storica', 3, 'completed', 1, 3),
(4, 'Progetto Appena Nato', 12, 'created', 1, 3),
(5, 'Analisi Dati Rendicontazione', 5, 'assigned', 1, 3),
(6, 'Intranet Servizi Interni', 4, 'assigned', 2, 4);

-- ============================================================================
-- 3. WORK PACKAGES
-- ============================================================================

INSERT INTO `wp` (`id`, `project_id`, `sequence_number`, `title`, `start_month`, `end_month`) VALUES
(1, 1, 1, 'Analisi e Design', 1, 3),
(2, 1, 2, 'Sviluppo Backend', 4, 6),

(3, 2, 1, 'Setup Infrastruttura', 1, 4),
(5, 2, 2, 'Pianificazione Vuota', 2, 4),

(4, 3, 1, 'Bug Fixing Generale', 1, 3),

(6, 5, 1, 'Raccolta e Pulizia Dati', 1, 3),
(7, 5, 2, 'Dashboard e Consegna', 3, 5),

(8, 6, 1, 'Servizi Applicativi', 1, 4);

-- ============================================================================
-- 4. TASKS
-- ============================================================================

INSERT INTO `tasks` (`id`, `wp_id`, `sequence_number`, `title`, `description`, `start_month`, `end_month`) VALUES
(1, 1, 1, 'Raccolta requisiti', 'Interviste con il cliente', 1, 2),
(2, 1, 2, 'Progettazione DB', 'Schema ER e logico', 2, 3),
(3, 2, 1, 'API REST', 'Sviluppo endpoint principali', 4, 5),

(4, 3, 1, 'Configurazione server', 'Setup macchina Linux', 1, 2),
(5, 3, 2, 'Pipeline CI/CD', 'Pipeline di build e deploy', 3, 4),

(6, 4, 1, 'Fix UI Login', 'Risoluzione glitch visivo', 1, 3),

(7, 6, 1, 'Import dataset', 'Importazione dei dati sorgente', 1, 2),
(8, 6, 2, 'Pulizia dati', 'Normalizzazione e controllo qualità', 2, 3),
(9, 7, 1, 'Report finale', 'Produzione dashboard e report conclusivo', 3, 5),

(10, 8, 1, 'Catalogo servizi', 'Implementazione del catalogo interno', 1, 2),
(11, 8, 2, 'Notifiche interne', 'Gestione notifiche operative', 3, 4);

-- ============================================================================
-- 5. TASK WORKERS
-- ============================================================================

INSERT INTO `task_workers` (`task_id`, `user_id`) VALUES
-- Sistema ERP Operativo, manager tech1, collaboratori tech2 e tech3
(1, 4),
(2, 4),
(2, 5),
(3, 4),

-- App Mobile Storica, manager tech1, collaboratore tech2
(6, 4),

-- Analisi Dati Rendicontazione, manager tech1, collaboratori tech2 e tech3
(7, 4),
(7, 5),
(8, 4),
(9, 4),

-- Intranet Servizi Interni, manager tech2, collaboratori tech1 e tech3
(10, 3),
(11, 5);

-- ============================================================================
-- 6. EXPECTED HOURS
-- ============================================================================

INSERT INTO `task_expected_hours` (`task_id`, `month_index`, `hours`) VALUES
-- Sistema ERP Operativo: progetto assegnato ma non ancora chiudibile
(1, 1, 10),
(1, 2, 10),
(2, 2, 5),
(2, 3, 5),
(3, 4, 15),
(3, 5, 0),

-- Portale Fornitori: progetto created con dati ancora incompleti
(5, 3, 8),
(5, 4, 8),

-- App Mobile Storica: progetto concluso
(6, 1, 20),
(6, 2, 20),
(6, 3, 20),

-- Analisi Dati Rendicontazione: progetto assegnato e chiudibile
(7, 1, 5),
(7, 2, 5),
(8, 2, 4),
(8, 3, 4),
(9, 3, 3),
(9, 4, 3),
(9, 5, 4),

-- Intranet Servizi Interni: progetto assegnato ma non chiudibile
(10, 1, 6),
(10, 2, 6),
(11, 3, 7),
(11, 4, 7);

-- ============================================================================
-- 7. WORKED HOURS
-- ============================================================================

INSERT INTO `task_worked_hours` (`task_id`, `user_id`, `month_index`, `hours`) VALUES
-- Sistema ERP Operativo: ore insufficienti per la conclusione
(1, 4, 1, 12),
(2, 4, 2, 3),
(2, 5, 2, 2),

-- App Mobile Storica: ore lavorate sufficienti
(6, 4, 1, 20),
(6, 4, 2, 20),
(6, 4, 3, 25),

-- Analisi Dati Rendicontazione: ore lavorate sufficienti per mostrare CONCLUDI
(7, 4, 1, 6),
(7, 4, 2, 5),
(8, 4, 2, 4),
(8, 4, 3, 4),
(9, 4, 3, 3),
(9, 4, 4, 3),
(9, 4, 5, 4),

-- Intranet Servizi Interni: ore parziali
(10, 3, 1, 6);
