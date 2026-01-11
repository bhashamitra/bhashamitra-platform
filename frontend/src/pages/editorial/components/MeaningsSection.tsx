import { useEffect, useState } from "react";

export interface MeaningDto {
    id: string;
    lemmaId: string;
    meaningLanguage: string;
    meaningText: string;
    priority: number;
}

interface MeaningsSectionProps {
    lemmaId: string;
}

export default function MeaningsSection({ lemmaId }: MeaningsSectionProps) {
    const [meanings, setMeanings] = useState<MeaningDto[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        loadMeanings();
    }, [lemmaId]);

    async function loadMeanings() {
        setLoading(true);
        try {
            const res = await fetch(`/api/admin/meanings?lemmaId=${lemmaId}`, {
                credentials: "include",
                headers: { Accept: "application/json" },
            });
            if (res.ok) {
                const data = await res.json();
                setMeanings(data);
            }
        } catch {
            // silently fail - section will show empty
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="mt-8 border-t border-slate-200 pt-4">
            <div className="flex items-center justify-between mb-3">
                <h2 className="text-sm font-medium text-slate-700">Meanings</h2>
                <button
                    type="button"
                    className="admin-btn admin-btn-default text-xs"
                    onClick={() => {/* TODO: open modal */}}
                >
                    + Add Meaning
                </button>
            </div>

            {loading ? (
                <p className="text-sm text-slate-500">Loading meanings...</p>
            ) : meanings.length === 0 ? (
                <p className="text-sm text-slate-500">No meanings yet.</p>
            ) : (
                <table className="admin-table text-sm">
                    <thead>
                        <tr>
                            <th>Language</th>
                            <th>Meaning</th>
                            <th>Priority</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
                        {meanings.map((m) => (
                            <tr key={m.id}>
                                <td>{m.meaningLanguage}</td>
                                <td>{m.meaningText}</td>
                                <td>{m.priority}</td>
                                <td className="text-right">
                                    <button
                                        type="button"
                                        className="text-blue-600 hover:underline text-xs mr-2"
                                        onClick={() => {/* TODO: open edit modal */}}
                                    >
                                        Edit
                                    </button>
                                    <button
                                        type="button"
                                        className="text-red-600 hover:underline text-xs"
                                        onClick={() => {/* TODO: delete */}}
                                    >
                                        Delete
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}
        </div>
    );
}
