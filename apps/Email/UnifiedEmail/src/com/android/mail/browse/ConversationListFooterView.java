/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mail.browse;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.mail.R;
import com.android.mail.providers.Folder;
import com.android.mail.providers.UIProvider;
import com.android.mail.ui.ViewMode;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;

public class ConversationListFooterView extends LinearLayout implements View.OnClickListener,
        ViewMode.ModeChangeListener {

    public interface FooterViewClickListener {
        void onFooterViewErrorActionClick(Folder folder, int errorStatus);
        void onFooterViewLoadMoreClick(Folder folder);
        /// M: add for local search(start remote search in footer view not by IME key).
        void onFooterViewRemoteSearchClick(Folder folder);
    }

    protected View mLoading;
    protected View mNetworkError;
    protected View mLoadMore;
    protected Button mErrorActionButton;
    protected TextView mErrorText;
    protected Folder mFolder;
    protected Uri mLoadMoreUri;
    protected int mErrorStatus;
    protected FooterViewClickListener mClickListener;
    protected final boolean mTabletDevice;
    // Backgrounds for different states.
    private static Drawable sWideBackground;
    private static Drawable sNormalBackground;

    public ConversationListFooterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTabletDevice = Utils.useTabletUI(context.getResources());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mLoading = findViewById(R.id.loading);
        mNetworkError = findViewById(R.id.network_error);
        mLoadMore = findViewById(R.id.load_more);
        mLoadMore.setOnClickListener(this);
        mErrorActionButton = (Button) findViewById(R.id.error_action_button);
        mErrorActionButton.setOnClickListener(this);
        mErrorText = (TextView)findViewById(R.id.error_text);
    }

    public void setClickListener(FooterViewClickListener listener) {
        mClickListener = listener;
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        final Folder f = (Folder) v.getTag();
        if (id == R.id.error_action_button) {
            mClickListener.onFooterViewErrorActionClick(f, mErrorStatus);
        } else if (id == R.id.load_more) {
            LogUtils.d(LogTag.getLogTag(), "LoadMore triggered folder [%s]",
                    f != null ? f.loadMoreUri : "null");
            mClickListener.onFooterViewLoadMoreClick(f);
        }
    }

    /// M: update UI status, for Loading/LoadMore/NetworkError.
    public void updateLoadingStatus(boolean start) {
        LogUtils.d(LogTag.getLogTag(), "updateLoadingStatus show loading progress dialog ? [%s]", start);
        mNetworkError.setVisibility(View.GONE);
        if (start) {
            mLoading.setVisibility(View.VISIBLE);
            mLoadMore.setVisibility(View.GONE);
        } else {
            mLoading.setVisibility(View.GONE);
            mLoadMore.setVisibility(View.VISIBLE);
        }
    }

    public void setFolder(Folder folder) {
        mFolder = folder;
        mErrorActionButton.setTag(mFolder);
        mLoadMore.setTag(mFolder);
        mLoadMoreUri = folder.loadMoreUri;
    }

    /**
     * Update the view to reflect the new folder status.
     */
    public boolean updateStatus(final ConversationCursor cursor) {
        if (cursor == null) {
            updateLoadingStatus(true);
            return true;
        }
        boolean showFooter = true;
        final Bundle extras = cursor.getExtras();
        final int cursorStatus = extras.getInt(UIProvider.CursorExtraKeys.EXTRA_STATUS);
        mErrorStatus = extras.containsKey(UIProvider.CursorExtraKeys.EXTRA_ERROR) ?
                extras.getInt(UIProvider.CursorExtraKeys.EXTRA_ERROR)
                : UIProvider.LastSyncResult.SUCCESS;
        final int totalCount = extras.getInt(UIProvider.CursorExtraKeys.EXTRA_TOTAL_COUNT);
        /// M: Get the value from extra
        final boolean allMessagesLoadFinish = extras.getBoolean(
                UIProvider.CursorExtraKeys.EXTRA_MESSAGES_LOAD_FINISH, false);
        if (UIProvider.CursorStatus.isWaitingForResults(cursorStatus)) {
            updateLoadingStatus(true);
        } else if (mErrorStatus != UIProvider.LastSyncResult.SUCCESS) {
            mNetworkError.setVisibility(View.VISIBLE);
            mErrorText.setText(Utils.getSyncStatusText(getContext(), mErrorStatus));
            mLoading.setVisibility(View.GONE);
            mLoadMore.setVisibility(View.GONE);
            // Only show the "Retry" button for I/O errors; it won't help for
            // internal errors.
            mErrorActionButton.setVisibility(
                    mErrorStatus != UIProvider.LastSyncResult.SECURITY_ERROR ?
                    View.VISIBLE : View.GONE);

            final int actionTextResourceId;
            switch (mErrorStatus) {
                case UIProvider.LastSyncResult.CONNECTION_ERROR:
                    actionTextResourceId = R.string.retry;
                    break;
                case UIProvider.LastSyncResult.AUTH_ERROR:
                    actionTextResourceId = R.string.signin;
                    break;
                case UIProvider.LastSyncResult.SECURITY_ERROR:
                    actionTextResourceId = R.string.retry;
                    mNetworkError.setVisibility(View.GONE);
                    break; // Currently we do nothing for security errors.
                case UIProvider.LastSyncResult.STORAGE_ERROR:
                    actionTextResourceId = R.string.info;
                    break;
                case UIProvider.LastSyncResult.INTERNAL_ERROR:
                    actionTextResourceId = R.string.report;
                    break;
                default:
                    actionTextResourceId = R.string.retry;
                    mNetworkError.setVisibility(View.GONE);
                    break;
            }
            mErrorActionButton.setText(actionTextResourceId);

        } else if (mLoadMoreUri != null && cursor.getCount() < totalCount
                /**M: filter some dirty update callback, default when cursor count little than total count.
                 * load more view will show, in fact in the initial step of remote search the loader complete result
                 * is 0 before run search on server, which will cause loading status stopped.
                 * It make user confused, 'It should loading instead of load more' @}*/
                && cursor.getCount() > 0) {
                /** @}*/
            LogUtils.d(LogTag.getLogTag(),
                    " LoadMore finished folder [%s], totalCount %d, cursor count %d",
                    mLoadMoreUri, totalCount, cursor.getCount());
            if (allMessagesLoadFinish) {
                /// M: When all messages load finish, load more do not need to display.
                showFooter = false;
            } else {
                updateLoadingStatus(false);
            }
        } else {
            showFooter = false;
        }
        return showFooter;
    }

    /**
     * Update to the appropriate background when the view mode changes.
     */
    @Override
    public void onViewModeChanged(int newMode) {
        final Drawable drawable;
        if (mTabletDevice && newMode == ViewMode.CONVERSATION_LIST) {
            drawable = getWideBackground();
        } else {
            drawable = getNormalBackground();
        }
        setBackgroundDrawable(drawable);
    }

    private Drawable getWideBackground() {
        if (sWideBackground == null) {
            sWideBackground = getBackground(R.drawable.conversation_wide_unread_selector);
        }
        return sWideBackground;
    }

    private Drawable getNormalBackground() {
        if (sNormalBackground == null) {
            sNormalBackground = getBackground(R.drawable.conversation_unread_selector);
        }
        return sNormalBackground;
    }

    private Drawable getBackground(int resId) {
        return getContext().getResources().getDrawable(resId);
    }
}
