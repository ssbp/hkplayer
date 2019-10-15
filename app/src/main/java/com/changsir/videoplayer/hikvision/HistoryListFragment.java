package com.changsir.videoplayer.hikvision;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.changsir.videoplayer.R;
import com.changsir.videoplayer.hikvision.utils.HistoryThread;
import com.haibin.calendarview.Calendar;
import com.haibin.calendarview.CalendarView;
import com.hikvision.netsdk.NET_DVR_FINDDATA_V30;
import com.hikvision.netsdk.NET_DVR_TIME;

import java.util.ArrayList;
import java.util.List;

/**
 * 回放列表
 */
public class HistoryListFragment extends Fragment {

    private HikviPresenter hikviPresenter;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private CalendarView calendarView;
    private List<NET_DVR_FINDDATA_V30> historyList = new ArrayList<>();
    private HistoryAdapter historyAdapter;

    private PlayerActivityInterface playerActivityInterface;

    public HistoryListFragment() {
    }

    public void setHikviPresenter(HikviPresenter hikviPresenter) {
        this.hikviPresenter = hikviPresenter;
    }

    public void setPlayerActivityInterface(PlayerActivityInterface playerActivityInterface) {
        this.playerActivityInterface = playerActivityInterface;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_history_list, container, false);
        progressBar = v.findViewById(R.id.progressBar);
        calendarView = v.findViewById(R.id.calendarView);
        calendarView.setOnCalendarSelectListener(onSelectedListener);

        recyclerView = v.findViewById(R.id.recyclerView);
        initRecyclerView();

        getHistory( calendarView.getSelectedCalendar());
        return v;
    }

    private void initRecyclerView() {
        historyAdapter = new HistoryAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(historyAdapter);
    }

    private CalendarView.OnCalendarSelectListener onSelectedListener = new CalendarView.OnCalendarSelectListener() {
        @Override
        public void onCalendarOutOfRange(Calendar calendar) {

        }

        @Override
        public void onCalendarSelect(Calendar calendar, boolean isClick) {
            if(null != playerActivityInterface) {
                playerActivityInterface.updateDate(calendar.getYear()+"年"+calendar.getMonth()+"月");
            }
            getHistory(calendar);
        }
    };

    /**
     * 获取回放列表
     */
    private void getHistory(Calendar calendar) {
        if (null != hikviPresenter) {

            java.util.Calendar jCalendar = java.util.Calendar.getInstance();
            jCalendar.set(java.util.Calendar.YEAR, calendar.getYear());
            jCalendar.set(java.util.Calendar.MONTH, calendar.getMonth()-1);
            jCalendar.set(java.util.Calendar.DAY_OF_MONTH, calendar.getDay());

            hikviPresenter.getHistoryList(jCalendar, new HistoryThread.OnHikviHistoryListener() {
                @Override
                public void onStart() {
                    progressBar.setVisibility(View.VISIBLE);
                }

                @Override
                public void onFinish(List<NET_DVR_FINDDATA_V30> list) {
                    historyList.clear();
                    historyList.addAll(list);
                    historyAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onError() {

                }
            });
        }
    }

    /**
     * 适配器
     */
    class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_list_item, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NET_DVR_FINDDATA_V30 data = historyList.get(position);
            String t = date2Str(data.struStartTime) + " - " + date2Str(data.struStopTime);
            holder.textView.setText(t);
            setHistoryClickListener(holder.textView, data);
        }

        @Override
        public int getItemCount() {
            return historyList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.text1);
            }
        }
    }

    /**
     * 历史列表点击
     */
    private void setHistoryClickListener(View view, NET_DVR_FINDDATA_V30 data) {
        view.setOnClickListener(view1 -> {
            if(null != playerActivityInterface) {
                playerActivityInterface.playCallBacy(HikviUtil.byte2Str(data.sFileName));
            }
        });
    }

    /**
     * 时间转换
     * @param date
     * @return
     */
    private String date2Str(NET_DVR_TIME date) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(add0(date.dwHour));
        stringBuilder.append(":");
        stringBuilder.append(add0(date.dwMinute));
        stringBuilder.append(":");
        stringBuilder.append(add0(date.dwSecond));
        return stringBuilder.toString();
    }

    private String add0(int v) {
        if(v < 10) {
            return "0"+v;
        }
        return String.valueOf(v);
    }

}
