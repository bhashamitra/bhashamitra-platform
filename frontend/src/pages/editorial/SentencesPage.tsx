import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Pencil } from "lucide-react";
import type { UsageSentenceDto, PagedSentencesResponse } from "../../types/sentence";
import { useEnabledLanguages } from "../../hooks/useEnabledLanguages";
import { apiFetch } from "../../utils/apiClient";

type SortKey = "sentenceNative" | "sentenceLatin" | "translation" | "status";
type SortDir = "asc" | "desc";

const STATUS_OPTIONS = ["DRAFT", "REVIEW", "PUBLISHED", "ARCHIVED"];
const PAGE_SIZES = [20, 50, 100, 200];

export default function SentencesPage() {
    const { languages } = useEnabledLanguages();

    // Filter state
    const [search, setSearch] = useState("");
    const [language, setLanguage] = useState("mr");
    const [status, setStatus] = useState("");

    // Pagination state (not fully supported by backend yet, but keeping for consistency)
    const [page, setPage] = useState(0);
    const [size, setSize] = useState(20);

    // Sort state
    const [sortKey, setSortKey] = useState<SortKey>("sentenceNative");
    const [sortDir, setSortDir] = useState<SortDir>("asc");

    // Data state
    const [data, setData] = useState<PagedSentencesResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // Links count cache
    const [linksCount, setLinksCount] = useState<Record<string, number>>({});

    // Load sentences
    useEffect(() => {
        loadSentences();
    }, [search, language, status, page, size, sortKey, sortDir]);

    // Load links count after sentences are loaded
    useEffect(() => {
        if (data && data.content.length > 0) {
            loadLinksCount();
        }
    }, [data]);

    async function loadSentences() {
        setLoading(true);
        setError(null);

        try {
            const params = new URLSearchParams();
            if (search) params.set("search", search);
            params.set("language", language);
            if (status) params.set("status", status);
            params.set("page", String(page));
            params.set("size", String(size));
            params.set("sort", sortKey);
            params.set("direction", sortDir);

            const res = await apiFetch(`/api/admin/sentences?${params}`, {
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
                throw new Error(`Failed to load sentences (${res.status})`);
            }

            const json = (await res.json()) as PagedSentencesResponse;
            setData(json);
        } catch (e: any) {
            setData(null);
            setError(e?.message ?? "Failed to load sentences.");
        } finally {
            setLoading(false);
        }
    }

    async function loadLinksCount() {
        if (!data || !data.content) return;
        
        try {
            const counts: Record<string, number> = {};
            await Promise.all(
                data.content.map(async (sentence) => {
                    try {
                        const res = await apiFetch(`/api/admin/lemma-sentence-links?sentenceId=${sentence.id}`, {
                            headers: { Accept: "application/json" },
                        });
                        if (res.ok) {
                            const links = await res.json();
                            counts[sentence.id] = links.length;
                        }
                    } catch {
                        counts[sentence.id] = 0;
                    }
                })
            );
            setLinksCount(counts);
        } catch {
            // Ignore errors in links count loading
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
                    <h1 className="h1">Sentences</h1>
                    <p className="admin-page-subtitle">
                        Manage usage sentence examples.
                    </p>
                </div>
                <div className="flex items-center gap-2">
                    <span className="text-sm text-slate-600">
                        {loading ? "Loading…" : `${data?.totalElements ?? 0} total`} · Sorted by {sortKey}{sortDir === "asc" ? " ↑" : " ↓"}
                    </span>
                    <button
                        type="button"
                        onClick={loadSentences}
                        className="admin-btn admin-btn-default"
                    >
                        Refresh
                    </button>
                    <Link to="/admin/sentences/new" className="admin-btn admin-btn-primary">
                        Create New Sentence
                    </Link>
                </div>
            </div>

            {/* Filters */}
            <div className="mt-4 flex flex-wrap items-end gap-3">
                <input
                    type="text"
                    placeholder="Search native, latin, or translation..."
                    value={search}
                    onChange={(e) => {
                        setSearch(e.target.value);
                        setPage(0);
                    }}
                    className="rounded-md border border-slate-300 px-3 py-1.5 text-sm"
                />

                <select
                    value={language}
                    onChange={(e) => {
                        setLanguage(e.target.value);
                        setPage(0);
                    }}
                    className="rounded-md border border-slate-300 px-3 py-1.5 text-sm"
                >
                    {languages.map((l) => (
                        <option key={l.code} value={l.code}>{l.name}</option>
                    ))}
                </select>

                <select
                    value={status}
                    onChange={(e) => {
                        setStatus(e.target.value);
                        setPage(0);
                    }}
                    className="rounded-md border border-slate-300 px-3 py-1.5 text-sm"
                >
                    <option value="">All Statuses</option>
                    {STATUS_OPTIONS.map((s) => (
                        <option key={s} value={s}>{s}</option>
                    ))}
                </select>

                <select
                    value={size}
                    onChange={(e) => {
                        setSize(Number(e.target.value));
                        setPage(0);
                    }}
                    className="rounded-md border border-slate-300 px-3 py-1.5 text-sm"
                >
                    {PAGE_SIZES.map((s) => (
                        <option key={s} value={s}>{s} per page</option>
                    ))}
                </select>
            </div>

            {error && (
                <div className="error-message mt-3">
                    {error}
                </div>
            )}

            {/* Table */}
            <div className="mt-4">
                <table className="admin-table">
                    <thead>
                        <tr>
                            <th className="w-32">Actions</th>
                            <th>
                                <button type="button" onClick={() => handleSort("sentenceNative")} className="admin-th-sort">
                                    Native{sortIndicator("sentenceNative")}
                                </button>
                            </th>
                            <th>
                                <button type="button" onClick={() => handleSort("sentenceLatin")} className="admin-th-sort">
                                    Latin{sortIndicator("sentenceLatin")}
                                </button>
                            </th>
                            <th>
                                <button type="button" onClick={() => handleSort("translation")} className="admin-th-sort">
                                    Translation{sortIndicator("translation")}
                                </button>
                            </th>
                            <th>Links</th>
                            <th>
                                <button type="button" onClick={() => handleSort("status")} className="admin-th-sort">
                                    Status{sortIndicator("status")}
                                </button>
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        {!loading && data?.content.map((sentence) => (
                            <SentenceRow key={sentence.id} sentence={sentence} linksCount={linksCount[sentence.id]} />
                        ))}
                        {!loading && (!data || data.content.length === 0) && (
                            <tr>
                                <td colSpan={6} className="px-4 py-6 text-slate-600">
                                    No sentences found.
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

function SentenceRow({ sentence, linksCount }: { sentence: UsageSentenceDto; linksCount?: number }) {
    const navigate = useNavigate();
    
    const statusClass = {
        DRAFT: "pill-info",
        REVIEW: "pill-warn",
        PUBLISHED: "pill-ok",
        ARCHIVED: "pill-muted",
    }[sentence.status || ""] ?? "pill-info";

    return (
        <tr>
            <td>
                <button
                    type="button"
                    className="action-btn action-btn-edit"
                    onClick={() => navigate(`/admin/sentences/${sentence.id}/edit`)}
                    title="Edit sentence"
                    aria-label="Edit sentence"
                >
                    <Pencil size={14} />
                </button>
            </td>
            <td>{sentence.sentenceNative}</td>
            <td className="text-slate-700">{sentence.sentenceLatin || "-"}</td>
            <td className="text-slate-700">{sentence.translation || "-"}</td>
            <td className="text-slate-600 text-xs">
                {linksCount !== undefined ? linksCount : "-"}
            </td>
            <td>
                <span className={statusClass}>{sentence.status || "DRAFT"}</span>
            </td>
        </tr>
    );
}
