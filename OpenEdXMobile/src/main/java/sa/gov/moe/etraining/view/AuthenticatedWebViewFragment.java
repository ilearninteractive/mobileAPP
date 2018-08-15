package sa.gov.moe.etraining.view;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import sa.gov.moe.etraining.R;

import roboguice.inject.InjectView;
import sa.gov.moe.etraining.base.BaseFragment;
import sa.gov.moe.etraining.logger.Logger;
import sa.gov.moe.etraining.view.custom.AuthenticatedWebView;

/**
 * Provides a webview which authenticates the user before loading a page,
 * Javascript can also be passed in arguments for evaluation.
 */
public class AuthenticatedWebViewFragment extends BaseFragment {
    protected final Logger logger = new Logger(getClass().getName());
    public static final String ARG_URL = "ARG_URL";
    public static final String ARG_JAVASCRIPT = "ARG_JAVASCRIPT";
    public static final String ARG_IS_MANUALLY_RELOADABLE = "ARG_IS_MANUALLY_RELOADABLE";

    @InjectView(R.id.auth_webview)
    protected AuthenticatedWebView authWebView;

    public static Bundle makeArguments(@NonNull String url, @Nullable String javascript, boolean isManuallyReloadable) {
        final Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        args.putBoolean(ARG_IS_MANUALLY_RELOADABLE, isManuallyReloadable);
        if (!TextUtils.isEmpty(javascript)) {
            args.putString(ARG_JAVASCRIPT, javascript);
        }
        return args;
    }

    public static Fragment newInstance(@NonNull String url) {
        return newInstance(url, null);
    }

    public static Fragment newInstance(@NonNull String url, @Nullable String javascript) {
        final Fragment fragment = new AuthenticatedWebViewFragment();
        fragment.setArguments(makeArguments(url, javascript, false));
        return fragment;
    }

    public static Fragment newInstance(@NonNull String url, @Nullable String javascript, boolean isManuallyReloadable) {
        final Fragment fragment = new AuthenticatedWebViewFragment();
        fragment.setArguments(makeArguments(url, javascript, isManuallyReloadable));
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_authenticated_webview, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getArguments() != null) {
            final String url = getArguments().getString(ARG_URL);
            final String javascript = getArguments().getString(ARG_JAVASCRIPT);
            final boolean isManuallyReloadable = getArguments().getBoolean(ARG_IS_MANUALLY_RELOADABLE);

            authWebView.initWebView(getActivity(), false, isManuallyReloadable);
            authWebView.loadUrlWithJavascript(true, url, javascript);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        authWebView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        authWebView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        authWebView.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        authWebView.onDestroyView();
    }
}
