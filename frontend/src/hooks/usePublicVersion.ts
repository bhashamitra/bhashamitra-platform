import { useEffect, useState } from "react";
import type { PublicVersion } from "../types/publicVersion";

export function usePublicVersion() {
    const [version, setVersion] = useState<PublicVersion | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const ac = new AbortController();

        async function loadVersion() {
            try {
                const res = await fetch("/api/public/version", {
                    signal: ac.signal,
                    headers: { Accept: "application/json" },
                });

                if (!res.ok) throw new Error("Failed to load version");

                const data = (await res.json()) as PublicVersion;
                setVersion(data);
            } catch (err: any) {
                if (err?.name !== "AbortError") setVersion(null);
            } finally {
                setLoading(false);
            }
        }

        loadVersion();
        return () => ac.abort();
    }, []);

    return { version, loading };
}
