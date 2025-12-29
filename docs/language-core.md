# BhashaMitra Language Core

Version: 1.0  
Last reviewed: 2025-12-22

---

## Purpose

The Language Core defines the foundational, language-agnostic concepts used by
the BhashaMitra platform to model, represent, and teach living languages.

It establishes what is considered **fundamental language data** versus
higher-level product features. This document acts as a long-term contract
that guides all future development.

---

## Scope

The Language Core applies uniformly across all supported languages,
including (but not limited to) Marathi, Hindi, Gujarati, and Tamil.

It must remain stable and consistent as additional languages and features
are introduced.

---

## Included in the Language Core

The following are considered first-class primitives and must be supported
by all implementations:

### 1. Words (Lemmas)
- Canonical dictionary form of a word in its native script
- Each lemma represents a single conceptual word entry and serves as the primary anchor for meanings, usage sentences, surface forms, and pronunciation.

### 2. Surface Forms
- Inflected or conjugated forms of a lemma
- Common spelling variants and transliterations
- Frequently observed spoken or informal forms

### 3. Usage Sentences
- Real-world sentences demonstrating actual language usage
- Sentences may be tagged by register (spoken, formal, neutral)
- Sentences are linked explicitly to the lemmas they contain

### 4. Meanings
- Concise, learner-friendly meanings
- Meanings may be multilingual (e.g., English, Hindi)
- Meanings prioritize clarity over exhaustiveness

### 5. Pronunciation
- Audio pronunciation for lemmas
- Multiple pronunciations may exist (speaker, region)
- Pronunciation focuses on natural, neutral usage and does not aim to catalog
  all dialectal or phonetic variations.
---

## Explicitly Excluded from the Language Core

The following are intentionally not part of the Language Core and must be
layered on top of it if added in the future:

- Full grammar engines or rule systems
- AI-generated definitions or explanations
- Crowdsourced or user-generated content workflows
- User accounts, progress tracking, or gamification
- Mobile applications
- Platform-specific UI concerns

---

## Design Principles

- **Usage before theory**  
  Language is modeled based on how it is actually used, not abstract rules.

- **Spoken language is first-class**  
  Informal and spoken forms are treated as legitimate language data.

- **One concept, one canonical representation**  
  Each linguistic concept should have a single authoritative form.

- **Language-agnostic core**  
  The same core model must support multiple Indian languages without
  special-casing.

- **Quality over quantity**  
  Depth and correctness are prioritized over size.

---

## Evolution Policy

The Language Core is expected to evolve slowly.

Changes must be:
- Intentional
- Backward-conscious
- Clearly documented in version history

Frequent changes indicate boundary leakage and should be avoided.

---

## Status

Active and authoritative as of the above review date.
