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

import java.util.Collection;

import org.HdrHistogram.Recorder;
import org.tussleframework.TimeRecorder;
import org.tussleframework.TussleRuntimeException;

public class HdrTimeRecorder implements TimeRecorder {
    public final Recorder serviceTimeRecorder = new Recorder(Long.MAX_VALUE, 3);
    public final Recorder responseTimeRecorder = new Recorder(Long.MAX_VALUE, 3);
    public final Recorder errorsRecorder = new Recorder(Long.MAX_VALUE, 3);

    @Override
    public void recordTimes(String operation, long startTime, long intendedStartTime, long finishTime, long count, boolean success) {
        if (success) {
            if (startTime > 0) {
                if (count == 1) {
                    serviceTimeRecorder.recordValue(finishTime - startTime);
                } else {
                    serviceTimeRecorder.recordValueWithCount(finishTime - startTime, count);
                }
            }
            if (intendedStartTime > 0) {
                if (count == 1) {
                    responseTimeRecorder.recordValue(finishTime - intendedStartTime);
                } else {
                    responseTimeRecorder.recordValueWithCount(finishTime - intendedStartTime, count);
                }
            }
        } else {
            errorsRecorder.recordValue(finishTime - intendedStartTime);
        }
    }

    @Override
    public void startRecording(String operation, String rateUnits, String timeUnits) {
        ///
    }

    @Override
    public void stopRecording() {
        ///
    }

    @Override
    public void addResults(Collection<?> results, String rateUnits, String timeUnits) {
        throw new TussleRuntimeException(getClass().getSimpleName() + " - method 'addResults' not supported");
    }
}
