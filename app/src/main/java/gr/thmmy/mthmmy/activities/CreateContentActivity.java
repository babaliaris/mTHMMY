package gr.thmmy.mthmmy.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;
import android.widget.Toast;

import gr.thmmy.mthmmy.R;
import gr.thmmy.mthmmy.editorview.EditorView;
import gr.thmmy.mthmmy.editorview.EmojiKeyboard;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import timber.log.Timber;

public class CreateContentActivity extends AppCompatActivity implements EmojiKeyboard.EmojiKeyboardOwner,
        NewTopicTask.NewTopicTaskCallbacks {

    public final static String EXTRA_NEW_TOPIC_URL = "new-topic-extra";

    private EditorView contentEditor;
    private EmojiKeyboard emojiKeyboard;
    private TextInputLayout subjectInput;
    private MaterialProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_content);

        //Initialize toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Create topic");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        ((TextView) findViewById(R.id.toolbar_title)).setText("Create topic");

        progressBar = findViewById(R.id.progressBar);

        Intent callingIntent = getIntent();
        String newTopicUrl = callingIntent.getStringExtra(EXTRA_NEW_TOPIC_URL);

        emojiKeyboard = findViewById(R.id.emoji_keyboard);

        subjectInput = findViewById(R.id.subject_input);

        contentEditor = findViewById(R.id.main_content_editorview);
        setEmojiKeyboardInputConnection(contentEditor.getInputConnection());
        contentEditor.setEmojiKeyboardOwner(this);
        contentEditor.setOnSubmitListener(v -> {
            if (newTopicUrl != null) {
                new NewTopicTask(this).execute(newTopicUrl, subjectInput.getEditText().getText().toString(),
                        contentEditor.getText().toString());
            }
        });
    }

    @Override
    public void setEmojiKeyboardVisible(boolean visible) {
        emojiKeyboard.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean isEmojiKeyboardVisible() {
        return emojiKeyboard.getVisibility() == View.VISIBLE;
    }

    @Override
    public void setEmojiKeyboardInputConnection(InputConnection ic) {
        emojiKeyboard.setInputConnection(ic);
    }

    @Override
    public void onBackPressed() {
        if (emojiKeyboard.getVisibility() == View.VISIBLE) {
            emojiKeyboard.setVisibility(View.GONE);
            contentEditor.updateEmojiKeyboardVisibility();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onNewTopicTaskStarted() {
        Timber.i("New topic creation started");
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onNewTopicTaskFinished(boolean success) {
        progressBar.setVisibility(View.INVISIBLE);
        if (success) {
            Timber.i("New topic created successfully");
            finish();
        } else {
            Timber.w("New topic creation failed");
            Toast.makeText(getBaseContext(), "Failed to create new topic!", Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
