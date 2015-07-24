/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package demo.maps.com.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import demo.maps.com.myapplication.R;


public class AutocompletePlaceAdapter extends ArrayAdapter<AutocompletePlaceAdapter.AutocompletePlace> implements Filterable {
    private LayoutInflater mInflater;

    private static final String TAG = "AutocompletePlaceAdapter";
    /**
     * Current results returned by this adapter.
     */
    private ArrayList<AutocompletePlace> mResultList;

    /**
     * Handles autocomplete requests.
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * The bounds used for Places Geo Data autocomplete API requests.
     */
    private LatLngBounds mBounds;

    /**
     * The autocomplete filter used to restrict queries to a specific set of place types.
     */
    private AutocompleteFilter mPlaceFilter;

    /**
     * Initializes with a resource for text rows and autocomplete query bounds.
     *
     * @see android.widget.ArrayAdapter#ArrayAdapter(android.content.Context, int)
     */

    public AutocompletePlaceAdapter(Context mContext, int resource, ArrayList<AutocompletePlace> mResultList, GoogleApiClient googleApiClient,
                                    LatLngBounds bounds, AutocompleteFilter filter) {
        super(mContext, resource, mResultList);
        this.mResultList = mResultList;
        this.mGoogleApiClient = googleApiClient;
        this.mBounds = bounds;
        this.mPlaceFilter = filter;
        this.mInflater = LayoutInflater.from(mContext);
    }
    /**
     * Returns the number of results received in the last autocomplete query.
     */
    @Override
    public int getCount() {
        return mResultList.size();
    }

    /**
     * Returns an item from the last autocomplete query.
     */
    @Override
    public AutocompletePlace getItem(int position) {
        return mResultList.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder mholder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item, null);
            mholder = new ViewHolder();
            mholder.mName = (TextView) convertView.findViewById(R.id.mName);
            mholder.mAddress = (TextView) convertView.findViewById(R.id.mAddress);
            convertView.setTag(mholder);
        } else {
            mholder = (ViewHolder) convertView.getTag();
        }
        if (mResultList != null && mResultList.size() > 0) {
            AutocompletePlace mAutoComplete = mResultList.get(position);
            String description = mAutoComplete.toString();
            String name = "";
            String address = "";
            if (description.split(",").length > 0)
                name = description.split(",")[0];
            if (description.split(",").length > 1)
                address = description.split(",")[1];
            if (description.split(",").length > 2)
                address = address + " " + description.split(",")[2];
            mholder.mName.setText(name);
            mholder.mAddress.setText(address);
        }

        return convertView;
    }


    /**
     * Returns the filter for the current set of autocomplete results.
     */

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                if (constraint != null) {
                    // Query the autocomplete API for the (constraint) search string.
                    mResultList = getAutocomplete(constraint);
                    if (mResultList != null) {
                        // The API successfully returned results.
                        results.values = mResultList;
                        results.count = mResultList.size();
                    }
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    // The API returned at least one result, update the data.
                    notifyDataSetChanged();
                } else {
                    // The API did not return any results, invalidate the data set.
                    notifyDataSetInvalidated();
                }
            }
        };
        return filter;
    }

    /**
     * Submits an autocomplete query to the Places Geo Data Autocomplete API.
     * Results are returned as {@link AutocompletePlaceAdapter.AutocompletePlace}
     * objects to store the Place ID and description that the API returns.
     * Returns an empty list if no results were found.
     * Returns null if the API client is not available or the query did not complete
     * successfully.
     *
     * @param constraint Autocomplete query string
     * @return Results from the autocomplete API or null if the query was not successful.
     */
    private ArrayList<AutocompletePlace> getAutocomplete(CharSequence constraint) {
        if (mGoogleApiClient.isConnected()) {
            Log.i(TAG, "Starting autocomplete query for: " + constraint);
            PendingResult<AutocompletePredictionBuffer> results =
                    Places.GeoDataApi .getAutocompletePredictions(mGoogleApiClient, constraint.toString(),mBounds, mPlaceFilter);

            // This method should have been called off the main UI thread. Block and wait for at most 60s
            // for a result from the API.
            AutocompletePredictionBuffer autocompletePredictions = results.await(60, TimeUnit.SECONDS);

            // Confirm that the query completed successfully, otherwise return null
            final Status status = autocompletePredictions.getStatus();
            if (!status.isSuccess()) {
                Toast.makeText(getContext(), "Error contacting API: " + status.toString(),
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error getting autocomplete prediction API call: " + status.toString());
                autocompletePredictions.release();
                return null;
            }

            Log.i(TAG, "Query completed. Received " + autocompletePredictions.getCount()
                    + " predictions.");

            // Copy the results into our own data structure, because we can't hold onto the buffer.
            // AutocompletePrediction objects encapsulate the API response (place ID and description).

            Iterator<AutocompletePrediction> iterator = autocompletePredictions.iterator();
            ArrayList resultList = new ArrayList<>(autocompletePredictions.getCount());
            while (iterator.hasNext()) {
                AutocompletePrediction prediction = iterator.next();
                // Get the details of this prediction and copy it into a new PlaceAutocomplete object.
                resultList.add(new AutocompletePlace(prediction.getPlaceId(),
                        prediction.getDescription()));
            }

            // Release the buffer now that all data has been copied.
            autocompletePredictions.release();

            return resultList;
        }
        Log.e(TAG, "Google API client is not connected for autocomplete query.");
        return null;
    }

    /**
     * Holder for Places Geo Data Autocomplete API results.
     */
    public class AutocompletePlace {

        public CharSequence placeId;
        public CharSequence description;

        AutocompletePlace(CharSequence placeId, CharSequence description) {
            this.placeId = placeId;
            this.description = description;
        }

        @Override
        public String toString() {
            return description.toString();
        }
    }


    public class ViewHolder {
        public TextView mName;
        public TextView mAddress;

    }
}
