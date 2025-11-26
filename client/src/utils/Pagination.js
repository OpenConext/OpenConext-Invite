import {isEmpty} from "./Utils";
import {getParameterByName} from "./QueryParameters";

export const pageCount = 10;

export const paginationQueryParams = (page, queryParams = {}) => {
    if (!isEmpty(page)) {
        if (!isEmpty(page.query)) {
            queryParams.query = encodeURIComponent(page.query);
        }
        if (!isEmpty(page.pageNumber)) {
            queryParams.pageNumber = page.pageNumber;
        }
        if (!isEmpty(page.pageSize)) {
            queryParams.pageSize = page.pageSize;
        }
        if (!isEmpty(page.sort)) {
            queryParams.sort = page.sort;
        }
        if (!isEmpty(page.sortDirection)) {
            queryParams.sortDirection = page.sortDirection;
        }
        if (!isEmpty(page.roleId)) {
            queryParams.roleId = encodeURIComponent(page.roleId);
        }
    }
    return Object.entries(queryParams).reduce((acc, entry) => {
        acc += `${entry[0]}=${encodeURIComponent(entry[1])}&`
        return acc;
    }, "");
}

export const defaultPagination = (sort = "name", sortDirection = "ASC") => {
    const dp = {
        query: "",
        pageNumber: searchParameterFromQueryParams("page", true, 1) - 1,
        pageSize: pageCount,
        sort: searchParameterFromQueryParams("sort", false, sort),
        sortDirection: searchParameterFromQueryParams("sortDirection", false, sortDirection)
    };
    return {...dp};
}

export const storeSearchQueryParameter = (parameterName, value) => {
    const url = new URL(window.location);
    url.searchParams.set(parameterName, value);
    window.history.pushState({[parameterName]: value}, "", url);
}

export const searchParameterFromQueryParams = (parameterName, isNumeric, defaultValue) => {
    const parameterByName = getParameterByName(parameterName, window.location.search);
    const value = parameterByName || defaultValue;
    return isNumeric ? parseInt(value, 10) : value;
}