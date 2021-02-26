UPDATE study SET status='DRAFT' where status='0';
UPDATE study SET status='PENDING' where status='1';
UPDATE study SET status='REVIEWING' where status='2';
UPDATE study SET status='CHANGE_REQUEST' where status='3';
UPDATE study SET status='DENIED' where status='4';
UPDATE study SET status='APPROVED' where status='5';
UPDATE study SET status='PUBLISHED' where status='6';
UPDATE study SET status='CLOSED' where status='7';
