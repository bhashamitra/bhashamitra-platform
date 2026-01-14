import { useEffect, useState, useRef } from "react";
import { Trash2, Mic, MicOff } from "lucide-react";
import { apiFetch } from "../../../utils/apiClient";

interface PronunciationDto {
    id: string;
    ownerType: string;
    ownerId: string;
    speaker: string | null;
    region: string | null;
    audioUri: string;
    durationMs: number | null;
    isPrimary: boolean | null;
}

interface SentencePronunciationsSectionProps {
    sentenceId: string;
    sentenceStatus: string | null;
}

// Duration limits per entity type (hard max)
const MAX_RECORDING_DURATION = 10; // seconds for SENTENCE
const RECOMMENDED_DURATION = 8; // seconds (recommended ≤8s for sentences)

export default function SentencePronunciationsSection({ sentenceId, sentenceStatus }: SentencePronunciationsSectionProps) {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [pronunciations, setPronunciations] = useState<PronunciationDto[]>([]);
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
    
    // Form state
    const [speaker, setSpeaker] = useState("");
    const [region, setRegion] = useState("");

    useEffect(() => {
        loadPronunciations();
    }, [sentenceId]);

    async function loadPronunciations() {
        setLoading(true);
        setError(null);
        try {
            const res = await apiFetch(
                `/api/admin/pronunciations?ownerType=SENTENCE&ownerId=${sentenceId}`,
                {
                    headers: { Accept: "application/json" },
                }
            );
            if (res.ok) {
                const data = await res.json();
                setPronunciations(data);
            } else if (res.status === 401 || res.status === 403) {
                setError("You are not authorized to view pronunciations.");
            } else {
                setError("Failed to load pronunciations.");
            }
        } catch (e: any) {
            setError("Failed to load pronunciations.");
        } finally {
            setLoading(false);
        }
    }

    async function startRecording() {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            mediaStreamRef.current = stream;

            const mediaRecorder = new MediaRecorder(stream, {
                mimeType: 'audio/webm'
            });
            mediaRecorderRef.current = mediaRecorder;

            audioChunksRef.current = [];
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
            };

            mediaRecorder.start();
            setIsRecording(true);
            setRecordingTime(0);

            // Start timer
            recordingTimerRef.current = window.setInterval(() => {
                setRecordingTime((prev) => {
                    const newTime = prev + 1;
                    // Auto-stop at hard max
                    if (newTime >= MAX_RECORDING_DURATION) {
                        stopRecording();
                        return MAX_RECORDING_DURATION;
                    }
                    return newTime;
                });
            }, 1000);
        } catch (err: any) {
            setError(err?.message ?? "Failed to access microphone. Please check permissions.");
        }
    }

    function stopRecording() {
        if (mediaRecorderRef.current && isRecording) {
            mediaRecorderRef.current.stop();
            setIsRecording(false);
        }
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

    async function handleUploadPronunciation(e: React.FormEvent<HTMLFormElement>) {
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
            setError("Please record audio or select an audio file.");
            return;
        }

        // Validate file size (1MB = 1048576 bytes)
        const MAX_FILE_SIZE = 1048576;
        if (audioFile.size > MAX_FILE_SIZE) {
            setError(`File size exceeds 1 MB limit. Current size: ${(audioFile.size / 1024).toFixed(2)} KB`);
            return;
        }

        setUploading(true);
        setError(null);

        try {
            const uploadFormData = new FormData();
            uploadFormData.append("ownerType", "SENTENCE");
            uploadFormData.append("ownerId", sentenceId);
            uploadFormData.append("audioFile", audioFile);
            
            const speakerValue = formData.get("speaker") as string;
            if (speakerValue?.trim()) {
                uploadFormData.append("speaker", speakerValue.trim());
            }
            
            const regionValue = formData.get("region") as string;
            if (regionValue?.trim()) {
                uploadFormData.append("region", regionValue.trim());
            }

            // Calculate duration from recorded audio or use a default
            let durationMs: number | null = null;
            if (recordedAudio) {
                durationMs = recordingTime * 1000;
            }
            if (durationMs !== null) {
                uploadFormData.append("durationMs", String(durationMs));
            }

            const res = await apiFetch("/api/admin/pronunciations", {
                method: "POST",
                body: uploadFormData,
            });

            if (res.status === 401) {
                setError("You are not signed in.");
                return;
            }
            if (res.status === 403) {
                setError("You are not authorized to upload pronunciations.");
                return;
            }
            if (!res.ok) {
                const text = await res.text();
                throw new Error(text || `Failed to upload pronunciation (${res.status})`);
            }

            // Success - reload pronunciations
            await loadPronunciations();
            
            // Clear form and recording
            if (pronunciationFormRef.current) {
                pronunciationFormRef.current.reset();
            }
            setSpeaker("");
            setRegion("");
            clearRecordedAudio();
        } catch (e: any) {
            setError(e?.message ?? "Failed to upload pronunciation.");
        } finally {
            setUploading(false);
        }
    }

    async function handleDeletePronunciation(pronunciationId: string) {
        const confirmMessage = sentenceStatus === "PUBLISHED"
            ? "This sentence is PUBLISHED. Deleting this pronunciation will affect published content. Are you sure you want to delete this pronunciation?"
            : "Are you sure you want to delete this pronunciation?";

        if (!confirm(confirmMessage)) {
            return;
        }

        setError(null);
        try {
            const res = await apiFetch(`/api/admin/pronunciations/${pronunciationId}`, {
                method: "DELETE",
            });

            if (res.status === 401) {
                setError("You are not signed in.");
                return;
            }
            if (res.status === 403) {
                setError("You are not authorized to delete pronunciations.");
                return;
            }
            if (!res.ok) {
                throw new Error(`Failed to delete pronunciation (${res.status})`);
            }

            // Success - reload pronunciations
            await loadPronunciations();
        } catch (e: any) {
            setError(e?.message ?? "Failed to delete pronunciation.");
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

    return (
        <div className="mt-8 border-t border-slate-200 pt-4">
            <h2 className="section-header">Pronunciations</h2>

            {error && (
                <div className="error-message mb-4">
                    {error}
                </div>
            )}

            {/* Existing Pronunciations Table */}
            {loading ? (
                <p className="text-sm text-slate-500">Loading pronunciations...</p>
            ) : pronunciations.length === 0 ? (
                <p className="text-sm text-slate-500 mb-4">No pronunciations yet.</p>
            ) : (
                <div className="mb-6 overflow-x-auto">
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
                                    <td className="w-32">
                                        <button
                                            type="button"
                                            className="action-btn action-btn-danger"
                                            onClick={() => handleDeletePronunciation(pron.id)}
                                            title="Delete pronunciation"
                                            aria-label="Delete pronunciation"
                                        >
                                            <Trash2 size={14} />
                                        </button>
                                    </td>
                                    <td>{pron.speaker || "—"}</td>
                                    <td>{pron.region || "—"}</td>
                                    <td>
                                        {pron.durationMs !== null
                                            ? `${(pron.durationMs / 1000).toFixed(1)}s`
                                            : "—"}
                                    </td>
                                    <td>
                                        <audio
                                            controls
                                            src={`/api/admin/pronunciations/${pron.id}/audio`}
                                            className="h-8"
                                        >
                                            Your browser does not support the audio element.
                                        </audio>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {/* Add Pronunciation Form (Always Visible) */}
            <div className="border border-slate-200 rounded-lg p-4 bg-slate-50">
                <h3 className="text-sm font-medium text-slate-700 mb-4">Add Pronunciation</h3>
                
                <form ref={pronunciationFormRef} onSubmit={handleUploadPronunciation} className="space-y-4">
                    {/* Audio Input */}
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
                                    disabled={uploading || !!recordedAudio}
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
                                    <MicOff size={16} />
                                    Stop Recording
                                </button>
                            )}
                            {recordedAudio && (
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

                        {/* Duration Guidance */}
                        <p className="text-xs text-slate-500 mb-2">
                            Recommended: ≤{RECOMMENDED_DURATION}s. Maximum: {MAX_RECORDING_DURATION}s. Max file size: 1 MB.
                        </p>

                        {/* File Upload */}
                        <input
                            type="file"
                            name="audioFile"
                            accept="audio/webm,audio/mpeg,audio/wav"
                            disabled={isRecording || !!recordedAudio}
                            className="block w-full text-sm text-slate-700 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-medium file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100 disabled:opacity-50 disabled:cursor-not-allowed"
                        />

                        {/* Audio Preview */}
                        {audioUrl && (
                            <div className="mt-3">
                                <audio controls src={audioUrl} className="w-full max-w-md">
                                    Your browser does not support the audio element.
                                </audio>
                            </div>
                        )}
                    </div>

                    {/* Speaker */}
                    <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">
                            Speaker (Optional)
                        </label>
                        <input
                            type="text"
                            name="speaker"
                            value={speaker}
                            onChange={(e) => setSpeaker(e.target.value)}
                            placeholder="e.g., Native Speaker"
                            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                            disabled={uploading}
                        />
                    </div>

                    {/* Region */}
                    <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">
                            Region (Optional)
                        </label>
                        <input
                            type="text"
                            name="region"
                            value={region}
                            onChange={(e) => setRegion(e.target.value)}
                            placeholder="e.g., Maharashtra"
                            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                            disabled={uploading}
                        />
                    </div>

                    {/* Submit Button */}
                    <div className="flex justify-end">
                        <button
                            type="submit"
                            disabled={uploading || isRecording}
                            className={`admin-btn admin-btn-primary ${uploading || isRecording ? "admin-btn-disabled" : ""}`}
                        >
                            {uploading ? "Uploading..." : "Upload Pronunciation"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
