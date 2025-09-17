import React, {useEffect, useRef, useState} from "react";
import I18n from "../locale/I18n";
import {ReactComponent as SearchIcon} from "@surfnet/sds/icons/functional-icons/search.svg";
import {isEmpty} from "../utils/Utils";
import {sortObjects, valueForSort} from "../utils/Sort";
import {headerIcon} from "../utils/Forms";
import "./Entities.scss";
import {Button, Loader, Pagination, Tooltip} from "@surfnet/sds";
import {pageCount} from "../utils/Pagination";
import {useNavigate} from "react-router-dom";

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

    const [initial, setInitial] = useState(!isEmpty(customSearch));
    const [query, setQuery] = useState("");
    const [sorted, setSorted] = useState(defaultSort);
    const [reverse, setReverse] = useState(false);
    const [page, setPage] = useState(1);

    const searchRef = useRef();
    const navigate = useNavigate();

    useEffect(() => {
        if ((displaySearch || inputFocus) && searchRef && searchRef.current) {
            searchRef.current.focus();
        }
    }, [displaySearch, inputFocus])

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
        //When the user changes the query text we reset the page number
        const queryChanged = currentQuery !== newQuery;
        if (queryChanged) {
            setPage(1);
        }
        callCustomSearch(newQuery, sorted, reverse, queryChanged ? 1 : page);
    }

    const renderSearch = () => {
        const filterClassName = (!hideTitle && filters) ? "filters-with-title" : `${modelName}-search-filters`;
        return (
            <section className="entities-search">
                {(!hideTitle) &&
                    <h2>{title || `${I18n.t(`${modelName}.title`)} (${(totalElements || entities.length).toLocaleString()})`}</h2>}
                {(loading || hideTitle) && <Loader/>}
                {!isEmpty(filters) && <div className={`${filterClassName} search-filter`}>{filters}</div>}
                <div className={`search ${showNew ? "" : "standalone"}`}>
                    {(!isEmpty(searchAttributes) || customSearch) &&
                        <div className={"sds--text-field sds--text-field--has-icon"}>
                            <div className="sds--text-field--shape">
                                <div className="sds--text-field--input-and-icon">
                                    <input className={"sds--text-field--input"}
                                           type="search"
                                           ref={searchRef}
                                           onChange={queryChanged}
                                           value={query}
                                           placeholder={I18n.t(`${modelName}.searchPlaceHolder`)}/>
                                    <span className="sds--text-field--icon">
                                    <SearchIcon/>
                                </span>
                                </div>
                            </div>
                        </div>}
                </div>
                {showNew &&
                    <Button onClick={newEntity}
                            className={`${hideTitle && !filters ? "no-title" : ""}`}
                            txt={newLabel || I18n.t(`${modelName}.new`)}/>
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
        setReverse(newReserve);
        callCustomSearch(query, key, newReserve, page);
    }

    const callCustomSearch = (newQuery, newSorted, newReversed, newPage) => {
        if (customSearch) {
            //Adjust page, as serverSide is zero-based
            customSearch(newQuery, newSorted, newReversed, newPage - 1);
            setInitial(false);
        }
        if (searchCallback) {
            const searchResult = filterEntities(query);
            searchCallback(searchResult);
        }

    }

    const getEntityValue = (entity, column) => {
        if (column.mapper) {
            return column.mapper(entity);
        }
        return entity[column.key];
    }

    const onRowClick = (e, entity) => {
        if (typeof rowLinkMapper === "function") {
            rowLinkMapper(e, entity);
        }
    }

    const entityRow = (entity, index) => {
        const additionalClassName = isEmpty(rowClassNameResolver) ? "" : rowClassNameResolver(entity);
        return <tr key={`tr_${entity.id}_${index}`}
                   className={`${typeof rowLinkMapper === "function" ? "clickable" : ""} ${onHover ? "hoverable" : ""} ${additionalClassName}`}>
            {columns.map((column, i) =>
                <td key={`td_${column.key}_${i}`}
                    onClick={e => (column.key !== "check" && !column.hasLink) ?
                        onRowClick(e, entity) : undefined}
                    data-label={typeof column === "string" ? column.header : ""}
                    className={`${column.key} ${column.nonSortable ? "" : "sortable"} ${column.className ? column.className : ""}`}>
                    {getEntityValue(entity, column)}
                </td>)}
        </tr>;
    }

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
                    <div className={"sds--table"}>
                        <table className={tableClassName || modelName}>
                            <thead>
                            <tr>
                                {columns.map((column, i) => {
                                    const showHeader = !actions || i < 1 || column.showHeader;
                                    return <th key={`th_${column.key}_${i}`}
                                               className={`${column.key} ${column.class || ""} ${column.nonSortable ? "" : "sortable"} ${showHeader ? "" : "hide"}`}
                                               onClick={() => !column.nonSortable && setSortedKey(column.key)}>
                                        {column.header}
                                        {column.toolTip && <Tooltip tip={column.toolTip}/>}
                                        {headerIcon(column, sorted, reverse)}
                                    </th>
                                })}
                            </tr>
                            </thead>
                            <tbody>
                            {sortedEntities.map((entity, index) =>
                                entityRow(entity, index)
                            )}
                            </tbody>
                        </table>
                    </div>}
                {(!hasEntities && !initial && !customEmptySearch && !loading && !hideTitle && !busy) &&
                    <p className="no-entities">{customNoEntities || I18n.t(`${modelName}.noEntities`)}</p>}
                <Pagination currentPage={page}
                            onChange={nbr => {
                                setPage(nbr);
                                callCustomSearch(query, sorted, reverse, nbr);
                            }}
                            total={totalElements || total}
                            pageCount={pageCount}/>
            </section>
        );
    };

    if (loading) {
        return <Loader/>;
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
