import { useEffect, useState } from "react";
import { Trash2, Pencil, Plus, X } from "lucide-react";
import type { LemmaSentenceLinkDto } from "../../../types/sentence";
import { apiFetch } from "../../../utils/apiClient";

interface SentenceLinksSectionProps {
    sentenceId: string;
    sentenceStatus: string | null;
}

interface LemmaDto {
    id: string;
    language: string;
    lemmaNative: string;
    lemmaLatin: string | null;
    pos: string | null;
    status: string | null;
}

interface MeaningDto {
    id: string;
    lemmaId: string;
    meaningLanguage: string;
    meaningText: string;
    priority: number;
}

interface SurfaceFormDto {
    id: string;
    lemmaId: string;
    formNative: string;
    formLatin: string | null;
    formType: string | null;
}

interface LinkFormData {
    lemmaId: string;
    meaningId: string;
    surfaceFormId: string;
    linkType: string;
}

const LINK_TYPES = [
    { value: "EXACT", label: "Exact" },
    { value: "INFLECTED", label: "Inflected" },
    { value: "IMPLIED", label: "Implied" },
];

export default function SentenceLinksSection({ sentenceId, sentenceStatus }: SentenceLinksSectionProps) {
    const [links, setLinks] = useState<LemmaSentenceLinkDto[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    
    // Modal state
    const [showModal, setShowModal] = useState(false);
    const [editingLink, setEditingLink] = useState<LemmaSentenceLinkDto | null>(null);
    const [saving, setSaving] = useState(false);
    
    // Form state
    const [formData, setFormData] = useState<LinkFormData>({
        lemmaId: "",
        meaningId: "",
        surfaceFormId: "",
        linkType: "EXACT",
    });

    // Options for dropdowns
    const [lemmas, setLemmas] = useState<LemmaDto[]>([]);
    const [meanings, setMeanings] = useState<MeaningDto[]>([]);
    const [surfaceForms, setSurfaceForms] = useState<SurfaceFormDto[]>([]);
    const [loadingOptions, setLoadingOptions] = useState(false);

    useEffect(() => {
        loadLinks();
    }, [sentenceId]);

    // Load meanings and surface forms when lemma changes
    useEffect(() => {
        if (formData.lemmaId) {
            loadMeaningsAndSurfaceForms(formData.lemmaId);
        } else {
            setMeanings([]);
            setSurfaceForms([]);
        }
    }, [formData.lemmaId]);

    async function loadLinks() {
        setLoading(true);
        setError(null);
        try {
            const res = await apiFetch(`/api/admin/lemma-sentence-links?sentenceId=${sentenceId}`, {
                headers: { Accept: "application/json" },
            });
            if (res.ok) {
                const data = await res.json();
                setLinks(data);
            } else if (res.status === 401 || res.status === 403) {
                setError("You are not authorized to view links.");
            }
        } catch (e: any) {
            setError("Failed to load links.");
        } finally {
            setLoading(false);
        }
    }

    async function loadLemmas() {
        setLoadingOptions(true);
        try {
            // Load lemmas - we'll load from Marathi (mr) as default, but ideally we'd have a search
            // For now, load first 100 lemmas from Marathi
            const res = await apiFetch("/api/admin/lemmas?language=mr&page=0&size=100", {
                headers: { Accept: "application/json" },
            });
            if (res.ok) {
                const data = await res.json();
                setLemmas(data.content || []);
            }
        } catch (e: any) {
            console.error("Failed to load lemmas:", e);
        } finally {
            setLoadingOptions(false);
        }
    }

    // Load lemma/meaning/surface form data for display
    async function loadLinkDetails(link: LemmaSentenceLinkDto) {
        // Load lemma if not already loaded
        if (!lemmas.find(l => l.id === link.lemmaId)) {
            try {
                const res = await apiFetch(`/api/admin/lemmas/${link.lemmaId}`, {
                    headers: { Accept: "application/json" },
                });
                if (res.ok) {
                    const lemma = await res.json();
                    setLemmas(prev => [...prev, lemma]);
                }
            } catch {
                // Ignore errors
            }
        }

        // Load meanings if meaningId is present
        if (link.meaningId && !meanings.find(m => m.id === link.meaningId)) {
            try {
                const res = await apiFetch(`/api/admin/meanings?lemmaId=${link.lemmaId}`, {
                    headers: { Accept: "application/json" },
                });
                if (res.ok) {
                    const meaningsData = await res.json();
                    setMeanings(prev => [...prev, ...meaningsData.filter((m: MeaningDto) => !prev.find(p => p.id === m.id))]);
                }
            } catch {
                // Ignore errors
            }
        }

        // Load surface forms if surfaceFormId is present
        if (link.surfaceFormId && !surfaceForms.find(sf => sf.id === link.surfaceFormId)) {
            try {
                const res = await apiFetch(`/api/admin/surface-forms?lemmaId=${link.lemmaId}`, {
                    headers: { Accept: "application/json" },
                });
                if (res.ok) {
                    const surfaceFormsData = await res.json();
                    setSurfaceForms(prev => [...prev, ...surfaceFormsData.filter((sf: SurfaceFormDto) => !prev.find(p => p.id === sf.id))]);
                }
            } catch {
                // Ignore errors
            }
        }
    }

    // Load details for all links when links change
    useEffect(() => {
        links.forEach(link => {
            loadLinkDetails(link);
        });
    }, [links]);

    async function loadMeaningsAndSurfaceForms(lemmaId: string) {
        setLoadingOptions(true);
        try {
            // Load meanings
            const meaningsRes = await apiFetch(`/api/admin/meanings?lemmaId=${lemmaId}`, {
                headers: { Accept: "application/json" },
            });
            if (meaningsRes.ok) {
                const meaningsData = await meaningsRes.json();
                setMeanings(meaningsData);
            }

            // Load surface forms
            const surfaceFormsRes = await apiFetch(`/api/admin/surface-forms?lemmaId=${lemmaId}`, {
                headers: { Accept: "application/json" },
            });
            if (surfaceFormsRes.ok) {
                const surfaceFormsData = await surfaceFormsRes.json();
                setSurfaceForms(surfaceFormsData);
            }
        } catch (e: any) {
            console.error("Failed to load meanings/surface forms:", e);
        } finally {
            setLoadingOptions(false);
        }
    }

    function openCreateModal() {
        setEditingLink(null);
        setFormData({
            lemmaId: "",
            meaningId: "",
            surfaceFormId: "",
            linkType: "EXACT",
        });
        setMeanings([]);
        setSurfaceForms([]);
        setError(null);
        loadLemmas();
        setShowModal(true);
    }

    function openEditModal(link: LemmaSentenceLinkDto) {
        setEditingLink(link);
        setFormData({
            lemmaId: link.lemmaId,
            meaningId: link.meaningId || "",
            surfaceFormId: link.surfaceFormId || "",
            linkType: link.linkType || "EXACT",
        });
        setError(null);
        // Load lemmas and then meanings/surface forms for the selected lemma
        loadLemmas();
        if (link.lemmaId) {
            loadMeaningsAndSurfaceForms(link.lemmaId);
        }
        setShowModal(true);
    }

    function closeModal() {
        setShowModal(false);
        setEditingLink(null);
        setError(null);
    }

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        
        if (!formData.lemmaId) {
            setError("Lemma is required.");
            return;
        }

        setSaving(true);
        setError(null);

        try {
            const url = editingLink
                ? `/api/admin/lemma-sentence-links/${editingLink.id}`
                : `/api/admin/lemma-sentence-links`;
            
            const method = editingLink ? "PUT" : "POST";
            
            const body: any = {
                lemmaId: formData.lemmaId,
                linkType: formData.linkType,
            };

            if (!editingLink) {
                body.sentenceId = sentenceId;
            }

            // Only include meaningId and surfaceFormId if they're selected
            if (formData.meaningId) {
                body.meaningId = formData.meaningId;
            }
            if (formData.surfaceFormId) {
                body.surfaceFormId = formData.surfaceFormId;
            }

            const res = await apiFetch(url, {
                method,
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
                setError("You are not authorized to modify links.");
                return;
            }
            if (!res.ok) {
                const text = await res.text();
                const errorMsg = text || `Failed to ${editingLink ? "update" : "create"} link (${res.status})`;
                setError(errorMsg);
                return;
            }

            // Success - reload links and close modal
            await loadLinks();
            closeModal();
        } catch (e: any) {
            setError(e?.message ?? `Failed to ${editingLink ? "update" : "create"} link.`);
        } finally {
            setSaving(false);
        }
    }

    async function handleDelete(linkId: string) {
        const confirmMessage = sentenceStatus === "PUBLISHED"
            ? "This sentence is PUBLISHED. Deleting this link will affect published content. Are you sure you want to delete this link?"
            : "Are you sure you want to delete this link?";

        if (!confirm(confirmMessage)) {
            return;
        }

        try {
            const res = await apiFetch(`/api/admin/lemma-sentence-links/${linkId}`, {
                method: "DELETE",
                credentials: "include",
            });

            if (res.status === 401) {
                setError("You are not signed in.");
                return;
            }
            if (res.status === 403) {
                setError("You are not authorized to delete links.");
                return;
            }
            if (!res.ok) {
                throw new Error(`Failed to delete link (${res.status})`);
            }

            // Success - reload links
            await loadLinks();
        } catch (e: any) {
            setError(e?.message ?? "Failed to delete link.");
        }
    }

    // Load lemma/meaning/surface form data for display
    useEffect(() => {
        if (links.length === 0) return;

        // Load details for all links when links change
        async function loadAllLinkDetails() {
            const promises: Promise<void>[] = [];

            links.forEach((link) => {
                // Load lemma if not already loaded
                if (!lemmas.find(l => l.id === link.lemmaId)) {
                    promises.push(
                        apiFetch(`/api/admin/lemmas/${link.lemmaId}`, {
                            headers: { Accept: "application/json" },
                        })
                            .then(async (res) => {
                                if (res.ok) {
                                    const lemma = await res.json();
                                    setLemmas(prev => {
                                        if (prev.find(l => l.id === lemma.id)) return prev;
                                        return [...prev, lemma];
                                    });
                                }
                            })
                            .catch(() => {
                                // Ignore errors
                            })
                    );
                }

                // Load meanings if meaningId is present
                if (link.meaningId && !meanings.find(m => m.id === link.meaningId)) {
                    promises.push(
                        apiFetch(`/api/admin/meanings?lemmaId=${link.lemmaId}`, {
                            headers: { Accept: "application/json" },
                        })
                            .then(async (res) => {
                                if (res.ok) {
                                    const meaningsData = await res.json();
                                    setMeanings(prev => {
                                        const newMeanings = meaningsData.filter((m: MeaningDto) => !prev.find(p => p.id === m.id));
                                        return [...prev, ...newMeanings];
                                    });
                                }
                            })
                            .catch(() => {
                                // Ignore errors
                            })
                    );
                }

                // Load surface forms if surfaceFormId is present
                if (link.surfaceFormId && !surfaceForms.find(sf => sf.id === link.surfaceFormId)) {
                    promises.push(
                        apiFetch(`/api/admin/surface-forms?lemmaId=${link.lemmaId}`, {
                            headers: { Accept: "application/json" },
                        })
                            .then(async (res) => {
                                if (res.ok) {
                                    const surfaceFormsData = await res.json();
                                    setSurfaceForms(prev => {
                                        const newSurfaceForms = surfaceFormsData.filter((sf: SurfaceFormDto) => !prev.find(p => p.id === sf.id));
                                        return [...prev, ...newSurfaceForms];
                                    });
                                }
                            })
                            .catch(() => {
                                // Ignore errors
                            })
                    );
                }
            });

            await Promise.all(promises);
        }

        loadAllLinkDetails();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [links]);

    // Get display text for a link
    function getLinkDisplayText(link: LemmaSentenceLinkDto): string {
        const parts: string[] = [];
        
        // Find lemma name
        const lemma = lemmas.find(l => l.id === link.lemmaId);
        if (lemma) {
            parts.push(`Lemma: ${lemma.lemmaNative}${lemma.lemmaLatin ? ` (${lemma.lemmaLatin})` : ""}`);
        } else {
            parts.push("Lemma: (loading...)");
        }

        // Add meaning if present
        if (link.meaningId) {
            const meaning = meanings.find(m => m.id === link.meaningId);
            if (meaning) {
                parts.push(`Meaning: ${meaning.meaningText}`);
            } else {
                parts.push("Meaning: (loading...)");
            }
        }

        // Add surface form if present
        if (link.surfaceFormId) {
            const surfaceForm = surfaceForms.find(sf => sf.id === link.surfaceFormId);
            if (surfaceForm) {
                parts.push(`Form: ${surfaceForm.formNative}${surfaceForm.formLatin ? ` (${surfaceForm.formLatin})` : ""}`);
            } else {
                parts.push("Form: (loading...)");
            }
        }

        // Add link type
        const linkTypeLabel = LINK_TYPES.find(lt => lt.value === link.linkType)?.label || link.linkType;
        parts.push(`Type: ${linkTypeLabel}`);

        return parts.join(" | ");
    }

    if (loading) {
        return (
            <div className="mt-8 border-t border-slate-200 pt-4">
                <p className="text-slate-500">Loading links...</p>
            </div>
        );
    }

    return (
        <div className="mt-8 border-t border-slate-200 pt-4">
            <div className="flex items-center justify-between mb-4">
                <h2 className="section-header-alt">Links</h2>
                <button
                    type="button"
                    onClick={openCreateModal}
                    className="admin-btn admin-btn-primary"
                >
                    <Plus size={16} className="mr-1" />
                    Add Link
                </button>
            </div>

            {error && !showModal && (
                <div className="error-message mb-4">
                    {error}
                </div>
            )}

            {links.length === 0 ? (
                <p className="text-sm text-slate-500">No links yet. Add a link to connect this sentence to a lemma.</p>
            ) : (
                <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-slate-200">
                        <thead className="bg-slate-50">
                            <tr>
                                <th className="px-3 py-2 text-left text-xs font-medium text-slate-700 uppercase tracking-wider w-32">
                                    Actions
                                </th>
                                <th className="px-3 py-2 text-left text-xs font-medium text-slate-700 uppercase tracking-wider">
                                    Link Details
                                </th>
                            </tr>
                        </thead>
                        <tbody className="bg-white divide-y divide-slate-200">
                            {links.map((link) => (
                                <tr key={link.id}>
                                    <td className="px-3 py-2 whitespace-nowrap">
                                        <div className="flex items-center gap-2">
                                            <button
                                                type="button"
                                                className="action-btn action-btn-edit"
                                                onClick={() => openEditModal(link)}
                                                title="Edit link"
                                                aria-label="Edit link"
                                            >
                                                <Pencil size={14} />
                                            </button>
                                            <button
                                                type="button"
                                                className="action-btn action-btn-delete"
                                                onClick={() => handleDelete(link.id)}
                                                title="Delete link"
                                                aria-label="Delete link"
                                            >
                                                <Trash2 size={14} />
                                            </button>
                                        </div>
                                    </td>
                                    <td className="px-3 py-2 text-sm text-slate-900">
                                        {getLinkDisplayText(link)}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {/* Modal */}
            {showModal && (
                <div className="modal-backdrop">
                    <div className="modal-container-md">
                        <div className="p-6">
                            <div className="modal-header">
                                <h3 className="modal-title">
                                    {editingLink ? "Edit Link" : "Add Link"}
                                </h3>
                                <button
                                    type="button"
                                    onClick={closeModal}
                                    className="modal-close-btn"
                                    aria-label="Close"
                                >
                                    <X size={20} />
                                </button>
                            </div>

                            {error && (
                                <div className="error-message mb-4">
                                    {error}
                                </div>
                            )}

                            <form onSubmit={handleSubmit} className="space-y-4">
                                <div>
                                    <label className="block text-sm font-medium text-slate-700 mb-1">
                                        Lemma *
                                    </label>
                                    <select
                                        value={formData.lemmaId}
                                        onChange={(e) => {
                                            setFormData({
                                                ...formData,
                                                lemmaId: e.target.value,
                                                meaningId: "", // Clear when lemma changes
                                                surfaceFormId: "", // Clear when lemma changes
                                            });
                                        }}
                                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                                        disabled={saving || loadingOptions}
                                        required
                                    >
                                        <option value="">-- Select a lemma --</option>
                                        {lemmas.map((lemma) => (
                                            <option key={lemma.id} value={lemma.id}>
                                                {lemma.lemmaNative} {lemma.lemmaLatin ? `(${lemma.lemmaLatin})` : ""}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                {formData.lemmaId && (
                                    <>
                                        <div>
                                            <label className="block text-sm font-medium text-slate-700 mb-1">
                                                Meaning (optional)
                                            </label>
                                            <select
                                                value={formData.meaningId}
                                                onChange={(e) => setFormData({ ...formData, meaningId: e.target.value })}
                                                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                                                disabled={saving || loadingOptions}
                                            >
                                                <option value="">-- No meaning --</option>
                                                {meanings.map((meaning) => (
                                                    <option key={meaning.id} value={meaning.id}>
                                                        [{meaning.meaningLanguage}] {meaning.meaningText} (Priority: {meaning.priority})
                                                    </option>
                                                ))}
                                            </select>
                                        </div>

                                        <div>
                                            <label className="block text-sm font-medium text-slate-700 mb-1">
                                                Surface Form (optional)
                                            </label>
                                            <select
                                                value={formData.surfaceFormId}
                                                onChange={(e) => setFormData({ ...formData, surfaceFormId: e.target.value })}
                                                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                                                disabled={saving || loadingOptions}
                                            >
                                                <option value="">-- No surface form --</option>
                                                {surfaceForms.map((sf) => (
                                                    <option key={sf.id} value={sf.id}>
                                                        {sf.formNative} {sf.formLatin ? `(${sf.formLatin})` : ""} {sf.formType ? `[${sf.formType}]` : ""}
                                                    </option>
                                                ))}
                                            </select>
                                        </div>
                                    </>
                                )}

                                <div>
                                    <label className="block text-sm font-medium text-slate-700 mb-1">
                                        Link Type *
                                    </label>
                                    <select
                                        value={formData.linkType}
                                        onChange={(e) => setFormData({ ...formData, linkType: e.target.value })}
                                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                                        disabled={saving}
                                        required
                                    >
                                        {LINK_TYPES.map((lt) => (
                                            <option key={lt.value} value={lt.value}>{lt.label}</option>
                                        ))}
                                    </select>
                                </div>

                                <div className="flex gap-3 pt-4">
                                    <button
                                        type="submit"
                                        disabled={saving}
                                        className={`admin-btn admin-btn-primary ${saving ? "admin-btn-disabled" : ""}`}
                                    >
                                        {saving ? "Saving..." : editingLink ? "Update Link" : "Create Link"}
                                    </button>
                                    <button
                                        type="button"
                                        onClick={closeModal}
                                        className="admin-btn admin-btn-default"
                                        disabled={saving}
                                    >
                                        Cancel
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
