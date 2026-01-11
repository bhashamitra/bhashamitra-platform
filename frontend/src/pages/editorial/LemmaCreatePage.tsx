import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useEnabledLanguages } from "../../hooks/useEnabledLanguages";
import { POS_OPTIONS } from "../../constants/lemma";

export default function LemmaCreatePage() {
    const navigate = useNavigate();
    const { languages } = useEnabledLanguages();

    const [language, setLanguage] = useState("mr");
    const [lemmaNative, setLemmaNative] = useState("");
    const [lemmaLatin, setLemmaLatin] = useState("");
    const [pos, setPos] = useState("");
    const [notes, setNotes] = useState("");

    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        
        if (!lemmaNative.trim()) {
            setError("Native text is required.");
            return;
        }

        setSaving(true);
        setError(null);

        try {
            const res = await fetch("/api/admin/lemmas", {
                method: "POST",
                credentials: "include",
                headers: {
                    "Content-Type": "application/json",
                    Accept: "application/json",
                },
                body: JSON.stringify({
                    language,
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
                setError("You are not authorized to create lemmas.");
                return;
            }
            if (!res.ok) {
                const text = await res.text();
                throw new Error(text || `Failed to create lemma (${res.status})`);
            }

            // Success - redirect to list
            navigate("/admin/lemmas");
        } catch (e: any) {
            setError(e?.message ?? "Failed to create lemma.");
        } finally {
            setSaving(false);
        }
    }

    return (
        <div className="mt-2">
            <div className="admin-page-header">
                <div>
                    <h1 className="h1">Create New Lemma</h1>
                    <p className="admin-page-subtitle">
                        Add a new dictionary entry.
                    </p>
                </div>
            </div>

            {error && (
                <div className="mt-3 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                    {error}
                </div>
            )}

            <form onSubmit={handleSubmit} className="mt-6 max-w-lg space-y-4">
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
                        Native Text *
                    </label>
                    <input
                        type="text"
                        value={lemmaNative}
                        onChange={(e) => setLemmaNative(e.target.value)}
                        placeholder="e.g., नमस्कार"
                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                        disabled={saving}
                        autoFocus
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
                        placeholder="e.g., namaskar"
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
                        placeholder="Optional notes about this lemma..."
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
                        {saving ? "Creating..." : "Create Lemma"}
                    </button>
                    <Link
                        to="/admin/lemmas"
                        className="admin-btn admin-btn-default"
                    >
                        Cancel
                    </Link>
                </div>
            </form>
        </div>
    );
}