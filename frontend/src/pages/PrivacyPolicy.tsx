export default function PrivacyPolicy() {
    return (
        <div className="min-h-screen bg-[var(--page-bg)] px-6 py-12">
            <div className="mx-auto max-w-3xl rounded-2xl border border-slate-200 bg-white/90 backdrop-blur p-8 shadow-sm">
                <h1 className="text-3xl font-extrabold text-[var(--warriors-blue)]">
                    Bhashamitra Privacy Policy
                </h1>

                <p className="mt-4 text-slate-700">
                    Bhashamitra is committed to protecting the privacy of its users. This
                    Privacy Policy explains how we collect, use, and safeguard your
                    information when you use our website and services.
                </p>

                <p className="mt-4 text-slate-700">
                    Third-party websites or services that you may access through links on
                    Bhashamitra operate under their own privacy policies. We encourage you
                    to review the privacy policies of those third parties before providing
                    any personal information.
                </p>

                {/* Collection */}
                <h2 className="mt-8 text-xl font-bold text-[var(--warriors-blue)]">
                    Collection and Use of User Information
                </h2>

                <p className="mt-3 text-slate-700">
                    When you register with Bhashamitra, we may collect personal information
                    such as your name, email address, and other details you choose to
                    provide through registration or account-related forms.
                </p>

                <p className="mt-3 text-slate-700">
                    When you visit our website, we may also automatically collect certain
                    information about your device and usage, including your IP address,
                    browser type and version, operating system, referring URLs, and
                    general site usage patterns.
                </p>

                <p className="mt-3 text-slate-700">
                    We use this information to:
                </p>

                <ul className="mt-3 list-disc pl-6 text-slate-700 space-y-1">
                    <li>Personalize content and improve user experience</li>
                    <li>Notify you of new features, updates, or products (if you opt in)</li>
                    <li>Communicate promotions or announcements (if you opt in)</li>
                    <li>
                        Ensure compatibility with commonly used browsers and operating
                        systems
                    </li>
                </ul>

                <p className="mt-4 text-slate-700">
                    We may also use aggregated and anonymized information for internal
                    research purposes, including understanding user needs, improving our
                    services, and guiding strategic development. This information does not
                    identify individual users.
                </p>

                {/* User Choices */}
                <h2 className="mt-8 text-xl font-bold text-[var(--warriors-blue)]">
                    User Choices
                </h2>

                <p className="mt-3 text-slate-700">
                    Where applicable, Bhashamitra provides you with choices regarding
                    promotional communications. You may opt in or opt out of receiving
                    updates, announcements, or promotional messages at any time.
                </p>

                {/* Cookies */}
                <h2 className="mt-8 text-xl font-bold text-[var(--warriors-blue)]">
                    Cookies
                </h2>

                <p className="mt-3 text-slate-700">
                    Cookies are small files stored on your device that help us recognize
                    returning users and improve site functionality. Bhashamitra uses
                    cookies to maintain session state, understand how users interact with
                    the site, and improve overall usability.
                </p>

                <p className="mt-3 text-slate-700">
                    You may delete or disable cookies through your browser settings.
                    Please note that disabling cookies may affect certain features and
                    require you to log in again during future visits.
                </p>

                <p className="mt-3 text-slate-700">
                    You can find more information about cookies at{" "}
                    <a
                        href="https://www.cookiecentral.com"
                        target="_blank"
                        rel="noreferrer"
                        className="font-semibold text-[var(--warriors-blue)] underline"
                    >
                        cookiecentral.com
                    </a>
                    .
                </p>

                {/* Security */}
                <h2 className="mt-8 text-xl font-bold text-[var(--warriors-blue)]">
                    Security
                </h2>

                <p className="mt-3 text-slate-700">
                    Bhashamitra uses reasonable administrative, technical, and
                    organizational measures to protect personal information against
                    unauthorized access, loss, misuse, or alteration. However, no method
                    of transmission or storage is completely secure.
                </p>

                {/* Updates */}
                <h2 className="mt-8 text-xl font-bold text-[var(--warriors-blue)]">
                    Updating Your Information
                </h2>

                <p className="mt-3 text-slate-700">
                    If you have an account with Bhashamitra, you may update your personal
                    information or communication preferences through your account
                    settings when such functionality is available.
                </p>

                {/* Changes */}
                <h2 className="mt-8 text-xl font-bold text-[var(--warriors-blue)]">
                    Changes to This Policy
                </h2>

                <p className="mt-3 text-slate-700">
                    We may update this Privacy Policy from time to time to reflect changes
                    in legal requirements, best practices, or site functionality. Updates
                    will be posted on this page, and we encourage you to review it
                    periodically.
                </p>

                <p className="mt-6 text-sm text-slate-500">
                    Last updated: {new Date().toLocaleDateString()}
                </p>
            </div>
        </div>
    );
}
