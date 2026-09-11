import {createContext, useContext} from "react";

//Wrap a <Tabs> tree with a Provider so every sibling tab shares one persisted
//search query: switching tabs must carry the search text over, not clear it.
export const SearchGroupContext = createContext(null);

export const useSearchGroup = () => useContext(SearchGroupContext);
