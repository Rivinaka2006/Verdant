package lk.evolvex.rivinaka.verdant.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import lk.evolvex.rivinaka.verdant.R;

public class HomeFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find the first item in the Special Offers section.
        // We need to add an ID to the LinearLayout in fragment_home.xml first.
        View firstSpecialOffer = view.findViewById(R.id.specialOfferItem1);

        if (firstSpecialOffer != null) {
            firstSpecialOffer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Navigate to the SingleProductFragment
                    Fragment singleProductFragment = new singleProductFragment();
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, singleProductFragment)
                            .addToBackStack(null) // Allows users to navigate back to Home
                            .commit();
                }
            });
        }
    }
}
