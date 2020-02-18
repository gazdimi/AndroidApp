package com.kirkinis.p16049.trackmyrun;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class Text2Speech
{
    private TextToSpeech t2s;
    private TextToSpeech.OnInitListener initListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            if(status == TextToSpeech.SUCCESS)
            {
                t2s.setLanguage(Locale.ENGLISH);
            }
        }
    };

    public Text2Speech(Context context)
    {
        t2s = new TextToSpeech(context,initListener);
    }

    public void speak(String msg)
    {
        t2s.speak(msg,TextToSpeech.QUEUE_ADD,null,null);
    }
}
