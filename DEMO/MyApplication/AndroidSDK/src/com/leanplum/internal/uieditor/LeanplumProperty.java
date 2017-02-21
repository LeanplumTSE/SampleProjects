// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal.uieditor;

import android.annotation.TargetApi;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.util.TypedValue;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.leanplum.Leanplum;
import com.leanplum.callbacks.VariablesChangedCallback;
import com.leanplum.internal.ClassUtil;
import com.leanplum.internal.CollectionUtil;
import com.leanplum.internal.FileManager;
import com.leanplum.internal.Log;
import com.leanplum.internal.Util;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Generic Property Class Implements behavior to get and set a view classes property.
 *
 * @author Ben Marten
 */
class LeanplumProperty {
  private final String propertyName;
  private final String getterMethodName;
  private final String setterMethodName;
  private final List<Class> setterMethodArgs;

  /**
   * Creates a leanplum property by property name and settermethodargs.
   *
   * @param propertyName The name of the property
   * @param setterMethodArgs The types of the arguments (class)
   */
  public LeanplumProperty(String propertyName, Class setterMethodArgs) {
    this(propertyName,
        "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1),
        "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1),
        Collections.singletonList(setterMethodArgs));
  }

  /**
   * Creates a leanplum property by providing name, getter & setter method and args.
   *
   * @param propertyName The name of the property.
   * @param getterMethod The name of the getter method.
   * @param setterMethod The name of the setter method.
   * @param setterMethodArgs The setter method arguments.
   */
  public LeanplumProperty(String propertyName, String getterMethod, String setterMethod,
      List<Class> setterMethodArgs) {
    this.propertyName = propertyName;
    this.getterMethodName = getterMethod;
    this.setterMethodName = setterMethod;
    this.setterMethodArgs = setterMethodArgs;
  }

  public String getPropertyName() {
    return propertyName;
  }

  String getSetterMethodName() {
    return setterMethodName;
  }

  List<Class> getSetterMethodArgs() {
    return setterMethodArgs;
  }

  /**
   * Get the value of the property specifically to send it to the server. Will be overridden by
   * specific implementations.
   *
   * @param object The object to retrieve the property value from.
   * @return The property value.
   */
  public Object getPropertyValueServer(Object object) {
    return getPropertyValue(object);
  }

  /**
   * Retrieves the value of the property for the given object.
   *
   * @param object The target object.
   * @return The retrieved value.
   */
  public Object getPropertyValue(Object object) {
    if (this.getterMethodName == null) {
      Log.p("Getter method is undefined for property: " + this.getPropertyName());
      return null;
    }
    Object value = ClassUtil.invokeMethod(object, this.getterMethodName, null, null);
    if (value instanceof Double) {
      return ((Number) value).floatValue();
    } else if (value instanceof SpannableStringBuilder) {
      return value.toString();
    } else {
      return value;
    }
  }

  /**
   * Sets the property value of the given object.
   *
   * @param object The target object.
   * @param value The value of the given object.
   */
  public void setPropertyValue(final Object object, Object value) {
    if (this.setterMethodName == null || this.setterMethodArgs == null) {
      Log.p("Setter method/args is undefined for property: " + this.getPropertyName());
      return;
    }
    // Quickfix for handling double handling (we need float)
    if (value instanceof Double) {
      value = ((Double) value).floatValue();
    }
    final Object finalValue = value;
    ClassUtil.invokeMethod(object, this.setterMethodName, this.setterMethodArgs,
        CollectionUtil.newArrayList(finalValue));
  }

  /**
   * Evaluates if the view needs to be reset.
   *
   * @param view the view to evaluate
   * @param value the value to check
   * @return true, if the current value equals the passed value, else false.
   */
  public boolean isValueInViewEquivalent(View view, Object value) {
    Object currentValue = null;
    try {
      currentValue = this.getPropertyValue(view);
    } catch (Throwable t) {
      Util.handleException(t);
    }

    if (value == null && currentValue == null) {
      return true;
    } else if (value == null || currentValue == null) {
      return false;
    }
    // Special handling for double & float comparison.
    if (currentValue instanceof Float || value instanceof Float ||
        currentValue instanceof Double || value instanceof Double) {
      if (currentValue.equals(((Number) value).floatValue())) {
        return true;
      } else {
        // Make sure there is no rounding error while assigning Float from Double.
        double doubleValue = ((Number) value).doubleValue();
        double currentDoubleValue = ((Number) currentValue).doubleValue();
        return Math.abs(doubleValue - currentDoubleValue) < 2 * Float.MIN_VALUE *
            Math.abs(currentDoubleValue + doubleValue);
      }
    }

    return currentValue.equals(value);
  }

  /**
   * Helper method to map several properties to a map using their name as keys.
   *
   * @param properties The properties to be added to a map.
   * @return A hash map of properties.
   */
  public static Map<String, LeanplumProperty> createPropertyMap(LeanplumProperty... properties) {
    Map<String, LeanplumProperty> result = new HashMap<>(properties.length);
    for (LeanplumProperty property : properties) {
      result.put(property.getPropertyName(), property);
    }
    return result;
  }
}

/**
 * Property Class to handle the frame of a view.
 *
 * @author Ben Marten
 */
class LeanplumPropertyFrame extends LeanplumProperty {
  public LeanplumPropertyFrame(String propertyName) {
    super(propertyName, null, null, null);
  }

  @Override
  public Object getPropertyValue(Object object) {
    return LeanplumCustomProperties.getFrame(((View) object));
  }

  @Override
  public void setPropertyValue(final Object object, final Object value) {
    //noinspection unchecked
    LeanplumCustomProperties.setFrame(((View) object), (Map<String, Number>) value);
  }

  @Override
  public boolean isValueInViewEquivalent(View view, Object value) {
    return true;
  }
}

/**
 * Property Class to handle the url of a webview.
 *
 * @author Ben Marten
 */
class LeanplumPropertyWebViewUrl extends LeanplumProperty {
  public LeanplumPropertyWebViewUrl() {
    super("url", null, null, null);
  }

  @Override
  public Object getPropertyValue(Object object) {
    return ((WebView) object).getUrl();
  }

  @Override
  public void setPropertyValue(final Object object, final Object value) {
    final WebView webView = (WebView) object;

    final WebViewClient existingWebViewClient = ClassUtil.getWebViewClient(webView);
    // Avoid reassigning LeanplumWebViewClient to itself...
    if (existingWebViewClient != null &&
        !existingWebViewClient.getClass().isAssignableFrom(LeanplumWebViewClient.class)) {
      LeanplumInterfaceEditor.stopUpdating();
      webView.setWebViewClient(new LeanplumWebViewClient(existingWebViewClient,
          new LeanplumWebViewClient.LeanplumWebViewClientBlock() {
            @Override
            public void onPageFinished(WebView view, String url) {
              // Run on main thread
              try {
                (new Handler(Looper.getMainLooper())).post(new Runnable() {
                  @Override
                  public void run() {
                    try {
                      LeanplumInterfaceEditor.startUpdating();
                      LeanplumInterfaceEditor.sendUpdateDelayedDefault();
                    } catch (Throwable t) {
                      Util.handleException(t);
                    }
                  }
                });
                webView.setWebViewClient(existingWebViewClient);
                existingWebViewClient.onPageFinished(view, url);
              } catch (Throwable t) {
                Util.handleException(t);
              }
            }
          })

      );
    }

    webView.loadUrl((String) value);
  }

  @Override
  public boolean isValueInViewEquivalent(View view, Object value) {
    // always reset URL to avoid redirect issues
    return true;
  }
}

/**
 * Property Class to handle the background of a view.
 *
 * @author Ben Marten
 */
class LeanplumPropertyBackground extends LeanplumProperty {
  public LeanplumPropertyBackground() {
    super("background", "getBackground", "setBackground", null);
  }

  @Override
  public Object getPropertyValueServer(Object object) {
    Object background = getPropertyValue(object);
    if (background instanceof Number) {
      return background;
    } else {
      Log.p("Background is undefined or not a color.");
      return 0;
    }
  }

  @Override
  @TargetApi(11)
  public Object getPropertyValue(Object object) {
    Drawable background = ((View) object).getBackground();
    if (background instanceof ColorDrawable) {
      ColorDrawable backgroundColor = (ColorDrawable) background;
      return backgroundColor.getColor();
    }
    return background;
  }

  @Override
  @TargetApi(16)
  public void setPropertyValue(Object object, Object value) {
    if (value instanceof Number) {
      ((View) object).setBackgroundColor(((Number) value).intValue());
    } else if (value instanceof Drawable) {
      ((View) object).setBackground((Drawable) value);
    } else if (value == null) {
      ((View) object).setBackground(null);
      ((View) object).setBackgroundColor(0);
    }
  }
}

/**
 * Property Class to handle the backgroundimage of a view.
 *
 * @author Ben Marten
 */
class LeanplumPropertyBackgroundImage extends LeanplumProperty {
  public LeanplumPropertyBackgroundImage() {
    super("backgroundImage", "getBackground", "setBackground", null);
  }

  @Override
  public Object getPropertyValue(Object object) {
    Object background = ((View) object).getBackground();
    return (background != null && Drawable.class.isAssignableFrom(background.getClass()));
  }

  @Override
  public void setPropertyValue(Object object, Object value) {
    ((View) object).setBackgroundColor(((Number) value).intValue());
  }
}

/**
 * Property Class to handle the text size of a view.
 *
 * @author Ben Marten
 */
class LeanplumPropertyTextSize extends LeanplumProperty {
  public LeanplumPropertyTextSize() {
    super("textSize", null, null, null);
  }

  @Override
  public Object getPropertyValue(Object object) {
    return ((TextView) object).getTextSize();
  }

  @Override
  public void setPropertyValue(final Object object, Object value) {
    final float textSize = ((Number) value).floatValue();
    ((TextView) object).setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
  }
}

/**
 * Property Class to handle the text color of a view.
 *
 * @author Ben Marten
 */
class LeanplumPropertyTextColor extends LeanplumProperty {
  public LeanplumPropertyTextColor() {
    super("textColor", "getTextColors", "setTextColor",
        CollectionUtil.<Class>newArrayList(int.class));
  }

  @Override
  public Object getPropertyValue(Object object) {
    Object value = super.getPropertyValue(object);
    if (value != null && ColorStateList.class.isAssignableFrom(value.getClass())) {
      ColorStateList colorStateList = (ColorStateList) value;
      return colorStateList.getDefaultColor();
    } else {
      Log.p("Text color is undefined or not a color.");
      return null;
    }
  }

  @Override
  public void setPropertyValue(final Object object, Object value) {
    final int color = ((Number) value).intValue();
    try {
      String methodName = this.getSetterMethodName();
      List<Class> setterMethodArgs = this.getSetterMethodArgs();
      final Method method = object.getClass().getMethod(methodName, setterMethodArgs.get(0));
      method.invoke(object, color);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }
}

/**
 * Property Class to handle the colorfilter of a view.
 *
 * @author Ben Marten
 */
class LeanplumPropertyDrawableColorFilter extends LeanplumProperty {
  /**
   * Reason for this workaround is that, progressBar.getIndeterminateDrawable().getColorFilter()
   * always returns null.
   */
  private static WeakHashMap<ProgressBar, Integer> colors = new WeakHashMap<>();

  public LeanplumPropertyDrawableColorFilter(String propertyName, String getterMethod) {
    super(propertyName, getterMethod, null, CollectionUtil.<Class>newArrayList(int.class));
  }

  @Override
  /**
   * There is no method to retrieve the
   */
  public Object getPropertyValue(Object object) {
    ProgressBar progressBar = (ProgressBar) object;
    return colors.get(progressBar);
  }

  @Override
  public void setPropertyValue(final Object object, Object value) {
    ProgressBar progressBar = (ProgressBar) object;
    if (value == null) {
      progressBar.getIndeterminateDrawable().setColorFilter(null);
      progressBar.getProgressDrawable().setColorFilter(null);
    } else {
      final int color = ((Number) value).intValue();
      progressBar.getIndeterminateDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
      progressBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
      colors.put(progressBar, color);
    }
  }
}

/**
 * Property Class to handle an image.
 *
 * @author Ben Marten
 */
class LeanplumPropertyImage extends LeanplumProperty {
  public LeanplumPropertyImage() {
    super("image", "getDrawable", null, null);
  }

  @Override
  public Object getPropertyValueServer(Object object) {
    return (super.getPropertyValue(object) != null);
  }

  @Override
  public Object getPropertyValue(Object object) {
    return ((ImageView) object).getDrawable();
  }

  @Override
  public void setPropertyValue(final Object object, final Object value) {
    ImageView imageView = (ImageView) object;
    if (value == null) {
      imageView.setImageResource(android.R.color.transparent);
    } else if (value instanceof Drawable) {
      ((ImageView) object).setImageDrawable((Drawable) value);
    } else if (value instanceof String) {
      final String path = FileManager.fileRelativeToDocuments(value.toString());
      final Bitmap bitmap = BitmapFactory.decodeFile(path);
      if (bitmap != null) {
        imageView.setImageBitmap(bitmap);
      } else {
        Leanplum.addOnceVariablesChangedAndNoDownloadsPendingHandler(
            new VariablesChangedCallback() {
              @Override
              public void variablesChanged() {
                Bitmap downloadedBitmap = BitmapFactory.decodeFile(path);
                if (downloadedBitmap != null) {
                  Log.p("Setting downloaded image ...");
                  ImageView imageView = (ImageView) object;
                  imageView.setImageBitmap(downloadedBitmap);
                  LeanplumInterfaceEditor.sendUpdateDelayed(1000);
                } else {
                  Log.p("Image file not found!");
                }
              }
            });
      }
    }
  }

  /**
   * Always reset the image to its original value before updating the image again. This avoids some
   * image view specific issues. TODO(Ben): Improve, after unit tests have been added.
   */
  @Override
  public boolean isValueInViewEquivalent(View view, Object value) {
    return true;
  }
}

/**
 * Property Class to handle the scaleType of an image.
 *
 * @author Ben Marten
 */
class LeanplumPropertyScaleType extends LeanplumProperty {
  public LeanplumPropertyScaleType() {
    super("scaleType", null, null, null);
  }

  @Override
  public Object getPropertyValue(Object object) {
    ImageView imageView = (ImageView) object;
    return imageView.getScaleType().ordinal();
  }

  @Override
  public void setPropertyValue(Object object, Object value) {
    final int index = ((Number) value).intValue();
    ((ImageView) object).setScaleType(ImageView.ScaleType.values()[index]);
  }
}
