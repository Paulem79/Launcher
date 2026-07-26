#!/usr/bin/env python3
import argparse
import json
import sys
import urllib.parse
import urllib.request
from typing import Any, Dict, List, Optional

USER_AGENT = "LauncherMC/1.3.0 (https://github.com/Paulem79/Launcher)"


def extract_project_ref(url: str) -> str:
    """Extrait le slug ou l'ID du projet depuis l'URL Modrinth."""
    if not url:
        return ""
    clean_url = url.rstrip("/")
    last_slash = clean_url.rfind("/")
    return clean_url[last_slash + 1 :] if last_slash != -1 else clean_url


def fetch_exact_version_id(
        project_ref: str,
        target_version: str,
        modloader: str = "forge",
        game_version: str = "1.20.1",
) -> Optional[str]:
    """Interroge l'API Modrinth pour trouver l'ID de version exact correspondant."""
    try:
        loaders_param = json.dumps([modloader.lower()])
        versions_param = json.dumps([game_version])

        query = urllib.parse.urlencode(
            {"loaders": loaders_param, "game_versions": versions_param}
        )

        api_url = f"https://api.modrinth.com/v2/project/{project_ref}/version?{query}"

        req = urllib.request.Request(
            api_url, headers={"User-Agent": USER_AGENT}
        )

        with urllib.request.urlopen(req, timeout=4) as response:
            if response.status != 200:
                return None

            versions = json.loads(response.read().decode("utf-8"))
            if not isinstance(versions, list):
                return None

            fallback_id = None
            target_lower = target_version.lower()

            for ver in versions:
                ver_number = ver.get("version_number", "")
                ver_id = ver.get("id", "")
                ver_lower = ver_number.lower()

                # 1. Match exact
                if ver_lower == target_lower:
                    return ver_id

                # 2. Match partiel (ex: "v2.1.3", "2.1.3+1.20.1")
                if fallback_id is None and (
                        target_lower in ver_lower or ver_lower.startswith(target_lower)
                ):
                    fallback_id = ver_id

            if fallback_id:
                return fallback_id
            if len(versions) > 0:
                return versions[0].get("id")

    except Exception:
        pass

    return None


def parse_modrinth_data(
        input_data: List[Dict[str, Any]],
        modloader: str = "forge",
        game_version: str = "1.20.1",
) -> Dict[str, List[Dict[str, Any]]]:
    """
    Traite la liste initiale et génère le format compatible avec FlowUpdater :
    {
        "modrinthMods": [
            {
                "projectReference": "...",
                "versionNumber": "...",
                "versionId": "..." | null
            }
        ]
    }
    """
    mods_list = []
    total = len(input_data)

    for i, item in enumerate(input_data, start=1):
        if not isinstance(item, dict):
            continue

        raw_url = item.get("url")
        target_version = item.get("version")

        if raw_url and target_version:
            project_ref = extract_project_ref(raw_url)

            if project_ref and target_version:
                sys.stderr.write(
                    f"[{i}/{total}] Traitement de {project_ref} ({target_version})...\n"
                )

                version_id = fetch_exact_version_id(
                    project_ref, target_version, modloader, game_version
                )

                # Structure exacte attendue par ModrinthVersionInfo
                mods_list.append(
                    {
                        "projectReference": project_ref,
                        "versionNumber": target_version,
                        "versionId": version_id,  # Sera sérialisé en null si None
                    }
                )

    return {"modrinthMods": mods_list}


def main():
    parser = argparse.ArgumentParser(
        description="Convertit un JSON de mods en format FlowUpdater ModrinthVersionInfo."
    )
    parser.add_argument(
        "input",
        help="Chemin vers le fichier JSON d'entrée, URL ou '-' pour stdin",
    )
    parser.add_argument(
        "-o",
        "--output",
        help="Fichier de sortie (affiche sur stdout si non spécifié)",
    )
    parser.add_argument(
        "-l",
        "--loader",
        default="forge",
        help="Modloader (ex: forge, fabric, neoforge) - par défaut: forge",
    )
    parser.add_argument(
        "-g",
        "--game-version",
        default="1.20.1",
        help="Version de Minecraft - par défaut: 1.20.1",
    )

    args = parser.parse_args()

    # Lecture de l'entrée
    try:
        if args.input == "-":
            raw_input = sys.stdin.read()
        elif args.input.startswith("http://") or args.input.startswith("https://"):
            req = urllib.request.Request(args.input, headers={"User-Agent": USER_AGENT})
            with urllib.request.urlopen(req) as resp:
                raw_input = resp.read().decode("utf-8")
        else:
            with open(args.input, "r", encoding="utf-8") as f:
                raw_input = f.read()

        data = json.loads(raw_input)
        if not isinstance(data, list):
            sys.stderr.write("Erreur: Le JSON d'entrée doit être un tableau à la racine.\n")
            sys.exit(1)

    except Exception as e:
        sys.stderr.write(f"Impossible de lire l'entrée: {e}\n")
        sys.exit(1)

    # Conversion
    result_structure = parse_modrinth_data(data, args.loader, args.game_version)
    output_json = json.dumps(result_structure, indent=2, ensure_ascii=False)

    # Écriture de la sortie
    if args.output:
        with open(args.output, "w", encoding="utf-8") as f:
            f.write(output_json)
        sys.stderr.write(f"Terminé ! Résultat enregistré dans '{args.output}'.\n")
    else:
        print(output_json)


if __name__ == "__main__":
    main()