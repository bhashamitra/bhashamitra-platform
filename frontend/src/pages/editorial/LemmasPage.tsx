import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import type { LemmaDto, PagedLemmasResponse } from "../../types/lemma";
import { useEnabledLanguages } from "../../hooks/useEnabledLanguages";
import { POS_OPTIONS, STATUS_OPTIONS } from "../../constants/lemma";

type SortKey = "lemmaNative" | "lemmaLatin" | "language" | "pos" | "status";
type SortDir = "asc" | "desc";

const PAGE_SIZES = [20, 50, 100, 200];

export default function LemmasPage() {
    // Filter state
    const [search, setSearch] = useState("");
    const [language, setLanguage] = useState("mr");
    const [status, setStatus] = useState("");
    const [pos, setPos] = useState("");

    // Pagination state
    const [page, setPage] = useState(0);
    const [size, setSize] = useState(20);

    // Sort state
    const [sortKey, setSortKey] = useState<SortKey>("lemmaNative");
    const [sortDir, setSortDir] = useState<SortDir>("asc");

    // Data state
    const [data, setData] = useState<PagedLemmasResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // Languages for dropdown
    const { languages } = useEnabledLanguages();

    // Load lemmas
    useEffect(() => {
        loadLemmas();
    }, [search, language, status, pos, page, size, sortKey, sortDir]);

    async function loadLemmas() {
        setLoading(true);
        setError(null);

        const params = new URLSearchParams();
        if (search) params.set("search", search);
        params.set("language", language);
        if (status) params.set("status", status);
        if (pos) params.set("pos", pos);
        params.set("page", String(page));
        params.set("size", String(size));
        params.set("sort", sortKey);
        params.set("direction", sortDir);

        try {
            const res = await fetch(`/api/admin/lemmas?${params}`, {
                credentials: "include",
                headers: { Accept: "application/json" },
            });

            if (res.status === 401) {
                setData(null);
                setError("You are not signed in.");
                return;
            }
            if (res.status === 403) {
                setData(null);
                setError("You are not authorized to view this page.");
                return;
            }
            if (!res.ok) {
                throw new Error(`Failed to load lemmas (${res.status})`);
            }

            const json = (await res.json()) as PagedLemmasResponse;
            setData(json);
        } catch (e: any) {
            setData(null);
            setError(e?.message ?? "Failed to load lemmas.");
        } finally {
            setLoading(false);
        }
    }

    function handleSort(key: SortKey) {
        if (sortKey === key) {
            setSortDir((d) => (d === "asc" ? "desc" : "asc"));
        } else {
            setSortKey(key);
            setSortDir("asc");
        }
        setPage(0);
    }

    const sortIndicator = (key: SortKey) =>
        sortKey === key ? (sortDir === "asc" ? " ↑" : " ↓") : "";

    return (
        <div className="mt-2">
            <div className="admin-page-header">
                <div>
                    <h1 className="h1">Lemmas</h1>
                    <p className="admin-page-subtitle">
                        Manage dictionary entries for all languages.
                    </p>
                </div>
                <div className="flex items-center gap-2">
                    <span className="text-sm text-slate-600">
                        {loading ? "Loading…" : `${data?.totalElements ?? 0} total`} · Sorted by {sortKey}{sortDir === "asc" ? " ↑" : " ↓"}
                    </span>
                    <button
                        type="button"
                        onClick={loadLemmas}
                        className="admin-btn admin-btn-default"
                    >
                        Refresh
                    </button>
                    <Link to="/admin/lemmas/new" className="admin-btn admin-btn-primary">
                        Create New Lemma
                    </Link>
                </div>
            </div>

            {/* Filters */}
            <div className="mt-4 flex flex-wrap items-end gap-3">
                <input
                    type="text"
                    placeholder="Search native or latin..."
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    className="rounded-md border border-slate-300 px-3 py-1.5 text-sm"
                />

                <select
                    value={language}
                    onChange={(e) => { setLanguage(e.target.value); setPage(0); }}
                    className="rounded-md border border-slate-300 px-3 py-1.5 text-sm"
                >
                    {languages.map((l) => (
                        <option key={l.code} value={l.code}>{l.name}</option>
                    ))}
                </select>

                <select
                    value={status}
                    onChange={(e) => { setStatus(e.target.value); setPage(0); }}
                    className="rounded-md border border-slate-300 px-3 py-1.5 text-sm"
                >
                    <option value="">All Statuses</option>
                    {STATUS_OPTIONS.map((s) => (
                        <option key={s} value={s}>{s}</option>
                    ))}
                </select>

                <select
                    value={pos}
                    onChange={(e) => { setPos(e.target.value); setPage(0); }}
                    className="rounded-md border border-slate-300 px-3 py-1.5 text-sm"
                >
                    <option value="">All POS</option>
                    {POS_OPTIONS.map((p) => (
                        <option key={p} value={p}>{p}</option>
                    ))}
                </select>

                <select
                    value={size}
                    onChange={(e) => { setSize(Number(e.target.value)); setPage(0); }}
                    className="rounded-md border border-slate-300 px-3 py-1.5 text-sm"
                >
                    {PAGE_SIZES.map((s) => (
                        <option key={s} value={s}>{s} per page</option>
                    ))}
                </select>
            </div>

            {error && (
                <div className="mt-3 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                    {error}
                </div>
            )}

            {/* Table */}
            <div className="mt-4">
                <table className="admin-table">
                    <thead>
                        <tr>
                            <th>
                                <button type="button" onClick={() => handleSort("lemmaNative")} className="admin-th-sort">
                                    Native{sortIndicator("lemmaNative")}
                                </button>
                            </th>
                            <th>
                                <button type="button" onClick={() => handleSort("lemmaLatin")} className="admin-th-sort">
                                    Latin{sortIndicator("lemmaLatin")}
                                </button>
                            </th>
                            <th>
                                <button type="button" onClick={() => handleSort("language")} className="admin-th-sort">
                                    Lang{sortIndicator("language")}
                                </button>
                            </th>
                            <th>
                                <button type="button" onClick={() => handleSort("pos")} className="admin-th-sort">
                                    POS{sortIndicator("pos")}
                                </button>
                            </th>
                            <th>
                                <button type="button" onClick={() => handleSort("status")} className="admin-th-sort">
                                    Status{sortIndicator("status")}
                                </button>
                            </th>
                            <th>Related</th>
                        </tr>
                    </thead>
                    <tbody>
                        {!loading && data?.content.map((lemma) => (
                            <LemmaRow key={lemma.id} lemma={lemma} />
                        ))}
                        {!loading && (!data || data.content.length === 0) && (
                            <tr>
                                <td colSpan={6} className="px-4 py-6 text-slate-600">
                                    No lemmas found.
                                </td>
                            </tr>
                        )}
                        {loading && (
                            <tr>
                                <td colSpan={6} className="px-4 py-6 text-slate-600">
                                    Loading…
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>

            {/* Pagination */}
            {data && data.totalPages > 1 && (
                <div className="mt-4 flex items-center justify-between text-sm text-slate-600">
                    <span>
                        Showing {data.page * data.size + 1}–{Math.min((data.page + 1) * data.size, data.totalElements)} of {data.totalElements}
                    </span>
                    <div className="flex gap-2">
                        <button
                            type="button"
                            disabled={data.first}
                            onClick={() => setPage((p) => p - 1)}
                            className={`admin-btn admin-btn-default ${data.first ? "admin-btn-disabled" : ""}`}
                        >
                            Previous
                        </button>
                        <span className="px-2 py-1">
                            Page {data.page + 1} of {data.totalPages}
                        </span>
                        <button
                            type="button"
                            disabled={data.last}
                            onClick={() => setPage((p) => p + 1)}
                            className={`admin-btn admin-btn-default ${data.last ? "admin-btn-disabled" : ""}`}
                        >
                            Next
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}

function LemmaRow({ lemma }: { lemma: LemmaDto }) {
    const statusClass = {
        DRAFT: "pill-warn",
        REVIEW: "pill-info",
        PUBLISHED: "pill-ok",
        ARCHIVED: "pill-info",
    }[lemma.status] ?? "pill-info";

    const counts = lemma.counts;

    return (
        <tr>
            <td>
                <Link
                    to={`/admin/lemmas/${lemma.id}/edit`}
                    className="font-medium text-[var(--warriors-blue)] hover:underline"
                >
                    {lemma.lemmaNative}
                </Link>
            </td>
            <td className="text-slate-700">{lemma.lemmaLatin || "-"}</td>
            <td className="text-slate-700">{lemma.language}</td>
            <td className="text-slate-700">{lemma.pos || "-"}</td>
            <td>
                <span className={statusClass}>{lemma.status}</span>
            </td>
            <td className="text-slate-600 text-xs">
                {counts ? (
                    <span title="Meanings / Surface Forms / Sentences / Pronunciations">
                        {counts.meanings}M · {counts.surfaceForms}S · {counts.sentences}E · {counts.pronunciations}P
                    </span>
                ) : "-"}
            </td>
        </tr>
    );
}