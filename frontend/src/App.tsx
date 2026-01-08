
import { useMe } from "./hooks/useMe";
import PrivacyPolicy from "./pages/PrivacyPolicy";
import LandingCard from "./components/LandingCard";
import AppShell from "./layout/AppShell";
import { Routes, Route, Link } from "react-router-dom";
import AdminDashboard from "./pages/AdminDashboard";

export default function App() {
    const { me, loading, isEditorOrAdmin, displayName } = useMe();

    return (
        <AppShell
            loading={loading}
            isEditorOrAdmin={isEditorOrAdmin}
            isLoggedIn={!!me}
        >
            <Routes>
                <Route
                    path="/"
                    element={
                        <LandingCard
                            loading={loading}
                            displayName={displayName}
                        />
                    }
                />
                <Route
                    path="/privacy"
                    element={
                        <div className="relative">
                            <div className="absolute top-4 left-4 z-10">
                                <Link
                                    to="/"
                                    className="text-sm font-semibold text-[var(--warriors-blue)] underline"
                                >
                                    ← Back
                                </Link>
                            </div>
                            <PrivacyPolicy />
                        </div>
                    }
                />


                <Route path="/admin" element={<AdminDashboard />} />
            </Routes>
        </AppShell>
    );

}
