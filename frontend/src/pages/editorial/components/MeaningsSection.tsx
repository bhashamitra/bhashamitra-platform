import { useEffect, useState } from "react";
import { Trash2, Pencil } from "lucide-react";

export interface MeaningDto {
    id: string;
    lemmaId: string;
    meaningLanguage: string;
    meaningText: string;
    priority: number;
}

interface MeaningsSectionProps {
    lemmaId: string;
    lemmaStatus: string | null;
}

// Common meaning languages (typically English or Hindi for definitions)
const MEANING_LANGUAGES = [
    { code: "en", name: "English" },
    { code: "hi", name: "Hindi" },
];

interface MeaningFormData {
    meaningLanguage: string;
    meaningText: string;
    priority: number;
}

export default function MeaningsSection({ lemmaId, lemmaStatus }: MeaningsSectionProps) {
    const [meanings, setMeanings] = useState<MeaningDto[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    
    // Modal state
    const [showModal, setShowModal] = useState(false);
    const [editingMeaning, setEditingMeaning] = useState<MeaningDto | null>(null);
    const [saving, setSaving] = useState(false);
    
    // Form state
    const [formData, setFormData] = useState<MeaningFormData>({
        meaningLanguage: "en",
        meaningText: "",
        priority: 1,
    });

    useEffect(() => {
        loadMeanings();
    }, [lemmaId]);

    async function loadMeanings() {
        setLoading(true);
        setError(null);
        try {
            const res = await fetch(`/api/admin/meanings?lemmaId=${lemmaId}`, {
                credentials: "include",
                headers: { Accept: "application/json" },
            });
            if (res.ok) {
                const data = await res.json();
                setMeanings(data);
            } else if (res.status === 401 || res.status === 403) {
                setError("You are not authorized to view meanings.");
            }
        } catch (e: any) {
            setError("Failed to load meanings.");
        } finally {
            setLoading(false);
        }
    }

    function openCreateModal() {
        setEditingMeaning(null);
        setFormData({
            meaningLanguage: "en",
            meaningText: "",
            priority: 1,
        });
        setError(null);
        setShowModal(true);
    }

    function openEditModal(meaning: MeaningDto) {
        setEditingMeaning(meaning);
        setFormData({
            meaningLanguage: meaning.meaningLanguage,
            meaningText: meaning.meaningText,
            priority: meaning.priority,
        });
        setError(null);
        setShowModal(true);
    }

    function closeModal() {
        setShowModal(false);
        setEditingMeaning(null);
        setError(null);
    }

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        
        if (!formData.meaningText.trim()) {
            setError("Meaning text is required.");
            return;
        }

        setSaving(true);
        setError(null);

        try {
            const url = editingMeaning
                ? `/api/admin/meanings/${editingMeaning.id}`
                : `/api/admin/meanings`;
            
            const method = editingMeaning ? "PUT" : "POST";
            
            const body = editingMeaning
                ? {
                      meaningLanguage: formData.meaningLanguage,
                      meaningText: formData.meaningText.trim(),
                      priority: formData.priority,
                  }
                : {
                      lemmaId: lemmaId,
                      meaningLanguage: formData.meaningLanguage,
                      meaningText: formData.meaningText.trim(),
                      priority: formData.priority,
                  };

            const res = await fetch(url, {
                method,
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
                setError("You are not authorized to modify meanings.");
                return;
            }
            if (!res.ok) {
                const text = await res.text();
                throw new Error(text || `Failed to ${editingMeaning ? "update" : "create"} meaning (${res.status})`);
            }

            // Success - reload meanings and close modal
            await loadMeanings();
            closeModal();
        } catch (e: any) {
            setError(e?.message ?? `Failed to ${editingMeaning ? "update" : "create"} meaning.`);
        } finally {
            setSaving(false);
        }
    }

    async function handleDelete(meaningId: string) {
        const isPublished = lemmaStatus === "PUBLISHED";
        const confirmMessage = isPublished
            ? "This lemma is published. Are you sure you want to delete this meaning?"
            : "Are you sure you want to delete this meaning?";
        
        if (!confirm(confirmMessage)) {
            return;
        }

        setError(null);
        try {
            const res = await fetch(`/api/admin/meanings/${meaningId}`, {
                method: "DELETE",
                credentials: "include",
                headers: { Accept: "application/json" },
            });

            if (res.status === 401) {
                setError("You are not signed in.");
                return;
            }
            if (res.status === 403) {
                setError("You are not authorized to delete meanings.");
                return;
            }
            if (!res.ok) {
                throw new Error(`Failed to delete meaning (${res.status})`);
            }

            // Success - reload meanings
            await loadMeanings();
        } catch (e: any) {
            setError(e?.message ?? "Failed to delete meaning.");
        }
    }

    // Calculate next priority (highest + 1, or 1 if empty)
    function getNextPriority() {
        if (meanings.length === 0) return 1;
        const maxPriority = Math.max(...meanings.map((m) => m.priority));
        return maxPriority + 1;
    }

    return (
        <>
            <div className="mt-8 border-t border-slate-200 pt-4">
                <div className="flex items-center justify-between mb-3">
                    <h2 className="text-sm font-medium text-slate-700">Meanings</h2>
                    <button
                        type="button"
                        className="admin-btn admin-btn-default text-xs"
                        onClick={openCreateModal}
                    >
                        + Add Meaning
                    </button>
                </div>

                {error && !showModal && (
                    <div className="error-message mb-3">
                        {error}
                    </div>
                )}

                {loading ? (
                    <p className="text-sm text-slate-500">Loading meanings...</p>
                ) : meanings.length === 0 ? (
                    <p className="text-sm text-slate-500">No meanings yet.</p>
                ) : (
                    <table className="admin-table text-sm">
                        <thead>
                            <tr>
                                <th className="w-32">Actions</th>
                                <th>Meaning</th>
                                <th>Language</th>
                                <th>Priority</th>
                            </tr>
                        </thead>
                        <tbody>
                            {meanings.map((m) => (
                                <tr key={m.id}>
                                    <td>
                                        <div className="flex items-center gap-1.5">
                                            <button
                                                type="button"
                                                className="action-btn action-btn-edit"
                                                onClick={() => openEditModal(m)}
                                                title="Edit meaning"
                                                aria-label="Edit meaning"
                                            >
                                                <Pencil size={14} />
                                            </button>
                                            <button
                                                type="button"
                                                className="action-btn action-btn-delete"
                                                onClick={() => handleDelete(m.id)}
                                                title="Delete meaning"
                                                aria-label="Delete meaning"
                                            >
                                                <Trash2 size={14} />
                                            </button>
                                        </div>
                                    </td>
                                    <td>{m.meaningText}</td>
                                    <td>{m.meaningLanguage}</td>
                                    <td>{m.priority}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </div>

            {/* Create/Edit Modal */}
            {showModal && (
                <div className="modal-backdrop">
                    <div className="modal-container-sm">
                        <div className="p-6">
                            <div className="modal-header">
                                <h3 className="modal-title">
                                    {editingMeaning ? "Edit Meaning" : "Add Meaning"}
                                </h3>
                                <button
                                    type="button"
                                    onClick={closeModal}
                                    className="modal-close-btn"
                                    disabled={saving}
                                >
                                    ✕
                                </button>
                            </div>

                            {error && (
                                <div className="error-message mb-4">
                                    {error}
                                </div>
                            )}

                            <form onSubmit={handleSubmit} className="space-y-4">
                                <div>
                                    <label className="form-label">
                                        Language *
                                    </label>
                                    <select
                                        value={formData.meaningLanguage}
                                        onChange={(e) =>
                                            setFormData({ ...formData, meaningLanguage: e.target.value })
                                        }
                                        className={`form-input ${saving ? "form-input-disabled" : ""}`}
                                        disabled={saving}
                                        required
                                    >
                                        {MEANING_LANGUAGES.map((lang) => (
                                            <option key={lang.code} value={lang.code}>
                                                {lang.name}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                <div>
                                    <label className="form-label">
                                        Meaning Text *
                                    </label>
                                    <textarea
                                        value={formData.meaningText}
                                        onChange={(e) =>
                                            setFormData({ ...formData, meaningText: e.target.value })
                                        }
                                        rows={3}
                                        className={`form-input ${saving ? "form-input-disabled" : ""}`}
                                        disabled={saving}
                                        required
                                        maxLength={1024}
                                    />
                                    <p className="mt-1 text-xs text-slate-500">
                                        {formData.meaningText.length}/1024 characters
                                    </p>
                                </div>

                                <div>
                                    <label className="form-label">
                                        Priority *
                                    </label>
                                    <input
                                        type="number"
                                        value={formData.priority}
                                        onChange={(e) =>
                                            setFormData({
                                                ...formData,
                                                priority: parseInt(e.target.value) || 1,
                                            })
                                        }
                                        min={1}
                                        className={`form-input ${saving ? "form-input-disabled" : ""}`}
                                        disabled={saving}
                                        required
                                    />
                                    <p className="mt-1 text-xs text-slate-500">
                                        Lower numbers appear first. Next available: {getNextPriority()}
                                    </p>
                                </div>

                                <div className="flex gap-3 pt-4">
                                    <button
                                        type="submit"
                                        disabled={saving}
                                        className={`admin-btn admin-btn-primary ${saving ? "admin-btn-disabled" : ""}`}
                                    >
                                        {saving
                                            ? editingMeaning
                                                ? "Updating..."
                                                : "Creating..."
                                            : editingMeaning
                                              ? "Update Meaning"
                                              : "Create Meaning"}
                                    </button>
                                    <button
                                        type="button"
                                        onClick={closeModal}
                                        disabled={saving}
                                        className="admin-btn admin-btn-default"
                                    >
                                        Cancel
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
}
