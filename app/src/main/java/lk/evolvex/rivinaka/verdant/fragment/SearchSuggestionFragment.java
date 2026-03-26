package lk.evolvex.rivinaka.verdant.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.activity.MainHome;
import lk.evolvex.rivinaka.verdant.adapter.RecentSearchAdapter;

public class SearchSuggestionFragment extends Fragment implements RecentSearchAdapter.OnRecentSearchClickListener {

    private EditText etSearchInput;
    private RecyclerView rvRecentSearches;
    private RecentSearchAdapter adapter;
    private List<String> recentSearches;
    private static final String PREFS_NAME = "search_prefs";
    private static final String KEY_RECENT_SEARCHES = "recent_searches";
    private static final int MAX_RECENT_COUNT = 10;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search_suggestion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof MainHome) {
            MainHome mainHome = (MainHome) getActivity();
            mainHome.setHeaderVisibility(View.GONE);
            mainHome.setBottomNavVisibility(View.GONE);
        }

        etSearchInput = view.findViewById(R.id.etSearchInput);
        rvRecentSearches = view.findViewById(R.id.rvRecentSearches);
        TextView btnClearAll = view.findViewById(R.id.btnClearAll);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        loadRecentSearches();

        adapter = new RecentSearchAdapter(recentSearches, this);
        rvRecentSearches.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecentSearches.setAdapter(adapter);

        etSearchInput.requestFocus();
        showKeyboard();

        etSearchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearchInput.getText().toString().trim();
                if (!query.isEmpty()) {
                    performSearch(query);
                }
                return true;
            }
            return false;
        });

        btnClearAll.setOnClickListener(v -> {
            recentSearches.clear();
            saveRecentSearches();
            adapter.notifyDataSetChanged();
        });
    }

    private void performSearch(String query) {
        saveQuery(query);
        hideKeyboard();
        
        SearchFragment searchFragment = SearchFragment.newInstance(query);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, searchFragment)
                .addToBackStack(null)
                .commit();
    }

    private void saveQuery(String query) {
        if (recentSearches.contains(query)) {
            recentSearches.remove(query);
        }
        recentSearches.add(0, query);
        if (recentSearches.size() > MAX_RECENT_COUNT) {
            recentSearches.remove(recentSearches.size() - 1);
        }
        saveRecentSearches();
    }

    private void loadRecentSearches() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_RECENT_SEARCHES, null);
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<String>>() {}.getType();
            recentSearches = gson.fromJson(json, type);
        } else {
            recentSearches = new ArrayList<>();
        }
    }

    private void saveRecentSearches() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(recentSearches);
        editor.putString(KEY_RECENT_SEARCHES, json);
        editor.apply();
    }

    @Override
    public void onRecentSearchClick(String query) {
        etSearchInput.setText(query);
        performSearch(query);
    }

    @Override
    public void onRemoveRecentSearch(int position) {
        recentSearches.remove(position);
        saveRecentSearches();
        adapter.notifyItemRemoved(position);
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etSearchInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearchInput.getWindowToken(), 0);
        }
    }
}