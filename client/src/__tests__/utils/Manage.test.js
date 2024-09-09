import {mergeProvidersProvisioningsRoles, reduceApplicationFromUserRoles} from "../../utils/Manage";
import applications from "./applications.json";
import roles from "./roles.json";
import userRoles from "./userRoles.json";

test("mergeProvidersProvisioningsRoles", () => {
    const results = mergeProvidersProvisioningsRoles(applications.providers, applications.provisionings, roles);
    expect(results.length).toEqual(6);
});

test("reduceApplicationFromUserRoles", () => {
    const results = reduceApplicationFromUserRoles(userRoles, "en");
    const applicationNames = results.map(app => app.applicationName);
    //Sorting alphabetically on applicationName
    expect(applicationNames).toEqual([
        "Calendar EN (SURF bv)",
        "Research EN (SURF bv)",
        "Research EN (SURF bv)",
        "Wiki EN (SURF bv)",
        "Wiki EN (SURF bv)",
        "Wiki EN (SURF bv)"
    ]);
    const roleNames = results
        .filter(app => app.applicationName.startsWith("Wiki"))
        .map(app => app.roleName);
    //Sub-sorting alphabetically on roleName
    expect(roleNames).toEqual([
        "Wiki 1 Role",
        "Wiki 2 Role",
        "Wiki Another Role (3) - Calendar (1)"
    ]);
});

