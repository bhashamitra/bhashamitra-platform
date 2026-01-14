import { useEffect, useState, useRef } from "react";
import { Trash2, Pencil, Volume2, Plus, X, Mic, Square } from "lucide-react";

export interface SurfaceFormDto {
    id: string;
    lemmaId: string;
    formNative: string;
    formLatin: string | null;
    formType: string | null;
    featuresJson: string | null;
    notes: string | null;
}

interface PronunciationDto {
    id: string;
    ownerType: string;
    ownerId: string;
    speaker: string | null;
    region: string | null;
    audioUri: string;
    durationMs: number | null;
}

interface SurfaceFormsSectionProps {
    lemmaId: string;
    lemmaStatus: string | null;
}

// Form types for surface forms (required field)
const FORM_TYPES = [
    { value: "infinitive", label: "Infinitive" },
    { value: "finite_verb_present", label: "Finite verb – Present" },
    { value: "finite_verb_past", label: "Finite verb – Past" },
    { value: "finite_verb_future", label: "Finite verb – Future" },
    { value: "progressive_continuous", label: "Progressive / Continuous" },
    { value: "perfect_resultative", label: "Perfect / Resultative" },
    { value: "imperative", label: "Imperative" },
    { value: "participle", label: "Participle" },
    { value: "conjunctive_gerund", label: "Conjunctive / Gerund" },
    { value: "compound_auxiliary", label: "Compound / Auxiliary construction" },
    { value: "colloquial_spoken", label: "Colloquial / Spoken variant" },
];

// Form types that support features
const FORM_TYPES_WITH_FEATURES = [
    "finite_verb_present",
    "finite_verb_past",
    "finite_verb_future",
    "imperative",
    "participle",
];

// Feature options
const GENDER_OPTIONS = [
    { value: "unspecified", label: "Unspecified" },
    { value: "masculine", label: "Masculine" },
    { value: "feminine", label: "Feminine" },
    { value: "neuter", label: "Neuter" },
];

const NUMBER_OPTIONS = [
    { value: "unspecified", label: "Unspecified" },
    { value: "singular", label: "Singular" },
    { value: "plural", label: "Plural" },
];

const PERSON_OPTIONS = [
    { value: "unspecified", label: "Unspecified" },
    { value: "first", label: "First" },
    { value: "second", label: "Second" },
    { value: "third", label: "Third" },
];

const POLITENESS_OPTIONS = [
    { value: "unspecified", label: "Unspecified" },
    { value: "polite", label: "Polite" },
    { value: "familiar", label: "Familiar" },
];

// Helper functions for features JSON
function parseFeaturesJson(json: string | null): { gender: string; number: string; person: string; politeness: string } {
    const defaults = {
        gender: "unspecified",
        number: "unspecified",
        person: "unspecified",
        politeness: "unspecified",
    };
    
    if (!json || !json.trim()) return defaults;
    
    try {
        const parsed = JSON.parse(json);
        return {
            gender: parsed.gender || "unspecified",
            number: parsed.number || "unspecified",
            person: parsed.person || "unspecified",
            politeness: parsed.politeness || "unspecified",
        };
    } catch {
        return defaults;
    }
}

function buildFeaturesJson(features: { gender: string; number: string; person: string; politeness: string }): string | null {
    const obj: Record<string, string> = {};
    
    if (features.gender && features.gender !== "unspecified") {
        obj.gender = features.gender;
    }
    if (features.number && features.number !== "unspecified") {
        obj.number = features.number;
    }
    if (features.person && features.person !== "unspecified") {
        obj.person = features.person;
    }
    if (features.politeness && features.politeness !== "unspecified") {
        obj.politeness = features.politeness;
    }
    
    // If all are unspecified, return null
    if (Object.keys(obj).length === 0) {
        return null;
    }
    
    return JSON.stringify(obj);
}

// Check if form type supports features
function formTypeSupportsFeatures(formType: string): boolean {
    return FORM_TYPES_WITH_FEATURES.includes(formType);
}

// Format features as compact chips for table display
// Only shows features if form type supports them
function formatFeaturesForTable(featuresJson: string | null, formType: string | null): string {
    // Don't show features if form type doesn't support them, even if features_json exists
    if (!formType || !formTypeSupportsFeatures(formType)) {
        return "—";
    }
    
    if (!featuresJson || !featuresJson.trim()) return "—";
    
    try {
        const parsed = JSON.parse(featuresJson);
        const chips: string[] = [];
        
        if (parsed.gender) {
            const genderMap: Record<string, string> = {
                masculine: "M",
                feminine: "F",
                neuter: "N",
            };
            chips.push(genderMap[parsed.gender] || parsed.gender);
        }
        
        if (parsed.number) {
            const numberMap: Record<string, string> = {
                singular: "SG",
                plural: "PL",
            };
            chips.push(numberMap[parsed.number] || parsed.number);
        }
        
        if (parsed.person) {
            const personMap: Record<string, string> = {
                first: "1",
                second: "2",
                third: "3",
            };
            chips.push(personMap[parsed.person] || parsed.person);
        }
        
        if (parsed.politeness) {
            if (parsed.politeness === "polite") {
                chips.push("Polite");
            }
        }
        
        return chips.length > 0 ? chips.join(" ") : "—";
    } catch {
        return "—";
    }
}

interface SurfaceFormFormData {
    formNative: string;
    formLatin: string;
    formType: string;
    features: {
        gender: string;
        number: string;
        person: string;
        politeness: string;
    };
    notes: string;
}

export default function SurfaceFormsSection({ lemmaId, lemmaStatus }: SurfaceFormsSectionProps) {
    const [surfaceForms, setSurfaceForms] = useState<SurfaceFormDto[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [pronunciationCounts, setPronunciationCounts] = useState<Record<string, number>>({});
    
    // Pronunciation management state
    const [expandedSurfaceFormId, setExpandedSurfaceFormId] = useState<string | null>(null);
    const [pronunciations, setPronunciations] = useState<PronunciationDto[]>([]);
    const [loadingPronunciations, setLoadingPronunciations] = useState(false);
    const [pronunciationError, setPronunciationError] = useState<string | null>(null);
    const [uploading, setUploading] = useState(false);
    
    // Audio recording state
    const [isRecording, setIsRecording] = useState(false);
    const [recordedAudio, setRecordedAudio] = useState<Blob | null>(null);
    const [audioUrl, setAudioUrl] = useState<string | null>(null);
    const [recordingTime, setRecordingTime] = useState(0); // Time in seconds
    const mediaRecorderRef = useRef<MediaRecorder | null>(null);
    const mediaStreamRef = useRef<MediaStream | null>(null);
    const recordingTimerRef = useRef<number | null>(null);
    const audioChunksRef = useRef<Blob[]>([]);
    const pronunciationFormRef = useRef<HTMLFormElement | null>(null);
    
    // Duration limits per entity type (hard max)
    const MAX_RECORDING_DURATION = 5; // seconds for SURFACE_FORM
    const RECOMMENDED_DURATION = 3; // seconds (recommended ≤3s for surface forms)
    
    // Modal state
    const [showModal, setShowModal] = useState(false);
    const [editingSurfaceForm, setEditingSurfaceForm] = useState<SurfaceFormDto | null>(null);
    const [saving, setSaving] = useState(false);
    // Track original features state to detect if user made changes
    const [originalFeatures, setOriginalFeatures] = useState<{ gender: string; number: string; person: string; politeness: string } | null>(null);
    
    // Form state
    const [formData, setFormData] = useState<SurfaceFormFormData>({
        formNative: "",
        formLatin: "",
        formType: "", // No default - user must select
        features: {
            gender: "unspecified",
            number: "unspecified",
            person: "unspecified",
            politeness: "unspecified",
        },
        notes: "",
    });
    
    // Advanced features accordion state
    const [showAdvancedFeatures, setShowAdvancedFeatures] = useState(false);

    useEffect(() => {
        loadSurfaceForms();
    }, [lemmaId]);

    async function loadSurfaceForms() {
        setLoading(true);
        setError(null);
        try {
            const res = await fetch(`/api/admin/surface-forms?lemmaId=${lemmaId}`, {
                credentials: "include",
                headers: { Accept: "application/json" },
            });
            if (res.ok) {
                const data = await res.json();
                setSurfaceForms(data);
                // Load pronunciation counts for each surface form
                await loadPronunciationCounts(data);
            } else if (res.status === 401 || res.status === 403) {
                setError("You are not authorized to view surface forms.");
            }
        } catch (e: any) {
            setError("Failed to load surface forms.");
        } finally {
            setLoading(false);
        }
    }

    async function loadPronunciationCounts(forms: SurfaceFormDto[]) {
        const counts: Record<string, number> = {};
        // Load counts for all surface forms in parallel
        await Promise.all(
            forms.map(async (sf) => {
                try {
                    const res = await fetch(
                        `/api/admin/pronunciations?ownerType=SURFACE_FORM&ownerId=${sf.id}`,
                        {
                            credentials: "include",
                            headers: { Accept: "application/json" },
                        }
                    );
                    if (res.ok) {
                        const pronunciations = await res.json();
                        counts[sf.id] = pronunciations.length;
                    }
                } catch {
                    counts[sf.id] = 0;
                }
            })
        );
        setPronunciationCounts(counts);
    }

    function openCreateModal() {
        setEditingSurfaceForm(null);
        setFormData({
            formNative: "",
            formLatin: "",
            formType: "", // No default - user must select
            features: {
                gender: "unspecified",
                number: "unspecified",
                person: "unspecified",
                politeness: "unspecified",
            },
            notes: "",
        });
        setShowAdvancedFeatures(false);
        setError(null);
        setShowModal(true);
    }

    function openEditModal(surfaceForm: SurfaceFormDto) {
        setEditingSurfaceForm(surfaceForm);
        // Store original featuresJson and formType to preserve features if form type doesn't support features
        const features = parseFeaturesJson(surfaceForm.featuresJson);
        setOriginalFeatures({ ...features }); // Deep copy
        setFormData({
            formNative: surfaceForm.formNative,
            formLatin: surfaceForm.formLatin ?? "",
            formType: surfaceForm.formType ?? "", // Use existing value or empty
            features,
            notes: surfaceForm.notes ?? "",
        });
        setShowAdvancedFeatures(false);
        setError(null);
        setShowModal(true);
    }

    function closeModal() {
        setShowModal(false);
        setEditingSurfaceForm(null);
        setOriginalFeatures(null);
        setError(null);
    }

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        
        if (!formData.formNative.trim()) {
            setError("Native form is required.");
            return;
        }

        setSaving(true);
        setError(null);

        try {
            const url = editingSurfaceForm
                ? `/api/admin/surface-forms/${editingSurfaceForm.id}`
                : `/api/admin/surface-forms`;
            
            const method = editingSurfaceForm ? "PUT" : "POST";
            
            // Build body, omitting null/empty optional fields
            const buildBody = () => {
                const base: any = {
                    formNative: formData.formNative.trim(),
                };
                
                if (formData.formLatin.trim()) {
                    base.formLatin = formData.formLatin.trim();
                }
                
                // Form type is required - validate it's not empty
                if (!formData.formType || !formData.formType.trim()) {
                    setError("Form Type is required. Please select a form type.");
                    setSaving(false);
                    return;
                }
                base.formType = formData.formType.trim();
                
                // Handle featuresJson based on form type and whether user made changes
                if (formTypeSupportsFeatures(base.formType)) {
                    // Form type supports features - check if user made changes
                    if (originalFeatures) {
                        const featuresChanged = 
                            formData.features.gender !== originalFeatures.gender ||
                            formData.features.number !== originalFeatures.number ||
                            formData.features.person !== originalFeatures.person ||
                            formData.features.politeness !== originalFeatures.politeness;
                        
                        if (featuresChanged) {
                            // User made changes - send current features (backend will update)
                            base.featuresJson = buildFeaturesJson(formData.features); // Can be null if all unspecified
                        }
                        // If no changes, don't include featuresJson in request (backend will preserve original)
                    } else {
                        // No original features tracked (shouldn't happen, but handle gracefully)
                        // Send current features
                        base.featuresJson = buildFeaturesJson(formData.features);
                    }
                } else {
                    // Form type doesn't support features - don't send featuresJson
                    // Backend will preserve original featuresJson (it only updates if provided)
                    // This allows features to be preserved even when form type changes
                }
                
                if (formData.notes.trim()) {
                    base.notes = formData.notes.trim();
                }
                
                if (!editingSurfaceForm) {
                    base.lemmaId = lemmaId;
                }
                
                return base;
            };
            
            const body = buildBody();

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
                setError("You are not authorized to modify surface forms.");
                return;
            }
            if (!res.ok) {
                let errorMessage = `Failed to ${editingSurfaceForm ? "update" : "create"} surface form (${res.status})`;
                try {
                    const text = await res.text();
                    if (text) {
                        errorMessage = text;
                        // Add helpful suggestion for duplicate errors
                        if (text.includes("already exists") && !editingSurfaceForm) {
                            errorMessage += ". Please edit the existing surface form instead.";
                        }
                    }
                } catch {
                    // Ignore if we can't read the response
                }
                throw new Error(errorMessage);
            }

            // Success - reload surface forms and close modal
            await loadSurfaceForms();
            closeModal();
        } catch (e: any) {
            setError(e?.message ?? `Failed to ${editingSurfaceForm ? "update" : "create"} surface form.`);
        } finally {
            setSaving(false);
        }
    }

    async function handleDelete(surfaceFormId: string) {
        const isPublished = lemmaStatus === "PUBLISHED";
        const confirmMessage = isPublished
            ? "This lemma is published. Are you sure you want to delete this surface form?"
            : "Are you sure you want to delete this surface form?";
        
        if (!confirm(confirmMessage)) {
            return;
        }

        setError(null);
        try {
            const res = await fetch(`/api/admin/surface-forms/${surfaceFormId}`, {
                method: "DELETE",
                credentials: "include",
                headers: { Accept: "application/json" },
            });

            if (res.status === 401) {
                setError("You are not signed in.");
                return;
            }
            if (res.status === 403) {
                setError("You are not authorized to delete surface forms.");
                return;
            }
            if (!res.ok) {
                throw new Error(`Failed to delete surface form (${res.status})`);
            }

            // Success - reload surface forms (counts will reload automatically)
            await loadSurfaceForms();
            // Close pronunciations section if this surface form was expanded
            if (expandedSurfaceFormId === surfaceFormId) {
                setExpandedSurfaceFormId(null);
            }
        } catch (e: any) {
            setError(e?.message ?? "Failed to delete surface form.");
        }
    }

    // Pronunciation management functions
    async function togglePronunciations(surfaceFormId: string) {
        if (expandedSurfaceFormId === surfaceFormId) {
            // Clean up recording state before closing
            if (isRecording) {
                stopRecording();
            }
            clearRecordedAudio();
            setExpandedSurfaceFormId(null);
            setPronunciations([]);
        } else {
            // Clean up any existing recording when opening a different surface form
            if (isRecording) {
                stopRecording();
            }
            clearRecordedAudio();
            setExpandedSurfaceFormId(surfaceFormId);
            await loadPronunciations(surfaceFormId);
        }
    }

    // Audio recording functions
    async function startRecording() {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            mediaStreamRef.current = stream;
            
            const mediaRecorder = new MediaRecorder(stream, {
                mimeType: 'audio/webm' // Most widely supported
            });
            
            mediaRecorderRef.current = mediaRecorder;
            audioChunksRef.current = [];
            setRecordingTime(0);
            
            mediaRecorder.ondataavailable = (event) => {
                if (event.data.size > 0) {
                    audioChunksRef.current.push(event.data);
                }
            };
            
            mediaRecorder.onstop = () => {
                const audioBlob = new Blob(audioChunksRef.current, { type: 'audio/webm' });
                setRecordedAudio(audioBlob);
                const url = URL.createObjectURL(audioBlob);
                setAudioUrl(url);
                
                // Stop all tracks to release microphone
                if (mediaStreamRef.current) {
                    mediaStreamRef.current.getTracks().forEach(track => track.stop());
                    mediaStreamRef.current = null;
                }
                
                // Clear timer
                if (recordingTimerRef.current) {
                    clearInterval(recordingTimerRef.current);
                    recordingTimerRef.current = null;
                }
                setRecordingTime(0);
            };
            
            mediaRecorder.start();
            setIsRecording(true);
            setPronunciationError(null);
            
            // Start timer that automatically stops after MAX_RECORDING_DURATION seconds
            let elapsed = 0;
            recordingTimerRef.current = window.setInterval(() => {
                elapsed += 1;
                setRecordingTime(elapsed);
                
                if (elapsed >= MAX_RECORDING_DURATION) {
                    stopRecording();
                }
            }, 1000);
        } catch (error: any) {
            console.error('Error starting recording:', error);
            setPronunciationError(
                error.name === 'NotAllowedError' 
                    ? 'Microphone access denied. Please allow microphone access and try again.'
                    : 'Failed to start recording. Please check your microphone.'
            );
        }
    }

    function stopRecording() {
        if (mediaRecorderRef.current && isRecording) {
            mediaRecorderRef.current.stop();
            setIsRecording(false);
        }
        // Also stop stream tracks if MediaRecorder stop doesn't trigger onstop
        if (mediaStreamRef.current) {
            mediaStreamRef.current.getTracks().forEach(track => track.stop());
            mediaStreamRef.current = null;
        }
        // Clear timer
        if (recordingTimerRef.current) {
            clearInterval(recordingTimerRef.current);
            recordingTimerRef.current = null;
        }
    }

    function clearRecordedAudio() {
        if (audioUrl) {
            URL.revokeObjectURL(audioUrl);
        }
        setRecordedAudio(null);
        setAudioUrl(null);
        setRecordingTime(0);
        audioChunksRef.current = [];
        if (recordingTimerRef.current) {
            clearInterval(recordingTimerRef.current);
            recordingTimerRef.current = null;
        }
    }

    async function loadPronunciations(surfaceFormId: string) {
        setLoadingPronunciations(true);
        setPronunciationError(null);
        try {
            const res = await fetch(
                `/api/admin/pronunciations?ownerType=SURFACE_FORM&ownerId=${surfaceFormId}`,
                {
                    credentials: "include",
                    headers: { Accept: "application/json" },
                }
            );
            if (res.ok) {
                const data = await res.json();
                setPronunciations(data);
            } else if (res.status === 401 || res.status === 403) {
                setPronunciationError("You are not authorized to view pronunciations.");
            } else {
                setPronunciationError("Failed to load pronunciations.");
            }
        } catch (e: any) {
            setPronunciationError("Failed to load pronunciations.");
        } finally {
            setLoadingPronunciations(false);
        }
    }

    async function handleUploadPronunciation(e: React.FormEvent<HTMLFormElement>, surfaceFormId: string) {
        e.preventDefault();
        
        const formData = new FormData(e.currentTarget);
        let audioFile: File | null = formData.get("audioFile") as File;
        
        // Use recorded audio if available, otherwise use file input
        if (recordedAudio && !audioFile) {
            // Convert recorded blob to File
            audioFile = new File([recordedAudio], `recording-${Date.now()}.webm`, {
                type: 'audio/webm'
            });
        }
        
        if (!audioFile) {
            setPronunciationError("Please record audio or select an audio file.");
            return;
        }

        // Validate file size (1MB = 1048576 bytes)
        const MAX_FILE_SIZE = 1048576;
        if (audioFile.size > MAX_FILE_SIZE) {
            setPronunciationError(`File size exceeds 1 MB limit. Current size: ${(audioFile.size / 1024).toFixed(2)} KB`);
            return;
        }

        setUploading(true);
        setPronunciationError(null);

        try {
            const uploadFormData = new FormData();
            uploadFormData.append("ownerType", "SURFACE_FORM");
            uploadFormData.append("ownerId", surfaceFormId);
            uploadFormData.append("audioFile", audioFile);
            
            const speaker = formData.get("speaker") as string;
            if (speaker?.trim()) {
                uploadFormData.append("speaker", speaker.trim());
            }
            
            const region = formData.get("region") as string;
            if (region?.trim()) {
                uploadFormData.append("region", region.trim());
            }

            // Calculate duration from recorded audio or use a default
            let durationMs: number | null = null;
            if (recordedAudio) {
                durationMs = recordingTime * 1000;
            }
            if (durationMs !== null) {
                uploadFormData.append("durationMs", String(durationMs));
            }

            const res = await fetch("/api/admin/pronunciations", {
                method: "POST",
                credentials: "include",
                body: uploadFormData,
            });

            if (res.status === 401) {
                setPronunciationError("You are not signed in.");
                return;
            }
            if (res.status === 403) {
                setPronunciationError("You are not authorized to upload pronunciations.");
                return;
            }
            if (!res.ok) {
                const text = await res.text();
                throw new Error(text || `Failed to upload pronunciation (${res.status})`);
            }

            // Success - reload pronunciations and counts
            await loadPronunciations(surfaceFormId);
            await loadSurfaceForms(); // Reload to update counts
            
            // Clear form and recording
            // Use form ref instead of event.currentTarget (which may be null after async operations)
            if (pronunciationFormRef.current) {
                pronunciationFormRef.current.reset();
            }
            clearRecordedAudio();
        } catch (e: any) {
            setPronunciationError(e?.message ?? "Failed to upload pronunciation.");
        } finally {
            setUploading(false);
        }
    }

    // Cleanup audio URL and timer on unmount
    useEffect(() => {
        return () => {
            if (audioUrl) {
                URL.revokeObjectURL(audioUrl);
            }
            stopRecording();
            if (recordingTimerRef.current) {
                clearInterval(recordingTimerRef.current);
            }
        };
    }, [audioUrl]);

    async function handleDeletePronunciation(pronunciationId: string, surfaceFormId: string) {
        if (!confirm("Are you sure you want to delete this pronunciation?")) {
            return;
        }

        setPronunciationError(null);
        try {
            const res = await fetch(`/api/admin/pronunciations/${pronunciationId}`, {
                method: "DELETE",
                credentials: "include",
            });

            if (res.status === 401) {
                setPronunciationError("You are not signed in.");
                return;
            }
            if (res.status === 403) {
                setPronunciationError("You are not authorized to delete pronunciations.");
                return;
            }
            if (!res.ok) {
                throw new Error(`Failed to delete pronunciation (${res.status})`);
            }

            // Success - reload pronunciations and counts
            await loadPronunciations(surfaceFormId);
            await loadSurfaceForms(); // Reload to update counts
        } catch (e: any) {
            setPronunciationError(e?.message ?? "Failed to delete pronunciation.");
        }
    }

    return (
        <>
            <div className="mt-8 border-t border-slate-200 pt-4">
                <div className="flex items-center justify-between mb-3">
                    <h2 className="text-sm font-medium text-slate-700">Surface Forms</h2>
                    <button
                        type="button"
                        className="admin-btn admin-btn-default text-xs"
                        onClick={openCreateModal}
                    >
                        + Add Surface Form
                    </button>
                </div>

                {error && !showModal && (
                    <div className="mb-3 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                        {error}
                    </div>
                )}

                {loading ? (
                    <p className="text-sm text-slate-500">Loading surface forms...</p>
                ) : surfaceForms.length === 0 ? (
                    <p className="text-sm text-slate-500">No surface forms yet.</p>
                ) : (
                    <table className="admin-table text-sm">
                        <thead>
                            <tr>
                                <th className="w-40">Actions</th>
                                <th>Form (Native)</th>
                                <th>Form (Latin)</th>
                                <th>Type</th>
                                <th>Features</th>
                            </tr>
                        </thead>
                        <tbody>
                            {surfaceForms.map((sf) => {
                                const count = pronunciationCounts[sf.id] ?? 0;
                                return (
                                    <tr key={sf.id}>
                                        <td>
                                            <div className="flex items-center gap-1 flex-wrap">
                                                {count > 0 ? (
                                                    <button
                                                        type="button"
                                                        className="action-btn action-btn-audio flex items-center gap-0.5"
                                                        onClick={() => togglePronunciations(sf.id)}
                                                        title={`Manage pronunciations (${count})`}
                                                        aria-label={`Manage pronunciations (${count})`}
                                                    >
                                                        <Volume2 size={14} className="text-blue-600" />
                                                        <span className="text-xs text-blue-600">({count})</span>
                                                    </button>
                                                ) : (
                                                    <button
                                                        type="button"
                                                        className="action-btn action-btn-audio-empty flex items-center gap-0.5"
                                                        onClick={() => togglePronunciations(sf.id)}
                                                        title="Add pronunciation"
                                                        aria-label="Add pronunciation"
                                                    >
                                                        <Volume2 size={14} className="text-slate-500" />
                                                        <Plus size={12} className="text-blue-600" />
                                                    </button>
                                                )}
                                                <button
                                                    type="button"
                                                    className="action-btn action-btn-edit"
                                                    onClick={() => openEditModal(sf)}
                                                    title="Edit surface form"
                                                    aria-label="Edit surface form"
                                                >
                                                    <Pencil size={14} />
                                                </button>
                                                <button
                                                    type="button"
                                                    className="action-btn action-btn-delete"
                                                    onClick={() => handleDelete(sf.id)}
                                                    title="Delete surface form"
                                                    aria-label="Delete surface form"
                                                >
                                                    <Trash2 size={14} />
                                                </button>
                                            </div>
                                        </td>
                                        <td>{sf.formNative}</td>
                                        <td className="text-slate-700">{sf.formLatin || "-"}</td>
                                        <td className="text-slate-700">{sf.formType || "-"}</td>
                                        <td className="text-slate-600 text-xs">
                                            <span className="inline-flex items-center gap-1">
                                                {formatFeaturesForTable(sf.featuresJson, sf.formType)}
                                            </span>
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                )}

                {/* Pronunciation Management Sections */}
                {surfaceForms.map((sf) => {
                    if (expandedSurfaceFormId !== sf.id) return null;
                    
                    return (
                        <div key={`pronunciations-${sf.id}`} className="mt-4 border border-slate-200 rounded-lg p-4 bg-slate-50">
                            <div className="flex items-center justify-between mb-4">
                                <h3 className="text-sm font-medium text-slate-700">
                                    Pronunciations for "{sf.formNative}"
                                </h3>
                                <button
                                    type="button"
                                    onClick={() => setExpandedSurfaceFormId(null)}
                                    className="action-btn action-btn-edit"
                                    title="Close pronunciations"
                                    aria-label="Close pronunciations"
                                >
                                    <X size={14} />
                                </button>
                            </div>

                            {pronunciationError && (
                                <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                                    {pronunciationError}
                                </div>
                            )}

                            {/* Existing Pronunciations List */}
                            {loadingPronunciations ? (
                                <p className="text-sm text-slate-500 mb-4">Loading pronunciations...</p>
                            ) : pronunciations.length > 0 ? (
                                <div className="mb-4">
                                    <table className="admin-table text-sm">
                                        <thead>
                                            <tr>
                                                <th className="w-32">Actions</th>
                                                <th>Speaker</th>
                                                <th>Region</th>
                                                <th>Duration</th>
                                                <th>Audio</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {pronunciations.map((pron) => (
                                                <tr key={pron.id}>
                                                    <td>
                                                        <button
                                                            type="button"
                                                            className="action-btn action-btn-delete"
                                                            onClick={() => handleDeletePronunciation(pron.id, sf.id)}
                                                            title="Delete pronunciation"
                                                            aria-label="Delete pronunciation"
                                                        >
                                                            <Trash2 size={14} />
                                                        </button>
                                                    </td>
                                                    <td className="text-slate-700">{pron.speaker || "-"}</td>
                                                    <td className="text-slate-700">{pron.region || "-"}</td>
                                                    <td className="text-slate-700">
                                                        {pron.durationMs != null ? `${(pron.durationMs / 1000).toFixed(1)}s` : "-"}
                                                    </td>
                                                    <td>
                                                        <audio 
                                                            controls 
                                                            src={`/api/admin/pronunciations/${pron.id}/audio`}
                                                            className="w-full max-w-xs"
                                                            preload="metadata"
                                                        >
                                                            Your browser does not support the audio element.
                                                        </audio>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            ) : (
                                <p className="text-sm text-slate-500 mb-4">No pronunciations yet.</p>
                            )}

                            {/* Always-visible Add Pronunciation Form */}
                            <div className="border-t border-slate-200 pt-4">
                                <h4 className="text-sm font-medium text-slate-700 mb-3">Add Pronunciation</h4>
                                <form 
                                    ref={pronunciationFormRef}
                                    onSubmit={(e) => handleUploadPronunciation(e, sf.id)} 
                                    className="space-y-3"
                                >
                                    <div>
                                        <label className="block text-sm font-medium text-slate-700 mb-2">
                                            Audio *
                                        </label>
                                        
                                        {/* Recording Controls */}
                                        <div className="mb-3 flex items-center gap-2">
                                            {!isRecording ? (
                                                <button
                                                    type="button"
                                                    onClick={startRecording}
                                                    disabled={uploading || !!audioUrl}
                                                    className="admin-btn admin-btn-default flex items-center gap-2 text-sm"
                                                >
                                                    <Mic size={16} />
                                                    Record Audio
                                                </button>
                                            ) : (
                                                <button
                                                    type="button"
                                                    onClick={stopRecording}
                                                    disabled={uploading}
                                                    className="admin-btn admin-btn-danger flex items-center gap-2 text-sm"
                                                >
                                                    <Square size={16} />
                                                    Stop Recording
                                                </button>
                                            )}
                                            {audioUrl && (
                                                <button
                                                    type="button"
                                                    onClick={clearRecordedAudio}
                                                    disabled={uploading || isRecording}
                                                    className="admin-btn admin-btn-default text-sm"
                                                >
                                                    Clear Recording
                                                </button>
                                            )}
                                            {isRecording && (
                                                <span className={`text-sm flex items-center gap-1 ${
                                                    recordingTime >= MAX_RECORDING_DURATION - 2 
                                                        ? 'text-red-600 font-semibold' 
                                                        : 'text-slate-600'
                                                }`}>
                                                    <span className="w-2 h-2 bg-red-600 rounded-full animate-pulse"></span>
                                                    Recording... {recordingTime}s / {MAX_RECORDING_DURATION}s
                                                </span>
                                            )}
                                        </div>
                                        
                                        {/* Audio Preview */}
                                        {audioUrl && (
                                            <div className="mb-3 p-3 bg-slate-50 rounded-md border border-slate-200">
                                                <p className="text-xs text-slate-600 mb-2">Recorded Audio Preview:</p>
                                                <audio controls src={audioUrl} className="w-full" />
                                            </div>
                                        )}
                                        
                                        {/* File Upload (Alternative to Recording) */}
                                        <div className="mb-2">
                                            <p className="text-xs text-slate-600 mb-2">Or upload a file:</p>
                                            <input
                                                type="file"
                                                name="audioFile"
                                                accept="audio/mpeg,audio/wav,audio/ogg,audio/webm"
                                                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                                                disabled={uploading || isRecording || !!audioUrl}
                                                onChange={(e) => {
                                                    if (e.target.files && e.target.files.length > 0) {
                                                        clearRecordedAudio();
                                                    }
                                                }}
                                            />
                                        </div>
                                        
                                        <p className="mt-1 text-xs text-slate-500">
                                            Record audio using your microphone, or upload a file. 
                                            <br />
                                            <strong>Recommended:</strong> ≤{RECOMMENDED_DURATION} seconds. <strong>Hard max:</strong> {MAX_RECORDING_DURATION} seconds.
                                            <br />
                                            Max file size: 1 MB. Supported formats: WebM (preferred), MP3, WAV
                                        </p>
                                    </div>

                                    <div className="grid grid-cols-2 gap-3">
                                        <div>
                                            <label className="block text-sm font-medium text-slate-700 mb-1">
                                                Speaker
                                            </label>
                                            <input
                                                type="text"
                                                name="speaker"
                                                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                                                disabled={uploading}
                                                maxLength={100}
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-sm font-medium text-slate-700 mb-1">
                                                Region
                                            </label>
                                            <input
                                                type="text"
                                                name="region"
                                                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                                                disabled={uploading}
                                                maxLength={100}
                                            />
                                        </div>
                                    </div>

                                    <div className="flex gap-3 pt-2">
                                        <button
                                            type="submit"
                                            disabled={uploading}
                                            className={`admin-btn admin-btn-primary ${uploading ? "admin-btn-disabled" : ""}`}
                                        >
                                            {uploading ? "Uploading..." : "Upload Pronunciation"}
                                        </button>
                                    </div>
                                </form>
                            </div>
                        </div>
                    );
                })}
            </div>

            {/* Create/Edit Modal */}
            {showModal && (
                <div className="modal-backdrop">
                    <div className="bg-white rounded-lg shadow-lg max-w-lg w-full mx-4 max-h-[90vh] overflow-y-auto">
                        <div className="p-6">
                            <div className="flex items-center justify-between mb-4">
                                <h3 className="text-lg font-semibold text-slate-900">
                                    {editingSurfaceForm ? "Edit Surface Form" : "Add Surface Form"}
                                </h3>
                                <button
                                    type="button"
                                    onClick={closeModal}
                                    className="text-slate-400 hover:text-slate-600"
                                    disabled={saving}
                                >
                                    ✕
                                </button>
                            </div>

                            {error && (
                                <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                                    {error}
                                </div>
                            )}

                            <form onSubmit={handleSubmit} className="space-y-4">
                                <div>
                                    <label className="block text-sm font-medium text-slate-700 mb-1">
                                        Native Form *
                                    </label>
                                    <input
                                        type="text"
                                        value={formData.formNative}
                                        onChange={(e) =>
                                            setFormData({ ...formData, formNative: e.target.value })
                                        }
                                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                                        disabled={saving}
                                        required
                                        maxLength={255}
                                    />
                                    <p className="mt-1 text-xs text-slate-500">
                                        {formData.formNative.length}/255 characters
                                    </p>
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-slate-700 mb-1">
                                        Latin Transliteration
                                    </label>
                                    <input
                                        type="text"
                                        value={formData.formLatin}
                                        onChange={(e) =>
                                            setFormData({ ...formData, formLatin: e.target.value })
                                        }
                                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                                        disabled={saving}
                                        maxLength={255}
                                    />
                                    <p className="mt-1 text-xs text-slate-500">
                                        {formData.formLatin.length}/255 characters
                                    </p>
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-slate-700 mb-1">
                                        Form Type *
                                    </label>
                                    <select
                                        value={formData.formType}
                                        onChange={(e) =>
                                            setFormData({ ...formData, formType: e.target.value })
                                        }
                                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                                        disabled={saving}
                                        required
                                    >
                                        <option value="">-- Select a form type --</option>
                                        {FORM_TYPES.map((type) => (
                                            <option key={type.value} value={type.value}>
                                                {type.label}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                {/* Features Section - Conditional */}
                                {formData.formType && formTypeSupportsFeatures(formData.formType) && (
                                    <div className="border border-slate-200 rounded-md p-4 bg-slate-50">
                                        <div className="flex items-center justify-between mb-3">
                                            <label className="block text-sm font-medium text-slate-700">
                                                Features (optional)
                                            </label>
                                            {(formData.features.gender !== "unspecified" ||
                                              formData.features.number !== "unspecified" ||
                                              formData.features.person !== "unspecified" ||
                                              formData.features.politeness !== "unspecified") && (
                                                <button
                                                    type="button"
                                                    onClick={() => {
                                                        setFormData({
                                                            ...formData,
                                                            features: {
                                                                gender: "unspecified",
                                                                number: "unspecified",
                                                                person: "unspecified",
                                                                politeness: "unspecified",
                                                            },
                                                        });
                                                    }}
                                                    className="text-xs text-slate-600 hover:text-slate-800 underline"
                                                    disabled={saving}
                                                >
                                                    Clear features
                                                </button>
                                            )}
                                        </div>

                                        <div className="space-y-3">
                                            {/* Gender */}
                                            <div>
                                                <label className="block text-xs font-medium text-slate-600 mb-1">
                                                    Gender
                                                </label>
                                                <div className="flex gap-2 flex-wrap">
                                                    {GENDER_OPTIONS.map((opt) => (
                                                        <label
                                                            key={opt.value}
                                                            className="flex items-center gap-1.5 cursor-pointer"
                                                        >
                                                            <input
                                                                type="radio"
                                                                name="gender"
                                                                value={opt.value}
                                                                checked={formData.features.gender === opt.value}
                                                                onChange={(e) =>
                                                                    setFormData({
                                                                        ...formData,
                                                                        features: {
                                                                            ...formData.features,
                                                                            gender: e.target.value,
                                                                        },
                                                                    })
                                                                }
                                                                disabled={saving}
                                                                className="w-3.5 h-3.5 text-blue-600"
                                                            />
                                                            <span className="text-sm text-slate-700">{opt.label}</span>
                                                        </label>
                                                    ))}
                                                </div>
                                            </div>

                                            {/* Number */}
                                            <div>
                                                <label className="block text-xs font-medium text-slate-600 mb-1">
                                                    Number
                                                </label>
                                                <div className="flex gap-2 flex-wrap">
                                                    {NUMBER_OPTIONS.map((opt) => (
                                                        <label
                                                            key={opt.value}
                                                            className="flex items-center gap-1.5 cursor-pointer"
                                                        >
                                                            <input
                                                                type="radio"
                                                                name="number"
                                                                value={opt.value}
                                                                checked={formData.features.number === opt.value}
                                                                onChange={(e) =>
                                                                    setFormData({
                                                                        ...formData,
                                                                        features: {
                                                                            ...formData.features,
                                                                            number: e.target.value,
                                                                        },
                                                                    })
                                                                }
                                                                disabled={saving}
                                                                className="w-3.5 h-3.5 text-blue-600"
                                                            />
                                                            <span className="text-sm text-slate-700">{opt.label}</span>
                                                        </label>
                                                    ))}
                                                </div>
                                            </div>

                                            {/* Advanced Features Accordion */}
                                            <div>
                                                <button
                                                    type="button"
                                                    onClick={() => setShowAdvancedFeatures(!showAdvancedFeatures)}
                                                    className="flex items-center gap-1 text-xs font-medium text-slate-600 hover:text-slate-800"
                                                    disabled={saving}
                                                >
                                                    <span>{showAdvancedFeatures ? "▼" : "▶"}</span>
                                                    <span>Advanced</span>
                                                </button>

                                                {showAdvancedFeatures && (
                                                    <div className="mt-2 space-y-3 pl-4 border-l-2 border-slate-200">
                                                        {/* Person */}
                                                        <div>
                                                            <label className="block text-xs font-medium text-slate-600 mb-1">
                                                                Person
                                                            </label>
                                                            <div className="flex gap-2 flex-wrap">
                                                                {PERSON_OPTIONS.map((opt) => (
                                                                    <label
                                                                        key={opt.value}
                                                                        className="flex items-center gap-1.5 cursor-pointer"
                                                                    >
                                                                        <input
                                                                            type="radio"
                                                                            name="person"
                                                                            value={opt.value}
                                                                            checked={formData.features.person === opt.value}
                                                                            onChange={(e) =>
                                                                                setFormData({
                                                                                    ...formData,
                                                                                    features: {
                                                                                        ...formData.features,
                                                                                        person: e.target.value,
                                                                                    },
                                                                                })
                                                                            }
                                                                            disabled={saving}
                                                                            className="w-3.5 h-3.5 text-blue-600"
                                                                        />
                                                                        <span className="text-sm text-slate-700">{opt.label}</span>
                                                                    </label>
                                                                ))}
                                                            </div>
                                                        </div>

                                                        {/* Politeness */}
                                                        <div>
                                                            <label className="block text-xs font-medium text-slate-600 mb-1">
                                                                Politeness
                                                            </label>
                                                            <div className="flex gap-2 flex-wrap">
                                                                {POLITENESS_OPTIONS.map((opt) => (
                                                                    <label
                                                                        key={opt.value}
                                                                        className="flex items-center gap-1.5 cursor-pointer"
                                                                    >
                                                                        <input
                                                                            type="radio"
                                                                            name="politeness"
                                                                            value={opt.value}
                                                                            checked={formData.features.politeness === opt.value}
                                                                            onChange={(e) =>
                                                                                setFormData({
                                                                                    ...formData,
                                                                                    features: {
                                                                                        ...formData.features,
                                                                                        politeness: e.target.value,
                                                                                    },
                                                                                })
                                                                            }
                                                                            disabled={saving}
                                                                            className="w-3.5 h-3.5 text-blue-600"
                                                                        />
                                                                        <span className="text-sm text-slate-700">{opt.label}</span>
                                                                    </label>
                                                                ))}
                                                            </div>
                                                        </div>
                                                    </div>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                )}

                                {/* Show note if form type changed and features exist but are hidden */}
                                {editingSurfaceForm &&
                                    formData.formType &&
                                    !formTypeSupportsFeatures(formData.formType) &&
                                    editingSurfaceForm.featuresJson && (
                                        <div className="text-xs text-slate-500 italic">
                                            Features saved (not applicable for this form type)
                                        </div>
                                    )}

                                <div>
                                    <label className="block text-sm font-medium text-slate-700 mb-1">
                                        Notes
                                    </label>
                                    <textarea
                                        value={formData.notes}
                                        onChange={(e) =>
                                            setFormData({ ...formData, notes: e.target.value })
                                        }
                                        rows={3}
                                        className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                                        disabled={saving}
                                    />
                                </div>

                                <div className="flex gap-3 pt-4">
                                    <button
                                        type="submit"
                                        disabled={saving}
                                        className={`admin-btn admin-btn-primary ${saving ? "admin-btn-disabled" : ""}`}
                                    >
                                        {saving
                                            ? editingSurfaceForm
                                                ? "Updating..."
                                                : "Creating..."
                                            : editingSurfaceForm
                                              ? "Update Surface Form"
                                              : "Create Surface Form"}
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
