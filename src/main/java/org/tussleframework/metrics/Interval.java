/*
 * Copyright (c) 2021-2022, Azul Systems
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

package org.tussleframework.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Interval {
    public long start = Long.MIN_VALUE;
    public long finish = Long.MAX_VALUE;
    public String name = "";
    public boolean adjusted;

    public boolean contains(long start, long finish) {
        return start <= finish && this.start < finish && start <= this.finish;
    }

    public static long mul(long v, long m) {
        if (v == Long.MAX_VALUE || v == Long.MIN_VALUE)
            return v;
        else
            return v * m;
    }

    public Interval(Interval i) {
        start = i.start;
        finish = i.finish;
        name = i.name;
        adjusted = i.adjusted;
    }

    public Interval scale(long m) {
        return new Interval(mul(start, m), mul(finish, m), name, adjusted);
    }

    public void adjust(long stamp) {
        if (adjusted) {
            return;
        }
        if (start != Long.MIN_VALUE) {
            start += stamp;
        }
        if (finish != Long.MAX_VALUE) {
            finish += stamp;
        }
        adjusted = true;
    }

    public void update(long s, long f) {
        update(s);
        update(f);
    }

    public void update(long stamp) {
        if (start == Long.MIN_VALUE || start > stamp)
            start = stamp;
        if (finish == Long.MAX_VALUE || finish < stamp)
            finish = stamp;
    }

    public static String toString(long stamp) {
        if (stamp == Long.MIN_VALUE) {
            return "MIN";
        }
        if (stamp == Long.MAX_VALUE) {
            return "MAX";
        }
        return stamp / 1000 + "";
    }

    @Override
    public String toString() {
        return toString(start) + "," + toString(finish) + "," + name;
    }
}
