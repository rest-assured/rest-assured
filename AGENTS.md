# Agent guidelines for rest-assured

Guidance for AI agents (and humans) working in this repository.

## Changelog

Record every end-user-affecting change in `changelog.txt`, under the top
`Changelog next version` section.

- **User-facing changes always go in.** If a consumer of the library can observe
  the change (behavior, API, dependency, bug fix), add a concise entry. Credit the
  PR and contributor when applicable, e.g. `(#1861) (thanks to Anusha-7254 for PR)`.
- **Build-only and example-only changes stay out** by default (Maven plugin config,
  CI, internal example modules, and similar have no user impact).
- **Exception:** a build-only or example-only change that comes from an external
  contributor PR may still get an entry purely for attribution.
- Commit changelog-only updates with the message
  `[ci skip] Updated changelog to reflect the latest changes`.

Rule of thumb before committing: ask "does a consumer of the library observe this?"
Yes means it belongs in the changelog. No means leave the changelog alone, unless
you are crediting an outside contributor.
