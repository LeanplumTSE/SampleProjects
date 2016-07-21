// Copyright 2013, Leanplum, Inc.

package com.leanplum;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import android.content.res.Configuration;
import android.os.Build;
import android.util.DisplayMetrics;

// Helpful resources
// https://code.google.com/p/android-apktool/
// http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/4.1.2_r1/android/content/res/Configuration.java#Configuration.compareTo%28android.content.res.Configuration%29

class ResourceQualifiers {
  static abstract class QualifierFilter {
    abstract Object getMatch(String str);
    abstract boolean isMatch(Object value, Configuration config, DisplayMetrics display);
    Map<String, Object> bestMatch(Map<String, Object> values, Configuration config, DisplayMetrics display) {
      return values;
    }
  }

  enum Qualifier {
    MCC(new QualifierFilter() {
      @Override
      public Object getMatch(String str) {
        if (str.startsWith("mcc")) {
          return Integer.getInteger(str.substring(3));
        }
        return null;
      }
      @Override
      public boolean isMatch(Object value, Configuration config, DisplayMetrics display) {
        return config.mcc == ((Integer) value);
      }
    }),
    MNC(new QualifierFilter() {
      @Override
      public Object getMatch(String str) {
        if (str.startsWith("mnc")) {
          return Integer.getInteger(str.substring(3));
        }
        return null;
      }
      @Override
      public boolean isMatch(Object value, Configuration config, DisplayMetrics display) {
        return config.mnc == ((Integer) value);
      }
    }),
    LANGUAGE(new QualifierFilter() {
      @Override
      public Object getMatch(String str) {
        if (str.length() == 2) {
          return str;
        }
        return null;
      }
      @Override
      public boolean isMatch(Object value, Configuration config, DisplayMetrics display) {
        return config.locale.getLanguage().equals(value);
      }
    }),
    REGION(new QualifierFilter() {
      @Override
      public Object getMatch(String str) {
        if (str.startsWith("r") && str.length() == 3) {
          return str.substring(1);
        }
        return null;
      }
      @Override
      public boolean isMatch(Object value, Configuration config, DisplayMetrics display) {
        return config.locale.getCountry().toLowerCase().equals(value);
      }
    }),
    LAYOUT_DIRECTION(new QualifierFilter() {
      // From http://developer.android.com/reference/android/content/res/Configuration.html#SMALLEST_SCREEN_WIDTH_DP_UNDEFINED
      public static final int SCREENLAYOUT_LAYOUTDIR_LTR = 0x00000040;
      public static final int SCREENLAYOUT_LAYOUTDIR_RTL = 0x00000080;
      public static final int SCREENLAYOUT_LAYOUTDIR_MASK = 0x000000c0;
      @Override
      public Object getMatch(String str) {
        if (str.equals("ldrtl")) {
         return SCREENLAYOUT_LAYOUTDIR_RTL;
        } else if (str.equals("ldltr")) {
          return SCREENLAYOUT_LAYOUTDIR_LTR;
        }
        return null;
      }
      @Override
      public boolean isMatch(Object value, Configuration config, DisplayMetrics display) {
        return (config.screenLayout & SCREENLAYOUT_LAYOUTDIR_MASK) == (Integer) value;
      }
    }),
    SMALLEST_WIDTH(new QualifierFilter() {
      @Override
      public Object getMatch(String str) {
        if (str.startsWith("sw") && str.endsWith("dp")) {
          return Integer.getInteger(str.substring(2, str.length() - 2));
        }
        return null;
      }
      @Override
      public boolean isMatch(Object value, Configuration config, DisplayMetrics display) {
        try {
          Field field = config.getClass().getField("smallestScreenWidthDp");
          int smallestWidthDp = (int) (Integer) field.get(config);
          return smallestWidthDp >= (Integer) value;
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }
      @Override
      Map<String, Object> bestMatch(Map<String, Object> values, Configuration config, DisplayMetrics display) {
        Map<String, Object> result = new HashMap<String, Object>();
        int max = Integer.MIN_VALUE;
        for (String key : values.keySet()) {
          Integer intObj = (Integer) values.get(key);
          if (intObj > max) {
            max = intObj;
            result.clear();
          }
          if (intObj == max) {
            result.put(key, intObj);
          }
        }
        return result;
      }
    }),
    AVAILABLE_WIDTH(new QualifierFilter() {
      @Override
      public Object getMatch(String str) {
        if (str.startsWith("w") && str.endsWith("dp")) {
          return Integer.getInteger(str.substring(1, str.length() - 2));
        }
        return null;
      }
      @Override
      public boolean isMatch(Object value, Configuration config, DisplayMetrics display) {
        try {
          Field field = config.getClass().getField("screenWidthDp");
          int screenWidthDp = (int) (Integer) field.get(config);
          return screenWidthDp >= (Integer) value;
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }
      @Override
      Map<String, Object> bestMatch(Map<String, Object> values, Configuration config, DisplayMetrics display) {
        Map<String, Object> result = new HashMap<String, Object>();
        int max = Integer.MIN_VALUE;
        for (String key : values.keySet()) {
          Integer intObj = (Integer) values.get(key);
          if (intObj > max) {
            max = intObj;
            result.clear();
          }
          if (intObj == max) {
            result.put(key, intObj);
          }
        }
        return result;
      }
    }),
    AVAILABLE_HEIGHT(new QualifierFilter() {
      @Override
      public Object getMatch(String str) {
        if (str.startsWith("h") && str.endsWith("dp")) {
          return Integer.getInteger(str.substring(1, str.length() - 2));
        }
        return null;
      }
      @Override
      public boolean isMatch(Object value, Configuration config, DisplayMetrics display) {
        try {
          Field field = config.getClass().getField("screenHeightDp");
          int screenHeightDp = (int) (Integer) field.get(config);
          return screenHeightDp >= (Integer) value;
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }
      @Override
      Map<String, Object> bestMatch(Map<String, Object> values, Configuration config, DisplayMetrics display) {
        Map<String, Object> result = new HashMap<String, Object>();
        int max = Integer.MIN_VALUE;
        for (String key : values.keySet()) {
          Integer intObj = (Integer) values.get(key);
          if (intObj > max) {
            max = intObj;
            result.clear();
          }
          if (intObj == max) {
            result.put(key, intObj);
          }
        }
        return result;
      }
    }),
    SCREEN_SIZE(new QualifierFilter() {
      @Override
      public Object getMatch(String str) {
        if (str.equals("small")) {
          return Configuration.SCREENLAYOUT_SIZE_SMALL;
        } else if (str.equals("normal")) {
          return Configuration.SCREENLAYOUT_SIZE_NORMAL;
        } else if (str.equals("large")) {
          return Configuration.SCREENLAYOUT_SIZE_LARGE;
        } else if (str.equals("xlarge")) {
          return Configuration.SCREENLAYOUT_SIZE_XLARGE;
        }
        return null;
      }
      @Override
      public boolean isMatch(Object value, Configuration config, DisplayMetrics display) {
        return (config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) <= (Integer) value;
      }
      @Override
      Map<String, Object> bestMatch(Map<String, Object> values, Configuration config, DisplayMetrics display) {
        Map<String, Object> result = new HashMap<String, Object>();
        int max = Integer.MIN_VALUE;
        for (String key : values.keySet()) {
          Integer intObj = (Integer) values.get(key);
          if (intObj > max) {
            max = intObj;
            result.clear();
          }
          if (intObj == max) {
            result.put(key, intObj);
          }
        }
        return result;
      }
    }),
    SCREEN_ASPECT(new QualifierFilter() {
      @Override
      public Object getMatch(String str) {
        if (str.equals("long")) {
          return Configuration.SCREENLAYOUT_LONG_YES;
        } else if (str.equals("notlong")) {
          return Configuration.SCREENLAYOUT_LONG_NO;
        }
        return null;
      }
      @Override
      public boolean isMatch(Object value, Configuration config, DisplayMetrics display) {
        return (config.screenLayout & Configuration.SCREENLAYOUT_LONG_MASK) == (Integer) value;
      }
    }),
    SCREEN_ORIENTATION(new QualifierFilter() {
      @Override
      public Object getMatch(String str) {
        if (str.equals("port")) {
          return Configuration.ORIENTATION_PORTRAIT;
        } else if (str.equals("land")) {
          return Configuration.ORIENTATION_LANDSCAPE;
        }
        return null;
      }
      @Override
      public boolean isMatch(Object value, Configuration config, DisplayMetrics display) {
        return config.orientation  == (Integer) value;
      }
    }),
    UI_MODE(new QualifierFilter() {
      public static final int UI_MODE_TYPE_TELEVISION = 0x00000004;
      public static final int UI_MODE_TYPE_APPLIANCE = 0x00000005;
      @Override
      public Object getMatch(String str) {
        if (str.equals("car")) {
          return Configuration.UI_MODE_TYPE_CAR;
        } else if (str.equals("desk")) {
          return Configuration.UI_MODE_TYPE_DESK;
        } else if (str.equals("television")) {
          return UI_MODE_TYPE_TELEVISION;
        } else if (str.equals("appliance")) {
          return UI_MODE_TYPE_APPLIANCE;
        }
        return null;
      }
      @Override
      public boolean isMatch(Object value, Configuration config, DisplayMetrics display) {
        return (config.uiMode & Configuration.UI_MODE_TYPE_MASK) == (Integer) value;
      }
    }),
    NIGHT_MODE(new QualifierFilter() {
      @Override
      public Object getMatch(String str) {
        if (str.equals("night")) {
          return Configuration.UI_MODE_NIGHT_YES;
        } else if (str.equals("notnight")) {
          return Configuration.UI_MODE_NIGHT_NO;
        }
        return null;
      }
      @Override
      public boolean isMatch(Object value, Configuration config, DisplayMetrics display) {
        return (config.uiMode & Configuration.UI_MODE_NIGHT_MASK) == (Integer) value;
      }
    }),
    SCREEN_PIXEL_DENSITY(new QualifierFilter() {
      public static final int DENSITY_TV = 0x000000d5;
      public static final int DENSITY_XXHIGH = 0x000001e0;
      public static final int DENSITY_NONE = 0;
      @Override
      public Object getMatch(String str) {
        if (str.equals("ldpi")) {
           return DisplayMetrics.DENSITY_LOW;
        } else if (str.equals("mdpi")) {
          return DisplayMetrics.DENSITY_MEDIUM;
        } else if (str.equals("hdpi")) {
          return DisplayMetrics.DENSITY_HIGH;
        } else if (str.equals("xhdpi")) {
          return DisplayMetrics.DENSITY_XHIGH;
        } else if (str.equals("nodpi")) {
          return DENSITY_NONE;
        } else if (str.equals("tvdpi")) {
          return DENSITY_TV;
        } else if (str.equals("xxhigh")) {
          return DENSITY_XXHIGH;
        }
        return null;
      }
      @Override
      public boolean isMatch(Object value, Configuration config, DisplayMetrics display) {
        return true;
      }
      @Override
      Map<String, Object> bestMatch(Map<String, Object> values, Configuration config, DisplayMetrics display) {
        Map<String, Object> result = new HashMap<String, Object>();
        int min = Integer.MAX_VALUE;
        for (String key : values.keySet()) {
          Integer intObj = (Integer) values.get(key);
          if (intObj < min && intObj >= display.densityDpi) {
            min = intObj;
            result.clear();
          }
          if (intObj == min) {
            result.put(key, intObj);
          }
        }
        if (result.size() == 0) {
          int max = Integer.MIN_VALUE;
          for (String key : values.keySet()) {
            Integer intObj = (Integer) values.get(key);
            if (intObj > max) {
              max = intObj;
              result.clear();
            }
            if (intObj == max) {
              result.put(key, intObj);
            }
          } 
        }
        return result;
      }
    }),
    TOUCHSCREEN_TYPE(new QualifierFilter() {
      @Override
      public Object getMatch(String str) {
        if (str.equals("notouch")) {
          return Configuration.TOUCHSCREEN_NOTOUCH;
        } else if (str.equals("finger")) {
          return Configuration.TOUCHSCREEN_FINGER;
        }
        return null;
      }
      @Override
      public boolean isMatch(Object value, Configuration config, DisplayMetrics display) {
        return config.touchscreen == (Integer) value;
      }
    }),
    KEYBOARD_AVAILABILITY(new QualifierFilter() {
      @Override
      public Object getMatch(String str) {
        if (str.equals("keysexposed")) {
          return Configuration.KEYBOARDHIDDEN_NO;
        } else if (str.equals("keyshidden")) {
          return Configuration.KEYBOARDHIDDEN_YES;
        } else if (str.equals("keyssoft")) {
          return 0;
        }
        return null;
      }
      @Override
      public boolean isMatch(Object value, Configuration config, DisplayMetrics display) {
        if ((Integer) value == 0) {
          return true;
        }
        return config.keyboardHidden == (Integer) value;
      }
    }),
    PRIMARY_TEXT_INPUTMETHOD(new QualifierFilter() {
      @Override
      public Object getMatch(String str) {
        if (str.equals("nokeys")) {
          return Configuration.KEYBOARD_NOKEYS;
        } else if (str.equals("qwerty")) {
          return Configuration.KEYBOARD_QWERTY;           
        } else if (str.equals("12key")) {
          return Configuration.KEYBOARD_12KEY;
        }
        return null;
      }
      @Override
      public boolean isMatch(Object value, Configuration config, DisplayMetrics display) {
        return config.keyboard == (Integer) value;
      }
    }),
    NAVIGATION_KEY_AVAILABILITY(new QualifierFilter() {
      @Override
      public Object getMatch(String str) {
        if (str.equals("navexposed")) {
          return Configuration.NAVIGATIONHIDDEN_NO;
        } else if (str.equals("navhidden")) {
          return Configuration.NAVIGATIONHIDDEN_YES;
        }
        return null;
      }
      @Override
      public boolean isMatch(Object value, Configuration config, DisplayMetrics display) {
        return config.navigationHidden == (Integer) value;
      }
    }),
    PRIMARY_NON_TOUCH_NAVIGATION_METHOD(new QualifierFilter() {
      @Override
      public Object getMatch(String str) {
        if (str.equals("nonav")) {
          return Configuration.NAVIGATION_NONAV;
        } else if (str.equals("dpad")) {
          return Configuration.NAVIGATION_DPAD;
        } else if (str.equals("trackball")) {
          return Configuration.NAVIGATION_TRACKBALL;
        } else if (str.equals("wheel")) {
          return Configuration.NAVIGATION_WHEEL;
        }
        return null;
      }
      @Override
      public boolean isMatch(Object value, Configuration config, DisplayMetrics display) {
        return config.navigation == (Integer) value;
      }
    }),
    PLATFORM_VERSION(new QualifierFilter() {
      @Override
      public Object getMatch(String str) {
        if (str.startsWith("v")) {
          return Integer.getInteger(str.substring(1));
        }
        return null;
      }
      @Override
      public boolean isMatch(Object value, Configuration config, DisplayMetrics display) {
        return Build.VERSION.SDK_INT >= (Integer) value;
      }
    });

    private QualifierFilter filter;

    private Qualifier(QualifierFilter filter) {
      this.filter = filter;
    }

    public QualifierFilter getFilter() {
      return filter;
    }
  }

  Map<Qualifier, Object> qualifiers = new HashMap<Qualifier, Object>();

  public static ResourceQualifiers fromFolder(String folderName) {
    ResourceQualifiers result = new ResourceQualifiers();
    String[] nameParts = folderName.toLowerCase().split("-");
    int qualifierIndex = 0;
    for (String part : nameParts) {
      boolean isMatch = false;
      while (!isMatch && qualifierIndex < Qualifier.values().length) {
        Qualifier qualifier = Qualifier.values()[qualifierIndex];
        Object match = qualifier.getFilter().getMatch(part);
        if (match != null) {
          result.qualifiers.put(qualifier, match);
          isMatch = true;
        }
        qualifierIndex++;
      }
    }
    return result;
  }
}