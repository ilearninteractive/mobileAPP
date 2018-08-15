package sa.gov.moe.etraining.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.facebook.Settings;
import com.facebook.widget.LikeView;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;

import sa.gov.moe.etraining.R;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import okhttp3.Request;
import sa.gov.moe.etraining.base.BaseFragment;
import sa.gov.moe.etraining.core.IEdxEnvironment;
import sa.gov.moe.etraining.event.NetworkConnectivityChangeEvent;
import sa.gov.moe.etraining.http.callback.ErrorHandlingOkCallback;
import sa.gov.moe.etraining.http.notifications.FullScreenErrorNotification;
import sa.gov.moe.etraining.http.notifications.SnackbarErrorNotification;
import sa.gov.moe.etraining.http.provider.OkHttpClientProvider;
import sa.gov.moe.etraining.interfaces.RefreshListener;
import sa.gov.moe.etraining.logger.Logger;
import sa.gov.moe.etraining.model.api.AnnouncementsModel;
import sa.gov.moe.etraining.model.api.EnrolledCoursesResponse;
import sa.gov.moe.etraining.module.facebook.IUiLifecycleHelper;
import sa.gov.moe.etraining.social.facebook.FacebookProvider;
import sa.gov.moe.etraining.util.NetworkUtil;
import sa.gov.moe.etraining.util.StandardCharsets;
import sa.gov.moe.etraining.util.WebViewUtil;
import sa.gov.moe.etraining.view.custom.EdxWebView;
import sa.gov.moe.etraining.view.custom.URLInterceptorWebViewClient;

public class CourseCombinedInfoFragment extends BaseFragment implements RefreshListener {

    static final String TAG = CourseCombinedInfoFragment.class.getCanonicalName();

    private final Logger logger = new Logger(getClass().getName());

    private EdxWebView announcementWebView;
    private View notificationSettingRow;
    private Switch notificationSwitch;

    private EnrolledCoursesResponse courseData;
    private List<AnnouncementsModel> savedAnnouncements;
    private IUiLifecycleHelper uiHelper;

    @Inject
    protected IEdxEnvironment environment;

    @Inject
    private OkHttpClientProvider okHttpClientProvider;

    private FullScreenErrorNotification errorNotification;

    private SnackbarErrorNotification snackbarErrorNotification;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        logger.debug("created: " + getClass().getName());

        final Context context = getContext();

        Settings.sdkInitialize(context);

        uiHelper = IUiLifecycleHelper.Factory.getInstance(getActivity(), null);
        uiHelper.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_course_combined_info, container, false);

        announcementWebView = (EdxWebView) view.findViewById(R.id.announcement_webview);
        URLInterceptorWebViewClient client = new URLInterceptorWebViewClient(
                getActivity(), announcementWebView);
        // treat every link as external link in this view, so that all links will open in external browser
        client.setAllLinksAsExternal(true);

        notificationSettingRow = view.findViewById(R.id.notification_setting_row);
        notificationSwitch = (Switch) view.findViewById(R.id.notification_switch);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        errorNotification = new FullScreenErrorNotification(announcementWebView);
        snackbarErrorNotification = new SnackbarErrorNotification(announcementWebView);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {

            try {
                savedAnnouncements = savedInstanceState.getParcelableArrayList(Router.EXTRA_ANNOUNCEMENTS);
            } catch (Exception ex) {
                logger.error(ex);
            }

        }

        try {
            final Bundle bundle = getArguments();
            courseData = (EnrolledCoursesResponse) bundle.getSerializable(Router.EXTRA_COURSE_DATA);
            FacebookProvider fbProvider = new FacebookProvider();

            if (courseData != null) {
                //Create the inflater used to create the announcement list
                if (savedAnnouncements == null) {
                    loadAnnouncementData(courseData);
                } else {
                    populateAnnouncements(savedAnnouncements);
                }
            }

            if (environment.getConfig().isNotificationEnabled()
                    && courseData != null && courseData.getCourse() != null) {
                notificationSettingRow.setVisibility(View.VISIBLE);
                final String courseId = courseData.getCourse().getId();
                final String subscriptionId = courseData.getCourse().getSubscription_id();
                boolean isSubscribed = environment.getNotificationDelegate().isSubscribedByCourseId(courseId);
                notificationSwitch.setChecked(isSubscribed);
                notificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        environment.getNotificationDelegate().changeNotificationSetting(
                                courseId, subscriptionId, isChecked);
                    }
                });
            } else {
                notificationSwitch.setOnCheckedChangeListener(null);
                notificationSettingRow.setVisibility(View.GONE);
            }
        } catch (Exception ex) {
            logger.error(ex);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        LikeView.handleOnActivityResult(getActivity(), requestCode, resultCode, data);
        //
        uiHelper.onActivityResult(requestCode, resultCode, data, null);

    }

    @Override
    public void onResume() {
        super.onResume();
        uiHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (savedAnnouncements != null) {
            outState.putParcelableArrayList(Router.EXTRA_ANNOUNCEMENTS, new ArrayList<Parcelable>(savedAnnouncements));
        }
        uiHelper.onSaveInstanceState(outState);

    }

    private void loadAnnouncementData(EnrolledCoursesResponse enrollment) {
        okHttpClientProvider.getWithOfflineCache().newCall(new Request.Builder()
                .url(enrollment.getCourse().getCourse_updates())
                .get()
                .build())
                .enqueue(new ErrorHandlingOkCallback<List<AnnouncementsModel>>(getActivity(),
                        new TypeToken<List<AnnouncementsModel>>() {
                        }, errorNotification, snackbarErrorNotification,
                        this) {
                    @Override
                    protected void onResponse(final List<AnnouncementsModel> announcementsList) {
                        if (getActivity() == null) {
                            return;
                        }
                        savedAnnouncements = announcementsList;
                        if (announcementsList != null && announcementsList.size() > 0) {
                            populateAnnouncements(announcementsList);
                        } else {
                            errorNotification.showError(R.string.no_announcements_to_display,
                                    FontAwesomeIcons.fa_exclamation_circle, 0, null);
                        }
                    }

                    @Override
                    protected void onFinish() {
                        if (getActivity() == null) {
                            return;
                        }
                        if (!EventBus.getDefault().isRegistered(CourseCombinedInfoFragment.this)) {
                            EventBus.getDefault().registerSticky(CourseCombinedInfoFragment.this);
                        }
                    }
                });

    }

    private void populateAnnouncements(@NonNull List<AnnouncementsModel> announcementsList) {
        errorNotification.hideError();

        StringBuilder buff = WebViewUtil.getIntialWebviewBuffer(getActivity(), logger);

        buff.append("<body>");
        for (AnnouncementsModel model : announcementsList) {
            buff.append("<div class=\"header\">");
            buff.append(model.getDate());
            buff.append("</div>");
            buff.append("<div class=\"separator\"></div>");
            buff.append("<div>");
            buff.append(model.getContent());
            buff.append("</div>");
        }
        buff.append("</body>");

        announcementWebView.clearCache(true);
        announcementWebView.loadDataWithBaseURL(environment.getConfig().getApiHostURL(), buff.toString(), "text/html", StandardCharsets.UTF_8.name(), null);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(NetworkConnectivityChangeEvent event) {
        if (!NetworkUtil.isConnected(getContext())) {
            if (!errorNotification.isShowing()) {
                snackbarErrorNotification.showOfflineError(this);
            }
        }
    }

    @Override
    public void onRefresh() {
        errorNotification.hideError();
        loadAnnouncementData(courseData);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onRevisit() {
        if (NetworkUtil.isConnected(getActivity())) {
            snackbarErrorNotification.hideError();
        }
    }
}