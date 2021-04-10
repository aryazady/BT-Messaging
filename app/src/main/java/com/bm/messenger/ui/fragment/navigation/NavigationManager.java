package com.bm.messenger.ui.fragment.navigation;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bm.messenger.R;
import com.bm.messenger.model.LiveDataModel;

public class NavigationManager {

    private final FragmentManager fragmentManager;

    public NavigationManager(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    public void fragmentTransaction(int containerViewId, Fragment fragment, @Nullable LiveDataModel liveDataModel) {
        FragmentTransaction ft = fragmentManager.beginTransaction();
        if (containerViewId == R.id.fragment_bottom_bar)
            ft.replace(containerViewId, fragment);
        else if (liveDataModel == null)
            ft.add(containerViewId, fragment);
        else if (liveDataModel.getCurrPage() == LiveDataModel.CAUTION)
            ft.replace(containerViewId, fragment);
        else {
            ft = getTransactionAnimation(liveDataModel.getCurrPage(), liveDataModel.getNextPage());
            ft.replace(containerViewId, fragment);
        }
        ft.commit();
    }

    private FragmentTransaction getTransactionAnimation(int currPage, int nextPage) {
        FragmentTransaction ft = fragmentManager.beginTransaction();
        switch (currPage) {
            case LiveDataModel.HOME:
                if (nextPage == LiveDataModel.BROADCAST)
                    ft.setCustomAnimations(R.anim.slide_in_rtl, R.anim.slide_out_rtl);
                else
                    ft.setCustomAnimations(R.anim.slide_in_ltr, R.anim.slide_out_ltr);
                break;
            case LiveDataModel.BROADCAST:
                ft.setCustomAnimations(R.anim.slide_in_ltr, R.anim.slide_out_ltr);
                break;
            case LiveDataModel.CONVERSATION:
                ft.setCustomAnimations(R.anim.slide_in_rtl, R.anim.slide_out_rtl);
                break;
        }
        return ft;
    }
}
