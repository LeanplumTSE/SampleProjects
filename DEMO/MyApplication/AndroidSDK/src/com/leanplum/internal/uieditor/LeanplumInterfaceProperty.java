// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal.uieditor;

import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.leanplum.internal.ClassUtil;
import com.leanplum.internal.CollectionUtil;
import com.leanplum.internal.Log;
import com.leanplum.internal.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface property class to describe the hierarchy of classed and properties that can be
 * scanned.
 *
 * @author Ben Marten
 */
class LeanplumInterfaceProperty {
  /**
   * A linked hash map that represents the ordered view class hierarchy, e.g. View > TextView > ...
   */
  private static final Map<Class, Map<String, LeanplumProperty>> activePropertiesForClasses =
      CollectionUtil.newLinkedHashMap(
          View.class, LeanplumProperty.createPropertyMap(
              new LeanplumProperty("visibility", int.class),
              new LeanplumPropertyFrame("frame"),
              new LeanplumProperty(
                  "clickable",
                  "isClickable",
                  "setClickable",
                  CollectionUtil.<Class>newArrayList(boolean.class)
              ),
              new LeanplumProperty("alpha", float.class),
              new LeanplumPropertyBackground()
          ),
          TextView.class, LeanplumProperty.createPropertyMap(
              new LeanplumProperty("text", CharSequence.class),
              new LeanplumPropertyTextColor(),
              new LeanplumPropertyTextSize(),
              new LeanplumProperty("gravity", int.class)
          ),
          EditText.class, LeanplumProperty.createPropertyMap(
              new LeanplumProperty("text", CharSequence.class),
              new LeanplumProperty("hint", CharSequence.class),
              new LeanplumPropertyTextColor(),
              new LeanplumPropertyTextSize(),
              new LeanplumProperty("gravity", int.class)
          ),
          Button.class, LeanplumProperty.createPropertyMap(
              new LeanplumProperty("text", CharSequence.class),
              new LeanplumPropertyTextColor(),
              new LeanplumPropertyBackground(),
              new LeanplumPropertyBackgroundImage(),
              new LeanplumPropertyTextSize()
          ),
          ImageView.class, LeanplumProperty.createPropertyMap(
              new LeanplumPropertyImage(),
              new LeanplumPropertyScaleType()
          ),
          ProgressBar.class, LeanplumProperty.createPropertyMap(
              new LeanplumProperty("progress", int.class),
              new LeanplumPropertyBackground(),
              new LeanplumPropertyDrawableColorFilter(
                  "progressDrawable",
                  "getProgressDrawable"
              ),
              new LeanplumProperty("max", int.class)
          ),
          SearchView.class, LeanplumProperty.createPropertyMap(
              new LeanplumProperty("queryHint", CharSequence.class)
          ),
          Switch.class, LeanplumProperty.createPropertyMap(
              new LeanplumProperty(
                  "checked",
                  "isChecked",
                  "setChecked",
                  CollectionUtil.<Class>newArrayList(boolean.class)
              ),
              new LeanplumProperty(
                  "enabled",
                  "isEnabled",
                  "setEnabled",
                  CollectionUtil.<Class>newArrayList(boolean.class)
              )
          ),
          SeekBar.class, LeanplumProperty.createPropertyMap(
              new LeanplumProperty("progress", int.class),
              new LeanplumProperty("max", int.class)
          ),
          WebView.class, LeanplumProperty.createPropertyMap(
              new LeanplumPropertyWebViewUrl()
          )
      );
  private static final Map<Class, List<Class<?>>> speciesForClass = new HashMap<>();

  /**
   * Returns the properties for a view.
   *
   * @param view The view to analyze.
   * @return A map containing the properties of a view.
   */
  public static Map<String, Object> getPropertiesForView(View view) {
    final List<Class<?>> species = speciesForViewClass(view.getClass());
    final Map<String, Object> viewProperties = new HashMap<>();

    // Events
    if (LeanplumInterfaceEditor.getMode() == LeanplumEditorMode.LP_EDITOR_MODE_EVENT) {
      if (ClassUtil.implementsSetOnClickListener(view.getClass())) {
        viewProperties.put("tapEvent", true);
      }
    }

    // Properties
    // Add default properties
    viewProperties.put("absoluteFrame", LeanplumCustomProperties.getAbsoluteFrame(view));
    // Add class specific properties
    for (Class specie : species) {
      Map<String, LeanplumProperty> properties = activePropertiesForClasses.get(specie);
      for (LeanplumProperty property : properties.values()) {
        if (viewProperties.get(property.getPropertyName()) == null) {
          Object propertyValue = null;
          try {
            propertyValue = property.getPropertyValueServer(view);
          } catch (Throwable t) {
            Util.handleException(t);
          }
          if (propertyValue != null) {
            viewProperties.put(property.getPropertyName(), propertyValue);
          }
        }
      }
    }

    final Class<?> kind = species.get(species.size() - 1);
    return CollectionUtil.newHashMap("species", kind.getName(), "properties", viewProperties);
  }

  /**
   * Retrieves the LeanplumProperty with a given propertyName and a given view.
   *
   * @param propertyName The name of the property.
   * @param view The view to analyze.
   * @return The retrieved LeanmplumProperty, null if property is not known for this class.
   */
  public static LeanplumProperty getProperty(String propertyName, View view) {
    List<Class<?>> species = speciesForViewClass(view.getClass());
    for (Class className : species) {
      Map<String, LeanplumProperty> knownPropertiesForClass =
          activePropertiesForClasses.get(className);
      LeanplumProperty result = knownPropertiesForClass.get(propertyName);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  /**
   * Returns the actual value of a property on a given view object.
   *
   * @param propertyName The name of the property.
   * @param view The view to analyze.
   * @return The retrieved object.
   */
  public static Object getValueForProperty(String propertyName, View view) {
    LeanplumProperty property = LeanplumInterfaceProperty.getProperty(propertyName, view);
    if (property != null) {
      Object propertyValue = null;
      try {
        propertyValue = property.getPropertyValue(view);
      } catch (Throwable t) {
        Util.handleException(t);
      }
      return propertyValue;
    } else {
      throw new IllegalArgumentException("Property not found: " + propertyName + " for class "
          + view.getClass().getName());
    }
  }

  /**
   * Returns a list of species a view class belongs to.
   *
   * @param viewClass The view class.
   * @return A hierarchical species list.
   */
  private static List<Class<?>> speciesForViewClass(Class<?> viewClass) {
    List<Class<?>> species = speciesForClass.get(viewClass);
    if (species != null) {
      return species;
    }

    // Discover new class and map to species
    species = new ArrayList<>();
    for (Map.Entry<Class, Map<String, LeanplumProperty>> className :
        activePropertiesForClasses.entrySet()) {
      Class<?> type = className.getKey();
      if (type.isAssignableFrom(viewClass) && !species.contains(type)) {
        species.add(type);
      }
    }

    Log.p("Discovered new class: \"" + viewClass.getName() +
        "\" Species: " + species.toString());
    speciesForClass.put(viewClass, species);

    return species;
  }
}
