/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.tv.menu;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.TvContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;

import com.android.tv.R;
import com.android.tv.TvSettings;

public class EditChannelsDialogFragment extends DialogFragment {
    public static final String DIALOG_TAG = EditChannelsDialogFragment.class.getName();

    public static final String ARG_CURRENT_SERVICE_NAME = "current_service_name";
    public static final String ARG_CURRENT_PACKAGE_NAME = "current_package_name";

    private static final String TAG = EditChannelsDialogFragment.class.getSimpleName();

    private String mServiceName;
    private SimpleCursorAdapter mAdapter;

    private View mView;
    private ListView mListView;

    private int mIndexDisplayNumber;
    private int mIndexDisplayName;
    private int mIndexBrowsable;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arg = getArguments();
        assert(arg != null);

        String packageName = arg.getString(ARG_CURRENT_PACKAGE_NAME);
        mServiceName = arg.getString(ARG_CURRENT_SERVICE_NAME);
        assert(packageName != null && mServiceName != null);

        ComponentName name = new ComponentName(packageName, mServiceName);

        String displayName = getActivity()
                .getSharedPreferences(TvSettings.PREFS_FILE, Context.MODE_PRIVATE)
                .getString(TvSettings.PREF_DISPLAY_INPUT_NAME + packageName + "/" + mServiceName,
                        null);

        if (TextUtils.isEmpty(displayName)) {
            try {
                PackageManager pm = getActivity().getPackageManager();
                ServiceInfo info = pm.getServiceInfo(name, 0);
                displayName = String.valueOf(info.loadLabel(pm));
            } catch (NameNotFoundException e) {
                Log.e(TAG, "The service '" + name + "' is not found.", e);
                displayName = mServiceName;
            }
        }

        String title = String.format(getString(R.string.edit_channels_title), displayName);
        mView = LayoutInflater.from(getActivity()).inflate(R.layout.edit_channels, null);
        initButtons();
        initListView();

        return new AlertDialog.Builder(getActivity())
                .setView(mView)
                .setTitle(title)
                .create();
    }

    private void initButtons() {
        Button button = (Button) mView.findViewById(R.id.button_enable);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                updateAllChannels(true);
            }
        });

        button = (Button) mView.findViewById(R.id.button_disable);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                updateAllChannels(false);
            }
        });
    }

    private void initListView() {
        getLoaderManager().initLoader(0, null, new LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                Uri uri = TvContract.Channels.CONTENT_URI;
                String[] projections = { TvContract.Channels._ID,
                        TvContract.Channels.DISPLAY_NUMBER,
                        TvContract.Channels.DISPLAY_NAME,
                        TvContract.Channels.BROWSABLE};
                String selection = TvContract.Channels.SERVICE_NAME + " = ?";
                String[] selectArgs = { mServiceName };

                return new CursorLoader(
                        getActivity(),
                        uri,
                        projections,
                        selection,
                        selectArgs,
                        null);
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
                mIndexDisplayNumber = cursor.getColumnIndex(TvContract.Channels.DISPLAY_NUMBER);
                mIndexDisplayName = cursor.getColumnIndex(TvContract.Channels.DISPLAY_NAME);
                mIndexBrowsable = cursor.getColumnIndex(TvContract.Channels.BROWSABLE);

                mAdapter.changeCursor(cursor);
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
                mAdapter.changeCursor(null);
            }
        });

        // TODO: need to show logo when TvProvider supports logo-related field.
        String[] from = {TvContract.Channels.DISPLAY_NAME};
        int[] to = {R.id.channel_text_view};

        mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.edit_channels_item, null, from,
                to, 0);
        mAdapter.setViewBinder(new ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (columnIndex == mIndexDisplayName) {
                    String channelNumber = cursor.getString(mIndexDisplayNumber);
                    String channelName = cursor.getString(mIndexDisplayName);
                    String channelString;
                    if (TextUtils.isEmpty(channelName)) {
                        channelString = channelNumber;
                    } else {
                        channelString = String.format(getString(R.string.channel_item),
                                channelNumber, channelName);
                    }
                    CheckedTextView checkedTextView = (CheckedTextView) view;
                    checkedTextView.setText(channelString);
                    checkedTextView.setChecked(cursor.getInt(mIndexBrowsable) == 1);
                }
                return true;
            }
        });

        mListView = (ListView) mView.findViewById(android.R.id.list);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setAdapter(mAdapter);
        mListView.setEmptyView(mView.findViewById(R.id.empty));

        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckedTextView checkedTextView =
                        (CheckedTextView) view.findViewById(R.id.channel_text_view);
                boolean checked = checkedTextView.isChecked();
                int fromTop = view.getTop();

                ContentValues values = new ContentValues();
                values.put(TvContract.Channels.BROWSABLE, checked ? 0 : 1);

                Uri uri = Uri.parse(TvContract.Channels.CONTENT_URI + "/" + id);
                getActivity().getContentResolver().update(uri, values, null, null);

                mListView.setSelectionFromTop(position, fromTop);
            }
        });
    }

    private void updateAllChannels(boolean browsable) {
        if (mAdapter == null || mAdapter.getCursor() == null) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(TvContract.Channels.BROWSABLE, browsable ? 1 : 0);
        String where = TvContract.Channels.SERVICE_NAME + " = ?";
        String[] selectionArgs = { mServiceName };

        getActivity().getContentResolver().update(
                TvContract.Channels.CONTENT_URI,
                values,
                where,
                selectionArgs);
    }
}