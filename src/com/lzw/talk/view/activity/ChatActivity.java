package com.lzw.talk.view.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.avos.avoscloud.AVMessage;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.Session;
import com.lzw.talk.R;
import com.lzw.talk.adapter.ChatMsgViewAdapter;
import com.lzw.talk.base.App;
import com.lzw.talk.db.DBHelper;
import com.lzw.talk.db.DBMsg;
import com.lzw.talk.entity.Msg;
import com.lzw.talk.receiver.MsgReceiver;
import com.lzw.talk.service.ChatService;
import com.lzw.talk.service.MessageListener;
import com.lzw.talk.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends Activity implements OnClickListener, MessageListener {
  private EditText mEditTextContent;
  private ListView mListView;
  private ChatMsgViewAdapter mAdapter;
  private List<Msg> msgs;
  public static ChatActivity instance;
  Activity cxt;
  ProgressBar progressBar;
  AVUser me;
  DBHelper dbHelper;
  View btnSend;

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    cxt = this;
    instance = this;
    getWindow().setSoftInputMode(
        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    setContentView(R.layout.chat_room);
    findView();
    initData();
  }

  private void findView() {
    progressBar = (ProgressBar) findViewById(R.id.progressBar);
    mListView = (ListView) findViewById(R.id.listview);

    mEditTextContent = (EditText) findViewById(R.id.et_sendmessage);
    btnSend = findViewById(R.id.btn_send);
    btnSend.setOnClickListener(this);
  }

  @Override
  public void onResume() {
    super.onResume();
    refresh();
    MsgReceiver.registerSessionListener(ChatService.getPeerId(App.chatUser), this);
  }

  @Override
  public void onPause() {
    super.onPause();
    MsgReceiver.unregisterSessionListener(ChatService.getPeerId(App.chatUser));
  }

  public void initData() {
    ActionBar actionBar = getActionBar();
    View view = LayoutInflater.from(cxt).inflate(R.layout.chat_bar, null);
    TextView title = (TextView) view.findViewById(R.id.title);
    actionBar.setDisplayShowCustomEnabled(true);
    actionBar.setDisplayShowTitleEnabled(false);
    actionBar.setDisplayShowHomeEnabled(false);
    actionBar.setCustomView(view);
    title.setText(App.chatUser.getUsername());
    me = AVUser.getCurrentUser();
    dbHelper = new DBHelper(cxt, App.DB_NAME, App.DB_VER);
    msgs = new ArrayList<Msg>();
    mAdapter = new ChatMsgViewAdapter(this);
    mListView.setAdapter(mAdapter);
  }

  public void refresh() {
    new GetDataTask().execute();
  }

  @Override
  public void onMessage(Msg msg) {
    refresh();
  }

  class GetDataTask extends AsyncTask<Void, Void, Void> {
    boolean res;
    List<Msg> msgs;

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... params) {
      try {
        me = (AVUser) me.fetch();  //to refresh object
        msgs = DBMsg.getMsgs(dbHelper, ChatService.getPeerId(me), ChatService.getPeerId(App.chatUser));
        res = true;
      } catch (Exception e) {
        e.printStackTrace();
        res = false;
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      super.onPostExecute(aVoid);
      progressBar.setVisibility(View.INVISIBLE);
      if (res) {
        addMsgsAndRefresh(msgs);
      } else {
        Utils.toast(cxt, R.string.failedToGetData);
      }
    }
  }

  public void addMsgsAndRefresh(List<Msg> msgs) {
    this.msgs = msgs;
    mAdapter.setDatas(this.msgs);
    mAdapter.notifyDataSetChanged();
    scroolToLast();
  }

  @Override
  public void onClick(View v) {
    // TODO Auto-generated method stub
    switch (v.getId()) {
      case R.id.btn_send:
        send();
        break;
    }
  }

  private void send() {
    String contString = mEditTextContent.getText().toString();
    if (contString.length() > 0) {
      new SendTask(contString).execute();
      mEditTextContent.setText("");
    }
  }

  class SendTask extends AsyncTask<Void, Void, Void> {
    String text;

    boolean res;

    public SendTask(String text) {
      this.text = text;
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected Void doInBackground(Void... params) {
      try {
        String peerId = ChatService.getPeerId(App.chatUser);
        Msg msg;
        msg = new Msg();
        msg.setStatus(Msg.STATUS_SEND_START);
        msg.setContent(text);
        msg.setTimestamp(System.currentTimeMillis());
        msg.setFromPeerId(ChatService.getSelfId());
        msg.setToPeerIds(Utils.oneToList(peerId));
        msg.setObjectId(Utils.uuid());
        msg.setType(Msg.TYPE_TEXT);

        DBMsg.insertMsg(dbHelper, msg);
        AVMessage avMsg = msg.toAVMessage();
        Session session =ChatService.getSession();
        session.sendMessage(avMsg);
        res = true;
      } catch (Exception e) {
        e.printStackTrace();
        res = false;
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      progressBar.setVisibility(View.GONE);
      if (res) {
        refresh();
      } else {
        Utils.toast(cxt, R.string.badNetwork);
      }
    }
  }

  private void scroolToLast() {
    mListView.setSelection(mListView.getCount() - 1);
  }
}