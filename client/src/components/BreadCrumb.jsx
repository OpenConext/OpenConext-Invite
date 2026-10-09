import React from "react";
import "./BreadCrumb.scss";
import {useAppStore} from "../stores/AppStore";
import {Link} from "react-router";
import {isEmpty} from "../utils/Utils";
import DOMPurify from "dompurify";
import {
    Breadcrumb,
    BreadcrumbItem,
    BreadcrumbLink,
    BreadcrumbList,
    BreadcrumbPage,
    BreadcrumbSeparator
} from "@surfnet/curve-react";
import {HouseIcon} from "@phosphor-icons/react";

export const BreadCrumb = () => {

    const paths = useAppStore(state => state.breadcrumbPath);
    const clearFlash = useAppStore(state => state.clearFlash);

    const items = (paths || []).filter(p => !isEmpty(p));
    if (isEmpty(items)) {
        return <div/>;
    }

    const label = p => <span dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(p.value)}}/>;

    return (
        <Breadcrumb aria-label="breadcrumbs">
            <BreadcrumbList>
                {items.map((p, i) => {
                    const isLast = i === items.length - 1;
                    const isHome = i === 0 && p.path === "/home";
                    return (
                        <React.Fragment key={i}>
                            {i !== 0 && <BreadcrumbSeparator/>}
                            <BreadcrumbItem>
                                {(!isLast && p.path) ?
                                    <BreadcrumbLink render={<Link to={p.path} onClick={() => clearFlash()}/>}>
                                        {isHome ?
                                            <HouseIcon aria-label={DOMPurify.sanitize(p.value)}/> : label(p)}
                                    </BreadcrumbLink> :
                                    <BreadcrumbPage>{label(p)}</BreadcrumbPage>}
                            </BreadcrumbItem>
                        </React.Fragment>
                    );
                })}
            </BreadcrumbList>
        </Breadcrumb>
    );
}
