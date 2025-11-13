import { describe, it, expect } from 'vitest';
import {useAppStore} from "../../stores/AppStore";

describe('AppStore', () => {
    it("Store outside functional component", () => {
        const csrfToken = useAppStore.getState().csrfToken;
        expect(csrfToken).toBeNull();

        useAppStore.setState({csrfToken: "test"});

        const updatedCsrfToken = useAppStore.getState().csrfToken;
        expect(updatedCsrfToken).toEqual("test");
    });
});