export type RelatedCounts = {
    meanings: number;
    surfaceForms: number;
    sentences: number;
    pronunciations: number;
};

export type LemmaDto = {
    id: string;
    language: string;
    lemmaNative: string;
    lemmaLatin: string | null;
    pos: string | null;
    notes: string | null;
    status: string;
    counts: RelatedCounts | null;
};

export type PagedLemmasResponse = {
    content: LemmaDto[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
};