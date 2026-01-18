import { useState } from "react";
import { HelpCircle } from "lucide-react";
import HelpDrawer from "./HelpDrawer";
import { loadHelpContent } from "../utils/helpContentLoader";

interface HelpButtonProps {
    pageId: string;
}

export default function HelpButton({ pageId }: HelpButtonProps) {
    const [isOpen, setIsOpen] = useState(false);

    const content = loadHelpContent(pageId);

    return (
        <>
            <button
                type="button"
                onClick={() => setIsOpen(true)}
                className="help-button"
                title="Show help"
                aria-label="Show help"
            >
                <HelpCircle size={20} />
            </button>

            <HelpDrawer
                isOpen={isOpen}
                onClose={() => setIsOpen(false)}
                helpContent={content}
            />
        </>
    );
}
