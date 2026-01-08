import { Link } from "react-router-dom";

type TopRightNavProps = {
    loading: boolean;
    isEditorOrAdmin: boolean;
    isLoggedIn: boolean;
};

export default function TopRightNav({
                                        loading,
                                        isEditorOrAdmin,
                                        isLoggedIn,
                                    }: TopRightNavProps) {
    return (
        <div className="absolute top-4 right-4 z-10 flex gap-2">
            {!loading && isEditorOrAdmin && (
                <Link
                    to="/admin"
                    className="inline-flex items-center rounded-md border border-slate-200 bg-white/70 backdrop-blur px-3 py-2 text-sm font-semibold text-[var(--warriors-blue)] hover:bg-slate-50"
                >
                    Admin
                </Link>
            )}

            {isLoggedIn && (
                <a
                    href="/logout"
                    className="inline-flex items-center rounded-md border px-3 py-2 text-sm font-semibold bg-white/70 backdrop-blur border-[var(--warriors-gold)] text-[var(--warriors-blue)] hover:bg-[var(--warriors-gold)]/30"
                >
                    Logout
                </a>
            )}
        </div>
    );
}
