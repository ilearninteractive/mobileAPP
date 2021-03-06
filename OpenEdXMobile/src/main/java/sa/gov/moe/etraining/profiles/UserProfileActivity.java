package sa.gov.moe.etraining.profiles;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import com.google.inject.Inject;

import sa.gov.moe.etraining.base.BaseSingleFragmentActivity;
import sa.gov.moe.etraining.module.analytics.Analytics;
import sa.gov.moe.etraining.util.Config;

public class UserProfileActivity extends BaseSingleFragmentActivity {
    public static final String EXTRA_USERNAME = "username";

    @Inject
    private Config config;

    public static Intent newIntent(@NonNull Context context, @NonNull String username) {
        return new Intent(context, UserProfileActivity.class)
                .putExtra(EXTRA_USERNAME, username);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideToolbarShadow();
        environment.getAnalyticsRegistry().trackScreenView(Analytics.Screens.PROFILE_VIEW);
    }

    @Override
    public Fragment getFirstFragment() {
        return UserProfileFragment.newInstance(getIntent().getStringExtra(EXTRA_USERNAME));
    }
}
