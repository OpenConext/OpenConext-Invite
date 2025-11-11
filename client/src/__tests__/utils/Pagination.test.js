import { describe, it, expect } from 'vitest';
import {defaultPagination, paginationQueryParams} from "../../utils/Pagination";

describe('Pagination', () => {
    it("paginationQueryParams defaults", () => {
        const page = defaultPagination("desc", "DESC");
        const queryParams = paginationQueryParams(page, {custom: "val"});
        expect(queryParams).toEqual("custom=val&pageNumber=0&pageSize=10&sort=desc&sortDirection=DESC&");
    });

    it("paginationQueryParams empty", () => {
        const queryParams = paginationQueryParams({});
        expect(queryParams).toEqual("");
    });
});
