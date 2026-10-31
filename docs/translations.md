# Translations

Languages and the rules for writing a translation. Moved from CLAUDE.md.

Translations: English (`values/`) plus the languages of grocy-android's app resources (ca, cs,
de, el, es, et, fr, he, hu, it, ja, nb, nl, pl, pt-BR, pt-PT, ru, sk, sv, tr, uk, zh-CN,
zh-TW; regions as `values-pt-rBR`). Hebrew exists twice, `values-he` (browsers) and an
identical `values-iw` (Android still reports the old code, Compose Resources match exactly);
the Android `localeFilters` include `iw` too. Android lays RTL languages out right to left by
itself, the web app sets `LocalLayoutDirection` and `<html dir>` from `isRightToLeft`
(core:model). Claude writes the translations, they are
uploaded to Transifex as "to review" and native speakers correct them there; corrections come
back into the repository. Rules for writing one:

- Use the terms of Grocy's own web frontend, from its translation in the Grocy repository
  (`localization/<language>/*.po`, e.g. "API-Schlüssel verwalten", "Bestand",
  "Hausarbeiten", "Einkaufszettel"), so the app and the web UI say the same.
- Address the user like Grocy does in that language (German "du", French "vous", ...).
  The slogan "ERP beyond your fridge" stays English, so does Home Assistant's "Long-lived
  access token" (German "Long-Lived-Access-Token"). German "theme" is "Farbschema".
- Keep every placeholder (`%1$s`); leave out `translatable="false"` strings.
- A new language: `values-<tag>/strings.xml`, its tag in `AppLanguages` and in
  `locales_config.xml`; `LocalesConfigTest` fails until all three match.
- Long texts in `files/` (changelog, licenses) have no language qualifiers and stay English.
