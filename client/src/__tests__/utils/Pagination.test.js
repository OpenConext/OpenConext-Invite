import {defaultPagination, paginationQueryParams} from "../../utils/Pagination";

test("paginationQueryParams defaults", () => {
    const page = defaultPagination("desc", "DESC");
    const queryParams = paginationQueryParams(page, {custom: "val"});
    expect(queryParams).toEqual("custom=val&pageNumber=0&pageSize=10&sort=desc&sortDirection=DESC&");
});

test("paginationQueryParams empty", () => {
    const queryParams = paginationQueryParams({});
    expect(queryParams).toEqual("");
});
