// Copyright 2014, Leanplum, Inc.

package com.leanplum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.Geofence.Builder;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationStatusCodes;

class GeofenceStatus {
  public static final int UNKNOWN = 1;
  public static final int INSIDE = 2;
  public static final int OUTSIDE = 4;

  public static boolean shouldTriggerEnteredGeofence(Number currentStatus, Number newStatus) {
    return ((currentStatus.intValue() == OUTSIDE || currentStatus.intValue() == UNKNOWN) &&
        newStatus.intValue() == INSIDE);
  }

  public static boolean shouldTriggerExitedGeofence(Number currentStatus, Number newStatus) {
    return (currentStatus.intValue() == INSIDE &&
        newStatus.intValue() == OUTSIDE);
  }
}

/**
 * Handles geofencing.
 * @author Atanas Dobrev
 */
class LocationManagerImpl implements
    GoogleApiClient.ConnectionCallbacks, OnConnectionFailedListener, LocationManager {
  private Map<String, Object> lastKnownState;
  private Map<String, Object> stateBeforeBackground;
  private List<Geofence> allGeofences;
  private List<Geofence> backgroundGeofences;
  private List<String> trackedGeofenceIds;
  private boolean isInBackground;
  static final String PERMISSION = "android.permission.ACCESS_FINE_LOCATION";
  static final String METADATA = "com.google.android.gms.version";
  private GoogleApiClient googleApiClient;

  private static LocationManagerImpl instance;

  public static synchronized LocationManager instance() {
    if (instance == null) {
      instance = new LocationManagerImpl();
    }
    return instance;
  }

  private LocationManagerImpl() {
    trackedGeofenceIds = new ArrayList<String>();
    loadLastKnownRegionState();
    isInBackground = Util.isInBackground();
  }

  @SuppressWarnings("unchecked")
  public void setRegionsData(Map<String, Object> regionData,
                             Set<String> foregroundRegionNames, Set<String> backgroundRegionNames) {
    if (!Util.hasPlayServices()) {
      return;
    }

    allGeofences = new ArrayList<Geofence>();
    backgroundGeofences = new ArrayList<Geofence>();

    for (String regionName : regionData.keySet()) {
      boolean isForeground = foregroundRegionNames.contains(regionName);
      boolean isBackground = backgroundRegionNames.contains(regionName);
      if (isForeground || isBackground) {
        Geofence geofence = geofenceFromMap((Map<String, Object>) regionData.get(regionName),
            regionName);
        if (geofence != null) {
          if (isBackground) {
            backgroundGeofences.add(geofence);
          }
          allGeofences.add(geofence);
          if (lastKnownState.get(geofence.getRequestId()) == null) {
            lastKnownState.put(geofence.getRequestId(), GeofenceStatus.UNKNOWN);
          }
        }
      }
    }

    startLocationClient();
  }

  public void updateGeofencing() {
    if (allGeofences != null && backgroundGeofences != null) {
      startLocationClient();
    }
  }

  private void loadLastKnownRegionState() {
    if (lastKnownState != null) {
      return;
    }
    Context context = Leanplum.getContext();
    SharedPreferences defaults = context.getSharedPreferences(
        "__leanplum__location", Context.MODE_PRIVATE);
    String regionsStateJson = defaults.getString(Constants.Keys.REGION_STATE, null);
    if (regionsStateJson == null) {
      lastKnownState = new HashMap<String, Object>();
    } else {
      lastKnownState = JsonConverter.fromJson(regionsStateJson);
    }
  }

  private void saveLastKnownRegionState() {
    if (lastKnownState == null) {
      return;
    }
    Context context = Leanplum.getContext();
    SharedPreferences defaults = context.getSharedPreferences(
        "__leanplum__location", Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = defaults.edit();
    editor.putString(Constants.Keys.REGION_STATE, JsonConverter.toJson(lastKnownState));
    try {
      editor.apply();
    } catch (NoSuchMethodError e) {
      editor.commit();
    }
  }

  private Geofence geofenceFromMap(Map<String, Object> regionData, String regionName) {
    Number latitude = (Number) regionData.get("lat");
    Number longitude = (Number) regionData.get("lon");
    Number radius = (Number) regionData.get("radius");
    Number version = (Number) regionData.get("version");
    if (latitude == null) {
      return null;
    }
    Builder geofenceBuilder = new Builder();
    geofenceBuilder.setCircularRegion(latitude.floatValue(),
        longitude.floatValue(), radius.floatValue());
    geofenceBuilder.setRequestId(geofenceID(regionName, version.intValue()));
    geofenceBuilder.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
        Geofence.GEOFENCE_TRANSITION_EXIT);
    geofenceBuilder.setExpirationDuration(Geofence.NEVER_EXPIRE);
    return geofenceBuilder.build();
  }

  private String geofenceID(String regionName, Integer version) {
    return "__leanplum" + regionName + "_" + version.toString();
  }

  private void startLocationClient() {
    if (!isPermissionGranted() || !isMetaDataSet()) {
      Log.d("Leanplum",
          "You have to set the application meta data and location permission to use geofencing.");
      return;
    }
    Context context = Leanplum.getContext();
    if (googleApiClient == null) {
      googleApiClient = new GoogleApiClient.Builder(Leanplum.getContext())
          .addApi(LocationServices.API)
          .addConnectionCallbacks(this)
          .addOnConnectionFailedListener(this)
          .build();
    }
    if (!googleApiClient.isConnected() && !googleApiClient.isConnecting()) {
      googleApiClient.connect();
    } else if (googleApiClient.isConnected()) {
      updateTrackedGeofences();
    }
  }

  private boolean isPermissionGranted() {
    Context context = Leanplum.getContext();
    return context.checkCallingOrSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED;
  }

  private boolean isMetaDataSet() {
    Context context = Leanplum.getContext();
    try {
      ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
          context.getPackageName(), PackageManager.GET_META_DATA);
      if (appInfo != null) {
        if (appInfo.metaData != null) {
          Object value = appInfo.metaData.get(METADATA);
          if (value != null) {
            return true;
          }
        }
      }
      return false;
    } catch (NameNotFoundException e) {
      return false;
    }
  }

  private void updateTrackedGeofences() {
    if (allGeofences == null || googleApiClient == null || !googleApiClient.isConnected()) {
      return;
    }
    if (!isInBackground && Util.isInBackground()) {
      stateBeforeBackground = new HashMap<String, Object>();
      for (String key : lastKnownState.keySet()) {
        stateBeforeBackground.put(key, lastKnownState.get(key));
      }
    }
    List<Geofence> toBeTrackedGeofences = getToBeTrackedGeofences();
    if (trackedGeofenceIds.size() > 0) {
      LocationServices.GeofencingApi.removeGeofences(googleApiClient, trackedGeofenceIds);
    }
    trackedGeofenceIds = new ArrayList<String>();
    if (toBeTrackedGeofences.size() > 0) {
      LocationServices.GeofencingApi.addGeofences(
          googleApiClient, toBeTrackedGeofences, getTransitionPendingIntent());
      for (Geofence geofence : toBeTrackedGeofences) {
        trackedGeofenceIds.add(geofence.getRequestId());
        //TODO: stateBeforeBackground doesn't get persisted. 
        // If the app goes to the background and terminates, stateBeforeBackground will be reset.
        if (isInBackground && !Util.isInBackground() && stateBeforeBackground != null
            // This is triggered only for in-app messages, since backgroundGeofences are only for
            // pushes.
            // TODO(aleks): This would not work for in-app messages if we have the same geolocation
            // triggering it, as a locally triggered push notification.
            && !backgroundGeofences.contains(geofence)) {
          Number lastStatus = (Number) stateBeforeBackground.get(geofence.getRequestId());
          Number currentStatus = (Number) lastKnownState.get(geofence.getRequestId());
          if (currentStatus != null && lastStatus != null) {
            if (GeofenceStatus.shouldTriggerEnteredGeofence(lastStatus, currentStatus)) {
              maybePerformActions(geofence, "enterRegion");
            }
            if (GeofenceStatus.shouldTriggerExitedGeofence(lastStatus, currentStatus)) {
              maybePerformActions(geofence, "exitRegion");
            }
          }
        }
      }
    }
    if (isInBackground && !Util.isInBackground()) {
      stateBeforeBackground = null;
    }
    isInBackground = Util.isInBackground();
  }

  private List<Geofence> getToBeTrackedGeofences() {
    if (Util.isInBackground()) {
      return backgroundGeofences;
    } else {
      return allGeofences;
    }
  }

  public void updateStatusForGeofences(List<Geofence> geofences, int transitionType) {
    for (Geofence geofence : geofences) {
      if (!trackedGeofenceIds.contains(geofence.getRequestId()) &&
          geofence.getRequestId().startsWith("__leanplum")) {
        ArrayList<String> geofencesToRemove = new ArrayList<String>();
        geofencesToRemove.add(geofence.getRequestId());
        if (googleApiClient != null && googleApiClient.isConnected()) {
          LocationServices.GeofencingApi.removeGeofences(googleApiClient, geofencesToRemove);
        }
        continue;
      }
      Number currentStatus = (Number) lastKnownState.get(geofence.getRequestId());
      if (currentStatus != null) {
        if (GeofenceStatus.shouldTriggerEnteredGeofence(currentStatus,
            getStatusForTransitionType(transitionType))) {
          maybePerformActions(geofence, "enterRegion");
        }
        if (GeofenceStatus.shouldTriggerExitedGeofence(currentStatus,
            getStatusForTransitionType(transitionType))) {
          maybePerformActions(geofence, "exitRegion");
        }
      }
      lastKnownState.put(geofence.getRequestId(),
          getStatusForTransitionType(transitionType));
    }
    saveLastKnownRegionState();
  }

  private void maybePerformActions(Geofence geofence, String action) {
    String regionName = getRegionName(geofence.getRequestId());
    if (regionName != null) {
      int filter = Util.isInBackground() ?
          LeanplumMessageMatchFilter.LEANPLUM_ACTION_FILTER_BACKGROUND :
          LeanplumMessageMatchFilter.LEANPLUM_ACTION_FILTER_ALL;
      Leanplum.maybePerformActions(action, regionName, filter, null, null);
    }
  }

  private int getStatusForTransitionType(int transitionType) {
    if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER ||
        transitionType == Geofence.GEOFENCE_TRANSITION_DWELL) {
      return GeofenceStatus.INSIDE;
    } else {
      return GeofenceStatus.OUTSIDE;
    }
  }

  private String getRegionName(String geofenceRequestId) {
    if (geofenceRequestId.startsWith("__leanplum")) {
      return (String) geofenceRequestId.substring(10, geofenceRequestId.lastIndexOf("_"));
    }
    return null;
  }

  private PendingIntent getTransitionPendingIntent() {
    Context context = Leanplum.getContext();
    Intent intent = new Intent(context, ReceiveTransitionsIntentService.class);
    return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  @Override
  public void onConnected(Bundle arg0) {
    try {
      updateTrackedGeofences();
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  @Override
  public void onConnectionSuspended(int i) {
    // According to the Android documentation at
    // https://developers.google.com/android/reference/com/google/android/gms/common/api/GoogleApiClient.ConnectionCallbacks?hl=en
    // GoogleApiClient will automatically attempt to restore the connection.
  }

  @Override
  public void onConnectionFailed(ConnectionResult connectionResult) {
    if (connectionResult.hasResolution()) {
    } else {
    }
  }
}
