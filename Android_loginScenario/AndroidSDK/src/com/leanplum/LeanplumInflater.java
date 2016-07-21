// Copyright 2013, Leanplum, Inc.

package com.leanplum;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Inflates layout files that may be overridden by other files.
 * @author Andrew First
 */
public class LeanplumInflater {
  private Context context;
  private LeanplumResources res;

  public static LeanplumInflater from(Context context) {
    return new LeanplumInflater(context);
  }

  private LeanplumInflater(Context context) {
    this.context = context;
  }

  public LeanplumResources getLeanplumResources() {
    return getLeanplumResources(null);
  }

  public LeanplumResources getLeanplumResources(Resources baseResources) {
    if (res != null) {
      return res;
    }
    if (baseResources == null) {
      baseResources = context.getResources();
    }
    if (baseResources instanceof LeanplumResources) {
      return (LeanplumResources) baseResources;
    }
    res = new LeanplumResources(baseResources);
    return res;
  }

  /**
   * Creates a view from the corresponding resource ID.
   */
  public View inflate(int layoutResID) {
    return inflate(layoutResID, null, false);
  }

  /**
   * Creates a view from the corresponding resource ID.
   */
  public View inflate(int layoutResID, ViewGroup root) {
    return inflate(layoutResID, root, root != null);
  }

  /**
   * Creates a view from the corresponding resource ID.
   */
  public View inflate(int layoutResID, ViewGroup root, boolean attachToRoot) {
    Var<String> var = null;
    try {
      LeanplumResources res = getLeanplumResources(context.getResources());
      var = res.getOverrideResource(layoutResID);
      if (var == null || var.stringValue.equals(var.defaultValue())) {
        return LayoutInflater.from(context).inflate(layoutResID, root, attachToRoot);
      }
      int overrideResId = var.overrideResId();
      if (overrideResId != 0) {
        return LayoutInflater.from(context).inflate(overrideResId, root, attachToRoot);
      }
    } catch (Throwable t) {
      if (!(t instanceof InflateException)) {
        Util.handleException(t);
      }
      return LayoutInflater.from(context).inflate(layoutResID, root, attachToRoot);
    }

    try {
      ByteArrayOutputStream fileData = new ByteArrayOutputStream();
      InputStream stream = var.stream();
      byte[] buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = stream.read(buffer)) > -1) {
        fileData.write(buffer, 0, bytesRead);
      }
      stream.close();
      Object xmlBlock = Class.forName("android.content.res.XmlBlock").getConstructor(
          byte[].class).newInstance(fileData.toByteArray());
      XmlResourceParser parser = null;
      try {
        parser = (XmlResourceParser) xmlBlock.getClass().getMethod(
            "newParser").invoke(xmlBlock);
        return LayoutInflater.from(context).inflate(parser, root, attachToRoot);
      } catch (Throwable t) {
        throw new RuntimeException(t);
      } finally {
        if (parser != null) {
          parser.close();
        }
      }
    } catch (Throwable t) {
      Log.e("Leanplum", "Could not inflate resource " + layoutResID + ":" + var.stringValue(), t);
    }
    return LayoutInflater.from(context).inflate(layoutResID, root, attachToRoot);
  }
}
