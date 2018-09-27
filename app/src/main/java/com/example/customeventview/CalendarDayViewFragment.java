package com.example.customeventview;


import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;


/**
 * A simple {@link Fragment} subclass.
 */
public class CalendarDayViewFragment extends DialogFragment implements View.OnLongClickListener, View.OnDragListener, View.OnClickListener {

    public static final String TAG = "CalendarDayViewFragment";
    private String[] dayHourTimes = {"12 AM", "1 AM", "2 AM", "3 AM", "4 AM", "5 AM", "6 AM", "7 AM", "8 AM", "9 AM", "10 AM", "11 AM", "12 PM", "1 PM", "2 PM", "3 PM",
            "4 PM", "5 PM", "6 PM", "7 PM", "8 PM", "9 PM", "10 PM", "11 PM"};
    private List<EventObject> eventsList = new ArrayList<>();

    private LinearLayout hoursLinearLayout;
    private RelativeLayout eventsView;
    private ScrollView scrollView;

    private static final float HOUR_MARGIN_LEFT = 12;
    private static final float HOUR_MARGIN_RIGHT = 5;
    private static final float HOUR_VIEW_HEIGHT = 63;
    private static final float DIVIDER_LINE_MARGIN_LEFT = 45;
    private static final float DIVIDER_LINE_MARGIN_TOP = 8;
    private static final float EVENT_GAP = 1;
    private static final int MAX_EVENTS_SIZE = 3;
    private static final int MARGIN_MULTIPLIER_MAX_SIZE = 2;
    private Random rand = new Random();
    private static int MAX_RANDOM_VALUE_LIMIT = 1000;
    private static final int STANDARD_EVENT_TEXT_SIZE = 12;
    private static final int ADDITIONAL_EVENT_TEXT_SIZE = 18;
    private static final String ADDITIONAL_EVENT_ID_SEPARATOR = ":";
    public boolean showNowLine = true;
    private List<List<EventObject>> eventsToDraw = new ArrayList<>();//list is a grouping/combination of events with in the list
    private List<EventObject> mappedEventObjects = new ArrayList<>();//Stores the grouped events and is initialized every time during iteration


    public static CalendarDayViewFragment newInstance() {

        Bundle args = new Bundle();

        CalendarDayViewFragment fragment = new CalendarDayViewFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_calendar_day_view, container, false);

        scrollView = view.findViewById(R.id.scroll_view);
        hoursLinearLayout = view.findViewById(R.id.hours_linear_layout);
        eventsView = view.findViewById(R.id.events_view);

        RelativeLayout.LayoutParams eventsViewParams = (RelativeLayout.LayoutParams) eventsView.getLayoutParams();
        int eventsViewLeftMargin = (int) convertDpToPixel(DIVIDER_LINE_MARGIN_LEFT + HOUR_MARGIN_LEFT, getContext());
        int eventsViewRightMargin = (int) convertDpToPixel(HOUR_MARGIN_RIGHT, getContext());
        eventsViewParams.setMargins(eventsViewLeftMargin, 0, eventsViewRightMargin, 0);
        eventsView.setLayoutParams(eventsViewParams);
        eventsView.setOnDragListener(this);

        int hourHeight = (int) convertDpToPixel(HOUR_VIEW_HEIGHT, getContext());
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, hourHeight);
        int leftMargin = (int) convertDpToPixel(HOUR_MARGIN_LEFT, getContext());
        int rightMargin = (int) convertDpToPixel(HOUR_MARGIN_RIGHT, getContext());
        layoutParams.setMargins(leftMargin, 0, rightMargin, 0);

        for (int i = 0; i < dayHourTimes.length; i++) {
            View hourLayout = inflater.inflate(R.layout.hour_layout, null, false);
            TextView textView = hourLayout.findViewById(R.id.hour_text);
            textView.setText(dayHourTimes[i]);
            hourLayout.setLayoutParams(layoutParams);
            hoursLinearLayout.addView(hourLayout);
        }

        //populates the calendar events and draws them on view
        setCalendarEvents();
        return view;
    }

    /**
     * Draws the calendar events on the view
     */
    private void drawEvents() {
        int parentWidth = eventsView.getWidth();//total width of the layout
        int eventWidth;

        for (int i = 0; i < eventsToDraw.size(); i++) {
            List<EventObject> groupedEvents = eventsToDraw.get(i);
            //sorting the grouped events in descending order based on duration
            Collections.sort(groupedEvents, new CustomDurationComparator());

            if (groupedEvents.size() > MAX_EVENTS_SIZE) {
                float maxPercent = 85.0f;
                float minPercent = 15.0f;
                float totalPercent = 100.0f;
                int threeEventsWidth = (int) (parentWidth * (maxPercent / totalPercent));
                showLog("Three events width:::::" + threeEventsWidth);
                int additionalEventsWidth = (int) (parentWidth * (minPercent / totalPercent));
                showLog("Additional Events width:::::" + additionalEventsWidth);

                eventWidth = threeEventsWidth / MAX_EVENTS_SIZE;
                calculateEventMargins(groupedEvents, eventWidth, threeEventsWidth, additionalEventsWidth);
            } else {
                //if size <=3 then draw the events
                eventWidth = parentWidth / groupedEvents.size();
                //Calculates the event margins and then draws the events
                calculateEventMargins(groupedEvents, eventWidth, 0, 0);//threeEventsWidth & additionalEventsWidth should be zero
            }
        }
        //shows the now line
        if (showNowLine) {
            View nowLineView = new View(getContext());
            nowLineView.setBackgroundColor(Color.MAGENTA);
            RelativeLayout.LayoutParams viewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                    (int) convertDpToPixel(1, getContext()));
            Calendar nowCalendar = Calendar.getInstance();
            int currentHour = nowCalendar.get(Calendar.HOUR_OF_DAY);
            int currentMinutes = nowCalendar.get(Calendar.MINUTE);
            int top = (int) convertDpToPixel((HOUR_VIEW_HEIGHT * currentHour) + currentMinutes + DIVIDER_LINE_MARGIN_TOP, getContext());
            viewParams.setMargins(0, top, 0, 0);
            nowLineView.setLayoutParams(viewParams);
            eventsView.addView(nowLineView);
        }
    }

//    /**
//     * Overlays all the events and its dependent child events
//     *
//     * @param allOverlappingEvents
//     * @param eventWidth
//     * @param textGravity
//     * @param textSize
//     */
//    private void drawOverLappingEvents(List<EventObject> allOverlappingEvents, int eventWidth, int textGravity, int textSize) {
//        int size = allOverlappingEvents.size();
//        for (int k = 0; k < size; k++) {
//            EventObject eventObject = allOverlappingEvents.get(k);
//            int[] startTime = getStartTime(eventObject.getStartTime());
//            int[] endTime = getEndTime(eventObject.getEndTime());
//            //0 -> hour & 1 -> minute
//            int eventHeight = (int) ((HOUR_VIEW_HEIGHT * endTime[0]) + endTime[1] + DIVIDER_LINE_MARGIN_TOP)
//                    - (int) ((HOUR_VIEW_HEIGHT * startTime[0]) + startTime[1] + DIVIDER_LINE_MARGIN_TOP);
//            //No size conversion, pass direct pixels for width
//            RelativeLayout.LayoutParams textViewParams = new RelativeLayout.LayoutParams((int) (eventWidth - EVENT_GAP),
//                    (int) convertDpToPixel(eventHeight, getContext()));
//            int top = (int) convertDpToPixel((HOUR_VIEW_HEIGHT * startTime[0]) + startTime[1] + DIVIDER_LINE_MARGIN_TOP, getContext());
//            int left = eventObject.getLeftMargin();
//            textViewParams.setMargins(left, top, 0, 0);
//            eventsView.addView(getTextView(textViewParams, eventObject, ContextCompat.getColor(getContext(), R.color.colorPrimary), textGravity, textSize));
//        }
//    }

    /**
     * Draws the events on views
     *
     * @param allOverlappingEvents
     * @param eventWidth
     * @param textGravity
     * @param textSize
     */
    private void drawEventsOnView(List<EventObject> allOverlappingEvents, int eventWidth, int textGravity, int textSize) {
        int size = allOverlappingEvents.size();
        for (int k = 0; k < size; k++) {
            EventObject eventObject = allOverlappingEvents.get(k);
            //No size conversion, pass direct pixels for width
            RelativeLayout.LayoutParams textViewParams = new RelativeLayout.LayoutParams((int) (eventWidth - EVENT_GAP),
                    eventObject.getEventHeight());
            int top = eventObject.getTopMargin();
            int left = eventObject.getLeftMargin();
            textViewParams.setMargins(left, top, 0, 0);
            eventsView.addView(getTextView(textViewParams, eventObject, ContextCompat.getColor(getContext(), R.color.colorPrimary), textGravity, textSize));
        }
    }

    /**
     * Creates the margins of events
     *
     * @param allOverlappingEvents
     * @param eventWidth
     */
    private void calculateEventMargins(List<EventObject> allOverlappingEvents, int eventWidth, int threeEventsWidth, int additionalEventsWidth) {
        int size = allOverlappingEvents.size();
        for (int k = 0; k < size; k++) {
            EventObject eventObject = allOverlappingEvents.get(k);
            int[] startTime = getStartTime(eventObject.getStartTime());
            int[] endTime = getEndTime(eventObject.getEndTime());
            //0 -> hour & 1 -> minute
            int eventHeight = (int) ((HOUR_VIEW_HEIGHT * endTime[0]) + endTime[1] + DIVIDER_LINE_MARGIN_TOP)
                    - (int) ((HOUR_VIEW_HEIGHT * startTime[0]) + startTime[1] + DIVIDER_LINE_MARGIN_TOP);
            eventHeight = (int) convertDpToPixel(eventHeight, getContext());
            eventObject.setEventHeight(eventHeight);
            showLog("Event Id:::::" + eventObject.getId());
            showLog("Event height:::::" + eventHeight);
            showLog("Event width:::::" + eventWidth);

            int top = (int) convertDpToPixel((HOUR_VIEW_HEIGHT * startTime[0]) + startTime[1] + DIVIDER_LINE_MARGIN_TOP, getContext());
            eventObject.setTopMargin(top);
            int left = 0;//Initially, by default, left margin will be zero
            showLog("X1: " + left);
            eventObject.setX1(left);
            showLog("Y1: " + top);
            eventObject.setY1(top);
            showLog("X2:::::" + (left + eventWidth));
            eventObject.setX2(left + eventWidth);
            showLog("Y2:::::" + (top + eventHeight));
            eventObject.setY2(top + eventHeight);
            showLog("==============================");
        }
        //check collision detection
        List<EventObject> tempList = new ArrayList<>();
        List<EventObject> popUpList = new ArrayList<>();
        if (allOverlappingEvents.size() > 0) {
            EventObject firstObject = allOverlappingEvents.get(0);
            tempList.add(firstObject);
            allOverlappingEvents.remove(0);//removing the first object initially
            if (allOverlappingEvents.size() > 0) {
                for (Iterator<EventObject> iterator = allOverlappingEvents.iterator(); iterator.hasNext(); ) {
                    EventObject tempObject = iterator.next();
                    int marginMultiplier = detectCollision(tempList, tempObject, eventWidth);//detects collision
                    tempObject.setCountMultiplier(marginMultiplier);
                    if (marginMultiplier > MARGIN_MULTIPLIER_MAX_SIZE) {
                        popUpList.add(tempObject);
                        showLog("Popup list object: " + tempObject.getName() + " with ID: " + tempObject.getId() + " has count multiplier: " + tempObject.getCountMultiplier());
                    } else {
                        tempObject.setLeftMargin(marginMultiplier * eventWidth);
                        tempList.add(tempObject);
                    }
                    showLog("=====================================================");
                    iterator.remove();
                }
            }
        }
        drawEventsOnView(tempList, eventWidth, Gravity.NO_GRAVITY, STANDARD_EVENT_TEXT_SIZE);//draws the events. This also fills the empty spaces.
        //Displays the "...", on clicking it displays the additional events
        if (popUpList.size() > 0) {
            //creating additional event object overlay
            List<EventObject> additionalList = new ArrayList<>();
            additionalList.add(getAdditionalEventsObject(threeEventsWidth, popUpList));
            //draw additional event
//            drawOverLappingEvents(additionalList, additionalEventsWidth, Gravity.CENTER_HORIZONTAL, ADDITIONAL_EVENT_TEXT_SIZE);
            drawEventsOnView(additionalList, additionalEventsWidth, Gravity.CENTER_HORIZONTAL, ADDITIONAL_EVENT_TEXT_SIZE);
        }
    }

    /**
     * Code block to check whether two rectangles are colliding and adjusts the margin accordingly
     *
     * @param tempList
     * @param tempObject
     * @return
     */
    private int detectCollision(List<EventObject> tempList, EventObject tempObject, int eventWidth) {
        int count = 0;
        for (int i = 0; i < tempList.size(); i++) {
            if (tempList.get(i).getX1() < tempObject.getX2() &&
                    tempList.get(i).getX2() > tempObject.getX1() &&
                    tempList.get(i).getY1() < tempObject.getY2() &&
                    tempList.get(i).getY2() > tempObject.getY1()) {
                showLog("ID " + tempObject.getId() + " overlaps with, ID " + tempList.get(i).getId());
                count++;
                if (tempList.get(i).getCountMultiplier() != 0 &&
                        tempList.get(i).getX1() == tempObject.getX1() &&
                        tempList.get(i).getX2() == tempObject.getX2()) {//In cases where there can be two events on the left side. Also, checking with the same X-axis, as two events can have same.
                    showLog("Additional count with ID " + tempList.get(i).getId() + " has count multiplier "+tempList.get(i).getCountMultiplier());
                    count = count + tempList.get(i).getCountMultiplier();
                }
            }
        }
        return count;
    }

    private TextView getTextView(RelativeLayout.LayoutParams textViewParams, EventObject eventObject, int eventColor, int textGravity, int textSize) {
        TextView textView = new TextView(getContext());
        textView.setId(rand.nextInt(MAX_RANDOM_VALUE_LIMIT));
        textView.setLayoutParams(textViewParams);
        textView.setText(eventObject.getName());
        textView.setTextColor(Color.WHITE);
//        textView.setBackgroundColor(Color.BLACK);
        textView.setBackgroundResource(R.drawable.event_bg);
        GradientDrawable drawable = (GradientDrawable) textView.getBackground();
        drawable.setColor(eventColor);
        textView.setTextSize(textSize);
        int leftPadding = (int) convertDpToPixel(5, getContext());
        int rightPadding = (int) convertDpToPixel(5, getContext());
        textView.setPadding(leftPadding, 0, rightPadding, 0);
        textView.setGravity(textGravity);
        textView.setTag(eventObject.getId());
//        textView.setAlpha((float) 0.5);
        textView.setOnLongClickListener(this);
        textView.setOnClickListener(this);
        return textView;
    }

    private boolean checkTimeRange(Calendar startTime, Calendar endTime, Calendar currentTime) {
        boolean status = false;
        int currentHour = currentTime.get(Calendar.HOUR_OF_DAY); // Get hour in 24 hour format
        int currentMinute = currentTime.get(Calendar.MINUTE);

        int startHour = startTime.get(Calendar.HOUR_OF_DAY);
        int startMinute = startTime.get(Calendar.MINUTE);

        int endHour = endTime.get(Calendar.HOUR_OF_DAY);
        int endMinute = endTime.get(Calendar.MINUTE);

        Date currentDateTime = parseDate(currentHour + ":" + currentMinute);
        Date startDateTime = parseDate(startHour + ":" + startMinute);
        Date endDateTime = parseDate(endHour + ":" + endMinute);

        if (currentDateTime.before(endDateTime) && currentDateTime.after(startDateTime)) {
            //your logic
            status = true;
        }
        return status;
    }

    private boolean endTimeGreaterThanEventStartTime(Calendar filteredEventEndTime, Calendar allEventCurrentTime) {
        boolean status = false;
        int startHour = allEventCurrentTime.get(Calendar.HOUR_OF_DAY); // Get hour in 24 hour format
        int startMinute = allEventCurrentTime.get(Calendar.MINUTE);

        int endHour = filteredEventEndTime.get(Calendar.HOUR_OF_DAY);
        int endMinute = filteredEventEndTime.get(Calendar.MINUTE);

        Date startEventDateTime = parseDate(startHour + ":" + startMinute);
        Date filteredEventDateTime = parseDate(endHour + ":" + endMinute);

        if (filteredEventDateTime.after(startEventDateTime)) {
            //your logic
            status = true;
        }
        return status;
    }

    private Date parseDate(String date) {

        final String inputFormat = "HH:mm";
        SimpleDateFormat inputParser = new SimpleDateFormat(inputFormat, Locale.US);
        try {
            return inputParser.parse(date);
        } catch (java.text.ParseException e) {
            return new Date(0);
        }
    }

    private int[] getStartTime(Calendar startCalendar) {
        int startHour = startCalendar.get(Calendar.HOUR_OF_DAY);
        int startMinute = startCalendar.get(Calendar.MINUTE);
        int[] startTime = new int[]{startHour, startMinute};
        return startTime;
    }

    private int[] getEndTime(Calendar endCalendar) {
        int endHour = endCalendar.get(Calendar.HOUR_OF_DAY);
        int endMinute = endCalendar.get(Calendar.MINUTE);
        int[] endTime = new int[]{endHour, endMinute};
        return endTime;
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp      A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px      A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return dp;
    }

    @Override
    public boolean onLongClick(View view) {
        // Create a new ClipData.
        // This is done in two steps to provide clarity. The convenience method
        // ClipData.newPlainText() can create a plain text ClipData in one step.

        // Create a new ClipData.Item from the ImageView object's tag
        ClipData.Item item = new ClipData.Item((CharSequence) view.getTag());

        // Create a new ClipData using the tag as a label, the plain text MIME type, and
        // the already-created item. This will create a new ClipDescription object within the
        // ClipData, and set its MIME type entry to "text/plain"
        String[] mimeTypes = {ClipDescription.MIMETYPE_TEXT_PLAIN};

        ClipData data = new ClipData(view.getTag().toString(), mimeTypes, item);

        // Instantiates the drag shadow builder.
        View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);

        // Starts the drag
        view.startDrag(data//data to be dragged
                , shadowBuilder //drag shadow
                , view//local data about the drag and drop operation
                , 0//no needed flags
        );

        //Set view visibility to INVISIBLE as we are going to drag the view
        view.setVisibility(View.INVISIBLE);
        return true;
    }

    @Override
    public boolean onDrag(View view, DragEvent event) {
        // Defines a variable to store the action type for the incoming event
        int action = event.getAction();
        // Handles each of the expected events
        switch (action) {
            case DragEvent.ACTION_DRAG_STARTED:
                // Determines if this View can accept the dragged data
                if (event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                    // if you want to apply color when drag started to your view you can uncomment below lines
                    // to give any color tint to the View to indicate that it can accept
                    // data.

                    // returns true to indicate that the View can accept the dragged data.
                    return true;

                }

                // Returns false. During the current drag and drop operation, this View will
                // not receive events again until ACTION_DRAG_ENDED is sent.
                return false;

            case DragEvent.ACTION_DRAG_ENTERED:
                // Return true; the return value is ignored.
                return true;
            case DragEvent.ACTION_DRAG_LOCATION:
                //no action necessary
                int y = Math.round(view.getY()) + Math.round(event.getY());
                int translatedY = y - scrollView.getScrollY();
                Log.i("translated", "" + translatedY + " " + scrollView.getScrollY() + " " + y);
                int threshold = 50;
                // make a scrolling up due the y has passed the threshold
                if (translatedY < 200) {
                    // make a scroll up by 30 px
                    scrollView.smoothScrollBy(0, -15);
                }
                // make a autoscrolling down due y has passed the 500 px border
                if (translatedY + threshold > scrollView.getHeight() - 200) {
                    // make a scroll down by 30 px
                    scrollView.smoothScrollBy(0, 15);
                }

                return true;
            case DragEvent.ACTION_DRAG_EXITED:
                // Ignore the event
                return true;
            case DragEvent.ACTION_DROP:
                // Gets the item containing the dragged data
                ClipData.Item item = event.getClipData().getItemAt(0);

                // Gets the text data from the item.
                String dragData = item.getText().toString();

                // Displays a message containing the dragged data.
                Toast.makeText(getContext(), "Dragged data is " + dragData, Toast.LENGTH_SHORT).show();

                float X = event.getX();
                float Y = event.getY();

                View textView = (View) event.getLocalState();
                RelativeLayout.LayoutParams textViewParams = (RelativeLayout.LayoutParams) textView.getLayoutParams();
                int top = (int) Y - (textView.getHeight() / 2);
                int left = (int) X - (textView.getWidth() / 2);
                textViewParams.setMargins(left, top, 0, 0);
                textView.setLayoutParams(textViewParams);
                textView.setVisibility(View.VISIBLE);//finally set Visibility to VISIBLE

                // Returns true. DragEvent.getResult() will return true.
                return true;
            case DragEvent.ACTION_DRAG_ENDED:
                // Does a getResult(), and displays what happened.
                if (event.getResult()) {
                    Toast.makeText(getContext(), "The drop was handled.", Toast.LENGTH_SHORT).show();
                } else {
                    View restoreView = (View) event.getLocalState();
                    restoreView.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(), "The drop didn't work.", Toast.LENGTH_SHORT).show();
                }
                // returns true; the value is ignored.
                return true;

            // An unknown action type was received.
            default:
                Log.e("DragDrop Example", "Unknown action type received by OnDragListener.");
                break;
        }
        return false;
    }

    private void showLog(String message) {
        Log.i("Events Info", message);
    }

    /**
     * Creates the additional event object (...)
     *
     * @param threeEventsWidth
     * @param additionalList
     * @return
     */
    private EventObject getAdditionalEventsObject(int threeEventsWidth, List<EventObject> additionalList) {
        //creating additional event object overlay
        EventObject additionalEvenObject = new EventObject();
        additionalEvenObject.setLeftMargin((int) (threeEventsWidth + (1 * EVENT_GAP)));
        additionalEvenObject.setName("...");
        StringBuilder sb = new StringBuilder();
        for (EventObject obj : additionalList) {
            sb.append(obj.getId());
            sb.append(ADDITIONAL_EVENT_ID_SEPARATOR);
        }
        additionalEvenObject.setId(sb.toString().substring(0, sb.toString().length() - 1));

        if (additionalList.size() > 0) {
            Calendar startCalendar = additionalList.get(0).getStartTime();
            additionalEvenObject.setStartTime(startCalendar);

            Calendar endCalendar = Calendar.getInstance();//default height to 30 mins
            endCalendar.set(Calendar.HOUR_OF_DAY, startCalendar.get(Calendar.HOUR_OF_DAY));
            endCalendar.set(Calendar.MINUTE, startCalendar.get(Calendar.MINUTE));
            endCalendar.add(Calendar.MINUTE, 30);
            additionalEvenObject.setEndTime(endCalendar);

            int[] startTime = getStartTime(additionalEvenObject.getStartTime());
            int[] endTime = getEndTime(additionalEvenObject.getEndTime());
            //0 -> hour & 1 -> minute
            int eventHeight = (int) ((HOUR_VIEW_HEIGHT * endTime[0]) + endTime[1] + DIVIDER_LINE_MARGIN_TOP)
                    - (int) ((HOUR_VIEW_HEIGHT * startTime[0]) + startTime[1] + DIVIDER_LINE_MARGIN_TOP);
            eventHeight = (int) convertDpToPixel(eventHeight, getContext());
            additionalEvenObject.setEventHeight(eventHeight);

            int top = (int) convertDpToPixel((HOUR_VIEW_HEIGHT * startTime[0]) + startTime[1] + DIVIDER_LINE_MARGIN_TOP, getContext());
            additionalEvenObject.setTopMargin(top);
        }
        return additionalEvenObject;
    }

    private void setCalendarEvents() {
//        //event 1
//        EventObject eventObject = new EventObject("1", "2:30AM to 4AM event", 2, 30, 4, 0);
//        eventsList.add(eventObject);
//        //event 2
//        eventObject = new EventObject("2", "2:30AM to 4AM event", 2, 30, 4, 0);
//        eventsList.add(eventObject);
//        //event 3
//        eventObject = new EventObject("3", "3AM to 6AM event", 3, 0, 6, 0);
//        eventsList.add(eventObject);
//        //event 4
//        eventObject = new EventObject("4", "2:30AM to 6AM event",2, 30, 6, 0);
//        eventsList.add(eventObject);
//        //event 5
//        eventObject = new EventObject("5", "1AM to 4AM event", 1, 0, 4, 0);
//        eventsList.add(eventObject);
//        //event 6
//        eventObject = new EventObject("6", "5AM to 6AM event", 5, 0, 6, 0);
//        eventsList.add(eventObject);
//        //event 7
//        eventObject = new EventObject("7", "6AM to 7AM event", 6, 0, 7, 0);
//        eventsList.add(eventObject);
//        //event 8
//        eventObject = new EventObject("8", "7AM to 8AM event", 7, 0, 8, 0);
//        eventsList.add(eventObject);
//        //event 9
//        eventObject = new EventObject("9", "7.30AM to 8.30AM event", 7, 30, 8, 30);
//        eventsList.add(eventObject);
//        //event 10
//        eventObject = new EventObject("10", "8AM to 9AM event",8,0,9,0);
//        eventsList.add(eventObject);
//          //event 11
//        eventObject = new EventObject("11", "7AM to 11AM event", 7,0,11,0);
//        eventsList.add(eventObject);

//        //event 12
//        eventObject = new EventObject();
//        eventObject.setId("12");
//        eventObject.setName("1AM to 11PM event");
//
//        Calendar startCalendar = Calendar.getInstance();
//        startCalendar.set(Calendar.HOUR_OF_DAY, 1);
//        startCalendar.set(Calendar.MINUTE, 0);
//        eventObject.setStartTime(startCalendar);
//
//        Calendar endCalendar = Calendar.getInstance();
//        endCalendar.set(Calendar.HOUR_OF_DAY, 23);
//        endCalendar.set(Calendar.MINUTE, 0);
//        eventObject.setEndTime(endCalendar);
//
//        eventsList.add(eventObject);

//        //event 13
//        eventObject = new EventObject("13", "6AM to 7AM event",6,0,7,0);
//        eventsList.add(eventObject);
//        //event 14
//        eventObject = new EventObject("14", "6AM to 7AM event",6,0,7,0);
//        eventsList.add(eventObject);
//        //event 15
//        eventObject = new EventObject("15", "6AM to 7AM event", 6,0,7,0);
//        eventsList.add(eventObject);

//        //event 16
//        eventObject = new EventObject();
//        eventObject.setId("16");
//        eventObject.setName("12AM to 10PM event");
//
//        Calendar startCalendar = Calendar.getInstance();
//        startCalendar.set(Calendar.HOUR_OF_DAY, 0);
//        startCalendar.set(Calendar.MINUTE, 0);
//        eventObject.setStartTime(startCalendar);
//
//        Calendar endCalendar = Calendar.getInstance();
//        endCalendar.set(Calendar.HOUR_OF_DAY, 22);
//        endCalendar.set(Calendar.MINUTE, 0);
//        eventObject.setEndTime(endCalendar);
//
//        eventsList.add(eventObject);

//        //event 17
//        eventObject = new EventObject("17", "5.30AM to 6.30AM event",5,30, 6,30);
//        eventsList.add(eventObject);
//        //event 18
//        eventObject = new EventObject("18", "9AM to 11AM event",9,0,11,0);
//        eventsList.add(eventObject);
//        //event 19
//        eventObject = new EventObject("19", "10AM to 11AM event",10, 0,11,0);
//        eventsList.add(eventObject);
//        //event 20
//        eventObject = new EventObject("20", "6.30AM to 8AM event", 6,30,8,0);
//        eventsList.add(eventObject);
//        //event 21
//        eventObject = new EventObject("21", "1PM to 2PM event",13,0,14,0);
//        eventsList.add(eventObject);
//        //event 22
//        eventObject = new EventObject("22", "2PM to 2:30PM event", 14,0,14,30);
//        eventsList.add(eventObject);

        ///////////////////////////////////////////////////////////////////////////
        //event 1
        EventObject eventObject = new EventObject("1", "8:15AM to 8:30AM event", 8, 15, 8, 30);
        eventsList.add(eventObject);
        //event 10
        eventObject = new EventObject("10", "11:00AM to 12:00PM event", 11, 0, 12, 0);
        eventsList.add(eventObject);
        //event 4
        eventObject = new EventObject("4", "9:42AM to 10:42AM event", 9, 42, 10, 42);
        eventsList.add(eventObject);
        //event 6
        eventObject = new EventObject("6", "10:15AM to 10:45AM event", 10, 15, 10, 45);
        eventsList.add(eventObject);
        //event 8
        eventObject = new EventObject("8", "10:30AM to 11:30AM event", 10, 30, 11, 30);
        eventsList.add(eventObject);
        //event 9
        eventObject = new EventObject("9", "10:30AM to 11:30AM event", 10, 30, 11, 30);
        eventsList.add(eventObject);
        //event 2
        eventObject = new EventObject("2", "8:30AM to 9:00AM event", 8, 30, 9, 0);
        eventsList.add(eventObject);
        //event 5
        eventObject = new EventObject("5", "10:00AM to 11:00AM event", 10, 0, 11, 0);
        eventsList.add(eventObject);
        //event 7
        eventObject = new EventObject("7", "10:30AM to 11:30AM event", 10, 30, 11, 30);
        eventsList.add(eventObject);
        //event 3
        eventObject = new EventObject("3", "8:53AM to 9:53AM event", 8, 53, 9, 53);
        eventsList.add(eventObject);


        Collections.sort(eventsList, new Comparator<EventObject>() {
            public int compare(EventObject o1, EventObject o2) {
                return o1.getStartTime().compareTo(o2.getStartTime());
            }
        });
        for (EventObject object : eventsList) {
            showLog("Event id: " + object.getId());
            showLog("Event start time: " + object.getName());
            showLog("===============================================");
        }
        List<EventObject> tempEventsList = new ArrayList<>();
        tempEventsList.addAll(eventsList);
        manipulateOverlappingEvents(tempEventsList);

//        Collections.sort(eventsList, new CustomDurationComparator());
    }

    private void manipulateOverlappingEvents(List<EventObject> tempEventsList) {
        List<EventObject> overlappingEventObjects = new ArrayList<>();
        Set<EventObject> eventsGroup;
        showLog("Temp list before size: " + tempEventsList.size());
        for (Iterator<EventObject> iterator = tempEventsList.iterator(); iterator.hasNext(); ) {
            EventObject iteratorEventObject = iterator.next();

            showLog("Iterator event object id: " + iteratorEventObject.getId());
            showLog("Iterator event object name: " + iteratorEventObject.getName());

            overlappingEventObjects = getOverlappingEventObjects(iteratorEventObject);

            //adding the overlapping events object to the list, this is a combination of events/overlapping events
            eventsGroup = new HashSet<>();
            eventsGroup.add(iteratorEventObject);
            eventsGroup.addAll(overlappingEventObjects);
            eventsToDraw.add(new ArrayList<EventObject>(eventsGroup));

            mappedEventObjects = new ArrayList<>();
            iterator.remove();
            showLog("Grouped events: " + eventsGroup.size());
            showLog("================================================================");
            if (overlappingEventObjects.size() > 0) {
                break;
            }
        }

        for (EventObject obj : overlappingEventObjects) {
            for (Iterator<EventObject> innerIterator = tempEventsList.iterator(); innerIterator.hasNext(); ) {
                EventObject innerIteratorObject = innerIterator.next();
                if (obj.getId().equalsIgnoreCase(innerIteratorObject.getId())) {
                    innerIterator.remove();
                }
            }
        }

        if (tempEventsList.size() > 0) {
            manipulateOverlappingEvents(tempEventsList);
        } else if (tempEventsList.size() == 0) {
            showLog("Temp list after size: " + tempEventsList.size());
            //Adding an handler since a delay is required to retrieve the width of a view
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    handler.removeCallbacks(this);
                    //draws the events on to the view
                    drawEvents();
                }
            }, 100);
        }
    }

    private List<EventObject> getOverlappingEventObjects(EventObject iteratorEventObject) {

        int size = eventsList.size();
        for (int i = 0; i < size; i++) {
            if (!iteratorEventObject.getId().equalsIgnoreCase(eventsList.get(i).getId()) && !eventsList.get(i).isMarked()) {
                //end time in between start and end time of the events
                boolean endTimeInRange = checkTimeRange(eventsList.get(i).getStartTime(), eventsList.get(i).getEndTime(), iteratorEventObject.getEndTime());
                boolean startTimeInRange = checkTimeRange(eventsList.get(i).getStartTime(), eventsList.get(i).getEndTime(), iteratorEventObject.getStartTime());
                boolean endTimeGreaterThanSTartTime = endTimeGreaterThanEventStartTime(iteratorEventObject.getEndTime(), eventsList.get(i).getStartTime());
                if (endTimeInRange || startTimeInRange || endTimeGreaterThanSTartTime) {
                    eventsList.get(i).setMarked(true);
                    mappedEventObjects.add(eventsList.get(i));
                    getOverlappingEventObjects(eventsList.get(i));//calling the same method again just to make sure any additional overlapping events are present
                } else if (eventsList.get(i).getStartTime().get(Calendar.HOUR_OF_DAY) == iteratorEventObject.getStartTime().get(Calendar.HOUR_OF_DAY) &&
                        eventsList.get(i).getStartTime().get(Calendar.MINUTE) == iteratorEventObject.getStartTime().get(Calendar.MINUTE) &&
                        eventsList.get(i).getEndTime().get(Calendar.HOUR_OF_DAY) == iteratorEventObject.getEndTime().get(Calendar.HOUR_OF_DAY) &&
                        eventsList.get(i).getEndTime().get(Calendar.MINUTE) == iteratorEventObject.getEndTime().get(Calendar.MINUTE)) {
                    eventsList.get(i).setMarked(true);
                    mappedEventObjects.add(eventsList.get(i));
                }
            }
        }
        //Also marking the sent object
        if (!iteratorEventObject.isMarked()) {
            for (int j = 0; j < size; j++) {
                if (iteratorEventObject.getId().equalsIgnoreCase(eventsList.get(j).getId())) {
                    eventsList.get(j).setMarked(true);
                    iteratorEventObject.setMarked(true);//to skip multiple iterations, this is necessary
                }
            }
        }
        return mappedEventObjects;
    }

    @Override
    public void onClick(View v) {

        String eventId = (String) v.getTag();
        if (!eventId.contains(ADDITIONAL_EVENT_ID_SEPARATOR)) {
            for (EventObject obj : eventsList) {
                if (obj.getId().equalsIgnoreCase(eventId)) {
                    Toast.makeText(getContext(), obj.getName(), Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        } else {
            Toast.makeText(getContext(), v.getTag().toString(), Toast.LENGTH_SHORT).show();
        }
    }
}
