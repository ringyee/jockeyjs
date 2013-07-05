package com.jockeyjs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.util.SparseArray;
import android.webkit.WebView;

import com.jockeyjs.JockeyHandler.OnCompletedListener;

public abstract class JockeyImpl implements Jockey {

	// A default Callback that does nothing.
	protected static final JockeyCallback _DEFAULT = new JockeyCallback() {
		@Override
		public void call() {
		}
	};

	private Map<String, List<JockeyHandler>> _listeners = new HashMap<String, List<JockeyHandler>>();
	private SparseArray<JockeyCallback> _callbacks = new SparseArray<JockeyCallback>();

	private OnValidateListener _onValidateListener;

	private Handler _handler = new Handler();

	private final JockeyWebViewClient _client;

	public JockeyImpl() {
		_client = new JockeyWebViewClient(this);
	}

	@Override
	public void send(String type, WebView toWebView) {
		send(type, toWebView, null);
	}

	@Override
	public void send(String type, WebView toWebView, Object withPayload) {
		send(type, toWebView, withPayload, null);
	}

	@Override
	public void send(String type, WebView toWebView, JockeyCallback complete) {
		send(type, toWebView, null, complete);

	}

	@Override
	public void on(String type, JockeyHandler... handler) {

		if (!this.handles(type)) {
			_listeners.put(type, new ArrayList<JockeyHandler>());
		}

		_listeners.get(type).addAll(Arrays.asList(handler));
	}

	@Override
	public void off(String type) {
		_listeners.remove(type);
	}

	@Override
	public boolean handles(String eventName) {
		return _listeners.containsKey(eventName);
	}

	protected void add(int messageId, JockeyCallback callback) {
		_callbacks.put(messageId, callback);
	}

	protected void triggerEventFromWebView(final WebView webView,
			JockeyWebViewPayload envelope) {
		final int messageId = envelope.id;
		String type = envelope.type;

		if (this.handles(type)) {
			List<JockeyHandler> handlers = _listeners.get(type);

			for (JockeyHandler handler : handlers) {
				handler.perform(envelope.payload, new OnCompletedListener() {
					@Override
					public void onCompleted() {
						//This has to be done with a handler because a webview load must be triggered
						//in the UI thread
						_handler.post(new Runnable() {
							@Override
							public void run() {
								triggerCallbackOnWebView(webView, messageId);
							}
						});
					}
				});
			}
		}
	}

	protected void triggerCallbackForMessage(int messageId) {
		try {
			JockeyCallback complete = _callbacks.get(messageId, _DEFAULT);
			complete.call();
		} catch (Exception e) {
			e.printStackTrace();
		}
		_callbacks.remove(messageId);
	}

	public void validate(String host) throws HostValidationException {
		if (_onValidateListener != null && !_onValidateListener.validate(host)) {
			throw new HostValidationException();
		}
	}

	@Override
	public void setOnValidateListener(OnValidateListener listener) {
		_onValidateListener = listener;
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public void configure(WebView webView) {
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(this.getWebViewClient());
	}

	private JockeyWebViewClient getWebViewClient() {
		return this._client;
	}

	public static JockeyImpl getDefault() {
		return new DefaultJockeyImpl();
	}
}
