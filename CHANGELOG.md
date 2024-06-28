
# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## Unreleased - YYYY-MM-DD

## 0.0.15 - 2024-06-26

- Update dependencies
- Force login for authenticated user new invite
- Warn user for email-formats not accepted by Microsoft
- Show error when SCIM is not responding
- CreatedAt for new provisionings
- Show warning when deleting role
- Endpoint for Profile to fetch roles for user
- Introduce RoleManager permissions

## 0.0.14 - 2024-05-27

- Different invitation screen for inviters
- Institution manager can manage managers
- Improve invite email texts
- Fix typo in help message
- Ignore exceptions in remote endpoints during login
- When creating role, not preselect random/first app
- SCIM update group request after user role delete
- Resending an invite should use the language setting the original invite was sent in
- Do not delete expired user-roles without notification

## 0.0.12 - 2024-20-04

- Fix error-handling when name-update fails in SCIM call

## 0.0.11 - 2024-04-16

- Fix styling of welcome deadend on medium screens
- Make clear on welcome/profile which are your actual guest roles and for which you have a different authority
- Fix wrong explanation with override invite settings option
- Voot and invite-AA endpoints will only return roles the user is actually a guest in, not a different authority, just like provisioning already works
- Unbreak 'also invite as guest' when a user upgrades their own role
- Support updating changed attributes in SCIM
- Provisionings now show created date
- Fix correct target authorities of Teams admins, managers while migrating a team
- Update several frontend dependencies for vulnerabilities
- Update to newest version of SURF SDS

## 0.0.10 - 2024-03-14

- Not used bi-directional relation removed
- Enable metrics endpoint
- Update github page
- Choose language of invitation mail
- Bugfix for migrated users
- Idempotency bugfix for manage queries
- Refactor the GitHub action for building the app and publishing the docker
- Rename confusingly misspelled lifecy(c)le config
- Enable metrics

## [0.0.5] - 17-02-2024

- Remove Institution admin role when entitlement is no longer present
- Fix javascript error when searching in apps
- Allow role expiery to be changed
- Require givenname and sn attributes

## [0.0.4] - 16-01-2024

- Add API tokens for direct access to api
- Add Multirole applications
- Add features for SURFconext Teams migation
- Update translations
- Notifications for removed applications
- Add encryption for manage secrets
- Allow admins to accept eduID-only invites with institution account

## [0.0.3] - 15-11-2023

- Delete owl sound
- Fix userrole deletion

## [0.0.2] - 07-11-2023

- First stable release
