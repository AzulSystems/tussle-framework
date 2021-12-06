// (C) Azul Systems 2017-2021, Ruslan Scherbakov

// Constants

const BASE_API_URL_BENCHMARKS = window.location.protocol === 'file:' ? 'http://localhost:10200/benchmarks/' : 'benchmarks/';
const BASE_API_URL_REPORTS = window.location.protocol === 'file:' ? 'http://localhost:10200/reports/' : 'reports/';
const BASE_API_URL_PORTAL_DATA = window.location.protocol === 'file:' ? 'http://localhost:10200/portal/data/' : 'portal/data/';
const BASE_RELEASE_API_URL = 'release';
const BASE_SEARCH_API_URL = 'search';
const DISPLAY_RUNS = 'RUNS';
const DISPLAY_SUMMARY = 'SUMMARY';
const DISPLAY_DETAILS = 'DETAILS';
const DISPLAY_REPORT = 'REPORT';
const DISPLAY_HL_SUMMARY = 'HL_SUMMARY';
const DISPLAY_PORTAL = 'PORTAL';
const DISPLAY_SEARCH = 'SEARCH';

const colors = [
    '#0000CC',
    '#FF00AA',
    '#AA00FF',
    '#00CC00',
    '#AAAA00',
    '#CC91CC',
    '#00AA00',
    '#D42424',
    '#2DB1BB',
    '#9B9B9B',
    '#F07500',
    '#99CC11',
];

const xTicks_default = 100;

const groupByToggles_default = [
    { name: 'host', selected: true },
    { name: 'VM', selected: true },
    { name: 'heap', selected: true },
    { name: 'config', selected: true },
    { name: 'build', selected: true },
    { name: 'app', selected: true },
    { name: 'workload', selected: false },
    { name: 'params', selected: true },
    { name: 'steps', selected: true },
    { name: 'ID', selected: false }
];

const separateByToggles_default = [
    { name: 'host', selected: false },
    { name: 'heap', selected: true },
    { name: 'app', selected: false },
    { name: 'workload', selected: true },
    { name: 'params', selected: false },
    { name: 'steps', selected: true },
    { name: 'none', selected: false }
];

function getWebPath(path) {
    path = encodeURIComponent(path).replace(/%2F/g, '/');
    let uri;
    if (path.startsWith("/nsk-fs1")) {
        uri = 'http:/' + path;
    } else if (path.startsWith("nsk-fs1")) {
        uri = 'http://' + path;
    } else {
        uri = 'http://release' + path;
    }
    console.log('getWebPath ' + uri);
    return uri;
}

function fixPath(path) {
    if (path.startsWith('release/')) {
        path = path.substring('release'.length);
    } else if (path.startsWith('/release/')) {
        path = path.substring('/release'.length);
    } else if (path.startsWith('http://release/')) {
        path = path.substring('http://release'.length);
    } else if (path.startsWith('http:/release/')) {
        path = path.substring('http:/release'.length);
    } else if (path.startsWith('http://nsk-fs1/')) {
        path = path.substring('http:/'.length);
    } else if (path.startsWith('http:/nsk-fs1/')) {
        path = path.substring('http:'.length);
    }
    return path;
}

// Utils

function getBenchmarkName(runProperties) {
    return runProperties.benchmark_name;// + (runProperties.workload_name ? ' ' + runProperties.workload_name : '');
}

function getMetricData(hits, benchmark, operation, heap, host) {
    const metricData = [];
    let units = '';
    let xscale = '';
    let name = '';
    for (const hit of hits) {
        const runProperties = hit._source.runProperties;
        if (benchmark !== getBenchmarkName(runProperties) || heap !== runProperties.heap || host !== runProperties.hosts) {
            continue;
        }
        hit._source.metrics?.forEach(metric => {
            if (!metric.hasCharted && getMetricLabel(metric) === operation) {
                metric.hasCharted = true;
                name = metric.operation || operation;
                units = metric.units;
                if (metric.xscale) {
                    xscale = metric.xscale;
                }
                metricData.push({ runProperties, metric });
            }
        });
    }
    let labels = [];
    if (metricData.length > 0 && metricData[0].metric.hasPercentiles()) {
        labels = metricData[0].metric.getValues('percentile_names');
        for (let i = 1; i < metricData.length; i++) {
            const percentileNames = metricData[i].metric.getValues('percentile_names');
            if (percentileNames && labels.length > percentileNames.length) {
                labels = percentileNames;
            }
        }
    }
    for (let metricDat of metricData) {
        const percentileValues = metricDat.metric.getValues('percentile_values');
        if (percentileValues) {
            let extraElements = percentileValues.length - labels.length;
            if (extraElements > 0) {
                percentileValues.splice(labels.length, extraElements);
            }
        }
        const latencyPercentileValues = metricDat.metric.getValues('latency_percentile_values');
        if (latencyPercentileValues) {
            let extraElements = latencyPercentileValues.length - labels.length;
            if (extraElements > 0) {
                latencyPercentileValues.splice(labels.length, extraElements);
            }
        }
    }
    return {
        name,
        labels,
        metricData,
        units,
        xscale
    };
}

function produceAggregatedComparisonReport($scope, hits, reducedResults, existingSummary) {
    const resultToggles = $scope.resultToggles;
    const showDataElements = $scope.showDataElements;
    const showOptions = $scope.showOptions;
    let appName = '';
    let apps = '';
    let jvm_vs = '';
    let jvmVersions = '';
    const hosts = [];
    const heaps = [];
    const charts = [];
    const benchmarks = [];
    const operations = [];
    const checkUnits = showOptions.includes('check_units');
    const confs = !showOptions.includes('no_confs');
    let confCount = 0;
    for (let hit_i = 0; hit_i < hits.length; hit_i++) {
        const hit = hits[hit_i];
        const runProperties = hit._source.runProperties;
        const vm = runProperties.vm_name;
        const appName_i = runProperties.application_name.capitalize() + (runProperties.application_version ? ' ' + runProperties.application_version : '');
        if (!appName_i.startsWith('Unknown')) {
            confCount++;
        }
        if (hit_i === 0) {
            appName = appName_i.capitalize();
        } else {
            if (appName !== appName_i) {
                appName = 'different apps';
                apps = 's';
            }
        }
        const benchmarkName = getBenchmarkName(runProperties);
        if (benchmarks.indexOf(benchmarkName) < 0) {
            benchmarks.push(benchmarkName);
        }
        if (heaps.indexOf(runProperties.heap) < 0) {
            heaps.push(runProperties.heap);
        }
        if (hosts.indexOf(runProperties.hosts) < 0) {
            hosts.push(runProperties.hosts);
        }
        if (vm) {
            if (hit_i === 0) {
                jvm_vs = vm;
                jvmVersions = vm;
                if (runProperties.build) {
                    jvmVersions += '(' + runProperties.build + ')';
                }
            } else {
                if (jvm_vs.indexOf(vm) < 0) {
                    jvm_vs += ' vs. ' + vm;
                    if (hit_i < hits.length - 1) {
                        jvmVersions += ', ' + vm;
                    } else {
                        jvmVersions += ' and ' + vm;
                    }
                    if (runProperties.build) {
                        jvmVersions += '(' + runProperties.build + ')';
                    }
                }
            }
        }
        if (hit._source.metrics) {
            hit._source.metrics.forEach((metric) => {
                const metricLabelChart = getMetricLabel(metric) + ' (chart)';
                const metricLabelChartCounts = getMetricLabel(metric) + ' counts (chart)';
                const operation = getMetricLabel(metric);
                console.log('operation ' + operation);
                if (operations.indexOf(operation) < 0 && (showData(metricLabelChart, TYPE_CHART, showDataElements) || showData(metricLabelChartCounts, TYPE_CHART, showDataElements))) {
                    console.log('metrics showData operation=' + operation + ', metricLabelChart=' + metricLabelChart);
                    operations.push(operation);
                }
            });
        }
    }
    let common = '';
    if (hosts.length === 1 && hosts[0]) {
        common += 'hosts [' + hosts[0] + '] ';
    }
    if (heaps.length === 1 && heaps[0]) {
        common += 'heap [' + heaps[0] + '] ';
    }
    if (common) {
        common = '<b>Common properties:</b> ' + common;
    }
    if (heaps.length === 0) {
        heaps.push('');
    }
    if (heaps.length === 0) {
        heaps.push('');
    }
    if (operations.length === 0) {
        operations.push('');
    }
    console.log('operations ' + operations.length + ': ' + operations.join());
    console.log('heaps ' + heaps.length + ': ' + heaps.join());
    console.log('hosts ' + hosts.length + ': ' + hosts.join());
    for (const benchmark of benchmarks) for (const operation of operations) for (const heap of heaps) for (const host of hosts) {
        const hd = getMetricData(hits, benchmark, operation, heap, host);
        let prefix = '';
        if (benchmarks.length > 1) {
            prefix += '[' + benchmark + '] ';
        }
        if (hosts.length > 1) {
            prefix += '[' + host + '] ';
        }
        if (heaps.length > 1) {
            prefix += '[' + heap + '] ';
        }
        const metricData = hd.metricData;
        const metricName = operation;
        if (metricData.length > 0) {
            const dataDataValues = {};
            const dataDataLabels = {};
            const datasets = [];
            const chartConfigs = confCount ? [] : null;
            let hlines = [];
            for (let i = 0; i < metricData.length; i++) {
                const vm = metricData[i].runProperties.vm_name;
                let vm_num = vm;
                const configName = 'm' + (i + 1);
                if (confs) {
                    if (configName !== 'unknown') {
                        vm_num += ' [' + configName + ']';
                    }
                    let conf = vm;
                    let confAdded = 0;
                    const inConf = metricData[i].runProperties.config || metricData[i].runProperties.build || metricData[i].runProperties.workload_parameters;
                    if (inConf) {
                        conf += ' [';
                    }
                    if (metricData[i].runProperties.config) {
                        conf += metricData[i].runProperties.config;
                        confAdded++;
                    }
                    if (metricData[i].runProperties.workload_parameters) {
                        if (confAdded > 0) {
                            conf += ', ';
                        }
                        conf += metricData[i].runProperties.workload_parameters;
                        confAdded++;
                    }
                    if (metricData[i].runProperties.build) {
                        if (confAdded > 0) {
                            conf += ', ';
                        }
                        conf += 'build ' + metricData[i].runProperties.build;
                    }
                    if (inConf) {
                        conf += ']';
                    }
                    if (confAdded === 0) {
                        conf = vm;   
                    }
                    if (confCount > 0) {
                        chartConfigs.push({
                            name: configName,
                            config: conf
                        });
                    }
                } else {
                    for (let i0 = 0; i0 < i; i0++) {
                        const vm0 = metricData[i0].runProperties.vm_name;
                        const host0 = metricData[i0].runProperties.host;
                        if (vm0 === vm && host0 !== host) {
                            vm_num = vm + '(' + host + ')';
                        }
                    }
                }
                if (confCount === 0) {
                    vm_num = '';
                }
                const metric = metricData[i].metric;
                hlines = hlines.concat(getHLines(metric, i));
                if (metric.percentile_values) {
                    datasetsPercentiles1.push({ type: 'line', label: vm_num });
                    dataPercentiles1.push(metric.percentile_values);
                }
                if (metric.latency_percentile_values) {
                    datasetsPercentilesLatency.push({ type: 'line', label: vm_num });
                    dataPercentilesLatency.push(metric.latency_percentile_values);
                }
                let diffStop = false;
                metric.metricValues.forEach(mv => {
                    if (!diffStop && mv.name !== 'percentile_names') {
                        if (!dataDataValues[mv.name]) {
                            dataDataValues[mv.name] = [];
                        }
                        if (!mv.isPercentiles) {
                            if (!dataDataLabels[mv.name]) {
                                dataDataLabels[mv.name] = getMetricValuesLabels(metric, mv.values, showOptions);
                            } else if (!checkUnits || equalXvalues(metric, metricData[0].metric)) {
                                if (mv.values.length > dataDataLabels[mv.name].labels.length) {
                                    dataDataLabels[mv.name] = getMetricValuesLabels(metric, mv.values, showOptions);
                                }
                            } else {
                                console.log('Different x-scales!');
                                diffStop = true;
                            }
                        }
                        if (!diffStop) {
                            dataDataValues[mv.name].push(mv);
                            datasets.push({ type: 'line', label: vm_num, vlines: getVLines(metric) });
                        }
                    }
                });
            }
            for (const mvName in dataDataValues) {
                const thisName = mvName ? `${metricName} [${mvName}]` : metricName;
                console.log(`Adding aggregated chart: ${prefix} - ${thisName}`);
                if (dataDataValues[mvName][0].isPercentiles) {
                    charts.push({
                        selected: showData(thisName + ' (chart)', TYPE_CHART, showDataElements),
                        colors,
                        metricName: thisName,
                        configs: chartConfigs,
                        data: dataDataValues[mvName].map(mv => mv.values),
                        labels: hd.labels,
                        datasets,
                        options: getPercentilesChartOptions(prefix + thisName, 'percentiles', hd.units, hd)
                    });
                } else {
                    charts.push({
                        selected: showData(thisName + ' (chart)', TYPE_CHART, showDataElements) || showData(metricName + ' (chart)', TYPE_CHART, showDataElements),
                        colors,
                        metricName: thisName,
                        configs: chartConfigs,
                        data: dataDataValues[mvName].map(mv => mv.values),
                        labels: dataDataLabels[mvName].labels,
                        labelsSet: dataDataLabels[mvName],
                        datasets,
                        options: getChartOptions(prefix + thisName, hd.xscale, hd.units, null, hlines, [], dataDataLabels[mvName].labels.length > 100, showOptions),
                    });
                }
            }
        }
    }
    const summary = existingSummary || [];
    const addNormed = () => {
        if (!hits.hasNormed) {
            console.log('Adding normed metrics...');
            addNormalizedMetrics(hits);
            hits.hasNormed = true;
            produceAggregatedComparisonReport($scope, hits, reducedResults, summary);
        } else {
            console.log('Normed metrics already added...');
        }
    };
    if (existingSummary && existingSummary.find(elem => !!elem.hidingToggles)) {
        const t = existingSummary.find(elem => !!elem.hidingToggles).hidingToggles;
        console.log(`found existing toggles: ${t.joinProps('name')}`);
    }
    const toggles = existingSummary && existingSummary.find(elem => !!elem.hidingToggles) ? existingSummary.find(elem => !!elem.hidingToggles).hidingToggles : [];
    if (!hits.hasNormed) {
        toggles.splice(0, 0, {
            name: 'Add normalized outliers',
            addNormed: true,
            selected: false,
            toggle: addNormed
        });
    } else {
        for (let it = toggles.length - 1; it >= 0; it--) {
            if (toggles[it].addNormed) {
                console.log(`removing toggle at ${it}`);
                toggles.splice(it, 1);
            }
        }
    }
    const unitNames = 'ms';
    if (!existingSummary && confCount > 0) {
        summary.push({
            html:
                '<p>This performance comparison summary shows quantitative differences between <b>$JVMVersions</b> in context of <b>$Application</b>. ' +
                'The analysis makes use of the benchmark <b>$Benchmark$BenchmarkParams</b>.</p>' +
                '<p>$Common</p>'
        });
    }
    // Charts toggles section
    let chartsPos = summary.length;
    if (existingSummary) {
        existingSummary.forEach((elem, idx) => {
            if (elem.charts) {
                chartsPos = idx + 1;
            }
        });
    } else {
        toggles.push({
            name: 'all',
            selected: true,
            toggle: allToggle => toggles.forEach(toggle => {
                if (!toggle.addNormed) {
                    toggle.selected = allToggle.selected;
                }
            })
        });
        summary.push({
            html: '<h4>Charts</h4>'
        }, {
            displayToggles: { name: 'Select Charts', show: false },
            hidingToggles: toggles
        }, {
            html: '<br/>'
        });
        chartsPos = summary.length;
    }
    // Charts content section 
    if (charts.length > 0) {
        charts.forEach(chart => {
            toggles.push({
                name: chart.metricName,
                selected: !!chart.selected
            });
            const visible = () => toggles.selected(chart.metricName)
            if (chart.configs) {
                summary.splice(chartsPos++, 0, {
                    visible,
                    html:
                        '<div><u>Metric:</u> ' + chart.metricName + '</div>' +
                        '<div><u>Configurations:</u><br/>' + chart.configs.map(c => '<b>' + c.name + '</b> - ' + c.config).join('<br/>') + '</div>'
                });
            }
            summary.splice(chartsPos++, 0, { visible, charts: [chart] });
        });
    } else if (!existingSummary) {
        summary.push({ html: '<p>The Metrics Charts data is not available for selected runs.</p>' });
    }
    toggles.forEach(t => t.visible = true);
    // Values section
    if (!existingSummary && confCount > 0 && reducedResults) {
        const aggregatedResults = filterValueResultsTable(reducedResults, row => isValue(row.type) || isHeader(row.type));
        summary.push({
            html: '<h4>Values</h4>'
        }, {
            displayToggles: { name: 'Select Values', show: false },
            hidingToggles: resultToggles.filter(t => isValue(t.type) || isHeader(t.type))
        }, {
            displayTable: { show: true },
            table: aggregatedResults
        });
    }
    // Details section (CHARTS + VALUES)
    if (!existingSummary && confCount > 0) {
        const displayDetails = { name: 'Details', show: false };
        summary.push({
            html: '<h4>Comparison Details</h4>'
        }, {
            html: '<p>The following table shows results from several runs of the <b>$Benchmark$BenchmarkParams</b>, along with values of scores in corresponding units. Click "Details" to open that table.</p>'
        }, {
            displayToggles: displayDetails,
            hidingToggles: resultToggles
        }, {
            displayTable: displayDetails,
            table: reducedResults
        });
    }
    // Process templates
    for (const summar of summary) {
        if (summar.html) {
            summar.html = summar.html
                .replace(/\$JVMs_vs/g, jvm_vs)
                .replace(/\$JVMVersions/g, jvmVersions || 'jvm(name n/a)')
                .replace(/\$Application/g, appName || 'app(name n/a)')
                .replace(/\$APPS/g, apps || 'apps(n/a)')
                .replace(/\$Benchmark/g, benchmarks.join())
                .replace(/\$Units/g, unitNames)
                .replace(/\$Common/g, common);
        }
    }
    return summary;
}

function produceReport($scope, hits, reducedResults) {
    const resultToggles = $scope.resultToggles;
    const displayDetails = { name: 'More Metrics', show: false };
    const displayHeader = { name: 'Configuration', show: true };
    const displaySummary = { name: 'Results', show: true };
    const headerInfo = reduceResultsTable($scope, produceBasicReportTable(hits));
    const summaryResults = filterValueResultsTable(reducedResults, row => isValue(row.type) &&
        (row[0].value.indexOf('conforming rate') >= 0 || row[0].value.indexOf('max rate') >= 0 || row[0].value.indexOf('high bound') >= 0));
    const report =
        [{
            logo: true,
            cls: 'main_logo'
        }, {
            html: '<div class="center-aligner">'
                + '<div class="main_title">ISV Report</div>'
                + '<div class="main_subtitle">Copyright &copy; 2021 Azul Systems</div>'
                + '<br/></div>'
        }, {
            cls: 'sub_table half',
            table_caption: displayHeader.name,
            //hidingToggles: [],
            //displayToggles: displayHeader,
            displayTable: displayHeader,
            //table_width: '100%',
            table: headerInfo,
            vspace: true,
        }, {
            cls: 'sub_table half',
            table_caption: displaySummary.name,
            //hidingToggles: [],
            //displayToggles: displaySummary,
            displayTable: displaySummary,
            //table_width: '100%',
            table: summaryResults,
            vspace: true,
        }, {
            cls: 'sub_table half',
            vspace: true,
        }, {
            cls: 'sub_table half',
            vspace: true,
        }];
    const hwInfo = reduceResultsTable($scope, produceHardwareTable(hits));
    if (hwInfo && Object.keys(hwInfo).length > 0) {
        const displayHw = { name: 'HW Details', show: false };
        report.push({
            cls: 'sub_table',
            hidingToggles: [],
            displayToggles: displayHw,
            displayTable: displayHw,
            table: hwInfo,
            //vspace: true,
        });
    }
    const osInfo = reduceResultsTable($scope, produceOsTable(hits));
    if (osInfo && Object.keys(osInfo).length > 0) {
        const displayOs = { name: 'OS Details', show: false };
        report.push({
            cls: 'sub_table',
            hidingToggles: [],
            displayToggles: displayOs,
            displayTable: displayOs,
            table: osInfo,
            //vspace: true,
        });
    }
    const jvmInfo = reduceResultsTable($scope, produceJvmTable(hits));
    if (jvmInfo && Object.keys(jvmInfo).length > 0) {
        const displayJvm = { name: 'JVM Details', show: false };
        report.push({
            cls: 'sub_table',
            hidingToggles: [],
            displayToggles: displayJvm,
            displayTable: displayJvm,
            table: jvmInfo,
            //vspace: true,
        });
    }
    report.push({
        cls: 'sub_table',
        displayToggles: displayDetails,
        hidingToggles: resultToggles,
        displayTable: { show: true },
        table: reducedResults,
        table_width: '100%',
    });
    return report;
}

function createToggleFilter($scope, filterWhat) {
    const togglesFilter = {
        value: ''
    };
    togglesFilter.onChange = () => {
        console.log(`FILTER onChange: [${togglesFilter.value}]`);
        togglesFilter.toggles[0].name = togglesFilter.value;
        togglesFilter.toggles[0].visible = !!togglesFilter.value;
    };
    let newToggle = () => {/* */};
    const onRemove = t => {
        for (let i = 0; i < togglesFilter.toggles.length; i++) {
            if (t === togglesFilter.toggles[i]) {
                togglesFilter.toggles.splice(i, 1);
            }
        }
        if (togglesFilter.toggles.length === 0) {
            togglesFilter.toggles = [newToggle()];
        }
    }
    const onToggle = t => {
        if (t === togglesFilter.toggles[0]) {
            togglesFilter.toggles.splice(0, 0, newToggle());
        }
        $scope.toggleToggles(t, filterWhat, t.name);
    }
    newToggle = () => {
        return { name: 'new', generic: true, style: 'common_toggle', selected: false, visible: false, toggle: onToggle, remove: onRemove }
    };
    togglesFilter.toggles = [newToggle()];
    return togglesFilter;
}

function produceDetailedComparisonReport($scope, hits, reducedResults) {
    const resultToggles = $scope.resultToggles; 
    const displayDetails = { show: true };
    const toShow = () => resultToggles.filter(t => t.selected && !t.generic && t.name !== 'avg').map(t => convertShowArg(t.name)).join(',');
    const toOpts = () => $scope.showOptions.join(',');
    const toFilter = () => $scope.filterDataElements.join(',');
    const filterValues = resultToggles.find(toggle => toggle.type === TYPE_FILTER_VALUES);
    if (filterValues) {
        filterValues.togglesFilter = createToggleFilter($scope, TYPE_ALL_VALUES);
    }
    const filterCharts = resultToggles.find(toggle => toggle.type === TYPE_FILTER_CHARTS);
    if (filterCharts) {
        filterCharts.togglesFilter = createToggleFilter($scope, TYPE_ALL_CHARTS);
        filterCharts.togglesFilter.pin = true;
    }
    return [{
        html: '<h4>ISV Details Report</h4>'
    }, {
        buttons: [{
            name: 'Open Link',
            onClick: event => $scope.openResults(event, $scope.queryArgs(hits.joinProps('_id', ','), toOpts(), toShow(), toFilter()), '_blank')
        }, {
            name: 'Open Summary',
            onClick: event => $scope.openResults(event, $scope.queryArgs(hits.joinProps('_id', ','), toOpts(), toShow(), toFilter(), 1), '_blank')
        }]
    }, {
        displayToggles: displayDetails,
        hidingToggles: resultToggles,
    }, {
        displayTable: displayDetails,
        table: reducedResults
    }];
}

function produceBasicReportTable(hits) {
    let table = {};
    if (hits.length > 0) {
        for (let hidx = 0; hidx < hits.length; hidx++) {
            let hit = hits[hidx];
            let runProperties = hit._source.runProperties;
            let cls = runProperties.vm_name.toLowerCase();
            pushVal(table, 'Benchmark', TYPE_HEADER, runProperties.benchmark, hidx, cls);
            pushVal(table, 'Tested by', TYPE_HEADER, runProperties.testedBy, hidx, cls);
            pushVal(table, 'Test date', TYPE_HEADER, runProperties.date, hidx, cls);
            pushVal(table, 'Test sponsor', TYPE_HEADER, runProperties.sponsor, hidx, cls);
        }
    }
    return table;
}

function producePropsTable(hits, propsName) {
    let table = {};
    if (hits.length > 0) {
        for (let hidx = 0; hidx < hits.length; hidx++) {
            let hit = hits[hidx];
            let runProperties = hit._source.runProperties;
            let cls = runProperties.vm_name.toLowerCase();
            if (runProperties[propsName] && runProperties[propsName].length === 1) {
                const props = runProperties[propsName][0];
                for (let prop in props) {
                    pushVal(table, prop, TYPE_HEADER, props[prop], hidx, cls);
                }
            }
        }
    }
    return table;
}

function produceHardwareTable(hits) {
    return producePropsTable(hits, 'hardware');
}

function produceOsTable(hits) {
    return producePropsTable(hits, 'os');
}

function produceJvmTable(hits) {
    return producePropsTable(hits, 'jvm');
}

function produceSetupTable(hits) {
    let table = {};
    if (hits.length > 0) {
        for (let hidx = 0; hidx < hits.length; hidx++) {
            let hit = hits[hidx];
            let runProperties = hit._source.runProperties;
            let cls = runProperties.vm_name.toLowerCase();
            pushVal(table, 'ID', TYPE_ID, hit._id, hidx, cls, () => false);
            pushVal(table, 'VM', TYPE_HEADER, runProperties.vm_name, hidx, cls);
            pushVal(table, 'Heap', TYPE_HEADER, runProperties.heap, hidx, cls);
            pushVal(table, 'Build', TYPE_HEADER, runProperties.build, hidx, cls);
            pushVal(table, 'Application', TYPE_HEADER, runProperties.application_name, hidx, cls);
            pushVal(table, 'Application Version', TYPE_HEADER, runProperties.application_version, hidx, cls);
            pushVal(table, 'Benchmark', TYPE_HEADER, runProperties.benchmark_name.hideVersion(), hidx, cls);
            pushVal(table, 'Benchmark Version', TYPE_HEADER, runProperties.benchmark_name.getVersion(), hidx, cls);
            pushVal(table, 'Workload', TYPE_HEADER, runProperties.workload_name, hidx, cls);
            pushVal(table, 'Params', TYPE_HEADER, runProperties.workload_parameters, hidx, cls);
            pushVal(table, 'Config', TYPE_HEADER, runProperties.config, hidx, cls, () => false);
            pushVal(table, 'Host', TYPE_HEADER, runProperties.hosts, hidx, cls);
            pushVal(table, 'Test Run Date', TYPE_DATETIME, runProperties.start_time, hidx, cls);
            pushVal(table, 'Test Run Time (minutes)', TYPE_NUMBER, runProperties.time_spent_minutes, hidx, cls);
            pushVal(table, 'Test Run Conductor', TYPE_HEADER, runProperties.conductor, hidx, cls);
        }
    }
    return table;
}

function getPrefix(runProperties, toggles) {
    let prefix = '';
    if (toggles.selected('host') && runProperties.host) {
        prefix += '[' + runProperties.host + '] ';
    }
    if (toggles.selected('heap') && runProperties.heap) {
        prefix += '[' + runProperties.heap + '] ';
    }
    if (toggles.selected('app') && runProperties.application) {
        prefix += '[' + runProperties.application + '] ';
    }
    if (toggles.selected('workload') && runProperties.workload_name) {
        if (toggles.selected('params') && runProperties.workload_parameters) {
            prefix += '[' + runProperties.workload_name + '//' + runProperties.workload_parameters + '] ';
        } else {
            prefix += '[' + runProperties.workload_name + '] ';
        }
    } else if (toggles.selected('params') && runProperties.workload_parameters) {
        prefix += '[' + runProperties.workload_parameters + '] ';
    }
    return prefix;
}

function hasPercentiles(hits) {
    let n = 0;
    hits.forEach(hit => hit._source.metrics?.forEach(metric => n += metric.hasPercentiles()));
    return n;
}

function hasRates(hits) {
    let n = 0;
    hits.forEach(hit => hit._source.metrics?.forEach(metric => n += metric.name !== 'hiccup_times' && !!metric.operation && !!metric.actualRate));
    return n;
}

function hasTotals(hits) {
    let n = 0;
    hits.forEach(hit => hit._source.metrics?.forEach(metric => n += metric.name !== 'hiccup_times' && !!metric.operation && !!metric.totalValues));
    return n;
}

function hasChartValues(hits) {
    let n = 0;
    hits.forEach(hit => hit._source.metrics?.forEach(metric => n += metric.hasValues() || metric.hasCounts() || metric.hasThroughput()));
    console.log(`hasChartValues: ${n}`);
    return n;
}

function initLazyToggle(lazyElem, t, selected) {
    t.selected = t.visible = lazyElem.selected = selected;
    lazyElem.extra = lazyElem.extra || [];
    lazyElem.extra.push(() => t.selected = t.visible = lazyElem.selected);
    if (!lazyElem.toggle) {
        lazyElem.toggle = () => lazyElem.extra.forEach(f => f());
    }    
}

function pushMetricsChart(metric, runProperties, hidx, table, toggles, prefix, stepNumber, showDataElements) {
    const cls = runProperties.vm_name.toLowerCase();
    if (metric.operation && metric.operation.startsWith('warmup_')) {
        return;
    }
    if (metric.group) {
        return;
    }
    if (metric.hasValues()) {
        const lazyChart = metric.name == 'hiccup_times' || metric.name == 'top' || metric.name == 'cpu' || 
                metric.name == 'network' || metric.name == 'disk' || metric.name.indexOf('-mw') >= 0; 
        const metricLabel = getMetricLabel(metric) + ' (chart)';
        pushVal(table, prefix + metricLabel, TYPE_CHART_TIME, metric, hidx, cls, () => toggles.selected(metricLabel), () => toggles.select(metricLabel, false));
        console.log('ADDED values chart ' + metricLabel);
        const t = toggles.addToggleOpt(metricLabel, TYPE_CHART_TIME, showDataElements);
        if (lazyChart) {
            const mwName = metric.name.indexOf('-mw') >= 0 ? 'MW' : metric.name;
            const mname = metric.name == 'hiccup_times' ? 'hiccup' : mwName;
            const lazyElem = toggles.addToggleOpt(`${mname} charts`, TYPE_CHART_HEADER, showDataElements, 'common_toggle');
            initLazyToggle(lazyElem, t, false);
        }
    }
    if (metric.hasCounts()) {
        const metricLabel = getMetricLabel(metric) + ' counts (chart)';
        toggles.addToggleOpt(metricLabel, TYPE_CHART_COUNTS, showDataElements);
        pushVal(table, prefix + metricLabel, TYPE_CHART_COUNTS, metric, hidx, cls, () => toggles.selected(metricLabel), () => toggles.select(metricLabel, false));
        console.log('ADDED counts chart ' + metricLabel);
    }
    if (metric.hasThroughput()) {
        const metricLabel = getMetricLabel(metric) + ' throughput (chart)';
        toggles.addToggleOpt(metricLabel, TYPE_CHART_THROUGHPUT, showDataElements);
        pushVal(table, prefix + metricLabel, TYPE_CHART_THROUGHPUT, metric, hidx, cls, () => toggles.selected(metricLabel), () => toggles.select(metricLabel, false));
        console.log('ADDED throughput chart ' + metricLabel);
    }
    const showHdr = showData('hdr', TYPE_CHART, showDataElements);
    if (metric.hasPercentiles()) {
        const metricLabel = getMetricLabel(metric) + ' HDR (chart)';
        pushVal(table, prefix + metricLabel, TYPE_CHART_HISTOGRAM, metric, hidx, cls, () => toggles.selected(metricLabel), () => toggles.select(metricLabel, false));
        console.log('ADDED hdr chart ' + metricLabel);
        const t = toggles.addToggleOpt(metricLabel, TYPE_CHART_HISTOGRAM, showDataElements);
        const lazyElem = toggles.addToggleOpt("HDR charts", TYPE_CHART_HEADER, showDataElements, 'common_toggle');
        initLazyToggle(lazyElem, t, showHdr);
    }
    if (metric.getValues('percentile_counts')) {
        const metricLabel = getMetricLabel(metric) + ' HDR counts (chart)';
        pushVal(table, prefix + metricLabel, TYPE_CHART_COUNT_HISTOGRAM, metric, hidx, cls, () => toggles.selected(metricLabel), () => toggles.select(metricLabel, false));
        console.log('ADDED HDR_counts chart ' + metricLabel);
        const t = toggles.addToggleOpt(metricLabel, TYPE_CHART_COUNT_HISTOGRAM, showDataElements);
        const lazyElem = toggles.addToggleOpt("HDR charts", TYPE_CHART_HEADER, showDataElements, 'common_toggle');
        initLazyToggle(lazyElem, t, showHdr);
    }
}

function pushMetricsCharts(metrics, runProperties, hidx, table, toggles, prefix, stepNumber, showDataElements) {
    if (metrics) {
        metrics.forEach(metric => pushMetricsChart(metric, runProperties, hidx, table, toggles, prefix, stepNumber, showDataElements));
    }
}

function pushMetricsValues(metrics, runProperties, hidx, table, toggles, prefix, stepNumber, showDataElements) {
    if (!metrics)
        return;
    const cls = runProperties.vm_name.toLowerCase();
    const tooManyMetrics = metrics.length > 100;
    metrics.forEach(metric => {
        if (metric.operation && metric.operation.startsWith('warmup_') || stepNumber !== metric.step) {
            return;
        }
        const toogleOperation = tooManyMetrics ? null : metric.operation;
        if (toogleOperation) {
            toggles.addToggleOpt(metric.operation, TYPE_NUMBER, "all");
        }
    });
    metrics.forEach(metric => {
        if (metric.operation && metric.operation.startsWith('warmup_') || stepNumber !== metric.step) {
            return;
        }
        if (metric.name) {
            toggles.addToggleOpt(metric.name, TYPE_NUMBER, showDataElements);
        }
    });
    metrics.forEach(metric => {
        if (metric.operation && metric.operation.startsWith('warmup_') || stepNumber !== metric.step) {
            return;
        }
        const metricLabel = getMetricLabel(metric, true);
        const metricLabel2 = getMetricLabel(metric);
        const toogleOperation = tooManyMetrics ? null : metric.operation || null;
        let error_rate = 0;
        if (isPrime(metric.name)) {
            error_rate = metric.totalErrors / metric.totalValues;
            if (metric.operation) {
                pushVal(table, prefix + metricLabel2 + ' error rate', TYPE_NUMBER, error_rate, hidx, cls + (error_rate > 0 ? ' data_error' : ''), () => toggles.selected(metric.name, toogleOperation, 'error rate'));
                pushVal(table, prefix + metricLabel2 + ' total errors', TYPE_NUMBER, metric.totalErrors, hidx, cls, () => toggles.selected(metric.name, toogleOperation, 'total errors'));
                pushVal(table, prefix + metricLabel2 + ' total values', TYPE_NUMBER, metric.totalValues, hidx, cls, () => toggles.selected(metric.name, toogleOperation, 'total values'));
            }
        } else if (!metric.name.startsWith('hiccup_times') && metric.operation && metric.totalValues) {
            pushVal(table, prefix + metricLabel + ' total values', TYPE_NUMBER, metric.totalValues, hidx, cls, () => toggles.selected(metric.name, toogleOperation, 'total values'));
        }
        if (metric.hasPercentiles()) {
            PERCENTILES.forEach(pr => {
                const value1 = getMetricPercentileValue(metric, pr);
                if (value1 !== null) {
                    pushVal(table, prefix + metricLabel + ' p' + pr + ' HDR', TYPE_NUMBER, value1, hidx, cls, () => toggles.selected(metric.name, toogleOperation, 'p' + pr, 'HDR')).error_rate = error_rate;
                }
                const value2 = getMetricLatencyPercentileValue(metric, pr);
                if (value2 !== null) {
                    pushVal(table, prefix + metricLabel + ' latency p' + pr + ' HDR', TYPE_NUMBER, value2, hidx, cls, () => toggles.selected(metric.name, toogleOperation, 'p' + pr, 'HDR')).error_rate = error_rate;
                }
            });
        } else {
            if (typeof metric.avg_value !== 'undefined') {
                pushVal(table, prefix + metricLabel + ' avg', TYPE_NUMBER, metric.avg_value, hidx, cls, () => toggles.selected(metric.name, toogleOperation, 'avg')).error_rate = error_rate;
                if (metric.avg_value) {
                    if (metric.stdev_value) {
                        const v = 100 * metric.stdev_value / metric.avg_value;
                        pushVal(table, prefix + metricLabel + ' var', TYPE_NUMBER, roundPercent(v), hidx, cls, () => toggles.selected(metric.name, toogleOperation, 'var')).error_rate = error_rate;
                    }
                }
            }
            if (typeof metric.min_value !== 'undefined') {
                pushVal(table, prefix + metricLabel + ' min', TYPE_NUMBER, metric.min_value, hidx, cls, () => toggles.selected(metric.name, toogleOperation, 'min')).error_rate = error_rate;
            }
            if (typeof metric.max_value !== 'undefined') {
                pushVal(table, prefix + metricLabel + ' max', TYPE_NUMBER, metric.max_value, hidx, cls, () => toggles.selected(metric.name, toogleOperation, 'max')).error_rate = error_rate;
            }
            if (typeof metric.geom_value !== 'undefined') {
                pushVal(table, prefix + metricLabel + ' geom', TYPE_NUMBER, metric.geom_value, hidx, cls, () => toggles.selected(metric.name, toogleOperation, 'geom')).error_rate = error_rate;
            }
            if (typeof metric.value !== 'undefined') {
                pushVal(table, prefix + metricLabel, TYPE_NUMBER, metric.value, hidx, cls, () => toggles.selected(metric.name, toogleOperation)).error_rate = error_rate;
            }
        }
        if (metric.outliers_names) {
            toggles.addToggleOpt('outliers', TYPE_NUMBER, showDataElements);
            for (let i = 0; i < metric.outliers_names.length; i++) {
                pushVal(table, prefix + metricLabel + ' outlier ' + metric.outliers_names[i], TYPE_NUMBER, metric.outliers_values[i], hidx, cls, () => toggles.selected(metric.name, toogleOperation, 'outliers'));
            }
        }
        metric.metricValues.forEach(mv => {
            if (mv.isValues) {
                console.log(`Adding metric values: metric.name=${metric.name} toogleOperation=${toogleOperation} mv.type=${mv.type} mv.name=${mv.name}`);
                const mlabel = prefix + metricLabel + ' ' + mv.name;
                toggles.addToggleOpt(metric.name, TYPE_NUMBER, showDataElements);
                toggles.addToggleOpt(mv.name, TYPE_NUMBER, showDataElements);
                pushVal(table, mlabel + ' avg', TYPE_NUMBER, mv.avg_value, hidx, cls, () => toggles.selected(metric.name, toogleOperation, 'avg', mv.name || null)).error_rate = error_rate;
                pushVal(table, mlabel + ' min', TYPE_NUMBER, mv.min_value, hidx, cls, () => toggles.selected(metric.name, toogleOperation, 'min', mv.name || null)).error_rate = error_rate;
                pushVal(table, mlabel + ' max', TYPE_NUMBER, mv.max_value, hidx, cls, () => toggles.selected(metric.name, toogleOperation, 'max', mv.name || null)).error_rate = error_rate;
            }
        });
        if (metric.targetRate) {
            pushVal(table, prefix + metricLabel2 + ' target rate', TYPE_NUMBER, metric.targetRate, hidx, cls, () => toggles.selected(metric.name, toogleOperation, 'target rate'));
        }
        if (metric.actualRate) {
            pushVal(table, prefix + metricLabel2 + ' actual rate', TYPE_NUMBER, metric.actualRate, hidx, cls, () => toggles.selected(metric.name, toogleOperation, 'actual rate'));
        }
        if (metric.highBound) {
            pushVal(table, prefix + metricLabel2 + ' high bound', TYPE_NUMBER, metric.highBound, hidx, cls, () => toggles.selected(metric.name, toogleOperation, 'high bound'));
        }
    });
}

function produceResultsTable(hits, $scope) {
    const toggles = $scope.resultToggles;
    const separateByToggles = $scope.separateByToggles;
    const showDataElements = $scope.showDataElements;
    const noCharts = parseShowOpts($scope.showOptions).noCharts;
    console.log('produceResultsTable separateByToggles: ' + separateByToggles.joinSelected('name', ','));
    const table = {};
    if (hits.length === 0) {
        trimTable(table);
        return table;
    }
    let heaps = [];
    let apps = [];
    let workloads = [];
    for (const hit of hits) {
        let runProperties = hit._source.runProperties;
        if (heaps.indexOf(runProperties.heap) < 0)
            heaps.push(runProperties.heap);
        if (apps.indexOf(runProperties.application) < 0)
            apps.push(runProperties.application);
        if (workloads.indexOf(runProperties.workload_name) < 0)
            workloads.push(runProperties.workload_name);
    }
    let hidx = 0;
    for (const hit of hits) {
        let runProperties = hit._source.runProperties;
        let cls = runProperties.vm_name.toLowerCase();
        let races = [0];
        for (let race_i = 0; race_i < races.length; race_i++, hidx++) {
            const prefix = getPrefix(hit._source.runProperties, separateByToggles);
            pushVal(table, 'ID', TYPE_ID, hit._id, hidx, cls, () => toggles.selected('id'));
            pushVal(table, 'VM', TYPE_HEADER, runProperties.vm_name, hidx, cls, () => toggles.selected('VM'));
            pushVal(table, 'Build', TYPE_HEADER, runProperties.build, hidx, cls, () => toggles.selected('build'));
            pushVal(table, 'Heap', TYPE_HEADER, runProperties.heap, hidx, cls, () => toggles.selected('heap'));
            pushVal(table, 'App', TYPE_HEADER, runProperties.application, hidx, cls, () => toggles.selected('app'));
            pushVal(table, 'Benchmark', TYPE_HEADER, runProperties.benchmark_name, hidx, cls, () => toggles.selected('benchmark'));
            pushVal(table, 'Workload', TYPE_HEADER, runProperties.workload_name, hidx, cls, () => toggles.selected('workload'));
            pushVal(table, 'Config', TYPE_HEADER, runProperties.config, hidx, cls, () => toggles.selected('config')).cls += ' data_wrap';
            pushVal(table, 'Params', TYPE_HEADER, runProperties.workload_parameters, hidx, cls, () => toggles.selected('params')).cls += ' data_wrap';
            pushVal(table, 'Host', TYPE_HEADER, runProperties.hosts, hidx, cls, () => toggles.selected('host'));
            pushVal(table, 'Results', TYPE_RESULTS_DIR, runProperties.results_dir, hidx, cls, () => toggles.selected('results'));
            pushVal(table, 'Test run date', TYPE_DATETIME, runProperties.start_time, hidx, cls, () => toggles.selected('run time'));
            pushVal(table, 'Test run time (minutes)', TYPE_NUMBER, runProperties.time_spent_minutes, hidx, cls, () => toggles.selected('run time'));
            if (race_i === 0 && !noCharts) {
                pushMetricsCharts(hit._source.metrics, hit._source.runProperties, hidx, table, toggles, prefix, race_i, showDataElements);
            }
        }
    }
    hidx = 0;
    let stepsSeparated = separateByToggles.selected('steps') ? 1 : 0;
    let stepsNotSeparated = 1 - stepsSeparated;
    for (let hit_i = 0; hit_i < hits.length; hit_i++, hidx += stepsSeparated) {
        let hit = hits[hit_i];
        let races = hit._source.races ? hit._source.races : [0];
        for (let race_i = 0; race_i < races.length; race_i++) {
            let prefix = getPrefix(hit._source.runProperties, separateByToggles);
            pushMetricsValues(hit._source.metrics, hit._source.runProperties, hidx, table, toggles, prefix, race_i, showDataElements);
            hidx += stepsNotSeparated;
        }
    }
    trimTable(table);
    return table;
}

function deleteEmptyRows(table) {
    for (let key of Object.keys(table)) {
        let row = table[key];
        if (row.type === TYPE_HEADER)
            continue;
        let empty = true;
        for (let i = 1; i < row.length; i++) {
            if (row[i] && row[i].value) {
                empty = false;
                break;
            }
        }
        if (empty) {
            delete table[key];
        }
    }
}

function getTotalCols(table) {
    let totalCols = 0;
    for (let key of Object.keys(table)) {
        if (totalCols < table[key].length) {
            totalCols = table[key].length;
        }
    }
    return totalCols;
}

function trimTable(table) {
    deleteEmptyRows(table);
    const totalCols = getTotalCols(table);
    for (let key of Object.keys(table)) {
        let row = table[key];
        for (let i = 1; i < totalCols; i++) {
            if (!row[i]) {
                if (table['VM'] && table['VM'][i] && table['VM'][i].value) {
                    let vm = table['VM'][i].value.toLowerCase();
                    pushRowVal(row, null, i, vm);
                } else {
                    pushRowVal(row, null, i, 'undef-vm');
                }
            }
        }
    }
}

function dumpTable(msg, table) {
    console.log('-------------------------------------------------------------------');
    console.log(msg);
    const totalCols = getTotalCols(table);
    for (let key of Object.keys(table)) {
        let row = table[key];
        let line = '';
        if (row.type) {
            line += '{type=' + row.type + '}';
        }
        line += ' | ';
        for (let i = 0; i < totalCols; i++) {
            if (row[i]) {
                if (row[i].cls) {
                    line += '{cls=' + row[i].cls + '}';
                }
                if (row[i].values)
                    line += '[' + row[i].values.joinProps('value', '+') + ']';
                else if (row[i].value)
                    line += row[i].value;
                else if (row[i])
                    line += 'NO_VALUE';
            } else {
                line += 'NO CELL';
            }
            line += ' | ';
        }
        console.log(line);
    }
}

function prepareTable(table, filter) {
    let tableOut = {};
    let rowOutLastHeader = null;
    for (let key of Object.keys(table)) {
        if (filter && !filter(table[key])) {
            continue;
        }
        let rowOut = tableOut[key] = [table[key][0]];
        rowOut.visible = table[key].visible;
        rowOut.hide = table[key].hide;
        rowOut.type = table[key].type;
        rowOut.position = table[key].position;
        if (rowOut.type === TYPE_ID) {
            tableOut[key].cls += ' border_bottom border_top';
        }
        if (rowOut.type === TYPE_HEADER) {
            if (rowOutLastHeader === null || rowOutLastHeader.position <= rowOut.position) {
                rowOutLastHeader = rowOut;
            }
        }
    }
    if (rowOutLastHeader) {
        rowOutLastHeader.cls += ' border_bottom';
    }
    return tableOut;
}

function filterValueResultsTable(table, filter) {
    const tableOut = prepareTable(table, filter);
    const totalCols = getTotalCols(table);
    let oi = 1;
    for (let i = 1; i < totalCols; i++) {
        for (let key of Object.keys(table)) {
            let rowOut = tableOut[key];
            if (rowOut) {
                rowOut.visible = null;
                rowOut[oi] = { ...table[key][i] };
                ///console.log(`rowOut ot=${oi} key=${key} type=${tableOut[key].type} length=${tableOut[key].length}`);
            }
        }
        oi++;
    }
    return tableOut;
}

function reduceResultsTable($scope, table) {
    const groupByToggles = $scope.groupByToggles;
    const benchmark = $scope.selectedBenchmark;
    const excludeData = $scope.excludeData;
    const xTicks = $scope.xTicks;
    console.log('groupByToggles: ' + groupByToggles.joinSelected('name', ','));
    const tableOut = prepareTable(table);
    const totalCols = getTotalCols(table);
    let processed = [];
    let oi = 1;
    for (let i = 1; i < totalCols; i++) {
        if (processed[i]) {
            continue;
        }
        // prepare current cell
        for (let key of Object.keys(table)) {
            let rowOut = tableOut[key];
            rowOut[oi] = copyValue(table[key][i]);
            let cell = rowOut[oi];
            if (cell === null) {
                cell = rowOut[oi] = { value: null, orig_value: null };
            }
            if (rowOut.type === TYPE_RESULTS_DIR && cell.value) {
                cell.values = [{
                    value: 'res_dir1' + '\u2197',
                    br: true,
                    cls: 'link',
                    tooltip: cell.value,
                    onClick: () => window.open(getWebPath(cell.value))
                }];
            } else if (rowOut.type === TYPE_ID) {
                let id = cell.value;
                cell.values = [{
                    value: id,
                    cls: 'link',
                    onClick: () => window.open(BASE_API_URL_BENCHMARKS + benchmark + '/' + id)
                }];
                cell.values.push({
                    // value: '\u2717 ',
                    value: '\u274E ',
                    br: true,
                    cls: 'link',
                    tooltip: 'Exclude ' + id + ' from table',
                    onClick: () => excludeData(id)
                });
            } else if (rowOut.type === TYPE_DATETIME) {
                cell.values = [];
                if (cell.value) {
                    cell.values.push({
                        value: cell.value.formatDateTimeUTC(),
                        br: true
                    });
                }
            } else if (rowOut.type === TYPE_NUMBER) {
                cell.valueMin = cell.orig_value;
                cell.valueMax = cell.orig_value;
                if (cell.orig_value !== null) {
                    cell.orig_values = [cell.orig_value];
                } else {
                    cell.orig_values = [];
                }
                if (cell.value === null) {
                    cell.value = '-';
                }
            } else if (rowOut.type === TYPE_CHART_TIME) {
                cell.charts = [];
                if (cell.value) {
                    let metricsChart = getMetricValuesChart(cell.value, $scope.showOptions);
                    if (metricsChart) {
                        cell.charts.push(metricsChart);
                    }
                }
            } else if (rowOut.type === TYPE_CHART_HISTOGRAM) {
                cell.charts = [];
                if (cell.value) {
                    let metricsChart = getMetricPercentilesChart(cell.value, $scope.showOptions)
                    if (metricsChart) {
                        cell.charts.push(metricsChart);
                    }
                }
            } else if (rowOut.type === TYPE_CHART_COUNTS) {
                cell.charts = [];
                if (cell.value) {
                    let metricsChart = getMetricCountChart(cell.value, $scope.showOptions)
                    if (metricsChart) {
                        cell.charts.push(metricsChart);
                    }
                }
            } else if (rowOut.type === TYPE_CHART_THROUGHPUT) {
                cell.charts = [];
                if (cell.value) {
                    let metricsChart = getMetricThroughputChart(cell.value, $scope.showOptions)
                    if (metricsChart) {
                        cell.charts.push(metricsChart);
                    }
                }
            } else if (rowOut.type === TYPE_CHART_COUNT_HISTOGRAM) {
                cell.charts = [];
                if (cell.value) {
                    let metricsChart = getMetricPercentilesCountChart(cell.value, $scope.showOptions)
                    if (metricsChart) {
                        cell.charts.push(metricsChart);
                    }
                }
            }
        }
        // accumulate peer cells
        for (let j = i + 1; j < totalCols; j++) {
            let equal = 0;
            let total = 0;
            for (let groupByItem of groupByToggles) {
                if (groupByItem.selected) {
                    total++;
                    if (table[groupByItem.name]) {
                        equal += isEqual(table[groupByItem.name][i].value, table[groupByItem.name][j].value);
                    } else {
                        let capitalised = groupByItem.name.capitalize();
                        if (table[capitalised]) {
                            equal += isEqual(table[capitalised][i].value, table[capitalised][j].value);
                        } else {
                            console.log('Missing row: ' + groupByItem.name + ' and ' + capitalised);
                            equal++;
                        }
                    }
                }
            }
            if (equal === total) {
                for (let key of Object.keys(table)) {
                    let rowOut = tableOut[key];
                    let cell = rowOut[oi];
                    let peer = table[key][j];
                    if (rowOut.type === TYPE_RESULTS_DIR) {
                        cell.values.push({
                            value: ' res_dir' + (cell.values.length + 1) + '\u2197',
                            br: true,
                            cls: 'link',
                            tooltip: peer.value,
                            onClick: () => window.open(getWebPath(peer.value))
                        });
                    } else if (rowOut.type === TYPE_ID) {
                        cell.values.push({
                            value: peer.value,
                            cls: 'link',
                            onClick: () => window.open(BASE_API_URL_BENCHMARKS + benchmark + '/' + peer.value)
                        });
                        cell.values.push({
                            // value: '\u2717 ',
                            value: '\u274E ',
                            br: true,
                            cls: 'link',
                            tooltip: 'Exclude ' + peer.value + ' from table',
                            onClick: () => excludeData(peer.value)
                        });
                    } else if (rowOut.type === TYPE_DATETIME) {
                        if (peer.value) {
                            cell.values.push({
                                value: peer.value.formatDateTimeUTC(),
                                br: true
                            });
                        }
                    } else if (rowOut.type === TYPE_HEADER || rowOut.type === TYPE_PARAMS) {
                        let val1 = cell.value;
                        let val2 = peer.value;
                        if (!val1)
                            cell.value = val2;
                        else if (val2 && val1 !== val2 && val1.indexOf(' ' + val2 + ' ') < 0 && !val1.endsWith(' ' + val2)) {
                            cell.value += ' ' + val2;
                        }
                    } else if (rowOut.type === TYPE_NUMBER) {
                        let val2 = peer !== null ? peer.value : null;
                        if (val2 === null)
                            val2 = '-';
                        cell.value += ' ' + val2;
                        if (peer !== null) {
                            cell.valueMin = minNumber(cell.valueMin, peer.orig_value);
                            cell.valueMax = maxNumber(cell.valueMax, peer.orig_value);
                            if (peer.orig_value !== null) {
                                cell.orig_values.push(peer.orig_value);
                            }
                            cell.cls = (cell.cls + ' ' + peer.cls).removeDups();
                            if (peer.error_rate && peer.error_rate > 0) {
                                if (!cell.error_rate)
                                    cell.error_rate = 0;
                                cell.error_rate += peer.error_rate;
                            }
                        }
                    } else if (rowOut.type === TYPE_CHART_TIME && peer.value) {
                        let metricsChart = getMetricValuesChart(peer.value, $scope.showOptions);
                        if (metricsChart) {
                            cell.charts.push(metricsChart);
                        }
                    } else if (rowOut.type === TYPE_CHART_HISTOGRAM && peer.value) {
                        let metricsChart = getMetricPercentilesChart(peer.value, $scope.showOptions)
                        if (metricsChart) {
                            cell.charts.push(metricsChart);
                        }
                    } else if (rowOut.type === TYPE_CHART_COUNTS && peer.value) {
                        let metricsChart = getMetricCountChart(peer.value, $scope.showOptions)
                        if (metricsChart) {
                            cell.charts.push(metricsChart);
                        }
                    } else if (rowOut.type === TYPE_CHART_THROUGHPUT && peer.value) {
                        let metricsChart = getMetricThroughputChart(peer.value, $scope.showOptions)
                        if (metricsChart) {
                            cell.charts.push(metricsChart);
                        }
                    } else if (rowOut.type === TYPE_CHART_COUNT_HISTOGRAM && peer.value) {
                        let metricsChart = getMetricPercentilesCountChart(peer.value, $scope.showOptions)
                        if (metricsChart) {
                            cell.charts.push(metricsChart);
                        }
                    }
                }
                processed[j] = true;
            }
        }
        oi++;
    }
    return tableOut;
}

function produceComparisonTable(table, toggles, compare_prev, compare_with) {
    const baseRow = table['VM'];
    const totalCols = getTotalCols(table);
    const comparedColumns = [];
    if (baseRow) {
        baseRow.forEach(c => delete c.comparisonBase);
        baseRow.forEach(c => delete c.compareWith);
    }
    for (let key of Object.keys(table)) {
        let row = table[key];
        for (let i = row.length - 1; i >= 0; i--) {
            if (row[i] && row[i].diff) {
                row.splice(i, 1);
            }
        }
    }
    for (let key of Object.keys(table)) {
        let row = table[key];
        if (row.type === TYPE_NUMBER) {
            const lowerBetter = isLowerBetterValue(row[0].value);
            for (let i = 1; i < totalCols; i++) {
                let cell = row[i];
                if (cell && cell.orig_values) {
                    cell.values = [];
                    delete cell.diffValue;
                    delete cell.diffXValue;
                    cell.valueAvg = avgNumber(cell.orig_values);
                    cell.valueSum = sumNumber(cell.orig_values);
                    cell.valueStdev = stdevNumber(cell.orig_values);
                    let bestValue = roundNumber(lowerBetter ? cell.valueMin : cell.valueMax);
                    let worstValue = roundNumber(lowerBetter ? cell.valueMax : cell.valueMin);
                    let tooltip = 'Score(s): ' + cell.value + '\n' +
                        'Average value: ' + roundNumber(cell.valueAvg) + '\n' +
                        'Best value: ' + bestValue + '\n' +
                        'Worst value: ' + worstValue + '\n';
                    let stdevValue = null;
                    if (cell.valueStdev && cell.valueAvg) {
                        stdevValue = roundPercent(100 * cell.valueStdev / cell.valueAvg) + '%';
                        tooltip += 'Variability (standard deviation / average):  ' + stdevValue + '\n';
                    }
                    if (cell.valueSum) {
                        tooltip += 'Sum:  ' + cell.valueSum + '\n';
                    }
                    cell.values.push({
                        value: cell.value,
                        cls: 'nowrap',// cell.cls,
                        visible: () => toggles.selected('values')
                    });
                    if (cell.orig_values.length > 0) {
                        if (compare_prev === -1) {
                            cell.values.push({
                                value: roundNumber(cell.valueAvg),
                                cls: 'nowrap avg_value',
                                space: true,
                                tooltip: 'Average score: ' + roundNumber(cell.valueAvg)
                            });
                        } else {
                            cell.values.push({
                                value: roundNumber(cell.valueAvg),
                                cls: 'nowrap avg_value',
                                space: ' ',
                                tooltip: 'Average score: ' + roundNumber(cell.valueAvg),
                                //visible: () => i === 1 && !toggles.selected('values') && !toggles.selected('best') && !toggles.selected('worst') || toggles.selected('avg') 
                                visible: () => true
                            });
                            cell.values.push({
                                value: bestValue,
                                cls: 'nowrap best_value',
                                tooltip: 'Best score: ' + bestValue,
                                visible: () => toggles.selected('best')
                            });
                            cell.values.push({
                                value: worstValue,
                                cls: 'nowrap worst_value',
                                tooltip: 'Worst score: ' + worstValue,
                                visible: () => toggles.selected('worst')
                            });
                        }
                    }
                    if (stdevValue !== null) {
                        cell.values.push({
                            value: '[' + stdevValue + ']',
                            tooltip: 'Variability (standard deviation / average): ' + stdevValue,
                            visible: () => toggles.selected('var')
                        });
                    }
                    if (cell.valueSum !== null) {
                        cell.values.push({
                            value: '[' + cell.valueSum + ']',
                            tooltip: 'Sum: ' + cell.valueSum,
                            visible: () => toggles.selected('sum')
                        });
                    }
                    let baseIdx = -1;
                    if (compare_with && compare_with > 0) {
                        if (compare_with !== i) {
                            baseIdx = compare_with;
                        }
                    } else if (compare_prev === 1) {
                        if (i > 1) {
                            baseIdx = i - 1;
                        }
                    } else if (compare_prev === 2) {
                        if (i > 1 && (i % 2) === 0) {
                            baseIdx = i - 1;
                        }
                    } else if (compare_prev === 0) {
                        if (i > 1) {
                            baseIdx = 1;
                        }
                    }
                    let peer = baseIdx > 0 && baseIdx < row.length ? row[baseIdx] : null;
                    let peerCell = baseIdx > 0 && baseRow ? baseRow[baseIdx] : null;
                    if (peer) {
                        comparedColumns[i] = baseIdx;
                        if (peerCell) {
                            peerCell.comparisonBase = true;
                            if (!baseRow[i].compareWith) {
                                baseRow[i].compareWith = () => produceComparisonTable(table, toggles, null, i);
                            }
                        }
                        let diffValue = cell.valueAvg && peer.valueAvg ? (cell.valueAvg - peer.valueAvg) / peer.valueAvg : null;
                        let diffXValue = null;
                        if (cell.valueAvg && peer.valueAvg) {
                            diffXValue = lowerBetter ? peer.valueAvg / cell.valueAvg : cell.valueAvg / peer.valueAvg;
                        }
                        if (diffValue !== null) {
                            if (lowerBetter) {
                                diffValue = -diffValue;
                            }
                            cell.diffValue = diffValue;
                            cell.diffXValue = diffXValue;
                            let diffStr = 'Difference with ' + (compare_prev > 0 ? 'previous' : 'first') + ' column: ' + getDiffValue(diffValue) + ' (' + roundNumber(diffXValue) + ' times)';
                            tooltip += diffStr + '\n';
                            cell.values.push({
                                value: getDiffValue(diffValue),
                                cls: getDiffClass(diffValue),
                                tooltip: diffStr,
                                visible: () => toggles.selected('diff %', '!diff col')
                            });
                            cell.values.push({
                                value: roundNumber(diffValue),
                                cls: getDiffClass(diffValue),
                                tooltip: '(Raw number) ' + diffStr,
                                visible: () => toggles.selected('diff raw', '!diff col')
                            });
                            cell.values.push({
                                // value: 'x' + roundNumber(diffXValue),
                                value: '' + getPercentValue(diffXValue),
                                cls: getDiffClass(diffValue),
                                tooltip: '(ratio) ' + diffStr,
                                visible: () => toggles.selected('diff x', '!diff col')
                            });
                        }
                    }
                    if (cell.error_rate || cell.error_rate === 0) {
                        tooltip += 'Error rate: ' + roundPercent(100 * cell.error_rate) + '%\n'
                    }
                    cell.tooltip = tooltip;
                    if (cell.error_rate > 0) {
                        cell.cls += ' error_rate';
                        cell.values.forEach(e => e.cls += ' error_rate');
                    }
                }
            }
        } else if (row.type === TYPE_CHART_TIME || row.type === TYPE_CHART_HISTOGRAM || row.type === TYPE_CHART_COUNTS
            || row.type === TYPE_CHART_THROUGHPUT || row.type === TYPE_CHART_COUNT_HISTOGRAM) {
            for (let i = 1; i < totalCols; i++) {
                let cell = row[i];
                if (cell) {
                    cell.value = null;
                    cell.values = null;
                }
            }
        }
    }
    for (let key of Object.keys(table)) {
        let row = table[key];
        let added = 0;
        for (let j = 0; j < comparedColumns.length; j++) {
            let baseIdx = comparedColumns[j];
            if (!baseIdx) {
                continue;
            }
            let idx = j;
            const cellDiff = {
                diff: true,
                cls: 'nowrap',
                visible: () => toggles.selected('diff col')
            };
            const cell = row[idx + added];
            if (cell) {
                if (row.type === TYPE_ID || row.type === TYPE_HEADER) {
                    cellDiff.value = 'diff';
                } else if (row.type === TYPE_NUMBER) {
                    if (typeof cell.diffValue !== 'undefined') {
                        let err_cls = cell.error_rate > 0 ? ' error_rate' : '';
                        cellDiff.values = [];
                        cellDiff.values.push({
                            value: getDiffValue(cell.diffValue),
                            cls: getDiffClass(cell.diffValue) + err_cls,
                            visible: () => toggles.selected('diff %')
                        });
                        cellDiff.values.push({
                            value: roundNumber(cell.diffValue),
                            cls: getDiffClass(cell.diffValue) + err_cls,
                            visible: () => toggles.selected('diff raw')
                        });
                        cellDiff.values.push({
                            value: '' + getPercentValue(cell.diffXValue),
                            cls: getDiffClass(cell.diffValue) + err_cls,
                            visible: () => toggles.selected('diff x')
                        });
                    }
                }
            }
            row.splice(idx + added + 1, 0, cellDiff);
            added++;
        }
    }
    return table;
}

// /////// /// //
// ANGULAR APP //
// /////// /// //

const module = angular.module('ISVViewerApp', ['chart.js']);

initModule(module);

module.controller('ISVViewerCtrl', ($scope, $http, $location, $window) => {
    $scope.isLoading = 0;
    $scope.workloads = [];
    $scope.times = [{
        name: 'Hour',
        selected: true,
        start: 0,
        finish: 3600 * 1000
    }, {
        name: 'Day',
        selected: true,
        start: 3600 * 1000,
        finish: 24 * 3600 * 1000
    }, {
        name: 'Two Days',
        selected: false,
        start: 24 * 3600 * 1000,
        finish: 2 * 24 * 3600 * 1000
    }, {
        name: 'Three Days',
        selected: false,
        start: 2 * 24 * 3600 * 1000,
        finish: 3 * 24 * 3600 * 1000
    }, {
        name: 'Week',
        selected: false,
        start: 3 * 24 * 3600 * 1000,
        finish: 7 * 24 * 3600 * 1000
    }, {
        name: 'Two Weeks',
        selected: false,
        start: 7 * 24 * 3600 * 1000,
        finish: 14 * 24 * 3600 * 1000
    }, {
        name: 'Month',
        selected: false,
        start: 14 * 24 * 3600 * 1000,
        finish: 30 * 24 * 3600 * 1000
    }, {
        name: 'Old',
        selected: false,
        start: 30 * 24 * 3600 * 1000,
        finish: -1
    }];
    $scope.vmsPrev = [];
    $scope.appsPrev = [];
    $scope.heapsPrev = [];
    $scope.hostsPrev = [];
    $scope.buildsPrev = [];
    $scope.configsPrev = [];
    $scope.paramsTwoLevelPrev = [];
    $scope.vms = [];
    $scope.apps = [];
    $scope.heaps = [];
    $scope.hosts = [];
    $scope.builds = [];
    $scope.configs = [];
    $scope.paramsTwoLevel = [];
    $scope.runs = [];
    $scope.showDataElements = [];
    $scope.filterDataElements = [];
    $scope.showOptions = [];
    $scope.benchmarks = [];
    let benchmark = getCookie('benchmarks');
    if (benchmark) {
        $scope.benchmarks = [{ name: benchmark }];
        $scope.selectedBenchmark = benchmark;
    } else {
        $scope.selectedBenchmark = '';
    }
    console.log('selectedBenchmark2 ' + $scope.selectedBenchmark);
    $scope.compare_prev = 0;
    $scope.vms.allSelected = false;
    $scope.apps.allSelected = false;
    $scope.times.allSelected = false;
    $scope.hosts.allSelected = false;
    $scope.builds.allSelected = false;
    $scope.configs.allSelected = false;
    $scope.paramsTwoLevel.allSelected = false;
    $scope.anyConfigsSelected = true;
    $scope.showAllMetrics = true;
    $scope.showAllCharts = false;
    $scope.showAllValues = false;
    $scope.showDefaultValues = false;
    $scope.showCustomTest = ".*";
    $scope.filterText = ".*";
    $scope.optsText = '';
    $scope.noSortMetrics = false;
    $scope.useIDs = false;
    $scope.splitPaths = false;
    $scope.wrapPaths = true;
    $scope.xTicks = xTicks_default;
    $scope.resultsHeaderProperties = ['_id', 'host', 'vm_name', 'heap', 'config', 'build', 'application', 'workload_name', 'workload_parameters', 'results_dir'];
    $scope.searchHeaderProperties = ['results_dir', 'id', 'host', 'benchmark', 'config'];
    $scope.searchHeaderPropertiesSplit = ['results_dir', 'id', 'host', 'benchmark', 'config'];
    $scope.showOptions = [];
    $scope.wide_charts = false;
    $scope.sortDirLabel = [];
    $scope.sortedCol = null;
    $scope.sortingDir = 1;
    let headerNames = new Map();
    headerNames.set('_id', 'id');
    headerNames.set('results_dir', 'res');
    headerNames.set('workload_name', 'workload');
    headerNames.set('workload_parameters', 'params');
    $scope.headerName = (header) => {
        if (header.startsWith('results_dir')) {
            return header.replace(/results_dir/, 'Res');
        }
        let name = headerNames.get(header);
        return (name ? name : header).capitalize();
    }
    $scope.groupByToggles = [...groupByToggles_default];
    $scope.separateByToggles = [...separateByToggles_default];
    $scope.num_comparison_columns = 0;
    $scope.display = DISPLAY_RUNS;
    $scope.displayProducedResults = false;
    $scope.moveItem = (arr, index, n) => {
        if (index + n >= 0 && index + n < arr.length) {
            arr.splice(index + n, 0, arr.splice(index, 1)[0]);
        }
    }
    $scope.operationFailed = response => {
        console.log('operationFailed!');
        $scope.isLoading--;
        if (response.status === -1) {
            $scope.lastOperationStatus = 'Connection error!';
        } else if (response.error) {
            $scope.lastOperationStatus = response.status + ' ' + response.error;
        } else if (response.data && response.data.message) {
            $scope.lastOperationStatus = 'Error ' + response.status + ': ' + response.data.message;
        } else {
            $scope.lastOperationStatus = response.path + ': ' + response.error + ' ' + response.status + ': ' + response.message;
        }
        console.log('operationFailed: ' + $scope.lastOperationStatus + ' - balance: ' + $scope.isLoading);
    };
    $scope.operationPassed = function(_response) {
        $scope.isLoading--;
        console.log('operationPassed - balance: ' + $scope.isLoading);
        $scope.lastOperationStatus = '';
        return new Promise(resolve => resolve());
    }
    function handleWorkloadListResponse(r) {
        console.log('handleWorkloadListResponse...');
        let workloadObjs = [];
        for (let w of r.data.aggregations.workload_count.buckets) {
            workloadObjs.push({ name: w.key, selected: $scope.workloads.selected(w.key), count: w.doc_count });
        }
        $scope.workloads = workloadObjs;
    }
    function handleHostListResponse(r) {
        console.log('handleHostListResponse...');
        let hostObjs = [];
        for (let h of r.data.aggregations.host_count.buckets) {
            hostObjs.push({ name: h.key, selected: $scope.hosts.selected(h.key), count: h.doc_count });
        }
        $scope.hosts = hostObjs;
    }
    function handleRunListResponse(resp) {
        console.log('handleRunListResponse...');
        let hits = resp.data.hits.hits;
        normalizeHitsProperties(hits);
        let runs = [];
        let vms = [];
        let apps = [];
        let heaps = [];
        let hosts = [];
        let builds = [];
        let configs = [];
        let params = [];
        let paramsTwoLevel = [];
        for (let hit of hits) {
            let runProperties = hit._source.runProperties;
            let config = runProperties.config;
            if (!vms.includes(runProperties.vm_name))
                vms.push(runProperties.vm_name);
            if (!heaps.includes(runProperties.heap))
                heaps.push(runProperties.heap);
            if (!hosts.includes(runProperties.hosts))
                hosts.push(runProperties.hosts);
            if (!builds.includes(runProperties.build))
                builds.push(runProperties.build);
            if (!apps.includes(runProperties.application))
                apps.push(runProperties.application);
            if (!params.includes(runProperties.workload_parameters))
                params.push(runProperties.workload_parameters);
            for (let cfg of config.split(' ')) {
                if (!configs.includes(cfg))
                    configs.push(cfg);
            }
            let run = Object.assign({ _id: hit._id, report: hit._source.report ? true : false }, runProperties);
            runs.push(run);
        }
        heaps.sortNum();
        vms.sort();
        apps.sort();
        hosts.sortNum();
        builds.sortNum();
        configs.sortNum();
        params.sortNum();
        runs.sort((a, b) => {
            let res = a.host.localeCompare(b.host);
            if (res === 0)
                res = a.vm_name.localeCompare(b.vm_name);
            if (res === 0)
                res = a.build.localeCompare(b.build);
            if (res === 0)
                res = a.heap.localeCompare(b.heap);
            if (res === 0)
                res = a.config.localeCompare(b.config);
            return res;
        });
        let vmsPrev = $scope.vms.length > 0 ? $scope.vms : $scope.vmsPrev;
        let appsPrev = $scope.apps.length > 0 ? $scope.apps : $scope.appsPrev;
        let heapsPrev = $scope.heaps.length > 0 ? $scope.heaps : $scope.heapsPrev;
        let buildsPrev = $scope.builds.length > 0 ? $scope.builds : $scope.buildsPrev;
        let configsPrev = $scope.configs.length > 0 ? $scope.configs : $scope.configsPrev;
        let vmObjs = [];
        for (let vm of vms) {
            vmObjs.push({ name: vm, selected: vmsPrev.selected(vm) });
        }
        let appObjs = [];
        for (let app of apps) {
            appObjs.push({ name: app, selected: appsPrev.selected(app) });
        }
        let heapObjs = [];
        for (let heap of heaps) {
            heapObjs.push({ name: heap, selected: heapsPrev.selected(heap) });
        }
        let buildObjs = [];
        for (let b of builds) {
            buildObjs.push({ name: b, selected: buildsPrev.selected(b) });
        }
        let configObjs = [];
        for (let c of configs) {
            configObjs.push({ name: c, selected: configsPrev.selected(c) });
        }
        let paramsTwoLevelObjs = [];
        for (let p of params) {
            let parObj = getParamObj($scope.selectedBenchmark, p);
            if (!paramsTwoLevel.includes(parObj.name)) {
                paramsTwoLevel.push(parObj.name);
                paramsTwoLevelObjs.push(parObj);
            } else {
                paramsTwoLevelObjs.byName(parObj.name).subs.push(parObj.subs[0]);
            }
            let prevParObj = $scope.paramsTwoLevel.byName(parObj.name);
            if (prevParObj) {
                paramsTwoLevelObjs.byName(parObj.name).subs.select(prevParObj.subs.selectedNames(), true);
            }
        }
        $scope.paramsTwoLevel.selectedNames().forEach(name => {
            if (paramsTwoLevelObjs.select(name, true)) {
                paramsTwoLevelObjs.byName(name).subs.select(_ALL_, true);
            }
        });
        $scope.visibleRuns.allSelected = false;
        $scope.anyConfigsSelected = true;
        $scope.runs = runs;
        if ($scope.vms.length > 0)
            $scope.vmsPrev = $scope.vms;
        $scope.vms = vmObjs;
        if ($scope.apps.length > 0)
            $scope.appsPrev = $scope.apps;
        $scope.apps = appObjs;
        if ($scope.heaps.length > 0)
            $scope.heapsPrev = $scope.heaps;
        $scope.heaps = heapObjs;
        if ($scope.hosts.length > 0)
            $scope.hostsPrev = $scope.hosts;
        if ($scope.builds.length > 0)
            $scope.buildsPrev = $scope.builds;
        $scope.builds = buildObjs;
        if ($scope.configs.length > 0)
            $scope.configsPrev = $scope.configs;
        $scope.configs = configObjs;
        if ($scope.paramsTwoLevel.length > 0)
            $scope.paramsTwoLevelPrev = $scope.paramsTwoLevel;
        $scope.paramsTwoLevel = paramsTwoLevelObjs;
        $scope.filterRunList();
    }
    $scope.toggleCharts = (inputToggle, inputType) => {
        $scope.resultToggles.forEach(toggle => {
            if (inputType === TYPE_ALL_CHARTS && toggle.name.endsWith(' (chart)') || inputType && toggle.type === inputType) {
                toggle.selected = inputToggle.selected;
            }
        });
    }
    $scope.highlightToggles = (pattern) => {
        $scope.resultToggles.forEach(toggle => {
            toggle.highlighted = matches(toggle.mame, pattern);
        });
    }
    $scope.toggleToggles = (inputToggle, inputType, pattern) => {
        $scope.resultToggles.forEach(toggle => {
            if (toggle.type === TYPE_CHART_HEADER || !toggle.visible) {
                return;
            }
            if (pattern && (isChart(toggle.type) && inputType === TYPE_ALL_CHARTS || isValue(toggle.type) && inputType === TYPE_ALL_VALUES)) {
                if (matches(toggle.name, pattern)) {
                    toggle.selected = inputToggle.selected;
                }
            } else if (inputType === TYPE_ALL || (inputType && toggle.type === inputType) || (inputType === TYPE_ALL_CHARTS && isChart(toggle.type))) {
                toggle.selected = inputToggle.selected;
            }
        });
    }
    function handleSearchResponse(path, r) {
        console.log('handleSearchResponse...');
        let foundResults = [];
        if (r.data.docs) {
            foundResults = r.data.docs;
            if ($scope.sortedCol === null) {
                $scope.sortedCol = 'results_dir';
            }
            $scope.sortDirLabel[$scope.sortedCol] = $scope.sortingDir === 1 ? '\u2193' : '\u2191';
            foundResults.sortNum($scope.sortedCol, $scope.sortingDir);
        }
        let maxElems = 0;
        foundResults.forEach(res => {
            res.results_dir_short = shortenPath(res.results_dir, path);
            const dirElems = res.results_dir_short.split('/').filter(s => !!s);
            maxElems = dirElems.length > maxElems ? dirElems.length : maxElems;
            let paths = path + '/';
            res.results_dir_elems = [];
            for (const dirElem of dirElems) {
                paths += dirElem + '/';
                res.results_dir_elems.push({ name: dirElem, path: paths });
            }
        });
        $scope.searchHeaderPropertiesSplit = [];
        for (let i = 1; i <= maxElems; i++) {
            $scope.searchHeaderPropertiesSplit.push('results_dir' + i);
            foundResults.forEach(res => {
                if (!res.results_dir_elems[i - 1]) {
                    res.results_dir_elems[i - 1] = {}
                }
                res['results_dir' + i] = res.results_dir_elems[i - 1].name || '';
            });
        }
        $scope.foundResults = foundResults;
        $scope.foundResultsPath = path;
        $scope.searchHeaderPropertiesSplit.push(...['id', 'host', 'benchmark', 'config']);
    }
    function handleResultResponse(r) {
        console.log('handleResultResponse...');
        const hits = (r.data.docs ? r.data.docs : r.data.hits.hits).filter(doc => doc && doc._source);
        normalizeHitsProperties(hits);
        normalizeHitsMetrics(hits, $scope.showOptions, $scope.filterDataElements);
        const noCharts = parseShowOpts($scope.showOptions).noCharts || !hasChartValues(hits);
        const noTotals = !hasTotals(hits);
        const noRates = !hasRates(hits);
        const showAllCharts = $scope.showDataElements.includes('all') || $scope.showDataElements.includes('ALL') || $scope.showDataElements.includes('allcharts');
        const showAllValues = $scope.showDataElements.includes('all') || $scope.showDataElements.includes('ALL') || $scope.showDataElements.includes('allvalues');
        const header = { type: TYPE_HEADER, style: 'common_toggle' };
        const generic = { generic: true, style: 'common_toggle' };
        const chart_header = { type: TYPE_CHART_HEADER, style: 'common_toggle' };
        const toggles = [
            { name: 'Headers', type: TYPE_HEADERS, style: 'toggle_label' },
            { name: 'id', ...header, selected: showData('id', TYPE_HEADER, $scope.showDataElements) },
            { name: 'VM', ...header, selected: true, missing: !availableProps(hits, 'vm_name') },
            { name: 'build', ...header, selected: showData('build', TYPE_HEADER, $scope.showDataElements), missing: !availableProps(hits, 'build') },
            { name: 'app', ...header, selected: showData('app', TYPE_HEADER, $scope.showDataElements), missing: !availableProps(hits, 'application') },
            { name: 'benchmark', ...header, selected: showData('benchmark', TYPE_HEADER, $scope.showDataElements), missing: !availableProps(hits, 'benchmark_name') },
            { name: 'workload', ...header, selected: showData('workload', TYPE_HEADER, $scope.showDataElements), missing: !availableProps(hits, 'workload_name') },
            { name: 'params', ...header, selected: showData('params', TYPE_HEADER, $scope.showDataElements), missing: !availableProps(hits, 'workload_parameters') },
            { name: 'config', ...header, selected: showData('config', TYPE_HEADER, $scope.showDataElements), missing: !availableProps(hits, 'config') },
            { name: 'host', ...header, selected: showData('host', TYPE_HEADER, $scope.showDataElements), missing: !availableProps(hits, 'hosts') },
            { name: 'heap', ...header, selected: showData('heap', TYPE_HEADER, $scope.showDataElements), missing: !availableProps(hits, 'heap') },
            { name: 'results', ...header, selected: showData('results', TYPE_HEADER, $scope.showDataElements), missing: !availableProps(hits, 'results_dir') },
            { name: 'run time', ...header, selected: showData('run time', TYPE_HEADER, $scope.showDataElements), missing: !availableProps(hits, 'time_spent_minutes') },
            { name: '', style: 'toggle_separator', missing: noCharts },
            { name: 'Filter Charts', type: TYPE_FILTER_CHARTS, style: 'toggle_filter', missing: noCharts },
            { name: 'Charts', style: 'toggle_label', missing: noCharts },
            { name: 'all charts', ...chart_header, selected: showAllCharts, missing: noCharts, toggle: t => $scope.toggleToggles(t, TYPE_ALL_CHARTS) },
            { name: 'wide charts', type: TYPE_CHART_HEADER, style: 'common_toggle', selected: $scope.wide_charts, missing: noCharts, toggle: () => $scope.wide_charts = !$scope.wide_charts },
            { name: '', generic: true, type: TYPE_CHART_MAX },
            { name: '', generic: true, style: 'toggle_separator' },
            { name: 'Filter Values', type: TYPE_FILTER_VALUES, style: 'toggle_filter' },
            { name: 'Values', generic: true, style: 'toggle_label' },
            { name: 'all values', ...generic, selected: showAllValues, toggle: t => $scope.toggleToggles(t, TYPE_NUMBER) },
            //{ name: 'steps', ...header, selected: showData('steps', TYPE_HEADER, $scope.showDataElements) },
            { name: 'avg', ...header, selected: showData('avg', TYPE_HEADER, $scope.showDataElements) },
            ///{ name: 'best', ...header, selected: showData('best', TYPE_HEADER, $scope.showDataElements) },
            ///{ name: 'worst', ...header, selected: showData('worst', TYPE_HEADER, $scope.showDataElements) },
            { name: 'min', ...header, selected: showData('min', TYPE_HEADER, $scope.showDataElements) },
            { name: 'max', ...header, selected: showData('max', TYPE_HEADER, $scope.showDataElements) },
            { name: 'diff %', ...header, selected: true, missing: hits.length < 2 },
            ///{ name: 'diff x', ...header, selected: showData('diff x', TYPE_HEADER, $scope.showDataElements), missing: hits.length < 2 },
            ///{ name: 'diff raw', ...header, selected: showData('diff raw', TYPE_HEADER, $scope.showDataElements), missing: hits.length < 2 },
            { name: 'diff col', ...header, selected: showData('diff col', TYPE_HEADER, $scope.showDataElements), missing: hits.length < 2 },
            ///{ name: 'value', ...header, selected: showData('value', TYPE_HEADER, $scope.showDataElements) },
            { name: 'values', ...header, selected: showData('values', TYPE_HEADER, $scope.showDataElements) },
            ///{ name: 'var', ...header, selected: showData('var', TYPE_HEADER, $scope.showDataElements) },
            ///{ name: 'sum', ...header, selected: showData('sum', TYPE_HEADER, $scope.showDataElements) },
            { name: 'error rate', ...header, selected: showData('error rate', TYPE_HEADER, $scope.showDataElements), missing: noTotals },
            { name: 'total errors', ...header, selected: showData('total errors', TYPE_HEADER, $scope.showDataElements), missing: noTotals },
            { name: 'total values', ...header, selected: showData('total values', TYPE_HEADER, $scope.showDataElements), missing: noTotals },
            { name: 'target rate', ...header, selected: showData('target rate', TYPE_HEADER, $scope.showDataElements), missing: noRates },
            { name: 'actual rate', ...header, selected: showData('actual rate', TYPE_HEADER, $scope.showDataElements), missing: noRates },
        ];
        if (hasPercentiles(hits)) {
            PERCENTILES.forEach(pr => toggles.addToggleOpt('p' + pr, TYPE_HEADER, $scope.showDataElements, 'common_toggle'));
            toggles.addToggleOpt('HDR', TYPE_HEADER, $scope.showDataElements, 'common_toggle');
        }
        hits.forEach(hit => {
            hit._source.metrics?.forEach(metric => {
                metric.metricValues.forEach(mv => {
                    if (mv.isValues && mv.name) {
                        toggles.addToggleOpt(mv.name, TYPE_HEADER, $scope.showDataElements, 'common_toggle');
                    }
                });
            });
        });
        toggles.push({ name: '', generic: true, type: TYPE_VALUES });
        toggles.forEach(t => t.visible = true);
        $scope.resultToggles = toggles.filter(t => !t.missing);
        $scope.producedResults = produceResultsTable(hits, $scope);
        let reducedResults = reduceResultsTable($scope, $scope.producedResults);
        reducedResults = produceComparisonTable(reducedResults, $scope.resultToggles, $scope.compare_prev);
        if ($scope.display === DISPLAY_SUMMARY) {
            $scope.report = produceAggregatedComparisonReport($scope, hits, reducedResults);
        } else if ($scope.display === DISPLAY_REPORT) {
            $scope.report = produceReport($scope, hits, reducedResults);
        } else {
            $scope.report = produceDetailedComparisonReport($scope, hits, reducedResults);
        }
    }
    $scope.excludeData = (id) => {
        for (let i = $scope.visibleRuns.length - 1; i >= 0; i--) {
            if ($scope.visibleRuns[i]._id === id)
                $scope.visibleRuns.splice(i, 1)
            else
                $scope.visibleRuns[i].selected = true;
        }
        $scope.openDetailedReport(null, null, '_self', $scope.display === 'SUMMARY');
    }
    $scope.getBenchmarkList = () => {
        console.log('getBenchmarkList...');
        return $http({ method: 'GET', url: BASE_API_URL_BENCHMARKS + '_mapping' })
            .then(response => handleBenchmarkListResponse(response, 'benchmarks'))
            .catch($scope.operationFailed);
    }
    function handleBenchmarkListResponse(r, propName) {
        console.log('handleBenchmarkListResponse...');
        let benchmarks = [];
        if (r.data[propName] && r.data[propName]['mappings']) {
            for (let b in r.data[propName]['mappings']) {
                console.log(propName + ': ' + b);
                benchmarks.push({ name: b, selected: false });
            }
        }
        $scope.benchmarks = benchmarks;
        if (!$scope.selectedBenchmark || !$scope.benchmarks.map(b => b.name).includes($scope.selectedBenchmark)) {
            $scope.selectedBenchmark = $scope.benchmarks[0].name;
        } else if (!$scope.benchmarks || $scope.benchmarks.length === 0) {
            $scope.selectedBenchmark = '';
        }
    }
    $scope.getReportList = () => {
        console.log('getReportList...');
        return $http({ method: 'GET', url: BASE_API_URL_PORTAL_DATA })
            .then(response => handlePortalDataResponse(response))
            .catch($scope.operationFailed);
    }
    function handlePortalDataResponse(r) {
        console.log('handlePortalDataResponse...');
        let reports = [];
        if (r.data.doc && r.data.doc.data) {
            r.data.doc.data.forEach(dat => console.log('portal data: ' + dat.name));
            reports = r.data.doc.data;
        }
        $scope.reports = reports;
    }
    $scope.showRuns = () => {
        console.log('showRuns...');
        $scope.getBenchmarkList()
            .then($scope.getHostList)
            .then($scope.getWorkloadList);
    }
    $scope.showPortal = () => {
        console.log('showPortal...');
        $scope.getReportList();
    }
    $scope.getWorkloadList = () => {
        console.log('getWorkloadList...');
        let req_data = {
            size: 0,
            aggs: {
                workload_count: {
                    terms: {
                        field: 'runProperties.workload_name',
                        size: 1000
                    }
                }
            }
        };
        $scope.isLoading++;
        $scope.lastOperationStatus = 'Loading workloads...';
        return $http({ method: 'POST', url: BASE_API_URL_BENCHMARKS + $scope.selectedBenchmark + '/_search', data: req_data })
            .then(response => handleWorkloadListResponse(response))
            .then($scope.operationPassed)
            .catch($scope.operationFailed);
    }
    $scope.getHostList = () => {
        console.log('getHostList...');
        let req_data = {
            size: 0,
            aggs: {
                host_count: {
                    terms: {
                        field: 'runProperties.host',
                        size: 1000
                    }
                }
            }
        };
        $scope.isLoading++;
        $scope.lastOperationStatus = 'Loading hosts...';
        return $http({ method: 'POST', url: BASE_API_URL_BENCHMARKS + $scope.selectedBenchmark + '/_search', data: req_data })
            .then(response => handleHostListResponse(response))
            .then($scope.operationPassed)
            .catch($scope.operationFailed);
    }
    $scope.clearRunList = function() {
        $scope.runs = [];
        $scope.vms = [];
        $scope.apps = [];
        $scope.heaps = [];
        $scope.builds = [];
        $scope.configs = [];
        $scope.paramsTwoLevel = [];
        $scope.filterRunList();
    }
    $scope.start = 0;
    $scope.total = 5000;
    $scope.getRunList = () => {
        if (!$scope.selectedBenchmark) {
            return $scope.clearRunList();
        }
        setCookie('benchmarks', $scope.selectedBenchmark);
        const bench = $scope.selectedBenchmark;
        const workloads = [];
        $scope.workloads.fixAllSelected();
        $scope.workloads.forEach(w => { if (w.selected) workloads.push(w.name); });
        const hosts = [];
        $scope.hosts.fixAllSelected();
        $scope.hosts.forEach(h => { if (h.selected) hosts.push(h.name); });
        if (workloads.length === 0 || hosts.length === 0) {
            return $scope.clearRunList();
        }
        const req_data = {
            from: $scope.start,
            size: $scope.total,
            query: {
                bool: {
                    must: [
                        { terms: { 'runProperties.workload_name': workloads } },
                        { terms: { 'runProperties.host': hosts } }
                    ]
                }
            }
        };
        $scope.isLoading++;
        $scope.lastOperationStatus = 'Loading runs...';
        return $http({ method: 'POST', url: BASE_API_URL_BENCHMARKS + bench + '/_search?_source=run-status,report,runProperties&pretty', data: req_data })
            .then(response => handleRunListResponse(response))
            .then($scope.operationPassed)
            .catch($scope.operationFailed);
    }
    $scope.openReport = (report) => {
        $window.open(report.url, '_blank');
    }
    $scope.queryArgs = (runs, opts, show, filter, summary) => {
        const q = {
            b: $scope.selectedBenchmark,
            p: $scope.compare_prev,
            opts,
            show,
            filter
        };
        if (!$scope.separateByToggles.equallsSelectedTo(separateByToggles_default)) {
            q.s = $scope.separateByToggles.joinSelected('name', ',');
        }
        if (!$scope.groupByToggles.equallsSelectedTo(groupByToggles_default)) {
            q.g = $scope.groupByToggles.joinSelected('name', ',');
        }
        if ($scope.xTicks !== xTicks_default) {
            q.xTicks = $scope.xTicks;
        }
        q.r = runs;
        if (summary) {
            q.summary = '1';
        }
        return q;
    }
    $scope.openDetailedReport = (event, prev, target, summary) => {
        console.log('openDetailedReport...');
        if (prev !== null) {
            $scope.compare_prev = prev;
        }
        let runs = $scope.visibleRuns.joinSelected('_id', ',');
        if (runs.length > 0) {
            const q = $scope.queryArgs(runs, $scope.showOptions.join(','), $scope.showDataElements.join(','), $scope.filterDataElements.join(','), summary);
            $scope.openResults(event, q, target);
        }
    }
    function getShowOpts() {
        if ($scope.showDefaultValues) {
            return "";  
        }
        if ($scope.showAllMetrics) {
            return "all";  
        } 
        if ($scope.showAllCharts) { 
            return  "allcharts";
        }
        if ($scope.showAllValues) {
            return "allvalues";
        }
        if ($scope.showCustomValues) {
            return $scope.showCustomTest;
        }
        return $scope.showDataElements.join(',');   
    }
    function getFilterOpts() {
        if ($scope.filterValues) {
            return $scope.filterText;
        }
        return $scope.filterDataElements.join(',');
    }
    function getOptionsOpts() {
        const showOptions = [...$scope.showOptions];
        if ($scope.noSortMetrics) {
            showOptions.push("nosort");
        }             
        if ($scope.optsValues) {
            showOptions.push($scope.optsText);
        }
        return showOptions.join(',');
    }
    $scope.openSearchResults = (event, prev, target, summary) => {
        console.log('openSearchResults...');
        if (prev !== null) {
            $scope.compare_prev = prev;
        }
        let runs = $scope.useIDs ?
        $scope.foundResults.joinSelected('id', ',', encodeURIComponent) : 
        $scope.foundResults.joinSelected('results_dir', ',', encodeURIComponent);
        if (runs.length > 0) {
            const q = $scope.queryArgs(runs, getOptionsOpts(), getShowOpts(), getFilterOpts(), summary);
            $scope.openResults(event, q, target);
        }
    }
    $scope.showDetailedReportOrSummary = () => {
        console.log('showDetailedReportOrSummary...');
        let req = BASE_API_URL_BENCHMARKS;
        if ($scope.selectedBenchmark) {
            req += $scope.selectedBenchmark + '/';
        }
        req += '_mget';
        let req_data = { docs: [] };
        let pathes = [];
        for (let r of $scope.visibleRuns) {
            if (r.selected) {
                if (r._id.startsWith('/') || r._id.startsWith('%2F')) {
                    pathes.push(r._id);
                } else {
                    req_data.docs.push({ _id: r._id });
                }
            }
        }
        if (pathes.length > 0) {
            return $scope.showDetailedReportOrSummaryFromPath(pathes);
        }
        if (req_data.docs.length === 0) {
            $scope.report = null;
            return;
        }
        $scope.isLoading++;
        $scope.lastOperationStatus = 'Loading detailed summary report...';
        return $http({ method: 'POST', url: req, data: req_data })
            .then(response => handleResultResponse(response))
            .then($scope.operationPassed)
        // .catch($scope.operationFailed);
    };
    $scope.showDetailedReportOrSummaryFromPath = (pathes) => {
        console.log('showDetailedReportOrSummaryFromPath: ' + pathes);
        $scope.isLoading++;
        $scope.lastOperationStatus = 'Loading detailed summary report from path...';
        let url = BASE_RELEASE_API_URL + pathes;
        if ($scope.processDirsRec) {
            url += "?rec=true"
        }
        return $http({ method: 'GET', url })
            .then(response => handleResultResponse(response))
            .then($scope.operationPassed)
        // .catch($scope.operationFailed);
    };
    $scope.searchResultsFromPath = (path) => {
        path = fixPath(path);
        console.log('searchResultsFromPath: ' + path);
        $scope.isLoading++;
        $scope.lastOperationStatus = 'Loading search results from path...';
        let url = BASE_SEARCH_API_URL + path;
        return $http({ method: 'GET', url })
            .then(response => handleSearchResponse(path, response))
            .then($scope.operationPassed)
        // .catch($scope.operationFailed);
    };
    $scope.showResults = () => {
        $scope.visibleRuns = [];
        $scope.visibleRuns.allSelected = false;
        let localMetrics = [];
        if (typeof metricsData !== 'undefined') localMetrics.push(metricsData);
        if (typeof metricsData1 !== 'undefined') localMetrics.push(metricsData1);
        if (typeof metricsData2 !== 'undefined') localMetrics.push(metricsData2);
        if (typeof metricsData3 !== 'undefined') localMetrics.push(metricsData3);
        if (typeof metricsData4 !== 'undefined') localMetrics.push(metricsData4);
        if (typeof metricsData5 !== 'undefined') localMetrics.push(metricsData5);
        if (typeof metricsData6 !== 'undefined') localMetrics.push(metricsData6);
        if (typeof metricsData7 !== 'undefined') localMetrics.push(metricsData7);
        if (typeof metricsData8 !== 'undefined') localMetrics.push(metricsData8);
        if (typeof metricsData9 !== 'undefined') localMetrics.push(metricsData9);
        if (typeof metrics_data !== 'undefined') localMetrics.push(metrics_data);
        if (typeof metrics_data1 !== 'undefined') localMetrics.push(metrics_data1);
        if (typeof metrics_data2 !== 'undefined') localMetrics.push(metrics_data2);
        if (typeof metrics_data3 !== 'undefined') localMetrics.push(metrics_data3);
        if (typeof metrics_data4 !== 'undefined') localMetrics.push(metrics_data4);
        if (typeof metrics_data5 !== 'undefined') localMetrics.push(metrics_data5);
        if (typeof metrics_data6 !== 'undefined') localMetrics.push(metrics_data6);
        if (typeof metrics_data7 !== 'undefined') localMetrics.push(metrics_data7);
        if (typeof metrics_data8 !== 'undefined') localMetrics.push(metrics_data8);
        if (typeof metrics_data9 !== 'undefined') localMetrics.push(metrics_data9);
        if (localMetrics.length > 0) {
            console.log('showResults: using local metrics...');
            let docs = [];
            localMetrics.forEach(adoc => adoc.forEach(doc => docs.push(doc)));
            $scope.display = DISPLAY_REPORT;
            $scope.showDataElements = ['summary_max_latency_(chart)', 'summary_max_response_time_(chart)'];
            $scope.wide_charts = true;
            handleResultResponse({ data: { docs } });
            return;
        }
        if ($location.search().r) {
            for (let runId of $location.search().r.split(',')) {
                if (runId) {
                    $scope.visibleRuns.push({ _id: fixPath(runId), selected: true });
                }
            }
        }
        if ($location.search().portal) {
            console.log('showResults: portal...');
            $scope.display = DISPLAY_PORTAL;
        } else if ($location.search().search) {
            console.log('showResults: search...');
            $scope.display = DISPLAY_SEARCH;
        } else if ($scope.visibleRuns.length === 0) {
            console.log('showResults: runs...');
            $scope.display = DISPLAY_RUNS;
        } else {
            if ($location.search().summary) {
                console.log('showResults: summary...');
                $scope.display = DISPLAY_SUMMARY;
            } else if ($location.search().hls) {
                console.log('showResults: high level summary ..');
                $scope.display = DISPLAY_HL_SUMMARY;
            } else {
                console.log('showResults: details...');
                $scope.display = DISPLAY_DETAILS;
            }
            if ($location.search().g) {
                $scope.groupByToggles.forEach(e => e.selected = false);
                $location.search().g.split(',').forEach(group => $scope.groupByToggles.select(group, true));
            }
            if ($location.search().s) {
                $scope.separateByToggles.forEach(e => e.selected = false);
                $location.search().s.split(',').forEach(group => $scope.separateByToggles.select(group, true));
            } else {
                $scope.separateByToggles.forEach(e => e.selected = false);
            }
            if (typeof $location.search().show === 'string') {
                $scope.showDataElements = $location.search().show.split(',');
            } else {
                $scope.showDataElements = [];
            }
            if (typeof $location.search().filter === 'string') {
                $scope.filterDataElements = $location.search().filter.split(',');
            } else {
                $scope.filterDataElements = [];
            }
            if (typeof $location.search().opts === 'string') {
                $scope.showOptions = $location.search().opts.split(',');
            } else {
                $scope.showOptions = [];
            }
            if ($location.search().wide) {
                console.log('showResults: wide...');
                $scope.wide_charts = true;
            }
            $scope.processDirsRec = !!$location.search().rec;
            $scope.compare_prev = $location.search().p;
            if (!$scope.compare_prev) {
                $scope.compare_prev = 0;
            }
            $scope.selectedBenchmark = $location.search().b;
            console.log('selectedBenchmark1: ' + $scope.selectedBenchmark + ' processDirsRec: ' + $scope.processDirsRec);
        }
        if ($location.search().xTicks) {
            $scope.xTicks = $location.search().xTicks;
        }
        if ($scope.display === DISPLAY_PORTAL) {
            $scope.showPortal();
        } else if ($scope.display === DISPLAY_RUNS) {
            $scope.showRuns();
        } else if ($scope.display === DISPLAY_SEARCH) {
            $scope.searchResultsFromPath($location.search().search);
        } else {
            $scope.showDetailedReportOrSummary();
        }
    }
    $scope.openResultsDir = function(event, results_dir) {
        event.stopPropagation();
        if (results_dir) {
            window.open(getWebPath(results_dir));
        }
    };
    $scope.openID = (event, id) => {
        event.stopPropagation();
        if (event.ctrlKey) {
            $scope.openResults(event, { url: BASE_API_URL_BENCHMARKS + $scope.selectedBenchmark + '/' + id });
        } else {
            $scope.openResults(event, { r: id });
        }
    };
    $scope.openHighLevelSummaryReport = (event, target) => {
        event.stopPropagation();
        $scope.openResults(event, { hls: 1 }, target);
    };
    $scope.setReportFlag = (val) => {
        $scope.visibleRuns.forEach(run => { if (run.selected) run.report = val });
    };
    $scope.openResults = (event, q, target) => {
        if (!target && event /* && event.ctrlKey */) {
            target = '_blank';
        }
        if (target === '_blank') {
            let qs = '#!?A';
            if (q.url) {
                qs = q.url;
            }
            if (q.portal) {
                qs += '&portal';
            }
            if (q.summary) {
                qs += '&summary';
            }
            if (q.hls) {
                qs += '&hls'; // high level summary
            }
            if (q.opts) {
                qs += '&opts=' + q.opts;
            }
            if (q.r) {
                qs += '&r=' + q.r; // runs
            }
            if (q.show) {
                qs += '&show=' + q.show;
            }
            if (q.filter) {
                qs += '&filter=' + q.filter;
            }
            if (q.g) {
                qs += '&g=' + q.g; // group by
            }
            if (q.s) {
                qs += '&s=' + q.s; // separate by
            }
            if (q.b) {
                qs += '&b=' + q.b; // benchmark
            }
            if (q.p) {
                qs += '&p=' + q.p; // prev
            }
            if (q.xTicks) {
                qs += '&xTicks=' + q.xTicks; // chart x-ticks
            }
            $window.open(qs, target);
        } else {
            let prevHandler = $scope.$on('$locationChangeSuccess', () => {/**/});
            $location.search(q);
            $scope.$on('$locationChangeSuccess', prevHandler);
            $scope.showResults();
        }
    };
    $scope.$on('$locationChangeSuccess', (_angularEvent, newUrl, oldUrl) => {
        if (newUrl !== oldUrl) {
            $scope.showResults();
        }
    });
    $scope.sortRunItems = (col) => {
        if (col !== null) {
            console.log('sortRunItems: ' + col);
            $scope.sortingDir = $scope.sortedCol === col ? -$scope.sortingDir : 1;
            $scope.sortedCol = col;
        }
        if ($scope.sortedCol === null) {
            return;
        }
        const secondaryCols = [];
        if ($scope.sortedCol !== 'workload_name') {
            secondaryCols.push('workload_name');
        }
        if ($scope.sortedCol !== 'workload_parameters') {
            secondaryCols.push('workload_parameters');
        }
        $scope.sortDirLabel = [];
        $scope.resultsHeaderProperties.forEach(e => $scope.sortDirLabel[e] = '');
        $scope.sortDirLabel[$scope.sortedCol] = $scope.sortingDir === 1 ? '\u2193' : '\u2191';
        $scope.visibleRuns.sortNum($scope.sortedCol, $scope.sortingDir, secondaryCols);
    };
    $scope.sortSearchItems = (col) => {
        $scope.sortingDir = $scope.sortedCol === col ? -$scope.sortingDir : 1;
        $scope.sortedCol = col;
        if ($scope.splitPaths) {
            $scope.searchHeaderPropertiesSplit.forEach(e => $scope.sortDirLabel[e] = '');
        } else {
            $scope.searchHeaderProperties.forEach(e => $scope.sortDirLabel[e] = '');
        }
        $scope.sortDirLabel[$scope.sortedCol] = $scope.sortingDir === 1 ? '\u2193' : '\u2191';
        $scope.foundResults.sortNum($scope.sortedCol, $scope.sortingDir);
    }
    $scope.filterRunList_ = () => {
        const visibleRuns = [];
        for (let run of $scope.runs) {
            let configsVisible = 0;
            let configNum = 0;
            for (let cfg of $scope.configs) {
                if ($scope.anyConfigsSelected) {
                    if (cfg.selected && run.config.indexOf(cfg.name) >= 0) {
                        configsVisible++;
                        configNum++;
                        break;
                    }
                } else {
                    if (run.config.hasWord(cfg.name)) {
                        configNum++;
                        if (cfg.selected) {
                            configsVisible++;
                        }
                    }
                }
            }
            let inTimes = $scope.times.allSelected;
            if (!inTimes) {
                let now = new Date().getTime();
                for (let time of $scope.times) {
                    if (time.selected && run.start_time &&
                        run.finish_time.getTime() > now - time.finish &&
                        (time.finish === -1 || run.finish_time.getTime() <= now - time.start)) {
                        inTimes = true;
                        break;
                    }
                }
            }
            let inVMs = $scope.vms.allSelected || $scope.vms.selected(run.vm_name);
            let inApps = $scope.apps.allSelected || $scope.apps.selected(run.application);
            let inHeaps = $scope.heaps.allSelected || $scope.heaps.selected(run.heap);
            let inHosts = $scope.hosts.allSelected || $scope.hosts.selected(run.hosts);
            let inBuilds = $scope.builds.allSelected || $scope.builds.selected(run.build);
            let inParams = false;
            let parObj = getParamObj($scope.selectedBenchmark, run.workload_parameters);
            for (let par of $scope.paramsTwoLevel) {
                if (par.selected && parObj.name === par.name) {
                    if (par.subs.selected(parObj.subs[0].name)) {
                        inParams = true;
                        break;
                    }
                }
            }
            if (inParams && inVMs && inApps && inHeaps && inTimes && inHosts && inBuilds && configNum > 0 && configsVisible === configNum) {
                visibleRuns.push(run);
            }
        }
        visibleRuns.fixAllSelected();
        $scope.visibleRuns = visibleRuns;
        $scope.sortRunItems(null);
    };
    $scope.filterRunList = () => {
        $scope.times.fixAllSelected();
        $scope.vms.fixAllSelected();
        $scope.apps.fixAllSelected();
        $scope.heaps.fixAllSelected();
        $scope.hosts.fixAllSelected();
        $scope.builds.fixAllSelected();
        $scope.configs.fixAllSelected();
        $scope.paramsTwoLevel.fixAllSelected();
        for (let par of $scope.paramsTwoLevel) {
            par.subs.fixAllSelected();
        }
        $scope.filterRunList_();
    };
    $scope.filterUpdate = (from) => from.applyAllSelected($scope.filterRunList_);
    $scope.filterUpdateAllWorkload = () => {
        $scope.workloads.applyAllSelected($scope.filterRunList_);
        $scope.getRunList();
    };
    $scope.$watch('selectedBenchmark', (newValue, oldValue) => {
        if (newValue !== oldValue) {
            $scope.showResults();
        }
    });
    $scope.appInit = $scope.showResults;
});
