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

import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.GeolocationApi;
import com.google.maps.PlacesApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.GeolocationPayload;
import com.google.maps.model.GeolocationResult;
import com.google.maps.model.PlacesSearchResponse;
import com.google.maps.model.PlacesSearchResult;
import com.google.maps.model.TrafficModel;
import com.google.maps.model.TravelMode;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * FIXME: docs
 */
public class TravelTimeEstimator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TravelTimeEstimator.class);

    private final GeoApiContext geoApiContext;

    public TravelTimeEstimator(String apiKeyFileLocation)
    {
        Objects.requireNonNull(apiKeyFileLocation);

        try (Stream<String> lines = Files.lines(Paths.get(apiKeyFileLocation)))
        {
            String apiKey = lines
                .filter(TravelTimeEstimator::isValidKey)
                .findFirst().orElseThrow(() ->
                new IllegalArgumentException("API Key file does not contain a valid API Key: " + apiKeyFileLocation)
            );

            this.geoApiContext = new GeoApiContext.Builder().apiKey(apiKey).build();
        }
        catch (IOException ioe)
        {
            LOGGER.error("Unable to open file containing API Key!");
            throw new IllegalStateException(ioe);
        }
    }

    private static boolean isValidKey(String value)
    {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Get the estimated current travel time to the provided location search string. The caller's current location will
     * be used as the departure location, and the first search result will be used as the destination.
     *
     * @param destinationSearchText Text search for the place to which we wish to travel.
     * @param includeTraffic Whether to include traffic in the calculation.
     *
     * @return The current estimated travel time, without or without traffic, to the provided destination in seconds.
     */
    public Optional<Long> getCurrentTravelTime(String destinationSearchText, boolean includeTraffic)
    {
        Objects.requireNonNull(destinationSearchText);

        try
        {
            Optional<PlacesSearchResult> placesSearchResult =
                performTextPlacesSearch(destinationSearchText);
            if (placesSearchResult.isPresent())
            {
                GeolocationResult userLocation =
                    GeolocationApi.geolocate(geoApiContext, new GeolocationPayload()).await();

                LOGGER.info("Using {} as user location (accuracy is {}m)",
                    userLocation.location, userLocation.accuracy);

                DirectionsResult directionsResult = DirectionsApi.newRequest(geoApiContext)
                    .mode(TravelMode.DRIVING)
                    .trafficModel(TrafficModel.BEST_GUESS)
                    .departureTime(DateTime.now())
                    .origin(userLocation.location)
                    .destination(placesSearchResult.get().geometry.location)
                    .await();

                if (directionsResult.routes.length > 0)
                {
                    if (directionsResult.routes.length > 1)
                    {
                        LOGGER.warn("Got multiple possible routes, using first one.");
                    }
                    DirectionsRoute route = directionsResult.routes[0];

                    // API specifies that legs should be an array of length 1 in single-waypoint cases, but
                    // check anyway to be safe
                    if (route.legs.length != 1)
                    {
                        LOGGER.warn("Maps API returned multiple legs... not sure what to do about that.");
                        return Optional.empty();
                    }
                    DirectionsLeg leg = route.legs[0];

                    return Optional.of(includeTraffic ? leg.durationInTraffic.inSeconds : leg.duration.inSeconds);
                }
                else
                {
                    LOGGER.warn("Unable to find a route!");
                }
            }
            else
            {
                LOGGER.warn("Unable to find any places matching '{}'", destinationSearchText);
            }
        }
        catch (ApiException | IOException | InterruptedException e)
        {
            LOGGER.warn("Failure executing API Request", e);
        }
        return Optional.empty();
    }

    private Optional<PlacesSearchResult> performTextPlacesSearch(String searchText)
    {
        Objects.requireNonNull(searchText);

        try
        {
            LOGGER.info("Executing places search for '{}'", searchText);

            PlacesSearchResponse response =
                PlacesApi.textSearchQuery(geoApiContext, searchText).await();

            LOGGER.info("Got response from API: {}", response);

            return Stream.of(response.results).findFirst();
        }
        catch (ApiException | IOException | InterruptedException e)
        {
            LOGGER.warn("Failure executing API Request", e);
            return Optional.empty();
        }
    }
}
