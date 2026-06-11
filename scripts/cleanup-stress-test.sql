-- Cleanup script for stress test tickets
-- Run this directly on the MySQL database

USE docflow_ai;

-- First, check how many stress test tickets exist
SELECT COUNT(*) as stress_test_count FROM ticket WHERE title LIKE '%????%';

-- Delete related data first (comments, attachments, etc.)
DELETE FROM ticket_comment WHERE ticket_id IN (SELECT id FROM ticket WHERE title LIKE '%????%');
DELETE FROM ticket_attachment WHERE ticket_id IN (SELECT id FROM ticket WHERE title LIKE '%????%');
DELETE FROM ticket_linked_knowledge WHERE ticket_id IN (SELECT id FROM ticket WHERE title LIKE '%????%');

-- Delete the stress test tickets
DELETE FROM ticket WHERE title LIKE '%????%';

-- Verify cleanup
SELECT COUNT(*) as remaining_tickets FROM ticket;
