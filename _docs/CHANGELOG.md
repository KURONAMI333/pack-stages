# Changelog

All notable changes to Pack Stages will be documented in this file.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) — [Semver](https://semver.org/)

## [Unreleased]

## [0.1.1] - 2026-10-12

### Changed
- Locked items now stay listed in JEI with an "Unlocks at" line by default. Before, they were hidden until unlocked; `"jei": "hide"` still hides them

### Removed
- First-time milestones. Use an advancement with a function reward instead

## [0.1.0] - 2026-10-11

### Added
- Per-player stages defined in datapack JSON that lock making items, using items, workstation blocks and screens
- Optional JEI support: show what unlocks a locked item, or hide it
- `/pack_stages` commands to grant, remove and check stages

### Notes
- Minecraft 1.21.1 NeoForge
