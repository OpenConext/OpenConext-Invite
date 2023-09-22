export const organisationName = invitationRole => {
    const application = invitationRole.role.application;
    return ` (${application.data.metaDataFields["OrganizationName:en"]})`;
}
