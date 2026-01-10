import { Routes, Route, Navigate } from "react-router-dom";
import { useMe } from "./hooks/useMe";
import AppShell from "./layout/AppShell";
import LandingPage from "./components/LandingPage";
import PrivacyPolicy from "./pages/PrivacyPolicy";
import EditorialDashboard from "./pages/editorial/EditorialDashboard.tsx";
import EditorialLayout from "./layout/EditorialLayout.tsx";
import LemmasPage from "./pages/editorial/LemmasPage.tsx";
import SentencesPage from "./pages/editorial/SentencesPage.tsx";
import PronunciationsPage from "./pages/editorial/PronunciationsPage.tsx";
import LanguagesPage from "./pages/admin/LanguagesPage.tsx";

export default function App() {
    const { me, loading, isEditorOrAdmin, displayName } = useMe();

    return (
        <AppShell
            loading={loading}
            isEditorOrAdmin={isEditorOrAdmin}
            isLoggedIn={!!me}
            displayName={displayName}
        >
            <Routes>
                <Route path="/" element={<LandingPage />} />
                <Route path="/privacy" element={<PrivacyPolicy />} />

                {/* Admin area */}
                <Route path="/admin" element={<EditorialLayout />}>
                    <Route index element={<EditorialDashboard />} />
                    <Route path="lemmas" element={<LemmasPage />} />
                    <Route path="sentences" element={<SentencesPage />} />
                    <Route path="pronunciations" element={<PronunciationsPage />} />
                    <Route path="languages" element={<LanguagesPage />} />
                </Route>

                {/* Optional: if someone hits /admin/... unknown */}
                <Route path="/admin/*" element={<Navigate to="/admin" replace />} />
            </Routes>
        </AppShell>
    );
}
