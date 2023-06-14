import React, {useState} from "react";
import I18n from "../locale/I18n";
import {ReactComponent as SearchIcon} from "@surfnet/sds/icons/functional-icons/search.svg";
import {isEmpty} from "../utils/Utils";
import {sortObjects, valueForSort} from "../utils/Sort";
import {headerIcon} from "../utils/Forms";
import "./Entities.scss";
import {Button, Loader} from "@surfnet/sds";

import {Pagination} from "@surfnet/sds";
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
                             pagination = true,
                             showActionsAlways,
                             displaySearch = true,
                             searchCallback,
                             customSearch,
                             entities,
                             searchAttributes,
                             newEntityPath,
                             newEntityFunc,
                             defaultSort,
                             busy = false
                         }) => {

    const [initial, setInitial] = useState(!isEmpty(customSearch));
    const [query, setQuery] = useState("");
    const [sorted, setSorted] = useState(defaultSort);
    const [reverse, setReverse] = useState(false);
    const [page, setPage] = useState(1);

    const navigate = useNavigate();

    const newEntity = () => {
        if (newEntityFunc) {
            newEntityFunc();
        } else {
            navigate(newEntityPath);
        }
    };

    const queryChanged = e => {
        const query = e.target.value;
        setQuery(query);
        if (customSearch) {
            customSearch(query);
            setInitial(false);
        }
        if (searchCallback) {
            searchCallback(filterEntities(entities, query, searchAttributes));
        }
    }

    const renderSearch = () => {
        const filterClassName = !hideTitle && filters ? "filters-with-title" : `${modelName}-search-filters`;
        return (
            <section className="entities-search">
                {showNew &&
                    <Button onClick={newEntity}
                            className={`${hideTitle && !filters ? "no-title" : ""}`}
                            txt={newLabel || I18n.t(`${modelName}.new`)}/>
                }
                {!hideTitle && <h2>{title || `${I18n.t(`${modelName}.title`)} (${entities.length})`}</h2>}
                <div className={filterClassName}>{filters}</div>
                <div className={`search ${showNew ? "" : "standalone"}`}>
                    {(!isEmpty(searchAttributes) || customSearch) &&
                        <div className={"sds--text-field sds--text-field--has-icon"}>
                            <div className="sds--text-field--shape">
                                <div className="sds--text-field--input-and-icon">
                                    <input className={"sds--text-field--input"}
                                           type="search"
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
            </section>
        );
    };

    const filterEntities = () => {
        if (isEmpty(query) || customSearch) {
            return entities;
        }
        const queryLower = query.toLowerCase();
        return entities.filter(entity => {
            return searchAttributes.some(attr => {
                const val = valueForSort(attr, entity);
                return val.toLowerCase().indexOf(queryLower) > -1
            });
        });

    };

    const setSortedKey = key => () => {
        const reversed = (sorted === key ? !reverse : false);
        setSorted(key);
        setReverse(reversed)
    }

    const getEntityValue = (entity, column) => {
        if (column.mapper) {
            return column.mapper(entity);
        }
        return entity[column.key];
    }

    const onRowClick = (entity) => e => {
        const hasValue = typeof rowLinkMapper === "function" && rowLinkMapper(entity);
        if (hasValue) {
            hasValue(entity)(e);
        }
    }

    const renderEntities = () => {
        const hasEntities = !isEmpty(entities);
        const customEmptySearch = customSearch && (isEmpty(query) || query.trim().length < 3);
        const total = entities.length;
        if (pagination) {
            const minimalPage = Math.min(page, Math.ceil(entities.length / pageCount));
            entities = entities.slice((minimalPage - 1) * pageCount, minimalPage * pageCount);
        }
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
                                    const showHeader = !actions || i < 2 || column.showHeader;
                                    return <th key={`th_${column.key}_${i}`}
                                               className={`${column.key} ${column.class || ""} ${column.nonSortable ? "" : "sortable"} ${showHeader ? "" : "hide"}`}
                                               onClick={setSortedKey(column.key)}>
                                        {column.header}
                                        {headerIcon(column, sorted, reverse)}
                                    </th>
                                })}
                            </tr>
                            </thead>
                            <tbody>
                            {entities.map((entity, index) =>
                                <tr key={`tr_${entity.id}_${index}`}
                                    className={`${(typeof rowLinkMapper === "function" && rowLinkMapper(entity)) ? "clickable" : ""} ${onHover ? "hoverable" : ""}`}>
                                    {columns.map((column, i) =>
                                        <td key={`td_${column.key}_${i}`}
                                            onClick={(column.key !== "check" && column.key !== "role" && !column.hasLink) ?
                                                onRowClick(rowLinkMapper, entity) : undefined}
                                            className={`${column.key} ${column.nonSortable ? "" : "sortable"} ${column.className ? column.className : ""}`}>
                                            {getEntityValue(entity, column)}
                                        </td>)}
                                </tr>
                            )}
                            </tbody>
                        </table>
                    </div>}
                {(!hasEntities && !children && !initial && !customEmptySearch) &&
                    <p className="no-entities">{customNoEntities || I18n.t(`${modelName}.noEntities`)}</p>}
                {pagination && <Pagination currentPage={page}
                                           onChange={nbr => setPage(nbr)}
                                           total={total}
                                           pageCount={pageCount}/>}
            </section>
        );
    };

    if (loading) {
        return <Loader/>;
    }
    const filteredEntities = filterEntities(entities, query, searchAttributes, customSearch);
    const sortedEntities = sortObjects(filteredEntities, sorted, reverse);
    console.log("busy: " + busy + "  length: " + filteredEntities.length)
    return (
        <div className={`mod-entities ${className}`}>
            {displaySearch && renderSearch()}
            {!busy && renderEntities(sortedEntities)}
            {busy && <Loader/>}
            <div>{children}</div>
        </div>);
}
