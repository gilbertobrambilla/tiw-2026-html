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
  `hours` int NOT NULL DEFAULT '0',
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
-- Aggreghiamo preventivamente le ore previste
         LEFT JOIN (
    SELECT task_id, SUM(hours) AS total_expected
    FROM task_expected_hours
    GROUP BY task_id
) ph ON t.id = ph.task_id
-- Aggreghiamo preventivamente le ore lavorate
         LEFT JOIN (
    SELECT task_id, SUM(hours) AS total_worked
    FROM task_worked_hours
    GROUP BY task_id
) wh ON t.id = wh.task_id;

-- view for project monthly report
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
    COALESCE(teh.hours, 0) AS expected_hours,
    COALESCE(twh.total_worked, 0) AS worked_hours
FROM projects p
         JOIN wp w ON p.id = w.project_id
         JOIN tasks t ON w.id = t.wp_id
         LEFT JOIN task_expected_hours teh ON t.id = teh.task_id
         LEFT JOIN (
    SELECT task_id, month_index, SUM(hours) AS total_worked
    FROM task_worked_hours
    GROUP BY task_id, month_index
) twh ON teh.task_id = twh.task_id AND teh.month_index = twh.month_index;

-- view for collaborator monthly report
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
         LEFT JOIN task_worked_hours twh ON t.id = twh.task_id AND tw.user_id = twh.user_id;




-- ============================================================================
-- 1. UTENTI (Già forniti da te, li rimetto per completezza dello script)
-- ============================================================================
INSERT INTO `users` (`id`, `username`, `first_name`, `last_name`, `password`, `role`) VALUES
                                                                                          (1, 'admin1', 'admin1', 'cogn', '123', 'admin'),
                                                                                          (2, 'admin2', 'admin2', 'cogn', '123', 'admin'),
                                                                                          (3, 'tech1', 'tech1', 'cogn', '123', 'technical'),
                                                                                          (4, 'tech2', 'tech2', 'cogn', '123', 'technical');

-- ============================================================================
-- 2. PROGETTI
-- ============================================================================
INSERT INTO `projects` (`id`, `title`, `duration_months`, `status`, `created_by`, `managed_by`) VALUES
                                                                                                    (1, 'Sistema ERP Operativo', 6, 'assigned', 1, 3), -- Progetto 1: Vivo e assegnato
                                                                                                    (2, 'Sito Web Incompleto', 4, 'created', 2, 4),    -- Progetto 2: Incompleto, per testare i blocchi
                                                                                                    (3, 'App Mobile Storica', 3, 'completed', 1, 3);  -- Progetto 3: Concluso, per report in sola lettura

-- ============================================================================
-- 3. WORK PACKAGES (WP)
-- ============================================================================
INSERT INTO `wp` (`id`, `project_id`, `sequence_number`, `title`, `start_month`, `end_month`) VALUES
-- Progetto 1 (6 mesi)
(1, 1, 1, 'Analisi e Design', 1, 3),
(2, 1, 2, 'Sviluppo Backend', 4, 6),
-- Progetto 2 (4 mesi)
(3, 2, 1, 'Setup Infrastruttura', 1, 4),
-- Progetto 3 (3 mesi)
(4, 3, 1, 'Bug Fixing Generale', 1, 3);

-- ============================================================================
-- 4. TASKS
-- ============================================================================
INSERT INTO `tasks` (`id`, `wp_id`, `sequence_number`, `title`, `description`, `start_month`, `end_month`) VALUES
-- Progetto 1 -> WP 1
(1, 1, 1, 'Raccolta requisiti', 'Interviste con il cliente', 1, 2),
(2, 1, 2, 'Progettazione DB', 'Schema ER e logico', 2, 3),
-- Progetto 1 -> WP 2
(3, 2, 1, 'API Rest', 'Sviluppo endpoint principali', 4, 5),
-- Progetto 2 -> WP 3
(4, 3, 1, 'Configurazione server', 'Setup macchina Linux', 1, 2),
(5, 3, 2, 'Setup CI/CD', 'Pipeline su GitHub Actions', 3, 4),
-- Progetto 3 -> WP 4
(6, 4, 1, 'Fix UI Login', 'Risoluzione glitch visivo', 1, 3);

-- ============================================================================
-- 5. ASSEGNAZIONE COLLABORATORI (task_workers)
-- ============================================================================
INSERT INTO `task_workers` (`task_id`, `user_id`) VALUES
                                                      (1, 4),       -- Task 1.1 assegnato a tech2
                                                      (2, 3),       -- Task 1.2 assegnato a tech1 (manager che fa anche il collaboratore)
                                                      (2, 4),       -- Task 1.2 assegnato anche a tech2 (più collaboratori)
                                                      (3, 4),       -- Task 2.1 assegnato a tech2
-- Nota: I task 4 e 5 del Progetto 2 NON hanno collaboratori per testare il blocco
                                                      (6, 4);       -- Task 1.1 (Proj 3) assegnato a tech2

-- ============================================================================
-- 6. ORE PREVISTE (task_expected_hours)
-- ============================================================================
INSERT INTO `task_expected_hours` (`task_id`, `month_index`, `hours`) VALUES
-- Task 1 (M1-M2)
(1, 1, 10),
(1, 2, 10),
-- Task 2 (M2-M3)
(2, 2, 5),
(2, 3, 5),
-- Task 3 (M4-M5) -> M5 lasciato deliberatamente a 0
(3, 4, 15),
(3, 5, 0),
-- Task 5 (M3-M4) -> Progetto 2, ha le ore ma non i workers
(5, 3, 8),
(5, 4, 8),
-- Task 6 (M1-M3) -> Progetto concluso
(6, 1, 20),
(6, 2, 20),
(6, 3, 20);

-- ============================================================================
-- 7. ORE LAVORATE (task_worked_hours)
-- ============================================================================
INSERT INTO `task_worked_hours` (`task_id`, `user_id`, `month_index`, `hours`) VALUES
-- Progetto 1
(1, 4, 1, 12), -- tech2 lavora 12h (più delle 10 previste). M2 è a zero, impedisce la chiusura.
(2, 3, 2, 2),  -- tech1 lavora 2h
(2, 4, 2, 3),  -- tech2 lavora 3h nello stesso mese/task
-- Progetto 3 (Concluso: ore lavorate >= previste in tutti i mesi)
(6, 4, 1, 20),
(6, 4, 2, 20),
(6, 4, 3, 25); -- Supera leggermente per realismo

-- ============================================================================
-- 8. CORNER CASES: EMPTY STATES
-- ============================================================================

-- Aggiungiamo il Progetto 4: Totalmente vuoto (Nessun WP associato)
-- Stato 'created', creato da admin1, assegnato a tech1
INSERT INTO `projects` (`id`, `title`, `duration_months`, `status`, `created_by`, `managed_by`) VALUES
    (4, 'Progetto Appena Nato (Vuoto)', 12, 'created', 1, 3);

-- Aggiungiamo il WP 5: Un Work Package vuoto (Nessun Task associato)
-- Lo inseriamo all'interno del Progetto 2 (Sito Web Incompleto) creato da admin2
INSERT INTO `wp` (`id`, `project_id`, `sequence_number`, `title`, `start_month`, `end_month`) VALUES
    (5, 2, 2, 'WP Senza Task', 2, 4);
