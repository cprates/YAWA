package com.isel.pdm.yawa.fragments

import android.app.ListFragment
import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Toast
import com.android.volley.VolleyError
import com.isel.pdm.yawa.*
import com.isel.pdm.yawa.DataContainers.ForecastDO
import com.isel.pdm.yawa.DataContainers.WeatherStateDO
import com.isel.pdm.yawa.UI.ForecastListAdapter
import com.isel.pdm.yawa.tools.DateConverter
import java.text.SimpleDateFormat
import java.util.*

class ForecastFragment : ListFragment() {
    // Can't be initialized here because for each transaction between fragments, the view will be
    // infalted in onCreateView, and the SwipeRefreshLayout will be a new object
    private var swR: SwipeRefreshLayout? = null
    private val callbackSet : ICallbackSet by lazy {
        object : ICallbackSet {
            override fun onError(error: VolleyError) {
                swR?.isRefreshing = false
                Toast.makeText(activity, R.string.error1004, Toast.LENGTH_SHORT).show()
            }
            override fun onSucceed(response: Any?) {
                swR?.isRefreshing = false
                val forecastState = response as ForecastDO

                if(listView.adapter == null) {
                    listView.adapter = ForecastListAdapter(activity,
                            this@ForecastFragment.buildListViewDataSet(forecastState))
                }
                else {
                    // If we already have an adpater, just refresh the views in UI Thread
                    (listView.adapter as ForecastListAdapter).updateData(
                            buildListViewDataSet(activity.application.weatherManager.getLocalForecastWeather()))
                    //TODO: como runnable não é executado...
//                    val run = Runnable(){
//                        fun run() {
                            (listView.adapter as ForecastListAdapter).notifyDataSetChanged()
                            listView.invalidateViews()
                            listView.refreshDrawableState()
//                        }
//                    }
//                    activity.runOnUiThread(run)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // retain the fragment on screen orientation changes
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.forecast_fragment_layout, container, false)
    }

    private fun buildListViewDataSet(forecastList: ForecastDO) : ArrayList<Map<String, Any?>> {
        val retList = ArrayList<Map<String, Any?>>()
        for(forecastDO: WeatherStateDO in forecastList.weatherStateDOList) {
            // Convert Unix seconds to Date
            val date = DateConverter.unixSecondsToDateString(forecastDO.date,
                     TimeZone.getDefault(), SimpleDateFormat("yyyy-MM-dd, E"))
            retList.add(
                    mapOf(
                            Pair(IWeatherManager.DATE_KEY, date),
                            Pair(IWeatherManager.MAX_TEMP_KEY, forecastDO.temp_max.toString()),
                            Pair(IWeatherManager.MIN_TEMP_KEY, forecastDO.temp_min.toString()),
                            Pair(IWeatherManager.WEATHER_ICON_KEY, forecastDO.weatherIcon)
                    )
            )
        }

        return retList
    }


    override fun onPause() {
        super.onPause()
        swR?.isRefreshing = false
    }

    override fun onActivityCreated (savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Set click listener
        listView.setOnItemClickListener { adapterView, view, position, id ->
            onListViewItemClicked(adapterView, view, position, id)
        }
        //
        swR = activity.findViewById(R.id.forecast_weather_swiperefresh) as SwipeRefreshLayout
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
        val city = sharedPref.getString(activity.application.settingsLocationStr, activity.application.defaultLocation)
        swR?.setOnRefreshListener({
            activity.application.weatherManager.refreshForecastWeather(city, callbackSet)
        })

        // update the UI
        activity.application.weatherManager.getForecastWeather(city, callbackSet)

    }

    /**
     * Called when the user press an item of ListView
     */
    private fun onListViewItemClicked(adapterView: AdapterView<*>, view: View, position: Int, id: Long) {
        val newFragment = WeatherDetailsFragment()
        fragmentManager.beginTransaction().replace(R.id.forecast_data_holder, newFragment)
                .addToBackStack(null)
                .commit()

        //
        val bundle = Bundle()
        // TODO: set "positions" as a constant
        bundle.putInt(WeatherDetailsFragment.POSITION_TAG, position)
        newFragment.arguments = bundle
    }
}