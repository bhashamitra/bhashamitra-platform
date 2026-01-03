import { useEffect, useMemo, useState } from "react";

type MeResponse = {
    email?: string;
    username?: string;
    groups?: string[];
};

export default function App() {
    const [me, setMe] = useState<MeResponse | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const ac = new AbortController();

        async function loadMe() {
            try {
                const res = await fetch("/api/me", {
                    credentials: "include",
                    signal: ac.signal,
                    headers: { "Accept": "application/json" },
                });

                if (!res.ok) {
                    setMe(null);
                    return;
                }

                const data = (await res.json()) as MeResponse;
                setMe(data);
            } catch (err) {
                // Ignore abort errors; treat all else as "not logged in / unavailable"
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

    const displayName = me?.username || me?.email;

    return (
        <div className="min-h-screen bg-[var(--page-bg)] relative">
            {/* soft background wash */}
            <div
                className="absolute inset-0 opacity-10 pointer-events-none"
                style={{
                    background:
                        "radial-gradient(circle at 20% 20%, var(--warriors-gold), transparent 45%), radial-gradient(circle at 80% 30%, var(--warriors-blue), transparent 50%)",
                }}
            />

            {/* Top-right nav area */}
            <div className="absolute top-4 right-4 z-10">
                {!loading && isEditorOrAdmin && (
                    <a
                        href="/admin"
                        className="inline-flex items-center rounded-md border border-slate-200 bg-white/70 backdrop-blur px-3 py-2 text-sm font-semibold text-[var(--warriors-blue)] hover:bg-slate-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--warriors-blue)]"
                    >
                        Admin
                    </a>
                )}
                {me && (
                    <a
                        href="/logout"
                        className="inline-flex items-center rounded-md border px-3 py-2 text-sm font-semibold
                     bg-white/70 backdrop-blur
                     border-[var(--warriors-gold)] text-[var(--warriors-blue)]
                     hover:bg-[var(--warriors-gold)]/30"
                    >
                        Logout
                    </a>
                )}
            </div>

            {/* Landing content */}
            <div className="relative z-0 flex min-h-screen items-center justify-center px-4">
                <div className="w-full max-w-xl rounded-2xl border border-slate-200 bg-white/80 backdrop-blur p-8 shadow-sm text-center">
                    <h1 className="text-4xl sm:text-5xl font-extrabold text-[var(--warriors-blue)]">
                        Bhashamitra
                    </h1>

                    <p className="mt-4 text-base sm:text-lg text-slate-700">
                        Indian language learning, coming soon.
                    </p>

                    <div className="mt-6">
            <span className="inline-block rounded-full bg-[var(--warriors-gold)] px-5 py-2 text-sm font-semibold text-slate-900">
              Early access coming soon
            </span>
                    </div>

                    {/* tiny status line (optional) */}
                    <div className="mt-6 text-sm text-slate-500">
                        {loading ? (
                            <span>Checking session…</span>
                        ) : displayName ? (
                            <span>
                Signed in as <span className="font-semibold text-slate-700">{displayName}</span>
              </span>
                        ) : null}
                    </div>
                </div>
            </div>
        </div>
    );
}
