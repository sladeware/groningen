#!/bin/bash

if [[ "$(uname)" == 'Darwin' ]]; then
  readonly GSED=$(which gsed)

  if [ ! -x "${GSED}" ]; then
    echo 'GNU Sed could not be found on Mac OS X.' >&2
    echo 'Please install it via "brew install gnu-sed".' >&2
  fi

  readonly SED="${GSED}"
else
  SED=$(which sed)
fi

readonly OUTPUT='src/main/java/org/arbeitspferde/groningen/Build.java'
readonly INPUT="${OUTPUT}.in"

readonly BUILD_USER=$(whoami)
readonly BUILD_TIMESTAMP=$(date -u)
readonly BUILD_BRANCH=$(git symbolic-ref HEAD 2> /dev/null | "${SED}" 's,refs/heads/,,' || echo "<UNKNOWN>")
readonly BUILD_COMMIT=$(git log | head -n1 | awk '{ print $2 }')
readonly BUILD_UNAME=$(uname -s -m -p -r -p)

"${SED}" "s,@@BUILD_USER@@,${BUILD_USER},;s,@@BUILD_TIMESTAMP@@,${BUILD_TIMESTAMP},;s,@@BUILD_BRANCH@@,${BUILD_BRANCH},;s,@@BUILD_COMMIT@@,${BUILD_COMMIT},;s,@@BUILD_UNAME@@,${BUILD_UNAME}," \
  "${INPUT}" \
  > "${OUTPUT}"
