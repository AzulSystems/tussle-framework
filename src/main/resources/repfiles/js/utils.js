/*
 * (C) Azul Systems 2017-2023, author Ruslan Scherbakov
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * 
 * * Neither the name of [project] nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */

// Constants

let c_idx = 1;

const TYPE_HEADERS = c_idx++;
const TYPE_HEADER_MIN = c_idx++;
const TYPE_ID = c_idx++;
const TYPE_HEADER = c_idx++;
const TYPE_PARAMS = c_idx++;
const TYPE_DATE = c_idx++;
const TYPE_DATETIME = c_idx++;
const TYPE_RESULTS_DIR = c_idx++;
const TYPE_INFO = c_idx++;
const TYPE_FILTER_CHARTS = c_idx++;
const TYPE_FILTER_VALUES = c_idx++;
const TYPE_HEADER_MAX = c_idx++;

const TYPE_CHART_MIN = c_idx++;
const TYPE_CHART_CONTROLLER = c_idx++;
const TYPE_CHART_GROUP = c_idx++;
const TYPE_CHART = c_idx++;
const TYPE_CHART_TIME = c_idx++;
const TYPE_CHART_HISTOGRAM = c_idx++;
const TYPE_CHART_COUNTS = c_idx++;
const TYPE_CHART_THROUGHPUT = c_idx++;
const TYPE_CHART_COUNT_HISTOGRAM = c_idx++;
const TYPE_CHART_MAX = c_idx++;

const TYPE_VALUES = c_idx++;
const TYPE_NUMBER = c_idx++;

const TYPE_ALL = 1000;
const TYPE_ALL_CHARTS = 2000;
const TYPE_ALL_VISIBLE_CHARTS = 2100;
const TYPE_ALL_VALUES = 3000;
const _ALL_ = '_ALL_';
const MAX_LONG = 900719925474099;

const PERCENTILES = ['0', '50', '90', '99', '99.9', '99.99', '99.999', '100'];

function isChart(type) {
    return TYPE_CHART_MIN <= type && type <= TYPE_CHART_MAX;
}

function isHeader(type) {
    return TYPE_HEADER_MIN <= type && type <= TYPE_HEADER_MAX;
}

function isValue(type) {
    return type === TYPE_NUMBER || type === TYPE_VALUES;
}

function getNormedType(type) {
    if (type === TYPE_CHART_HISTOGRAM ||
        type === TYPE_CHART_COUNTS ||
        type === TYPE_CHART_THROUGHPUT ||
        type === TYPE_CHART_COUNT_HISTOGRAM) {
        type = TYPE_CHART_TIME;
    }
    return type;
}

// Utils

function randInt(max, min) {
    min = min || 0;
    return (min + (max - min) * Math.random()) | 0;
}

function randomColor() {
    return '#' + randInt(1 << 24).toString(16)
}

function randomDarkColor() {
    return '#' + randInt(1 << 16).toString(16)
}

function isLowerBetterValue(name) {
    name = name.toLowerCase();
    if (name.indexOf('total value') >= 0 ||
        name.indexOf('throughput') >= 0 ||
        name.indexOf('rate') >= 0) {
        return false;
    } else {
        return name.indexOf('(%cpu') >= 0 || 
            name.indexOf('%util') >= 0 ||
            name.indexOf('total errors') >= 0 ||
            name.indexOf('error rate') >= 0 ||
            name.indexOf('query') >= 0 ||
            name.indexOf('time') >= 0 || 
            name.indexOf('latency') >= 0 ||
            name.indexOf('seconds') >= 0;
    }
}

function pad(n) {
    if (n < 10) {
        return '0' + n;
    }
    return n;
}

function isEqual(a, b) {
    if (a !== null) {
        if (b !== null)
            return a === b ? 1 : 0;
        else
            return 0;
    } else if (b !== null) {
        return 0;
    } else {
        return 1;
    }
}

function roundPercent(s, f) {
    if (s === null)
        return null;
    if (f === null)
        f = 1;
    let n = parseFloat(s);
    return n.toFixed(f);
}

function getPercentValue(diff) {
    if (diff !== null) {
        return roundPercent(100 * diff, 0) + '%';
    }
    return null;
}

function getDiffValue(diff) {
    if (diff !== null) {
        let value = roundPercent(100 * diff, 0) + '%';
        if (diff > 0)
            value = '+' + value;
        return value;
    }
    return null;
}

function getDiffXValue(diff) {
    if (diff !== null) {
        let value;
        if (diff >= 1)
            value = 'x' + roundNumber(diff);
        else
            value = 'x' + roundNumber(1 / diff);
        return value;
    }
    return null;
}

function roundNumber(s) {
    if (s === null)
        return null;
    let n = parseFloat(s);
    if (n === 0)
        return '0';
    if (n >= 100 || n <= -100 || n === n.toFixed(0))
        return n.toFixed(0);
    if (n >= 10 || n <= -10 || n === n.toFixed(1))
        return n.toFixed(1);
    if (n >= 1 || n <= -1 || n === n.toFixed(2))
        return n.toFixed(2);
    if (n >= .1 || n <= -.1 || n === n.toFixed(3))
        return n.toFixed(3);
    if (n >= .01 || n <= -.01 || n === n.toFixed(4))
        return n.toFixed(4);
    return n.toFixed(5);
}

function maxNumber(a, b) {
    if (a !== null && b !== null) {
        return a > b ? a : b;
    }
    if (a !== null) {
        return a;
    }
    return b;
}

function minNumber(a, b) {
    if (a !== null && b !== null) {
        return a < b ? a : b;
    }
    if (a !== null) {
        return a;
    }
    return b;
}

function avgNumber(arr) {
    let sum = 0;
    let count = 0;
    if (arr) for (const n of arr) {
        if (n !== null) {
            count++;
            sum += n;
        }
    }
    return count > 0 ? sum / count : null;
}

function sumNumber(arr) {
    let sum = 0;
    let count = 0;
    if (arr) for (const n of arr) {
        if (n !== null) {
            count++;
            sum += n;
        }
    }
    return count > 0 ? sum : null;
}

function stdevNumber(arr) {
    if (!arr)
        return 0;
    const a = avgNumber(arr);
    if (a === null)
        return 0;
    let sum = 0;
    let count = 0;
    for (const n of arr) {
        if (n !== null) {
            count++;
            sum += Math.pow(n - a, 2);
        }
    }
    return count > 1 ? Math.sqrt(sum / (count - 1)) : 0;
}

function pushRowVal(row, val, idx, clss) {
    if (typeof val === 'undefined') {
        val = null;
    }
    if (!clss) {
        clss = '';
    }
    row[idx] = {
        orig_value: val,
        value: row.type === TYPE_NUMBER ? roundNumber(val) : val,
        //cls: clss + ' data_col nowrap'
        cls: clss + ' data_col'
    };
    return row[idx];
}

function pushVal(table, name, type, val, idx, clss, visible, hide) {
    if (typeof val === 'undefined') {
        val = null;
    }
    let row = table[name];
    if (!row) {
        row = table[name] = [{
            value: name,
            cls: 'first_col nowrap',
        }];
        row.visible = visible;
        row.hide = hide;
        row.position = Object.keys(table).length;
        row.type = type;
        row.cls = '';
    }
    return pushRowVal(row, val, idx + 1, clss);
}

function findVal(table, name, val) {
    if (table[name] === null) {
        return -1;
    }
    let row = table[name];
    if (row) {
        for (let i = 1; i < row.length; i++) {
            if (row[i] && row[i].value === val)
                return i - 1;
        }
    }
    return -1;
}

function copyValue(val) {
    if (!val)
        return null;
    let o = {
        value: val.value
    }
    if (typeof val.cls !== 'undefined') {
        o.cls = val.cls;
    }
    if (typeof val.orig_value !== 'undefined') {
        o.orig_value = val.orig_value;
    }
    if (typeof val.error_rate !== 'undefined') {
        o.error_rate = val.error_rate;
    }
    return o;
}

function getDiffClass(diff) {
    if (diff >= 0.1)
        return 'large_pos';
    if (diff >= 0.03)
        return 'light_pos';
    if (diff <= -0.1)
        return 'large_neg';
    if (diff <= -0.03)
        return 'light_neg';
    if (diff < 0)
        return 'lightlight_neg';
    return 'lightlight_pos';
}

function getSign(diff) {
    if (diff >= 0)
        return '+';
    else
        return '-';
}

function getParamObj(benchmark, p) {
    if (benchmark !== 'esrally') {
        p = 'subparams,' + p;
    }
    let pos = p.indexOf(',');
    let paramBase = pos >= 0 ? p.substring(0, pos) : p;
    let paramSub = pos >= 0 ? p.substring(pos + 1) : '';
    let obj = {
        name: paramBase,
        selected: false,
        subs: [{ name: paramSub, selected: true }]
    }
    obj.subs.allSelected = true;
    return obj;
}

function countUnique(arr, fn) {
    let map = new Map();
    arr.forEach(a => map.set(fn(a), 1));
    return map.size;
}

function splitStringDigit(s) {
    let parts = [''];
    if (s) {
        parts = s.split(/(\d+)/);
        for (let i = 1; i < parts.length; i += 2) {
            parts[i] = parseInt(parts[i]);
        }
    }
    return parts;
}

function setCookie(cname, cvalue, exdays) {
    if (!exdays) {
        exdays = 30;
    }
    const d = new Date();
    d.setTime(d.getTime() + (exdays * 24 * 60 * 60 * 1000));
    document.cookie = `${cname}=${cvalue};expires=${d.toUTCString()};path=/`;
}

function getCookie(cname, def) {
    let name = cname + '=';
    let decodedCookie = decodeURIComponent(document.cookie);
    for (const item of decodedCookie.split(';')) {
        let c = item;
        while (c.charAt(0) === ' ') {
            c = c.substring(1);
        }
        if (c.indexOf(name) === 0) {
            return c.substring(name.length, c.length);
        }
    }
    return def;
}

function getCookieInt(cname, def) {
    const res = parseInt(getCookie(cname, def));
    if (isNaN(res)) {
        return def;
    } else {
        return res;
    }
}

function shortenPath(dir, baseDir) {
    if (dir.startsWith(baseDir)) {
        dir = dir.substring(baseDir.length);
        if (dir.startsWith('/')) {
            dir = dir.substring(1);
        }
    }
    return dir;
}

function matches(name, pattern) {
    if (!pattern) {
        return false;
    }
    if (!name) {
        return false;
    }
    name = name.toLowerCase();
    pattern = pattern.toLowerCase();
    if (name.indexOf(pattern) >= 0) {
        return true;
    }
    try {
        return new RegExp(pattern, 'ig').test(name);
    } catch (e) {
    }
    return false;
}

// Date prototype

Date.prototype.formatDateTimeUTC = function() {
    return this.getUTCFullYear() +
        '-' + pad(this.getUTCMonth() + 1) +
        '-' + pad(this.getUTCDate()) +
        ' ' + pad(this.getUTCHours()) +
        ':' + pad(this.getUTCMinutes()) +
        ':' + pad(this.getUTCSeconds());
};

Date.prototype.formatDateUTC = function() {
    return this.getUTCFullYear() +
        '-' + pad(this.getUTCMonth() + 1) +
        '-' + pad(this.getUTCDate());
};

Date.prototype.formatDateTime = function() {
    return this.getFullYear() +
        '-' + pad(this.getMonth() + 1) +
        '-' + pad(this.getDate()) +
        ' ' + pad(this.getHours()) +
        ':' + pad(this.getMinutes()) +
        ':' + pad(this.getSeconds());
};

Date.prototype.formatTimeOnlyUTC = function() {
    return pad(this.getUTCHours()) +
        ':' + pad(this.getUTCMinutes()) +
        ':' + pad(this.getUTCSeconds());
};

Date.prototype.formatTimeOnly = function() {
    return pad(this.getHours()) +
        ':' + pad(this.getMinutes()) +
        ':' + pad(this.getSeconds());
};

// Array prototype

Array.prototype.sum = function(func) {
    let sum = 0;
    if (func) {
        for (let i = 1; i < this.length; i++) {
            if (func(this[i])) {
                sum += this[i];
            }
        }
    } else {
        for (let i = 1; i < this.length; i++) {
            sum += this[i];
        }
    }
    return sum;
}

Array.prototype.avg = function() {
    return avgNumber(this);
}

function maxImpl(arr) {
    if (!arr || arr.length === 0)
        return null;
    let max = arr[0];
    for (let i = 1; i < arr.length; i++) {
        if (max < arr[i])
            max = arr[i]
    }
    return max;
}

Array.prototype.max = function() {
    return maxImpl(this);
}

Array.prototype.min = function() {
    if (this.length === 0)
        return null;
    let min = this[0];
    for (let i = 1; i < this.length; i++) {
        if (min > this[i])
            min = this[i]
    }
    return min;
}

Array.prototype.stdev = function() {
    return stdevNumber(this);
}

function cmpSplit(sortDir, cols, a, b) {
    let res = 0;
    let col = 0
    for (; col < cols.length; col++) {
        if (a.split[col] && b.split[col]) {
            let i = 0;
            for (; i < a.split[col].length; i++) {
                if (i < b.split[col].length) {
                    if (i % 2 === 0)
                        res = a.split[col][i].localeCompare(b.split[col][i]);
                    else
                        res = a.split[col][i] - b.split[col][i];
                    if (res === 0)
                        continue;
                } else {
                    res = 1;
                }
                break;
            }
            if (i === a.split[col].length && i < b.split[col].length) {
                res = -1;
            }
        } else {
            res = a.time[col] - b.time[col];
        }
        if (res !== 0) {
            break;
        }
    }
    if (col === 0) {
        return res * sortDir;
    } else {
        return res;
    }
}


function cmpNumbers(a, b) {
    if (a > b) return 1;
    if (a < b) return -1;
    return 0;
}

Array.prototype.sortNumbers = function() {
    return this.sort((a, b) => cmpNumbers(a, b));
}

Array.prototype.sortNum = function(firstCol, sortDir, secondaryCols) {
    if (sortDir !== -1) {
        sortDir = 1;
    }
    let cols = [];
    if (secondaryCols) {
        cols = secondaryCols;
        cols.splice(0, 0, firstCol);
    } else {
        cols.push(firstCol);
    }
    const cmpArr = [];
    for (const item of this) {
        const obj = { orig: item, time: [], split: [] };
        for (let col = 0; col < cols.length; col++) {
            const prop = cols[col];
            const val = prop ? item[prop] : item;
            if (val && typeof val.getTime === 'function') {
                obj.time[col] = val.getTime();
            } else {
                obj.split[col] = splitStringDigit(val);
            }
        }
        cmpArr.push(obj);
    }
    cmpArr.sort((a, b) => cmpSplit(sortDir, cols, a, b));
    for (let i = 0; i < this.length; i++) {
        this[i] = cmpArr[i].orig;
    }
    return this;
}

Array.prototype.removeDups = function() {
    let res = [];
    for (const item of this) {
        if (res.indexOf(item) < 0) {
            res.push(item);
        }
    }
    return res;
}

Array.prototype.removeWords = function() {
    for (const argument of arguments) {
        for (let i = this.length - 1; i >= 0; i--) {
            if (this[i] === argument) {
                this.splice(i, 1);
            }
        }
    }
    return this;
}

Array.prototype.removeByProp = function(prop, value) {
    for (let i = this.length - 1; i >= 0; i--) {
        if (this[i][prop] === value) {
            this.splice(i, 1);
        }
    }
    return this;
}

Array.prototype.hasName = function(name) {
    for (const item of this) {
        if (!isNullOrUndefined(item.name) && item.name === name) {
            return true;
        }
    }
    return false;
}

function isNullOrUndefined(p) {
    return p === null || typeof p === 'undefined';
}

Array.prototype.byName = function(name) {
    return this.find(item => !isNullOrUndefined(item.name) && item.name === name) || null;
}

Array.prototype.byNameType = function(name, type) {
    return this.find(item => !isNullOrUndefined(item.name) && item.name === name && !isNullOrUndefined(item.type) && item.type === type) || null;
}

Array.prototype.byPropValue = function(prop, value) {
    return this.find(item => !isNullOrUndefined(item.name) && item[prop] === value) || null;
}

Array.prototype.selected = function() {
    let posAll = 0;
    let pos = 0;
    let neg = 0;
    for (const argument of arguments) {
        let name = argument;
        if (isNullOrUndefined(name))
            continue;
        let check = false;
        if (name.startsWith('!')) {
            check = true;
            name = name.substring(1);
        } else {
            posAll++;
        }
        for (const item of this) {
            if (typeof item.name !== 'undefined' && item.name === name && item.selected && item.visible) {
                check ? neg++ : pos++;
                break;
            }
        }
    }
    return pos === posAll && neg === 0;
}

Array.prototype.select = function() {
    let res = 0;
    const val = arguments.length > 0 ? arguments[arguments.length - 1] : false;
    for (let a = 0; a < arguments.length - 1; a++) {
        const name = arguments[a];
        for (const item of this) {
            if (name === _ALL_ || typeof item.name !== 'undefined' && item.name === name) {
                item.selected = val;
                res++;
            }
        }
    }
    return res;
}

Array.prototype.selectedNames = function() {
    let selected = [];
    for (const item of this) {
        if (typeof item.name !== 'undefined' && item.selected) {
            selected.push(item.name);
        }
    }
    return selected;
}

Array.prototype.getNextPosForChartType = function(name, type) {
    let i = 0;
    while (i < this.length && !isChart(this[i].type)) {
        i++;
    }
    if (i === this.length) {
        return 0;
    }
    if (type === TYPE_CHART_GROUP && name.startsWith('primary')) {
        while (i < this.length && isChart(this[i].type) && this[i].type !== TYPE_CHART_GROUP) {
            i++;
        }
    } else {
        while (i < this.length && isChart(this[i].type) && getNormedType(type) >= getNormedType(this[i].type)) {
            i++;
        }
    }
    return i;
}

Array.prototype.getNextPosForValueType = function(type) {
    let pos = -1;
    for (let i = 0; i < this.length; i++) {
        if (isValue(type) && isValue(this[i].type)) {
            pos = i;
        }
    }
    if (pos >= 0) {
        return pos + 1;
    }
    return this.length;
}

Array.prototype.getNextPos = function(name, type) {
    return isChart(type) ? this.getNextPosForChartType(name, type) : this.getNextPosForValueType(type);
}

Array.prototype.addToggleOpt = function(name, type, showDataElements, style, hidden) {
    let elem = this.byNameType(name, type);
    if (!elem) {
        let pos = this.getNextPos(name, type);
        ///console.log(`addToggleOpt [${name}] [${type}]`);
        elem = { name, type, style, selected: showData(name, type, showDataElements), visible: !hidden };
        this.splice(pos, 0, elem);
    }
    return elem;
}

Array.prototype.addToggle = function(name, type, selected, hidden) {
    if (!this.hasName(name)) {
        let pos = this.getNextPos(name, type);
        this.splice(pos, 0, { name, type, selected, visible: !hidden });
    }
}

Array.prototype.equallsSelectedTo = function(that) {
    const set = new Set();
    let naCount = 0;
    this.filter(elem => elem.selected).forEach(elem => set.add(elem.name));
    that.filter(elem => elem.selected).forEach(elem => {
        if (set.has(elem.name)) {
            set.delete(elem.name);
        } else {
            naCount++;
        }
    });
    return naCount === 0 && set.size === 0;
}

Array.prototype.joinSelected = function(prop, sep, fn) {
    let props = '';
    for (const item of this) {
        if (item.selected) {
            if (sep !== undefined && sep !== null && props.length > 0) {
                props += sep;
            }
            if (item[prop]) {
                if (fn) {
                    props += fn(item[prop]);
                } else {
                    props += item[prop];
                }
            }
        }
    }
    return props;
}

Array.prototype.joinProps = function(prop, sep) {
    let props = '';
    for (const item of this) {
        if (sep !== undefined && sep !== null && props.length > 0) {
            props += sep;
        }
        if (item[prop]) {
            props += item[prop];
        }
    }
    return props;
}

Array.prototype.fixAllSelected = function() {
    for (const tag of this) {
        if (!tag.selected) {
            if (this.allSelected)
                this.allSelected = false;
            return;
        }
    }
    if (this.length > 0 && !this.allSelected) {
        this.allSelected = true;
    }
}

Array.prototype.applyAllSelected = function(onChange) {
    let changed = 0;
    for (const tag of this) {
        if (tag.selected !== this.allSelected) {
            tag.selected = this.allSelected;
            changed++;
        }
    }
    if (changed > 0 && onChange) {
        onChange();
    }
};

// String proto

String.prototype.removeDups = function(sep) {
    return this.split(sep).removeDups().join(sep).trim();
}

String.prototype.removeWords = function(...words) {
    return this.split(' ').removeWords(...words).join(' ').trim();
}

String.prototype.hasWord = function(word) {
    return this.split(' ').includes(word);
}

String.prototype.capitalize = function() {
    if (this == 'os') {
        return 'OS';
    }
    if (this == 'jvm') {
        return 'JVM';
    }
    if (this.length > 0) {
        return this.substring(0, 1).toUpperCase() + this.substring(1);
    } else {
        return '';
    }
}

String.prototype.hideVersion = function() {
    const pos = this.indexOf('-');
    if (this.length > 0 && pos > 0) {
        return this.substring(0, pos);
    } else {
        return this;
    }
}

String.prototype.getVersion = function() {
    const pos = this.indexOf('-');
    if (this.length > 0 && pos > 0) {
        return this.substring(pos + 1);
    } else {
        return this;
    }
}

// Charts

const linePlugin = {
    id: 'ext',
    getLinePosition: function(chart, dataIndex) {
        // first dataset is used to discover X coordinate of a point
        const meta = chart.getDatasetMeta(dataIndex);
        const data = meta.data;
        return data[dataIndex] ? data[dataIndex]._model : null;
    },
    getLineColor: function(chart, dataIndex) {
        const meta = typeof dataIndex === 'undefined' ? null : chart.getDatasetMeta(dataIndex);
        return meta ? meta.dataset._model.borderColor : '#f00080';
    },
    renderVerticalLine: function(chart, point, color) {
        const model = this.getLinePosition(chart, 0);
        if (!model) return;
        const xscale = chart.scales['x-axis-0'];
        const yscale = chart.scales['y-axis-0'];
        const context = chart.chart.ctx;
        // TBD const lineCol = point.color ? point.color : color;
        const lineCol = color;
        const xLeft = xscale.left + xscale.paddingLeft;
        const xRight = xscale.right;
        const lineLeftOffset = xLeft + (xRight - xLeft) * (point.x - xscale.min) / (xscale.max - xscale.min);
        // render vertical line
        context.beginPath();
        context.strokeStyle = lineCol;
        context.moveTo(lineLeftOffset, yscale.top + 20);
        context.lineTo(lineLeftOffset, yscale.bottom);
        context.stroke();
        // write label
        if (point.label) {
            context.fillStyle = lineCol;
            context.textAlign = 'center';
            context.fillText(point.label, lineLeftOffset, yscale.top + 10);
        }
    },
    renderHorisontalLine: function(chart, point) {
        const yscale = chart.scales['y-axis-0'];
        const xscale = chart.scales['x-axis-0'];
        const context = chart.chart.ctx;
        const lineTopOffset = yscale.bottom - (yscale.bottom - yscale.top) * point.y / yscale.max;
        const col = point.color ? point.color : this.getLineColor(chart, point.dataIndex);
        // render horizontal line
        context.beginPath();
        context.strokeStyle = col;
        context.moveTo(xscale.left, lineTopOffset);
        context.lineTo(xscale.right, lineTopOffset);
        context.stroke();
        // write label
        if (point.label) {
            const x = (xscale.right - xscale.left) * (point.labelPos ? point.labelPos : 0.5) + xscale.left;
            const y = lineTopOffset - 10;
            context.textAlign = 'center';
            context.fillStyle = col;
            context.fillText(point.label, x, y);
        }
    },
    afterDatasetsDraw: function(chart, easing) {
        const datasets = chart.data.datasets;
        const options = chart.config.options;
        datasets.forEach((dataset, datasetIndex) => {
            const meta = chart.getDatasetMeta(datasetIndex);
            if (!meta.hidden && options.ibuttons) {
                if (dataset.vlines) {
                    dataset.vlines.forEach(point => {
                        const ibutton = options.ibuttons.find(b => b.name === point.name)
                        if (ibutton && ibutton.pressed) {
                            this.renderVerticalLine(chart, point, dataset.borderColor);
                        }
                    });
                }
                if (dataset.hlines) {
                    dataset.hlines.forEach(hline => {
                        const ibutton = options.ibuttons.find(b => b.name === hline.name)
                        if (ibutton && ibutton.pressed) {
                            this.renderHorisontalLine(chart, hline, dataset.borderColor);
                        }
                    });
                }
            }
        });
        if (options.vlines) {
            options.vlines.forEach(point => {
                if (options.ibuttons) {
                    const ibutton = options.ibuttons.find(b => b.name === point.name)
                    if (ibutton && ibutton.pressed) {
                        this.renderVerticalLine(chart, point, '#f08080');
                    }
                }
            });
        }
        if (options.hlines) {
            options.hlines.forEach(hline => {
                if (options.ibuttons) {
                    const ibutton = options.ibuttons.find(b => b.name === hline.name)
                    if (ibutton && ibutton.pressed) {
                        this.renderHorisontalLine(chart, hline);
                    }
                }
            });
        }
    }
};

Chart.plugins.register(linePlugin);

function getLineChartDatasetOptions(label, hlines, vlines, chartType) {
    return {
        fill: 'origin',
        type: chartType || 'line',
        borderWidth: 1,
        pointBorderColor: 'rgba(0,0,100,.5)',
        pointRadius: 1,
        label,
        vlines,
        hlines,
    }
}

function getBasicChartOptions(title, xScaleLabel, yScaleLabel, max, isLong, hideLegend) {
    if (xScaleLabel) {
        xScaleLabel += ' ->';
    }
    if (yScaleLabel) {
        yScaleLabel += ' ->';
    }
    return {
        xScaleLabel: xScaleLabel,
        maintainAspectRatio: false,
        elements: {
            point: {
                borderWidth: 0,
                radius: isLong ? 0 : 3,
            },
            line: {
                cubicInterpolationMode: 'monotone',
                borderWidth: 1, // isLong ? 1 : 1,
                fill: false
            }
        },
        title: {
            display: !!title,
            text: title
        },
        legend: {
            display: !hideLegend,
            position: 'top',
            labels: {
                boxWidth: 10
            }
        },
        scales: {
            xAxes: [{
                scaleLabel: {
                    display: !!xScaleLabel,
                    labelString: xScaleLabel
                },
                ticks: {
                    // beginAtZero: true,
                    // maxRotation: 90,
                    // minRotation: 90
                }
            }],
            yAxes: [{
                type: 'linear',
                // type: 'logarithmic',
                scaleLabel: {
                    display: !!yScaleLabel,
                    labelString: yScaleLabel,
                },
                ticks: {
                    suggestedMax: max,
                    beginAtZero: true
                }
            }],
        }
    };
}

function getChartButtons(ibuttons, hlines, vlines, showOptions, datasets) {
    if (hlines) {
        if ((showOptions.includes('avg') || showOptions.includes('Avg')) && hlines.find(h => h.name === 'avg')) {
            if (!ibuttons.find(ib => ib.name === 'avg')) {
                ibuttons.push({ name: 'avg', pressed: showOptions.includes('Avg') });
            }
        }
        if ((showOptions.includes('min') || showOptions.includes('Min')) && hlines.find(h => h.name === 'min')) {
            if (!ibuttons.find(ib => ib.name === 'min')) {
                ibuttons.push({ name: 'min', pressed: showOptions.includes('Min') });
            }
        }
        for (const name of ['p0', 'p25', 'p50', 'p75', 'p90', 'p99', 'p99.9']) {
            if ((showOptions.includes('p_all') ||
                showOptions.includes('P_all') ||
                showOptions.includes(name) ||
                showOptions.includes(name.capitalize())) &&
                hlines.find(h => h.name === name)) {
                if (!ibuttons.find(ib => ib.name === name)) {
                    ibuttons.push({ name, pressed: showOptions.includes(name.capitalize()) || showOptions.includes('P_all') });
                }
            }
        }
        if ((showOptions.includes('max') || showOptions.includes('Max')) && hlines.find(h => h.name === 'max')) {
            if (!ibuttons.find(ib => ib.name === 'max')) {
                ibuttons.push({ name: 'max', pressed: showOptions.includes('Max') });
            }
        }
        for (const buttonType of ['outliers', 'markers']) {
            if (hlines.find(h => h.name === buttonType)) {
                if (!ibuttons.find(ib => ib.name === buttonType)) {
                    ibuttons.push({ name: buttonType, pressed: showOptions.includes(buttonType) });
                }
            }
        }
    }
    if (datasets) {
        datasets.forEach(dataset => getChartButtons(ibuttons, dataset.hlines, dataset.vlines, showOptions));
        if (datasets.length >= 5) {
            if (!ibuttons.find(ib => ib.name === 'all')) {
                //
            }
        }
    }
    return ibuttons;
}

function getChartOptions(title, xScaleLabel, yScaleLabel, max, hlines, vlines, isLong, showOptions, datasets) {
    const hideLegend = showOptions.includes('nolegend');
    const options = getBasicChartOptions(title, xScaleLabel, yScaleLabel, max, isLong, hideLegend);
    const nums = showOptions.includes('nums');
    options.vlines = vlines;
    options.hlines = hlines;
    options.elements.line.fill = showOptions.includes('fill');
    options.ibuttons = [];
    getChartButtons(options.ibuttons, hlines, vlines, showOptions, datasets);
    if (xScaleLabel && options.scales.xAxes) {
        options.scales.xAxes.forEach(xAxe => xAxe.scaleLabel.labelString = nums ? 'iterations ->' : xScaleLabel);
    }
    return options;
}

function getPercentilesChartOptions(title, xScaleLabel, yScaleLabel, hd) {
    const options = getBasicChartOptions(title, xScaleLabel, yScaleLabel, null, false, false);
    if (hd && hd.p99) {
        let vlines = [];
        for (let p = 0; p < hd.p99.length; p++) {
            vlines.push({
                label: hd.p99[p],
                x: hd.p99pos[p]
            });
        }
        options.vlines = vlines;
    }
    return options;
}

function composeSimpleChartLabels(inputSize, start, factor) {
    let labels = [];
    for (let m = 0; m < inputSize; m++) {
        labels[m] = start + m * factor;
    }
    return labels;
}

function composeChartLabels(inputSize, delay, N, start) {
    let labels = [];
    if (N === -1)
        N = inputSize;
    let D = parseInt(inputSize / N);
    let n = 0;
    for (let m = 0; m < inputSize; m++) {
        if ((m % D) === 0 || m === inputSize - 1) {
            labels[n] = ((start + n * delay * D) / 60).toFixed(1);
            n++;
        }
    }
    return labels;
}

function smoothChartData(input, delay, N) {
    let output = [0];
    if (N === -1)
        N = input.length;
    if (N === input.length)
        return input;
    let D = parseInt(input.length / N);
    let n = 0;
    for (let m = 0; m < input.length; m++) {
        let val = parseInt(input[m]);
        output[n] += val;
        if (m > 0 && (m % D) === 0 || m === input.length - 1) {
            output[n] = (output[n] / D).toFixed(0);
            n++;
            if (m < input.length - 1) {
                output[n] = 0;
            }
        }
    }
    return output;
}

function maxChartData(input, delay, N) {
    let output = [0];
    if (N === -1)
        N = input.length;
    if (N === input.length)
        return input;
    let D = parseInt(input.length / N);
    let n = 0;
    for (let m = 0; m < input.length; m++) {
        let val = parseInt(input[m]);
        if (output[n] < val)
            output[n] = val;
        if (m > 0 && (m % D) === 0 || m === input.length - 1) {
            n++;
            if (m < input.length - 1) {
                output[n] = 0;
            }
        }
    }
    return output;
}

function getPercentileValue(percentileNames, percentileValues, percentile) {
    percentile = parseFloat(percentile);
    if (percentileNames && percentileValues) {
        const pfactor = percentileNames[percentileNames.length - 1] > 1 ? 1 : 100;
        for (let i = 0; i < percentileNames.length; i++) {
            if (percentileNames[i] * pfactor >= percentile) {
                return percentileValues[i];
            }
        }
    }
    return null;
}

function getMetricPercentileValue(metric, percentile) {
    const percentileNames = metric.getValues('percentile_names');
    const percentileValues = metric.getValues('percentile_values');
    return getPercentileValue(percentileNames, percentileValues, percentile);
}

function getMetricLatencyPercentileValue(metric, percentile) {
    const percentileNames = metric.getValues('latency_percentile_names');
    const percentileValues = metric.getValues('latency_percentile_values');
    return getPercentileValue(percentileNames, percentileValues, percentile);
}

function getMetricsNameSuffix(metric, scale, mname) {
    let name = metric.name;
    name = name.replace(/times/g, 'time');
    if (mname) {
        name = name.replace(/response_time/g, mname).replace(/service_time/g, mname);
    }
    if (scale && metric.units) {
        name += ' (' + metric.units + ')';
    }
    return name;
}

function getMetricNameWithOperation(metric, scale, mname) {
    let name = '';
    if (metric.operation) {
        name += metric.operation + ' ';
    }
    name += getMetricsNameSuffix(metric, scale, mname);
    return name;
}

function getMetricsShortName(metric, scale, mname) {
    let name = (mname || metric.name).replace(/_/g, ' ').replace(/times/g, 'time');
    if (scale && metric.units) {
        name += ' (' + metric.units + ')';
    }
    return name;
}

function getMetricLabel(metric, scale, mname) {
    let label = '';
    if (metric.host && (!metric.host.startsWith('driver') || metric.name === 'hiccup_times')) {
        label = metric.host + ' ';
    }
    label += getMetricNameWithOperation(metric, scale, mname);
    return label;
}

function convertShowArg(name) {
    name = name.replace(/ /g, '_').replace(/%/g, 'percent');
    while (name.indexOf('__') >= 0) {
        name = name.replace(/__/g, '_');
    }
    name = name.toLowerCase();
    return name;
}

const UNSELECTED_BY_DEFAULT = [
    'id', 'min', 'max', 'diff_col', 'diff_x', 'diff_raw',
    'var', 'sum', 'best', 'worst', 'values', 'value', 'geom',
    'total_values', 'total_errors', 'error_rate',
    'properties', 'results', 'target_rate',
    'top', 'disk', 'mpstat', 'network', 'rx', 'tx',
    'hiccups', 'hiccup_times'
];

const SELECTED_BY_DEFAULT = [
    'p50', 'build', 'app', 'avg'
];

const PRIME_METRICS = [
    'response_time', 'service_time', 'counts', 'rate'
];

const SECONDARY_METRICS = [
    'hiccups', 
    'hiccup_times',
    'top',
    'top_threads',
    'disk',
    'diskstat',
    'network',
    'netstat',
    'mpstat',
    'cpu_utilization',
];

function isPrimaryMetric(name) {
    if (name.indexOf('_hdr') >= 0) {
        return false;
    }
    return !!PRIME_METRICS.find(p => name.indexOf(p) >= 0) && !(name.indexOf('-err') >= 0 || name.indexOf('tlp-') >= 0 || name.indexOf('error_rate') >= 0);
}

const shownByDefault = name => SELECTED_BY_DEFAULT.includes(name) || isPrimaryMetric(name);
const hiddenWhenAll = name => UNSELECTED_BY_DEFAULT.includes(name) || name.indexOf('hdr') >= 0 || name.indexOf('-err') >= 0 || name.indexOf('tlp-') >= 0;   

function checkDataPatterns(names, showDataElements, from, type) {
    for (const p of showDataElements) {
        let pattern = p;
        let ret = true;
        if (pattern.startsWith('!')) {
            pattern = pattern.substring(1); 
            ret = false;
        }
        let forChart = false;
        if (pattern.endsWith('_chart')) {
            pattern = pattern.substring(0, pattern.length - 6);
            forChart = true;
        } else if (pattern.endsWith('_(chart)')) {
            pattern = pattern.substring(0, pattern.length - 8);
            forChart = true;
        }
        if (forChart !== isChart(type)) {
            continue;
        }
        if (names.find(name => pattern === name)) {
            console.log(`[checkDataPatterns] ${from} ${names} = ${pattern}: exact -> ${ret}`);
            return ret;
        }
        if (pattern.indexOf('*') >= 0 || pattern.indexOf('^') >= 0 || pattern.indexOf('$') >= 0 || pattern.indexOf('?') >= 0) {
            try {
                const regexp = new RegExp(pattern, 'ig');
                for (const name of names) {
                    if (name.indexOf('_hdr') >= 0 && pattern.indexOf('_hdr') < 0) {
                        continue;
                    }
                    if (regexp.test(name)) {
                        console.log(`[checkDataPatterns] ${from} ${names} ~ ${pattern}: regexp match > ${ret}`);
                        return ret;
                    }
                }
            } catch (e) {
                // ignore bas regexp here
            }
        }
    }
    return false;
}

function showData(name, type, showDataElements) {
    name = name.toLowerCase().replace(/ /g, '_');
    let res = false;
    if (!showDataElements || showDataElements.length === 0) {
        res = shownByDefault(name);
        console.log(`[showData] name="${name}" type="${type}" => shownByDefault ${res}`);
        return res;
    }
    if (showDataElements.includes('ALL')) {
        res = true;
        console.log(`[showData] name="${name}" type="${type}" => ALL ${res}`);
        return res;
    }
    if (showDataElements.includes(name)) {
        res = true;
        console.log(`[showData] name="${name}" type="${type}" => includes exact name ${res}`);
        return res;
    }
    if (showDataElements.includes('all')) {
        res = !hiddenWhenAll(name);
        console.log(`[showData] name="${name}" type="${type}" => all - !hiddenWhenAll ${res}`);
        return res;
    }
    if ((showDataElements.includes('allcharts') || showDataElements.includes('charts')) && isChart(type)) {
        res = true;
        console.log(`[showData] name="${name}" type="${type}" => isChart ${res}`);
        return res;
    }
    if (showDataElements.includes('allvalues')) {
        if (isValue(type)) {
            res = true;
            console.log(`[showData] name="${name}" type="${type}" => isValue ${res}`);
            return res;
        } else if (isHeader(type)) {
            res = !hiddenWhenAll(name);
            console.log(`[showData] name="${name}" type="${type}" => isHeader ${res}`);
            return res;
        }
    }
    name = convertShowArg(name);
    if (name === 'value') {
        res = true;
        console.log(`[showData] name="${name}" type="${type}" => isHeader ${res}`);
        return res;
    }
    res = checkDataPatterns([name], showDataElements, 'SHOW', type);
    console.log(`[showData] name="${name}" type="${type}" => checkDataPatterns ${showDataElements.join()} ${res}`);
    return res;
}

function equalXvalues(metric1, metric2) {
    if (!metric1 || !metric2) {
        return false;
    }
    if (metric1 === metric2) {
        return true;
    }
    if (metric1.delay !== metric2.delay) {
        return false;
    }
    if (metric1.xvalues && metric2.xvalues) {
        const minLen = metric1.xvalues.length < metric2.xvalues.length ? metric1.xvalues.length : metric2.xvalues.length;
        for (let i = 0; i < minLen; i++) {
            if (metric1.xvalues[i] !== metric2.xvalues[i]) {
                return false;
            }
        }
    } else if (metric1.xvalues || metric2.xvalues) {
        return false;
    }
    return true;
}

function getMetricValuesLabels(metric, mvValues, showOptions) {
    const nums = showOptions.includes('nums');
    // TODO const loga = showOptions.includes('loga');
    const dateTime = showOptions.includes('dateTime');
    const counts = showOptions.includes('counts');
    const utc = showOptions.includes('utc') || showOptions.includes('UTC');
    const noMin = showOptions.includes('noMin');
    // if (N > values.length)
    const valuesLength = mvValues[0].values.length;
    const N = valuesLength;
    const delay = metric.delay / 1000;
    let offset = 0;
    if (metric.trimLeft) {
        offset = metric.delay * metric.trimLeft;
    }
    const metricLabel = getMetricLabel(metric, true) + (mvValues[0].isCounts ? (counts ? ' counts' : 'rate') : ' values');
    const numLabels = composeSimpleChartLabels(valuesLength, 1 + (metric.trimLeft ? metric.trimLeft : 0), 1);
    let metricLabels;
    let chartType;
    if (metric.xvalues && metric.xvalues.length > 0 && metric.xvalues[0] > 150000000000) {
        if (utc) {
            if (dateTime) {
                metricLabels = metric.xvalues.map(xv => new Date(xv).formatDateTimeUTC());
            } else {
                metricLabels = metric.xvalues.map(xv => new Date(xv).formatTimeOnlyUTC());
            }
        } else {
            if (dateTime) {
                metricLabels = metric.xvalues.map(xv => new Date(xv).formatDateTime());
            } else {
                metricLabels = metric.xvalues.map(xv => new Date(xv).formatTimeOnly());
            }
        }
    } else if (metric.xvalues && metric.xvalues.length > 0) {
        metricLabels = metric.xvalues;
        console.log('getMetricValuesLabels: metricLabels -> xvalues ' + metricLabels[0]);
    } else if (metric.xsvalues && metric.xsvalues.length > 0) {
        metricLabels = metric.xsvalues;
        chartType = 'bar';
        console.log('getMetricValuesLabels: metricLabels -> xsvalues ' + metricLabels[0]);
    } else if (delay && delay > 0) {
        if (noMin) {
            const stampLabels = composeSimpleChartLabels(valuesLength, metric.start + offset, metric.delay);
            if (utc) {
                if (dateTime) {
                    metricLabels = stampLabels.map(xv => new Date(xv).formatDateTimeUTC());
                } else {
                    metricLabels = stampLabels.map(xv => new Date(xv).formatTimeOnlyUTC());
                }
            } else {
                if (dateTime) {
                    metricLabels = stampLabels.map(xv => new Date(xv).formatDateTime());
                } else {
                    metricLabels = stampLabels.map(xv => new Date(xv).formatTimeOnly());
                }
            }
        } else {
            metricLabels = composeChartLabels(valuesLength, delay, N, (metric.start + offset - metric.minStart) / 1000);
        }
        console.log('getMetricValuesLabels: metricLabels -> composeChartLabels ' + metricLabels[0]);
    } else {
        metricLabels = numLabels;
        console.log('getMetricValuesLabels: metricLabels -> numLabels ' + metricLabels[0]);
    }
    const mlabels = {
        // nums,
        // loga,
        numLabels,
        chartType,
        metricLabels,
        labels: (nums ? numLabels : metricLabels),
    };
    mlabels.toggleLabels = (chart) => {
        if (chart.options.nums) {
            chart.labels = mlabels.numLabels;
            if (chart.options.xScaleLabel && chart.options.scales.xAxes) {
                chart.options.scales.xAxes.forEach(xAxe => xAxe.scaleLabel.labelString = 'iterations ->')
            }
        } else {
            chart.labels = mlabels.metricLabels;
            if (chart.options.xScaleLabel && chart.options.scales.xAxes) {
                chart.options.scales.xAxes.forEach(xAxe => xAxe.scaleLabel.labelString = chart.options.xScaleLabel);
            }
        }
    }
    mlabels.toggleLoga = (chart) => {
        if (chart.options.loga) {
            if (chart.options.scales.yAxes) {
                chart.options.scales.yAxes.forEach(yAxe => yAxe.type = 'logarithmic');
            }
        } else {
            if (chart.options.scales.yAxes) {
                chart.options.scales.yAxes.forEach(yAxe => yAxe.type = 'linear');
            }
        }
    }
    mlabels.toggleCsv = (chart) => {
        chart.options.csv = false;
        const data = [];
        let s = `${metric.xunits ? metric.xunits : 'time'}`;
        mvValues.forEach(mv => s += ',' + (mv.name || metric.units));
        data.push(`${s}\n`);
        for (let i = 0; i < metricLabels.length; i++) {
            s = metricLabels[i];
            mvValues.forEach(mv => s += ',' + mv.values[i]);
            data.push(`${s}\n`);
        }
        const plain = chart.options.csvEvent ? chart.options.csvEvent.ctrlKey : null;
        const blob = new Blob(data, { type: plain ? 'text/plain' : 'text/csv' });
        if (plain) {
            window.open(URL.createObjectURL(blob), '_blank');
        } else {
            const fileName = `${metric.runProperties.benchmark} ${metricLabel}.csv`;
            const file = new File([blob], fileName, { lastModified: new Date() });
            const a = document.createElement('a');
            a.href = URL.createObjectURL(file);
            a.target = '_blank'
            a.download = fileName;
            a.click();
        }
    }
    return mlabels;
}

function getHLines(metric, dataIndex) {
    console.log('getHLines ' + getMetricLabel(metric, true));
    if (!metric) {
        return [];
    }
    let hlines = [];
    let offs = 0.01;
    if (metric.avg_value) {
        hlines.push({ name: 'avg', label: 'avg: ' + roundNumber(metric.avg_value), y: metric.avg_value, labelPos: offs, dataIndex });
        offs += 0.1;
    }
    if (metric.min_value) {
        hlines.push({ name: 'min', label: 'min: ' + roundNumber(metric.min_value), y: metric.min_value, labelPos: offs, dataIndex });
        offs += 0.1;
    }
    if (metric.normed) {
        for (const name of ['p0', 'p25', 'p50', 'p75', 'p90', 'p99', 'p99.9']) {
            if (metric.normed[name]) {
                hlines.push({ name, label: name + ': ' + roundNumber(metric.normed[name]), y: metric.normed[name], labelPos: offs, dataIndex });
                offs += 0.1;
            }
        }
    }
    if (metric.max_value) {
        hlines.push({ name: 'max', label: 'max: ' + roundNumber(metric.max_value), y: metric.max_value, labelPos: offs, dataIndex });
        offs += 0.1;
    }
    if (metric.outliers_names) {
        for (const oname of metric.outliers_names) {
            hlines.push({ name: 'outliers', label: oname, y: oname, labelPos: offs, dataIndex });
            offs += 0.1;
        }
    }
    if (metric.ymarkers) {
        metric.ymarkers.forEach(marker => {
            hlines.push({ name: 'markers', label: marker.name, y: marker.value, color: '#800000', labelPos: offs });
            offs += 0.1;
        });
    } else if (metric.markers) {
        metric.markers.forEach(marker => {
            hlines.push({ name: 'markers', label: marker.name, y: marker.yvalue, color: '#800000', labelPos: offs });
            offs += 0.1;
        });
    }
    return hlines;
}

function getHLinesFromData(data, showOptions) {
    if (!data) {
        return;
    }
    const inclAvg = showOptions.includes('avg');
    const inclMin = showOptions.includes('min');
    const inclMax = showOptions.includes('max');
    const hlines = [];
    let offs = 0.3;
    if (inclAvg) {
        const a = data.avg();
        hlines.push({ x: 1, name: 'avg', label: 'avg: ' + roundNumber(a), y: a, color: '#800000', labelPos: offs });
        offs += 0.2;
    }
    if (inclMax) {
        const a = data.max();
        if (a) {
            hlines.push({ x: 1, name: 'max', label: 'max: ' + roundNumber(a), y: a, color: '#004040', labelPos: offs });
            offs += 0.1;
        }
    }
    if (inclMin) {
        const a = data.min();
        if (a) {
            hlines.push({ x: 1, name: 'min', label: 'min: ' + roundNumber(a), y: a, color: '#000080', labelPos: offs });
            offs += 0.1;
        }
    }
    return hlines;
}

function getVLines(metric) {
    let vlines = [];
    /*    if (metric.xmarkers) {
            metric.xmarkers.forEach(marker => vlines.push({ name: 'markers', label: marker.name, x: marker.value, color: '#800000', labelPos: .5 }));
        } else if (metric.markers) {
            metric.markers.forEach(marker => vlines.push({ name: 'markers', label: marker.name, x: marker.xvalue, color: '#800000', labelPos: .5 }));
        }*/
    return vlines;
}

function getMetricValuesChart(metric, showOptions) {
    console.log(`getMetricValuesChart ${getMetricLabel(metric)} hasValues=${metric.hasValues()}`);
    if (!metric.hasValues()) {
        return;
    }
    const wide = showOptions.includes('wide');
    const labelsSet = getMetricValuesLabels(metric, metric.collectValues(), showOptions);
    const data = [];
    const datasets = [];
    metric.metricValues.forEach(mv => {
        if (mv.isValues) {
            data.push(mv.values);
            datasets.push(getLineChartDatasetOptions(mv.name, getHLinesFromData(mv.values, showOptions), null, labelsSet.chartType));
        }
    });
    const isLong = data.length > 0 && data[0].length > 100;
    const hlines = [];
    if (metric.ymarkers) {
        let offs = 0.3;
        metric.ymarkers.forEach(marker => {
            hlines.push({ name: 'markers', label: marker.name, y: marker.value, color: '#800000', labelPos: offs });
            offs += 0.1;
        });
    } else if (metric.markers) {
        let offs = 0.3;
        metric.markers.forEach(marker => {
            hlines.push({ name: 'markers', label: marker.name, y: marker.yvalue, color: '#800000', labelPos: offs });
            offs += 0.1;
        });
    }
    const options = getChartOptions(getMetricLabel(metric), metric.xunits, metric.units, metric.maxValue, hlines, getVLines(metric), isLong, showOptions, datasets)
    console.log(`getMetricValuesChart wide=${wide} labelsSet=${labelsSet}`);
    return {
        wide,
        data,
        options,
        datasets,
        labelsSet,
        labels: labelsSet.labels
    };
}

function getMetricChart(metric, showOptions, mv, valMax, label, units) {    
    if (!mv) {
        return null;
    }
    const wide = showOptions.includes('wide');
    const isLong = mv.values.length > 100;
    const labelsSet = getMetricValuesLabels(metric, [mv], showOptions);
    const data = [mv.values];
    const datasets = [getLineChartDatasetOptions(label)];
    const hlines = getHLinesFromData(mv.values, showOptions);
    const vlines = getVLines(metric);
    return {
        wide,
        data,
        datasets,
        labelsSet,
        labels: labelsSet.labels,
        options: getChartOptions(metric.operation, '', units, valMax, hlines, vlines, isLong, showOptions)
    }
}

function getMetricThroughputChart(metric, showOptions) {
   return getMetricChart(metric, showOptions, metric.getMetricValues('throughput'), metric.maxRate, 'Rate', 'op/s');
}

function getMetricCountChart(metric, showOptions) {
    return getMetricChart(metric, showOptions, metric.getMetricValues('counts'), metric.maxCount, 'Counts', metric.delayS !== 1 ? `op/${metric.delayS}s` : 'op/s');
}

function getMetricPercentilesChart(metric, showOptions) {
    const percentileNames = metric.getValues('percentile_names');
    const percentileValues = metric.getValues('percentile_values');
    const latencyPercentileValues = metric.getValues('latency_percentile_values');
    if (!percentileNames || !percentileValues) {
        return null;
    }
    const commonLabel = getMetricsShortName(metric) + ' HDR';
    const wide = showOptions.includes('wide');
    const labels = percentileNames;
    const data = [percentileValues];
    const vlines = getVLines(metric);
    const datasets = [getLineChartDatasetOptions(commonLabel)];
    if (latencyPercentileValues) {
        data.push(latencyPercentileValues);
        datasets.push(getLineChartDatasetOptions(commonLabel + ' latency'));
    }
    return {
        wide,
        data,
        labels,
        datasets,
        options: getChartOptions(getMetricLabel(metric, true), '', '', metric.maxValue, [], vlines, false, [])
    }
}

function getMetricPercentilesCountChart(metric, showOptions) {
    const percentileNames = metric.getValues('percentile_names');
    const percentileCounts = metric.getValues('percentile_counts');
    const latencyPercentileCounts = metric.getValues('latency_percentile_counts');
    if (!percentileNames || !percentileCounts) {
        return null;
    }
    const valMax = null;
    const wide = showOptions.includes('wide');
    const labels = percentileNames;
    const data = [percentileCounts];
    const vlines = getVLines(metric);
    const datasets = [getLineChartDatasetOptions('HDR Counts')];
    if (latencyPercentileCounts) {
        data.push(latencyPercentileCounts);
        datasets.push(getLineChartDatasetOptions('HDR Counts'));
    }
    return {
        wide,
        data,
        labels,
        datasets,
        options: getChartOptions(getMetricLabel(metric, true), '', '', valMax, [], vlines, false, [])
    }
}

function getMetricProps(schedule, operation) {
    if (!schedule)
        return 'N/A';
    for (const props of schedule) {
        if (props.parallel) {
            for (const ptask of props.parallel.tasks) {
                if (ptask.operation === operation) {
                    return getMetricProps(props.parallel.tasks, operation);
                }
            }
        } else if (props.operation === operation) {
            let s = '';
            for (let prop in props) {
                if (prop === 'operation') {
                    continue;
                }
                if (s.length > 0)
                    s += ' ';
                s += prop + '=' + props[prop];
            }
            return s;
        }
    }
    return 'N/A';
}

function getVMName(vmType, config) {
    let vm_type = vmType || '';
    vm_type = vm_type.toLowerCase();
    if (vm_type === 'zing' || vm_type === 'falcon' || vm_type === 'cc2') {
        let ret = vm_type === 'cc2' ? 'Zing-C2' : 'Zing';
        if (config.hasWord('cc2') && vm_type.indexOf('-C2') < 0) {
            ret += '-C2';
        }
        if (config.hasWord('profile-in')) {
            ret += '-P-IN';
        }
        if (config.hasWord('profile-out')) {
            ret += '-P-OUT';
        }
        if (config.hasWord('bee')) {
            ret += '-BEE';
        }
        return ret;
    } else if (vm_type.indexOf('openjdk') === 0 || vm_type.indexOf('hotspot') === 0 || vm_type.indexOf('zulu') === 0) {
        let ret = vm_type === 'openjdk' ? 'OpenJDK' : vmType;
        if (config.hasWord('g1')) {
            ret += '-G1';
        } else if (config.hasWord('cms')) {
            ret += '-CMS';
        } else if (config.hasWord('zgc')) {
            ret += '-ZGC';
        } else if (config.hasWord('shenandoah')) {
            ret += '-SH';
        } else if (config.hasWord('pargc') || config.hasWord('pgc')) {
            ret += '-PGC';
        }
        return ret;
    } else {
        return vmType;
    }
}

function normalizeWorkloadParams(par) {
    let ret = [];
    par.split('+').forEach(step => ret.push(step.split(',').sort().join(' ')));
    return ret.join(';');
}

function normalizeVM(vm, build, vm_version) {
    let ret = vm.replace(/zing-dolphin/g, 'falcon')
        .replace(/zing-c2/g, 'cc2')
        .replace(/oracle-release/g, 'hotspot')
        .replace(/oracle/g, 'hotspot');
    if (vm_version) {
        ret += vm_version;
    } else if (build && build.indexOf('11.') === 0) {
        ret += '11';
    }
    return ret;
}

function normalizeAppName(runProperties) {
    if (!runProperties.application_name) {
        if (runProperties.application) {
            let pos = runProperties.application.indexOf('-');
            if (pos < 0) {
                pos = runProperties.application.indexOf('_');
            }
            if (pos > 0) {
                runProperties.application_name = runProperties.application.substring(0, pos);
                runProperties.application_version = runProperties.application.substring(pos + 1).replace(/_/g, '.');
            } else {
                runProperties.application_name = runProperties.application;
            }
        } else if (runProperties.benchmark.startsWith('esrally')) {
            runProperties.application_name = 'elasticsearch';
        } else {
            runProperties.application_name = runProperties.benchmark.substring(0, runProperties.benchmark.indexOf('_'));
        }
    }
    runProperties.application_name = runProperties.application_name.toLowerCase();
    if (!runProperties.application_version) {
        if (runProperties.benchmark.indexOf('-') >= 0) {
            runProperties.application_version = runProperties.benchmark.substring(runProperties.benchmark.indexOf('-') + 1);
        } else if (runProperties.benchmark.indexOf('_') >= 0) {
            runProperties.application_version = runProperties.benchmark.substring(runProperties.benchmark.indexOf('_') + 1).replace(/_/g, '.');
        }
    }
    if (!runProperties.application) {
        runProperties.application = runProperties.application_name;
        if (!runProperties.application) {
            runProperties.application = '';
        } else if (runProperties.application_version) {
            runProperties.application += '-' + runProperties.application_version;
        }
    }
    runProperties.application = runProperties.application.toLowerCase();
    if (runProperties.benchmark.startsWith('esrally_')) {
        runProperties.benchmark_name = 'rally-' + runProperties.benchmark.substring('esrally_'.length).replace(/_/g, '.');
    } else {
        runProperties.benchmark_name = runProperties.benchmark.replace(/_/, '-').replace(/_/g, '.');
    }
    runProperties.benchmark_name = runProperties.benchmark_name.toLowerCase();
}

function makeShorter(arr, len) {
    if (!arr || !len || len >= arr.length)
        return;
    arr.splice(len, arr.length - len);
}

function trimArray(arr, left, right) {
    if (!arr || !(left || right) || left + right >= arr.length)
        return;
    arr.splice(arr.length - right, right);
    arr.splice(0, left);
}

function normedVal(values, perc, prop, res) {
    const idx = parseInt(values.length * perc);
    if (idx < 0 || idx >= values.length) {
        console.log(`false value idx: ${idx} perc: ${perc}`);
        return false;
    }
    const p = values[idx];
    if (!p || p < 0) {
        console.log(`false value p: ${p} at idx: ${idx} perc: ${perc}`);
        return false;
    }
    res[prop] = p;
    return true;
}

function makeNormedArray(values) {
    if (!values || !values.length) {
        return;
    }
    const res = {};
    if (!normedVal(values, 0, 'p0', res)) {
        return;
    }
    const normedValues = values.map(v => v / res.p0 - 1);
    return { normedValues };
}

function makeNormedPercentiles(values, res) {
    if (!values || !values.length || !res) {
        return null;
    }
    normedVal(values, 0, 'p0', res);
    normedVal(values, 1 / 4, 'p25', res);
    normedVal(values, 1 / 2, 'p50', res);
    normedVal(values, 3 / 4, 'p75', res);
    normedVal(values, 9 / 10, 'p90', res);
    normedVal(values, 99 / 100, 'p99', res);
    normedVal(values, 999 / 1000, 'p99.9', res);
}

function prepareNormedMetrics(metrics, mapMinLength) {
    metrics.forEach(metric => {
        if (metric.name === 'hiccup_times' || metric.name === 'cpu_utilization') {
            return;
        }
        const metricLabel = getMetricLabel(metric);
        const values = metric.getValues('values') || metric.getValues('p50_values');
        let vals = null;
        if (values) {
            const length = mapMinLength.get(metricLabel) ? mapMinLength.get(metricLabel) : values.length;
            vals = values.slice(0, length).sortNumbers();
        }
        if (vals && vals.length > 0) {
            metric.normed = makeNormedArray(vals);
            makeNormedPercentiles(vals, metric.normed);
        }
    });
}

function addNormedMetrics(metric, metrics, i) {
    const normed = metric.normed;
    if (normed) {
        for (const name of ['p0', 'p25', 'p50', 'p75', 'p90', 'p99', 'p99.9']) {
            if (normed[name] && normed.p0) {
                const level = normed[name] / normed.p0 - 1;
                metrics.splice(i + 1, 0, {
                    name: metric.operation ? metric.name : `${metric.name} (intergral_${name})`,
                    operation: metric.operation ? `${metric.operation} (intergral_${name})` : null,
                    scale: 'sum',
                    step: metric.step,
                    operation_step: metric.operation_step,
                    value: normed.normedValues.sum(x => x > level)
                });
            }
        }
        const normedMetrics = {
            name: metric.operation ? metric.name : metric.name + ' (normed)',
            operation: metric.operation ? metric.operation + ' (normed)' : null,
            units: metric.units,
            step: metric.step,
            operation_step: metric.operation_step,
            values: normed.normedValues,
            normed: {}
        };
        makeNormedPercentiles(normedMetrics.values, normedMetrics.normed);
        metrics.splice(i + 1, 0, normedMetrics);
    }
}

function addNormalizedMetrics(hits) {
    hits.forEach(hit => {
        const metrics = hit._source.metrics;
        prepareNormedMetrics(metrics, hits.mapMinLength);
        for (let i = metrics.length - 1; i >= 0; i--) {
            addNormedMetrics(metrics[i], metrics, i);
        }
    });
}

function fixMetricFields(metric) {
    metric.name = metric.name || '';
    metric.targetRate = metric.targetRate || metric.target_rate;
    metric.actualRate = metric.actualRate || metric.actual_rate;
    metric.totalValues = metric.totalValues || metric.total_values;
    metric.totalErrors = metric.totalErrors || metric.total_errors;
    metric.highBound = metric.highBound || metric.high_bound;
    metric.units = metric.units || metric.scale; // scale == units
    metric.xunits = metric.xunits || metric.xscale; // xscale == xunits
    if (metric.targetRate) {
        metric.operation = metric.operation || 'op';
        if (metric.percentOfHighBound) {
            metric.operation += '_' + parseInt(metric.percentOfHighBound);
        }
        metric.operation += '_' + parseInt(metric.targetRate);
        if (metric.retry) {
            metric.operation += '_' + parseInt(metric.retry);
        }
    }
    if (!metric.step || metric.step < 0) {
        metric.step = 0;
    }
    metric.delayS = metric.delay > 0 && metric.delay !== 1000 ? metric.delay / 1000 : 1;
    if (metric.min_values && !metric.p0_values) {
        metric.p0_values = metric.min_values;
        metric.min_values = undefined;
    }
    if (metric.max_values && !metric.p100_values) {
        metric.p100_values = metric.max_values;
        metric.max_values = undefined;
    }
    if (metric.p90_values && metric.values && !metric.p50_values) {
        metric.p50_values = metric.values;
        metric.values = undefined;
    }
}

function fixMetricValues(metric, showOpts) {
    console.log(`fixMetricValues: fixing metric ${metric.name} ${metric.operation}`);
    const delay = metric.delay / 1000;
    metric.metricValues = metric.metricValues || [];
    let mvRate = null;
    for (const type of ['values', 'p0_values', 'p50_values', 'p90_values', 'p99_values',
        'p999_values', 'p99_9_values', 'p9999_values', 'p99_99_values', 'p100_values', 'counts', 'throughput']) {
        if (metric[type]) {
            metric.metricValues.push({ type, values: metric[type] });
            metric[type] = undefined;
        }
    }
    if (metric.latencies) {
        metric.metricValues.push({ type: 'latency_values', values: metric.latencies });
        metric.latencies = undefined;        
    }
    //metric.metricValues = [metric.metricValues[0]] /// DEBUG !!!
    metric.metricValues.forEach(mv => {
        mv.type = mv.type.toLowerCase();
        if (!mv.name) {
            if (mv.type.indexOf('percentile') < 0 && mv.type.endsWith('values')) {
                if (mv.type.endsWith('_values')) {
                    mv.name = mv.type.substring(0, mv.type.length - '_values'.length).toLowerCase();
                } else {
                    mv.name = mv.type.substring(0, mv.type.length - 'values'.length).toLowerCase();
                }
                mv.name = mv.name.replace(/_/, '.');
            } else{
                mv.name = mv.type;
            }
        }
        if (mv.name === 'p999' || mv.name === 'p99_9') {
            mv.name = 'p99.9';
        } else if (mv.name === 'p9999' || mv.name === 'p99_99') {
            mv.name = 'p99.99';
        } else if (mv.name === 'p99999' || mv.name === 'p99_999') {
            mv.name = 'p99.999';
        } else if (mv.name === 'percentile_values') {
            mv.name = 'HDR';
        } else if (mv.name === 'percentile_counts') {
            mv.name = 'HDR counts';
        } else if (mv.name === 'latency_percentile_values') {
            mv.name = 'latency HDR';
        } else if (mv.name === 'latency_percentile_counts') {
            mv.name = 'latency HDR counts';
        }
        if (!mv.name) {
            mv.name = metric.name;
        }
        if (mv.type === 'counts' && showOpts.counts !== 'keep' && delay && delay > 0 && !metric.metricValues.find(mv => mv.type === 'throughput')) {
            if (showOpts.counts === 'both') {
                console.log(`fixMetricValues: adding rate in addition to ${mv.type} ${mv.values.length}...`);
                mvRate = {
                    values: [],
                    isThroughput: true,
                };
            } else {
                console.log(`fixMetricValues: normalizing rate ${mv.type} ${mv.values.length}...`);
                mvRate = mv;
            }
            for (let i = 0; i < mv.values.length; i++) {
                mvRate.values[i] = mv.values[i] / delay;
            }
            mvRate.type = 'throughput';
            if (showOpts.counts !== 'both') {
                mvRate = null;
            }
        }
        mv.isValues = mv.values && mv.values.length > 0 && mv.type.endsWith('values') && mv.type.indexOf('percentile') < 0;
        mv.isCounts = mv.values && mv.values.length > 0 && mv.type === 'counts';
        mv.isThroughput = mv.values && mv.values.length > 0 && mv.type === 'throughput';
        mv.isPercentiles = mv.values && mv.values.length > 0 && mv.type.indexOf('percentile') >= 0;
    });
    if (mvRate) {
        metric.metricValues.push(mvRate);
    }
    metric.hasValues = () => !!metric.metricValues.find(mv => mv.isValues && mv.values);
    metric.hasCounts = () => !!metric.metricValues.find(mv => mv.isCounts && mv.values);
    metric.hasThroughput = () => !!metric.metricValues.find(mv => mv.isThroughput && mv.values);
    metric.hasPercentiles = () => !!metric.metricValues.find(mv => mv.isPercentiles && mv.values);
    metric.getMetricValues = (mvName) => metric.metricValues.find(mv => mv.type === mvName);
    metric.getValues = (mvName) => {
        const mvals = metric.getMetricValues(mvName);
        return mvals ? mvals.values : null;
    }
    metric.collectValues = () => metric.metricValues.filter(mv => mv.isValues);
}

function trimMetricValues(metric, showOpts) {
    const trimLeft = showOpts.trimLeft; 
    const trimRight = showOpts.trimRight;
    const shortMetrics = showOpts.shortMetrics;
    const prime = isPrimaryMetric(metric.name);
    let shortened = false;
    if ((trimLeft > 0 || trimRight > 0) && prime) {
        metric.metricValues.forEach(mv => trimArray(mv.values, trimLeft, trimRight));
        metric.trimLeft = trimLeft;
        metric.trimRight = trimRight;
        shortened = true;
    }
    if (shortMetrics > 0 && prime) {
        metric.metricValues.forEach(mv => makeShorter(mv.values, shortMetrics));
        shortened = true;
    }
    if (shortened) {
        metric.metricValues.forEach(mv => {
            if (mv.avg_value) {
                mv.avg_value_orig = mv.avg_value;
                mv.avg_value = null;
            }
            if (mv.stdev_value) {
                mv.stdev_value_orig = mv.stdev_value;
                mv.stdev_value = null;
            }
            if (mv.max_value) {
                mv.max_value_orig = mv.max_value;
                mv.max_value = null;
            }
            if (mv.min_value) {
                mv.min_value_orig = mv.min_value;
                mv.min_value = null;
            }
        });
    }
}

function clearMetricValues(metric) {
    metric.metricValues.forEach(mv => {
        if (!mv.isPercentiles) {
            mv.values = null;
        }
    });
}

function calcMetricAverages(metric) {
    metric.metricValues.forEach(mv => {
        if (!mv.avg_value) {
            mv.avg_value = mv.values.avg();
        }
        if (!mv.stdev_value) {
            mv.stdev_value = mv.values.stdev();
        }
        if (!mv.max_value) {
            mv.max_value = mv.values.max();
        }
        if (!mv.min_value) {
            mv.min_value = mv.values.min();
        }
    });
}

function collectMetricStats(metric, mapMaxes, mapMinLength) {
    const metricLabel = getMetricLabel(metric);
    const metricLabelCounts = getMetricLabel(metric) + '_counts';
    const metricLabelRate = getMetricLabel(metric) + '_rate';
    const metricMax = maxImpl(metric.getValues('values'));
    if (metricMax && !mapMaxes.has(metricLabel) || mapMaxes.get(metricLabel) < metricMax) {
        mapMaxes.set(metricLabel, metricMax);
    }
    const metricMaxCount = maxImpl(metric.getValues('counts'));
    if (metricMaxCount && !mapMaxes.has(metricLabelCounts) || mapMaxes.get(metricLabelCounts) < metricMaxCount) {
        mapMaxes.set(metricLabelCounts, metricMaxCount);
    }
    const metricMaxRate = maxImpl(metric.getValues('throughput'));
    if (metricMaxRate && !mapMaxes.has(metricLabelRate) || mapMaxes.get(metricLabelRate) < metricMaxRate) {
        mapMaxes.set(metricLabelRate, metricMaxRate);
    }
    const values = metric.getValues('values') || metric.getValues('p50_values');
    const metricLength = values ? values.length : null;
    if (metricLength && !mapMinLength.has(metricLabel) || mapMinLength.get(metricLabel) > metricLength) {
        mapMinLength.set(metricLabel, metricLength);
    }
}

function groupMetrics(metrics, showOpts) {
    const groups = new Map();
    metrics.forEach(metric => {
        if (metric.group) {
            const grMetrics = groups.get(metric.group) || [];
            grMetrics.push(metric);
            groups.set(metric.group, grMetrics);
        }
    });
    groups.forEach((grMetrics, group) => {
        console.log(`group: ${group}`);
        const metricValues = [];
        const name = group;
        const operation = '';
        grMetrics.forEach(m => {
            metricValues.push({ name: m.operation, type: 'VALUES',  values: m.getValues('values') });
            metricValues.push({ name: m.operation, type: 'COUNTS',  values: m.getValues('counts') });
        });
        const metric = {
            name,
            operation,
            metricValues,
            ymarkers: grMetrics[0].ymarkers,
            scale: grMetrics[0].scale,
            step: grMetrics[0].step
        };
        fixMetricValues(metric, showOpts);
        metrics.push(metric);
    });
}

function normalizeMetricsStart(metrics, runProperties, filterDataElements, showOpts, mapMaxes, mapMinLength) {
    if (!metrics) {
        return;
    }
    metrics.forEach(m => m.runProperties = runProperties);
    metrics.forEach(fixMetricFields);
    metrics.forEach(metric => fixMetricValues(metric, showOpts));
    if (!showOpts.noSortMetrics) {
        console.log('Sorting metrics...')
        metrics.sortNum('name', 1, ['operation', 'operation_step']);
    }
    if (filterDataElements && filterDataElements.length > 0) {
        for (let i = metrics.length - 1; i >= 0; i--) {
            const metricLabel = getMetricLabel(metrics[i]);
            if (!checkDataPatterns([metrics[i].name, metricLabel], filterDataElements, 'FILTER', 0)) {
                console.log(`normalizeMetricsStart excluding metric ${metricLabel}`);
                metrics.splice(i, 1);
            }
        }
    }
    let minStart = MAX_LONG;
    metrics.forEach(metric => {
        trimMetricValues(metric, showOpts);
        collectMetricStats(metric, mapMaxes, mapMinLength);
        if (metric.start > 0) {
            minStart = Math.min(minStart, metric.start);
        }
        calcMetricAverages(metric);
    });
    if (minStart === MAX_LONG) {
        minStart = 0;
    }
    metrics.forEach(metric => metric.minStart = metric.start > 0 ? minStart : metric.start);
    groupMetrics(metrics, showOpts);
}

function normalizeMetricsFinish(metrics, mapMaxes, showOpts) {
    if (metrics) {
        metrics.forEach(metric => metric.maxValue = mapMaxes.get(getMetricLabel(metric)));
        metrics.forEach(metric => metric.maxCount = mapMaxes.get(getMetricLabel(metric) + '_counts'));
        metrics.forEach(metric => metric.maxRate = mapMaxes.get(getMetricLabel(metric) + '_rate'));
        if (showOpts.noCharts) {
            console.log('No charts mode');
            metrics.forEach(clearMetricValues);
        }
    }
}

function parseShowOpts(showOptions) {
    const showOpts = {
        trimLeft: 0,
        trimRight: 0,
        shortMetrics: false,
        normalizeWarmup: false,
        noSortMetrics: false,
        noCharts: false,
        counts: 'rate',
    };
    showOptions.forEach(e => {
        if (e.startsWith('chopLeft')) {
            showOpts.trimLeft = parseInt(e.substring(8));
        } else if (e.startsWith('chopRight')) {
            showOpts.trimRight = parseInt(e.substring(9));
        } else if (e.startsWith('chop')) {
            showOpts.trimLeft = showOpts.trimRight = parseInt(e.substring(4));
        } else if (e.startsWith('shorts')) {
            showOpts.shortMetrics = true;
        } else if (e.startsWith('normed')) {
            showOpts.normalizeWarmup = true;
        } else if (e.startsWith('noCharts') || e.startsWith('nocharts')) {
            showOpts.noCharts = true;
        } else if (e.startsWith('noSort') || e.startsWith('nosort')) {
            showOpts.noSortMetrics = true;
        } else if (e.startsWith('counts')) {
            showOpts.counts = e.substring(6).toLowerCase();
        }
    });
    return showOpts;
}

function normalizeHitsMetrics(hits, showOptions, filterDataElements) {
    console.log('normalizeHitsMetrics showOptions: ' + showOptions + ', ' + filterDataElements);
    const showOpts = parseShowOpts(showOptions);
    console.log(`normalizeHitsMetrics showOpts=${showOpts}`);
    hits.mapMaxes = new Map();
    hits.mapMinLength = new Map();
    //hits.forEach(hit => hit._source.metrics = [hit._source.metrics[0],hit._source.metrics[1]]); /// DEBUG !!!
    hits.forEach(hit => normalizeMetricsStart(hit._source.metrics, hit._source.runProperties, filterDataElements, showOpts, hits.mapMaxes, hits.mapMinLength));
    hits.forEach(hit => normalizeMetricsFinish(hit._source.metrics, hits.mapMaxes, showOpts));
    if (showOpts.normalizeWarmup) {
        addNormalizedMetrics(hits);
        hits.hasNormed = true;
    }
}

function normalizeRunProperties(doc) {
    if (!doc) {
        return;
    }
    if (!doc.runProperties && doc.run_properties) {
        doc.runProperties = doc.run_properties;
    } else if (!doc.runProperties) {
        return;
    }
    const runProperties = doc.runProperties;
    if (runProperties.normalized)
        return;
    runProperties.normalized = true;
    if (!runProperties.vm_args)
        runProperties.vm_args = '';
    if (!runProperties.vm_type)
        runProperties.vm_type = '';
    if (!runProperties.build)
        runProperties.build = '';
    if (!runProperties.vm_version)
        runProperties.vm_version = '';
    if (!runProperties.vm_name)
        runProperties.vm_name = 'VM!';
    if (!runProperties.config)
        runProperties.config = '';
    if (!runProperties.workload_parameters)
        runProperties.workload_parameters = '';
    if (!runProperties.benchmark)
        runProperties.benchmark = '';
    normalizeAppName(runProperties);
    const vm_args = runProperties.vm_args;
    const vm_type_init = runProperties.vm_type;
    if (vm_args.indexOf('-XX:+UseG1GC') >= 0) {
        runProperties.config += ' g1';
    }
    if (vm_args.indexOf('-XX:+UseConcMarkSweepGC') >= 0) {
        runProperties.config += ' cms';
    }
    runProperties.vm_type = normalizeVM(runProperties.vm_type, runProperties.build, runProperties.vm_version);
    runProperties.vm_name = getVMName(runProperties.vm_type, runProperties.config);
    runProperties.workload_parameters = normalizeWorkloadParams(runProperties.workload_parameters);
    runProperties.config = runProperties.config.removeWords(vm_type_init, runProperties.vm_type);
    if (runProperties.vm_type === 'hotspot' || runProperties.vm_type === 'zulu' || runProperties.vm_type === 'openjdk') {
        runProperties.config = runProperties.config.removeWords('g1', 'cms', 'zgc', 'shenandoah');
    }
    runProperties.config = runProperties.config.removeWords('loggc_details', 'ycsb_version0.14.0');
    runProperties.config = runProperties.config.removeDups(' ');
    runProperties.config = runProperties.config.replace('-XX:+FalconUseCompileStashing', 'use-CS').replace('-XX:-FalconUseCompileStashing', 'no-CS');
    const confs = runProperties.config.split(' ')
    const hosts = [runProperties.host];
    for (let i = 0; i < confs.length; i++) {
        if (confs[i] === 'custom') {
            confs.splice(i, confs.length - i);
            break;
        }
    }
    for (let i = confs.length - 1; i >= 0; i--) {
        let s = confs[i];
        if (s.startsWith('nodes_')) {
            confs.splice(i, 1);
            s = s.substring('nodes_'.length).split(',').forEach(h => hosts.push(h));
        } else if (s.startsWith('heap')) {
            confs.splice(i, 1);
            runProperties.heap = s.replace('heap', '') + 'G';
        } else if (s === runProperties.application || s === runProperties.application_name) {
            confs.splice(i, 1);
        } else if (s.startsWith('-XX:ProfileLogIn=')) {
            confs.splice(i, 1, 'profile-in');
        } else if (s.startsWith('-XX:ProfileLogOut=')) {
            confs.splice(i, 1, 'profile-out');
        } else if (s.startsWith('-XX:FalconObjectCacheRoot=')) {
            confs.splice(i, 1);
        } else if (s.startsWith('-XX:+UnlockExperimentalVMOptions')) {
            confs.splice(i, 1);
        }
    }
    if (!runProperties.heap) {
        runProperties.heap = '';
    }
    runProperties.config = confs.join(' ');
    runProperties.hosts = hosts.join(',');
    if (runProperties.start_time) {
        runProperties.start_time = new Date(runProperties.start_time);
    }
    if (runProperties.finish_time) {
        runProperties.finish_time = new Date(runProperties.finish_time);
    }
    let pool = '/net/nfs/mnt/pool';
    if (runProperties.results_dir && runProperties.results_dir.startsWith(pool)) {
        runProperties.results_dir = runProperties.results_dir.substring(pool.length);
    }
    if (runProperties.start_time && runProperties.finish_time) {
        runProperties.time_spent_minutes = runProperties.finish_time.getTime() - runProperties.start_time.getTime();
        runProperties.time_spent_minutes /= 60000;
    }
}

function normalizeHitsProperties(hits) {
    hits.forEach(hit => normalizeRunProperties(hit._source));
}

function availableProp(hit, prop) {
    if (!hit._source || !hit._source.runProperties) {
        return 0;
    }
    const ret = hit._source.runProperties[prop] ? 1 : 0;
    console.log(`availableProp: ${prop} = "${hit._source.runProperties[prop]}" -> ${ret} // ${typeof hit._source.runProperties[prop]}`);
    return ret;
}

function availableProps(hits, prop) {
    let n = 0;
    hits.forEach(hit => n += availableProp(hit, prop));
    return n;
}

// Module

function initModule(module) {
    module.filter('trust', ($sce) => (htmlCode) => $sce.trustAsHtml(htmlCode));
    module.filter('textDate', ($filter) => (time) => $filter('date')(time, 'yyyy MMM dd'));
    module.filter('textTime', ($filter) => (time) => $filter('date')(time, 'HH:mm:ss'));
    module.filter('textCurrency', ($filter) => (val) => $filter('currency')(val / 100, '$', 2));
    module.filter('field', ($filter) => (val, prop) => {
        if (prop === 'start_time' || prop === 'finish_time') {
            return val[prop] ? val[prop].formatDateTimeUTC() : '';
        }
        return val[prop];
    });
    const getClassFunc = $sc => () => {
        let res = $sc.bindClass ? $sc.bindClass : '';
        if (res.indexOf('toggle_separator') < 0) {
            res += ' toggle';
        }
        if ($sc.bindVar) {
            if (res.indexOf('toggle_small') >= 0)
                res += ' toggle_small_selected';
            else
                res += ' toggle_selected';
        }
        return res;
    }
    module.directive('toggle', () => {
        return {
            restrict: 'E',
            template: '<span ng-class="getClass()" ng-click="toggleX($event)">{{bindName}}</span> ',
            scope: {
                bindVar: '=',
                bindEvent: '=',
                bindClass: '@',
                bindName: '@',
                onToggle: '&'
            },
            controller: ($scope, $timeout) => {
                $scope.getClass = getClassFunc($scope);
                $scope.toggleX = (event) => {
                    $scope.bindEvent = event || null;
                    $scope.bindVar = !$scope.bindVar;
                    $timeout($scope.onToggle);
                };
            },
            link: (_scope, _element, _attrs) => {/**/}
        };
    });
    module.directive('toggleDel', () => {
        return {
            restrict: 'E',
            template: '<span><span ng-class="getClass()" ng-click="toggleX()">{{bindName}}</span><span ng-class="getDelClass()" ng-click="delX()">\u274E</span></span> ',
            scope: {
                bindVar: '=',
                bindClass: '@',
                bindName: '@',
                onToggle: '&',
                onDel: '&',
            },
            controller: ($scope, $timeout) => {
                $scope.getDelClass = () => 'toggle_del noselect';
                $scope.getClass = getClassFunc($scope);
                $scope.toggleX = () => {
                    $scope.bindVar = !$scope.bindVar;
                    $timeout($scope.onToggle);
                };
                $scope.delX = () => {
                    $timeout($scope.onDel);
                };
            },
            link: (_scope, _element, _attrs) => {/**/}
        };
    });
    module.directive('pushButton', () => {
        return {
            restrict: 'E',
            template: '<span class="{{bindClass}}" ng-click="toggleX()">{{bindName}}</span>\u200B',
            scope: {
                bindClass: '@',
                bindName: '@',
                onClick: '&'
            },
            controller: ($scope, $timeout) => $scope.toggleX = () => {
                //// console.log('button: ' + $scope.bindName + ' (' + $scope.bindClass + ')');
                $timeout(() => $scope.onClick());
            },
            link: (_scope, _element, _attrs) => {/**/}
        };
    });
    module.directive('fileRead', [() => {
        return {
            scope: {
                fileRead: '='
            },
            link: function(scope, element, _attributes) {
                element.bind('change', changeEvent => {
                    console.log('fileRead change');
                    const reader = new FileReader();
                    reader.onload = loadEvent => {
                        console.log('onload');
                        scope.$apply(() => scope.fileread = loadEvent.target.result)
                    };
                    //// reader.onload = loadEvent => console.log('onload');
                    reader.onloadend = loadEvent => console.log('onloadend');
                    reader.readAsDataURL(changeEvent.target.files[0]);
                });
            }
        }
    }]);
}
