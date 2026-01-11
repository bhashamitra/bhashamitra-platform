import { useEffect, useState } from "react";

type EnabledLanguage = {
    code: string;
    name: string;
};

export function useEnabledLanguages() {
    const [languages, setLanguages] = useState<EnabledLanguage[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        setLoading(true);
        fetch("/api/admin/languages", {
            credentials: "include",
            headers: { Accept: "application/json" },
        })
            .then((res) => res.json())
            .then((data) => {
                const enabled = data
                    .filter((l: any) => l.enabled)
                    .map((l: any) => ({ code: l.code, name: l.name }));
                setLanguages(enabled);
            })
            .catch(() => {
                setLanguages([{ code: "mr", name: "Marathi" }]);
            })
            .finally(() => setLoading(false));
    }, []);

    return { languages, loading };
}