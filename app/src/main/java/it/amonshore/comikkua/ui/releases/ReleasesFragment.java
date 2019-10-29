package it.amonshore.comikkua.ui.releases;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.XRelease;
import it.amonshore.comikkua.data.ReleaseHeader;
import it.amonshore.comikkua.data.ReleaseItem;
import it.amonshore.comikkua.ui.OnNavigationFragmentListener;


public class ReleasesFragment extends Fragment {

    private OnNavigationFragmentListener mListener;

    public ReleasesFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_releases, container, false);

        final ReleaseItem[] itemList = new ReleaseItem[]{
                ReleaseItem.fromString(ReleaseHeader.class,"Questa settimana;1/2"),
                ReleaseItem.fromString(XRelease.class,"Dorohedoro;22;domani;;;N;S"),
                ReleaseItem.fromString(XRelease.class,"BRPD - Inferno sulla terra;18;martedì, 15 gennaio;MagicPress - AA.VV;La tana del diavolo;N;S"),
                ReleaseItem.fromString(ReleaseHeader.class,"Settimana prossima;0/2"),
                ReleaseItem.fromString(XRelease.class,"Berserk;80;sabato, 12 gennaio;Planet Manga - Kentaro Miura;;S;S"),
                ReleaseItem.fromString(XRelease.class,"Saga;1~14;;BAO Publishing;;N;N"),
                ReleaseItem.fromString(ReleaseHeader.class,"Senza data;0/1"),
                // la regola è: n~m intervallo senza spazio così rimangono uniti e non vanno a capo
                ReleaseItem.fromString(XRelease.class,"Tokyo Ghoul;3~6, 8, 9~14, 20;;JPop;;N;N")
        };

        final Context context = getContext();
        final RecyclerView list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(context));
        list.setAdapter(new ReleasesRecyclerViewAdapter(context, itemList));

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnNavigationFragmentListener) {
            mListener = (OnNavigationFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnNavigationFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
