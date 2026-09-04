#!/usr/bin/env bash
# =====================================================================================
# Three-tier architecture check.
#
# The assessment rewards "use of a 3-tier architecture", and folder names alone prove
# nothing: a controller that queries a DAO directly is a two-tier application in three
# directories. This script reads the actual import statements and fails if any layer
# reaches somewhere it should not, which turns the claim into something the build enforces.
#
# Permitted direction of dependency:
#
#     PRESENTATION  (web, api)          may use  service, dto, domain, exception
#          |
#          v
#     BUSINESS      (service)           may use  dao, dto, domain, exception, config
#          |
#          v
#     DATA ACCESS   (dao)               may use  dto, domain
#
#     domain, dto, exception            shared model - depend on nothing above them
#     config                            composition root, not a tier: wires everything
#
# Run: ./verify-layers.sh
# =====================================================================================
set -uo pipefail

SRC="server/src/main/java/com/sunrise/dental"
cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ ! -d "$SRC" ]; then
    echo "Cannot find $SRC - run this from the project root."
    exit 2
fi

violations=0

# imports_of <layer> -> the sunrise packages that layer imports
imports_of() {
    find "$SRC/$1" -name '*.java' -exec grep -hoE "^import com\.sunrise\.dental\.[a-z]+" {} + 2>/dev/null \
        | sed 's/.*dental\.//' | sort -u | grep -vx "$1"
}

# forbid <layer> <package> <why>
forbid() {
    local layer="$1" forbidden="$2" why="$3"
    local hits
    hits=$(find "$SRC/$layer" -name '*.java' \
           -exec grep -lE "^import com\.sunrise\.dental\.$forbidden\." {} + 2>/dev/null || true)
    if [ -n "$hits" ]; then
        echo "  VIOLATION  $layer must not import $forbidden"
        echo "             $why"
        echo "$hits" | sed "s|^|             at |"
        violations=$((violations + 1))
    else
        printf "  ok         %s does not import %s\n" "$layer" "$forbidden"
    fi
}

echo ""
echo "Dependency direction"
echo "===================="
for layer in web api service dao domain dto exception config; do
    deps=$(imports_of "$layer" | tr '\n' ' ')
    printf "  %-10s -> %s\n" "$layer" "${deps:-(nothing of ours)}"
done

echo ""
echo "Layering rules"
echo "=============="

# Presentation must go through the business tier, never straight to the database.
forbid web  dao "A controller querying a DAO skips the business tier, so its rules can be bypassed."
forbid api  dao "A controller querying a DAO skips the business tier, so its rules can be bypassed."

# The business tier must not know it is being called over HTTP.
forbid service web "Business rules that reference HTTP cannot be reused or unit tested without a container."
forbid service api "Business rules that reference HTTP cannot be reused or unit tested without a container."

# The data tier must not reach upwards.
forbid dao service "An upward dependency makes the two tiers one cycle that cannot be tested apart."
forbid dao web     "The data tier has no business knowing about HTTP."
forbid dao api     "The data tier has no business knowing about HTTP."

# The shared model must stay free of every layer, so anything may depend on it safely.
forbid domain service "The domain model must not depend on the layers that use it."
forbid domain dao     "The domain model must not depend on the layers that use it."
forbid domain web     "The domain model must not depend on the layers that use it."

echo ""
echo "Framework isolation"
echo "==================="
# The business tier is testable without a container only while it stays free of servlet types.
servlet_in_service=$(find "$SRC/service" -name '*.java' \
    -exec grep -lE "^import (jakarta|javax)\.servlet" {} + 2>/dev/null || true)
if [ -n "$servlet_in_service" ]; then
    echo "  VIOLATION  the business tier imports servlet types:"
    echo "$servlet_in_service" | sed 's|^|             at |'
    violations=$((violations + 1))
else
    echo "  ok         the business tier imports no servlet types"
fi

# The domain model should be plain Java: no Spring, no JDBC, no servlets.
framework_in_domain=$(find "$SRC/domain" -name '*.java' \
    -exec grep -lE "^import (org\.springframework|jakarta|javax|java\.sql)\." {} + 2>/dev/null || true)
if [ -n "$framework_in_domain" ]; then
    echo "  VIOLATION  the domain model imports framework types:"
    echo "$framework_in_domain" | sed 's|^|             at |'
    violations=$((violations + 1))
else
    echo "  ok         the domain model is plain Java (no Spring, JDBC or servlet imports)"
fi

echo ""
if [ "$violations" -eq 0 ]; then
    echo "PASS - the three tiers are intact."
    exit 0
fi
echo "FAIL - $violations layering violation(s)."
exit 1
