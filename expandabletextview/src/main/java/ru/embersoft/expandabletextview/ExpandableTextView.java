package ru.embersoft.expandabletextview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

/*
 Created by gifffert on 20/10/2019.
 */

public class ExpandableTextView extends LinearLayout implements View.OnClickListener {

    private final int CONTENT = 2;

    private final int CONTENT_END_ANIM = 3;

    private final int CONTENT_EXPAND_ONLY = 4;
    public OnStateChangeListener onStateChangeListener;
    private TextView textView;
    private TextView expandTextView;
    private ImageView arrowImageView;
    private RelativeLayout hidebleRelativeLayout;
    private Drawable collapseImage;
    private Drawable expandImage;
    private int colorTextView;
    private String expandString;
    private String collapseString;
    private View bottomLine;
    private boolean isCollapse = false;
    private boolean isExpanded = false;
    private boolean primaryTextView = true;
    private int expandLine;
    private int textLines;
    private CharSequence textContent;
    private int textContentColor;
    private float textContentSize = 14;
    private Thread thread;
    private int sleepTime = 10;
    private boolean showLine = true;

    @SuppressLint("HandlerLeak") private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (CONTENT == msg.what) {
                textView.setMaxLines(msg.arg1);
                textView.invalidate();
            } else if (CONTENT_END_ANIM == msg.what) {
                setExpandState(msg.arg1);
            } else if (CONTENT_EXPAND_ONLY == msg.what) {
                changeExpandState(msg.arg1);
            }
            super.handleMessage(msg);
        }
    };

    public ExpandableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initValue(context, attrs);
        initView(context);
        initClick();
    }

    private void initValue(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ExpandTextView);

        expandLine = typedArray.getInteger(R.styleable.ExpandTextView_expandedLines, 5);

        collapseImage = typedArray.getDrawable(R.styleable.ExpandTextView_collapsedBitmap);
        expandImage = typedArray.getDrawable(R.styleable.ExpandTextView_expandedBitmap);

        colorTextView = typedArray.getColor(R.styleable.ExpandTextView_textColor,
                ContextCompat.getColor(context, R.color.colorPrimary));

        collapseString = typedArray.getString(R.styleable.ExpandTextView_collapseText);
        expandString = typedArray.getString(R.styleable.ExpandTextView_expandText);

        if (TextUtils.isEmpty(collapseString)) {
            collapseString = " ";
        }

        if (TextUtils.isEmpty(expandString)) {
            expandString = " ";
        }

        showLine = typedArray.getBoolean(R.styleable.ExpandTextView_showHideLine, showLine);

        textContentColor = typedArray.getColor(R.styleable.ExpandTextView_primaryTextColor,
                ContextCompat.getColor(context, R.color.colorSecondaryTextDefaultMaterialDark));
        textContentSize = typedArray.getDimension(R.styleable.ExpandTextView_textSize,
                textContentSize);

        sleepTime = typedArray.getInt(R.styleable.ExpandTextView_timeAnimation, sleepTime);

        typedArray.recycle();
    }

    private void initView(Context context) {

        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.expand_layout, this);

        hidebleRelativeLayout =
                (RelativeLayout) findViewById(R.id.expandableRelativeLayout);

        textView = (TextView) findViewById(R.id.collapsedTextView);
        textView.setTextColor(textContentColor);
        textView.getPaint().setTextSize(textContentSize);

        arrowImageView = (ImageView) findViewById(R.id.switcherImageView);

        expandTextView = (TextView) findViewById(R.id.hintTextView);
        expandTextView.setTextColor(colorTextView);

        bottomLine = findViewById(R.id.bottomLine);
        if (!showLine) {
            bottomLine.setVisibility(GONE);
        }
    }

    private void initClick() {
        textView.setOnClickListener(this);
        hidebleRelativeLayout.setOnClickListener(this);
    }

    public void setText(CharSequence charSequence) {

        textContent = charSequence;

        textView.setText(charSequence.toString());

        ViewTreeObserver viewTreeObserver = textView.getViewTreeObserver();
        viewTreeObserver.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

            @Override
            public boolean onPreDraw() {
                if (!primaryTextView) {
                    return true;
                }
                textLines = textView.getLineCount();
                isExpanded = textLines > expandLine;
                primaryTextView = false;
                if (isExpanded) {
                    isCollapse = true;
                    doAnimation(expandLine, expandLine, CONTENT_END_ANIM);
                } else {
                    isCollapse = false;
                    doNotExpand();
                }
                return true;
            }
        });

        if (!primaryTextView) {
            textLines = textView.getLineCount();
        }
    }

    private void doAnimation(final int startIndex, final int endIndex, final int what) {

        thread = new Thread(new Runnable() {

            @Override
            public void run() {

                if (startIndex < endIndex) {
                    // if start index smaller than end index ,do expand action
                    int count = startIndex;
                    while (count++ < endIndex) {
                        Message msg = handler.obtainMessage(CONTENT, count, 0);

                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        handler.sendMessage(msg);
                    }
                } else if (startIndex > endIndex) {
                    // if start index bigger than end index ,do collapse action
                    int count = startIndex;
                    while (count-- > endIndex) {
                        Message msg = handler.obtainMessage(CONTENT, count, 0);
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        handler.sendMessage(msg);
                    }
                }

                // animation end,send signal
                Message msg = handler.obtainMessage(what, endIndex, 0);
                handler.sendMessage(msg);
            }
        });

        thread.start();
    }

    /*
      change collapse or expand state(only change state,but not hide collapse/expand icon)
     */
    @SuppressWarnings("deprecation") private void changeExpandState(int endIndex) {
        hidebleRelativeLayout.setVisibility(View.VISIBLE);
        if (endIndex < textLines) {
            arrowImageView.setBackgroundDrawable(expandImage);
            expandTextView.setText(expandString);
        } else {
            arrowImageView.setBackgroundDrawable(collapseImage);
            expandTextView.setText(collapseString);
        }
    }

    /*
     change collapse/expand state(if number of expand lines bigger than original text lines,hide
     collapse/expand icon,and TextView will always be at expand state)
     */
    @SuppressWarnings("deprecation") private void setExpandState(int endIndex) {

        if (endIndex < textLines) {
            isCollapse = true;
            hidebleRelativeLayout.setVisibility(View.VISIBLE);
            arrowImageView.setBackgroundDrawable(expandImage);
            textView.setOnClickListener(this);
            expandTextView.setText(expandString);
        } else {
            isCollapse = false;
            hidebleRelativeLayout.setVisibility(View.GONE);
            arrowImageView.setBackgroundDrawable(collapseImage);
            textView.setOnClickListener(null);
            expandTextView.setText(collapseString);
        }
    }

    /*
     do not expand
     */
    private void doNotExpand() {
        textView.setMaxLines(expandLine);
        hidebleRelativeLayout.setVisibility(View.GONE);
        textView.setOnClickListener(null);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.expandableRelativeLayout
                || v.getId() == R.id.collapsedTextView) {
            clickImageToggle();
            if (null != onStateChangeListener) onStateChangeListener.onStateChange(isCollapse);
        }
    }

    private void clickImageToggle() {
        if (isCollapse) {
            // do collapse action
            doAnimation(expandLine, textLines, CONTENT_EXPAND_ONLY);
        } else {
            // do expand action
            doAnimation(textLines, expandLine, CONTENT_EXPAND_ONLY);
        }

        isCollapse = !isCollapse;
    }

    public void resetState(boolean isCollapse) {

        this.isCollapse = isCollapse;
        if (textLines > expandLine) {
            int sdk = android.os.Build.VERSION.SDK_INT;
            if (isCollapse) {
                hidebleRelativeLayout.setVisibility(View.VISIBLE);
                if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    arrowImageView.setBackgroundDrawable(expandImage);
                } else {
                    arrowImageView.setBackground(expandImage);
                }
                textView.setOnClickListener(this);
                textView.setMaxLines(expandLine);
                expandTextView.setText(expandString);
            } else {
                hidebleRelativeLayout.setVisibility(View.VISIBLE);
                if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    arrowImageView.setBackgroundDrawable(collapseImage);
                } else {
                    arrowImageView.setBackground(collapseImage);
                }
                textView.setOnClickListener(this);
                textView.setMaxLines(textLines);
                expandTextView.setText(collapseString);
            }
        } else {
            doNotExpand();
        }
    }

    public Drawable getCollapseImage() {
        return collapseImage;
    }

    public void setCollapseImage(Drawable collapseImage) {
        this.collapseImage = collapseImage;
    }

    public Drawable getExpandImage() {
        return expandImage;
    }

    public void setExpandImage(Drawable expandImage) {
        this.expandImage = expandImage;
    }

    public int getExpandLine() {
        return expandLine;
    }

    public void setExpandLine(int newExpandLines) {
        int start = isCollapse ? this.expandLine : textLines;
        int end = textLines < newExpandLines ? textLines : newExpandLines;
        doAnimation(start, end, CONTENT_END_ANIM);
        this.expandLine = newExpandLines;
    }

    public CharSequence getTextContent() {
        return textContent;
    }

    public int getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(int sleepTime) {
        this.sleepTime = sleepTime;
    }

    public void setOnStateChangeListener(OnStateChangeListener onStateChangeListener) {
        this.onStateChangeListener = onStateChangeListener;
    }

    public interface OnStateChangeListener {
        void onStateChange(boolean isCollapse);
    }
}
