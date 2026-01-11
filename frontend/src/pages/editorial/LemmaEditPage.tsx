import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { useEnabledLanguages } from "../../hooks/useEnabledLanguages";
import { POS_OPTIONS } from "../../constants/lemma";
import type { LemmaDto } from "../../types/lemma";
import MeaningsSection from "./components/MeaningsSection";

export default function LemmaEditPage() {
    const { id } = useParams<{ id: string }>();
    const { languages } = useEnabledLanguages();

    const [lemma, setLemma] = useState<LemmaDto | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // Form state
    const [language, setLanguage] = useState("");
    const [lemmaNative, setLemmaNative] = useState("");
    const [lemmaLatin, setLemmaLatin] = useState("");
    const [pos, setPos] = useState("");
    const [notes, setNotes] = useState("");

    const [saving, setSaving] = useState(false);
    const [statusChanging, setStatusChanging] = useState(false);

    // Load lemma
    useEffect(() => {
        if (!id) return;
        loadLemma();
    }, [id]);

    async function loadLemma() {
        setLoading(true);
        setError(null);

        try {
            const res = await fetch(`/api/admin/lemmas/${id}`, {
                credentials: "include",
                headers: { Accept: "application/json" },
            });

            if (res.status === 404) {
                setError("Lemma not found.");
                return;
            }
            if (!res.ok) {
                throw new Error(`Failed to load lemma (${res.status})`);
            }

            const data = (await res.json()) as LemmaDto;
            setLemma(data);
            setLanguage(data.language);
            setLemmaNative(data.lemmaNative);
            setLemmaLatin(data.lemmaLatin ?? "");
            setPos(data.pos ?? "");
            setNotes(data.notes ?? "");
        } catch (e: any) {
            setError(e?.message ?? "Failed to load lemma.");
        } finally {
            setLoading(false);
        }
    }

    async function handleSave(e: React.FormEvent) {
        e.preventDefault();

        if (!lemmaNative.trim()) {
            setError("Native text is required.");
            return;
        }

        setSaving(true);
        setError(null);

        try {
            const res = await fetch(`/api/admin/lemmas/${id}`, {
                method: "PUT",
                credentials: "include",
                headers: {
                    "Content-Type": "application/json",
                    Accept: "application/json",
                },
                body: JSON.stringify({
                    lemmaNative: lemmaNative.trim(),
                    lemmaLatin: lemmaLatin.trim() || null,
                    pos: pos || null,
                    notes: notes.trim() || null,
                }),
            });

            if (res.status === 401) {
                setError("You are not signed in.");
                return;
            }
            if (res.status === 403) {
                setError("You are not authorized to edit lemmas.");
                return;
            }
            if (!res.ok) {
                const text = await res.text();
                throw new Error(text || `Failed to save lemma (${res.status})`);
            }

            const updated = (await res.json()) as LemmaDto;
            setLemma(updated);
            setError(null);
        } catch (e: any) {
            setError(e?.message ?? "Failed to save lemma.");
        } finally {
            setSaving(false);
        }
    }

    async function handleStatusChange(newStatus: string) {
        setStatusChanging(true);
        setError(null);

        try {
            const res = await fetch(`/api/admin/lemmas/${id}/status?status=${newStatus}`, {
                method: "PUT",
                credentials: "include",
                headers: { Accept: "application/json" },
            });

            if (res.status === 401) {
                setError("You are not signed in.");
                return;
            }
            if (res.status === 403) {
                setError("You are not authorized to change status.");
                return;
            }
            if (!res.ok) {
                const text = await res.text();
                throw new Error(text || `Failed to change status (${res.status})`);
            }

            const updated = (await res.json()) as LemmaDto;
            setLemma(updated);
        } catch (e: any) {
            setError(e?.message ?? "Failed to change status.");
        } finally {
            setStatusChanging(false);
        }
    }

    // Get language name from code
    const languageName = languages.find((l) => l.code === language)?.name ?? language;

    // Status pill class
    function statusPillClass(status: string | null | undefined) {
        switch (status) {
            case "PUBLISHED": return "pill-ok";
            case "REVIEW": return "pill-warn";
            case "DRAFT": return "pill-info";
            case "ARCHIVED": return "pill-muted";
            default: return "pill-info";
        }
    }

    // Loading state
    if (loading) {
        return (
            <div className="mt-2">
                <p className="text-slate-500">Loading lemma...</p>
            </div>
        );
    }

    // Error / not found
    if (!lemma) {
        return (
            <div className="mt-2">
                <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                    {error ?? "Lemma not found."}
                </div>
                <Link to="/admin/lemmas" className="admin-btn admin-btn-default mt-4 inline-block">
                    ← Back to Lemmas
                </Link>
            </div>
        );
    }

    return (
        <div className="mt-2">
            <div className="admin-page-header">
                <div>
                    <h1 className="h1">Edit Lemma</h1>
                    <p className="admin-page-subtitle">
                        {lemma.lemmaNative} {lemma.lemmaLatin ? `(${lemma.lemmaLatin})` : ""}
                    </p>
                </div>
                <span className={`pill ${statusPillClass(lemma.status)}`}>
                    {lemma.status ?? "DRAFT"}
                </span>
            </div>

            {error && (
                <div className="mt-3 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                    {error}
                </div>
            )}

            {/* Status Actions */}
            <div className="mt-4 flex flex-wrap gap-2">
                {lemma.status === "DRAFT" && (
                    <button
                        onClick={() => handleStatusChange("REVIEW")}
                        disabled={statusChanging}
                        className="admin-btn admin-btn-default"
                    >
                        Submit for Review
                    </button>
                )}
                {lemma.status === "REVIEW" && (
                    <>
                        <button
                            onClick={() => handleStatusChange("PUBLISHED")}
                            disabled={statusChanging}
                            className="admin-btn admin-btn-primary"
                        >
                            Publish
                        </button>
                        <button
                            onClick={() => handleStatusChange("DRAFT")}
                            disabled={statusChanging}
                            className="admin-btn admin-btn-default"
                        >
                            Back to Draft
                        </button>
                    </>
                )}
                {lemma.status === "PUBLISHED" && (
                    <button
                        onClick={() => handleStatusChange("DRAFT")}
                        disabled={statusChanging}
                        className="admin-btn admin-btn-default"
                    >
                        Unpublish
                    </button>
                )}
                {lemma.status !== "ARCHIVED" && (
                    <button
                        onClick={() => handleStatusChange("ARCHIVED")}
                        disabled={statusChanging}
                        className="admin-btn admin-btn-danger"
                    >
                        Archive
                    </button>
                )}
                {lemma.status === "ARCHIVED" && (
                    <button
                        onClick={() => handleStatusChange("DRAFT")}
                        disabled={statusChanging}
                        className="admin-btn admin-btn-default"
                    >
                        Unarchive
                    </button>
                )}
            </div>

            {/* Edit Form */}
            <form onSubmit={handleSave} className="mt-6 max-w-lg space-y-4">
                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                        Language
                    </label>
                    <input
                        type="text"
                        value={languageName}
                        disabled
                        className="w-full rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-500"
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                        Native Text *
                    </label>
                    <input
                        type="text"
                        value={lemmaNative}
                        onChange={(e) => setLemmaNative(e.target.value)}
                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                        disabled={saving}
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                        Latin Transliteration
                    </label>
                    <input
                        type="text"
                        value={lemmaLatin}
                        onChange={(e) => setLemmaLatin(e.target.value)}
                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                        disabled={saving}
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                        Part of Speech
                    </label>
                    <select
                        value={pos}
                        onChange={(e) => setPos(e.target.value)}
                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                        disabled={saving}
                    >
                        <option value="">Select POS...</option>
                        {POS_OPTIONS.map((p) => (
                            <option key={p} value={p}>{p}</option>
                        ))}
                    </select>
                </div>

                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                        Notes
                    </label>
                    <textarea
                        value={notes}
                        onChange={(e) => setNotes(e.target.value)}
                        rows={3}
                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                        disabled={saving}
                    />
                </div>

                <div className="flex gap-3 pt-2">
                    <button
                        type="submit"
                        disabled={saving}
                        className={`admin-btn admin-btn-primary ${saving ? "admin-btn-disabled" : ""}`}
                    >
                        {saving ? "Saving..." : "Save Changes"}
                    </button>
                    <Link to="/admin/lemmas" className="admin-btn admin-btn-default">
                        Back to List
                    </Link>
                </div>
            </form>

            {/* Related Counts */}
            {lemma.counts && (
                <div className="mt-8 border-t border-slate-200 pt-4">
                    <h2 className="text-sm font-medium text-slate-700 mb-2">Related Content</h2>
                    <div className="flex gap-6 text-sm text-slate-600">
                        <span>Meanings: {lemma.counts.meanings}</span>
                        <span>Surface Forms: {lemma.counts.surfaceForms}</span>
                        <span>Pronunciations: {lemma.counts.pronunciations}</span>
                        <span>Sentences: {lemma.counts.sentences}</span>
                    </div>
                </div>
            )}

            {/* Meanings */}
            <MeaningsSection lemmaId={id!} />
        </div>
    );
}