import "./SearchField.scss";
import React from "react";
import {InputGroup, InputGroupAddon, InputGroupInput} from "@surfnet/curve-react";
import {MagnifyingGlassIcon as SearchIcon} from "@phosphor-icons/react";

export const SearchField = ({value, onChange, placeholder, inputRef, className = ""}) => (
    <InputGroup className={`search-field ${className}`}>
        <InputGroupInput type="search"
                         ref={inputRef}
                         value={value}
                         onChange={onChange}
                         placeholder={placeholder}/>
        <InputGroupAddon align="inline-end">
            <SearchIcon/>
        </InputGroupAddon>
    </InputGroup>
);
