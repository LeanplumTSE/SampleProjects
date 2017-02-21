// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal.uieditor;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Message;
import android.view.KeyEvent;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.leanplum.internal.Util;

/**
 * LeanplumWebViewClient class which forwards all events to the original webViewClient and executes
 * the onPageFinishedBlock on finish.
 *
 * @author Ben Marten
 */
public class LeanplumWebViewClient extends WebViewClient {
  private final LeanplumWebViewClientBlock leanplumWebViewClientBlock;
  private final WebViewClient existingWebViewClient;
  private int running = 0;

  /**
   * Creates a new LeanplumWebViewClient that takes care of sending a new screenshot to the server
   * once a website has loaded completely.
   *
   * @param existingWebViewClient The existing webViewClient.
   * @param leanplumWebViewClientBlock The code block that will be called when the page has finished
   * loading.
   */
  public LeanplumWebViewClient(WebViewClient existingWebViewClient,
      LeanplumWebViewClientBlock leanplumWebViewClientBlock) {
    this.existingWebViewClient = existingWebViewClient;
    this.leanplumWebViewClientBlock = leanplumWebViewClientBlock;
  }

  @Override
  public boolean shouldOverrideUrlLoading(WebView view, String urlNewString) {
    try {
      running++;
      view.loadUrl(urlNewString);
    } catch (Throwable t) {
      Util.handleException(t);
    }
    return existingWebViewClient.shouldOverrideUrlLoading(view, urlNewString);
  }

  //************************************************************************************************
  //*** Overridden methods that will be forwarded to the the original webView client.            ***
  //************************************************************************************************

  @Override
  public void onPageStarted(WebView view, String url, Bitmap favicon) {
    running = Math.max(running, 1); // First request move it to 1.
    existingWebViewClient.onPageStarted(view, url, favicon);
  }

  /**
   * Executes the custom onPageFinished method of the provided code block and the original method.
   */
  @Override
  public void onPageFinished(WebView view, String url) {
    try {
      if (--running <= 0) {
        leanplumWebViewClientBlock.onPageFinished(view, url);
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
    existingWebViewClient.onPageFinished(view, url);
  }

  @Override
  public void onLoadResource(WebView view, String url) {
    existingWebViewClient.onLoadResource(view, url);
  }

  @SuppressWarnings("deprecation")
  @Override
  @Deprecated
  @TargetApi(11)
  public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
    return existingWebViewClient.shouldInterceptRequest(view, url);
  }

  @SuppressWarnings("deprecation")
  @Override
  @Deprecated
  public void onTooManyRedirects(WebView view, Message cancelMsg, Message continueMsg) {
    existingWebViewClient.onTooManyRedirects(view, cancelMsg, continueMsg);
  }

  @SuppressWarnings("deprecation")
  @Override
  @Deprecated
  public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
    existingWebViewClient.onReceivedError(view, errorCode, description, failingUrl);
  }

  @Override
  public void onFormResubmission(WebView view, Message dontResend, Message resend) {
    existingWebViewClient.onFormResubmission(view, dontResend, resend);
  }

  @Override
  public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
    existingWebViewClient.doUpdateVisitedHistory(view, url, isReload);
  }

  @Override
  public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
    existingWebViewClient.onReceivedSslError(view, handler, error);
  }

  @Override
  public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host,
      String realm) {
    existingWebViewClient.onReceivedHttpAuthRequest(view, handler, host, realm);
  }

  @Override
  public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
    return existingWebViewClient.shouldOverrideKeyEvent(view, event);
  }

  @SuppressWarnings("deprecation")
  @Override
  @Deprecated
  public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
    existingWebViewClient.onUnhandledKeyEvent(view, event);
  }

  @Override
  public void onScaleChanged(WebView view, float oldScale, float newScale) {
    existingWebViewClient.onScaleChanged(view, oldScale, newScale);
  }

  @Override
  @TargetApi(12)
  public void onReceivedLoginRequest(WebView view, String realm, String account, String args) {
    existingWebViewClient.onReceivedLoginRequest(view, realm, account, args);
  }

  /**
   * Callback block that is called when the webView finished loading.
   */
  public interface LeanplumWebViewClientBlock {
    void onPageFinished(WebView view, String url);
  }
}
