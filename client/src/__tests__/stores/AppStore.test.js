import {useAppStore} from "../../stores/AppStore";

test("Store outside functional component", () => {
    const csrfToken = useAppStore.getState().csrfToken;
    expect(csrfToken).toBeNull();

    useAppStore.setState({csrfToken: "test"});

    const updatedCsrfToken = useAppStore.getState().csrfToken;
    expect(updatedCsrfToken).toEqual("test");
});