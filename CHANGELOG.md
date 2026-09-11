# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- Reward shops support KubeJS-created `yars:reward_shop` block types with shop-scoped trades and persistent per-player history.

### Added

- Native Debug Reward Shop and Debug Automatic Reward Box, available through the Functional Blocks Creative tab or `/give` without custom scripts.
- Shared and independent limited trades, staged and dynamic offers, two-item payments, and tagged-paper token examples.
- Reproducible pixel-written block textures and a complete debug-shop testing guide in `docs/debug-shops.md`.

### Fixed

- Fix production Forge startup by packaging the mixin refmap and applying generated shadow mappings on clean and incremental builds. Build and release tasks now verify the packaged mappings.
- Keep the previous valid trade catalog when scripted registration fails; preserve native offers across reloads and clear stale scripted entries between integrated worlds.
- Align Jade trade separators with item icons.
