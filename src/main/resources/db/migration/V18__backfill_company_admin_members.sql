INSERT INTO company_members (company_id, user_id, company_role, active)
SELECT c.id, c.user_id, 'COMPANY_ADMIN', TRUE
FROM companies c
WHERE NOT EXISTS (
    SELECT 1 FROM company_members cm
    WHERE cm.company_id = c.id AND cm.user_id = c.user_id
);
