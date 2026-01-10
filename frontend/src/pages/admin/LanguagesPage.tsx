import { useEffect, useMemo, useState } from "react";

type LanguageDto = {
    code: string;
    name: string;
    script: string;
    transliterationScheme: string;
    enabled: boolean;
};

type SortKey = "code" | "name" | "script" | "enabled";
type SortDir = "asc" | "desc";

function sortLabel(key: SortKey) {
    switch (key) {
        case "code":
            return "Code";
        case "name":
            return "Name";
        case "script":
            return "Script";
        case "enabled":
            return "Status";
    }
}

export default function LanguagesPage() {
    const [loading, setLoading] = useState(true);
    const [rows, setRows] = useState<LanguageDto[]>([]);
    const [error, setError] = useState<string | null>(null);

    const [sortKey, setSortKey] = useState<SortKey>("code");
    const [sortDir, setSortDir] = useState<SortDir>("asc");

    const [savingCode, setSavingCode] = useState<string | null>(null);

    async function loadLanguages() {
        setLoading(true);
        setError(null);

        try {
            const res = await fetch("/api/admin/languages", {
                credentials: "include",
                headers: { Accept: "application/json" },
            });

            if (res.status === 401) {
                setRows([]);
                setError("You are not signed in.");
                return;
            }
            if (res.status === 403) {
                setRows([]);
                setError("You are not authorized to view this page.");
                return;
            }
            if (!res.ok) {
                throw new Error(`Failed to load languages (${res.status})`);
            }

            const data = (await res.json()) as LanguageDto[];
            setRows(data);
        } catch (e: any) {
            setRows([]);
            setError(e?.message ?? "Failed to load languages.");
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadLanguages();
    }, []);

    function toggleSort(nextKey: SortKey) {
        if (sortKey === nextKey) {
            setSortDir((d) => (d === "asc" ? "desc" : "asc"));
        } else {
            setSortKey(nextKey);
            setSortDir("asc");
        }
    }

    const sorted = useMemo(() => {
        const copy = [...rows];

        copy.sort((a, b) => {
            const av =
                sortKey === "enabled"
                    ? a.enabled
                        ? "1"
                        : "0"
                    : String((a as any)[sortKey] ?? "");
            const bv =
                sortKey === "enabled"
                    ? b.enabled
                        ? "1"
                        : "0"
                    : String((b as any)[sortKey] ?? "");

            const cmp = av.localeCompare(bv, undefined, {
                numeric: true,
                sensitivity: "base",
            });

            return sortDir === "asc" ? cmp : -cmp;
        });

        return copy;
    }, [rows, sortKey, sortDir]);

    async function setEnabled(code: string, enabled: boolean) {
        // Requires backend endpoint:
        // PUT /api/admin/languages/{code}/enabled  body: { enabled: true|false }
        setSavingCode(code);
        setError(null);

        try {
            const res = await fetch(`/api/admin/languages/${encodeURIComponent(code)}/enabled`, {
                method: "PUT",
                credentials: "include",
                headers: { "Content-Type": "application/json", Accept: "application/json" },
                body: JSON.stringify({ enabled }),
            });

            if (res.status === 403) throw new Error("Not authorized to change language settings.");
            if (!res.ok) throw new Error(`Failed to update language (${res.status})`);

            setRows((prev) => prev.map((r) => (r.code === code ? { ...r, enabled } : r)));
        } catch (e: any) {
            setError(e?.message ?? "Failed to update language.");
        } finally {
            setSavingCode(null);
        }
    }

    const sortHint = `${sortLabel(sortKey)}${sortDir === "asc" ? " ↑" : " ↓"}`;

    return (
        <div className="mt-2">
            <div className="admin-page-header">
                <div>
                    <h1 className="h1">Languages</h1>
                    <p className="admin-page-subtitle">
                        Toggle which languages are enabled for the product.
                    </p>
                </div>

                <div className="flex items-center gap-2">
          <span className="text-sm text-slate-600">
            {loading ? "Loading…" : `${rows.length} total`} · Sorted by {sortHint}
          </span>

                    <button
                        type="button"
                        onClick={loadLanguages}
                        className="admin-btn admin-btn-default"
                    >
                        Refresh
                    </button>
                </div>
            </div>

            {error && (
                <div className="mt-3 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                    {error}
                </div>
            )}

            <div className="mt-4">
                <table className="admin-table">
                    <thead>
                    <tr>
                        <th>
                            <button
                                type="button"
                                onClick={() => toggleSort("code")}
                                className="admin-th-sort"
                            >
                                Code
                            </button>
                        </th>

                        <th>
                            <button
                                type="button"
                                onClick={() => toggleSort("name")}
                                className="admin-th-sort"
                            >
                                Name
                            </button>
                        </th>

                        <th>
                            <button
                                type="button"
                                onClick={() => toggleSort("script")}
                                className="admin-th-sort"
                            >
                                Script
                            </button>
                        </th>

                        <th>
                            <span>Transliteration</span>
                        </th>

                        <th>
                            <button
                                type="button"
                                onClick={() => toggleSort("enabled")}
                                className="admin-th-sort"
                            >
                                Status
                            </button>
                        </th>

                        <th className="text-right">
                            <span>Action</span>
                        </th>
                    </tr>
                    </thead>

                    <tbody>
                    {!loading &&
                        sorted.map((r) => {
                            const busy = savingCode === r.code;

                            const btnClass = [
                                "admin-btn",
                                r.enabled ? "admin-btn-default" : "admin-btn-primary",
                                busy ? "admin-btn-disabled" : "",
                            ]
                                .filter(Boolean)
                                .join(" ");

                            return (
                                <tr key={r.code}>
                                    <td className="font-mono text-slate-700">{r.code}</td>
                                    <td className="text-slate-800">{r.name}</td>
                                    <td className="text-slate-700">{r.script}</td>
                                    <td className="text-slate-700">{r.transliterationScheme}</td>

                                    <td>
                      <span className={r.enabled ? "pill-ok" : "pill-warn"}>
                        {r.enabled ? "Enabled" : "Disabled"}
                      </span>
                                    </td>

                                    <td className="text-right">
                                        <button
                                            type="button"
                                            disabled={busy}
                                            onClick={() => setEnabled(r.code, !r.enabled)}
                                            className={btnClass}
                                        >
                                            {busy ? "Saving…" : r.enabled ? "Disable" : "Enable"}
                                        </button>
                                    </td>
                                </tr>
                            );
                        })}

                    {!loading && sorted.length === 0 && (
                        <tr>
                            <td className="px-4 py-6 text-slate-600" colSpan={6}>
                                No languages found.
                            </td>
                        </tr>
                    )}

                    {loading && (
                        <tr>
                            <td className="px-4 py-6 text-slate-600" colSpan={6}>
                                Loading…
                            </td>
                        </tr>
                    )}
                    </tbody>
                </table>
            </div>

            <p className="mt-3 text-sm text-slate-500">Tip: click column headers to sort.</p>
        </div>
    );
}
