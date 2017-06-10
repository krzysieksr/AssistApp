
package com.amazonaws.sample.lex;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.auth.CognitoCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.sample.lex.R;
import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest;
import com.amazonaws.services.polly.model.Voice;

import java.io.IOException;
import java.net.URL;

public class MainActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private Button textDemoButton;
    private Button speechDemoButton;
    private ImageButton infoButton;
    //private Spinner voicesSpinner;
    private AmazonPollyPresigningClient client;
    MediaPlayer mediaPlayer;
    CognitoCachingCredentialsProvider credentialsProvider;
    private static final Regions MY_REGION = Regions.US_EAST_1;
    private static final String COGNITO_POOL_ID="us-east-1:be925de0-60c8-4d54-95ea-211edd0a6a3b";


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_main);
        initPollyClient();
        setupNewMediaPlayer();
        setupPlayButton();
        init();
    }

    void initPollyClient() {
        // Initialize the Amazon Cognito credentials provider.
        credentialsProvider =
                new CognitoCachingCredentialsProvider(
                        getApplicationContext(),
                        COGNITO_POOL_ID,
                        MY_REGION
                );

        // Create a client that supports generation of presigned URLs.
        client = new AmazonPollyPresigningClient(credentialsProvider);
    }

    @Override
    public void onBackPressed() {
        finish();
        moveTaskToBack(true);
    }

    /**
     * Initializes the application.
     */
    private void init() {
        Log.e(TAG, "Initializing app");
        textDemoButton = (Button) findViewById(R.id.button_select_text);
        speechDemoButton = (Button) findViewById(R.id.button_select_voice);
        textDemoButton.setOnClickListener(this);
        speechDemoButton.setOnClickListener(this);
        if(!hasRecordAudioPermission()) {
            requestRecordAudioPermission();
        }
    }

    private boolean hasRecordAudioPermission(){
        boolean hasPermission = (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);

        Log.i(TAG, "Has RECORD_AUDIO permission? " + hasPermission);
        return hasPermission;
    }

    private void requestRecordAudioPermission(){

        String requiredPermission = Manifest.permission.RECORD_AUDIO;

        // If the user previously denied this permission then show a message explaining why
        // this permission is needed
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                requiredPermission)) {

            //showToast("This app needs to record audio through the microphone....");
        }

        // request the permission.
        ActivityCompat.requestPermissions(this,
                new String[]{requiredPermission},
                1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        // This method is called when the user responds to the permissions dialog
    }


    void setupPlayButton() {
        infoButton = (ImageButton) findViewById(R.id.welcomeButton);
//        infoButton.setEnabled(false);
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                infoButton.setEnabled(false);

//                Voice selectedVoice = (Voice) voicesSpinner.getSelectedItem();


                String textToRead = "Tekst testowy";

                // Use voice's sample text if user hasn't provided any text to read.
//                if (textToRead.trim().isEmpty()) {
//                    textToRead = getSampleText(selectedVoice);
//                }

                // Create speech synthesis request.
                SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest =
                        new SynthesizeSpeechPresignRequest()
                                // Set text to synthesize.
                                .withText(textToRead)
                                // Set voice selected by the user.
                                .withVoiceId("en-US")
                                // Set format to MP3.
                                .withOutputFormat(OutputFormat.Mp3);

                // Get the presigned URL for synthesized speech audio stream.
                URL presignedSynthesizeSpeechUrl =
                        client.getPresignedSynthesizeSpeechUrl(synthesizeSpeechPresignRequest);

                Log.i(TAG, "Playing speech from presigned URL: " + presignedSynthesizeSpeechUrl);

                // Create a media player to play the synthesized audio stream.
                if (mediaPlayer.isPlaying()) {
                    setupNewMediaPlayer();
                }
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

                try {
                    // Set media player's data source to previously obtained URL.
                    mediaPlayer.setDataSource(presignedSynthesizeSpeechUrl.toString());
                } catch (IOException e) {
                    Log.e(TAG, "Unable to set data source for the media player! " + e.getMessage());
                }

                // Start the playback asynchronously (since the data source is a network stream).
                mediaPlayer.prepareAsync();
            }
        });
    }

    void setupNewMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
                setupNewMediaPlayer();
            }
        });
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
                infoButton.setEnabled(true);
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                infoButton.setEnabled(true);
                return false;
            }
        });
    }

    /**
     * On-click listener for buttons text and voice buttons.
     *
     * @param v {@link View}, instance of the button component.
     */
    @Override
    public void onClick(final View v) {
        switch ((v.getId())) {
            case R.id.button_select_text:
                Intent textIntent = new Intent(this, TextActivity.class);
                startActivity(textIntent);
                break;
            case R.id.button_select_voice:
                Intent voiceIntent = new Intent(this, InteractiveVoiceActivity.class);
                startActivity(voiceIntent);
                break;
        }
    }
}
