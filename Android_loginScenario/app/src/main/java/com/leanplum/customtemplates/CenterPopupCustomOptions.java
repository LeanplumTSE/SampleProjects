// Copyright 2014, Leanplum, Inc.

package com.leanplum.customtemplates;

import android.content.Context;
import android.graphics.Color;

import com.leanplum.ActionArgs;
import com.leanplum.ActionContext;
import com.leanplum.customtemplates.MessageTemplates.Args;

/**
 * Options used by {@link CenterPopup}.
 *
 * @author Martin Yanakiev
 */
public class CenterPopupCustomOptions extends CenterPopupOptions {
    private int width;
    private int height;

    public CenterPopupCustomOptions(ActionContext context) {
        super(context);
//    setWidth(context.numberNamed(Args.LAYOUT_WIDTH).intValue());
//    setHeight(context.numberNamed(Args.LAYOUT_HEIGHT).intValue());
    }

//  public int getWidth() {
//    return width;
//  }
//
//  private void setWidth(int width) {
//    this.width = width;
//  }
//
//  public int getHeight() {
//    return height;
//  }
//
//  private void setHeight(int height) {
//    this.height = height;
//  }

    public static ActionArgs toArgs(Context currentContext) {
        return BaseMessageOptions.toArgs(currentContext)
                .with(Args.LAYOUT_WIDTH, MessageTemplates.Values.CENTER_POPUP_WIDTH)
                .with(Args.LAYOUT_HEIGHT, MessageTemplates.Values.CENTER_POPUP_HEIGHT * 2)
                .withColor(Args.BACKGROUND_COLOR, Color.BLUE)
                .with(Args.ACCEPT_BUTTON_TEXT_2, MessageTemplates.Values.OK_TEXT)
                .withColor(Args.ACCEPT_BUTTON_BACKGROUND_COLOR_2, Color.WHITE)
                .withColor(Args.ACCEPT_BUTTON_TEXT_COLOR_2, Color.argb(255, 0, 122, 255));
    }
}
