// Copyright 2013, Leanplum, Inc.

package com.leanplum;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;

import com.leanplum.ResourceQualifiers.Qualifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Description of resources.asrc file (we don't use this right nwo)
// http://ekasiswanto.wordpress.com/2012/09/19/descriptions-of-androids-resources-arsc/

public class LeanplumResources extends Resources {
  public LeanplumResources(Resources base) {
    super(base.getAssets(), base.getDisplayMetrics(), base.getConfiguration());
  }

  /* internal */ @SuppressWarnings("unchecked")
  <T>Var<T> getOverrideResource(int id) {
    try {
      String name = getResourceEntryName(id);
      String type = getResourceTypeName(id);
      if (FileManager.resources == null) {
        return null;
      }
      HashMap<String, Object> resourceValues = (HashMap<String, Object>)
          FileManager.resources.objectForKeyPath();
      Map<String, String> eligibleFolders = new HashMap<String, String>();
      synchronized (VarCache.valuesFromClient) {
        for (String folder : resourceValues.keySet()) {
          if (!folder.toLowerCase().startsWith(type)) {
            continue;
          }
          HashMap<String, Object> files = (HashMap<String, Object>) resourceValues.get(folder);
          String eligibleFile = null;
          for (String filename : files.keySet()) {
            String currentName = filename.replace("\\.", ".");
            // Get filename without extension.
            int dotPos = currentName.lastIndexOf('.');
            if (dotPos >= 0) {
              currentName = currentName.substring(0, dotPos);
            }
    
            if (currentName.equals(name)) {
              eligibleFile = filename;
            }
          }
          if (eligibleFile == null) {
            continue;
          }
          eligibleFolders.put(folder, eligibleFile);
        }
      }

      Map<String, ResourceQualifiers> folderQualifiers = new HashMap<String, ResourceQualifiers>();
      for (String folder : eligibleFolders.keySet()) {
        folderQualifiers.put(folder, ResourceQualifiers.fromFolder(folder));
      }
  
      // 1. Eliminate qualifiers that contradict the device configuration.
      // See http://developer.android.com/guide/topics/resources/providing-resources.html
      Configuration config = getConfiguration();
      DisplayMetrics display = getDisplayMetrics();
      Set<String> matchedFolders = new HashSet<String>();
      for (String folder : eligibleFolders.keySet()) {
        ResourceQualifiers qualifiers = folderQualifiers.get(folder);
        for (Qualifier qualifier : qualifiers.qualifiers.keySet()) {
          if (qualifier.getFilter().isMatch(
              qualifiers.qualifiers.get(qualifier), config, display)) {
            matchedFolders.add(folder);
          }
        }
      }
  
      // 2. Identify the next qualifier in the table (MCC first, then MNC,
      // then language, and so on.
      for (Qualifier qualifier : ResourceQualifiers.Qualifier.values()) {
        Map<String, Object> betterMatchedFolders = new HashMap<String, Object>();
        for (String folder : matchedFolders) {
          ResourceQualifiers folderQualifier = folderQualifiers.get(folder);
          Object qualifierValue = folderQualifier.qualifiers.get(qualifier);
          if (qualifierValue != null) {
            betterMatchedFolders.put(folder, qualifierValue);
          }
        }
        betterMatchedFolders = qualifier.getFilter().bestMatch(
            betterMatchedFolders, config, display);
  
        // 3. Do any resource directories use this qualifier?
        if (!betterMatchedFolders.isEmpty()) {
          // Yes.
          // 4. Eliminate directories that do not include this qualifier.
          matchedFolders = betterMatchedFolders.keySet();
        }
      }
  
      // Return result.
      for (String folder : eligibleFolders.keySet()) {
        String varName = Constants.Values.RESOURCES_VARIABLE + "." + folder
            + "." + eligibleFolders.get(folder);
        return VarCache.getVariable(varName);
      }
    } catch (Exception e) {
      Log.e("Leanplum", "Error getting resource", e);
    }
    return null;
  }

  @Override
  public Drawable getDrawable(int id) throws NotFoundException {
    try {
      Var<String> override = getOverrideResource(id);
      if (override != null) {
        int overrideResId = override.overrideResId();
        if (overrideResId != 0) {
          return super.getDrawable(overrideResId);
        }
        if (!override.stringValue.equals(override.defaultValue())) {
          Drawable result = Drawable.createFromStream(override.stream(), override.fileValue());
          if (result != null) {
            return result;
          }
        }
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
    return super.getDrawable(id);
  }

  /*
   * TODO
  private String getOverrideString(int id) {
    // TODO
    return null;
  }
  
  @Override
  public String getString(int id) throws NotFoundException {
    //System.out.println(getResourceEntryName(id));

    String override = getOverrideString(id);
    if (override != null) {
      return override;
    }
    return super.getString(id);
  }
  
  @Override
  public String getString(int id, Object... formatArgs)
      throws NotFoundException {
    //System.out.println(getResourceEntryName(id));

    String override = getOverrideString(id);
    if (override != null) {
      return String.format(override, formatArgs);
    }
    return super.getString(id, formatArgs);
  }
  
  @Override
  public void parseBundleExtra(String tagName, AttributeSet attrs,
      Bundle outBundle) throws XmlPullParserException {
    // TODO Auto-generated method stub
    super.parseBundleExtra(tagName, attrs, outBundle);
  }
  
  @Override
  public void parseBundleExtras(XmlResourceParser parser, Bundle outBundle)
      throws XmlPullParserException, IOException {
    // TODO Auto-generated method stub
    super.parseBundleExtras(parser, outBundle);
  }
  
  @Override
  public void updateConfiguration(Configuration config, DisplayMetrics metrics) {
    // TODO Auto-generated method stub
    super.updateConfiguration(config, metrics);
  }
  
  @Override
  public String[] getStringArray(int id) throws NotFoundException {
    System.out.println(getResourceEntryName(id));

    return super.getStringArray(id);
  }

  @Override
  public XmlResourceParser getAnimation(int id) throws NotFoundException {
    // TODO Auto-generated method stub
    System.out.println(getResourceEntryName(id));

    return super.getAnimation(id);
  }
  
  @Override
  public boolean getBoolean(int id) throws NotFoundException {
    System.out.println(getResourceEntryName(id));

    Object override = resources.objectForKeyPath(getResourceEntryName(id));
    if (override != null) {
      return Boolean.valueOf(override.toString());
    }
    return super.getBoolean(id);
  }
  
  @Override
  public int getColor(int id) throws NotFoundException {
    // TODO Auto-generated method stub
    System.out.println(getResourceEntryName(id));

    return super.getColor(id);
  }
  
  @Override
  public ColorStateList getColorStateList(int id) throws NotFoundException {
    // TODO Auto-generated method stub
    System.out.println(getResourceEntryName(id));

    return super.getColorStateList(id);
  }
  
  @Override
  public float getDimension(int id) throws NotFoundException {
    // TODO Auto-generated method stub
   System.out.println(getResourceEntryName(id));

    return super.getDimension(id);
  }
  
  @Override
  public int getDimensionPixelOffset(int id) throws NotFoundException {
    // TODO Auto-generated method stub
    System.out.println(getResourceEntryName(id));

    return super.getDimensionPixelOffset(id);
  }
  
  @Override
  public int getDimensionPixelSize(int id) throws NotFoundException {
    // TODO Auto-generated method stub
    System.out.println(getResourceEntryName(id));

    return super.getDimensionPixelSize(id);
  }
  
  @Override
  public float getFraction(int id, int base, int pbase) {
    // TODO Auto-generated method stub
    System.out.println(getResourceEntryName(id));

    return super.getFraction(id, base, pbase);
  }
  
  @Override
  public int[] getIntArray(int id) throws NotFoundException {
    // TODO Auto-generated method stub
    System.out.println(getResourceEntryName(id));

    return super.getIntArray(id);
  }

  @Override
  public int getInteger(int id) throws NotFoundException {
    System.out.println(getResourceEntryName(id));

    Object override = resources.objectForKeyPath(getResourceEntryName(id));
    if (override != null) {
      return Integer.valueOf(override.toString());
    }
    return super.getInteger(id);
  }
  */
}
