UPDATE invitations
SET requested_authn_context = CASE requested_authn_context
    WHEN 'EduIDLinkedInstitution'
        THEN 'https://eduid.nl/trust/linked-institution'
    WHEN 'EduIDValidatedName'
        THEN 'https://eduid.nl/trust/validate-names'
    WHEN 'ValidateNamesExternal'
        THEN 'https://eduid.nl/trust/validate-names-external'
    WHEN 'EduIDRequireStudentAffiliation'
        THEN 'https://eduid.nl/trust/affiliation-student'
    WHEN 'TransparentAuthnContext'
        THEN 'transparent_authn_context'
END
WHERE requested_authn_context IN (
    'EduIDLinkedInstitution',
    'EduIDValidatedName',
    'ValidateNamesExternal',
    'EduIDRequireStudentAffiliation',
    'TransparentAuthnContext'
);