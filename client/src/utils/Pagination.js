import {isEmpty} from "./Utils";

export const pageCount = 3;

//https://gist.github.com/kottenator/9d936eb3e4e3c3e02598
export const pagination = (page, totalResults) => {
    const delta = 2,
        left = page - delta,
        right = page + delta + 1,
        range = [],
        rangeWithDots = []
    let l;

    for (let i = 1; i <= totalResults; i++) {
        if ((i === 1 || i === totalResults) || (i >= left && i < right)) {
            range.push(i);
        }
    }

    for (const i of range) {
        if (l) {
            if (i - l === 2) {
                rangeWithDots.push(l + 1);
            } else if (i - l !== 1) {
                rangeWithDots.push('...');
            }
        }
        rangeWithDots.push(i);
        l = i;
    }
    return rangeWithDots;
}

export const extractPageResultFromServerResult = (page, sorted, sortDirection) => {
    return {
        total: page.totalElements,
        pageCount: page.pageable.pageSize,
        currentPage: page.pageable.pageNumber,
        sort: sorted,
        sortDirection: sortDirection
    }
}

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
    }
    const queryPart = Object.entries(queryParams).reduce((acc, entry) => {
        acc += `${entry[0]}=${encodeURIComponent(entry[1])}&`
        return acc;
    }, "");
    return queryPart;

}

export const defaultPagination = () => {
    const dp = {
        query: "",
        pageNumber: 0,
        pageSize: pageCount,
        sort: "name",
        sortDirection: "ASC"
    };
    return {...dp};
}