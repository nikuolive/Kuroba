/*
 * Kuroba - *chan browser https://github.com/Adamantcheese/Kuroba/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.adamantcheese.chan.ui.controller.settings;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.github.adamantcheese.chan.BuildConfig;
import com.github.adamantcheese.chan.R;
import com.github.adamantcheese.chan.StartActivity;
import com.github.adamantcheese.chan.controller.Controller;
import com.github.adamantcheese.chan.core.model.ChanThread;
import com.github.adamantcheese.chan.core.model.Post;
import com.github.adamantcheese.chan.core.model.PostImage;
import com.github.adamantcheese.chan.core.model.PostLinkable;
import com.github.adamantcheese.chan.core.model.orm.Board;
import com.github.adamantcheese.chan.core.model.orm.Loadable;
import com.github.adamantcheese.chan.core.settings.ChanSettings;
import com.github.adamantcheese.chan.core.site.common.DefaultPostParser;
import com.github.adamantcheese.chan.core.site.parser.CommentParser;
import com.github.adamantcheese.chan.core.site.parser.PostParser;
import com.github.adamantcheese.chan.core.site.sites.chan4.Chan4PagesRequest.Page;
import com.github.adamantcheese.chan.ui.adapter.PostAdapter;
import com.github.adamantcheese.chan.ui.cell.PostCell;
import com.github.adamantcheese.chan.ui.cell.ThreadStatusCell;
import com.github.adamantcheese.chan.ui.theme.Theme;
import com.github.adamantcheese.chan.ui.theme.Theme.MaterialColorStyle;
import com.github.adamantcheese.chan.ui.theme.ThemeHelper;
import com.github.adamantcheese.chan.ui.toolbar.NavigationItem;
import com.github.adamantcheese.chan.ui.toolbar.Toolbar;
import com.github.adamantcheese.chan.ui.toolbar.ToolbarMenuItem;
import com.github.adamantcheese.chan.ui.view.FloatingMenu;
import com.github.adamantcheese.chan.ui.view.FloatingMenuItem;
import com.github.adamantcheese.chan.ui.view.ThumbnailView;
import com.github.adamantcheese.chan.ui.view.ViewPagerAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.HttpUrl;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.github.adamantcheese.chan.ui.theme.ThemeHelper.createTheme;
import static com.github.adamantcheese.chan.utils.AndroidUtils.dp;
import static com.github.adamantcheese.chan.utils.AndroidUtils.getContrastColor;
import static com.github.adamantcheese.chan.utils.AndroidUtils.getDimen;
import static com.github.adamantcheese.chan.utils.AndroidUtils.getString;
import static com.github.adamantcheese.chan.utils.AndroidUtils.resolveColor;
import static com.github.adamantcheese.chan.utils.LayoutUtils.inflate;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

public class ThemeSettingsController
        extends Controller {

    private Board dummyBoard = new Board();
    private Loadable dummyLoadable = Loadable.emptyLoadable();

    {
        dummyBoard.name = "name";
        dummyBoard.code = "code";
        dummyLoadable.mode = Loadable.Mode.THREAD;
    }

    private PostCell.PostCellCallback dummyPostCallback = new PostCell.PostCellCallback() {
        @Override
        public Loadable getLoadable() {
            return dummyLoadable;
        }

        @Override
        public void onPostClicked(Post post) {
        }

        @Override
        public void onPostDoubleClicked(Post post) {
        }

        @Override
        public void onThumbnailClicked(PostImage postImage, ThumbnailView thumbnail) {
        }

        @Override
        public void onShowPostReplies(Post post) {
        }

        @Override
        public Object onPopulatePostOptions(Post post, List<FloatingMenuItem> menu, List<FloatingMenuItem> extraMenu) {
            menu.add(new FloatingMenuItem(1, "Option"));
            return 0;
        }

        @Override
        public void onPostOptionClicked(Post post, Object id, boolean inPopup) {
        }

        @Override
        public void onPostLinkableClicked(Post post, PostLinkable linkable) {
        }

        @Override
        public void onPostNoClicked(Post post) {
        }

        @Override
        public void onPostSelectionQuoted(Post post, CharSequence quoted) {
        }

        @Override
        public Page getPage(Post op) {
            return null;
        }
    };

    private PostParser.Callback parserCallback = new PostParser.Callback() {
        @Override
        public boolean isSaved(int postNo) {
            return false;
        }

        @Override
        public boolean isInternal(int postNo) {
            return true;
        }
    };

    private ViewPager pager;
    private FloatingActionButton done;

    private List<Theme> themes;
    private MaterialColorStyle currentPrimaryColor;
    private MaterialColorStyle currentAccentColor;

    public ThemeSettingsController(Context context) {
        super(context);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        navigation.setTitle(R.string.settings_screen_theme);
        navigation.swipeable = false;
        navigation.buildMenu().withItem(R.drawable.ic_help_outline_white_24dp, this::helpClicked).build();
        view = inflate(context, R.layout.controller_theme);

        themes = ThemeHelper.getThemes();
        Theme currentTheme = ThemeHelper.getTheme();
        // restore these if the user pressed back instead of the default theme color
        currentPrimaryColor = currentTheme.primaryColor;
        currentAccentColor = currentTheme.accentColor;
        // setup all themes to contain the current accent color
        for (int i = 0; i < themes.size(); i++) {
            Theme theme = themes.get(i);
            theme.accentColor = currentAccentColor;
        }

        pager = view.findViewById(R.id.pager);
        done = view.findViewById(R.id.add);
        done.setOnClickListener(v -> saveTheme());

        pager.setAdapter(new Adapter());
        pager.setPageMargin(dp(6));
        for (int i = 0; i < themes.size(); i++) {
            Theme theme = themes.get(i);
            if (theme.name.equals(currentTheme.name)) {
                // Current theme
                pager.setCurrentItem(i, false);
                break;
            }
        }
    }

    @Override
    public boolean onBack() {
        ThemeHelper.resetThemes();
        ThemeHelper.getTheme().primaryColor = currentPrimaryColor;
        ThemeHelper.getTheme().accentColor = currentAccentColor;
        return super.onBack();
    }

    private Theme getViewedTheme() {
        return themes.get(pager.getCurrentItem());
    }

    private void saveTheme() {
        ChanSettings.theme.setSync(getViewedTheme().toString());
        ((StartActivity) context).restartApp();
    }

    private void helpClicked(ToolbarMenuItem item) {
        final AlertDialog dialog = new AlertDialog.Builder(context).setTitle("Help")
                .setMessage(R.string.setting_theme_explanation)
                .setPositiveButton("Close", null)
                .show();
        dialog.setCanceledOnTouchOutside(true);
    }

    private void showAccentColorPicker() {
        List<FloatingMenuItem> items = new ArrayList<>();
        FloatingMenuItem selected = null;
        for (MaterialColorStyle color : MaterialColorStyle.values()) {
            FloatingMenuItem floatingMenuItem = new FloatingMenuItem(color, color.prettyName());
            items.add(floatingMenuItem);
            if (color == getViewedTheme().accentColor) {
                selected = floatingMenuItem;
            }
        }

        FloatingMenu menu = getColorsMenu(items, selected, done, true);
        menu.setCallback(new FloatingMenu.FloatingMenuCallback() {
            @Override
            public void onFloatingMenuItemClicked(FloatingMenu menu, FloatingMenuItem item) {
                MaterialColorStyle color = (MaterialColorStyle) item.getId();
                for (int i = 0; i < themes.size(); i++) {
                    Theme theme = themes.get(i);
                    theme.accentColor = color;
                }
                done.setBackgroundTintList(ColorStateList.valueOf(resolveColor(color.accentStyleId,
                        R.attr.colorAccent
                )));
                //forcibly refresh all items
                Theme currentTheme = getViewedTheme();
                pager.setAdapter(null);
                pager.setAdapter(new Adapter());
                for (int i = 0; i < themes.size(); i++) {
                    Theme theme = themes.get(i);
                    if (theme.name.equals(currentTheme.name)) {
                        // Current theme
                        pager.setCurrentItem(i, false);
                        break;
                    }
                }
            }

            @Override
            public void onFloatingMenuDismissed(FloatingMenu menu) {

            }
        });
        menu.setPopupHeight(dp(300));
        menu.show();
    }

    private FloatingMenu getColorsMenu(
            List<FloatingMenuItem> items, FloatingMenuItem selected, View anchor, boolean useAccentColors
    ) {
        FloatingMenu menu = new FloatingMenu(context);

        menu.setItems(items);
        menu.setAdapter(new ColorsAdapter(items, useAccentColors));
        menu.setSelectedItem(selected);
        menu.setAnchor(anchor, Gravity.CENTER, 0, 0);
        return menu;
    }

    private class Adapter
            extends ViewPagerAdapter {
        public Adapter() {
        }

        @Override
        public View getView(final int position, ViewGroup parent) {
            final Theme theme = themes.get(position);

            Context themeContext = new ContextThemeWrapper(context, theme.resValue);
            themeContext.getTheme().setTo(createTheme(theme));

            CommentParser parser = new CommentParser().addDefaultRules();
            DefaultPostParser postParser = new DefaultPostParser(parser);
            Post.Builder builder1 = new Post.Builder().board(dummyBoard)
                    .id(123456789)
                    .opId(123456789)
                    .op(true)
                    .replies(1)
                    .setUnixTimestampSeconds(MILLISECONDS.toSeconds(System.currentTimeMillis() - MINUTES.toMillis(30)))
                    .subject("Lorem ipsum")
                    .comment("<span class=\"deadlink\">&gt;&gt;987654321</span><br>" + "http://example.com/<br>"
                            + "This text is normally colored.<br>"
                            + "<span class=\"spoiler\">This text is spoilered.</span><br>"
                            + "<span class=\"quote\">&gt;This text is inline quoted (greentext).</span>")
                    .id(9001);
            builder1.idColor = Color.WHITE;
            Post post1 = postParser.parse(theme, builder1, parserCallback);
            post1.repliesFrom.add(234567890);

            Post.Builder builder2 = new Post.Builder().board(dummyBoard)
                    .id(234567890)
                    .opId(123456789)
                    .name("W.T. Snacks")
                    .tripcode("!TcT.PTG1.2")
                    .setUnixTimestampSeconds(MILLISECONDS.toSeconds(System.currentTimeMillis() - MINUTES.toMillis(15)))
                    .comment(
                            "<a href=\"#p123456789\" class=\"quotelink\">&gt;&gt;123456789</a> This link is marked.<br>"
                                    + "<a href=\\\"#p111111111\\\" class=\\\"quotelink\\\">&gt;&gt;111111111</a><br>"
                                    + "This post is highlighted.")
                    .images(Collections.singletonList(new PostImage.Builder().imageUrl(HttpUrl.get(
                            BuildConfig.RESOURCES_ENDPOINT + "new_icon_512.png"))
                            .thumbnailUrl(HttpUrl.get(BuildConfig.RESOURCES_ENDPOINT + "new_icon_512.png"))
                            .filename("new_icon_512")
                            .extension("png")
                            .build()));
            Post post2 = postParser.parse(theme, builder2, parserCallback);
            List<Post> posts = new ArrayList<>();
            posts.add(post1);
            posts.add(post2);

            LinearLayout linearLayout = new LinearLayout(themeContext);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setBackgroundColor(resolveColor(theme.resValue, R.attr.backcolor));

            RecyclerView postsView = new RecyclerView(themeContext);
            LinearLayoutManager layoutManager = new LinearLayoutManager(themeContext);
            layoutManager.setOrientation(RecyclerView.VERTICAL);
            postsView.setLayoutManager(layoutManager);
            PostAdapter adapter = new PostAdapter(postsView, new PostAdapter.PostAdapterCallback() {
                @Override
                public Loadable getLoadable() {
                    return dummyLoadable;
                }

                @Override
                public void onUnhidePostClick(Post post) {
                }
            }, dummyPostCallback, new ThreadStatusCell.Callback() {
                @Override
                public long getTimeUntilLoadMore() {
                    return 0;
                }

                @Override
                public boolean isWatching() {
                    return false;
                }

                @Nullable
                @Override
                public ChanThread getChanThread() {
                    return null;
                }

                @Override
                public Page getPage(Post op) {
                    return null;
                }

                @Override
                public void onListStatusClicked() {
                    showAccentColorPicker();
                }
            }, theme);
            adapter.setThread(dummyLoadable, posts, false);
            adapter.highlightPost(post2);
            adapter.setPostViewMode(ChanSettings.PostViewMode.LIST);
            adapter.showError(ThreadStatusCell.SPECIAL + getString(R.string.setting_theme_accent));
            adapter.setMarkedForThemeController(123456789);
            postsView.setAdapter(adapter);

            final Toolbar toolbar = new Toolbar(themeContext);
            final View.OnClickListener colorClick = v -> {
                List<FloatingMenuItem> items = new ArrayList<>();
                FloatingMenuItem selected = null;
                for (MaterialColorStyle color : MaterialColorStyle.values()) {
                    FloatingMenuItem floatingMenuItem = new FloatingMenuItem(color, color.prettyName());
                    items.add(floatingMenuItem);
                    if (color == theme.primaryColor) {
                        selected = floatingMenuItem;
                    }
                }

                FloatingMenu menu = getColorsMenu(items, selected, toolbar, false);
                menu.setCallback(new FloatingMenu.FloatingMenuCallback() {
                    @Override
                    public void onFloatingMenuItemClicked(FloatingMenu menu, FloatingMenuItem item) {
                        MaterialColorStyle color = (MaterialColorStyle) item.getId();
                        theme.primaryColor = color;
                        toolbar.setBackgroundColor(resolveColor(color.primaryColorStyleId, R.attr.colorPrimary));
                    }

                    @Override
                    public void onFloatingMenuDismissed(FloatingMenu menu) {
                    }
                });
                menu.setPopupHeight(dp(300));
                menu.show();
            };
            toolbar.setCallback(new Toolbar.ToolbarCallback() {
                @Override
                public void onMenuOrBackClicked(boolean isArrow) {
                    colorClick.onClick(toolbar);
                }

                @Override
                public void onSearchVisibilityChanged(NavigationItem item, boolean visible) {
                }

                @Override
                public void onSearchEntered(NavigationItem item, String entered) {
                }
            });
            final NavigationItem item = new NavigationItem();
            item.title = theme.name;
            item.hasBack = false;
            toolbar.setNavigationItem(false, true, item, theme);
            toolbar.setOnClickListener(colorClick);
            toolbar.setTag(theme.name);

            linearLayout.addView(toolbar, new LayoutParams(MATCH_PARENT, getDimen(R.dimen.toolbar_height)));
            linearLayout.addView(postsView, new LayoutParams(MATCH_PARENT, WRAP_CONTENT));

            return linearLayout;
        }

        @Override
        public int getCount() {
            return themes.size();
        }
    }

    private static class ColorsAdapter
            extends BaseAdapter {
        private List<FloatingMenuItem> items;
        private boolean useAccentColors;

        public ColorsAdapter(List<FloatingMenuItem> items, boolean useAccentColors) {
            this.items = items;
            this.useAccentColors = useAccentColors;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            @SuppressLint("ViewHolder")
            TextView textView = (TextView) inflate(parent.getContext(), R.layout.toolbar_menu_item, parent, false);
            textView.setText(getItem(position));
            textView.setTypeface(ThemeHelper.getTheme().mainFont);

            MaterialColorStyle color = (MaterialColorStyle) items.get(position).getId();

            int colorForItem = useAccentColors
                    ? resolveColor(color.accentStyleId, R.attr.colorAccent)
                    : resolveColor(color.primaryColorStyleId, R.attr.colorPrimary);
            textView.setBackgroundColor(colorForItem);
            textView.setTextColor(getContrastColor(colorForItem));

            return textView;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public String getItem(int position) {
            return items.get(position).getText();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }
}
