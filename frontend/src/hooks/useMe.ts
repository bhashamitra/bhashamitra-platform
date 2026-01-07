import { useEffect, useMemo, useState } from "react";

export type MeResponse = {
    email?: string;
    username?: string;
    groups?: string[];
};

export function useMe() {
    const [me, setMe] = useState<MeResponse | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const ac = new AbortController();

        async function loadMe() {
            try {
                const res = await fetch("/api/me", {
                    credentials: "include",
                    signal: ac.signal,
                    headers: { Accept: "application/json" },
                });

                if (!res.ok) {
                    setMe(null);
                    return;
                }

                const data = (await res.json()) as MeResponse;
                setMe(data);
            } catch (err) {
                if ((err as any)?.name !== "AbortError") {
                    setMe(null);
                }
            } finally {
                setLoading(false);
            }
        }

        loadMe();
        return () => ac.abort();
    }, []);

    const isEditorOrAdmin = useMemo(() => {
        const groups = me?.groups ?? [];
        return groups.includes("admin") || groups.includes("editor");
    }, [me]);

    const displayName =   me?.email || me?.username;

    return {
        me,
        loading,
        isEditorOrAdmin,
        displayName,
    };
}
