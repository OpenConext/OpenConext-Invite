UPDATE invitations
SET email = TRIM(
        SUBSTRING_INDEX(
                SUBSTRING_INDEX(email, '<', -1),
                '>',
                1
        )
)
WHERE email LIKE '%<%@%>%';