/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.contacts.quickcontact;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.WebAddress;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.Data;
import android.text.TextUtils;
import android.util.Log;

import com.android.contacts.R;
import com.android.contacts.common.CallUtil;
import com.android.contacts.common.ContactsUtils;
import com.android.contacts.common.MoreContactUtils;
import com.android.contacts.common.model.account.AccountType.EditType;
import com.android.contacts.common.model.dataitem.DataItem;
import com.android.contacts.common.model.dataitem.DataKind;
import com.android.contacts.common.model.dataitem.EmailDataItem;
import com.android.contacts.common.model.dataitem.ImDataItem;
import com.android.contacts.common.model.dataitem.PhoneDataItem;
import com.android.contacts.common.model.dataitem.SipAddressDataItem;
import com.android.contacts.common.model.dataitem.StructuredPostalDataItem;
import com.android.contacts.common.model.dataitem.WebsiteDataItem;
import com.android.contacts.util.PhoneCapabilityTester;
import com.android.contacts.util.StructuredPostalUtils;

// The following lines are provided and maintained by Mediatek Inc.
import android.provider.ContactsContract.Contacts;

import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.extension.aassne.SimUtils;

import java.util.HashMap;
// The previous lines are provided and maintained by Mediatek Inc.

/**
 * Description of a specific {@link Data#_ID} item, with style information
 * defined by a {@link DataKind}.
 */
public class DataAction implements Action {
    private static final String TAG = "DataAction";

    private final Context mContext;
    private final DataKind mKind;
    private final String mMimeType;

    private CharSequence mBody;
    private CharSequence mSubtitle;
    private Intent mIntent;
    private Intent mAlternateIntent;
    private int mAlternateIconDescriptionRes;
    private int mAlternateIconRes;
    private int mPresence = -1;

    private Uri mDataUri;
    private long mDataId;
    private boolean mIsPrimary;

    /**
     * Create an action from common {@link Data} elements.
     */
    public DataAction(Context context, DataItem item, DataKind kind) {
        mContext = context;
        mKind = kind;
        mMimeType = item.getMimeType();

      
        /*
         * New Feature by Mediatek Begin.
         *   Original Android's code:
         *     
         *   CR ID: ALPS00308657
         *   Descriptions: RCS
         */
        String exMimeType = null;
        boolean pluginStatus = ExtensionManager.getInstance().getContactDetailExtension()
                .checkPluginSupport(ExtensionManager.COMMD_FOR_RCS);
        if (pluginStatus) {
            exMimeType = ExtensionManager.getInstance().getContactDetailExtension()
                    .getExtentionMimeType(ExtensionManager.COMMD_FOR_RCS);
        }
        /*
         * New Feature by Mediatek End.
         */
        
        // Determine type for subtitle
        mSubtitle = "";
        if (item.hasKindTypeColumn(kind)) {
            final int typeValue = item.getKindTypeColumn(kind);
            Log.d(TAG, "type value" + typeValue);
                // get type string
                /** M:AAS @ { */
            if (SimUtils.isAasPhoneType(typeValue)) {
                int slotId = ExtensionManager.getInstance().getContactAccountExtension()
                        .getCurrentSlot(ExtensionManager.COMMD_FOR_AAS);
                Log.d(TAG, "slotId: " +slotId);
                mSubtitle = (String) ExtensionManager.getInstance().getContactAccountExtension()
                        .getTypeLabel(
                                context.getResources(), typeValue,
                                item.getContentValues().getAsString(Data.DATA3), slotId,
                                ExtensionManager.COMMD_FOR_AAS);
            } else {
                /** M: @ } */
                for (EditType type : kind.typeList) {
                    if (type.rawValue == typeValue) {
                        if (type.customColumn == null) {
                            // Non-custom type. Get its description from the
                            // resource
                            mSubtitle = context.getString(type.labelRes);
                        } else {
                            // Custom type. Read it from the database
                            mSubtitle = item.getContentValues().getAsString(type.customColumn);
                        }
                        break;
                    }
                }
            }

        }


        mIsPrimary = item.isSuperPrimary();
        mBody = item.buildDataStringForDisplay(context, kind);

        mDataId = item.getId();
        mDataUri = ContentUris.withAppendedId(Data.CONTENT_URI, mDataId);

        final boolean hasPhone = PhoneCapabilityTester.isPhone(mContext);
        final ComponentName smsComponent = PhoneCapabilityTester.getSmsComponent(mContext);
        final boolean hasSms = (smsComponent != null);

        // Handle well-known MIME-types with special care
        if (item instanceof PhoneDataItem) {
            if (PhoneCapabilityTester.isPhone(mContext)) {
                PhoneDataItem phone = (PhoneDataItem) item;
                final String number = phone.getNumber();
                if (!TextUtils.isEmpty(number) && !TextUtils.isEmpty(mSubtitle)) {
                    /*
                     * New Feature by Mediatek Begin.
                     *   Original Android's code:
                     *     
                     *   CR ID: ALPS00308657
                     *   Descriptions: RCS
                     */
                    
                    sPhoneAndSubtitle.put(number, mSubtitle.toString());
                    /*
                     * New Feature by Mediatek End.
                     */
                    final Intent phoneIntent = hasPhone ? CallUtil.getCallIntent(number)
                            : null;
                    Intent smsIntent = null;
                    if (hasSms) {
                        smsIntent = new Intent(Intent.ACTION_SENDTO,
                                Uri.fromParts(CallUtil.SCHEME_SMSTO, number, null));
                        smsIntent.setComponent(smsComponent);
                    }

                    // Configure Icons and Intents. Notice actionIcon is already set to the phone
                    if (hasPhone && hasSms) {
                        mIntent = phoneIntent;
                        mAlternateIntent = smsIntent;
                        mAlternateIconRes = kind.iconAltRes;
                        mAlternateIconDescriptionRes = kind.iconAltDescriptionRes;
                    } else if (hasPhone) {
                        mIntent = phoneIntent;
                    } else if (hasSms) {
                        mIntent = smsIntent;
                    }
                }
            }
        } else if (item instanceof SipAddressDataItem) {
            if (PhoneCapabilityTester.isSipPhone(mContext)) {
                final SipAddressDataItem sip = (SipAddressDataItem) item;
                final String address = sip.getSipAddress();
                if (!TextUtils.isEmpty(address)) {
                    final Uri callUri = Uri.fromParts(CallUtil.SCHEME_SIP, address, null);
                    mIntent = CallUtil.getCallIntent(callUri);
                    // Note that this item will get a SIP-specific variant
                    // of the "call phone" icon, rather than the standard
                    // app icon for the Phone app (which we show for
                    // regular phone numbers.)  That's because the phone
                    // app explicitly specifies an android:icon attribute
                    // for the SIP-related intent-filters in its manifest.
                }
            }
        } else if (item instanceof EmailDataItem) {
            final EmailDataItem email = (EmailDataItem) item;
            final String address = email.getData();
            if (!TextUtils.isEmpty(address)) {
                final Uri mailUri = Uri.fromParts(CallUtil.SCHEME_MAILTO, address, null);
                mIntent = new Intent(Intent.ACTION_SENDTO, mailUri);
            }

        } else if (item instanceof WebsiteDataItem) {
            final WebsiteDataItem website = (WebsiteDataItem) item;
            final String url = website.getUrl();
            if (!TextUtils.isEmpty(url)) {
                WebAddress webAddress = new WebAddress(url);
                mIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webAddress.toString()));
            }

        } else if (item instanceof ImDataItem) {
            ImDataItem im = (ImDataItem) item;
            final boolean isEmail = im.isCreatedFromEmail();
            if (isEmail || im.isProtocolValid()) {
                final int protocol = isEmail ? Im.PROTOCOL_GOOGLE_TALK : im.getProtocol();

                if (isEmail) {
                    // Use Google Talk string when using Email, and clear data
                    // Uri so we don't try saving Email as primary.
                    mSubtitle = Im.getProtocolLabel(context.getResources(), Im.PROTOCOL_GOOGLE_TALK,
                            null);
                    mDataUri = null;
                }

                String host = im.getCustomProtocol();
                String data = im.getData();
                if (protocol != Im.PROTOCOL_CUSTOM) {
                    // Try bringing in a well-known host for specific protocols
                    host = ContactsUtils.lookupProviderNameFromId(protocol);
                }

                if (!TextUtils.isEmpty(host) && !TextUtils.isEmpty(data)) {
                    final String authority = host.toLowerCase();
                    final Uri imUri = new Uri.Builder().scheme(CallUtil.SCHEME_IMTO).authority(
                            authority).appendPath(data).build();
                    mIntent = new Intent(Intent.ACTION_SENDTO, imUri);

                    // If the address is also available for a video chat, we'll show the capability
                    // as a secondary action.
                    final int chatCapability = im.getChatCapability();
                    final boolean isVideoChatCapable =
                            (chatCapability & Im.CAPABILITY_HAS_CAMERA) != 0;
                    final boolean isAudioChatCapable =
                            (chatCapability & Im.CAPABILITY_HAS_VOICE) != 0;
                    if (isVideoChatCapable || isAudioChatCapable) {
                        mAlternateIntent = new Intent(
                                Intent.ACTION_SENDTO, Uri.parse("xmpp:" + data + "?call"));
                        if (isVideoChatCapable) {
                            mAlternateIconRes = R.drawable.sym_action_videochat_holo_light;
                            mAlternateIconDescriptionRes = R.string.video_chat;
                        } else {
                            mAlternateIconRes = R.drawable.sym_action_audiochat_holo_light;
                            mAlternateIconDescriptionRes = R.string.audio_chat;
                        }
                    }
                }
            }
        } else if (item instanceof StructuredPostalDataItem) {
            StructuredPostalDataItem postal = (StructuredPostalDataItem) item;
            final String postalAddress = postal.getFormattedAddress();
            if (!TextUtils.isEmpty(postalAddress)) {
                mIntent = StructuredPostalUtils.getViewPostalAddressIntent(postalAddress);
            }
            /*
             * New Feature by Mediatek Begin. Original Android's code: CR ID:
             * ALPS00308657 Descriptions: RCS
             */
        } else if (exMimeType != null && exMimeType.equals(item.getMimeType())) {
            String number = item.getContentValues().getAsString(Data.DATA1);
            mSubtitle = sPhoneAndSubtitle.get(number);
            int im = item.getContentValues().getAsInteger(Data.DATA5);
            int ft = item.getContentValues().getAsInteger(Data.DATA6);
            Log.i(TAG, "mSubtitle : " + mSubtitle + " | number : " + number);
            Intent intent = ExtensionManager.getInstance().getContactDetailExtension()
                    .getExtentionIntent(im, ft, ExtensionManager.COMMD_FOR_RCS);
            Log.i(TAG, "intent : " + intent + " | exMimeType : " + exMimeType);
            if (intent != null) {
                String name = item.getContentValues().getAsString(Contacts.DISPLAY_NAME);
                Log.i(TAG, "name : " + name + " | number : " + number);
                intent.putExtra(RCS_PHONE_NUMBER, number);
                intent.putExtra(RCS_DISPLAY_NAME, name);
                mIntent = intent;
            }
        }
        /*
         * New Feature by Mediatek End.
         */
        
        if (mIntent == null) {
            // Otherwise fall back to default VIEW action
            mIntent = new Intent(Intent.ACTION_VIEW);
            mIntent.setDataAndType(mDataUri, item.getMimeType());
        }

        mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    }

    @Override
    public int getPresence() {
        return mPresence;
    }

    public void setPresence(int presence) {
        mPresence = presence;
    }

    @Override
    public CharSequence getSubtitle() {
        return mSubtitle;
    }

    @Override
    public CharSequence getBody() {
        return mBody;
    }

    @Override
    public String getMimeType() {
        return mMimeType;
    }

    @Override
    public Uri getDataUri() {
        return mDataUri;
    }

    @Override
    public long getDataId() {
        return mDataId;
    }

    @Override
    public Boolean isPrimary() {
        return mIsPrimary;
    }

    @Override
    public Drawable getAlternateIcon() {
        if (mAlternateIconRes == 0) return null;

        final String resourcePackageName = mKind.resourcePackageName;
        if (resourcePackageName == null) {
            return mContext.getResources().getDrawable(mAlternateIconRes);
        }

        final PackageManager pm = mContext.getPackageManager();
        return pm.getDrawable(resourcePackageName, mAlternateIconRes, null);
    }

    @Override
    public String getAlternateIconDescription() {
        if (mAlternateIconDescriptionRes == 0) return null;
        return mContext.getResources().getString(mAlternateIconDescriptionRes);
    }

    @Override
    public Intent getIntent() {
        return mIntent;
    }

    @Override
    public Intent getAlternateIntent() {
        return mAlternateIntent;
    }

    @Override
    public void collapseWith(Action other) {
        // No-op
    }

    @Override
    public boolean shouldCollapseWith(Action t) {
        if (t == null) {
            return false;
        }
        if (!(t instanceof DataAction)) {
            Log.e(TAG, "t must be DataAction");
            return false;
        }
        DataAction that = (DataAction)t;
        if (!MoreContactUtils.shouldCollapse(mMimeType, mBody, that.mMimeType, that.mBody)) {
            return false;
        }
        if (!TextUtils.equals(mMimeType, that.mMimeType)
                || !ContactsUtils.areIntentActionEqual(mIntent, that.mIntent)) {
            return false;
        }
        return true;
    }

    // The following lines are provided and maintained by Mediatek Inc.
    private static HashMap<String, String> sPhoneAndSubtitle = new HashMap<String, String>();
    public static final String RCS_DISPLAY_NAME = "rcs_display_name";
    public static final String RCS_PHONE_NUMBER = "rcs_phone_number";
    // The previous lines are provided and maintained by Mediatek Inc.
    
}
