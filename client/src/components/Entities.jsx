import React, {useEffect, useRef, useState} from "react";
import I18n from "../locale/I18n";
import {isEmpty, sanitize} from "../utils/Utils";
import {sortObjects, valueForSort} from "../utils/Sort";
import {headerIcon} from "../utils/Forms";
import "./Entities.scss";
import {
    Button,
    Pagination,
    PaginationContent,
    PaginationEllipsis,
    PaginationItem,
    PaginationLink,
    PaginationNext,
    PaginationPrevious,
    Spinner,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow
} from "@surfnet/curve-react";
import {
    pageCount,
    pageHref,
    pageRangeWithDots,
    searchParameterFromQueryParams,
    storeSearchQueryParameter
} from "../utils/Pagination";
import {getStoredSearchQuery, storeSearchQuery} from "../utils/SearchQueryStorage";
import {useSearchGroup} from "../utils/SearchGroupContext";
import {useNavigate} from "react-router";
import {SearchField} from "./SearchField";
import {InfoTooltip} from "./InfoTooltip";

export const Entities = ({
                             modelName,
                             showNew,
                             newLabel,
                             columns,
                             children,
                             loading,
                             actions,
                             title,
                             filters,
                             rowLinkMapper,
                             tableClassName,
                             className = "",
                             customNoEntities,
                             hideTitle,
                             hideHeading = false,
                             onHover,
                             actionHeader = "",
                             totalElements = null,
                             showActionsAlways,
                             displaySearch = true,
                             searchCallback,
                             customSearch,
                             entities,
                             searchAttributes,
                             newEntityPath,
                             newEntityFunc,
                             defaultSort,
                             rowClassNameResolver,
                             inputFocus = false,
                             busy = false
                         }) => {

    //Tabs sharing a SearchGroupContext (e.g. all Home tabs, or all tabs of one Role) carry
    //the search text over when switching between them; standalone pages fall back to a key
    //scoped to this page and model, so unrelated pages never leak search text into each other.
    const searchGroup = useSearchGroup();
    const searchQueryKey = searchGroup ? `group:${searchGroup}` : `${window.location.pathname}:${modelName}`;

    const [query, setQuery] = useState(() => getStoredSearchQuery(searchQueryKey));
    const [sorted, setSorted] = useState(searchParameterFromQueryParams("sort", false, defaultSort));
    const [reverse, setReverse] = useState("DESC" === searchParameterFromQueryParams("sortDirection", false, "ASC"));
    const [page, setPage] = useState(searchParameterFromQueryParams("page", true, 1));

    const searchRef = useRef();
    const navigate = useNavigate();

    useEffect(() => {
        if ((displaySearch || inputFocus) && searchRef && searchRef.current) {
            searchRef.current.focus();
        }
    }, [displaySearch, inputFocus])

    //Restore the results for a query that was persisted when the user last left this tab
    useEffect(() => {
        if (query) {
            callCustomSearch(query, sorted, reverse, page);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const newEntity = () => {
        if (newEntityFunc) {
            newEntityFunc();
        } else {
            navigate(newEntityPath);
        }
    };

    const queryChanged = e => {
        const newQuery = e.target.value;
        const currentQuery = query;
        setQuery(newQuery);
        storeSearchQuery(searchQueryKey, newQuery);
        //When the user changes the query text we reset the page number
        const queryChanged = currentQuery !== newQuery;
        if (queryChanged) {
            setPage(1);
        }
        callCustomSearch(newQuery, sorted, reverse, queryChanged ? 1 : page);
    }

    const renderSearch = () => {
        const filterClassName = (!hideTitle && filters) ? "filters-with-title" : `${modelName}-search-filters`;
        const hasSearch = !isEmpty(searchAttributes) || customSearch;
        return (
            <section className={`entities-search ${showNew ? "" : "only-search"}`}>
                {(!hideTitle && !hideHeading) &&
                    <h2>{title || `${I18n.t(`${modelName}.title`)} (${(totalElements || entities.length).toLocaleString()})`}</h2>}
                {(loading || hideTitle) && <Spinner className="size-6"/>}
                {!isEmpty(filters) && <div className={`${filterClassName} search-filter`}>{filters}</div>}
                <div className={`search ${showNew ? "" : "standalone"}`}>
                    {hasSearch &&
                        <SearchField inputRef={searchRef}
                                     onChange={queryChanged}
                                     value={query}
                                     placeholder={I18n.t(`${modelName}.searchPlaceHolder`)}/>}
                </div>
                {showNew &&
                    <Button onClick={newEntity}
                            className={`${hideTitle && !filters ? "no-title" : ""}`}>
                        <span dangerouslySetInnerHTML={{__html: sanitize(newLabel || I18n.t(`${modelName}.new`))}}/>
                    </Button>
                }
            </section>
        );
    };

    const filterEntities = newQuery => {
        if (isEmpty(newQuery) || customSearch) {
            return entities;
        }
        const queryLower = newQuery.toLowerCase();
        return entities.filter(entity => searchAttributes.some(attr => {
            const val = valueForSort(attr, entity);
            //When the application is unknown in Manage then the val is a React span child object
            return (isEmpty(val) || typeof val !== "string" || val.toLowerCase === undefined) ? false : val.toLowerCase().indexOf(queryLower) > -1;
        }));
    };

    const setSortedKey = key => {
        const newReserve = (sorted === key ? !reverse : false);
        setSorted(key);
        storeSearchQueryParameter("sort", key);
        setReverse(newReserve);
        storeSearchQueryParameter("sortDirection", newReserve ? "ASC" : "DESC");
        callCustomSearch(query, key, newReserve, page);
    }

    const callCustomSearch = (newQuery, newSorted, newReversed, newPage) => {
        if (customSearch) {
            //Adjust page, as serverSide is zero-based
            customSearch(newQuery, newSorted, newReversed, newPage - 1);
        }
        if (searchCallback) {
            const searchResult = filterEntities(query);
            searchCallback(searchResult);
        }

    }

    const getEntityValue = (entity, column) => {
        return column.mapper ? column.mapper(entity) : entity[column.key];
    }

    const onRowClick = (e, entity) => {
        if (typeof rowLinkMapper === "function") {
            rowLinkMapper(e, entity);
        }
    }

    const entityRow = (entity, index) => {
        const additionalClassName = isEmpty(rowClassNameResolver) ? "" : rowClassNameResolver(entity);
        return <TableRow key={`tr_${entity.id}_${index}`}
                         className={`${typeof rowLinkMapper === "function" ? "clickable" : ""} ${onHover ? "hoverable" : ""} ${additionalClassName}`}>
            {columns.map((column, i) =>
                <TableCell key={`td_${column.key}_${i}`}
                           onClick={e => (column.key !== "check" && !column.hasLink) ?
                               onRowClick(e, entity) : undefined}
                           data-label={typeof column === "string" ? column.header : ""}
                           className={`${column.key} ${column.nonSortable ? "" : "sortable"} ${column.className ? column.className : ""}`}>
                    {getEntityValue(entity, column)}
                </TableCell>)}
        </TableRow>;
    }

    const changePage = nbr => {
        setPage(nbr);
        callCustomSearch(query, sorted, reverse, nbr);
        storeSearchQueryParameter("page", nbr);
    }

    const renderPagination = total => {
        const nbrPages = Math.ceil(total / pageCount);
        if (total <= pageCount) {
            return null;
        }
        return (
            <Pagination>
                <PaginationContent>
                    {page !== 1 && <PaginationItem>
                        <PaginationPrevious href={pageHref(page - 1)} iconOnly={true}
                                            onClick={e => {
                                                e.preventDefault();
                                                changePage(page - 1);
                                            }}/>
                    </PaginationItem>}
                    {pageRangeWithDots(page, nbrPages).map((nbr, index) =>
                        <PaginationItem key={`${nbr}_${index}`}>
                            {typeof nbr === "string" ?
                                <PaginationEllipsis/> :
                                <PaginationLink href={pageHref(nbr)} isActive={nbr === page}
                                                aria-current={nbr === page ? "page" : undefined}
                                                onClick={e => {
                                                    e.preventDefault();
                                                    changePage(nbr);
                                                }}>{nbr}</PaginationLink>}
                        </PaginationItem>
                    )}
                    {page !== nbrPages && <PaginationItem>
                        <PaginationNext href={pageHref(page + 1)} iconOnly={true}
                                        onClick={e => {
                                            e.preventDefault();
                                            changePage(page + 1);
                                        }}/>
                    </PaginationItem>}
                </PaginationContent>
            </Pagination>
        );
    };

    const renderEntities = sortedEntities => {
        const hasEntities = !isEmpty(sortedEntities);
        const customEmptySearch = customSearch && (isEmpty(query) || query.trim().length < 3);
        const total = sortedEntities.length;
        const minimalPage = Math.min(page, Math.ceil(sortedEntities.length / pageCount));
        sortedEntities = sortedEntities.slice((minimalPage - 1) * pageCount, minimalPage * pageCount);
        return (
            <section className="entities-list">
                {(actions && (showActionsAlways || hasEntities)) && <div className={`actions-header ${actionHeader}`}>
                    {actions}
                </div>}
                {hasEntities &&
                    <Table className={tableClassName || modelName}>
                        <TableHeader>
                            <TableRow>
                                {columns.map((column, i) => {
                                    const showHeader = !actions || i < 1 || column.showHeader;
                                    return <TableHead key={`th_${column.key}_${i}`}
                                                      className={`${column.key} ${column.class || ""} ${column.nonSortable ? "" : "sortable"} ${showHeader ? "" : "hide"}`}
                                                      onClick={() => !column.nonSortable && setSortedKey(column.key)}>
                                        <span className="th-content">
                                            {column.header}
                                            {column.toolTip && <InfoTooltip tip={column.toolTip}/>}
                                            {headerIcon(column, sorted, reverse)}
                                        </span>
                                    </TableHead>
                                })}
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {sortedEntities.map((entity, index) =>
                                entityRow(entity, index)
                            )}
                        </TableBody>
                    </Table>}
                {(!hasEntities && !customEmptySearch && !loading && !hideTitle && !busy) &&
                    <p className="no-entities">{customNoEntities || I18n.t(`${modelName}.noEntities`)}</p>}
                {renderPagination(totalElements || total)}
            </section>
        );
    };

    if (loading) {
        return <div className="loading-container"><Spinner className="size-8"/></div>;
    }
    const filteredEntities = filterEntities(query);
    const sortedEntities = customSearch ? filteredEntities : sortObjects(filteredEntities, sorted, reverse);
    return (
        <div className={`mod-entities ${className}`}>
            {displaySearch && renderSearch()}
            {renderEntities(sortedEntities)}
            <div>{children}</div>
        </div>);
}
