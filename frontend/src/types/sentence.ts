export interface UsageSentenceDto {
    id: string;
    language: string;
    sentenceNative: string;
    sentenceLatin: string | null;
    translation: string | null;
    register: string | null;
    explanation: string | null;
    difficulty: number | null;
    status: string | null;
}

export interface PagedSentencesResponse {
    content: UsageSentenceDto[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
}

export interface LemmaSentenceLinkDto {
    id: string;
    lemmaId: string;
    sentenceId: string;
    meaningId: string | null;
    surfaceFormId: string | null;
    linkType: string;
}
