import { Link, useLocation } from "react-router-dom";
import { useEffect, useRef, useState } from "react";
import { User } from "lucide-react";

type HeaderProps = {
    loading: boolean;
    isEditorOrAdmin: boolean;
    isLoggedIn: boolean;
    displayName?: string;
};

export default function Header({
                                   loading,
                                   isEditorOrAdmin,
                                   isLoggedIn,
                                   displayName,
                               }: HeaderProps) {
    const [open, setOpen] = useState(false);
    const menuRef = useRef<HTMLDivElement | null>(null);
    const location = useLocation();

    // Close menu on route change
    useEffect(() => {setOpen(false);}, [location.pathname]);

    // Close on outside click (use CLICK, not mousedown)
    useEffect(() => {
        function onDocClick(e: MouseEvent) {
            if (!menuRef.current) return;
            if (!menuRef.current.contains(e.target as Node)) setOpen(false);
        }
        document.addEventListener("click", onDocClick);
        return () => document.removeEventListener("click", onDocClick);
    }, []);

    // Close on ESC
    useEffect(() => {
        function onKey(e: KeyboardEvent) {
            if (e.key === "Escape") setOpen(false);
        }
        document.addEventListener("keydown", onKey);
        return () => document.removeEventListener("keydown", onKey);
    }, []);

    const menuId = "account-menu";

    return (
        <header className="relative z-10 border-b border-slate-200/60 bg-white/70 backdrop-blur">
            <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
                <Link
                    to="/"
                    className="text-xl font-extrabold tracking-tight text-[var(--warriors-blue)]"
                >
                    Bhashamitra
                </Link>

                <div className="flex items-center gap-3">
                    {!loading && isLoggedIn && (
                        <div className="relative" ref={menuRef}>
                            <button
                                type="button"
                                onClick={() => setOpen((v) => !v)}
                                aria-haspopup="menu"
                                aria-expanded={open}
                                aria-controls={menuId}
                                className="inline-flex h-9 w-9 items-center justify-center rounded-full border border-slate-200 bg-white/70 text-slate-700 hover:bg-slate-50"
                                title="Account"
                            >
                                <User className="h-5 w-5" />
                            </button>

                            {open && (
                                <div
                                    id={menuId}
                                    role="menu"
                                    aria-label="Account menu"
                                    className="absolute right-0 z-50 mt-2 w-56 overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm"
                                    // Important: prevent doc click handler from treating clicks inside as outside
                                    onClick={(e) => e.stopPropagation()}
                                >
                                    <div className="px-4 py-3 text-sm">
                                        <div className="text-slate-500">Signed in as</div>
                                        <div className="truncate font-semibold text-slate-800">
                                            {displayName ?? "Account"}
                                        </div>
                                    </div>

                                    <div className="h-px bg-slate-100" />

                                    <button
                                        type="button"
                                        role="menuitem"
                                        disabled
                                        className="block w-full cursor-not-allowed px-4 py-2 text-left text-sm text-slate-400"
                                        title="Coming soon"
                                    >
                                        Preferences
                                    </button>

                                    {isEditorOrAdmin && (
                                        <Link
                                            to="/admin"
                                            role="menuitem"
                                            className="block cursor-pointer px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
                                            onClick={() => setOpen(false)}
                                        >
                                            Editorial Dashboard
                                        </Link>
                                    )}

                                    <a
                                        href="/logout"
                                        role="menuitem"
                                        className="block cursor-pointer px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
                                        onClick={() => setOpen(false)}
                                    >
                                        Logout
                                    </a>
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </header>
    );
}
