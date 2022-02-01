#!/bin/bash
#
# Copyright (c) 2021, Azul Systems
# 
# All rights reserved.
# 
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
# 
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
# 
# * Redistributions in binary form must reproduce the above copyright notice,
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# 
# * Neither the name of [project] nor the names of its
#   contributors may be used to endorse or promote products derived from
#   this software without specific prior written permission.
# 
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
# 

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
