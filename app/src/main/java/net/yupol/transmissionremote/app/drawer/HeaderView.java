package net.yupol.transmissionremote.app.drawer;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.view.BezelImageView;

import net.yupol.transmissionremote.app.R;
import net.yupol.transmissionremote.app.TransmissionRemote;
import net.yupol.transmissionremote.app.server.Server;
import net.yupol.transmissionremote.app.utils.ColorUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class HeaderView extends RelativeLayout implements View.OnClickListener {

    private static final String TAG = HeaderView.class.getSimpleName();

    private static final float CIRCLE_TEXT_PADDING_RATIO = 0.35f;

    private static final int DRAWER_ITEM_ID_ADD_SERVER = -101;
    private static final int DRAWER_ITEM_ID_MANAGE_SERVERS = -102;

    private static final String KEY_ORDERED_SERVERS = "key_ordered_servers";

    private Drawer drawer;
    private List<Server> servers;

    private final TextView nameText;
    private final BezelImageView serverCircleCurrent;
    private final BezelImageView serverCircleSmallFirst;
    private final BezelImageView serverCircleSmallSecond;

    private boolean serverListExpanded = false;

    private final Server[] serversInCircles = new Server[3];

    private HeaderListener listener;

    private List<IDrawerItem<?>> serverSelectionItems;

    private final int secondaryTextColor;

    private final Drawer.OnDrawerItemClickListener drawerItemClickListener = new Drawer.OnDrawerItemClickListener() {
        @Override
        public boolean onItemClick(View view, int position, @NonNull IDrawerItem<?> drawerItem) {
            if (listener == null) return false;

            int id = (int) drawerItem.getIdentifier();
            switch (id) {
                case DRAWER_ITEM_ID_ADD_SERVER:
                    listener.onAddServerPressed();
                    return true;
                case DRAWER_ITEM_ID_MANAGE_SERVERS:
                    listener.onManageServersPressed();
                    return true;
            }

            if (id >=0 && id < servers.size()) {
                listener.onServerSelected(servers.get(id));
                drawer.closeDrawer();
                return true;
            }

            return false;
        }
    };

    private final Drawer.OnDrawerItemLongClickListener drawerItemLongClickListener = (view, position, iDrawerItem) -> false;
    private final ImageView serverListButton;

    public HeaderView(Context context) {
        this(context, null);
    }

    public HeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.drawer_header, this);

        secondaryTextColor = ColorUtils.resolveColor(context, android.R.attr.textColorSecondary, R.color.text_secondary);

        nameText = findViewById(R.id.name_text);

        serverListButton = findViewById(R.id.server_list_button);
        serverListButton.setImageResource(serverListExpanded ? R.drawable.ic_arrow_drop_up : R.drawable.ic_arrow_drop_down);
        View serverTextSection = findViewById(R.id.header_text_section);
        serverTextSection.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serverListExpanded) {
                    hideServersList();
                } else {
                    showServersList();
                }
            }
        });

        ImageButton settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onSettingsPressed();
            }
        });

        serverCircleCurrent = findViewById(R.id.circle_current);
        serverCircleCurrent.setOnClickListener(this);
        serverCircleSmallFirst = findViewById(R.id.circle_1);
        serverCircleSmallFirst.setOnClickListener(this);
        serverCircleSmallSecond = findViewById(R.id.circle_2);
        serverCircleSmallSecond.setOnClickListener(this);

        ((LayoutParams) serverCircleCurrent.getLayoutParams()).topMargin += getStatusBarHeight();
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public void setServers(List<Server> servers, int currentServerPosition) {
        this.servers = servers;
        if (currentServerPosition < 0) {
            Arrays.fill(serversInCircles, null);
            return;
        }

        TransmissionRemote app = TransmissionRemote.getApplication(getContext());
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        List<String> orderedServers;
        if (sp.contains(KEY_ORDERED_SERVERS)) {
            orderedServers = serversFromJson(sp.getString(KEY_ORDERED_SERVERS, ""));
            // remove servers which are not in server list from ordered server list
            orderedServers.removeIf(s -> app.getServerById(s) == null);
            // add new servers to ordered server list
            for (Server server : servers) {
                if (!orderedServers.contains(server.getId())) {
                    orderedServers.add(server.getId());
                }
            }
        } else {
            orderedServers = servers.stream().map(Server::getId).collect(Collectors.toCollection(LinkedList::new));
        }

        String currentServerId = servers.get(currentServerPosition).getId();
        orderedServers.remove(currentServerId);
        orderedServers.add(0, currentServerId);

        SharedPreferences.Editor editor = sp.edit();
        editor.putString(KEY_ORDERED_SERVERS, serversToJson(orderedServers));
        editor.apply();

        Arrays.fill(serversInCircles, null);
        int i = 0;
        Iterator<String> it = orderedServers.iterator();
        while (it.hasNext() && i < serversInCircles.length) {
            serversInCircles[i++] = app.getServerById(it.next());
        }

        nameText.setText(serversInCircles[0].getName());

        updateServerCircles();

        buildServerSelectionDrawerItems();
        if (drawer.switchedDrawerContent()) {
            showServersList(); // show updated server list
        }
    }

    private String serversToJson(List<String> servers) {
        return new JSONArray(servers).toString();
    }

    private List<String> serversFromJson(String serversJson) {
        List<String> servers = new LinkedList<>();
        try {
            JSONArray serversArray = new JSONArray(serversJson);
            for (int i=0; i<serversArray.length(); i++) {
                servers.add(serversArray.getString(i));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse servers list");
        }
        return servers;
    }

    private void updateServerCircles() {
        if (serversInCircles[0] != null) {
            serverCircleCurrent.setImageDrawable(serverDrawable(serversInCircles[0].getName()));
            serverCircleCurrent.setVisibility(VISIBLE);
        } else {
            serverCircleCurrent.setVisibility(GONE);
        }

        if (serversInCircles[1] != null) {
            serverCircleSmallFirst.setImageDrawable(serverDrawable(serversInCircles[1].getName()));
            serverCircleSmallFirst.setVisibility(VISIBLE);
        } else {
            serverCircleSmallFirst.setVisibility(GONE);
        }

        if (serversInCircles[2] != null) {
            serverCircleSmallSecond.setImageDrawable(serverDrawable(serversInCircles[2].getName()));
            serverCircleSmallSecond.setVisibility(VISIBLE);
        } else {
            serverCircleSmallSecond.setVisibility(GONE);
        }
    }

    @NonNull
    private ServerDrawable serverDrawable(String name) {
        return new ServerDrawable(name,
                ContextCompat.getColor(getContext(), R.color.drawer_header_circle_background),
                ContextCompat.getColor(getContext(), R.color.drawer_header_circle_foreground),
                CIRCLE_TEXT_PADDING_RATIO);
    }

    private void buildServerSelectionDrawerItems() {
        serverSelectionItems = new ArrayList<>(servers.size() + 2);
        for (int i=0; i<servers.size(); i++) {
            Server server = servers.get(i);
            PrimaryDrawerItem serverItem = new PrimaryDrawerItem()
                    .withName(server.getName())
                    .withIdentifier(i)
                    .withDescription(server.getHost() + (server.getPort() >= 0 ? ":" + server.getPort() : ""))
                    .withDescriptionTextColor(secondaryTextColor);
            serverSelectionItems.add(serverItem);
        }

        PrimaryDrawerItem addItem = new PrimaryDrawerItem()
                .withName(R.string.add_server)
                .withIcon(R.drawable.ic_add)
                .withSelectable(false)
                .withIdentifier(DRAWER_ITEM_ID_ADD_SERVER);
        serverSelectionItems.add(addItem);

        PrimaryDrawerItem manageItem = new PrimaryDrawerItem()
                .withName(R.string.manage_servers)
                .withIcon(R.drawable.ic_settings)
                .withSelectable(false)
                .withIdentifier(DRAWER_ITEM_ID_MANAGE_SERVERS);
        serverSelectionItems.add(manageItem);
    }

    public void setHeaderListener(HeaderListener listener) {
        this.listener = listener;
    }

    @Override
    public void onClick(View v) {
        if (listener == null) return;

        Server server;
        if (v == serverCircleCurrent) {
            server = serversInCircles[0];
        } else if (v == serverCircleSmallFirst) {
            server = serversInCircles[1];
        } else {
            server = serversInCircles[2];
        }

        listener.onServerSelected(server);
        drawer.closeDrawer();
    }

    public void showServersList() {
        serverListExpanded = true;
        serverListButton.setImageResource(R.drawable.ic_arrow_drop_up);
        drawer.switchDrawerContent(
                drawerItemClickListener,
                drawerItemLongClickListener,
                serverSelectionItems,
                servers.indexOf(serversInCircles[0]) + 1
        );

    }

    private void hideServersList() {
        serverListExpanded = false;
        serverListButton.setImageResource(R.drawable.ic_arrow_drop_down);
        drawer.resetDrawerContent();
    }

    public void setDrawer(Drawer drawer) {
        this.drawer = drawer;
    }

    public interface HeaderListener {
        void onSettingsPressed();
        void onServerSelected(Server server);
        void onAddServerPressed();
        void onManageServersPressed();
    }
}
