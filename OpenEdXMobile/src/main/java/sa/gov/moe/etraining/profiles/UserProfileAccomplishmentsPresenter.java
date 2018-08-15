package sa.gov.moe.etraining.profiles;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import sa.gov.moe.etraining.http.callback.Callback;
import sa.gov.moe.etraining.model.Page;
import sa.gov.moe.etraining.model.api.ProfileModel;
import sa.gov.moe.etraining.module.prefs.UserPrefs;
import sa.gov.moe.etraining.user.UserService;
import sa.gov.moe.etraining.view.ViewHoldingPresenter;
import sa.gov.moe.etraining.view.adapters.InfiniteScrollUtils;

public class UserProfileAccomplishmentsPresenter extends ViewHoldingPresenter<UserProfileAccomplishmentsPresenter.ViewInterface> {

    @NonNull
    private final UserService userService;

    @NonNull
    private final String username;

    private final boolean viewingOwnProfile;

    private InfiniteScrollUtils.PageLoadController pageLoadController;

    private int page = 1;

    @NonNull
    private List<BadgeAssertion> badges = new ArrayList<>();

    private boolean pageLoading = false;

    public UserProfileAccomplishmentsPresenter(@NonNull UserService userService, @NonNull UserPrefs userPrefs, @NonNull String username) {
        this.userService = userService;
        this.username = username;
        final ProfileModel model = userPrefs.getProfile();
        viewingOwnProfile = null != model && model.username.equalsIgnoreCase(username);
    }

    @Override
    public void attachView(@NonNull final ViewInterface view) {
        super.attachView(view);
        pageLoadController = new InfiniteScrollUtils.PageLoadController<>(new InfiniteScrollUtils.ListContentController<BadgeAssertion>() {
            @Override
            public void clear() {
                badges.clear();
                setViewModel();
            }

            @Override
            public void addAll(List<BadgeAssertion> items) {
                badges.addAll(items);
                setViewModel();
            }

            @Override
            public void setProgressVisible(boolean visible) {
                pageLoading = visible;
                setViewModel();
            }
        }, new InfiniteScrollUtils.PageLoader<BadgeAssertion>() {
            @Override
            public void loadNextPage(@NonNull final InfiniteScrollUtils.PageLoadCallback<BadgeAssertion> callback) {
                userService.getBadges(username, page).enqueue(new Callback<Page<BadgeAssertion>>() {
                    @Override
                    protected void onResponse(@NonNull final Page<BadgeAssertion> badges) {
                        ++page;
                        callback.onPageLoaded(badges);
                    }

                    @Override
                    protected void onFailure(@NonNull Throwable error) {
                        // do nothing. Better to just deal show what we can
                    }
                });
            }
        });
        pageLoadController.loadMore();
    }

    private void setViewModel() {
        assert getView() != null;
        getView().setModel(new ViewModel(badges, pageLoading, viewingOwnProfile));
    }

    public void onScrolledToEnd() {
        if (null == pageLoadController) {
            return;
        }
        pageLoadController.loadMore();
    }

    public void onClickShare(@NonNull BadgeAssertion badgeAssertion) {
        assert getView() != null;
        getView().startBadgeShareIntent(badgeAssertion.getAssertionUrl());
    }

    public interface ViewInterface {
        void setModel(@NonNull ViewModel model);

        void startBadgeShareIntent(@NonNull String sharedContent);
    }

    public static class ViewModel {
        @NonNull
        public final List<BadgeAssertion> badges;
        public final boolean pageLoading;
        public final boolean enableSharing;

        public ViewModel(@NonNull List<BadgeAssertion> badges, boolean pageLoading, boolean enableSharing) {
            this.badges = badges;
            this.pageLoading = pageLoading;
            this.enableSharing = enableSharing;
        }
    }
}
