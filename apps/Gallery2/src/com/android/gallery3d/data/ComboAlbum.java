/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.gallery3d.data;

import com.android.gallery3d.util.Future;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

// ComboAlbum combines multiple media sets into one. It lists all media items
// from the input albums.
// This only handles SubMediaSets, not MediaItems. (That's all we need now)
public class ComboAlbum extends MediaSet implements ContentListener {
    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/ComboAlbum";
    private final MediaSet[] mSets;
    private String mName;
    private final Path mPath;

    public ComboAlbum(Path path, MediaSet[] mediaSets, String name) {
        super(path, nextVersionNumber());
        mPath = path;
        mSets = mediaSets;
        for (MediaSet set : mSets) {
            set.addContentListener(this);
        }
        mName = name;
    }

    @Override
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        ArrayList<MediaItem> items = new ArrayList<MediaItem>();
        for (MediaSet set : mSets) {
            int size = set.getMediaItemCount();
            if (count < 1) break;
            if (start < size) {
                int fetchCount = (start + count <= size) ? count : size - start;
                ArrayList<MediaItem> fetchItems = set.getMediaItem(start, fetchCount);
                items.addAll(fetchItems);
                count -= fetchItems.size();
                start = 0;
            } else {
                start -= size;
            }
        }
        return items;
    }

    @Override
    public int getMediaItemCount() {
        int count = 0;
        for (MediaSet set : mSets) {
            count += set.getMediaItemCount();
        }
        return count;
    }

    @Override
    public boolean isLeafAlbum() {
        return true;
    }

    @Override
    public String getName() {
        return mName;
    }

    public void useNameOfChild(int i) {
        if (i < mSets.length) mName = mSets[i].getName();
    }

    @Override
    public long reload() {
        boolean changed = false;
        for (int i = 0, n = mSets.length; i < n; ++i) {
            long version = mSets[i].reload();
            if (version > mDataVersion) changed = true;
        }
        if (changed) mDataVersion = nextVersionNumber();
        return mDataVersion;
    }

    @Override
    public void onContentDirty() {
        notifyContentChanged();
    }

    @Override
    public Future<Integer> requestSync(SyncListener listener) {
        return requestSyncOnMultipleSets(mSets, listener);
    }
    
    // M: added for debug purpose only
    public String getComboInfo() {
        StringBuilder builder = new StringBuilder();
        builder.append("combo {");
        if (mSets == null || mSets.length == 0) {
            builder.append("[empty]");
        } else {
            for (MediaSet set : mSets) {
                if (set == null) {
                    continue;
                }
                builder.append(set.getName());
                builder.append(",");
                builder.append(set.getMediaItemCount());
                builder.append("; ");
            }
	      }
        builder.append("}");
        return builder.toString();
    }
}
