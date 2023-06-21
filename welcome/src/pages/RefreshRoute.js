import React, {useEffect} from "react";
import {useNavigate, useParams} from "react-router-dom";
import {Loader} from "@surfnet/sds";

const RefreshRoute = () => {

    const {path} = useParams();

    const navigate = useNavigate();

    useEffect(() => {
        const decodedPath = decodeURIComponent(path);
        navigate(decodedPath);
    }, [path, navigate]);

    return (
        <Loader/>
    );

}
export default RefreshRoute;
