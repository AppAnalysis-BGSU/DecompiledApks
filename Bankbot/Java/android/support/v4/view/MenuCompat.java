package android.support.v4.view;

import android.view.MenuItem;

public final class MenuCompat {
    private MenuCompat() {
    }

    @Deprecated
    public static void setShowAsAction(MenuItem menuItem, int i) {
        MenuItemCompat.setShowAsAction(menuItem, i);
    }
}
