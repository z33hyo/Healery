/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Lem Dulfo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities.appmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.GBDeviceAppAdapter;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;


public abstract class AbstractAppManagerFragment extends Fragment {
    public static final String ACTION_REFRESH_APPLIST
            = "nodomain.freeyourgadget.gadgetbridge.appmanager.action.refresh_applist";
    private static final Logger LOG = LoggerFactory.getLogger(AbstractAppManagerFragment.class);

    private ItemTouchHelper appManagementTouchHelper;

    protected abstract List<GBDeviceApp> getSystemAppsInCategory();

    protected abstract String getSortFilename();

    protected abstract boolean isCacheManager();

    protected abstract boolean filterApp(GBDeviceApp gbDeviceApp);

    public void startDragging(RecyclerView.ViewHolder viewHolder) {
        appManagementTouchHelper.startDrag(viewHolder);
    }

    protected void onChangedAppOrder() {
        List<UUID> uuidList = new ArrayList<>();
        for (GBDeviceApp gbDeviceApp : mGBDeviceAppAdapter.getAppList()) {
            uuidList.add(gbDeviceApp.getUUID());
        }
        AppManagerActivity.rewriteAppOrderFile(getSortFilename(), uuidList);
    }

    protected void refreshList() {
        appList.clear();
        ArrayList uuids = AppManagerActivity.getUuidsFromFile(getSortFilename());
        List<GBDeviceApp> systemApps = getSystemAppsInCategory();
        boolean needsRewrite = false;
        for (GBDeviceApp systemApp : systemApps) {
            if (!uuids.contains(systemApp.getUUID())) {
                uuids.add(systemApp.getUUID());
                needsRewrite = true;
            }
        }
        if (needsRewrite) {
            AppManagerActivity.rewriteAppOrderFile(getSortFilename(), uuids);
        }
        appList.addAll(getCachedApps(uuids));
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) { }
    };

    protected final List<GBDeviceApp> appList = new ArrayList<>();
    private GBDeviceAppAdapter mGBDeviceAppAdapter;
    protected GBDevice mGBDevice = null;

    protected List<GBDeviceApp> getCachedApps(List<UUID> uuids) {
        List<GBDeviceApp> cachedAppList = new ArrayList<>();

        return cachedAppList;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mGBDevice = ((AppManagerActivity) getActivity()).getGBDevice();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_REFRESH_APPLIST);

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, filter);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final FloatingActionButton appListFab = ((FloatingActionButton) getActivity().findViewById(R.id.fab));
        View rootView = inflater.inflate(R.layout.activity_appmanager, container, false);

        RecyclerView appListView = (RecyclerView) (rootView.findViewById(R.id.appListView));

        appListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    appListFab.hide();
                } else if (dy < 0) {
                    appListFab.show();
                }
            }
        });
        appListView.setLayoutManager(new LinearLayoutManager(getActivity()));

        ItemTouchHelper.Callback appItemTouchHelperCallback = new AppItemTouchHelperCallback(mGBDeviceAppAdapter);
        appManagementTouchHelper = new ItemTouchHelper(appItemTouchHelperCallback);

        appManagementTouchHelper.attachToRecyclerView(appListView);
        return rootView;
    }

    public boolean openPopupMenu(View view, GBDeviceApp deviceApp) {
        PopupMenu popupMenu = new PopupMenu(getContext(), view);
        popupMenu.getMenuInflater().inflate(R.menu.appmanager_context, popupMenu.getMenu());
        Menu menu = popupMenu.getMenu();
        final GBDeviceApp selectedApp = deviceApp;

        if (!selectedApp.isInCache()) {
            menu.removeItem(R.id.appmanager_app_reinstall);
            menu.removeItem(R.id.appmanager_app_delete_cache);
        }
        if (selectedApp.getType() == GBDeviceApp.Type.APP_SYSTEM || selectedApp.getType() == GBDeviceApp.Type.WATCHFACE_SYSTEM) {
            menu.removeItem(R.id.appmanager_app_delete);
        }
        if (!selectedApp.isConfigurable()) {
            menu.removeItem(R.id.appmanager_app_configure);
        }


        switch (selectedApp.getType()) {
            case WATCHFACE:
            case APP_GENERIC:
            case APP_ACTIVITYTRACKER:
                break;
            default:
                menu.removeItem(R.id.appmanager_app_openinstore);
        }
        //menu.setHeaderTitle(selectedApp.getName());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                                 public boolean onMenuItemClick(MenuItem item) {
                                                     return onContextItemSelected(item, selectedApp);
                                                 }
                                             }
        );

        popupMenu.show();
        return true;
    }

    private boolean onContextItemSelected(MenuItem item, GBDeviceApp selectedApp) {
        switch (item.getItemId()) {
            case R.id.appmanager_health_activate:
                GBApplication.deviceService().onInstallApp(Uri.parse("fake://health"));
                return true;
            case R.id.appmanager_hrm_activate:
                GBApplication.deviceService().onInstallApp(Uri.parse("fake://hrm"));
                return true;
            case R.id.appmanager_health_deactivate:
            case R.id.appmanager_hrm_deactivate:
            case R.id.appmanager_app_configure:
                GBApplication.deviceService().onAppStart(selectedApp.getUUID(), true);

                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    public class AppItemTouchHelperCallback extends ItemTouchHelper.Callback {

        private final GBDeviceAppAdapter gbDeviceAppAdapter;

        public AppItemTouchHelperCallback(GBDeviceAppAdapter gbDeviceAppAdapter) {
            this.gbDeviceAppAdapter = gbDeviceAppAdapter;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {

            //we only support up and down movement and only for moving, not for swiping apps away
            return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
            gbDeviceAppAdapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            //nothing to do
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            onChangedAppOrder();
        }

    }
}
