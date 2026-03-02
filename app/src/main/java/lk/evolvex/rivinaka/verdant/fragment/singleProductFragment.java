package lk.evolvex.rivinaka.verdant.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.activity.MainHome;

public class singleProductFragment extends Fragment {

    public singleProductFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_single_product, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Hide the top header and bottom navigation from MainHome
        if (getActivity() instanceof MainHome) {
            MainHome mainHome = (MainHome) getActivity();
            mainHome.setHeaderVisibility(View.GONE);
            mainHome.setBottomNavVisibility(View.GONE);
        }

        // Back arrow logic
        ImageView btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Navigate back to the previous fragment (Home)
                    if (getFragmentManager() != null) {
                        getFragmentManager().popBackStack();
                    }
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Restore visibility when leaving the fragment
        if (getActivity() instanceof MainHome) {
            MainHome mainHome = (MainHome) getActivity();
            // Note: We'll need to check which fragment we're returning to 
            // but for now, we'll let MainHome's navigation logic handle it 
            // or restore defaults here.
            mainHome.setBottomNavVisibility(View.VISIBLE);
            // Header visibility is usually managed by the navigation selection in MainHome
        }
    }
}
