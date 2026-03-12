# Technical Documentation: Internal Placeholder Identifier

The **Internal Placeholder Identifier** feature allows institutions to bridge the gap between their local identity management systems and the external eduID ecosystem. It enables "Pre-Provisioning," where an institution can assign roles and rights to a user in their internal systems *before* that user has accepted an invitation or linked their eduID.

## 1. Functional Overview

An institution can associate a local, internal ID (e.g., a staff number or temporary registration ID) with an invitation. When the user accepts the invitation, this identifier is used to synchronize the account with the institution’s downstream systems. Once the connection is established, the system transitions to using the permanent eduID identifier for all future updates.

---

## 2. API Specification (`POST /api/external/v1/invitations`)

The Invitation API supports two mutually exclusive methods for specifying recipients. You must use **either** the standard `invites` list **or** the `invitesWithInternalPlaceholderIdentifiers` list.

### Request Constraints

* **Mutually Exclusive:** Do not populate both arrays in a single request.
* **Requirement:** At least one email address must be present in one of the two arrays.
* **Labeling:** This field is explicitly named `internalPlaceholderIdentifier` in the API and UI to encourage its use as a transient "stand-in".

### Request Body (`InvitationRequest`)

| Attribute | Type | Description |
| --- | --- | --- |
| `invites` | Array (String) | Standard email list for users without pre-provisioned internal IDs. |
| `invitesWithInternalPlaceholderIdentifiers` | Array (Object) | List of objects containing `email` and `internalPlaceholderIdentifier`. |
| `intendedAuthority` | String | Authority level of the inviter (e.g., `"INVITER"`). |
| `roleIdentifiers` | Array (Int) | List of Role IDs to be granted upon acceptance. |

#### Example Payload: Pre-Provisioning Invitation

```json
{
  "intendedAuthority": "INVITER",
  "message": "Welcome. Your account is ready for activation.",
  "language": "en",
  "guestRoleIncluded": true,
  "invitesWithInternalPlaceholderIdentifiers": [
    {
      "email": "new_hire@institution.edu",
      "internalPlaceholderIdentifier": "STF-2024-9901"
    }
  ],
  "roleIdentifiers": [99],
  "roleExpiryDate": 1760788376,
  "expiryDate": 1730461976
}

```

---

## 3. Provisioning & Lifecycle Logic

### Phase 1: Initial Provisioning (SCIM POST)

When the user accepts the invitation, the system sends a SCIM `POST /v1/Users` message to the institution's provisioning endpoint. The `internalPlaceholderIdentifier` is sent as the `id` in this message.

### Phase 2: Identifier Transition

The system will use the `id` returned by your SCIM server in the response to that initial POST for all future updates (e.g., role changes or group memberships). This allows the institution to replace the temporary placeholder with a permanent identifier (such as a UUID or eduID URN) for all subsequent SCIM messages.

### Phase 3: Logging

The system logs the specific moment when the `id` changes from the placeholder to the permanent identifier to maintain a clear audit trail.

---

## 4. Privacy & Data Governance

| Question | Detail |
| --- | --- |
| **Is it personally identifiable?** | Yes. Because the institution provides this identifier from its own systems (e.g., a student ID), it is **directly traceable** to a person within that institution. |
| **Where is it stored?** | It is stored within the invitation metadata and temporarily in the SCIM provisioning layer until the permanent ID replacement occurs. |
| **Who has access?** | Access is restricted to authorized administrators within the Invite API and the technical logs during the transition phase. |
| **Is it shared globally?** | No. This is an **institution-specific** identifier. It is used to bridge internal systems and is not known to other applications or institutions within the wider eduID ecosystem. |

---

## 5. Summary of Benefits

1. **Combined Processes:** Allows the combination of legacy manual assignments with modern external identities like eduID.
2. **Internal Abstraction:** When used with the Institution-Information API during authentication, internal systems can continue to use their own identifiers, keeping the external eduID identifier abstracted away from most applications.
3. **Day-One Readiness:** Users gain access to necessary roles immediately upon their first login.
