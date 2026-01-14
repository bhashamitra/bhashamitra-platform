import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useEnabledLanguages } from "../../hooks/useEnabledLanguages";

const REGISTER_OPTIONS = [
    { value: "formal", label: "Formal" },
    { value: "neutral", label: "Neutral" },
    { value: "informal", label: "Informal" },
    { value: "colloquial", label: "Colloquial" },
];

export default function SentenceCreatePage() {
    const navigate = useNavigate();
    const { languages } = useEnabledLanguages();

    const [language, setLanguage] = useState("mr");
    const [sentenceNative, setSentenceNative] = useState("");
    const [sentenceLatin, setSentenceLatin] = useState("");
    const [translation, setTranslation] = useState("");
    const [register, setRegister] = useState("");
    const [explanation, setExplanation] = useState("");
    const [difficulty, setDifficulty] = useState<number | null>(null);

    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        
        if (!sentenceNative.trim()) {
            setError("Native sentence text is required.");
            return;
        }

        setSaving(true);
        setError(null);

        try {
            const body: any = {
                language,
                sentenceNative: sentenceNative.trim(),
            };
            
            if (sentenceLatin.trim()) body.sentenceLatin = sentenceLatin.trim();
            if (translation.trim()) body.translation = translation.trim();
            if (register) body.register = register;
            if (explanation.trim()) body.explanation = explanation.trim();
            if (difficulty !== null) body.difficulty = difficulty;

            const res = await fetch("/api/admin/sentences", {
                method: "POST",
                credentials: "include",
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
                setError("You are not authorized to create sentences.");
                return;
            }
            if (!res.ok) {
                const text = await res.text();
                throw new Error(text || `Failed to create sentence (${res.status})`);
            }

            const data = await res.json();
            // Success - redirect to edit page where user can add links
            navigate(`/admin/sentences/${data.id}/edit`);
        } catch (e: any) {
            setError(e?.message ?? "Failed to create sentence.");
        } finally {
            setSaving(false);
        }
    }

    return (
        <div className="mt-2">
            <div className="admin-page-header">
                <div>
                    <h1 className="h1">Create New Sentence</h1>
                    <p className="admin-page-subtitle">
                        Add a new usage sentence example.
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
                        Native Sentence Text *
                    </label>
                    <textarea
                        value={sentenceNative}
                        onChange={(e) => setSentenceNative(e.target.value)}
                        placeholder="e.g., मी तुम्हाला नमस्कार करतो."
                        rows={3}
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
                        value={sentenceLatin}
                        onChange={(e) => setSentenceLatin(e.target.value)}
                        placeholder="e.g., Mi tumhala namaskar karto."
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
                        placeholder="e.g., I greet you."
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
                        placeholder="Optional explanation or context..."
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
                        placeholder="1-5"
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
                        {saving ? "Creating..." : "Create Sentence"}
                    </button>
                    <Link
                        to="/admin/sentences"
                        className="admin-btn admin-btn-default"
                    >
                        Cancel
                    </Link>
                </div>
            </form>
        </div>
    );
}
