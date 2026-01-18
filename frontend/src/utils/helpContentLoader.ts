import commonHelp from "../help/lemma-common-help.json";
import createLemmaHelp from "../help/create-lemma-help.json";
import editLemmaHelp from "../help/edit-lemma-help.json";
import meaningCommonHelp from "../help/meaning-common-help.json";
import createMeaningHelp from "../help/create-meaning-help.json";
import editMeaningHelp from "../help/edit-meaning-help.json";
import pronunciationCommonHelp from "../help/pronunciation-common-help.json";
import createPronunciationLemmaHelp from "../help/create-pronunciation-lemma.json";
import createPronunciationSurfaceFormHelp from "../help/create-pronunciation-surface-form.json";
import createPronunciationSentenceHelp from "../help/create-pronunciation-sentence.json";
import surfaceFormCommonHelp from "../help/surface-form-common-help.json";
import createSurfaceFormHelp from "../help/create-surface-form-help.json";
import editSurfaceFormHelp from "../help/edit-surface-form-help.json";
import sentenceCommonHelp from "../help/sentence-common-help.json";
import createSentenceHelp from "../help/create-sentence-help.json";
import editSentenceHelp from "../help/edit-sentence-help.json";
import linkCommonHelp from "../help/link-common-help.json";
import createLinkHelp from "../help/create-link-help.json";
import editLinkHelp from "../help/edit-link-help.json";

interface HelpContent {
    pageId: string;
    title: string;
    sections: any[];
}

interface CommonHelpContent {
    sections: any[];
}

// Helper function to merge field-guidance sections with required-fields
function mergeFieldGuidance(requiredFieldsSection: any, optionalFieldsSection: any) {
    const requiredFields = requiredFieldsSection?.fields || [];
    const optionalFields = (optionalFieldsSection?.fields || []).filter((f: any) => !f.required || f.required === false);
    
    if (requiredFields.length === 0 && optionalFields.length === 0) {
        return null;
    }
    
    return {
        type: "field-guidance",
        requiredFields: requiredFields.length > 0 ? requiredFields : undefined,
        optionalFields: optionalFields.length > 0 ? optionalFields : undefined,
    };
}

export function loadHelpContent(pageId: string): HelpContent {
    // Meanings merge common content with page-specific content
    if (pageId === "create-meaning" || pageId === "edit-meaning") {
        const commonSections = (meaningCommonHelp as CommonHelpContent).sections;
        let pageSpecificContent: HelpContent;
        
        if (pageId === "create-meaning") {
            pageSpecificContent = createMeaningHelp as HelpContent;
        } else {
            pageSpecificContent = editMeaningHelp as HelpContent;
        }

        // Build merged sections with specific ordering
        const mergedSections: any[] = [];
        
        // 1. Definition
        const definitionIndex = commonSections.findIndex(s => s.type === "definition");
        if (definitionIndex >= 0) {
            mergedSections.push(commonSections[definitionIndex]);
        }
        
        // 2. Field Guidance (merge required-fields + field-guidance)
        const requiredFieldsIndex = pageSpecificContent.sections.findIndex(s => s.type === "required-fields");
        const optionalFieldsIndex = commonSections.findIndex(s => s.type === "field-guidance");
        const fieldGuidance = mergeFieldGuidance(
            requiredFieldsIndex >= 0 ? pageSpecificContent.sections[requiredFieldsIndex] : null,
            optionalFieldsIndex >= 0 ? commonSections[optionalFieldsIndex] : null
        );
        if (fieldGuidance) {
            mergedSections.push(fieldGuidance);
        }
        
        // 3. Examples
        const examplesIndex = commonSections.findIndex(s => s.type === "examples");
        if (examplesIndex >= 0) {
            mergedSections.push(commonSections[examplesIndex]);
        }
        
        // 4. Common Mistakes
        const commonMistakesIndex = commonSections.findIndex(s => s.type === "common-mistakes");
        if (commonMistakesIndex >= 0) {
            mergedSections.push(commonSections[commonMistakesIndex]);
        }
        
        // 5. Next Steps
        const nextStepsIndex = pageSpecificContent.sections.findIndex(s => s.type === "next-steps");
        if (nextStepsIndex >= 0) {
            mergedSections.push(pageSpecificContent.sections[nextStepsIndex]);
        }

        return {
            pageId: pageSpecificContent.pageId,
            title: pageSpecificContent.title,
            sections: mergedSections,
        };
    }

    // Pronunciations merge common content with page-specific content
    if (pageId === "create-pronunciation-lemma" || 
        pageId === "create-pronunciation-surface-form" || 
        pageId === "create-pronunciation-sentence") {
        const commonSections = (pronunciationCommonHelp as CommonHelpContent).sections;
        let pageSpecificContent: HelpContent;
        
        switch (pageId) {
            case "create-pronunciation-lemma":
                pageSpecificContent = createPronunciationLemmaHelp as HelpContent;
                break;
            case "create-pronunciation-surface-form":
                pageSpecificContent = createPronunciationSurfaceFormHelp as HelpContent;
                break;
            case "create-pronunciation-sentence":
                pageSpecificContent = createPronunciationSentenceHelp as HelpContent;
                break;
            default:
                throw new Error(`Unknown pronunciation page ID: ${pageId}`);
        }

        // Build merged sections with specific ordering
        const mergedSections: any[] = [];
        
        // 1. Definition
        const definitionIndex = commonSections.findIndex(s => s.type === "definition");
        if (definitionIndex >= 0) {
            mergedSections.push(commonSections[definitionIndex]);
        }
        
        // 2. Field Guidance (merge required-fields + field-guidance)
        const requiredFieldsIndex = pageSpecificContent.sections.findIndex(s => s.type === "required-fields");
        const optionalFieldsIndex = commonSections.findIndex(s => s.type === "field-guidance");
        const fieldGuidance = mergeFieldGuidance(
            requiredFieldsIndex >= 0 ? pageSpecificContent.sections[requiredFieldsIndex] : null,
            optionalFieldsIndex >= 0 ? commonSections[optionalFieldsIndex] : null
        );
        if (fieldGuidance) {
            mergedSections.push(fieldGuidance);
        }
        
        // 3. Common Mistakes
        const commonMistakesIndex = commonSections.findIndex(s => s.type === "common-mistakes");
        if (commonMistakesIndex >= 0) {
            mergedSections.push(commonSections[commonMistakesIndex]);
        }
        
        // 4. Next Steps
        const nextStepsIndex = pageSpecificContent.sections.findIndex(s => s.type === "next-steps");
        if (nextStepsIndex >= 0) {
            mergedSections.push(pageSpecificContent.sections[nextStepsIndex]);
        }

        return {
            pageId: pageSpecificContent.pageId,
            title: pageSpecificContent.title,
            sections: mergedSections,
        };
    }

    // Surface Forms merge common content with page-specific content
    if (pageId === "create-surface-form" || pageId === "edit-surface-form") {
        const commonSections = (surfaceFormCommonHelp as CommonHelpContent).sections;
        let pageSpecificContent: HelpContent;
        
        if (pageId === "create-surface-form") {
            pageSpecificContent = createSurfaceFormHelp as HelpContent;
        } else {
            pageSpecificContent = editSurfaceFormHelp as HelpContent;
        }

        // Build merged sections with specific ordering
        const mergedSections: any[] = [];
        
        // 1. Definition
        const definitionIndex = commonSections.findIndex(s => s.type === "definition");
        if (definitionIndex >= 0) {
            mergedSections.push(commonSections[definitionIndex]);
        }
        
        // 2. Field Guidance (merge required-fields + field-guidance from both common and page-specific)
        const requiredFieldsIndex = pageSpecificContent.sections.findIndex(s => s.type === "required-fields");
        const commonFieldGuidanceIndex = commonSections.findIndex(s => s.type === "field-guidance");
        const pageFieldGuidanceIndex = pageSpecificContent.sections.findIndex(s => s.type === "field-guidance");
        
        // Merge common and page-specific optional fields
        const commonOptionalFields = (commonFieldGuidanceIndex >= 0 ? commonSections[commonFieldGuidanceIndex].fields || [] : []).filter((f: any) => !f.required || f.required === false);
        const pageOptionalFields = (pageFieldGuidanceIndex >= 0 ? pageSpecificContent.sections[pageFieldGuidanceIndex].fields || [] : []).filter((f: any) => !f.required || f.required === false);
        const allOptionalFields = [...commonOptionalFields, ...pageOptionalFields];
        
        const requiredFields = requiredFieldsIndex >= 0 ? pageSpecificContent.sections[requiredFieldsIndex].fields || [] : [];
        
        if (requiredFields.length > 0 || allOptionalFields.length > 0) {
            mergedSections.push({
                type: "field-guidance",
                requiredFields: requiredFields.length > 0 ? requiredFields : undefined,
                optionalFields: allOptionalFields.length > 0 ? allOptionalFields : undefined,
            });
        }
        
        // 3. Common Mistakes
        const commonMistakesIndex = commonSections.findIndex(s => s.type === "common-mistakes");
        if (commonMistakesIndex >= 0) {
            mergedSections.push(commonSections[commonMistakesIndex]);
        }
        
        // 4. Next Steps
        const nextStepsIndex = pageSpecificContent.sections.findIndex(s => s.type === "next-steps");
        if (nextStepsIndex >= 0) {
            mergedSections.push(pageSpecificContent.sections[nextStepsIndex]);
        }

        return {
            pageId: pageSpecificContent.pageId,
            title: pageSpecificContent.title,
            sections: mergedSections,
        };
    }

    // Sentences merge common content with page-specific content
    if (pageId === "create-sentence" || pageId === "edit-sentence") {
        const commonSections = (sentenceCommonHelp as CommonHelpContent).sections;
        let pageSpecificContent: HelpContent;
        
        if (pageId === "create-sentence") {
            pageSpecificContent = createSentenceHelp as HelpContent;
        } else {
            pageSpecificContent = editSentenceHelp as HelpContent;
        }

        // Build merged sections with specific ordering
        const mergedSections: any[] = [];
        
        // 1. Definition
        const definitionIndex = commonSections.findIndex(s => s.type === "definition");
        if (definitionIndex >= 0) {
            mergedSections.push(commonSections[definitionIndex]);
        }
        
        // 2. Field Guidance (merge required-fields + field-guidance)
        const requiredFieldsIndex = pageSpecificContent.sections.findIndex(s => s.type === "required-fields");
        const optionalFieldsIndex = commonSections.findIndex(s => s.type === "field-guidance");
        const fieldGuidance = mergeFieldGuidance(
            requiredFieldsIndex >= 0 ? pageSpecificContent.sections[requiredFieldsIndex] : null,
            optionalFieldsIndex >= 0 ? commonSections[optionalFieldsIndex] : null
        );
        if (fieldGuidance) {
            mergedSections.push(fieldGuidance);
        }
        
        // 3. Examples
        const examplesIndex = commonSections.findIndex(s => s.type === "examples");
        if (examplesIndex >= 0) {
            mergedSections.push(commonSections[examplesIndex]);
        }
        
        // 4. Common Mistakes
        const commonMistakesIndex = commonSections.findIndex(s => s.type === "common-mistakes");
        if (commonMistakesIndex >= 0) {
            mergedSections.push(commonSections[commonMistakesIndex]);
        }
        
        // 5. Next Steps
        const nextStepsIndex = pageSpecificContent.sections.findIndex(s => s.type === "next-steps");
        if (nextStepsIndex >= 0) {
            mergedSections.push(pageSpecificContent.sections[nextStepsIndex]);
        }

        return {
            pageId: pageSpecificContent.pageId,
            title: pageSpecificContent.title,
            sections: mergedSections,
        };
    }

    // Links merge common content with page-specific content
    if (pageId === "create-link" || pageId === "edit-link") {
        const commonSections = (linkCommonHelp as CommonHelpContent).sections;
        let pageSpecificContent: HelpContent;
        
        if (pageId === "create-link") {
            pageSpecificContent = createLinkHelp as HelpContent;
        } else {
            pageSpecificContent = editLinkHelp as HelpContent;
        }

        // Build merged sections with specific ordering
        const mergedSections: any[] = [];
        
        // 1. Definition
        const definitionIndex = commonSections.findIndex(s => s.type === "definition");
        if (definitionIndex >= 0) {
            mergedSections.push(commonSections[definitionIndex]);
        }
        
        // 2. Field Guidance (merge required-fields + field-guidance)
        const requiredFieldsIndex = pageSpecificContent.sections.findIndex(s => s.type === "required-fields");
        const optionalFieldsIndex = commonSections.findIndex(s => s.type === "field-guidance");
        const fieldGuidance = mergeFieldGuidance(
            requiredFieldsIndex >= 0 ? pageSpecificContent.sections[requiredFieldsIndex] : null,
            optionalFieldsIndex >= 0 ? commonSections[optionalFieldsIndex] : null
        );
        if (fieldGuidance) {
            mergedSections.push(fieldGuidance);
        }
        
        // 3. Examples
        const examplesIndex = commonSections.findIndex(s => s.type === "examples");
        if (examplesIndex >= 0) {
            mergedSections.push(commonSections[examplesIndex]);
        }
        
        // 4. Common Mistakes
        const commonMistakesIndex = commonSections.findIndex(s => s.type === "common-mistakes");
        if (commonMistakesIndex >= 0) {
            mergedSections.push(commonSections[commonMistakesIndex]);
        }
        
        // 5. Next Steps
        const nextStepsIndex = pageSpecificContent.sections.findIndex(s => s.type === "next-steps");
        if (nextStepsIndex >= 0) {
            mergedSections.push(pageSpecificContent.sections[nextStepsIndex]);
        }

        return {
            pageId: pageSpecificContent.pageId,
            title: pageSpecificContent.title,
            sections: mergedSections,
        };
    }

    // Lemmas merge common content with page-specific content
    const commonSections = (commonHelp as CommonHelpContent).sections;
    let pageSpecificContent: HelpContent;

    switch (pageId) {
        case "create-lemma":
            pageSpecificContent = createLemmaHelp as HelpContent;
            break;
        case "edit-lemma":
            pageSpecificContent = editLemmaHelp as HelpContent;
            break;
        default:
            throw new Error(`Unknown help page ID: ${pageId}`);
    }

    // Build merged sections with specific ordering
    const mergedSections: any[] = [];
    
    // 1. Definition
    const definitionIndex = commonSections.findIndex(s => s.type === "definition");
    if (definitionIndex >= 0) {
        mergedSections.push(commonSections[definitionIndex]);
    }
    
    // 2. Field Guidance (merge required-fields + field-guidance)
    const requiredFieldsIndex = pageSpecificContent.sections.findIndex(s => s.type === "required-fields");
    const optionalFieldsIndex = commonSections.findIndex(s => s.type === "field-guidance");
    const fieldGuidance = mergeFieldGuidance(
        requiredFieldsIndex >= 0 ? pageSpecificContent.sections[requiredFieldsIndex] : null,
        optionalFieldsIndex >= 0 ? commonSections[optionalFieldsIndex] : null
    );
    if (fieldGuidance) {
        mergedSections.push(fieldGuidance);
    }
    
    // 3. Examples
    const examplesIndex = commonSections.findIndex(s => s.type === "examples");
    if (examplesIndex >= 0) {
        mergedSections.push(commonSections[examplesIndex]);
    }
    
    // 4. Common Mistakes
    const commonMistakesIndex = commonSections.findIndex(s => s.type === "common-mistakes");
    if (commonMistakesIndex >= 0) {
        mergedSections.push(commonSections[commonMistakesIndex]);
    }
    
    // 5. Next Steps
    const nextStepsIndex = pageSpecificContent.sections.findIndex(s => s.type === "next-steps");
    if (nextStepsIndex >= 0) {
        mergedSections.push(pageSpecificContent.sections[nextStepsIndex]);
    }
    
    // 6. Status Workflow (if applicable - check if it exists in page-specific)
    const statusWorkflowIndex = pageSpecificContent.sections.findIndex(s => s.type === "status-workflow");
    if (statusWorkflowIndex >= 0) {
        mergedSections.push(pageSpecificContent.sections[statusWorkflowIndex]);
    }

    return {
        pageId: pageSpecificContent.pageId,
        title: pageSpecificContent.title,
        sections: mergedSections,
    };
}
