package com.changsir.videoplayer.hikvision;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 视频播放的模型
 */
public class VideoModel implements Parcelable {

    public static final int VIDEO_HIKVI = 0;
    public static final int VIDEO_RTSP = 1;

    private int id;
    private String address;
    private int port;
    private String username;
    private String passwd;
    private int channel;
    private String name;
    private int type;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.address);
        dest.writeInt(this.port);
        dest.writeString(this.username);
        dest.writeString(this.passwd);
        dest.writeInt(this.channel);
        dest.writeString(this.name);
    }

    public VideoModel() {
    }

    protected VideoModel(Parcel in) {
        this.id = in.readInt();
        this.address = in.readString();
        this.port = in.readInt();
        this.username = in.readString();
        this.passwd = in.readString();
        this.channel = in.readInt();
        this.name = in.readString();
    }

    public static final Parcelable.Creator<VideoModel> CREATOR = new Parcelable.Creator<VideoModel>() {
        @Override
        public VideoModel createFromParcel(Parcel source) {
            return new VideoModel(source);
        }

        @Override
        public VideoModel[] newArray(int size) {
            return new VideoModel[size];
        }
    };
}
