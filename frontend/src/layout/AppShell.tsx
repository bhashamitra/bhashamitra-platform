import Header from "../components/Header";
import Footer from "../components/Footer";
import { usePublicVersion } from "../hooks/usePublicVersion";

type AppShellProps = {
    loading: boolean;
    isEditorOrAdmin: boolean;
    isLoggedIn: boolean;
    displayName?: string;
    children: React.ReactNode;
};

export default function AppShell({
                                     loading,
                                     isEditorOrAdmin,
                                     isLoggedIn,
                                     displayName,
                                     children,
                                 }: AppShellProps) {
    const { version, loading: versionLoading } = usePublicVersion();

    return (
        <div className="h-screen bg-[var(--page-bg)] relative flex flex-col overflow-hidden">
            {/* soft background wash */}
            <div
                className="absolute inset-0 opacity-10 pointer-events-none z-0"
                style={{
                    background:
                        "radial-gradient(circle at 20% 20%, var(--warriors-gold), transparent 45%), radial-gradient(circle at 80% 30%, var(--warriors-blue), transparent 50%)",
                }}
            />

            {/* Keep header clearly above main */}
            <div className="relative z-30">
                <Header
                    loading={loading}
                    isEditorOrAdmin={isEditorOrAdmin}
                    isLoggedIn={isLoggedIn}
                    displayName={displayName}
                />
            </div>

            {/* Only this area scrolls; keep it below header/footer */}
            <main className="relative z-10 flex-1 overflow-y-auto">{children}</main>

            {/* Keep footer clearly above main */}
            <div className="relative z-30">
                <Footer version={version} loading={versionLoading} />
            </div>
        </div>
    );
}
