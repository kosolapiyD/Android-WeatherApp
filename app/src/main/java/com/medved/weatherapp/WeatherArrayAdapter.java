package com.medved.weatherapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeatherArrayAdapter extends ArrayAdapter<Weather> {

    public WeatherArrayAdapter(@NonNull Context context, List<Weather> forecast) {
        super(context, -1, forecast);
    }

    // nested class for reuse list items while scrolling
    private static class ViewHolder {
        ImageView conditionImageView;
        TextView dayTextView;
        TextView lowTextView;
        TextView highTextView;
        TextView descriptionTextView;
    }

    //cache for already downloaded Bitmap objects
    private Map<String, Bitmap> bitmaps = new HashMap<>();

    @NonNull
    @Override // get custom list_items for ListView
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        Weather day = getItem(position); // position in ListView

        ViewHolder viewHolder; // contains references for each view (list_item) in ListView
        // check the ability to reuse ViewHolder for the element that out of the screen
        if (convertView == null) { // if there is no object - create new
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.list_item, parent, false);
            // the last argument is the automatic attachment of views in List
            // false - in this case because ListView calls getView() to obtain the View of each element of the list and attach it to ListView

            viewHolder.conditionImageView = (ImageView)convertView.findViewById(R.id.conditionImageView);
            viewHolder.dayTextView = (TextView)convertView.findViewById(R.id.dayTextView);
            viewHolder.lowTextView = (TextView)convertView.findViewById(R.id.lowTextView);
            viewHolder.highTextView = (TextView)convertView.findViewById(R.id.highTextView);
            viewHolder.descriptionTextView = (TextView)convertView.findViewById(R.id.descriptionTextView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag(); // reuse existing ViewHolder object
        }
        // if the weather condition image already downloaded (saved in cache), show it
        if (bitmaps.containsKey(day.iconURL)) {
            viewHolder.conditionImageView.setImageBitmap(bitmaps.get(day.iconURL));
        } else {
            // if not, load and show weather condition image
            new LoadImageTask(viewHolder.conditionImageView).execute(day.iconURL);
        }
        // get the data from Weather object and fill in
        Context context = getContext();
        viewHolder.dayTextView.setText(day.dayOfTheWeek.substring(0,1).toUpperCase() + day.dayOfTheWeek.substring(1));
        viewHolder.descriptionTextView.setText(day.description);
        viewHolder.lowTextView.setText(context.getString(R.string.low_temp, day.minTemp));
        viewHolder.highTextView.setText(context.getString(R.string.high_temp, day.maxTemp));


        return convertView;
    }

    // nested class for weather condition image download on background thread
    private class LoadImageTask extends AsyncTask<String, Void, Bitmap> {

        private ImageView imageView;

        public LoadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override // params contains the URL image address
        protected Bitmap doInBackground(String... params) {
            Bitmap bitmap = null;
            HttpURLConnection connection = null;

            try {
                URL url = new URL(params[0]); // create the image URL
                // casting to HttpURLConnection because openConnection() method, returns URLConnection
                connection = (HttpURLConnection) url.openConnection();
                try (InputStream inputStream = connection.getInputStream()){
                    bitmap = BitmapFactory.decodeStream(inputStream);
                    bitmaps.put(params[0], bitmap); // caching
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                connection.disconnect(); // get back to main thread
            }
            return bitmap;
        }

        @Override // and set the image back on main thread
        protected void onPostExecute(Bitmap bitmap) {
            imageView.setImageBitmap(bitmap);
        }
    }
}




















