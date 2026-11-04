#!/usr/bin/env python3
"""Check the hand-written API contract against the committed service documents.

Compares every (path, method) operation and every schema shared between the
contract and a service document. Prints every mismatch and exits 1 when one is
found, or 0 when the documents agree.
"""

from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

try:
    import yaml
except ModuleNotFoundError:
    sys.stderr.write(
        "PyYAML is required to run this check: "
        "python3 -m pip install PyYAML\n"
    )
    sys.exit(1)


REPO_ROOT = Path(__file__).resolve().parents[2]
CONTRACT = REPO_ROOT / "docs" / "api" / "contract" / "openapi.yaml"
SERVICE_DOCS = {
    "sky-offer": REPO_ROOT / "docs" / "api" / "openapi" / "sky-offer.openapi.yaml",
    "sky-booking": REPO_ROOT / "docs" / "api" / "openapi" / "sky-booking.openapi.yaml",
    "sky-message": REPO_ROOT / "docs" / "api" / "openapi" / "sky-message.openapi.yaml",
}
HTTP_METHODS = (
    "get",
    "put",
    "post",
    "delete",
    "options",
    "head",
    "patch",
    "trace",
)


def load_document(path: Path) -> dict[str, Any]:
    if not path.is_file():
        sys.stderr.write(f"Missing OpenAPI document: {path}\n")
        sys.exit(1)
    try:
        document = yaml.safe_load(path.read_text(encoding="utf-8"))
    except yaml.YAMLError as error:
        sys.stderr.write(f"Cannot parse {path}: {error}\n")
        sys.exit(1)
    if not isinstance(document, dict):
        sys.stderr.write(f"{path} does not contain an OpenAPI object\n")
        sys.exit(1)
    return document


def operations(document: dict[str, Any]) -> set[tuple[str, str]]:
    found: set[tuple[str, str]] = set()
    for path, path_item in document.get("paths", {}).items():
        if not isinstance(path_item, dict):
            continue
        for method in HTTP_METHODS:
            if method in path_item:
                found.add((path, method))
    return found


def schemas(document: dict[str, Any]) -> dict[str, Any]:
    return document.get("components", {}).get("schemas", {})


def type_signature(schema: Any) -> str:
    if not isinstance(schema, dict):
        return "any"

    reference = schema.get("$ref")
    if isinstance(reference, str):
        return f"ref:{reference.rsplit('/', 1)[-1]}"

    declared = schema.get("type")
    if declared == "array":
        return f"array<{type_signature(schema.get('items', {}))}>"
    if isinstance(declared, str):
        return declared

    for combinator in ("allOf", "oneOf", "anyOf"):
        if combinator in schema:
            parts = ",".join(
                type_signature(part) for part in schema[combinator]
            )
            return f"{combinator}<{parts}>"

    return "any"


def compare_schema(
    name: str,
    service: str,
    contract_schema: dict[str, Any],
    service_schema: dict[str, Any],
    mismatches: list[str],
) -> None:
    contract_properties = contract_schema.get("properties", {})
    service_properties = service_schema.get("properties", {})

    if not isinstance(contract_properties, dict):
        contract_properties = {}
    if not isinstance(service_properties, dict):
        service_properties = {}

    for prop in sorted(set(contract_properties) - set(service_properties)):
        mismatches.append(
            f"schema {name} ({service}): property '{prop}' is in the contract "
            "but not in the service document"
        )
    for prop in sorted(set(service_properties) - set(contract_properties)):
        mismatches.append(
            f"schema {name} ({service}): property '{prop}' is in the service "
            "document but not in the contract"
        )

    contract_required = set(contract_schema.get("required", []))
    service_required = set(service_schema.get("required", []))
    for prop in sorted(contract_required - service_required):
        mismatches.append(
            f"schema {name} ({service}): property '{prop}' is required in the "
            "contract but not in the service document"
        )
    for prop in sorted(service_required - contract_required):
        mismatches.append(
            f"schema {name} ({service}): property '{prop}' is required in the "
            "service document but not in the contract"
        )

    for prop in sorted(set(contract_properties) & set(service_properties)):
        contract_type = type_signature(contract_properties[prop])
        service_type = type_signature(service_properties[prop])
        if contract_type != service_type:
            mismatches.append(
                f"schema {name} ({service}): property '{prop}' has type "
                f"'{contract_type}' in the contract and '{service_type}' in "
                "the service document"
            )


def main() -> int:
    contract = load_document(CONTRACT)
    service_documents = {
        name: load_document(path) for name, path in SERVICE_DOCS.items()
    }

    mismatches: list[str] = []

    contract_operations = operations(contract)
    service_operations: dict[tuple[str, str], list[str]] = {}
    for service, document in service_documents.items():
        for operation in operations(document):
            service_operations.setdefault(operation, []).append(service)

    for path, method in sorted(contract_operations):
        owners = service_operations.get((path, method), [])
        label = f"operation {method.upper()} {path}"
        if not owners:
            mismatches.append(
                f"{label}: in the contract but missing from every service document"
            )
        elif len(owners) > 1:
            mismatches.append(
                f"{label}: appears in more than one service document "
                f"({', '.join(sorted(owners))})"
            )

    for path, method in sorted(service_operations):
        if (path, method) not in contract_operations:
            owners = ", ".join(sorted(service_operations[(path, method)]))
            mismatches.append(
                f"operation {method.upper()} {path}: in {owners} but missing "
                "from the contract"
            )

    contract_schemas = schemas(contract)
    shared_schema_names: set[str] = set()
    for service, document in service_documents.items():
        service_schemas = schemas(document)
        common = set(contract_schemas) & set(service_schemas)
        shared_schema_names.update(common)
        for name in sorted(common):
            compare_schema(
                name,
                service,
                contract_schemas[name],
                service_schemas[name],
                mismatches,
            )

    if mismatches:
        for mismatch in mismatches:
            print(f"MISMATCH: {mismatch}")
        print(
            f"\nContract parity failed: {len(mismatches)} mismatch"
            f"{'es' if len(mismatches) != 1 else ''}."
        )
        return 1

    print(
        "Contract parity OK: "
        f"{len(contract_operations)} operations and "
        f"{len(shared_schema_names)} shared schemas checked against "
        f"{len(service_documents)} service documents."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
