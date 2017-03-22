package com.github.sjnyag.animationwraplayout;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import com.github.sjnyag.AnimationWrapLayout;

import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    int mCounter = 0;
    Random mRandom = new Random();
    AnimationWrapLayout mList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mList = (AnimationWrapLayout) findViewById(R.id.list);
        findViewById(R.id.register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mList.addViewWithAnimation(inflateTextView(), 0)) {
                    mCounter--;
                }
            }
        });
    }

    private TextView inflateTextView() {
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(8);
        shape.setColor(0xff000000 | mRandom.nextInt(0x00ffffff));
        TextView textView = new TextView(this);
        textView.setGravity(Gravity.CENTER);
        textView.setTextColor(Color.WHITE);
        textView.setWidth(150 + mRandom.nextInt(200));
        textView.setHeight(150 + mRandom.nextInt(200));
        textView.setTextSize(32);
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            textView.setBackgroundDrawable(shape);
        } else {
            textView.setBackground(shape);
        }
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                mList.removeViewWithAnimation(view);
            }
        });
        textView.setText(String.format(Locale.JAPAN, "%1$02d", mCounter++));
        return textView;
    }

}
