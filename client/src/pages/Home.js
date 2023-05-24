import {useAppStore} from "../stores/AppStore";

export const Home = () => {
    const user = useAppStore((state) => state.user)
    return (
        <div className="home">
            {JSON.stringify(user)}
        </div>
    );
}
