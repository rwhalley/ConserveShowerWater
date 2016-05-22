package com.showerguilt.wasteful.showerguilt;


import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {


    private final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    int channel_config;
    int format;
    int sampleRate;
    Calendar showerCalendar; // keeps track of when to do things
    boolean isShowerOn = false;
    int bufferSize;
    final private double SMA_THRESHOLD = 10;
    final private int SMA_LENGTH = 60;

    //Band Pass Constants
    final private int UPPER_BOUND = 100;
    final private int LOWER_BOUND = 50;

    SMA mySMA;
    AudioRecord audioInput;
    short[] audioBuffer;
    Complex[] fftArr;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getPermissions();


    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    void getPermissions(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
            }
        } else {
            setupAudioRecord();
            startAudioBuffer();}
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!
                    // DO STUFF HERE
                    Log.d("MainActivity","Permission Granted");
                    setupAudioRecord();
                    startAudioBuffer();
                    initializeSMA();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setupAudioRecord(){
        channel_config = AudioFormat.CHANNEL_IN_MONO;
        format = AudioFormat.ENCODING_PCM_16BIT;
        sampleRate = 8000;
        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channel_config, format);
        audioInput = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channel_config, format, bufferSize);
    }

    public void startAudioBuffer() {
        audioBuffer = new short[bufferSize];
        audioInput.startRecording();

    }


    public void printBuffer(){
        audioInput.read(audioBuffer, 0, bufferSize);
        Log.d("MainActivity", "audioBuffer " + doFFT(shortToDouble(audioBuffer))[128].re());
    }

    public void initializeSMA(){
        mySMA = new SMA(SMA_LENGTH);
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateSMA();
                checkIfShowerOn();
            }
        }, 0, 100);
    }

    public void playSound(){
        final MediaPlayer mp = MediaPlayer.create(this, R.raw.bottom);
        mp.start();
    }

    //Get FFT
    //Apply BandPAss
    //Compute Average Value
    //Update Simple Moving Average (SMA)
    public void updateSMA(){
        Complex [] FFTarr = doFFT(shortToDouble(audioBuffer));
        FFTarr = bandPassFilter(FFTarr);
        double sum = 0;
        for(int i = 0; i<FFTarr.length;i++){
            sum+=FFTarr[i].re();
        }
        double bandPassAverage = sum/FFTarr.length;
        mySMA.compute(bandPassAverage);
    }


    //Compares Current Simple Moving Average to a Threshold
    //TODO consider adding a check for variance of signal
    public void checkIfShowerOn(){
        if (mySMA.currentAverage()>SMA_THRESHOLD && !isShowerOn){
            //Initialize stuff to do once shower is on
            isShowerOn=true;
            startShower();
        } else {
            //Shutdown
            isShowerOn=false;
            endShower();
        }
    }

    //Starts Clock once Shower is on.
    public void startShower(){
        playSound();


    }



    //Finish Shower Routine
    public void endShower(){

    }

    public double[] shortToDouble(short [] input){
        double[] output = new double[bufferSize];

        for (int j=0;j<bufferSize;j++) {
            output[j] = (double)input[j];
        }

        return output;
    }

    public Complex []doFFT(double [] input){
        Complex [] fftTempArr = new Complex [bufferSize];
        for(int i=0;i<bufferSize;i++){
            fftArr[i]=new Complex (input[i],0);
        }
        fftArr = FFT.fft(fftTempArr);
        return fftArr;

    }

    public Complex[] bandPassFilter(Complex[] input){
        Complex [] output = new Complex[bufferSize-LOWER_BOUND-(bufferSize-UPPER_BOUND)];
        int outputIndex = 0;
        for (int i=LOWER_BOUND;i<UPPER_BOUND;i++){
            output[outputIndex]=input[i];
            outputIndex++;
        }
        return output;
    }
}
