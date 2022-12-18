package com.example.lifeguard.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.lifeguard.R;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private List<ChatMessage> mMessages;
    private boolean right;

    public ChatAdapter(List<ChatMessage> messages) {
        mMessages = messages;
        right = false;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (right){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chat_message_user, parent, false);
        }else{
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chat_message_chatbot, parent, false);
        }
        right = !right;
        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ChatMessage message = mMessages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    public void addMessage(ChatMessage message) {
        mMessages.add(message);
        notifyItemInserted(mMessages.size() - 1);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mMessageTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            mMessageTextView = itemView.findViewById(R.id.text_message);
        }

        public void bind(ChatMessage message) {
            mMessageTextView.setText(message.getText());
        }
    }
}