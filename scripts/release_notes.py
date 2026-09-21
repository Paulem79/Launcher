#!/usr/bin/env python3
"""Generates the GitHub release notes (markdown) from the conventional commits since the previous release.

Usage: release_notes.py [--repo owner/name] [--from <ref>] [--to <ref>]
Without --from, the most recent "v<N>" tag reachable from --to is used (or the whole history if none).
"""
import argparse
import re
import subprocess

# (type, heading) in display order
CATEGORIES = [
    ("feat", "✨ Features"),
    ("fix", "🐛 Bug Fixes"),
    ("perf", "⚡ Performance"),
    ("refactor", "♻️ Refactoring"),
    ("docs", "📚 Documentation"),
    ("style", "🎨 Style"),
    ("test", "✅ Tests"),
    ("build", "📦 Build System"),
    ("ci", "👷 CI"),
    ("chore", "🔧 Chores"),
    ("revert", "⏪ Reverts"),
]
BREAKING = "🚨 Breaking Changes"
OTHER = "📝 Other Changes"

CONVENTIONAL = re.compile(r"^(?P<type>[a-zA-Z]+)(?:\((?P<scope>[^)]*)\))?(?P<breaking>!)?:\s*(?P<subject>.+)$")


def git(*args):
    return subprocess.run(["git", *args], check=True, capture_output=True, text=True).stdout.strip()


def previous_tag(to):
    tags = git("tag", "--list", "v[0-9]*", "--merged", to, "--sort=-v:refname").splitlines()
    head = git("rev-parse", to)
    for tag in tags:
        # The tag of the release being built may already exist, it is not the previous one
        if git("rev-list", "-n", "1", tag) != head:
            return tag
    return None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", default="Paulem79/Launcher")
    parser.add_argument("--from", dest="since")
    parser.add_argument("--to", default="HEAD")
    args = parser.parse_args()

    since = args.since or previous_tag(args.to)
    rev_range = f"{since}..{args.to}" if since else args.to
    log = git("log", rev_range, "--no-merges", "--format=%H%x1f%s")

    sections = {}
    for line in log.splitlines():
        sha, subject = line.split("\x1f", 1)
        match = CONVENTIONAL.match(subject)
        known = {name for name, _ in CATEGORIES}

        if match and match["type"].lower() in known:
            kind, scope, text = match["type"].lower(), match["scope"], match["subject"]
        else:
            kind, scope, text = None, None, subject
        heading = dict(CATEGORIES).get(kind, OTHER)

        text = text[0].upper() + text[1:]
        entry = f"- {'**' + scope + ':** ' if scope else ''}{text} ([`{sha[:7]}`](https://github.com/{args.repo}/commit/{sha}))"
        sections.setdefault(heading, []).append(entry)
        if match and match["breaking"]:
            sections.setdefault(BREAKING, []).append(entry)

    order = [BREAKING] + [heading for _, heading in CATEGORIES] + [OTHER]
    notes = []
    for heading in order:
        if heading in sections:
            notes.append(f"### {heading}\n\n" + "\n".join(sections[heading]))

    if not notes:
        notes.append("No notable changes.")
    if since:
        notes.append(f"**Full Changelog**: https://github.com/{args.repo}/compare/{since}...{git('rev-parse', args.to)}")

    print("\n\n".join(notes))


if __name__ == "__main__":
    main()
