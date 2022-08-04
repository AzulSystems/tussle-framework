#!/bin/bash

SCRIPT_DIR=$(cd $(dirname ${BASH_SOURCE[0]}) && pwd -P)

echo "==========================="
echo "Proc Benchmark test script"
echo "Test args: '${@}'"
echo "TEST_VAR: ${TEST_VAR}"
echo "RES_JSON: ${RES_JSON}"

pref="[test script] "

log() {
    echo "${pref}${@}"
}

init() {
    log "Init..."
    sleep 1
}

cleanup() {
    log "Init..."
    sleep 1
}

run() {
    log "Run: ${@}"
    while [[ "$1" == *=* ]]
    do
        export "$1"
        shift
    done
    TIME=${TIME:-3}
    log "TIME: ${TIME}"
    log "WARMUP: ${WARMUP}"
    log "TARGET: ${TARGET}"
    actualRate=$(( TARGET * 9999 / 10000 ))
    (( actualRate > 19900 )) && actualRate=19900
    if (( WARMUP > 0 ))
    then
        log "Warmup..."
        sleep 1
    fi
    for (( i = 1; i <= TIME; i++ ))
    do
        log "Run${i}..."
        sleep 1
    done
    if [[ "${RES_JSON}" == true ]]
    then
        log "Generating results JSON:"
        cat<<EOF>result.json
{"rate": ${actualRate},
 "rateUnits": "ooop/s",
 "time": ${TIME}000 }
EOF
        cat result.json
    else
        log Done
    fi
}

if [[ "$BASH_SOURCE" == "$0" ]]
then
    "$@"
fi
