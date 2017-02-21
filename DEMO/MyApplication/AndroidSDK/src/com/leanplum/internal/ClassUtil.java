// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.leanplum.internal.uieditor.model.ViewRootData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helper class to provide useful methods for operations with classes.
 *
 * @author Ben Marten
 */
public class ClassUtil {
  private static Map<String, Method> methodCache = CollectionUtil.newHashMap();
  private static Map<String, Field> fieldCache = CollectionUtil.newHashMap();

  /**
   * Retrieves a method for the given class by matching method name and parameter types via
   * reflection. Unlike {@link Class#getMethod}, this works with private methods as well.
   *
   * @param clazz The class for which the method is requested.
   * @param methodName The name of the requested method.
   * @param parameterTypes The parameter types of the requested method.
   * @return The requested Method, or null if not found.
   */
  public static Method getMethod(Class<?> clazz, String methodName, List<Class> parameterTypes) {
    // Lookup cached value for faster access.
    final String cacheKey = clazz.getName() + methodName +
        CollectionUtil.concatenateList(parameterTypes, null);
    final Method cachedMethod = methodCache.get(cacheKey);
    if (cachedMethod != null) {
      return cachedMethod;
    }

    for (Method method : clazz.getMethods()) {
      if (method.getName().equals(methodName)) {
        Class<?>[] clazzParameterTypes = method.getParameterTypes();

        if (parameterTypes == null && clazzParameterTypes.length == 0) {
          methodCache.put(cacheKey, method); // Add method to method cache.
          return method;
        }

        // Check if the parameter number matches.
        if (clazzParameterTypes != null && parameterTypes != null &&
            clazzParameterTypes.length == parameterTypes.size()) {
          Boolean match = true;
          // Loop through all the methods parameter types and compare them against the target types.
          for (Class<?> clazzParameterType : method.getParameterTypes()) {
            if (!parameterTypes.contains(clazzParameterType)) {
              match = false;
            }
          }
          if (match) {
            methodCache.put(cacheKey, method); // Add method to method cache.
            return method;
          }
        }
      }
    }
    return null;
  }

  /**
   * Finds a field of a given class recursively by stepping up the inheritance chain. This method
   * also finds private fields, for public methods use: clazz.getField().
   *
   * @param name The name of the field to be found.
   * @param clazz The class of the field to be found.
   * @return The Field.
   * @throws NoSuchFieldException
   */
  private static Field findField(Class clazz, String name) throws NoSuchFieldException {
    // Lookup cached value for faster access
    final String cacheKey = clazz.getName() + name;
    final Field cachedField = fieldCache.get(cacheKey);
    if (cachedField != null) {
      return cachedField;
    }

    Class currentClass = clazz;
    while (currentClass != Object.class) {
      for (Field field : currentClass.getDeclaredFields()) {
        if (name.equals(field.getName())) {
          fieldCache.put(cacheKey, field);
          return field;
        }
      }

      currentClass = currentClass.getSuperclass();
    }

    throw new NoSuchFieldException("Field " + name + " not found for class " + clazz);
  }

  /**
   * Checks whether the given class responds to the setOnClickListener method.
   *
   * @param clazz The class to check.
   * @return True if the class implements setConClickListener, otherwise false.
   */
  public static boolean implementsSetOnClickListener(Class<?> clazz) {
    if (!View.class.isAssignableFrom(clazz)) {
      try {
        clazz.getDeclaredMethod("setOnClickListener", View.OnClickListener.class);
      } catch (NoSuchMethodException e) {
        Log.e("Cannot set onClickListener via reflection." + e.getMessage());
        return false;
      }
    }
    return true;
  }

  /**
   * Returns the current View.OnClickListener for the given View.
   *
   * @param view The View whose click listener to retrieve.
   * @return The View.OnClickListener attached to the view; null if it could not be retrieved.
   */
  public static View.OnClickListener getOnClickListener(View view) {
    Object listener;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      listener = getListenerV14(view, "mOnClickListener");
    } else {
      listener = getListener(view, "mOnClickListener");
    }

    if (listener != null && listener instanceof View.OnClickListener) {
      return (View.OnClickListener) listener;
    }
    return null;
  }

  /**
   * Returns the current View.OnTouchListener for the given View.
   *
   * @param view The View whose touch listener to retrieve.
   * @return The View.OnTouchListener attached to the view; null if it could not be retrieved.
   */
  public static View.OnTouchListener getOnTouchListener(View view) {
    Object listener;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      listener = getListenerV14(view, "mOnTouchListener");
    } else {
      listener = getListener(view, "mOnTouchListener");
    }

    if (listener != null && listener instanceof View.OnClickListener) {
      return (View.OnTouchListener) listener;
    }
    return null;
  }

  /**
   * Returns the Listener, for APIs lower than ICS (API 14).
   *
   * @param view The view to get the listener from.
   * @param listenerFieldName The field name of the listener.
   * @return Returns the listener, null if not found.
   */
  private static Object getListener(View view, String listenerFieldName) {
    Object retrievedListener = null;
    String viewStr = "android.view.View";
    Field field;

    try {
      field = Class.forName(viewStr).getDeclaredField(listenerFieldName);
      retrievedListener = field.get(view);
    } catch (NoSuchFieldException e) {
      Log.e("Reflection: No Such Field.", e);
    } catch (IllegalAccessException e) {
      Log.e("Reflection: Illegal Access.", e);
    } catch (ClassNotFoundException e) {
      Log.e("Reflection: Class Not Found.", e);
    }

    return retrievedListener;
  }

  /**
   * Returns the Listener, for APIs from API14 on.
   *
   * @param view The view to get the listener from.
   * @param listenerFieldName The field name of the listener.
   * @return Returns the listener, null if not found.
   */
  private static Object getListenerV14(View view, String listenerFieldName) {
    Object retrievedListener = null;
    String viewClassName = "android.view.View";
    String listenerInfoClassName = "android.view.View$ListenerInfo";

    try {
      Field listenerField = Class.forName(viewClassName).getDeclaredField("mListenerInfo");
      if (listenerField == null) {
        Log.p("ListenerField is null");
        return null;
      }

      listenerField.setAccessible(true);
      Object listenerInfo = listenerField.get(view);
      if (listenerInfo == null) {
        Log.p("ListenerInfo is null");
        return null;
      }

      Field eventListenerField = Class.forName(listenerInfoClassName)
          .getDeclaredField(listenerFieldName);
      if (eventListenerField == null) {
        Log.p("EventListenerField is null");
        return null;
      }

      eventListenerField.setAccessible(true);
      retrievedListener = eventListenerField.get(listenerInfo);

    } catch (NoSuchFieldException e) {
      Log.e("Reflection: No Such Field.", e);
    } catch (IllegalAccessException e) {
      Log.e("Reflection: Illegal Access.", e);
    } catch (ClassNotFoundException e) {
      Log.e("Reflection: Class Not Found.", e);
    }

    return retrievedListener;
  }

  /**
   * Retrieves an existing WebViewClient of a WebView.
   *
   * @param webView the webView to get the existing client from.
   * @return WebViewClient object or null on error or if none existent.
   */
  public static WebViewClient getWebViewClient(WebView webView) {
    WebViewClient existingWebViewClient = null;
    try {
      Field providerField = webView.getClass().getDeclaredField("mProvider");

      providerField.setAccessible(true);
      Object provider = providerField.get(webView);

      Field contentsClientAdapterField =
          provider.getClass().getDeclaredField("mContentsClientAdapter");
      Log.p(contentsClientAdapterField.toString());

      contentsClientAdapterField.setAccessible(true);
      Object webViewContentsClientAdapter = contentsClientAdapterField.get(provider);

      Field mWebViewClient =
          webViewContentsClientAdapter.getClass().getDeclaredField("mWebViewClient");
      mWebViewClient.setAccessible(true);
      existingWebViewClient = (WebViewClient) mWebViewClient.get(webViewContentsClientAdapter);
    } catch (Exception e) {
      Log.e("Cannot retrieve WebViewClient via reflection." + e.getMessage());
    }

    return existingWebViewClient;
  }

  /**
   * Returns the view group of a view.
   *
   * @param view The view to get the viewgroup from.
   * @return The viewgroup.
   */
  public static ViewGroup getViewGroup(View view) {
    if (!(view instanceof ViewGroup)) {
      return null;
    } else {
      return (ViewGroup) view;
    }
  }

  /**
   * Get the rootviews of an activity.
   *
   * @param activity The activity to analyze.
   * @return A list containing the rootviews.
   */
  public static List<ViewRootData> getRootViews(Activity activity) {
    List<ViewRootData> rootViews = new ArrayList<>();

    Object globalWindowManager;
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
      globalWindowManager = getFieldValue("mWindowManager", activity.getWindowManager());
    } else {
      globalWindowManager = getFieldValue("mGlobal", activity.getWindowManager());
    }
    if (globalWindowManager == null) {
      return null;
    }
    Object rootObjects = getFieldValue("mRoots", globalWindowManager);
    Object paramsObject = getFieldValue("mParams", globalWindowManager);

    if (rootObjects == null || paramsObject == null) {
      return null;
    }

    Object[] roots;
    WindowManager.LayoutParams[] params;

    // Handle change in ArrayList implementation in kitkat.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      roots = ((List) rootObjects).toArray();

      List<WindowManager.LayoutParams> paramsList = CollectionUtil.uncheckedCast(paramsObject);
      params = paramsList.toArray(new WindowManager.LayoutParams[paramsList.size()]);
    } else {
      roots = (Object[]) rootObjects;
      params = (WindowManager.LayoutParams[]) paramsObject;
    }

    int index = 0;
    for (int i = 0; i < roots.length; i++) {
      Object root = roots[i];

      Boolean mAppVisible = CollectionUtil.uncheckedCast(getFieldValue("mAppVisible", root));
      if (!mAppVisible) {
        continue;
      }
      Object attachInfo = getFieldValue("mAttachInfo", root);
      int top = (int) getFieldValue("mWindowTop", attachInfo);
      int left = (int) getFieldValue("mWindowLeft", attachInfo);

      Rect winFrame = (Rect) getFieldValue("mWinFrame", root);
      Rect area = new Rect(left, top, left + winFrame.width(), top + winFrame.height());

      View view = (View) getFieldValue("mView", root);
      rootViews.add(new ViewRootData(index++, view, area, params[i]));
    }

    return rootViews;
  }

  /**
   * Get the value of a field via reflection of the given object.
   *
   * @param fieldName The fieldName to check.
   * @param target The target object to retrieve the value from.
   * @return The value, otherwise throws a LeanplumException on error.
   */
  public static Object getFieldValue(String fieldName, Object target) {
    try {
      return getFieldValueUnchecked(fieldName, target);
    } catch (Exception e) {
      Log.e("Cannot retrieve field value via reflection." + e.getMessage());
      return null;
    }
  }

  /**
   * Returns the value of the given field from the given object via reflection.
   *
   * @param fieldName The fieldName to check.
   * @param target The target object to retrieve the value from.
   * @return The value, otherwise throws exceptions on error.
   * @throws NoSuchFieldException
   * @throws IllegalAccessException
   */
  public static Object getFieldValueUnchecked(String fieldName, Object target)
      throws NoSuchFieldException, IllegalAccessException {
    if (target == null) {
      return null;
    }
    Field field = findField(target.getClass(), fieldName);

    field.setAccessible(true);
    return field.get(target);
  }

  /**
   * Returns the RecyclerViewClass otherwise if found, otherwise null.
   *
   * @return The recyclerview class otherwise null.
   */
  public static Class<?> recyclerViewClass() {
    try {
      return Class.forName("android.support.v7.widget.RecyclerView");
    } catch (ClassNotFoundException e) {
      Log.e("Cannot retrieve RecyclerViewClass via reflection." + e.getMessage());
      return null;
    }
  }

  /**
   * Returns the RecyclerViewClass otherwise if found, otherwise null. Class.forName() does not work
   * in this case because its an anonymous inner class.
   *
   * @return The recyclerview class otherwise null.
   */
  public static Class<?> recyclerViewAdapterClass() {
    Class recyclerViewClass = recyclerViewClass();
    if (recyclerViewClass != null) {
      for (Class innerClass : recyclerViewClass.getDeclaredClasses()) {
        if (innerClass.getName().equals("android.support.v7.widget.RecyclerView$Adapter")) {
          return innerClass;
        }
      }
    }
    return null;
  }

  /**
   * Invoke a method on an object via reflection.
   *
   * @param targetObject The target object to invoke the method on.
   * @param methodName The name of the method.
   * @param argumentClasses The classes of the arguments of the method.
   * @param arguments The actual argument objects.
   * @return Returns the Object returned by the method, othwerwise null.
   */
  public static Object invokeMethod(Object targetObject, String methodName,
      List<Class> argumentClasses, List<?> arguments) {
    Method method = ClassUtil.getMethod(targetObject.getClass(), methodName, argumentClasses);
    if (method != null) {
      try {
        return method.invoke(targetObject, (arguments != null) ? arguments.toArray() : null);
      } catch (Exception e) {
        Log.e("Cannot invoke method via reflection.", e.getMessage());
      }
    }
    return null;
  }

  public static Integer getIntegerField(Object object, String fieldName) {
    int result;
    try {
      Field positionField = ViewPager.LayoutParams.class.getDeclaredField(fieldName);
      positionField.setAccessible(true);
      result = positionField.getInt(object);
    } catch (Exception e) {
      Log.e("Cannot retrieve integer field via reflection." + e.getMessage());
      return null;
    }
    return result;
  }
}
