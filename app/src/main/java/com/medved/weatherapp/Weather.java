package com.medved.weatherapp;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

// Weather object that represents one day forecast
public class Weather {
    public final String dayOfTheWeek;
    public final String minTemp;
    public final String maxTemp;
    public final String description;
    public final String iconURL;

    public Weather(long timeStamp, double minTemp, double maxTemp, String description, String iconName) {

        // format the temp into a int
        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        numberFormat.setMaximumFractionDigits(0);

        this.dayOfTheWeek = convertTimeStampToDay(timeStamp);
        this.minTemp = numberFormat.format(minTemp) + "\u00B0"; // unicode to degree sign
        this.maxTemp = numberFormat.format(maxTemp) + "\u00B0";
        this.description = description;
        this.iconURL = "http://openweathermap.org/img/w/" + iconName + ".png";
    }

    private static String convertTimeStampToDay(long timeStamp) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeStamp * 1000); // get the time

        TimeZone timeZone = TimeZone.getDefault();
        calendar.add(Calendar.MILLISECOND, timeZone.getOffset(calendar.getTimeInMillis())); // device time zone

        SimpleDateFormat dateFormatter = new SimpleDateFormat("EEEE");
        return dateFormatter.format(calendar.getTime());
    }
}
