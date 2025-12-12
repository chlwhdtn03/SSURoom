package cse.ssuroom.chat;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Message {
    private String senderId;
    private String text;
    @ServerTimestamp
    private Date timestamp;

    public Message() {}

    public Message(String senderId, String text) {
        this.senderId = senderId;
        this.text = text;
    }
    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
