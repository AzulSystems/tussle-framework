#!/bin/bash

BASE_DIR=$(cd $(dirname $0); pwd)

INT0_DEF="start: 0, finish: 36000, name: ''"
INT0=${INT0:-${INT0_DEF}}
MAKE_REPORT=${MAKE_REPORT:-false}
MH=${MH:-3}

while [[ "${1}" == *=* ]]
do
export "${1}"
echo "Exported: '${1}'"
shift
done

RES_DIR=${RES_DIR:-${1:-.}}

INT0="- {${INT0}}"
[[ -n "${INT1}" ]] && INT1="- {${INT1}}"
[[ -n "${INT2}" ]] && INT2="- {${INT2}}"
[[ -n "${INT3}" ]] && INT3="- {${INT3}}"

[[ -n "${SLA1}" ]] && SLA1="- {${SLA1}}"
[[ -n "${SLA2}" ]] && SLA2="- {${SLA2}}"
[[ -n "${SLA3}" ]] && SLA3="- {${SLA3}}"

metricsConf="
resultsDir: . 
makeReport: ${MAKE_REPORT}
mergeHistos: ${MH}
intervals:
${INT0}
${INT1}
${INT2}
${INT3}
slaConfig:
${SLA1}
${SLA2}
${SLA3}
"

run_props=$( find "${RES_DIR}" -type f -name "run.properties.json" )

if [[ -z "${run_props}" ]]
then
    resultsDir=${RES_DIR}
    echo "No any run.properties found. Processing topmost results dir: ${resultsDir} ..."
    (
    cd "${resultsDir}" && \
    java -cp ${BASE_DIR}/benchmarks-common-*.jar org.benchmarks.tools.Analyzer -s "${metricsConf}"
    )
    exit
fi

echo "${run_props}" | while read r
do
    resultsDir=$(dirname "${r}")
    echo "Processing results dir: ${resultsDir} ..."
    (
    cd "${resultsDir}" && \
    java -cp ${BASE_DIR}/benchmarks-common-*.jar org.benchmarks.tools.Analyzer -s "${metricsConf}"
    )
done
