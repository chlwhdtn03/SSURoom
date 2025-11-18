package cse.ssuroom.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import cse.ssuroom.R;
import cse.ssuroom.adapter.ChatAdapter;

public class ChatFragment extends Fragment implements ChatAdapter.OnChatItemClickListener {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.chat_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new ChatAdapter(this));

        return view;
    }

    @Override
    public void onChatItemClick() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.screen, new ChatRoomFragment())
                .addToBackStack(null)
                .commit();
    }
}
