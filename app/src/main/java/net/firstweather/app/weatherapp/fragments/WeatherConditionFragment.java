/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Yoel Nunez <dev@nunez.guru>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */
package net.firstweather.app.weatherapp.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.firstweather.app.weatherapp.R;
import net.firstweather.app.weatherapp.data.Condition;
import net.firstweather.app.weatherapp.data.Units;

public class WeatherConditionFragment extends Fragment {
    private ImageView weatherIconImageView;
    private TextView dateLabelTextView;
    private TextView highTemperatureTextView;
    private TextView lowTemperatureTextView;
    private TextView forecastTextView;
    public WeatherConditionFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_week, container, false);

        weatherIconImageView = (ImageView) view.findViewById(R.id.weatherIconImageView);
        dateLabelTextView = (TextView) view.findViewById(R.id.dateTextView);
        highTemperatureTextView = (TextView) view.findViewById(R.id.highTemperatureTextView);
        lowTemperatureTextView = (TextView) view.findViewById(R.id.lowTemperatureTextView);
        forecastTextView = (TextView) view.findViewById(R.id.forecastTextView);

        return view;
    }

    public void loadForecast(Condition forecast, Units units) {
        int weatherIconImageResource = getResources().getIdentifier("icon_" + forecast.getCode(), "drawable", getActivity().getPackageName());

        weatherIconImageView.setImageResource(weatherIconImageResource);
        dateLabelTextView.setText(SpolszczenieDniTygodnia(forecast.getDay()));
        highTemperatureTextView.setText(getString(R.string.temperature_output, forecast.getHighTemperature(), units.getTemperature()));
        lowTemperatureTextView.setText(getString(R.string.temperature_output, forecast.getLowTemperature(), units.getTemperature()));
        forecastTextView.setText(SpolszczenieWarunkowAtmosferycznych(forecast.getDescription()));


    }

    public String SpolszczenieWarunkowAtmosferycznych(String angielskaNazwa){

        switch (angielskaNazwa){
            case("Tornado"):
                return "Tornado";

            case("Tropical Storm"):
                return "Burza tropikalna";

            case("Hurricane"):
                return "Huragan";

            case("Severe Thunderstorms"):
                return "Gwałtowne burze";

            case("Thunderstorms"):
                return "Burze";

            case("Mixed Rain And Snow"):
                return "Deszcz ze śniegiem";

            case("Mixed Rain And Sleet"):
                return "Deszcz ze śniegiem";

            case("Mixed Snow And Sleet"):
                return "Deszcz ze śniegiem";

            case("Freezing Drizzle"):
                return "Marznąca mżawka";

            case("Drizzle"):
                return "Mżawka";

            case("Freezing Rain"):
                return "Marznący deszcz";

            case("Showers"):
                return "Przelotny deszcz";

            case("Snow Flurries"):
                return "Śnieżyca";

            case("Light Snow Showers"):
                return "Przelotne opady śniegu";

            case("Blowing Snow"):
                return "Śnieg z wiatrem";

            case("Snow"):
                return "Śnieg";

            case("Rain"):
                return "Deszcz";

            case("Hail"):
                return "Grad";

            case("Sleet"):
                return "Śnieg z deszczem";

            case("Dust"):
                return "Zawierucha";

            case("Foggy"):
                return "Mgła";

            case("Haze"):
                return "Mgła";

            case("Smoky"):
                return "Mgliście";

            case("Blustery"):
                return "Wietrznie";

            case("Windy"):
                return "Wietrznie";

            case("Cold"):
                return "Zimno";

            case("Cloudy"):
                return "Pochmurnie";

            case("Mostly Cloudy"):
                return "Pochmurnie";

            case("Partly Cloudy"):
                return "Częściowe zachmurzenie";

            case("Clear"):
                return "Bezchmurnie";

            case("Sunny"):
                return "Słonecznie";

            case("Fair"):
                return "Bezchmurnie";

            case("Mixed Rain And Hail"):
                return "Deszcz z gradem";

            case("Hot"):
                return "Gorąco";

            case("Isolated Thunderstorms"):
                return "Możliwe burze";

            case("Scattered Thunderstorms"):
                return "Przelotne burze";

            case("Scattered Showers"):
                return "Przelotne opady";

            case("Scattered Snow Showers"):
                return "Przelotne opady śniegu";

            case("Heavy Snow"):
                return "Mocne opady śniegu";

            case("Thundershowers"):
                return "Wieczorne burze";

            case("Snow Showers"):
                return "Opady śniegu";

            case("Isolated Thundershowers"):
                return "Możliwe opady śniegu";

            case("Mostly Sunny"):
                return "Słonecznie";

            case("Not Available"):
                return "Brak danych";

        }

        return angielskaNazwa;
    }

    public String SpolszczenieDniTygodnia(String angielskaNazwa){
        switch (angielskaNazwa){
            case("Mon"):
                return "Pon.";

            case("Tue"):
                return "Wt.";

            case("Wed"):
                return "Śr";

            case("Thu"):
                return "Czw.";

            case("Fri"):
                return "Pt.";

            case("Sat"):
                return "Sob.";

            case("Sun"):
                return "Nd.";
        }

        return angielskaNazwa;
    }
}
