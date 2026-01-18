import { useEffect } from "react";
import { X } from "lucide-react";

interface HelpDrawerProps {
    isOpen: boolean;
    onClose: () => void;
    helpContent: HelpContent;
}

interface HelpContent {
    pageId: string;
    title: string;
    sections: HelpSection[];
}

interface HelpSection {
    type: string;
    title?: string;
    content?: string;
    fields?: any[];
    requiredFields?: any[];
    optionalFields?: any[];
    items?: any[];
}

export default function HelpDrawer({ isOpen, onClose, helpContent }: HelpDrawerProps) {
    // Close drawer on Escape key
    useEffect(() => {
        function handleEscape(e: KeyboardEvent) {
            if (e.key === "Escape" && isOpen) {
                onClose();
            }
        }
        document.addEventListener("keydown", handleEscape);
        return () => document.removeEventListener("keydown", handleEscape);
    }, [isOpen, onClose]);

    // Prevent body scroll when drawer is open
    useEffect(() => {
        if (isOpen) {
            document.body.style.overflow = "hidden";
        } else {
            document.body.style.overflow = "";
        }
        return () => {
            document.body.style.overflow = "";
        };
    }, [isOpen]);

    return (
        <>
            {/* Backdrop - only show when open */}
            {isOpen && (
                <div
                    className="fixed inset-0 z-[55] bg-black/20 transition-opacity"
                    onClick={onClose}
                    aria-hidden="true"
                />
            )}

            {/* Drawer */}
            <div className={`help-drawer ${isOpen ? "help-drawer-open" : ""}`}>
                {/* Header */}
                <div className="help-drawer-header">
                    <h2 className="help-drawer-title">{helpContent.title}</h2>
                    <button
                        onClick={onClose}
                        className="help-drawer-close-btn"
                        aria-label="Close help"
                    >
                        <X size={20} />
                    </button>
                </div>

                {/* Content */}
                <div className="help-drawer-content">
                    {helpContent.sections.map((section, idx) => (
                        <HelpSection key={idx} section={section} />
                    ))}
                </div>
            </div>
        </>
    );
}

function HelpSection({ section }: { section: HelpSection }) {
    switch (section.type) {
        case "definition":
            return (
                <div className="help-section">
                    <h3 className="help-section-title">{section.title}</h3>
                    <p className="help-section-text">{section.content}</p>
                </div>
            );

        case "required-fields":
            return (
                <div className="help-section">
                    <h3 className="help-section-title">{section.title}</h3>
                    {section.fields?.map((field, idx) => (
                        <div key={idx} className="help-field-item">
                            <h4 className="help-field-name">{field.name}</h4>
                            <p className="help-field-description">{field.description}</p>
                            <p className="help-field-guidance">{field.guidance}</p>
                        </div>
                    ))}
                </div>
            );

        case "field-guidance":
            return (
                <div className="help-section">
                    <h3 className="help-section-title">Field Guidance</h3>
                    
                    {/* Required Fields Subsection */}
                    {section.requiredFields && section.requiredFields.length > 0 && (
                        <div className="mb-6">
                            <h4 className="help-subsection-title">Required Fields</h4>
                            {section.requiredFields.map((field: any, idx: number) => (
                                <div key={idx} className="help-field-item">
                                    <h4 className="help-field-name">{field.name}</h4>
                                    <p className="help-field-description">{field.description}</p>
                                    <p className="help-field-guidance">{field.guidance}</p>
                                </div>
                            ))}
                        </div>
                    )}
                    
                    {/* Optional Fields Subsection */}
                    {section.optionalFields && section.optionalFields.length > 0 && (
                        <div>
                            <h4 className="help-subsection-title">Optional Fields</h4>
                            {section.optionalFields.map((field: any, idx: number) => (
                                <div key={idx} className="help-field-item">
                                    <h4 className="help-field-name">{field.name}</h4>
                                    <p className="help-field-description">{field.description}</p>
                                    <p className="help-field-guidance">{field.guidance}</p>
                                    {field.whenToFill && (
                                        <p className="help-field-note">
                                            <strong>When to fill:</strong> {field.whenToFill}
                                        </p>
                                    )}
                                    {field.precision && (
                                        <p className="help-field-note">{field.precision}</p>
                                    )}
                                    {field.purpose && (
                                        <p className="help-field-note">
                                            <strong>Purpose:</strong> {field.purpose}
                                        </p>
                                    )}
                                    {field.examples && field.examples.length > 0 && (
                                        <div className="help-examples">
                                            <strong className="help-examples-title">Examples:</strong>
                                            <ul className="help-examples-list">
                                                {field.examples.map((ex: any, exIdx: number) => (
                                                    <li key={exIdx} className="help-example-item">
                                                        {ex.native && (
                                                            <span className="help-example-native">
                                                                {ex.native}
                                                            </span>
                                                        )}
                                                        {ex.latin && (
                                                            <span className="help-example-latin">
                                                                {" "}
                                                                / {ex.latin}
                                                            </span>
                                                        )}
                                                        {ex.lemma && (
                                                            <span className="help-example-lemma">
                                                                {ex.lemma}
                                                            </span>
                                                        )}
                                                        {ex.pos && (
                                                            <span className="help-example-pos">
                                                                {" "}
                                                                ({ex.pos})
                                                            </span>
                                                        )}
                                                        {ex.note && (
                                                            <span className="help-example-note">
                                                                {" "}
                                                                — {ex.note}
                                                            </span>
                                                        )}
                                                        {ex.form && (
                                                            <span className="help-example-native">
                                                                {ex.form}
                                                            </span>
                                                        )}
                                                        {ex.features && (
                                                            <span className="help-example-note">
                                                                {" "}
                                                                — {ex.features}
                                                            </span>
                                                        )}
                                                    </li>
                                                ))}
                                            </ul>
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            );

        case "examples":
            return (
                <div className="help-section">
                    <h3 className="help-section-title">{section.title}</h3>
                    <ul className="help-examples-list">
                        {section.items?.map((item, idx) => (
                            <li key={idx} className="help-example-item">
                                {item.native && (
                                    <span className="help-example-native">{item.native}</span>
                                )}
                                {item.meaning && (
                                    <span className="help-example-note"> → {item.meaning}</span>
                                )}
                                {item.latin && !item.meaning && (
                                    <span className="help-example-latin"> / {item.latin}</span>
                                )}
                                {item.language && (
                                    <span className="help-example-pos"> ({item.language})</span>
                                )}
                                {item.pos && !item.language && (
                                    <span className="help-example-pos"> ({item.pos})</span>
                                )}
                                {item.note && (
                                    <span className="help-example-note"> — {item.note}</span>
                                )}
                            </li>
                        ))}
                    </ul>
                </div>
            );

        case "common-mistakes":
            return (
                <div className="help-section">
                    <h3 className="help-section-title">{section.title}</h3>
                    {section.items?.map((item, idx) => (
                        <div key={idx} className="help-mistake-item">
                            <h4 className="help-mistake-mistake">{item.mistake}</h4>
                            {item.example && (
                                <p className="help-mistake-example">
                                    <strong>Example:</strong> {item.example}
                                </p>
                            )}
                            <p className="help-mistake-solution">
                                <strong>Solution:</strong> {item.solution}
                            </p>
                        </div>
                    ))}
                </div>
            );

        case "next-steps":
            return (
                <div className="help-section">
                    <h3 className="help-section-title">{section.title}</h3>
                    {section.items?.map((item, idx) => (
                        <div key={idx} className="help-next-steps-item">
                            {item.step && (
                                <h4 className="help-next-steps-step">{item.step}</h4>
                            )}
                            {item.actions && (
                                <ul className="help-next-steps-list">
                                    {item.actions.map((action: string, actIdx: number) => (
                                        <li key={actIdx}>{action}</li>
                                    ))}
                                </ul>
                            )}
                            {item.content && <p className="help-next-steps-content">{item.content}</p>}
                        </div>
                    ))}
                </div>
            );

        default:
            return null;
    }
}
