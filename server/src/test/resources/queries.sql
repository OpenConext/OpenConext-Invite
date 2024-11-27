--Users
select u.name,
       u.email,
       u.schac_home_organization,
       u.sub,
       u.super_user,
       u.institution_admin,
       (SELECT ur.authority FROM user_roles ur WHERE ur.user_id = u.id LIMIT 1) AS authority
from users u
where MATCH (u.given_name, u.family_name, u.email) AGAINST ('doe' IN BOOLEAN MODE)
order by u.name;

---Roles
SELECT r.id,
       r.name,
       r.description,
       (SELECT COUNT(*) FROM user_roles ur WHERE ur.role_id = r.id) as userRoleCount,
       a.manage_id,
       a.manage_type
FROM roles r
         INNER JOIN application_usages au on au.role_id = r.id
         INNER JOIN applications a on au.application_id = a.id
WHERE MATCH (name, description) against ("de*" IN BOOLEAN MODE)
ORDER BY userRoleCount DESC LIMIT 5;

--UserRoles
select ur.authority, ur.end_date, ur.created_at, u.name, u.email, u.schac_home_organization
FROM user_roles ur
         INNER JOIN users u on u.id = ur.user_id
WHERE (ur.authority = 'GUEST' OR ur.guest_role_included) AND
      ur.role_id = 43229 AND
          MATCH (u.given_name, u.family_name, u.email) AGAINST ('d*'  IN BOOLEAN MODE);


