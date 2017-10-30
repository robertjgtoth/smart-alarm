/*
 * Copyright (c) 2017 Robert Toth
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.rtoth.smartalarm.maps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * FIXME: docs
 */
public class TravelTimeEstimationTester
{
    public static void main(String[] args) throws IOException
    {
        TravelTimeEstimator maps = new TravelTimeEstimator("google-maps-api.key");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
             Stream<String> lines = in.lines())
        {
            lines.forEach(line -> {
                Optional<Long> travelTime = maps.getCurrentTravelTime(line, true);
                if (travelTime.isPresent())
                {
                    System.out.println("Travel time to " + line + ": " + Duration.ofSeconds(travelTime.get()));
                }
                else
                {
                    System.out.println("Could not determine travel time to " + line);
                }
            });
        }
    }
}
