import { useEffect, useState } from "react";

type MeResponse = {
    email?: string;
    username?: string;
    groups?: string[];
};

export default function App() {
    const [me, setMe] = useState<MeResponse | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let cancelled = false;

        async function loadMe() {
            try {
                // IMPORTANT: same-origin cookie session/JWT is handled by Spring Boot.
                // This call just asks the backend who the user is.
                const res = await fetch("/api/me", { credentials: "include" });

                if (!res.ok) {
                    // not logged in (or backend says no)
                    if (!cancelled) setMe(null);
                    return;
                }

                const data = (await res.json()) as MeResponse;
                if (!cancelled) setMe(data);
            } catch {
                if (!cancelled) setMe(null);
            } finally {
                if (!cancelled) setLoading(false);
            }
        }

        loadMe();
        return () => {
            cancelled = true;
        };
    }, []);

    const groups = me?.groups ?? [];
    const isEditorOrAdmin = groups.includes("admin") || groups.includes("editor");

    return (
        <div className="min-h-screen bg-[var(--page-bg)] flex items-center justify-center relative">
            {/* Top-right nav area */}
            <div className="absolute top-4 right-4">
                {!loading && isEditorOrAdmin && (
                    <a
                        href="/admin"
                        className="inline-flex items-center rounded-md border border-slate-200 px-3 py-2 text-sm font-semibold text-[var(--warriors-blue)] hover:bg-slate-50"
                    >
                        Admin
                    </a>
                )}
            </div>

            {/* Existing landing page content */}
            <div className="text-center px-4">
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
            </div>
        </div>
    );
}
