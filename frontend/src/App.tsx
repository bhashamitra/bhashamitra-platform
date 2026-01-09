import { Routes, Route } from "react-router-dom";
import { useMe } from "./hooks/useMe";
import AppShell from "./layout/AppShell";
import LandingPage from "./components/LandingPage";
import PrivacyPolicy from "./pages/PrivacyPolicy";
import AdminDashboard from "./pages/AdminDashboard";

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
                <Route path="/admin" element={<AdminDashboard />} />
            </Routes>
        </AppShell>
    );
}
