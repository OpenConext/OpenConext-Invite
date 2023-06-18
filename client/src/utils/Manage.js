export const singleProviderToOption = provider => {
    return {
        value: provider.id,
        label: `${provider.data.metaDataFields["name:en"]} (${provider.data.metaDataFields["OrganizationName:en"]})`,
        type: provider.type
    };
}

export const providersToOptions = providers => {
    return providers.map(provider => singleProviderToOption(provider));
}

