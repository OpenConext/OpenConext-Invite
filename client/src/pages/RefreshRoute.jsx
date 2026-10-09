import {Spinner} from "@surfnet/curve-react";
import React, {useEffect} from "react";
import {useNavigate, useParams} from "react-router";

const RefreshRoute = () => {

    const {path} = useParams();

    const navigate = useNavigate();

    useEffect(() => {
        const decodedPath = decodeURIComponent(path);
        navigate(decodedPath);
    }, [path, navigate]);

    return (
        <div className="loading-container"><Spinner className="size-8"/></div>
    );

}
export default RefreshRoute;
