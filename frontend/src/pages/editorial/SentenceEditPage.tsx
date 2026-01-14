import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { useEnabledLanguages } from "../../hooks/useEnabledLanguages";
import type { UsageSentenceDto } from "../../types/sentence";
import SentenceLinksSection from "./components/SentenceLinksSection";
import SentencePronunciationsSection from "./components/SentencePronunciationsSection";
import { apiFetch } from "../../utils/apiClient";

const REGISTER_OPTIONS = [
    { value: "formal", label: "Formal" },
    { value: "neutral", label: "Neutral" },
    { value: "informal", label: "Informal" },
    { value: "colloquial", label: "Colloquial" },
];

export default function SentenceEditPage() {
    const { id } = useParams<{ id: string }>();
    const { languages } = useEnabledLanguages();

    const [sentence, setSentence] = useState<UsageSentenceDto | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // Form state
    const [language, setLanguage] = useState("");
    const [sentenceNative, setSentenceNative] = useState("");
    const [sentenceLatin, setSentenceLatin] = useState("");
    const [translation, setTranslation] = useState("");
    const [register, setRegister] = useState("");
    const [explanation, setExplanation] = useState("");
    const [difficulty, setDifficulty] = useState<number | null>(null);

    const [saving, setSaving] = useState(false);
    const [statusChanging, setStatusChanging] = useState(false);

    // Load sentence
    useEffect(() => {
        if (!id) return;
        loadSentence();
    }, [id]);

    async function loadSentence() {
        setLoading(true);
        setError(null);

        try {
            const res = await apiFetch(`/api/admin/sentences/${id}`, {
                headers: { Accept: "application/json" },
            });

            if (res.status === 404) {
                setError("Sentence not found.");
                return;
            }
            if (!res.ok) {
                throw new Error(`Failed to load sentence (${res.status})`);
            }

            const data = (await res.json()) as UsageSentenceDto;
            setSentence(data);
            setLanguage(data.language);
            setSentenceNative(data.sentenceNative);
            setSentenceLatin(data.sentenceLatin ?? "");
            setTranslation(data.translation ?? "");
            setRegister(data.register ?? "");
            setExplanation(data.explanation ?? "");
            setDifficulty(data.difficulty ?? null);
        } catch (e: any) {
            setError(e?.message ?? "Failed to load sentence.");
        } finally {
            setLoading(false);
        }
    }

    async function handleSave(e: React.FormEvent) {
        e.preventDefault();

        if (!sentenceNative.trim()) {
            setError("Native sentence text is required.");
            return;
        }

        setSaving(true);
        setError(null);

        try {
            const body: any = {
                sentenceNative: sentenceNative.trim(),
            };
            
            if (language) body.language = language;
            if (sentenceLatin.trim()) body.sentenceLatin = sentenceLatin.trim();
            if (translation.trim()) body.translation = translation.trim();
            if (register) body.register = register;
            if (explanation.trim()) body.explanation = explanation.trim();
            if (difficulty !== null) body.difficulty = difficulty;

            const res = await apiFetch(`/api/admin/sentences/${id}`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    Accept: "application/json",
                },
                body: JSON.stringify(body),
            });

            if (res.status === 401) {
                setError("You are not signed in.");
                return;
            }
            if (res.status === 403) {
                setError("You are not authorized to edit sentences.");
                return;
            }
            if (!res.ok) {
                const text = await res.text();
                throw new Error(text || `Failed to save sentence (${res.status})`);
            }

            const updated = (await res.json()) as UsageSentenceDto;
            setSentence(updated);
            setError(null);
        } catch (e: any) {
            setError(e?.message ?? "Failed to save sentence.");
        } finally {
            setSaving(false);
        }
    }

    async function handleStatusChange(newStatus: string) {
        setStatusChanging(true);
        setError(null);

        try {
            const res = await apiFetch(`/api/admin/sentences/${id}/status?status=${newStatus}`, {
                method: "PUT",
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

            const updated = (await res.json()) as UsageSentenceDto;
            setSentence(updated);
        } catch (e: any) {
            setError(e?.message ?? "Failed to change status.");
        } finally {
            setStatusChanging(false);
        }
    }

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
                <p className="text-slate-500">Loading sentence...</p>
            </div>
        );
    }

    // Error / not found
    if (!sentence) {
        return (
            <div className="mt-2">
                <div className="error-message">
                    {error ?? "Sentence not found."}
                </div>
                <Link to="/admin/sentences" className="admin-btn admin-btn-default mt-4 inline-block">
                    ← Back to Sentences
                </Link>
            </div>
        );
    }

    return (
        <div className="mt-2">
            <div className="admin-page-header">
                <div>
                    <h1 className="h1">Edit Sentence</h1>
                    <p className="admin-page-subtitle">
                        {sentence.sentenceNative}
                    </p>
                </div>
                <span className={`pill ${statusPillClass(sentence.status)}`}>
                    {sentence.status ?? "DRAFT"}
                </span>
            </div>

            {error && (
                <div className="error-message mt-3">
                    {error}
                </div>
            )}

            {/* Status Actions */}
            {sentence && (
                <div className="mt-4 flex flex-wrap gap-2">
                    {sentence.status === "DRAFT" && (
                        <button
                            onClick={() => handleStatusChange("REVIEW")}
                            disabled={statusChanging}
                            className="admin-btn admin-btn-default"
                        >
                            Submit for Review
                        </button>
                    )}
                    {sentence.status === "REVIEW" && (
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
                    {sentence.status === "PUBLISHED" && (
                        <button
                            onClick={() => handleStatusChange("DRAFT")}
                            disabled={statusChanging}
                            className="admin-btn admin-btn-default"
                        >
                            Unpublish
                        </button>
                    )}
                    {sentence.status !== "ARCHIVED" && (
                        <button
                            onClick={() => handleStatusChange("ARCHIVED")}
                            disabled={statusChanging}
                            className="admin-btn admin-btn-danger"
                        >
                            Archive
                        </button>
                    )}
                    {sentence.status === "ARCHIVED" && (
                        <button
                            onClick={() => handleStatusChange("DRAFT")}
                            disabled={statusChanging}
                            className="admin-btn admin-btn-default"
                        >
                            Unarchive
                        </button>
                    )}
                </div>
            )}

            <form onSubmit={handleSave} className="mt-6 max-w-lg space-y-4">
                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                        Language *
                    </label>
                    <select
                        value={language}
                        onChange={(e) => setLanguage(e.target.value)}
                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                        disabled={saving}
                    >
                        {languages.map((l) => (
                            <option key={l.code} value={l.code}>{l.name}</option>
                        ))}
                    </select>
                </div>

                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                        Native Sentence Text *
                    </label>
                    <textarea
                        value={sentenceNative}
                        onChange={(e) => setSentenceNative(e.target.value)}
                        rows={3}
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
                        value={sentenceLatin}
                        onChange={(e) => setSentenceLatin(e.target.value)}
                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                        disabled={saving}
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                        Translation
                    </label>
                    <input
                        type="text"
                        value={translation}
                        onChange={(e) => setTranslation(e.target.value)}
                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                        disabled={saving}
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                        Register
                    </label>
                    <select
                        value={register}
                        onChange={(e) => setRegister(e.target.value)}
                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                        disabled={saving}
                    >
                        <option value="">Select register...</option>
                        {REGISTER_OPTIONS.map((r) => (
                            <option key={r.value} value={r.value}>{r.label}</option>
                        ))}
                    </select>
                </div>

                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                        Explanation
                    </label>
                    <textarea
                        value={explanation}
                        onChange={(e) => setExplanation(e.target.value)}
                        rows={2}
                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                        disabled={saving}
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                        Difficulty (1-5)
                    </label>
                    <input
                        type="number"
                        min="1"
                        max="5"
                        value={difficulty ?? ""}
                        onChange={(e) => setDifficulty(e.target.value ? parseInt(e.target.value) : null)}
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
                    <Link to="/admin/sentences" className="admin-btn admin-btn-default">
                        Back to List
                    </Link>
                </div>
            </form>

            {/* Pronunciations Section */}
            {sentence && (
                <SentencePronunciationsSection sentenceId={id!} sentenceStatus={sentence.status} />
            )}

            {/* Links Section */}
            {sentence && (
                <SentenceLinksSection sentenceId={id!} sentenceStatus={sentence.status} />
            )}
        </div>
    );
}
