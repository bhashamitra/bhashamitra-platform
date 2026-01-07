import { useState } from "react";
import { useMe } from "./hooks/useMe";
import PrivacyPolicy from "./pages/PrivacyPolicy";
import LandingCard from "./components/LandingCard";
import AppShell from "./layout/AppShell";

export default function App() {
    const [showPrivacy, setShowPrivacy] = useState(false);
    const { me, loading, isEditorOrAdmin, displayName } = useMe();

    if (showPrivacy) {
        return (
            <AppShell
                loading={loading}
                isEditorOrAdmin={isEditorOrAdmin}
                isLoggedIn={!!me}
            >
                <div className="absolute top-4 left-4 z-10">
                    <button
                        onClick={() => setShowPrivacy(false)}
                        className="text-sm font-semibold text-[var(--warriors-blue)] underline"
                    >
                        ← Back
                    </button>
                </div>

                <PrivacyPolicy />
            </AppShell>
        );
    }

    return (
        <AppShell
            loading={loading}
            isEditorOrAdmin={isEditorOrAdmin}
            isLoggedIn={!!me}
        >
            <LandingCard
                loading={loading}
                displayName={displayName}
                onOpenPrivacy={() => setShowPrivacy(true)}
            />
        </AppShell>
    );
}
