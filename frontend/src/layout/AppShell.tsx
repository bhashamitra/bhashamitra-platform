import TopRightNav from "../components/TopRightNav";

type AppShellProps = {
    loading: boolean;
    isEditorOrAdmin: boolean;
    isLoggedIn: boolean;
    children: React.ReactNode;
};

export default function AppShell({
                                     loading,
                                     isEditorOrAdmin,
                                     isLoggedIn,
                                     children,
                                 }: AppShellProps) {
    return (
        <div className="min-h-screen bg-[var(--page-bg)] relative">
            {/* soft background wash */}
            <div
                className="absolute inset-0 opacity-10 pointer-events-none"
                style={{
                    background:
                        "radial-gradient(circle at 20% 20%, var(--warriors-gold), transparent 45%), radial-gradient(circle at 80% 30%, var(--warriors-blue), transparent 50%)",
                }}
            />

            <TopRightNav
                loading={loading}
                isEditorOrAdmin={isEditorOrAdmin}
                isLoggedIn={isLoggedIn}
            />

            {/* Page content */}
            <div className="relative z-0">{children}</div>
        </div>
    );
}
